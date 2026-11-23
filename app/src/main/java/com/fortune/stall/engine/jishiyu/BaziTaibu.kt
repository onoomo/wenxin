package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.util.SolarTerm
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 八字 · taibu 传统口径（独立引擎）。
 *
 * 移植自开源项目「太卜 taibu」packages/core/src/domains/bazi（MIT 许可），
 * 规则版本 bazi-2026.10 / bazi_dayun。四柱历法口径与 lunar.js EightChar 一致：
 * 年柱立春换年、月柱十二节换月（五虎遁）、日柱公历干支、时柱五鼠遁、晚子时次日干起时。
 *
 * 输出规范对齐 taibu：四柱（十神/藏干/纳音/地势/空亡/神煞）、天干五合/冲克、
 * 地支六合/半合/三合/三会/冲/刑/害/破、胎元/命宫、真太阳时信息、
 * 大运（十神/纳音/地势/神煞/支关系）+ 小运 + 流年 + 太岁。
 */
object BaziTaibu {

    val GAN = LunarCalendar.TIANGAN
    val ZHI = LunarCalendar.DIZHI

    // ===== 基础映射 =====
    val GAN_WX = mapOf("甲" to "木", "乙" to "木", "丙" to "火", "丁" to "火", "戊" to "土",
        "己" to "土", "庚" to "金", "辛" to "金", "壬" to "水", "癸" to "水")
    val ZHI_WX = mapOf("子" to "水", "丑" to "土", "寅" to "木", "卯" to "木", "辰" to "土", "巳" to "火",
        "午" to "火", "未" to "土", "申" to "金", "酉" to "金", "戌" to "土", "亥" to "水")
    private fun yang(g: String) = g in setOf("甲", "丙", "戊", "庚", "壬")
    private fun wxOrder(w: String) = listOf("木", "火", "土", "金", "水").indexOf(w)

    /** 五行关系：same/produce/produced/control/controlled */
    private fun elementRelation(from: String, to: String): String {
        if (from == to) return "same"
        val fi = wxOrder(from); val ti = wxOrder(to)
        if ((fi + 1) % 5 == ti) return "produce"
        if ((ti + 1) % 5 == fi) return "produced"
        if ((fi + 2) % 5 == ti) return "control"
        return "controlled"
    }

    /** 十神（相对日主） */
    fun tenGod(dayStem: String, target: String): String {
        if (dayStem == target) return "比肩"
        val rel = elementRelation(GAN_WX[dayStem] ?: "", GAN_WX[target] ?: "")
        val same = yang(dayStem) == yang(target)
        val map = mapOf(
            "same" to listOf("比肩", "劫财"),
            "produce" to listOf("食神", "伤官"),
            "control" to listOf("偏财", "正财"),
            "controlled" to listOf("七杀", "正官"),
            "produced" to listOf("偏印", "正印")
        )
        return map[rel]?.get(if (same) 0 else 1) ?: ""
    }

    // ===== 纳音 =====
    private val NAYIN = mapOf(
        "甲子" to "海中金", "乙丑" to "海中金", "丙寅" to "炉中火", "丁卯" to "炉中火",
        "戊辰" to "大林木", "己巳" to "大林木", "庚午" to "路旁土", "辛未" to "路旁土",
        "壬申" to "剑锋金", "癸酉" to "剑锋金", "甲戌" to "山头火", "乙亥" to "山头火",
        "丙子" to "涧下水", "丁丑" to "涧下水", "戊寅" to "城头土", "己卯" to "城头土",
        "庚辰" to "白蜡金", "辛巳" to "白蜡金", "壬午" to "杨柳木", "癸未" to "杨柳木",
        "甲申" to "泉中水", "乙酉" to "泉中水", "丙戌" to "屋上土", "丁亥" to "屋上土",
        "戊子" to "霹雳火", "己丑" to "霹雳火", "庚寅" to "松柏木", "辛卯" to "松柏木",
        "壬辰" to "长流水", "癸巳" to "长流水", "甲午" to "砂中金", "乙未" to "砂中金",
        "丙申" to "山下火", "丁酉" to "山下火", "戊戌" to "平地木", "己亥" to "平地木",
        "庚子" to "壁上土", "辛丑" to "壁上土", "壬寅" to "金箔金", "癸卯" to "金箔金",
        "甲辰" to "覆灯火", "乙巳" to "覆灯火", "丙午" to "天河水", "丁未" to "天河水",
        "戊申" to "大驿土", "己酉" to "大驿土", "庚戌" to "钗钏金", "辛亥" to "钗钏金",
        "壬子" to "桑柘木", "癸丑" to "桑柘木", "甲寅" to "大溪水", "乙卯" to "大溪水",
        "丙辰" to "沙中土", "丁巳" to "沙中土", "戊午" to "天上火", "己未" to "天上火",
        "庚申" to "石榴木", "辛酉" to "石榴木", "壬戌" to "大海水", "癸亥" to "大海水"
    )
    fun naYin(gz: String) = NAYIN[gz] ?: ""
    private fun naYinElement(gz: String): String {
        val n = naYin(gz)
        val last = n.lastOrNull()?.toString() ?: return ""
        return if (last in setOf("金", "木", "水", "火", "土")) last else ""
    }
    private fun getNaYinElementFromText(n: String) = n.lastOrNull()?.toString() ?: ""

