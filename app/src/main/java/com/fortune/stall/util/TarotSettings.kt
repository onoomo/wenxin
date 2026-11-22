package com.fortune.stall.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

/**
 * 塔罗牌面显示开关：持久化到 SharedPreferences，默认开启（在线从 Gitee 拉取）。
 * 用 Compose mutableStateOf 承载，设置页切换后各页面即时响应。
 */
object TarotSettings {
    private const val PREF = "tarot_settings"
    private const val KEY_SHOW_IMAGE = "show_card_image"

    private var prefs: SharedPreferences? = null

    /** 是否显示塔罗牌面（默认 true） */
    val showCardImage = mutableStateOf(true)

    /** 牌面缓存版本号，下载/删除后 +1 触发牌面刷新 */
    val imageVersion = mutableStateOf(0)
    fun bumpImageVersion() { imageVersion.value++ }

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        showCardImage.value = prefs!!.getBoolean(KEY_SHOW_IMAGE, true)
    }

    fun setShowCardImage(value: Boolean) {
        showCardImage.value = value
        prefs?.edit()?.putBoolean(KEY_SHOW_IMAGE, value)?.apply()
    }
}
