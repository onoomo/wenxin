package com.fortune.stall.engine

import com.fortune.stall.util.LunarCalendar
import java.time.LocalDateTime

/**
 * 六爻纳甲排盘引擎 —— 完整移植 divination-skills/liuyao/scripts/paigua.py。
 * 八宫六十四卦按卦变规则自动生成，纳甲纳支、六亲、六神、世应、旬空月破、伏神、动变一应俱全。
 */
object LiuYao {
    private val TIANGAN = LunarCalendar.TIANGAN
    private val DIZHI = LunarCalendar.DIZHI
    private val WUXING_ZHI = LunarCalendar.ZHI_WUXING
    private val WUXING_GAN = LunarCalendar.GAN_WUXING
    private val SHENG = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木")
    private val KE = mapOf("木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木")
    private val LIUSHEN = arrayOf("青龙", "朱雀", "勾陈", "腾蛇", "白虎", "玄武")

    private data class Jing(val hua: String, val wx: String, val neiGan: String, val waiGan: String,
                            val neiZhi: List<String>, val waiZhi: List<String>, val yinyang: String)
    private val JING_GUA = mapOf(
        "乾" to Jing("111", "金", "甲", "壬", listOf("子", "寅", "辰"), listOf("午", "申", "戌"), "阳"),
        "震" to Jing("100", "木", "庚", "庚", listOf("子", "寅", "辰"), listOf("午", "申", "戌"), "阳"),
        "坎" to Jing("010", "水", "戊", "戊", listOf("寅", "辰", "午"), listOf("申", "戌", "子"), "阳"),
        "艮" to Jing("001", "土", "丙", "丙", listOf("辰", "午", "申"), listOf("戌", "子", "寅"), "阳"),
        "坤" to Jing("000", "土", "乙", "癸", listOf("未", "巳", "卯"), listOf("丑", "亥", "酉"), "阴"),
        "巽" to Jing("011", "木", "辛", "辛", listOf("丑", "亥", "酉"), listOf("未", "巳", "卯"), "阴"),
        "离" to Jing("101", "火", "己", "己", listOf("卯", "丑", "亥"), listOf("酉", "未", "巳"), "阴"),
        "兑" to Jing("110", "金", "丁", "丁", listOf("巳", "卯", "丑"), listOf("亥", "酉", "未"), "阴")
    )
    private val HUA_TO_NAME = JING_GUA.entries.associate { it.value.hua to it.key }

    private val XIANG = mapOf("乾" to "天", "坤" to "地", "坎" to "水", "离" to "火", "震" to "雷", "巽" to "风", "艮" to "山", "兑" to "泽")
    private val FULLNAME_TABLE = mapOf(
        "乾乾" to "乾为天", "坤坤" to "坤为地", "坎坎" to "坎为水", "离离" to "离为火", "震震" to "震为雷", "巽巽" to "巽为风", "艮艮" to "艮为山", "兑兑" to "兑为泽",
        "坤乾" to "地天泰", "乾坤" to "天地否", "震坎" to "雷水解", "坎震" to "水雷屯", "艮坤" to "山地剥", "坤艮" to "地山谦", "巽乾" to "风天小畜", "乾巽" to "天风姤",
        "离坤" to "火地晋", "坤离" to "地火明夷", "兑乾" to "泽天夬", "乾兑" to "天泽履", "震乾" to "雷天大壮", "乾震" to "天雷无妄", "坎坤" to "水地比", "坤坎" to "地水师",
        "艮乾" to "山天大畜", "乾艮" to "天山遁", "巽坤" to "风地观", "坤巽" to "地风升", "离乾" to "火天大有", "乾离" to "天火同人", "兑坤" to "泽地萃", "坤兑" to "地泽临",
        "震坤" to "雷地豫", "坤震" to "地雷复", "巽坎" to "风水涣", "坎巽" to "水风井", "离艮" to "火山旅", "艮离" to "山火贲", "兑震" to "泽雷随", "震兑" to "雷泽归妹",
        "艮坎" to "山水蒙", "坎艮" to "水山蹇", "巽艮" to "风山渐", "艮巽" to "山风蛊", "离坎" to "火水未济", "坎离" to "水火既济", "兑巽" to "泽风大过", "巽兑" to "风泽中孚",
        "震艮" to "雷山小过", "艮震" to "山雷颐", "离兑" to "火泽睽", "兑离" to "泽火革", "坎乾" to "水天需", "乾坎" to "天水讼", "巽离" to "风火家人", "离巽" to "火风鼎",
        "震离" to "雷火丰", "离震" to "火雷噬嗑", "兑坎" to "泽水困", "坎兑" to "水泽节", "艮兑" to "山泽损", "兑艮" to "泽山咸", "巽震" to "风雷益", "震巽" to "雷风恒"
    )