    // ===== 藏干 =====
    private val HIDDEN = mapOf(
        "子" to listOf("癸"), "丑" to listOf("己", "癸", "辛"), "寅" to listOf("甲", "丙", "戊"),
        "卯" to listOf("乙"), "辰" to listOf("戊", "乙", "癸"), "巳" to listOf("丙", "庚", "戊"),
        "午" to listOf("丁", "己"), "未" to listOf("己", "丁", "乙"), "申" to listOf("庚", "壬", "戊"),
        "酉" to listOf("辛"), "戌" to listOf("戊", "辛", "丁"), "亥" to listOf("壬", "甲")
    )
    private val HIDDEN_QI = listOf("本气", "中气", "余气")
    data class HiddenStem(val stem: String, val qi: String, val tenGod: String)
    fun hiddenStems(branch: String, dayStem: String): List<HiddenStem> =
        (HIDDEN[branch] ?: emptyList()).mapIndexed { i, s -> HiddenStem(s, HIDDEN_QI.getOrNull(i) ?: "", tenGod(dayStem, s)) }

    // ===== 十二长生 =====
    private val DI_SHI_ORDER = listOf("长生", "沐浴", "冠带", "临官", "帝旺", "衰", "病", "死", "墓", "绝", "胎", "养")
    private val DI_SHI_START = mapOf("甲" to "亥", "乙" to "午", "丙" to "寅", "丁" to "酉", "戊" to "寅",
        "己" to "酉", "庚" to "巳", "辛" to "子", "壬" to "申", "癸" to "卯")
    fun diShi(dayStem: String, branch: String): String {
        val s = DI_SHI_START[dayStem] ?: return ""
        val si = ZHI.indexOf(s); val bi = ZHI.indexOf(branch)
        if (si < 0 || bi < 0) return ""
        val off = if (yang(dayStem)) (bi - si + 12) % 12 else (si - bi + 12) % 12
        return DI_SHI_ORDER.getOrNull(off) ?: ""
    }

    // ===== 空亡 =====
    private val XUN_KONG = mapOf("甲子旬" to listOf("戌", "亥"), "甲戌旬" to listOf("申", "酉"),
        "甲申旬" to listOf("午", "未"), "甲午旬" to listOf("辰", "巳"),
        "甲辰旬" to listOf("寅", "卯"), "甲寅旬" to listOf("子", "丑"))
    data class KongWang(val xun: String, val kongZhi: List<String>)
    fun kongWang(dayGan: String, dayZhi: String): KongWang {
        val gi = GAN.indexOf(dayGan); val zi = ZHI.indexOf(dayZhi)
        if (gi < 0 || zi < 0) return KongWang("甲子旬", XUN_KONG["甲子旬"]!!)
        val start = (zi - gi + 12) % 12
        val names = listOf("甲子旬", "甲戌旬", "甲申旬", "甲午旬", "甲辰旬", "甲寅旬")
        val starts = listOf("子", "戌", "申", "午", "辰", "寅")
        val idx = starts.indexOf(ZHI[start]).let { if (it < 0) 0 else it }
        return KongWang(names[idx], XUN_KONG[names[idx]]!!)
    }

    // ===== 干支关系数据表 =====
    private val LIU_HE = mapOf("子" to "丑", "丑" to "子", "寅" to "亥", "亥" to "寅", "卯" to "戌", "戌" to "卯",
        "辰" to "酉", "酉" to "辰", "巳" to "申", "申" to "巳", "午" to "未", "未" to "午")
    private val LIU_HE_HUA = mapOf("子" to "土", "丑" to "土", "寅" to "木", "亥" to "木", "卯" to "火", "戌" to "火",
        "辰" to "金", "酉" to "金", "巳" to "水", "申" to "水", "午" to "土", "未" to "土")
    private val SAN_HE = listOf(listOf("申", "子", "辰") to "水局", listOf("巳", "酉", "丑") to "金局",
        listOf("寅", "午", "戌") to "火局", listOf("亥", "卯", "未") to "木局")
    private val LIU_CHONG = mapOf("子" to "午", "午" to "子", "丑" to "未", "未" to "丑", "寅" to "申", "申" to "寅",
        "卯" to "酉", "酉" to "卯", "辰" to "戌", "戌" to "辰", "巳" to "亥", "亥" to "巳")
    private val XIANG_HAI = mapOf("子" to "未", "未" to "子", "丑" to "午", "午" to "丑", "寅" to "巳", "巳" to "寅",
        "卯" to "辰", "辰" to "卯", "申" to "亥", "亥" to "申", "酉" to "戌", "戌" to "酉")
    private val XIANG_PO = mapOf("子" to "酉", "酉" to "子", "丑" to "辰", "辰" to "丑", "寅" to "亥", "亥" to "寅",
        "卯" to "午", "午" to "卯", "巳" to "申", "申" to "巳", "未" to "戌", "戌" to "未")
    private val XIANG_XING = listOf(listOf("寅", "巳", "申") to "无恩之刑", listOf("丑", "戌", "未") to "恃势之刑",
        listOf("子", "卯") to "无礼之刑", listOf("辰") to "辰自刑", listOf("午") to "午自刑",
        listOf("酉") to "酉自刑", listOf("亥") to "亥自刑")
    private val SAN_HUI = listOf(listOf("寅", "卯", "辰") to "木", listOf("巳", "午", "未") to "火",
        listOf("申", "酉", "戌") to "金", listOf("亥", "子", "丑") to "水")
    private val BAN_HE = listOf(
        Triple("申", "子", "辰") to "水", Triple("子", "辰", "申") to "水",
        Triple("亥", "卯", "未") to "木", Triple("卯", "未", "亥") to "木",
        Triple("寅", "午", "戌") to "火", Triple("午", "戌", "寅") to "火",
        Triple("巳", "酉", "丑") to "金", Triple("酉", "丑", "巳") to "金")
    private val WU_HE = mapOf("甲己" to "土", "己甲" to "土", "乙庚" to "金", "庚乙" to "金", "丙辛" to "水",
        "辛丙" to "水", "丁壬" to "木", "壬丁" to "木", "戊癸" to "火", "癸戊" to "火")
    private val GAN_CHONG = listOf("甲" to "庚", "乙" to "辛", "丙" to "壬", "丁" to "癸")

