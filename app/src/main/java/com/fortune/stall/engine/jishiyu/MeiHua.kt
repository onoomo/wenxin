package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import java.time.LocalDateTime

/** 梅花易数 —— 1:1 移植自吉时雨 meihua.js (AGPL-3.0, xianbo.chen@gmail.com) */
object MeiHua {
    val GUA8 = arrayOf("乾", "兑", "离", "震", "巽", "坎", "艮", "坤")
    val GUA_CHAR = mapOf(
        "乾" to "☰",
        "坎" to "☵",
        "艮" to "☶",
        "震" to "☳",
        "巽" to "☴",
        "离" to "☲",
        "坤" to "☷",
        "兑" to "☱",
    )
    val DIZHI_NUM = mapOf(
        "子" to 1, "丑" to 2, "寅" to 3, "卯" to 4, "辰" to 5, "巳" to 6,
        "午" to 7, "未" to 8, "申" to 9, "酉" to 10, "戌" to 11, "亥" to 12
    )
    val HEX = mapOf(
        "乾" to "111",
        "坤" to "000",
        "震" to "001",
        "巽" to "110",
        "坎" to "010",
        "离" to "101",
        "艮" to "100",
        "兑" to "011",
    )
    val GUA64 = mapOf(
        "000000" to "坤为地",
        "100000" to "山地剥",
        "010000" to "水地比",
        "110000" to "风地观",
        "001000" to "雷地豫",
        "101000" to "火地晋",
        "011000" to "泽地萃",
        "111000" to "天地否",
        "000100" to "地山谦",
        "100100" to "艮为山",
        "010100" to "水山蹇",
        "110100" to "风山渐",
        "001100" to "雷山小过",
        "101100" to "火山旅",
        "011100" to "泽山咸",
        "111100" to "天山遁",
        "000010" to "地水师",
        "100010" to "山水蒙",
        "010010" to "坎为水",
        "110010" to "风水涣",
        "001010" to "雷水解",
        "101010" to "火水未济",
        "011010" to "泽水困",
        "111010" to "天水讼",
        "000110" to "地风升",
        "100110" to "山风蛊",
        "010110" to "水风井",
        "110110" to "巽为风",
        "001110" to "雷风恒",
        "101110" to "火风鼎",
        "011110" to "泽风大过",
        "111110" to "天风姤",
        "000001" to "地雷复",
        "100001" to "山雷颐",
        "010001" to "水雷屯",
        "110001" to "风雷益",
        "001001" to "震为雷",
        "101001" to "火雷噬嗑",
        "011001" to "泽雷随",
        "111001" to "天雷无妄",
        "000101" to "地火明夷",
        "100101" to "山火贲",
        "010101" to "水火既济",
        "110101" to "风火家人",
        "001101" to "雷火丰",
        "101101" to "离为火",
        "011101" to "泽火革",
        "111101" to "天火同人",
        "000011" to "地泽临",
        "100011" to "山泽损",
        "010011" to "水泽节",
        "110011" to "风泽中孚",
        "001011" to "雷泽归妹",
        "101011" to "火泽睽",
        "011011" to "兑为泽",
        "111011" to "天泽履",
        "000111" to "地天泰",
        "100111" to "山天大畜",
        "010111" to "水天需",
        "110111" to "风天小畜",
        "001111" to "雷天大壮",
        "101111" to "火天大有",
        "011111" to "泽天夬",
        "111111" to "乾为天",
    )

    data class GuaData(val upper: String, val lower: String, val deltaYao: Int,
        val base: String, val hu: String, val bian: String, val cuo: String, val zong: String,
        val tiGua: String, val yongGua: String, val tiWx: String, val yongWx: String, val relation: String)

    private fun trigramOf(hex3: String): String = HEX.entries.firstOrNull { it.value==hex3 }?.key ?: ""
    private fun wxOf(gua: String): String = when(gua){"乾","兑"->"金";"震","巽"->"木";"坎"->"水";"离"->"火";"艮","坤"->"土";else->""}
    private fun shengKe(ti: String, yong: String): String {
        if (ti==yong) return "体用比和，吉"
        val sheng=mapOf("木" to "火","火" to "土","土" to "金","金" to "水","水" to "木")
        val ke=mapOf("木" to "土","土" to "水","水" to "火","火" to "金","金" to "木")
        return when { sheng[ti]==yong -> "体生用，泄气，事难成"; sheng[yong]==ti -> "用生体，得生，吉"; ke[ti]==yong -> "体克用，吉"; ke[yong]==ti -> "用克体，凶"; else -> "体用比和" }
    }

    /** 由上下卦名与动爻(1-6)组装卦象 */
    fun compose(upper: String, lower: String, deltaYao: Int): GuaData {
        val ub = HEX[upper] ?: "111"; val lb = HEX[lower] ?: "111"
        val base = ub + lb
        val cl = deltaYao - 1
        val hu = base.substring(1,4) + base.substring(2,5)
        val bian = base.mapIndexed { i,b -> if (5-i==cl) if(b=='1') "0" else "1" else b.toString() }.joinToString("")
        val cuo = base.map { if(it=='1') "0" else "1" }.joinToString("")
        val zong = base.reversed()
        val tiUpper = deltaYao > 3
        val ti = if (tiUpper) upper else lower
        val yong = if (tiUpper) lower else upper
        return GuaData(upper, lower, deltaYao, base, hu, bian, cuo, zong, ti, yong, wxOf(ti), wxOf(yong), shengKe(wxOf(ti), wxOf(yong)))
    }

    /** 时间起卦: 年支数+农历月+农历日 定上卦, 加时支数定下卦与动爻 */
    fun byTime(dt: LocalDateTime): GuaData {
        val lun = LunarCalendar.fromDateTime(dt)
        val yearZhi = lun.yearGanZhi.substring(1)
        val yearNum = DIZHI_NUM[yearZhi] ?: 1
        val monthNum = kotlin.math.abs(lun.month)
        val dayNum = lun.day
        val hourNum = DIZHI_NUM[lun.hourZhi] ?: 1
        val up = (yearNum+monthNum+dayNum) % 8; val u = if(up==0) 8 else up
        val lo = (yearNum+monthNum+dayNum+hourNum) % 8; val l = if(lo==0) 8 else lo
        val d0 = (yearNum+monthNum+dayNum+hourNum) % 6; val d = if(d0==0) 6 else d0
        return compose(GUA8[u-1], GUA8[l-1], d)
    }

    /** 数字起卦 */
    fun byNumber(digits: String, addHour: Boolean, dt: LocalDateTime): GuaData {
        val ds = digits.filter { it.isDigit() }.map { it.toString() }
        val n = ds.size; var A: Int; var B: Int
        if (n==1) { A = (ds[0].toInt()%8).let{if(it==0) 8 else it}; B=A }
        else {
            val half = if (n==2) 1 else n/2
            val s1 = ds.take(half).sumOf{it.toInt()}; val s2 = ds.drop(half).sumOf{it.toInt()}
            A = if (s1 in 1..8) s1 else (s1%8).let{if(it==0) 8 else it}
            B = if (s2 in 1..8) s2 else (s2%8).let{if(it==0) 8 else it}
        }
        val hourMap = arrayOf(1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,1)
        val t = if (addHour) hourMap[dt.hour] else 0
        val c0 = (A+B+t)%6; val C = if(c0==0) 6 else c0
        return compose(GUA8[A-1], GUA8[B-1], C)
    }
    fun guaName(bits: String): String = GUA64[bits] ?: ""
}