    private data class GuaInfo(val xia: String, val shang: String, val gong: String, val shi: Int, val ying: Int)
    private val GUA64 = linkedMapOf<String, GuaInfo>()

    init { initBagong() }

    private fun fullName(xia: String, shang: String): String =
        FULLNAME_TABLE["$xia$shang"] ?: "${XIANG[xia]}${XIANG[shang]}卦"

    private fun initBagong() {
        val gongOrder = listOf("乾", "震", "坎", "艮", "坤", "巽", "离", "兑")
        for (gong in gongOrder) {
            val base = (JING_GUA[gong]!!.hua + JING_GUA[gong]!!.hua).toCharArray()
            // 本宫卦（八纯卦，六世）
            fun reg(cur: CharArray, shi: Int, ying: Int) {
                val xia = HUA_TO_NAME[cur.take(3).joinToString("")]!!
                val shang = HUA_TO_NAME[cur.drop(3).joinToString("")]!!
                GUA64[fullName(xia, shang)] = GuaInfo(xia, shang, gong, shi, ying)
            }
            reg(base.copyOf(), 6, 3)
            val prev = base.copyOf()
            for (world in 1..5) {
                val idx = world - 1
                prev[idx] = if (prev[idx] == '1') '0' else '1'
                val shi = world
                val ying = (shi + 3 - 1) % 6 + 1
                reg(prev.copyOf(), shi, ying)
            }
            val you = prev.copyOf()
            you[3] = base[3]
            reg(you.copyOf(), 4, 1)
            val gui = you.copyOf()
            gui[0] = base[0]; gui[1] = base[1]; gui[2] = base[2]
            reg(gui.copyOf(), 3, 6)
        }
    }

    private fun liuQin(gongWx: String, yaoWx: String): String = when {
        gongWx == yaoWx -> "兄弟"
        SHENG[gongWx] == yaoWx -> "子孙"
        SHENG[yaoWx] == gongWx -> "父母"
        KE[gongWx] == yaoWx -> "妻财"
        KE[yaoWx] == gongWx -> "官鬼"
        else -> "?"
    }

    private fun liushenStart(riGan: String): Int = when (riGan) {
        "甲", "乙" -> 0; "丙", "丁" -> 1; "戊" -> 2; "己" -> 3; "庚", "辛" -> 4; "壬", "癸" -> 5; else -> 0
    }

    private fun ganIdx(g: String) = TIANGAN.indexOf(g)
    private fun zhiIdx(z: String) = DIZHI.indexOf(z)

    data class DateGz(val nian: String, val yue: String, val ri: String, val shi: String)

    /** 公历 → 四柱干支（用自实现农历，可接受近似节气月） */
    fun dateToGanzhi(dt: LocalDateTime): DateGz {
        val lun = LunarCalendar.fromDateTime(dt)
        return DateGz(lun.yearGanZhi, lun.monthGanZhi, lun.dayGanZhi, lun.hourGanZhi)
    }

    private fun getXunkong(riGz: String): Pair<String, String> {
        val gi = ganIdx(riGz[0].toString())
        val zi = zhiIdx(riGz[1].toString())
        val start = (zi - gi).mod(12)
        return DIZHI[(start + 10) % 12] to DIZHI[(start + 11) % 12]
    }

    private val CHONG_MAP = mapOf("子" to "午", "丑" to "未", "寅" to "申", "卯" to "酉", "辰" to "戌", "巳" to "亥",
        "午" to "子", "未" to "丑", "申" to "寅", "酉" to "卯", "戌" to "辰", "亥" to "巳")
    private fun getYupo(yueZhi: String): String = CHONG_MAP[yueZhi] ?: ""

    private data class Yao(
        var pos: Int, var gan: String = "", var zhi: String = "", var wx: String = "",
        var mark: String = "", var liuqin: String = "", var isShi: Boolean = false,
        var isYing: Boolean = false, var isDong: Boolean = false,
        var liushen: String = "", var xunkong: Boolean = false, var yupo: Boolean = false,
        var bianZhi: String = "", var bianWx: String = "", var bianQin: String = "",
        var huishou: String = "", var jintui: String = "", var huamu: String = "",
        var wangshuai: List<String> = emptyList()
    )

    data class CastResult(val text: String, val name: String, val gong: String)