    /** 关系（原局四柱之间） */
    data class Relation(val type: String, val pillars: List<String>, val desc: String)
    fun pillarRelations(yZhi: String, mZhi: String, dZhi: String, hZhi: String?): List<Relation> {
        val bs = listOfNotNull(yZhi, mZhi, dZhi, hZhi)
        val names = listOf("年", "月", "日", "时")
        val out = mutableListOf<Relation>()
        for (i in bs.indices) for (j in i + 1 until bs.size)
            if (LIU_HE[bs[i]] == bs[j]) out += Relation("合", listOf(names[i], names[j]),
                "${bs[i]}${bs[j]}六合化${LIU_HE_HUA[bs[i]] ?: ""}")
        for ((br, el) in SAN_HE) {
            val u = bs.filter { it in br }.distinct()
            if (u.size == 3) out += Relation("合", bs.mapIndexed { i, b -> if (b in br) names[i] else null }.filterNotNull(), "${u.joinToString("")}三合$el")
        }
        for (i in bs.indices) for (j in i + 1 until bs.size)
            if (LIU_CHONG[bs[i]] == bs[j]) out += Relation("冲", listOf(names[i], names[j]), "${bs[i]}${bs[j]}相冲")
        for (i in bs.indices) for (j in i + 1 until bs.size)
            if (XIANG_HAI[bs[i]] == bs[j]) out += Relation("害", listOf(names[i], names[j]), "${bs[i]}${bs[j]}相害")
        for ((comb, nm) in XIANG_XING) {
            if (comb.size == 1) {
                if (bs.count { it == comb[0] } >= 2) out += Relation("刑", bs.mapIndexed { i, b -> if (b == comb[0]) names[i] else null }.filterNotNull(), nm)
            } else if (bs.filter { it in comb }.distinct().size >= 2) {
                out += Relation("刑", bs.mapIndexed { i, b -> if (b in comb) names[i] else null }.filterNotNull(), nm)
            }
        }
        for (i in bs.indices) for (j in i + 1 until bs.size)
            if (XIANG_PO[bs[i]] == bs[j]) out += Relation("破", listOf(names[i], names[j]), "${bs[i]}${bs[j]}相破")
        return out
    }
    /** 三会（方局） */
    fun sanHui(yZhi: String, mZhi: String, dZhi: String, hZhi: String?): List<Relation> {
        val bs = listOfNotNull(yZhi, mZhi, dZhi, hZhi); val names = listOf("年", "月", "日", "时")
        val out = mutableListOf<Relation>()
        for ((br, el) in SAN_HUI) {
            val u = bs.filter { it in br }.distinct()
            if (u.size == 3) out += Relation("合", bs.mapIndexed { i, b -> if (b in br) names[i] else null }.filterNotNull(), "${u.joinToString("")}三会${el}方")
        }
        return out
    }
    /** 半合 */
    fun banHe(yZhi: String, mZhi: String, dZhi: String, hZhi: String?): List<Relation> {
        val bs = listOfNotNull(yZhi, mZhi, dZhi, hZhi); val names = listOf("年", "月", "日", "时")
        val out = mutableListOf<Relation>(); val seen = mutableSetOf<String>()
        for ((t, el) in BAN_HE) {
            if (bs.contains(t.first) && bs.contains(t.second) && !bs.contains(t.third)) {
                val key = listOf(t.first, t.second).sorted().joinToString("") + el
                if (key in seen) continue; seen += key
                val pos = bs.mapIndexed { i, b -> if (b == t.first || b == t.second) names[i] else null }.filterNotNull()
                out += Relation("半合", pos, "${t.first}${t.second}半合$el（缺${t.third}）")
            }
        }
        return out
    }
    /** 天干五合 */
    fun ganWuHe(yG: String, mG: String, dG: String, hG: String?): List<Relation> {
        val gs = listOfNotNull(yG, mG, dG, hG); val names = listOf("年", "月", "日", "时")
        val out = mutableListOf<Relation>()
        for (i in gs.indices) for (j in i + 1 until gs.size) {
            val el = WU_HE[gs[i] + gs[j]]
            if (el != null) out += Relation("干合", listOf(names[i], names[j]), "${gs[i]}${gs[j]}合化$el")
        }
        return out
    }
    /** 天干冲克 */
    fun ganChong(yG: String, mG: String, dG: String, hG: String?): List<Relation> {
        val gs = listOfNotNull(yG, mG, dG, hG); val names = listOf("年", "月", "日", "时")
        val out = mutableListOf<Relation>()
        for (i in gs.indices) for (j in i + 1 until gs.size)
            if (GAN_CHONG.any { (a, b) -> (gs[i] == a && gs[j] == b) || (gs[i] == b && gs[j] == a) })
                out += Relation("干冲", listOf(names[i], names[j]), "${gs[i]}${gs[j]}相冲")
        return out
    }

