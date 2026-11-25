package com.fortune.stall.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** 一套完整的 AI 配置 */
data class AiConfig(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var endpoint: String,
    var apiKey: String,
    var model: String,
    var systemPrompt: String,
    var contextChars: Int,
    var apiPath: String = ""
)

/**
 * AI 设置：支持多套配置，可新增/切换/重命名/删除，全部只存本机。
 * 走 OpenAI 兼容的 chat/completions 接口，密钥只保存在本机。
 * 预设接口与模型名按各平台 2026 年最新文档更新。
 *
 * 顶层 endpoint/apiKey/model/... 是「当前激活配置」的镜像，供全局现有代码读取；
 * 切换配置时统一刷新，因此对话页模型名会自动跟随当前配置。
 */
object AiSettings {
    private const val PREF = "ai_settings"
    private const val KEY_CONFIGS = "configs_json"
    private const val KEY_ACTIVE = "active_config_id"
    private const val KEY_CUSTOM_PROMPTS = "custom_prompts_json"
    // 老版本单配置键（仅用于一次性迁移）
    private const val KEY_ENDPOINT = "endpoint"
    private const val KEY_KEY = "api_key"
    private const val KEY_MODEL = "model"
    private const val KEY_PROMPT = "system_prompt"
    private const val KEY_CONTEXT = "context_chars"

    const val DEFAULT_ENDPOINT = "https://api.deepseek.com/chat/completions"
    const val DEFAULT_MODEL = "deepseek-v4-flash"
    const val DEFAULT_PROMPT = """你是一位精通中国术数（小六壬、六爻、梅花易数、大六壬、奇门遁甲、八字、紫微斗数、太乙、塔罗等）的断卦师。
请根据我提供的排盘卦象进行专业解读：先点明卦象核心与用神，再结合具体事体分析吉凶成败、时间应期，最后给出简明建议。
要求：不使用开场白与客套话，条理清晰，直击要害；解读仅供参考。"""

    /** 断卦师人设切换预设：名称 + System Prompt 文本 */
    val PROMPT_PRESETS = listOf(
        PromptPreset("默认", DEFAULT_PROMPT),
        PromptPreset(
            "道家小六壬",
            "你是一位精通「道家小六壬」占卜术的玄学大师，请用专业、神秘而富有人文关怀的语气回答。"
        ),
        PromptPreset(
            "专业风水师",
            "你是一位持有执业资格的专业风水师，擅长将传统堪舆学与现代环境科学结合。请用专业严谨的语言分析，注重实际应用和可操作性。"
        ),
        PromptPreset(
            "民间高人",
            "你是一位隐居深山的民间高人，通晓阴阳五行、天机妙算。说话玄妙深奥，善用古语和典故，偶尔透露天机，令人敬畏。"
        ),
        PromptPreset(
            "学术研究者",
            "你是一位研究中国传统术数文化的学者，精通六壬、奇门、太乙等术数体系。请用学术化的语言分析，注重文献考证和理论依据。"
        ),
        PromptPreset(
            "温暖知心",
            "你是一位温暖知心的朋友，懂得小六壬占卜。说话亲切自然，像闺蜜聊天一样，在给出解读的同时保持严谨性。"
        )
    )

    /** 各大 AI 的 OpenAI 兼容接口与最新推荐模型名（点预设一键填入） */
    val PRESETS = listOf(
        AiPreset("DeepSeek", "https://api.deepseek.com/chat/completions", "deepseek-v4-flash"),
        AiPreset("OpenAI", "https://api.openai.com/v1/chat/completions", "gpt-5-mini"),
        AiPreset("通义", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "qwen-plus"),
        AiPreset("智谱", "https://open.bigmodel.cn/api/paas/v4/chat/completions", "glm-5.3-flash"),
        AiPreset("Kimi", "https://api.moonshot.cn/v1/chat/completions", "kimi-k2.6"),
        AiPreset("豆包", "https://ark.cn-beijing.volces.com/api/v3/chat/completions", "doubao-pro-32k"),
        AiPreset("文心", "https://qianfan.baidubce.com/v2/chat/completions", "ernie-4.5-8k"),
        AiPreset("混元", "https://api.hunyuan.cloud.tencent.com/v1/chat/completions", "hunyuan-turbos-latest"),
        // OpenAI 兼容：本地/自建中转常用，接口协议 openai-completions，
        // 地址填主机，路径单独填 /chat/completions，模型名由网关实际列表决定
        AiPreset("OpenAI 兼容", "http://127.0.0.1:8765/v1", "", "/chat/completions", "openai-completions")
    )

    private var prefs: SharedPreferences? = null