    /** 装卦主函数 */
    fun zhuangGua(xiaName: String, shangName: String, dongYaos: List<Int>, dt: LocalDateTime?): String {
        val fullname = fullName(xiaName, shangName)
        val info = GUA64[fullname] ?: return "错误: 无法找到卦 $xiaName/$shangName"
        val gongWx = JING_GUA[info.gong]!!.wx

        val xiaGua = JING_GUA[xiaName]!!
        val shangGua = JING_GUA[shangName]!!
        val yaos = mutableListOf<Yao>()
        for (i in 0..2) {
            val zhi = xiaGua.neiZhi[i]; val wx = WUXING_ZHI[zhi]!!
            yaos.add(Yao(i + 1, xiaGua.neiGan, zhi, wx,
                if (xiaGua.hua[i] == '1') "—" else "- -", liuQin(gongWx, wx),
                isShi = i + 1 == info.shi, isYing = i + 1 == info.ying, isDong = i + 1 in dongYaos))
        }
        for (i in 0..2) {
            val zhi = shangGua.waiZhi[i]; val wx = WUXING_ZHI[zhi]!!
            yaos.add(Yao(i + 4, shangGua.waiGan, zhi, wx,
                if (shangGua.hua[i] == '1') "—" else "- -", liuQin(gongWx, wx),
                isShi = i + 4 == info.shi, isYing = i + 4 == info.ying, isDong = i + 4 in dongYaos))
        }

        // 变卦
        var bianName: String? = null
        var bianYaos: List<Yao>? = null
        if (dongYaos.isNotEmpty()) {
            val hua = (xiaGua.hua + shangGua.hua).toCharArray()
            dongYaos.forEach { hua[it - 1] = if (hua[it - 1] == '1') '0' else '1' }
            val bx = HUA_TO_NAME[hua.take(3).joinToString("")]
            val bs = HUA_TO_NAME[hua.drop(3).joinToString("")]
            if (bx != null && bs != null) {
                bianName = fullName(bx, bs)
                val bxg = JING_GUA[bx]!!; val bsg = JING_GUA[bs]!!
                val bl = mutableListOf<Yao>()
                for (i in 0..2) { val z = bxg.neiZhi[i]; val w = WUXING_ZHI[z]!!; bl.add(Yao(i + 1, bxg.neiGan, z, w, liuqin = liuQin(gongWx, w))) }
                for (i in 0..2) { val z = bsg.waiZhi[i]; val w = WUXING_ZHI[z]!!; bl.add(Yao(i + 4, bsg.waiGan, z, w, liuqin = liuQin(gongWx, w))) }
                bianYaos = bl
            }
        }

        // 日期相关
        var nianGz = ""; var yueGz = ""; var riGz = ""; var shiGz = ""
        var riGan = "甲"; var riZhi = "子"; var yueZhi = "子"
        if (dt != null) {
            val gz = dateToGanzhi(dt)
            nianGz = gz.nian; yueGz = gz.yue; riGz = gz.ri; shiGz = gz.shi
            riGan = riGz[0].toString(); riZhi = riGz[1].toString(); yueZhi = yueGz[1].toString()
        }
        val xunkong = if (dt != null && riGz.length >= 2) getXunkong(riGz) else ("" to "")
        val yupo = if (dt != null) getYupo(yueZhi) else ""

        val lsStart = liushenStart(riGan)
        yaos.forEachIndexed { idx, yao ->
            yao.liushen = LIUSHEN[(lsStart + idx) % 6]
            yao.xunkong = yao.zhi in listOf(xunkong.first, xunkong.second)
            yao.yupo = yao.zhi == yupo
        }

        // 伏神
        val fushen = findFushen(yaos, info.gong, gongWx)

        // 动爻变爻关系
        if (bianYaos != null) {
            for (yao in yaos) {
                if (yao.isDong) {
                    val by = bianYaos[yao.pos - 1]
                    yao.bianZhi = by.zhi; yao.bianWx = by.wx; yao.bianQin = by.liuqin
                    if (SHENG[by.wx] == yao.wx) yao.huishou = "回头生"
                    else if (KE[by.wx] == yao.wx) yao.huishou = "回头克"
                    val yi = zhiIdx(yao.zhi); val bi = zhiIdx(by.zhi)
                    if (yao.wx == by.wx && bi != yi) {
                        yao.jintui = if ((bi - yi).mod(12) <= 6) "化进神" else "化退神"
                    }
                    if (by.zhi == MU_MAP[yao.wx]) yao.huamu = "化墓"
                }
            }
        }

        // 月建旺衰
        val yueWx = WUXING_ZHI[yueZhi] ?: ""
        val riWx = WUXING_ZHI[riZhi] ?: ""
        for (yao in yaos) {
            val states = mutableListOf<String>()
            if (yueWx.isNotEmpty()) {
                when {
                    yao.wx == yueWx -> states.add("月旺")
                    SHENG[yueWx] == yao.wx -> states.add("月相")
                    SHENG[yao.wx] == yueWx -> states.add("月休")
                    KE[yueWx] == yao.wx -> states.add("月囚")
                    KE[yao.wx] == yueWx -> states.add("月死")
                }
            }
            if (riWx.isNotEmpty()) {
                when {
                    yao.wx == riWx -> states.add("日旺")
                    SHENG[riWx] == yao.wx -> states.add("日生")
                    KE[riWx] == yao.wx -> states.add("日克")
                }
            }
            yao.wangshuai = states
        }

        return formatOutput(fullname, info, gongWx, yaos, dongYaos, bianName, bianYaos,
            fushen, nianGz, yueGz, riGz, shiGz, xunkong, yupo)
    }