    // ===== 神煞数据表 =====
    private val TIAN_YI_GUI_REN = mapOf("甲" to listOf("丑", "未"), "乙" to listOf("子", "申"), "丙" to listOf("亥", "酉"),
        "丁" to listOf("亥", "酉"), "戊" to listOf("丑", "未"), "己" to listOf("子", "申"), "庚" to listOf("丑", "未"),
        "辛" to listOf("寅", "午"), "壬" to listOf("卯", "巳"), "癸" to listOf("卯", "巳"))
    private val TAI_JI_GUI_REN = mapOf("甲" to listOf("子", "午"), "乙" to listOf("子", "午"), "丙" to listOf("卯", "酉"),
        "丁" to listOf("卯", "酉"), "戊" to listOf("辰", "戌", "丑", "未"), "己" to listOf("辰", "戌", "丑", "未"),
        "庚" to listOf("寅", "亥"), "辛" to listOf("寅", "亥"), "壬" to listOf("巳", "申"), "癸" to listOf("巳", "申"))
    private val YANG_REN = mapOf("甲" to "卯", "乙" to "辰", "丙" to "午", "丁" to "未", "戊" to "午", "己" to "未",
        "庚" to "酉", "辛" to "戌", "壬" to "子", "癸" to "丑")
    private val WEN_CHANG = mapOf("甲" to "巳", "乙" to "午", "丙" to "申", "丁" to "酉", "戊" to "申", "己" to "酉",
        "庚" to "亥", "辛" to "子", "壬" to "寅", "癸" to "卯")
    private val YI_MA = mapOf("寅" to "申", "午" to "申", "戌" to "申", "申" to "寅", "子" to "寅", "辰" to "寅",
        "巳" to "亥", "酉" to "亥", "丑" to "亥", "亥" to "巳", "卯" to "巳", "未" to "巳")
    private val TAO_HUA = mapOf("寅" to "卯", "午" to "卯", "戌" to "卯", "申" to "酉", "子" to "酉", "辰" to "酉",
        "巳" to "午", "酉" to "午", "丑" to "午", "亥" to "子", "卯" to "子", "未" to "子")
    private val HUA_GAI = mapOf("寅" to "戌", "午" to "戌", "戌" to "戌", "申" to "辰", "子" to "辰", "辰" to "辰",
        "巳" to "丑", "酉" to "丑", "丑" to "丑", "亥" to "未", "卯" to "未", "未" to "未")
    private val LU_SHEN = mapOf("甲" to "寅", "乙" to "卯", "丙" to "巳", "丁" to "午", "戊" to "巳", "己" to "午",
        "庚" to "申", "辛" to "酉", "壬" to "亥", "癸" to "子")
    private val JIE_SHA = mapOf("寅" to "亥", "午" to "亥", "戌" to "亥", "申" to "巳", "子" to "巳", "辰" to "巳",
        "巳" to "寅", "酉" to "寅", "丑" to "寅", "亥" to "申", "卯" to "申", "未" to "申")
    private val WANG_SHEN = mapOf("寅" to "巳", "午" to "巳", "戌" to "巳", "申" to "亥", "子" to "亥", "辰" to "亥",
        "巳" to "申", "酉" to "申", "丑" to "申", "亥" to "寅", "卯" to "寅", "未" to "寅")
    private val JIANG_XING = mapOf("寅" to "午", "午" to "午", "戌" to "午", "申" to "子", "子" to "子", "辰" to "子",
        "巳" to "酉", "酉" to "酉", "丑" to "酉", "亥" to "卯", "卯" to "卯", "未" to "卯")
    private val ZAI_SHA = mapOf("寅" to "子", "午" to "子", "戌" to "子", "申" to "午", "子" to "午", "辰" to "午",
        "巳" to "卯", "酉" to "卯", "丑" to "卯", "亥" to "酉", "卯" to "酉", "未" to "酉")
    private val GU_CHEN = mapOf("寅" to "巳", "卯" to "巳", "辰" to "巳", "巳" to "申", "午" to "申", "未" to "申",
        "申" to "亥", "酉" to "亥", "戌" to "亥", "亥" to "寅", "子" to "寅", "丑" to "寅")
    private val GUA_SU = mapOf("寅" to "丑", "卯" to "丑", "辰" to "丑", "巳" to "辰", "午" to "辰", "未" to "辰",
        "申" to "未", "酉" to "未", "戌" to "未", "亥" to "戌", "子" to "戌", "丑" to "戌")
    private val HONG_LUAN = mapOf("子" to "卯", "丑" to "寅", "寅" to "丑", "卯" to "子", "辰" to "亥", "巳" to "戌",
        "午" to "酉", "未" to "申", "申" to "未", "酉" to "午", "戌" to "巳", "亥" to "辰")
    private val TIAN_XI = mapOf("子" to "酉", "丑" to "申", "寅" to "未", "卯" to "午", "辰" to "巳", "巳" to "辰",
        "午" to "卯", "未" to "寅", "申" to "丑", "酉" to "子", "戌" to "亥", "亥" to "戌")
    private val DIAO_KE = mapOf("子" to "戌", "丑" to "亥", "寅" to "子", "卯" to "丑", "辰" to "寅", "巳" to "卯",
        "午" to "辰", "未" to "巳", "申" to "午", "酉" to "未", "戌" to "申", "亥" to "酉")
    private val SANG_MEN = mapOf("子" to "寅", "丑" to "卯", "寅" to "辰", "卯" to "巳", "辰" to "午", "巳" to "未",
        "午" to "申", "未" to "酉", "申" to "戌", "酉" to "亥", "戌" to "子", "亥" to "丑")
    private val XUE_REN = mapOf("子" to "酉", "丑" to "戌", "寅" to "亥", "卯" to "子", "辰" to "丑", "巳" to "寅",
        "午" to "卯", "未" to "辰", "申" to "巳", "酉" to "午", "戌" to "未", "亥" to "申")
    private val PI_TOU = mapOf("子" to "巳", "丑" to "午", "寅" to "未", "卯" to "申", "辰" to "酉", "巳" to "戌",
        "午" to "亥", "未" to "子", "申" to "丑", "酉" to "寅", "戌" to "卯", "亥" to "辰")
    private val GOU_SHA = mapOf("子" to "酉", "丑" to "戌", "寅" to "亥", "卯" to "子", "辰" to "丑", "巳" to "寅",
        "午" to "卯", "未" to "辰", "申" to "巳", "酉" to "午", "戌" to "未", "亥" to "申")
    private val JIAO_SHA = mapOf("子" to "卯", "丑" to "寅", "寅" to "丑", "卯" to "子", "辰" to "亥", "巳" to "戌",
        "午" to "酉", "未" to "申", "申" to "未", "酉" to "午", "戌" to "巳", "亥" to "辰")
    private val BAI_HU = mapOf("寅" to "午", "卯" to "未", "辰" to "申", "巳" to "酉", "午" to "戌", "未" to "亥",
        "申" to "子", "酉" to "丑", "戌" to "寅", "亥" to "卯", "子" to "辰", "丑" to "巳")
    private val TIAN_YI = mapOf("寅" to "丑", "卯" to "寅", "辰" to "卯", "巳" to "辰", "午" to "巳", "未" to "午",
        "申" to "未", "酉" to "申", "戌" to "酉", "亥" to "戌", "子" to "亥", "丑" to "子")
    private val FU_XING = mapOf("甲" to "寅", "乙" to "丑", "丙" to "子", "丁" to "亥", "戊" to "申", "己" to "未",
        "庚" to "午", "辛" to "巳", "壬" to "辰", "癸" to "卯")
    private val GUO_YIN = mapOf("甲" to "戌", "乙" to "亥", "丙" to "丑", "丁" to "寅", "戊" to "丑", "己" to "寅",
        "庚" to "辰", "辛" to "巳", "壬" to "未", "癸" to "申")
    private val TIAN_CHU = mapOf("甲" to "巳", "乙" to "午", "丙" to "巳", "丁" to "午", "戊" to "巳", "己" to "午",
        "庚" to "亥", "辛" to "子", "壬" to "亥", "癸" to "子")
    private val CI_GUAN = mapOf("甲" to "寅", "乙" to "卯", "丙" to "巳", "丁" to "午", "戊" to "辰", "己" to "未",
        "庚" to "申", "辛" to "酉", "壬" to "亥", "癸" to "子")
    private val HONG_YAN = mapOf("甲" to "午", "乙" to "午", "丙" to "寅", "丁" to "未", "戊" to "辰", "己" to "辰",
        "庚" to "戌", "辛" to "酉", "壬" to "子", "癸" to "申")
    private val LIU_XIA = mapOf("甲" to "酉", "乙" to "戌", "丙" to "未", "丁" to "申", "戊" to "巳", "己" to "午",
        "庚" to "辰", "辛" to "卯", "壬" to "亥", "癸" to "寅")
    private val FEI_REN = mapOf("甲" to "酉", "乙" to "戌", "丙" to "子", "丁" to "丑", "戊" to "子", "己" to "丑",
        "庚" to "卯", "辛" to "辰", "壬" to "午", "癸" to "未")
    private val XUE_TANG = mapOf("金" to "巳", "木" to "亥", "水" to "申", "土" to "申", "火" to "寅")
    private val KUI_GANG = listOf("庚辰", "庚戌", "壬辰", "戊戌")
    private val YIN_CHA_YANG_CUO = listOf("丙子", "丁丑", "戊寅", "辛卯", "壬辰", "癸巳", "丙午", "丁未", "戊申", "辛酉", "壬戌", "癸亥")
    private val SHI_E_DA_BAI = listOf("甲辰", "乙巳", "壬申", "丙申", "丁亥", "庚辰", "戊戌", "癸亥", "辛巳", "己丑")
    private val BA_ZHUAN = listOf("甲寅", "乙卯", "丙午", "丁未", "戊戌", "戊辰", "己未", "己丑", "庚申", "辛酉", "壬子", "癸丑")
    private val JIN_SHEN = listOf("乙丑", "己巳", "癸酉")
    private val GU_LUAN = listOf("乙巳", "丁巳", "辛亥", "戊申", "甲寅", "丙午", "戊午", "壬子")
    private val SI_FEI = mapOf("寅" to listOf("庚申", "辛酉"), "卯" to listOf("庚申", "辛酉"), "辰" to listOf("庚申", "辛酉"),
        "巳" to listOf("壬子", "癸亥"), "午" to listOf("壬子", "癸亥"), "未" to listOf("壬子", "癸亥"),
        "申" to listOf("甲寅", "乙卯"), "酉" to listOf("甲寅", "乙卯"), "戌" to listOf("甲寅", "乙卯"),
        "亥" to listOf("丙午", "丁巳"), "子" to listOf("丙午", "丁巳"), "丑" to listOf("丙午", "丁巳"))
    private val SAN_QI = mapOf("天三奇" to listOf("甲", "戊", "庚"), "地三奇" to listOf("乙", "丙", "丁"), "人三奇" to listOf("壬", "癸", "辛"))