    /** 所有配置（可组合观察，增删或字段变更会触发重组） */
    val configs = mutableStateListOf<AiConfig>()
    /** 用户自定义人设（在 6 个内置预设之外） */
    val customPrompts = mutableStateListOf<PromptPreset>()
    /** 当前激活配置 id */
    val activeId = mutableStateOf("")
    /** 配置变更版本号：字段重命名后由此触发界面刷新 */
    val revision = mutableStateOf(0)

    // 镜像：当前激活配置的各字段，供全局现有代码读取
    val endpoint = mutableStateOf(DEFAULT_ENDPOINT)
    val apiKey = mutableStateOf("")
    val model = mutableStateOf(DEFAULT_MODEL)
    val systemPrompt = mutableStateOf(DEFAULT_PROMPT)
    val contextChars = mutableStateOf(6000)
    /** 用户自填的接口路径，如 /chat/completions；为空则用 endpoint 原样 */
    val apiPath = mutableStateOf("")

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        load()
    }

    private fun load() {
        val json = prefs!!.getString(KEY_CONFIGS, null)
        val list = mutableListOf<AiConfig>()
        if (!json.isNullOrBlank()) {
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        AiConfig(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            name = o.optString("name", "配置${i + 1}"),
                            endpoint = o.optString("endpoint", DEFAULT_ENDPOINT),
                            apiKey = o.optString("key", ""),
                            model = o.optString("model", DEFAULT_MODEL),
                            systemPrompt = o.optString("prompt", DEFAULT_PROMPT),
                            contextChars = o.optInt("ctx", 6000),
                            apiPath = o.optString("path", "")
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }
        if (list.isEmpty()) {
            // 迁移老的单配置数据（含已弃用模型名升级）
            val oldEndpoint = prefs!!.getString(KEY_ENDPOINT, null)
            val oldKey = prefs!!.getString(KEY_KEY, null)
            val oldModel = prefs!!.getString(KEY_MODEL, null)
            if (oldEndpoint != null || oldKey != null || oldModel != null) {
                list.add(
                    AiConfig(
                        name = "默认配置",
                        endpoint = oldEndpoint ?: DEFAULT_ENDPOINT,
                        apiKey = oldKey ?: "",
                        model = upgradeModel(oldModel ?: DEFAULT_MODEL),
                        systemPrompt = prefs!!.getString(KEY_PROMPT, DEFAULT_PROMPT) ?: DEFAULT_PROMPT,
                        contextChars = prefs!!.getInt(KEY_CONTEXT, 6000),
                        apiPath = ""
                    )
                )
            } else {
                // 全新用户：用 DeepSeek 预设建一套
                list.add(
                    AiConfig(
                        name = "DeepSeek",
                        endpoint = PRESETS[0].endpoint,
                        apiKey = "",
                        model = PRESETS[0].model,
                        systemPrompt = DEFAULT_PROMPT,
                        contextChars = 6000
                    )
                )
            }
            persist(list)
        }
        configs.clear()
        configs.addAll(list)

        // 读取用户自定义人设
        val cpJson = prefs!!.getString(KEY_CUSTOM_PROMPTS, null)
        if (!cpJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(cpJson)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val nm = o.optString("name", "")
                    val tx = o.optString("text", "")
                    if (nm.isNotBlank() && tx.isNotBlank()) customPrompts.add(PromptPreset(nm, tx))
                }
            } catch (_: Exception) {
            }
        }

        val saved = prefs!!.getString(KEY_ACTIVE, "") ?: ""
        val target = if (list.any { it.id == saved }) saved else list[0].id
        switchTo(target)
    }

    /** 全部人设 = 内置 6 种 + 用户自定义 */
    fun allPrompts(): List<PromptPreset> = PROMPT_PRESETS + customPrompts

    /** 保存一个自定义人设（同名覆盖） */
    fun saveCustomPrompt(name: String, text: String) {
        if (name.isBlank() || text.isBlank()) return
        val idx = customPrompts.indexOfFirst { it.name == name }
        if (idx >= 0) customPrompts[idx] = PromptPreset(name, text)
        else customPrompts.add(PromptPreset(name, text))
        persistCustomPrompts()
        revision.value++
    }

    /** 删除一个自定义人设 */
    fun deleteCustomPrompt(name: String) {
        val removed = customPrompts.removeAll { it.name == name }
        if (removed) {
            persistCustomPrompts()
            revision.value++
        }
    }

    private fun persistCustomPrompts() {
        val arr = JSONArray()
        customPrompts.forEach { p ->
            arr.put(JSONObject().put("name", p.name).put("text", p.text))
        }
        prefs?.edit()?.putString(KEY_CUSTOM_PROMPTS, arr.toString())?.apply()
    }

    private fun upgradeModel(model: String): String = when (model.trim()) {
        "deepseek-chat", "deepseek-reasoner" -> DEFAULT_MODEL
        "gpt-3.5-turbo" -> "gpt-4.1-mini"
        else -> model
    }

    /** 当前激活配置 */
    fun current(): AiConfig? = configs.firstOrNull { it.id == activeId.value }

    /** 切换到指定配置，并刷新镜像 state（对话页模型名随之更新） */
    fun switchTo(id: String) {
        val cfg = configs.firstOrNull { it.id == id } ?: configs.firstOrNull() ?: return
        activeId.value = cfg.id
        apply(cfg)
        revision.value++
        prefs?.edit()?.putString(KEY_ACTIVE, cfg.id)?.apply()
    }

    private fun apply(cfg: AiConfig) {
        endpoint.value = cfg.endpoint
        apiKey.value = cfg.apiKey
        model.value = cfg.model
        systemPrompt.value = cfg.systemPrompt
        contextChars.value = cfg.contextChars
        apiPath.value = cfg.apiPath
    }

    /** 保存当前激活配置的编辑内容 */
    fun saveActive(
        name: String,
        endpoint: String,
        apiKey: String,
        model: String,
        prompt: String,
        ctx: Int,
        apiPath: String
    ) {
        val cfg = current() ?: return
        cfg.name = name
        cfg.endpoint = endpoint
        cfg.apiKey = apiKey
        cfg.model = model
        cfg.systemPrompt = prompt
        cfg.contextChars = ctx
        cfg.apiPath = apiPath
        apply(cfg)
        persist(configs)
        revision.value++
    }

    /** 新增一套配置并切换过去 */
    fun addConfig(name: String, preset: AiPreset? = null) {
        val cfg = AiConfig(
            name = name,
            endpoint = preset?.endpoint ?: DEFAULT_ENDPOINT,
            apiKey = "",
            model = preset?.model ?: DEFAULT_MODEL,
            systemPrompt = DEFAULT_PROMPT,
            contextChars = 6000,
            apiPath = preset?.apiPath ?: ""
        )
        configs.add(cfg)
        persist(configs)
        revision.value++
        switchTo(cfg.id)
    }

    /** 删除配置；至少保留一套 */
    fun deleteConfig(id: String) {
        if (configs.size <= 1) return
        val idx = configs.indexOfFirst { it.id == id }
        if (idx < 0) return
        configs.removeAt(idx)
        if (activeId.value == id) {
            switchTo(configs[(idx - 1).coerceAtLeast(0)].id)
        } else {
            persist(configs)
            revision.value++
        }
    }

    private fun persist(list: List<AiConfig>) {
        val arr = JSONArray()
        list.forEach { c ->
            val o = JSONObject()
            o.put("id", c.id)
            o.put("name", c.name)
            o.put("endpoint", c.endpoint)
            o.put("key", c.apiKey)
            o.put("model", c.model)
            o.put("prompt", c.systemPrompt)
            o.put("ctx", c.contextChars)
            o.put("path", c.apiPath)
            arr.put(o)
        }
        prefs?.edit()?.putString(KEY_CONFIGS, arr.toString())?.apply()
    }

    fun hasKey(): Boolean = apiKey.value.isNotBlank()

    /**
     * 把「API 地址」与用户自填的「接口路径」拼成最终请求地址。
     * - apiPath 为空：返回 endpoint 原样（兼容现有完整地址与所有预设）。
     * - apiPath 以 http:// 或 https:// 开头：视为完整地址，直接采用。
     * - 否则取 endpoint 的 scheme://authority，接上 apiPath（自动补前导 /）。
     */
    fun buildEndpoint(endpoint: String, apiPath: String): String {
        val ep = endpoint.trim()
        val p = apiPath.trim()
        if (p.isBlank()) return ep
        if (p.startsWith("http://") || p.startsWith("https://")) return p
        val schemeIdx = ep.indexOf("://")
        if (schemeIdx < 0) return ep
        val restStart = schemeIdx + 3
        val slash = ep.indexOf('/', restStart)
        val origin = if (slash >= 0) ep.substring(0, slash) else ep
        return origin + if (p.startsWith("/")) p else "/$p"
    }

    /** 用当前镜像的 endpoint + apiPath 得到最终请求地址 */
    fun effectiveEndpoint(): String = buildEndpoint(endpoint.value, apiPath.value)
}

/** 一个 AI 服务商预设：名称 + OpenAI 兼容接口地址 + 推荐模型名 */
data class AiPreset(
    val name: String,
    val endpoint: String,
    val model: String,
    val apiPath: String = "",
    val protocol: String = ""
)

/** 一个断卦师人设预设：名称 + System Prompt */
data class PromptPreset(val name: String, val text: String)
