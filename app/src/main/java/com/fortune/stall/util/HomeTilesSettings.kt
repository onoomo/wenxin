package com.fortune.stall.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import com.fortune.stall.ui.screen.HOME_TILES

/**
 * 首页术法的「排序 + 隐藏」设置。
 * 顺序按 key 列表持久化，隐藏按 key 集合持久化；用 Compose mutableStateOf 承载，
 * 首页长按进入编辑态拖动/隐藏后即时响应，重启后保持。
 */
object HomeTilesSettings {
    private const val PREF = "home_tiles_settings"
    private const val KEY_ORDER = "order"
    private const val KEY_HIDDEN = "hidden"
    private const val KEY_DEFAULT_HIDDEN_APPLIED = "default_hidden_applied_v1"

    /**
     * 首次安装默认隐藏的术法：山向奇门、玄空飞星、手机号预测。
     * 用户可在「隐藏/排序术法」里手动恢复显示。
     */
    private val DEFAULT_HIDDEN = setOf("shanxiangqimen", "ninestar", "shuzi")

    private var prefs: SharedPreferences? = null

    /** 全部默认 key 的顺序 */
    private val defaultKeys: List<String> = HOME_TILES.map { it.key }

    /** 当前显示顺序（含被隐藏项，隐藏项仍在列表末尾保留，方便恢复） */
    val order = mutableStateOf(defaultKeys)

    /** 被隐藏的 key 集合 */
    val hidden = mutableStateOf<Set<String>>(emptySet())

    /** 编辑态版本号：任何顺序/隐藏变动都 +1，供首页强制重组刷新 */
    val revision = mutableStateOf(0)

    /** 是否处于编辑态（长按磁贴或设置页入口触发）。不持久化，仅本次会话内存态。 */
    val editMode = mutableStateOf(false)

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val savedOrder = prefs!!.getString(KEY_ORDER, null)
        if (!savedOrder.isNullOrBlank()) {
            val parsed = savedOrder.split("|").filter { it.isNotBlank() }
            // 兼容模块增减：保留已知 key 的相对顺序，把新增 key 补到末尾，剔除已移除的 key
            val merged = parsed.filter { it in defaultKeys }.toMutableList()
            defaultKeys.forEach { if (it !in merged) merged.add(it) }
            order.value = merged
        }
        val savedHidden = prefs!!.getString(KEY_HIDDEN, null)
        val defaultHiddenApplied = prefs!!.getBoolean(KEY_DEFAULT_HIDDEN_APPLIED, false)
        if (!savedHidden.isNullOrBlank()) {
            hidden.value = savedHidden.split("|").filter { it in defaultKeys }.toSet()
        }
        // 首次（含老用户升级）应用默认隐藏清单，之后只用户手动设置生效
        if (!defaultHiddenApplied) {
            hidden.value = (hidden.value + DEFAULT_HIDDEN).filter { it in defaultKeys }.toSet()
            prefs!!.edit()
                .putString(KEY_HIDDEN, hidden.value.joinToString("|"))
                .putBoolean(KEY_DEFAULT_HIDDEN_APPLIED, true)
                .apply()
        }
    }

    private fun persist() {
        prefs?.edit()
            ?.putString(KEY_ORDER, order.value.joinToString("|"))
            ?.putString(KEY_HIDDEN, hidden.value.joinToString("|"))
            ?.apply()
        revision.value++
    }

    /** 交换两个位置（拖拽排序） */
    fun swap(from: Int, to: Int) {
        if (from == to || from !in order.value.indices || to !in order.value.indices) return
        val list = order.value.toMutableList()
        val tmp = list[from]
        list[from] = list[to]
        list[to] = tmp
        order.value = list
        persist()
    }

    /** 把 from 位置的项移动到 to 位置 */
    fun move(from: Int, to: Int) {
        if (from == to || from !in order.value.indices || to !in order.value.indices) return
        val list = order.value.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        order.value = list
        persist()
    }

    fun isHidden(key: String) = key in hidden.value

    fun setHidden(key: String, value: Boolean) {
        hidden.value = if (value) hidden.value + key else hidden.value - key
        persist()
    }

    fun toggleHidden(key: String) = setHidden(key, !isHidden(key))

    /** 恢复默认顺序与默认隐藏项（山向奇门/玄空飞星/手机号预测隐藏，其余显示） */
    fun reset() {
        order.value = defaultKeys
        hidden.value = DEFAULT_HIDDEN.toSet()
        persist()
    }

    /** 当前可见的 key（保持顺序，剔除隐藏项） */
    fun visibleKeys(): List<String> = order.value.filterNot { it in hidden.value }
}