    private data class SsCtx(val yG: String, val yZ: String, val mG: String, val mZ: String, val dG: String, val dZ: String,
        val hG: String?, val hZ: String?, val kong: List<String>, val yearNyEl: String)

    /** 计算某地支落宫的神煞 */
    private fun branchShenSha(c: SsCtx, target: String, pos: String?): List<String> {
        val out = mutableListOf<String>()
        fun add(v: String) { if (v.isNotEmpty() && v !in out) out += v }
        fun mv(list: List<String>?, label: String) { if (list?.contains(target) == true) add(label) }
        fun mm(map: Map<String, String>, key: String?, label: String) { if (key != null && map[key] == target) add(label) }
        mv(TIAN_YI_GUI_REN[c.dG], "天乙贵人")
        mv(TAI_JI_GUI_REN[c.dG], "太极贵人"); mv(TAI_JI_GUI_REN[c.yG], "太极贵人")
        mm(LU_SHEN, c.dG, "禄神"); mm(YANG_REN, c.dG, "羊刃"); mm(WEN_CHANG, c.dG, "文昌")
        mm(TIAN_CHU, c.dG, "天厨"); mm(GUO_YIN, c.dG, "国印贵人"); mm(FU_XING, c.dG, "福星贵人")
        mm(LIU_XIA, c.dG, "流霞"); mm(HONG_YAN, c.dG, "红艳煞"); mm(FEI_REN, c.dG, "飞刃"); mm(CI_GUAN, c.dG, "词馆")
        mm(YI_MA, c.dZ, "驿马"); if (c.yZ != null && YI_MA[c.yZ] == target && "驿马" !in out) add("驿马")
        mm(TAO_HUA, c.dZ, "桃花"); if (c.yZ != null && TAO_HUA[c.yZ] == target && "桃花" !in out) add("桃花")
        mm(HUA_GAI, c.dZ, "华盖"); if (c.yZ != null && HUA_GAI[c.yZ] == target && "华盖" !in out) add("华盖")
        mm(JIANG_XING, c.dZ, "将星"); if (c.yZ != null && JIANG_XING[c.yZ] == target && "将星" !in out) add("将星")
        mm(JIE_SHA, c.dZ, "劫煞"); if (c.yZ != null && JIE_SHA[c.yZ] == target && "劫煞" !in out) add("劫煞")
        mm(WANG_SHEN, c.dZ, "亡神"); if (c.yZ != null && WANG_SHEN[c.yZ] == target && "亡神" !in out) add("亡神")
        mm(ZAI_SHA, c.dZ, "灾煞"); if (c.yZ != null && ZAI_SHA[c.yZ] == target && "灾煞" !in out) add("灾煞")
        if (c.yearNyEl.isNotEmpty() && XUE_TANG[c.yearNyEl] == target) add("学堂")
        mm(HONG_LUAN, c.yZ, "红鸾"); mm(TIAN_XI, c.yZ, "天喜"); mm(DIAO_KE, c.yZ, "吊客")
        mm(SANG_MEN, c.yZ, "丧门"); mm(PI_TOU, c.yZ, "披头"); mm(GOU_SHA, c.yZ, "勾煞"); mm(JIAO_SHA, c.yZ, "绞煞")
        mm(TIAN_YI, c.mZ, "天医"); mm(BAI_HU, c.mZ, "白虎")
        mm(XUE_REN, c.dZ, "血刃")
        if (GU_CHEN[c.yZ] == target) add("孤辰")
        if (GUA_SU[c.yZ] == target) add("寡宿")
        if (target in c.kong) add("空亡")
        if (pos != null) {
            val all = listOfNotNull(c.yZ, c.mZ, c.dZ, c.hZ)
            if ((target == "戌" && "亥" in all) || (target == "亥" && "戌" in all)) add("天罗")
            if ((target == "辰" && "巳" in all) || (target == "巳" && "辰" in all)) add("地网")
            if (pos == "日") {
                val dp = c.dG + c.dZ
                if (dp in KUI_GANG) add("魁罡"); if (dp in YIN_CHA_YANG_CUO) add("阴差阳错"); if (dp in SHI_E_DA_BAI) add("十恶大败")
                if (dp in BA_ZHUAN) add("八专"); if (dp in JIN_SHEN) add("金神"); if (dp in GU_LUAN) add("孤鸾煞")
                if (SI_FEI[c.mZ]?.contains(dp) == true) add("四废")
            }
            val stems = listOf(c.yG, c.mG, c.dG, c.hG)
            val posNames = listOf("年", "月", "日", "时")
            for ((nm, q) in SAN_QI) for (i in 0..stems.size - 3)
                if (stems[i] == q[0] && stems[i + 1] == q[1] && stems[i + 2] == q[2] && pos in listOf(posNames[i], posNames[i + 1], posNames[i + 2])) { add(nm); break }
        }
        return out
    }
    private fun globalShenSha(c: SsCtx): List<String> {
        val out = mutableListOf<String>()
        val dp = c.dG + c.dZ
        if (dp in KUI_GANG) out += "魁罡"; if (dp in YIN_CHA_YANG_CUO) out += "阴差阳错"; if (dp in SHI_E_DA_BAI) out += "十恶大败"
        if (dp in BA_ZHUAN) out += "八专"; if (dp in JIN_SHEN) out += "金神"; if (dp in GU_LUAN) out += "孤鸾煞"
        val all = listOfNotNull(c.yZ, c.mZ, c.dZ, c.hZ)
        if ("戌" in all && "亥" in all) out += "天罗"
        if ("辰" in all && "巳" in all) out += "地网"
        val stems = listOf(c.yG, c.mG, c.dG, c.hG)
        for ((nm, q) in SAN_QI) for (i in 0..stems.size - 3)
            if (stems[i] == q[0] && stems[i + 1] == q[1] && stems[i + 2] == q[2]) { out += nm; break }
        return out.distinct()
    }

