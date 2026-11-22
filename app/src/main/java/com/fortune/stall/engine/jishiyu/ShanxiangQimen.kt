package com.fortune.stall.engine.jishiyu

/**
 * 山向奇门度数换算 —— 1:1 移植自吉时雨 qimen.js (AGPL-3.0, xianbo.chen@gmail.com)
 * 24 山向 / 72 局度数表 / 黄泉煞 / 双山五行 / 五鼠遁。
 * 排盘九宫部分复用 Qimen.paipan，本对象只负责「度数 → 山向 + 局数 + 日时柱」的换算数据。
 */
object ShanxiangQimen {
    data class Def(val shan: String, val xiang: String, val degree: Int, val ju: Int, val huangquan: String)

    private val _24SAN = arrayOf(
        "巳", "丙", "午", "丁", "未", "坤", "申", "庚", "酉", "辛", "戌", "乾",
        "亥", "壬", "子", "癸", "丑", "艮", "寅", "甲", "卯", "乙", "辰", "巽"
    )
    private val _24XIANG = arrayOf(
        "亥", "壬", "子", "癸", "丑", "艮", "寅", "甲", "卯", "乙", "辰", "巽",
        "巳", "丙", "午", "丁", "未", "坤", "申", "庚", "酉", "辛", "戌", "乾"
    )
    private val HUANGQUANSHA = arrayOf(
        "酉", "亥", "亥", "亥", "卯", "卯", "卯", "巳", "巳", "巳", "午", "午",
        "午", "辰", "辰", "辰", "寅", "寅", "寅", "申", "申", "申", "酉", "酉"
    )
    // 72 局：向 → [(度数, 带符号局数)]  正=阳遁局, 负=阴遁局
    private val _72JU = linkedMapOf(
        "亥" to listOf(315 to -6, 320 to -3, 325 to -9),
        "壬" to listOf(330 to -9, 335 to -3, 340 to -6),
        "子" to listOf(345 to -8, 350 to -2, 355 to -5),
        "癸" to listOf(0 to -7, 5 to -1, 10 to -4),
        "丑" to listOf(15 to -2, 20 to -5, 25 to -8),
        "艮" to listOf(30 to -1, 35 to -4, 40 to -7),
        "寅" to listOf(45 to -9, 50 to -3, 55 to -6),
        "甲" to listOf(60 to -7, 65 to -1, 70 to -4),
        "卯" to listOf(75 to -6, 80 to -9, 85 to -3),
        "乙" to listOf(90 to -5, 95 to -8, 100 to -2),
        "辰" to listOf(105 to -6, 110 to -9, 115 to -3),
        "巽" to listOf(120 to -5, 125 to -8, 130 to -2),
        "巳" to listOf(135 to 4, 140 to 7, 145 to 1),
        "丙" to listOf(150 to 1, 155 to 7, 160 to 4),
        "午" to listOf(165 to 2, 170 to 8, 175 to 5),
        "丁" to listOf(180 to 3, 185 to 9, 190 to 6),
        "未" to listOf(195 to 8, 200 to 5, 205 to 2),
        "坤" to listOf(210 to 9, 215 to 6, 220 to 3),
        "申" to listOf(225 to 1, 230 to 7, 235 to 4),
        "庚" to listOf(240 to 3, 245 to 9, 250 to 6),
        "酉" to listOf(255 to 4, 260 to 1, 265 to 7),
        "辛" to listOf(270 to 5, 275 to 2, 280 to 8),
        "戌" to listOf(285 to 4, 290 to 1, 295 to 7),
        "乾" to listOf(300 to 5, 305 to 2, 310 to 8)
    )
    private val _2SAN5XING = arrayOf(
        "壬子", "癸丑", "艮寅", "甲卯", "乙辰", "巽巳",
        "丙午", "丁未", "坤申", "庚酉", "辛戌", "乾亥"
    )

    val SHANXIANG: Map<Int, Def> = buildMap {
        for ((xiang, items) in _72JU) {
            val xi = _24XIANG.indexOf(xiang)
            val shan = _24SAN[xi]
            val hq = HUANGQUANSHA[xi]
            for ((deg, ju) in items) put(deg, Def(shan, xiang, deg, ju, hq))
        }
    }

    /** 度数 → 山向数据（degree 向下取整到 5 度，360→0） */
    fun findByDegree(degreeInput: Int): Def? {
        var degree = degreeInput
        if (degree > 360 || degree < 0) degree = 0
        degree = degree / 5 * 5
        if (degree == 360) degree = 0
        return SHANXIANG[degree]
    }

    /** 年份 → 年干支（原版：gan=(year-4)%10, zhi=(year-4)%12） */
    fun yearGanZhi(year: Int): String {
        val gan = "甲乙丙丁戊己庚辛壬癸"
        val zhi = "子丑寅卯辰巳午未申酉戌亥"
        val gi = Math.floorMod(year - 4, 10)
        val zi = Math.floorMod(year - 4, 12)
        return "${gan[gi]}${zhi[zi]}"
    }

    /** 双山五行：由「向」取对应地支 */
    private fun findZhi(x: String): String? {
        for (pair in _2SAN5XING) if (pair.contains(x)) return pair.substring(1, 2)
        return null
    }

    /** 五鼠遁：以年干当日干，查指定地支的干支 */
    private fun findGanZhi(gan: String, zhi: String): String? {
        val arr = QimenData.WU_SHU_DUN[gan] ?: return null
        for (gz in arr) if (gz.contains(zhi)) return gz
        return null
    }

    /** 依据年干 + 向，求时柱干支 */
    fun shiZhuByYearGan(yearGan: String, def: Def): String {
        val zhi = findZhi(def.xiang) ?: return ""
        return findGanZhi(yearGan, zhi) ?: ""
    }
}