    private val MU_MAP = mapOf("木" to "未", "火" to "戌", "金" to "丑", "水" to "辰", "土" to "辰")

    private data class FuShen(val qin: String, val pos: Int, val gan: String, val zhi: String, val wx: String)
    private fun findFushen(yaos: List<Yao>, gongName: String, gongWx: String): List<FuShen> {
        val existing = yaos.map { it.liuqin }.toSet()
        val missing = setOf("父母", "兄弟", "子孙", "妻财", "官鬼") - existing
        if (missing.isEmpty()) return emptyList()
        val g = JING_GUA[gongName]!!
        val ben = mutableListOf<Triple<Int, String, String>>()
        for (i in 0..2) ben.add(Triple(i + 1, g.neiGan, g.neiZhi[i]))
        for (i in 0..2) ben.add(Triple(i + 4, g.waiGan, g.waiZhi[i]))
        return missing.mapNotNull { qin ->
            ben.firstOrNull { liuQin(gongWx, WUXING_ZHI[it.third]!!) == qin }?.let { t ->
                FuShen(qin, t.first, t.second, t.third, WUXING_ZHI[t.third]!!)
            }
        }
    }

    private fun formatOutput(
        name: String, info: GuaInfo, gongWx: String, yaos: List<Yao>, dong: List<Int>,
        bianName: String?, bianYaos: List<Yao>?, fushen: List<FuShen>,
        nianGz: String, yueGz: String, riGz: String, shiGz: String,
        xunkong: Pair<String, String>, yupo: String
    ): String {
        val L = mutableListOf<String>()
        L.add("═".repeat(30))
        L.add("  ${info.gong}宫: $name")
        if (bianName != null) L.add("  变卦: $bianName")
        L.add("  卦宫五行: $gongWx")
        if (riGz.isNotEmpty()) {
            L.add("  年:$nianGz 月:$yueGz 日:$riGz 时:$shiGz")
            L.add("  旬空: ${xunkong.first}${xunkong.second}  月破: $yupo")
        }
        L.add("═".repeat(30))
        val posNames = arrayOf("初", "二", "三", "四", "五", "上")
        for (i in 5 downTo 0) {
            val yao = yaos[i]
            var mark = yao.mark
            if (yao.isDong) mark += if ("—" in mark) " ○" else " ×"
            val sa = if (yao.isShi) "世" else if (yao.isYing) "应" else ""
            val dongMark = (if (yao.xunkong) "空" else "") + (if (yao.yupo) "破" else "")
            val ws = yao.wangshuai.joinToString(",")
            var line = "  ${yao.liushen.padEnd(4, '　')}${yao.liuqin.padEnd(4, '　')}${yao.gan}${yao.zhi}${yao.wx.padEnd(3, '　')}${mark.padEnd(6, '　')}${sa.padEnd(3, '　')}"
            if (yao.isDong && bianYaos != null) {
                val by = bianYaos[i]
                val extra = listOf(yao.huishou, yao.jintui, yao.huamu).filter { it.isNotEmpty() }.joinToString(" ")
                line += "  → ${by.gan}${by.zhi}${by.wx.padEnd(3, '　')}${by.liuqin.padEnd(4, '　')}$extra"
            }
            if (dongMark.isNotEmpty() || ws.isNotEmpty()) line += "  [$dongMark $ws]".trim()
            L.add("${posNames[i]}爻$line")
        }
        if (fushen.isNotEmpty()) {
            L.add("─".repeat(30))
            L.add("【伏神】")
            for (fs in fushen) {
                val fly = yaos[fs.pos - 1]
                L.add("  ${fs.qin}: ${fs.gan}${fs.zhi}${fs.wx} 伏于${fs.pos}爻(${fly.liuqin}${fly.gan}${fly.zhi})下")
            }
        }
        L.add("═".repeat(30))
        L.add("【技术标注·断卦底稿】")
        L.add("主卦: $name  宫: ${info.gong}($gongWx)")
        L.add("世爻: ${info.shi}爻  应爻: ${info.ying}爻")
        if (dong.isNotEmpty()) L.add("动爻: $dong")
        if (bianName != null) L.add("变卦: $bianName")
        if (riGz.isNotEmpty()) {
            L.add("月建: $yueGz(${WUXING_ZHI[yueGz[1].toString()] ?: ""})  日辰: $riGz(${WUXING_ZHI[riGz[1].toString()] ?: ""})")
        }
        val shiYao = yaos[info.shi - 1]; val yingYao = yaos[info.ying - 1]
        L.add("世爻: ${shiYao.liuqin}${shiYao.gan}${shiYao.zhi}(${shiYao.wx})")
        L.add("应爻: ${yingYao.liuqin}${yingYao.gan}${yingYao.zhi}(${yingYao.wx})")
        L.add("─ 爻旺衰 ─")
        for (yao in yaos) {
            val ws = yao.wangshuai.joinToString(",").ifEmpty { "平" }
            val xk = if (yao.xunkong) " 旬空" else ""
            val yp = if (yao.yupo) " 月破" else ""
            L.add("  ${yao.pos}爻 ${yao.liuqin}${yao.gan}${yao.zhi}(${yao.wx}): $ws$xk$yp")
        }
        return L.joinToString("\n")
    }