    // ===== 输出数据类 =====
    data class Pillar(val name: String, val gan: String, val zhi: String, val ganZhi: String,
        val tenGod: String, val hidden: List<HiddenStem>, val naYin: String, val diShi: String,
        val kong: Boolean, val shenSha: List<String>)
    data class Liunian(val year: Int, val age: Int, val ganZhi: String, val tenGod: String,
        val naYin: String, val diShi: String, val shenSha: List<String>, val taiSui: List<String>,
        val relations: List<String>)
    data class Dayun(val ganZhi: String, val tenGod: String, val branchTenGod: String,
        val naYin: String, val diShi: String, val shenSha: List<String>, val relations: List<String>,
        val startYear: Int, val startAge: Int, val liunian: List<Liunian>)
    data class Result(
        val solarText: String, val lunarText: String, val gender: String,
        val pillars: List<Pillar>, val dayMaster: String,
        val kongWang: KongWang, val taiYuan: String, val mingGong: String, val taiXi: String, val shenGong: String,
        val globalShenSha: List<String>,
        val relations: List<Relation>, val banHe: List<Relation>, val sanHui: List<Relation>,
        val ganWuHe: List<Relation>, val ganChong: List<Relation>,
        val wuXing: Map<String, Int>, val wangShuai: List<String>,
        val dayun: List<Dayun>, val xiaoYun: List<String>, val qiYunText: String
    )

