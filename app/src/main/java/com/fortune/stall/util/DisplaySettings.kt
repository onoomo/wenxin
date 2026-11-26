package com.fortune.stall.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

/**
 * 首页显示模式：默认「详细卡片」，开启后切为「图标网格」。
 * 持久化到 SharedPreferences，用 Compose mutableStateOf 承载，设置页切换后首页即时响应。
 */
object DisplaySettings {
    private const val PREF = "display_settings"
    private const val KEY_ICON_MODE = "home_icon_mode"
    private const val KEY_INTRO_ANIM = "home_intro_anim"

    private var prefs: SharedPreferences? = null

    /** 首页是否使用图标网格（默认 false = 详细卡片） */
    val iconMode = mutableStateOf(false)

    /** 是否播放首页开场动画（默认 false = 关闭，直接进摊面；开启后才播放一次） */
    val introAnim = mutableStateOf(false)

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        iconMode.value = prefs!!.getBoolean(KEY_ICON_MODE, false)
        introAnim.value = prefs!!.getBoolean(KEY_INTRO_ANIM, false)
    }

    fun setIconMode(value: Boolean) {
        iconMode.value = value
        prefs?.edit()?.putBoolean(KEY_ICON_MODE, value)?.apply()
    }

    fun setIntroAnim(value: Boolean) {
        introAnim.value = value
        prefs?.edit()?.putBoolean(KEY_INTRO_ANIM, value)?.apply()
    }
}
