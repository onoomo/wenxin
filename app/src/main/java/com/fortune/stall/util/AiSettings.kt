package com.fortune.stall.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

/** 一个 AI 服务商预设：名称 + OpenAI 兼容接口地址 + 推荐模型名 */
data class AiPreset(val name: String, val endpoint: String, val model: String)

/**
 * AI 设置：API 地址 / 密钥 / 模型 / 自定义提示词 / 上下文长度。
 * 走 OpenAI 兼容的 chat/completions 接口，密钥只存本机。
 * 预设接口与模型名按各平台 2026 年最新文档更新。
 */
object AiSettings {
    private const val PREF = "ai_settings"
    private const val KEY_ENDPOINT = "endpoint"
    private const val KEY_KEY = "api_key"
    private const val KEY_MODEL = "model"
    private const val KEY_PROMPT = "system_prompt"
    private const val KEY_CONTEXT = "context_chars"
    private const val KEY_MIGRATED = "model_migrated_2026"

    const val DEFAULT_ENDPOINT = "https://api.deepseek.com/chat/completions"
    // deepseek-chat 已于 2026/07/24 弃用，改用 deepseek-v4-flash（非思考模式）
    const val DEFAULT_MODEL = "deepseek-v4-flash"
    const val DEFAULT_PROMPT = """你是一位精通中国术数（小六壬、六爻、梅花易数、大六壬、奇门遁甲、八字、紫微斗数、太乙、塔罗等）的断卦师。
请根据我提供的排盘卦象进行专业解读：先点明卦象核心与用神，再结合具体事体分析吉凶成败、时间应期，最后给出简明建议。
要求：不使用开场白与客套话，条理清晰，直击要害；解读仅供参考。"""

    /** 各大 AI 的 OpenAI 兼容接口与最新推荐模型名（点预设一键填入） */
    val PRESETS = listOf(
        AiPreset("DeepSeek", "https://api.deepseek.com/chat/completions", "deepseek-v4-flash"),
        AiPreset("OpenAI", "https://api.openai.com/v1/chat/completions", "gpt-5-mini"),
        AiPreset("通义", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "qwen-plus"),
        AiPreset("智谱", "https://open.bigmodel.cn/api/paas/v4/chat/completions", "glm-5.3-flash"),
        AiPreset("Kimi", "https://api.moonshot.cn/v1/chat/completions", "kimi-k2.6"),
        AiPreset("豆包", "https://ark.cn-beijing.volces.com/api/v3/chat/completions", "doubao-pro-32k"),
        AiPreset("文心", "https://qianfan.baidubce.com/v2/chat/completions", "ernie-4.5-8k"),
        AiPreset("混元", "https://api.hunyuan.cloud.tencent.com/v1/chat/completions", "hunyuan-turbos-latest")
    )

    private var prefs: SharedPreferences? = null

    val endpoint = mutableStateOf(DEFAULT_ENDPOINT)
    val apiKey = mutableStateOf("")
    val model = mutableStateOf(DEFAULT_MODEL)
    val systemPrompt = mutableStateOf(DEFAULT_PROMPT)
    val contextChars = mutableStateOf(6000)

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        endpoint.value = prefs!!.getString(KEY_ENDPOINT, DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT
        apiKey.value = prefs!!.getString(KEY_KEY, "") ?: ""
        model.value = prefs!!.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        systemPrompt.value = prefs!!.getString(KEY_PROMPT, DEFAULT_PROMPT) ?: DEFAULT_PROMPT
        contextChars.value = prefs!!.getInt(KEY_CONTEXT, 6000)

        // 一次性迁移：把已弃用的旧模型名升级到最新，避免老用户升级后调用失败
        if (!prefs!!.getBoolean(KEY_MIGRATED, false)) {
            val upgraded = when (model.value.trim()) {
                "deepseek-chat", "deepseek-reasoner" -> DEFAULT_MODEL
                "gpt-3.5-turbo" -> "gpt-4.1-mini"
                else -> model.value
            }
            if (upgraded != model.value) {
                model.value = upgraded
                prefs!!.edit().putString(KEY_MODEL, upgraded).apply()
            }
            prefs!!.edit().putBoolean(KEY_MIGRATED, true).apply()
        }
    }

    fun save(
        endpoint: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        contextChars: Int
    ) {
        this.endpoint.value = endpoint
        this.apiKey.value = apiKey
        this.model.value = model
        this.systemPrompt.value = systemPrompt
        this.contextChars.value = contextChars
        prefs?.edit()
            ?.putString(KEY_ENDPOINT, endpoint)
            ?.putString(KEY_KEY, apiKey)
            ?.putString(KEY_MODEL, model)
            ?.putString(KEY_PROMPT, systemPrompt)
            ?.putInt(KEY_CONTEXT, contextChars)
            ?.apply()
    }

    fun hasKey(): Boolean = apiKey.value.isNotBlank()
}