    private fun jz(idx: Int) = GAN[((idx % 10) + 10) % 10] + ZHI[((idx % 12) + 12) % 12]
    private fun jzIndex(gz: String) = JishiyuBase.jiaziIndex(gz).let { if (it < 0) 0 else it }
    private fun yearGanZhi(year: Int) = jz(year - 4)

    /** 太岁关系（流年支 vs 年支） */
    private fun taiSui(lnZhi: String, yearZhi: String): List<String> {
        val out = mutableListOf<String>()
        if (lnZhi == yearZhi) out += "值太岁"
        if (LIU_CHONG[yearZhi] == lnZhi) out += "冲太岁"
        if (LIU_HE[yearZhi] == lnZhi) out += "合太岁"
        if (XIANG_HAI[yearZhi] == lnZhi) out += "害太岁"
        if (XIANG_PO[yearZhi] == lnZhi) out += "破太岁"
        for ((comb, nm) in XIANG_XING) if (yearZhi in comb && lnZhi in comb && comb.size > 1) { out += "刑太岁($nm)"; break }
        return out
    }
    /** 大运/流年地支与原局地支的关系（简表） */
    private fun natalRelations(zhi: String, natal: List<String>): List<String> {
        val out = mutableListOf<String>()
        val labels = listOf("年", "月", "日", "时")
        for (i in natal.indices) {
            val nz = natal[i]
            if (LIU_HE[nz] == zhi) out += "${zhi}${nz}合${labels[i]}"
            if (LIU_CHONG[nz] == zhi) out += "${zhi}${nz}冲${labels[i]}"
            if (XIANG_HAI[nz] == zhi) out += "${zhi}${nz}害${labels[i]}"
            if (XIANG_PO[nz] == zhi) out += "${zhi}${nz}破${labels[i]}"
        }
        return out
    }

    /**
     * 主排盘。dayBoundary: "midnight"(默认, sect2) / "zi"(子时换日, sect1)。
     */
    fun paipan(dt: LocalDateTime, isMan: Boolean, dayBoundary: String = "midnight", longitude: Double? = null): Result {
        val lun = LunarCalendar.fromDateTime(dt)
        val gz = listOf(lun.yearGanZhi, lun.monthGanZhi, lun.dayGanZhi, lun.hourGanZhi)
        val yG = gz[0][0].toString(); val yZ = gz[0][1].toString()
        val mG = gz[1][0].toString(); val mZ = gz[1][1].toString()
        val dG = gz[2][0].toString(); val dZ = gz[2][1].toString()
        val hG = gz[3][0].toString(); val hZ = gz[3][1].toString()
        val kw = kongWang(dG, dZ)
        val yearNyEl = getNaYinElementFromText(naYin(gz[0]))
        val ctx = SsCtx(yG, yZ, mG, mZ, dG, dZ, hG, hZ, kw.kongZhi, yearNyEl)

        val pillars = listOf("年柱", "月柱", "日柱", "时柱").mapIndexed { i, nm ->
            val g = gz[i][0].toString(); val z = gz[i][1].toString()
            Pillar(nm, g, z, gz[i], if (i == 2) "日主" else tenGod(dG, g),
                hiddenStems(z, dG), naYin(gz[i]), diShi(dG, z), z in kw.kongZhi,
                branchShenSha(ctx, z, listOf("年", "月", "日", "时")[i]))
        }

        // 胎元：月干进一、月支进三；命宫：五虎遁；胎息/身宫（传统）
        val taiYuan = GAN[(GAN.indexOf(mG) + 1) % 10] + ZHI[(ZHI.indexOf(mZ) + 3) % 12]
        val mingGong = mingGong(yG, mZ, hZ)
        val hIdx = ZHI.indexOf(hZ)
        val taiXi = jz(jzIndex(gz[2]) + 120 - hIdx - 1) // 日干支顺数到时支前
        val shenGong = shenGong(mZ, hZ)

        // 五行统计（含藏干，本气权重2其余1 - 与现有风格接近）
        val wx = mutableMapOf("木" to 0, "火" to 0, "土" to 0, "金" to 0, "水" to 0)
        for (p in pillars) {
            GAN_WX[p.gan]?.let { wx[it] = (wx[it] ?: 0) + 1 }
            for (hs in p.hidden) GAN_WX[hs.stem]?.let { wx[it] = (wx[it] ?: 0) + 1 }
        }
        // 月令旺相休囚死
        val mwx = ZHI_WX[mZ] ?: "土"
        val order = listOf("木", "火", "土", "金", "水")
        val wang = mwx
        val xiang = order[(wxOrder(mwx) + 1) % 5]
        val xiu = order[(wxOrder(mwx) + 4) % 5]
        val qiu = order[(wxOrder(mwx) + 3) % 5]
        val si = order[(wxOrder(mwx) + 2) % 5]
        val wangShuai = listOf(wang, xiang, xiu, qiu, si)

        // 大运
        val dy = buildDayun(dt, isMan, dG, gz, ctx, listOf(yZ, mZ, dZ, hZ))

        return Result(
            solarText = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            lunarText = "农历${lun.month}月${lun.day}日",
            gender = if (isMan) "男" else "女",
            pillars = pillars, dayMaster = dG, kongWang = kw,
            taiYuan = taiYuan, mingGong = mingGong, taiXi = taiXi, shenGong = shenGong,
            globalShenSha = globalShenSha(ctx),
            relations = pillarRelations(yZ, mZ, dZ, hZ),
            banHe = banHe(yZ, mZ, dZ, hZ), sanHui = sanHui(yZ, mZ, dZ, hZ),
            ganWuHe = ganWuHe(yG, mG, dG, hG), ganChong = ganChong(yG, mG, dG, hG),
            wuXing = wx, wangShuai = wangShuai,
            dayun = dy.first, xiaoYun = dy.second, qiYunText = dy.third
        )
    }

