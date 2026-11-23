package com.fortune.stall.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

/**
 * AI 设置：API 地址 / 密钥 / 模型 / 自定义提示词 / 上下文长度。
 * 参考 cococora-s/jiang-xiao-liu-ren：走 OpenAI 兼容的 chat/completions 接口，
 * 支持 DeepSeek / OpenAI / 任意兼容端点，密钥只存本机。
 */
object AiSettings {
    private const val PREF = "ai_settings"
    private const val KEY_ENDPOINT = "endpoint"
    private const val KEY_KEY = "api_key"
    private const val KEY_MODEL = "model"
    private const val KEY_PROMPT = "system_prompt"
    private const val KEY_CONTEXT = "context_chars"

    // 默认走 DeepSeek（与参考项目一致），用户可在 API 配置页自由切换
    const val DEFAULT_ENDPOINT = "https://api.deepseek.com/chat/completions"
    const val DEFAULT_MODEL = "deepseek-chat"
    const val DEFAULT_PROMPT = """你是一位精通中国术数（小六壬、六爻、梅花易数、大六壬、奇门遁甲、八字、紫微斗数、太乙、塔罗等）的断卦师。
请根据我提供的排盘卦象进行专业解读：先点明卦象核心与用神，再结合具体事体分析吉凶成败、时间应期，最后给出简明建议。
要求：不使用开场白与客套话，条理清晰，直击要害；解读仅供参考。"""

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
