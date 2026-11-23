package com.fortune.stall.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

/**
 * 紫微排盘设置：主星亮度流派（对齐 ziwei-ds 的 school-config）。
 * 四套流派：QS 全书派 / XD1 现代修订（默认，与 App 原表一致）/ ZZ 中州派 / XD2 现代修订二。
 */
object ZiweiSettings {
    private const val PREF = "ziwei_settings"
    private const val KEY_SCHOOL = "brightness_school"
    const val DEFAULT_SCHOOL = "XD1"

    private var prefs: SharedPreferences? = null
    val brightnessSchool = mutableStateOf(DEFAULT_SCHOOL)

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        brightnessSchool.value = prefs!!.getString(KEY_SCHOOL, DEFAULT_SCHOOL) ?: DEFAULT_SCHOOL
    }

    fun setSchool(school: String) {
        brightnessSchool.value = school
        prefs?.edit()?.putString(KEY_SCHOOL, school)?.apply()
    }
}