    /** 命宫：从子宫起正月逆数到生月，再生月宫起生时顺数到卯 */
    private fun mingGong(yStem: String, mBranch: String, hBranch: String): String {
        val yinIdx = ZHI.indexOf("寅")
        val monthNum = (ZHI.indexOf(mBranch) - yinIdx + 12) % 12 + 1
        val monthPalace = (0 - (monthNum - 1) + 12) % 12
        val mingIdx = (monthPalace + 3 - ZHI.indexOf(hBranch) + 12) % 12
        val base = mapOf("甲" to 2, "己" to 2, "乙" to 4, "庚" to 4, "丙" to 6, "辛" to 6, "丁" to 8, "壬" to 8, "戊" to 0, "癸" to 0)[yStem] ?: return ZHI[mingIdx]
        val off = (mingIdx - yinIdx + 12) % 12
        return GAN[(base + off) % 10] + ZHI[mingIdx]
    }
    /** 身宫：月支起子时逆数到生时 */
    private fun shenGong(mBranch: String, hBranch: String): String {
        val idx = (ZHI.indexOf(mBranch) - ZHI.indexOf(hBranch) + 12) % 12
        return ZHI[idx] + "宫"
    }

    /** 大运：阳男阴女顺行，阴男阳女逆行；起运岁数由生日距最近节的天数/3 定 */
    private fun buildDayun(dt: LocalDateTime, isMan: Boolean, dayGan: String, gz: List<String>,
        ctx: SsCtx, natal: List<String>): Triple<List<Dayun>, List<String>, String> {
        val yearStem = gz[0][0].toString(); val monthGz = gz[1]; val monthIdx = JishiyuBase.jiaziIndex(monthGz)
        val yangYear = yang(yearStem)
        val forward = if (isMan) yangYear else !yangYear
        // 起运：找出生日所属的节（月首），算到下一节/上一节的天数差 /3
        val jieIdx = jieIndexFor(dt)
        val startDays = qiYunDays(dt, forward)
        val startAge = maxOf(1, (startDays / 3.0).let { kotlin.math.floor(it).toInt() }.let { if (it < 1) 1 else it })
        val birthYear = dt.year
        val startYear = birthYear + startAge

        val yiJieName = SolarTerm.NAMES[jieIdx]
        val qiYunText = "${yiJieName}起运 · 约${startAge}岁（${startYear}年）· ${if (forward) "顺行" else "逆行"}"

        val dayunList = mutableListOf<Dayun>()
        for (i in 0 until 8) {
            val dyIdx = ((monthIdx + (if (forward) i + 1 else -(i + 1))) % 60 + 60) % 60
            val dyGz = jz(dyIdx)
            val g = dyGz[0].toString(); val z = dyGz[1].toString()
            val tg = tenGod(dayGan, g)
            val mainStem = HIDDEN[z]?.firstOrNull() ?: ""
            val bTenGod = if (mainStem.isEmpty()) "" else tenGod(dayGan, mainStem)
            val sYear = startYear + i * 10
            val age = startAge + i * 10
            val liunian = (0 until 10).map { k ->
                val ly = sYear + k
                val lg = yearGanZhi(ly)
                val lgan = lg[0].toString(); val lzhi = lg[1].toString()
                val ss = branchShenSha(ctx, lzhi, null)
                Liunian(ly, age + k, lg, tenGod(dayGan, lgan), naYin(lg), diShi(dayGan, lzhi), ss,
                    taiSui(lzhi, ctx.yZ), natalRelations(lzhi, natal) + natalRelations(lzhi, listOf(z)).map { "$it(大运)" })
            }
            dayunList += Dayun(dyGz, tg, bTenGod, naYin(dyGz), diShi(dayGan, z),
                branchShenSha(ctx, z, null), natalRelations(z, natal), sYear, age, liunian)
        }
        // 小运（由时柱起，阳男阴女顺行）
        val hourIdx = JishiyuBase.jiaziIndex(gz[3]).let { if (it < 0) 0 else it }
        val xiaoYun = (1..startAge.coerceAtMost(10)).map { k ->
            jz(hourIdx + (if (forward) k else -k))
        }
        return Triple(dayunList, xiaoYun, qiYunText)
    }

    /** 生日所属月首节（寅月=立春 index2 ... 丑月=小寒 index0/1） */
    private fun jieIndexFor(dt: LocalDateTime): Int {
        // SolarTerm.NAMES 顺序：小寒,大寒,立春,...；每月第一个"节"索引 = (month-1)*2
        // 通过 rangeAt 找当前节气区间
        val range = SolarTerm.rangeAt(dt)
        return range?.let { SolarTerm.NAMES.indexOf(it.from.name) }.let { if (it == null || it < 0) (dt.monthValue - 1) * 2 else it }
    }

    /** 起运天数：生日到顺/逆方向最近节的实际日差 */
    private fun qiYunDays(dt: LocalDateTime, forward: Boolean): Int {
        val cur = jieIndexFor(dt)
        val targetIdx = if (forward) (cur + 2) % 24 else (cur - 2 + 24) % 24
        val year = if (!forward && cur < 2) dt.year - 1 else dt.year
        val t = SolarTerm.termTime(year, targetIdx) ?: SolarTerm.termTime(year + 1, targetIdx) ?: return 9
        val days = java.time.temporal.ChronoUnit.DAYS.between(dt.toLocalDate(), t.toLocalDate())
        return kotlin.math.abs(days).toInt().coerceIn(0, 30)
    }
}
