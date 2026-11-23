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
}
