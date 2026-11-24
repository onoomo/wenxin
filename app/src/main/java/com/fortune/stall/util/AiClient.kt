package com.fortune.stall.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/** 一条对话消息：role = system / user / assistant */
data class ChatMessage(val role: String, val content: String)

/** AI 请求结果：成功返回文本，失败返回可读错误 */
sealed class AiResult {
    data class Ok(val text: String) : AiResult()
    data class Err(val message: String) : AiResult()
}

/** 拉取模型列表结果 */
sealed class ModelsResult {
    data class Ok(val models: List<String>) : ModelsResult()
    data class Err(val message: String) : ModelsResult()
}

/**
 * OpenAI 兼容 chat/completions 客户端。
 * 不引入第三方网络库，直接用 JDK 的 HttpURLConnection（minSdk 24 可用），保证离线构建。
 * 支持 DeepSeek / OpenAI / 任意兼容端点。
 */
object AiClient {

    /**
     * 发起一次对话。
     * @param messages 完整消息列表（含 system 指令与历史上下文）
     */
    suspend fun chat(
        endpoint: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>
    ): AiResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(endpoint)
            conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "POST"
                connectTimeout = 20000
                readTimeout = 90000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Accept", "application/json")
                doOutput = true
            }

            val root = JSONObject().apply {
                put("model", model)
                put("stream", false)
                put("temperature", 0.7)
                val arr = JSONArray()
                messages.forEach { m ->
                    arr.put(JSONObject().put("role", m.role).put("content", m.content))
                }
                put("messages", arr)
            }
            val payload = root.toString()

            val os: OutputStream = conn.outputStream
            os.write(payload.toByteArray(StandardCharsets.UTF_8))
            os.flush()
            os.close()

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { r ->
                val sb = StringBuilder()
                var line: String?
                while (r.readLine().also { line = it } != null) sb.append(line)
                sb.toString()
            }

            if (code !in 200..299) {
                val err = try {
                    val e = JSONObject(text).optJSONObject("error")
                    e?.optString("message")?.takeIf { it.isNotBlank() } ?: "HTTP $code"
                } catch (e: Exception) {
                    "HTTP $code"
                }
                return@withContext AiResult.Err(err)
            }

            val reply = try {
                val data = JSONObject(text)
                val choices = data.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
                } else {
                    ""
                }
            } catch (e: Exception) {
                ""
            }

            if (reply.isBlank()) {
                AiResult.Err("AI 返回为空，请检查模型名与接口是否正确")
            } else {
                AiResult.Ok(reply.trim())
            }
        } catch (e: Exception) {
            val msg = when (e) {
                is java.net.SocketTimeoutException -> "连接超时，请检查网络或接口地址"
                is java.net.UnknownHostException -> "无法解析域名，请检查网络"
                is javax.net.ssl.SSLException -> "SSL 连接失败，请检查接口地址"
                else -> e.message?.takeIf { it.isNotBlank() } ?: "请求失败"
            }
            AiResult.Err(msg)
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * 从接口读取可用模型列表（OpenAI 兼容 GET /models）。
     * 由 chat/completions 端点推导出 /models 地址：
     *   https://xxx/v1/chat/completions  →  https://xxx/v1/models
     *   https://xxx/chat/completions     →  https://xxx/models
     * 需 API 地址 + Key 组合有效才能返回。
     */
    suspend fun fetchModels(endpoint: String, apiKey: String): ModelsResult =
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val modelsUrl = toModelsUrl(endpoint)
                conn = URL(modelsUrl).openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "GET"
                    connectTimeout = 20000
                    readTimeout = 30000
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    setRequestProperty("Accept", "application/json")
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { r ->
                    val sb = StringBuilder()
                    var line: String?
                    while (r.readLine().also { line = it } != null) sb.append(line)
                    sb.toString()
                }
                if (code !in 200..299) {
                    val err = try {
                        val e = JSONObject(text).optJSONObject("error")
                        e?.optString("message")?.takeIf { it.isNotBlank() } ?: "HTTP $code"
                    } catch (_: Exception) {
                        "HTTP $code（请检查 API 地址与 Key 是否正确）"
                    }
                    return@withContext ModelsResult.Err(err)
                }
                val data = JSONObject(text).optJSONArray("data")
                if (data == null || data.length() == 0) {
                    return@withContext ModelsResult.Err("接口未返回模型列表")
                }
                val models = (0 until data.length()).mapNotNull { i ->
                    data.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }
                }
                if (models.isEmpty()) {
                    ModelsResult.Err("模型列表为空")
                } else {
                    ModelsResult.Ok(models)
                }
            } catch (e: Exception) {
                val msg = when (e) {
                    is java.net.SocketTimeoutException -> "连接超时，请检查网络或接口地址"
                    is java.net.UnknownHostException -> "无法解析域名，请检查网络"
                    is javax.net.ssl.SSLException -> "SSL 连接失败，请检查接口地址"
                    else -> e.message?.takeIf { it.isNotBlank() } ?: "读取失败"
                }
                ModelsResult.Err(msg)
            } finally {
                conn?.disconnect()
            }
        }

    /** 由 chat/completions 端点推导 models 端点 */
    private fun toModelsUrl(endpoint: String): String {
        val trimmed = endpoint.trim().trimEnd('/')
        return when {
            trimmed.endsWith("/chat/completions") ->
                trimmed.removeSuffix("/chat/completions") + "/models"
            trimmed.contains("/chat/completions") ->
                trimmed.substringBefore("/chat/completions") + "/models"
            trimmed.endsWith("/models") -> trimmed
            else -> "$trimmed/models"
        }
    }
}