    // ── 起卦方法 ──
    fun yaoToGua(list: List<Int>): Triple<String, String, List<Int>> {
        val hua = mutableListOf<Char>(); val dong = mutableListOf<Int>()
        list.forEachIndexed { i, y ->
            hua.add(if (y == 7 || y == 9) '1' else '0')
            if (y == 6 || y == 9) dong.add(i + 1)
        }
        val xia = HUA_TO_NAME[hua.take(3).joinToString("")] ?: ""
        val shang = HUA_TO_NAME[hua.drop(3).joinToString("")] ?: ""
        return Triple(xia, shang, dong)
    }

    fun numberQigua(n1: Int, n2: Int, n3: Int): Triple<String, String, List<Int>> {
        val xt = arrayOf("乾", "兑", "离", "震", "巽", "坎", "艮", "坤")
        return Triple(xt[(n1 - 1).mod(8)], xt[(n2 - 1).mod(8)], listOf((n3 - 1).mod(6) + 1))
    }

    fun nameToGua(name: String): Pair<String?, String?> {
        GUA64[name]?.let { return it.xia to it.shang }
        val hit = GUA64.keys.firstOrNull { name in it }
        return hit?.let { GUA64[it]!!.xia to GUA64[it]!!.shang } ?: (null to null)
    }

    fun allGuaNames(): List<String> = GUA64.keys.toList()

    /** 铜钱摇卦 → 完整卦面 + 每次三钱背面数（供动画） */
    fun castByCoin(now: LocalDateTime, random: kotlin.random.Random): Pair<String, List<Int>> {
        val tosses = mutableListOf<Int>(); val yaos = mutableListOf<Int>()
        repeat(6) {
            val backs = (0..2).count { random.nextBoolean() }
            tosses.add(backs)
            yaos.add(when (backs) { 3 -> 9; 2 -> 8; 1 -> 7; else -> 6 })
        }
        val (xia, shang, dong) = yaoToGua(yaos)
        return zhuangGua(xia, shang, dong, now) to tosses
    }

    fun castByTime(now: LocalDateTime): String {
        val gz = dateToGanzhi(now)
        fun zi(g: String) = zhiIdx(g[1].toString()) + 1
        val y = zi(gz.nian); val m = zi(gz.yue); val d = zi(gz.ri); val h = zi(gz.shi)
        val xt = arrayOf("乾", "兑", "离", "震", "巽", "坎", "艮", "坤")
        val xia = xt[(y + m + d - 1).mod(8)]
        val shang = xt[(y + m + d + h - 1).mod(8)]
        val dong = (y + m + d + h - 1).mod(6) + 1
        return zhuangGua(xia, shang, listOf(dong), now)
    }

    fun castByNumbers(n1: Int, n2: Int, n3: Int, now: LocalDateTime): String {
        val (xia, shang, dong) = numberQigua(n1, n2, n3)
        return zhuangGua(xia, shang, dong, now)
    }
}
