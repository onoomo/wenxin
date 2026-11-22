package com.fortune.stall.engine.jishiyu

/**
 * 吉时雨术数模块通用数据模型。
 * 所有模块引擎把排盘结果组装成 CastResult，由通用渲染器呈现，
 * 从而 18 个模块共用一套 UI，风格统一。
 */

/** 结果里的一个分节：可有键值对、长文本段落、表格三选一或组合 */
data class CastSection(
    val title: String = "",
    val rows: List<Pair<String, String>> = emptyList(),
    val paragraphs: List<String> = emptyList(),
    val table: List<List<String>>? = null,
    val highlighted: Boolean = false
)

/** 一个模块的完整排盘结果 */
data class CastResult(
    val title: String,
    val subtitle: String = "",
    val seal: String = "",
    val sections: List<CastSection> = emptyList()
)

/** 输入字段类型 */
enum class FieldType { DATETIME, DATE, TIME, GENDER, NUMBER, TEXT }

/** 一个输入字段定义 */
data class InputField(
    val key: String,
    val label: String,
    val type: FieldType,
    val default: String = "",
    val hint: String = ""
)

/** 模块定义：元数据 + 输入字段 + 排盘函数 */
data class ModuleDef(
    val key: String,
    val name: String,
    val eyebrow: String,
    val desc: String,
    val fields: List<InputField>,
    val cast: (Map<String, String>) -> CastResult
)
