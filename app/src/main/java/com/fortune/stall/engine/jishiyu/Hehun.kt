package com.fortune.stall.engine.jishiyu

import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * 合婚 —— 1:1 移植自吉时雨 hehun.js (AGPL-3.0, xianbo.chen@gmail.com)
 * 依赖 BaziAnalyzer 的 analyze 结果与公共查询表，共 12 条规则。
 */
object Hehun {
    private val WX_SHENG = JishiyuBase.WX_SHENG
    private val WX_KE = JishiyuBase.WX_KE_MAP

    /** 四柱：年/月/日/时 干支 */
    data class Bazi(
        val yearGan: String, val yearZhi: String,
        val monthGan: String, val monthZhi: String,
        val dayGan: String, val dayZhi: String,
        val timeGan: String, val timeZhi: String
    ) {
        fun toArray() = listOf(yearGan + yearZhi, monthGan + monthZhi, dayGan + dayZhi, timeGan + timeZhi)
        fun gans() = listOf(yearGan, monthGan, dayGan, timeGan)
        fun zhis() = listOf(yearZhi, monthZhi, dayZhi, timeZhi)
        companion object {
            fun from(arr: List<String>): Bazi {
                fun g(i: Int) = arr[i][0].toString()
                fun z(i: Int) = arr[i][1].toString()
                return Bazi(g(0), z(0), g(1), z(1), g(2), z(2), g(3), z(3))
            }
        }
    }

    data class RuleResult(
        val name: String, val desc: String, val detail: String,
        val conflicts: List<String> = emptyList(),
        val pass: Boolean, val passDesc: String, val implemented: Boolean = true
    )
    data class Grade(val grade: String, val desc: String)
    data class HehunResult(
        val rules: List<RuleResult>, val passCount: Int, val totalRules: Int, val grade: Grade
    )

    // ===== 基础映射 =====
    private val CONG_MAP = mapOf(
        "子" to "午", "午" to "子", "丑" to "未", "未" to "丑", "寅" to "申", "申" to "寅",
        "卯" to "酉", "酉" to "卯", "辰" to "戌", "戌" to "辰", "巳" to "亥", "亥" to "巳")
    private val HAI_MAP = mapOf(
        "寅" to "巳", "巳" to "寅", "卯" to "辰", "辰" to "卯", "丑" to "午", "午" to "丑",
        "子" to "未", "未" to "子", "酉" to "戌", "戌" to "酉", "申" to "亥", "亥" to "申")
    private val HE6_MAP = mapOf(
        "子" to "丑", "丑" to "子", "寅" to "亥", "亥" to "寅", "卯" to "戌", "戌" to "卯",
        "辰" to "酉", "酉" to "辰", "巳" to "申", "申" to "巳", "午" to "未", "未" to "午")
    private val HE5_MAP = mapOf(
        "甲" to "己", "己" to "甲", "乙" to "庚", "庚" to "乙", "丙" to "辛", "辛" to "丙",
        "丁" to "壬", "壬" to "丁", "戊" to "癸", "癸" to "戊")
    private val GANG_CONG_MAP = mapOf(
        "甲" to "庚", "庚" to "甲", "乙" to "辛", "辛" to "乙",
        "丙" to "壬", "壬" to "丙", "丁" to "癸", "癸" to "丁")
    private val XING3 = arrayOf("丑未", "未戌", "戌丑", "寅巳", "巳申", "申寅")
    private val XING2 = arrayOf("子卯")
    private val TAOHUA = mapOf(
        "申" to "酉", "子" to "酉", "辰" to "酉", "寅" to "卯", "午" to "卯", "戌" to "卯",
        "巳" to "午", "酉" to "午", "丑" to "午", "亥" to "子", "卯" to "子", "未" to "子")
    private val YINYANG_CHACUO = setOf(
        "丙子", "丁丑", "戊寅", "辛卯", "壬辰", "癸巳", "丙午", "丁未", "戊申", "辛酉", "壬戌", "癸亥")
    private val TIANYI = mapOf(
        "甲" to arrayOf("丑", "未"), "戊" to arrayOf("丑", "未"),
        "乙" to arrayOf("申", "子"), "己" to arrayOf("申", "子"),
        "丙" to arrayOf("亥", "酉"), "丁" to arrayOf("亥", "酉"),
        "壬" to arrayOf("卯", "巳"), "癸" to arrayOf("卯", "巳"),
        "庚" to arrayOf("午", "寅"), "辛" to arrayOf("午", "寅"))
    private val HONGYAN = mapOf(
        "甲" to "午", "乙" to "午", "丙" to "寅", "丁" to "未", "戊" to "辰",
        "己" to "辰", "庚" to "戌", "辛" to "酉", "壬" to "子", "癸" to "申")
    private val LIUXIA = mapOf(
        "甲" to "酉", "乙" to "戌", "丙" to "未", "丁" to "申", "戊" to "巳",
        "己" to "午", "庚" to "辰", "辛" to "卯", "壬" to "亥", "癸" to "寅")

    private fun canggan(zhi: String): Array<String> =
        JishiyuBase.CANGGAN[zhi] ?: emptyArray()

    private fun ganWx(gan: String) = JishiyuBase.GAN_WUXING[gan] ?: ""
    private fun zhiWx(zhi: String) = JishiyuBase.ZHI_WUXING[zhi] ?: ""

    private fun extractNayinWuxing(nayin: String): String {
        for (wx in arrayOf("金", "木", "水", "火", "土")) if (nayin.contains(wx)) return wx
        return ""
    }
    private fun wuxingRelation(wx1: String, wx2: String): String = when {
        wx1 == wx2 -> "比和"
        WX_SHENG[wx1] == wx2 -> "相生"
        WX_KE[wx1] == wx2 -> "我克"
        WX_SHENG[wx2] == wx1 -> "相生"
        WX_KE[wx2] == wx1 -> "克我"
        else -> ""
    }

    private fun hasXing(a: String, b: String): Boolean {
        val pair = a + b; val rev = b + a
        if (XING3.any { it == pair || it == rev }) return true
        if (XING2.any { it == pair || it == rev }) return true
        return a == b && a in setOf("辰", "午", "酉", "亥")
    }
    private fun hasChong(a: String, b: String) = CONG_MAP[a] == b
    private fun hasHai(a: String, b: String) = HAI_MAP[a] == b
    private fun hasXingChongHai(a: String, b: String) = hasXing(a, b) || hasChong(a, b) || hasHai(a, b)
    private fun has6he(a: String, b: String) = HE6_MAP[a] == b
    private fun has5he(a: String, b: String) = HE5_MAP[a] == b
    private fun hasGanChong(a: String, b: String) = GANG_CONG_MAP[a] == b
    private fun hasGanKe(a: String, b: String) = WX_KE[ganWx(a)] == ganWx(b)

    private fun pillarName(i: Int) = arrayOf("年柱", "月柱", "日柱", "时柱").getOrNull(i) ?: ""

    private fun countShishen(b: Bazi, name: String): Int {
        var count = 0
        val rigan = b.dayGan
        b.gans().forEachIndexed { i, g -> if (i != 2 && Shishen.query(g, rigan) == name) count++ }
        b.zhis().forEach { z -> canggan(z).forEach { cg -> if (Shishen.query(cg, rigan) == name) count++ } }
        return count
    }

    private data class Spouse(val type: String, val pos: String, val zhi: String = "", val gan: String = "")
    private fun findSpouseStar(b: Bazi, isMan: Boolean): List<Spouse> {
        val rigan = b.dayGan
        val target = if (isMan) "正财" else "正官"
        val found = mutableListOf<Spouse>()
        b.gans().forEachIndexed { i, g ->
            if (Shishen.query(g, rigan) == target) found.add(Spouse("天干", pillarName(i), gan = g))
        }
        b.zhis().forEachIndexed { j, z ->
            canggan(z).forEach { cg ->
                if (Shishen.query(cg, rigan) == target) found.add(Spouse("地支藏干", pillarName(j), z, cg))
            }
        }
        return found
    }

    private fun taohuaCount(b: Bazi): Int {
        var count = 0
        val nz = b.yearZhi; val rz = b.dayZhi
        val thRz = TAOHUA[rz]; val thNz = TAOHUA[nz]
        b.zhis().forEach { z ->
            if (thRz != null && z == thRz) count++
            if (thNz != null && nz != rz && z == thNz) count++
        }
        return count
    }

    private fun judgeGrade(pass: Int): Grade = when {
        pass <= 3 -> Grade("坚决不可", "0-3条：存在严重不合，应坚决避免")
        pass <= 6 -> Grade("下等婚", "4-6条：有离婚的可能")
        pass <= 9 -> Grade("中等婚", "7-9条：算是不错的婚姻组合")
        else -> Grade("上等婚", "10-12条：非常难得，一般命局难以达到")
    }

    private fun unique(list: List<String>): List<String> {
        val r = mutableListOf<String>(); list.forEach { if (it !in r) r.add(it) }; return r
    }

    // ===== 主入口 =====
    fun hehun(pillars1: List<String>, isMan1: Boolean, pillars2: List<String>, isMan2: Boolean): HehunResult {
        val b1 = Bazi.from(pillars1); val b2 = Bazi.from(pillars2)
        val man = if (isMan1) b1 else b2
        val woman = if (isMan1) b2 else b1
        val a1 = BaziAnalyzer.analyze(pillars1)
        val a2 = BaziAnalyzer.analyze(pillars2)

        val results = mutableListOf<RuleResult>()
        results.add(ruleShengxiao(b1, b2))
        results.add(ruleNayin(b1, b2))
        results.add(ruleHungong(b1, b2))
        results.add(ruleShizhu(b1, b2))
        results.add(ruleNianzhu(b1, b2))
        results.add(ruleYongshen(a1, a2))
        results.add(ruleShishen(man, woman))
        results.add(ruleTiaohou(a1, a2))
        results.add(ruleKongwang(b1, isMan1, b2, isMan2))
        results.add(ruleXingge(a1, a2))
        results.add(ruleTaohua(b1, b2))
        results.add(ruleShensha(b1, b2))

        val pass = results.count { it.pass }
        return HehunResult(results, pass, results.size, judgeGrade(pass))
    }

    // 规则1 生肖
    private fun ruleShengxiao(b1: Bazi, b2: Bazi): RuleResult {
        val z1 = b1.yearZhi; val z2 = b2.yearZhi
        val sx1 = JishiyuBase.queryShengxiao(z1); val sx2 = JishiyuBase.queryShengxiao(z2)
        val conflicts = mutableListOf<String>()
        if (hasXing(z1, z2)) conflicts.add(z1 + z2 + "相刑")
        if (hasChong(z1, z2)) conflicts.add(z1 + z2 + "相冲")
        if (hasHai(z1, z2)) conflicts.add(z1 + z2 + "相害")
        val pass = conflicts.isEmpty()
        return RuleResult("生肖", "双方生肖不能犯刑、冲、穿（害）",
            "男方生肖$sx1($z1)，女方生肖$sx2($z2)", conflicts, pass,
            if (pass) "双方生肖无刑冲穿害，为吉" else "双方生肖犯${conflicts.joinToString("、")}，为凶")
    }

    // 规则2 纳音
    private fun ruleNayin(b1: Bazi, b2: Bazi): RuleResult {
        val ny1 = JishiyuBase.queryNayin(b1.yearGan + b1.yearZhi)
        val ny2 = JishiyuBase.queryNayin(b2.yearGan + b2.yearZhi)
        val wx1 = extractNayinWuxing(ny1); val wx2 = extractNayinWuxing(ny2)
        val rel = wuxingRelation(wx1, wx2)
        val pass = rel == "比和" || rel == "相生"
        return RuleResult("纳音", "双方年柱纳音不能相克，纳音相生为吉",
            "男方年柱纳音$ny1($wx1)，女方年柱纳音$ny2($wx2)，关系：$rel", emptyList(), pass,
            "纳音$rel，" + if (pass) "为吉" else "为凶")
    }

    // 规则3 婚宫
    private fun ruleHungong(b1: Bazi, b2: Bazi): RuleResult {
        val z1 = b1.yearZhi; val z2 = b2.yearZhi; val rz1 = b1.dayZhi; val rz2 = b2.dayZhi
        val conflicts = mutableListOf<String>()
        if (hasXingChongHai(z1, rz2)) {
            val t = if (hasXing(z1, rz2)) "刑" else if (hasChong(z1, rz2)) "冲" else "害"
            conflicts.add("男方属相${z1}与女方婚宫${rz2}犯$t")
        }
        if (hasXingChongHai(z2, rz1)) {
            val t = if (hasXing(z2, rz1)) "刑" else if (hasChong(z2, rz1)) "冲" else "害"
            conflicts.add("女方属相${z2}与男方婚宫${rz1}犯$t")
        }
        if (hasXingChongHai(rz1, rz2)) {
            val t = if (hasXing(rz1, rz2)) "刑" else if (hasChong(rz1, rz2)) "冲" else "害"
            conflicts.add("双方婚宫${rz1}${rz2}犯$t")
        }
        val pass = conflicts.isEmpty()
        return RuleResult("婚宫", "双方属相与日支（婚宫）之间，不能犯刑、冲、穿、克",
            "男方属相$z1，婚宫$rz1；女方属相$z2，婚宫$rz2", conflicts, pass,
            if (pass) "属相与婚宫无刑冲穿害，为吉" else "${conflicts.joinToString("；")}，为凶")
    }

    // 规则4 时柱
    private fun ruleShizhu(b1: Bazi, b2: Bazi): RuleResult {
        val sz1 = b1.timeZhi; val sz2 = b2.timeZhi
        val wx1 = zhiWx(sz1); val wx2 = zhiWx(sz2)
        val xch = mutableListOf<String>()
        if (hasXing(sz1, sz2)) xch.add("刑")
        if (hasChong(sz1, sz2)) xch.add("冲")
        if (hasHai(sz1, sz2)) xch.add("害")
        val rel = wuxingRelation(wx1, wx2)
        val wuxingKe = rel == "我克" || rel == "克我"
        val pass = xch.isEmpty() && !wuxingKe
        var detail = "男方时支$sz1($wx1)，女方时支$sz2($wx2)，五行关系：$rel"
        if (xch.isNotEmpty()) detail += "，犯${xch.joinToString("、")}"
        val passDesc = if (pass) "时柱五行无刑冲穿害克，为吉"
        else (if (xch.isNotEmpty()) "时支犯${xch.joinToString("、")}，" else "") + (if (wuxingKe) "五行相克，" else "") + "为凶"
        return RuleResult("时柱", "双方时柱的五行，不能犯刑、冲、穿、害", detail, emptyList(), pass, passDesc)
    }

    // 规则12 年柱
    private fun ruleNianzhu(b1: Bazi, b2: Bazi): RuleResult {
        val ng1 = b1.yearGan; val nz1 = b1.yearZhi; val ng2 = b2.yearGan; val nz2 = b2.yearZhi
        val ganHe = has5he(ng1, ng2); val ganChong = hasGanChong(ng1, ng2)
        val ganKe = hasGanKe(ng1, ng2); val zhiHe = has6he(nz1, nz2); val zhiChong = hasChong(nz1, nz2)
        val tianHeDiHe = ganHe && !zhiChong
        val tianKeDiChong = (ganKe || ganChong) && zhiChong
        var detail = "男方年柱$ng1$nz1，女方年柱$ng2$nz2；"
        detail += "天干" + (if (ganHe) "五合" else if (ganChong) "相冲" else if (ganKe) "相克" else "无特殊关系") + "，"
        detail += "地支" + (if (zhiHe) "六合" else if (zhiChong) "相冲" else "无特殊关系")
        val pass = tianHeDiHe || (!tianKeDiChong && (ganHe || zhiHe))
        val passDesc = if (tianHeDiHe) "天合地合，最吉"
        else if (tianKeDiChong) "天克地冲，最凶"
        else if (ganHe || zhiHe) (if (ganHe) "天干五合" else "") + (if (zhiHe) "地支六合" else "") + (if (pass) "，为吉" else "，为凶")
        else "无冲克或相合关系"
        return RuleResult("年柱", "天合地合为吉，天克地冲为凶", detail, emptyList(), pass, passDesc,
            passDesc != "无冲克或相合关系")
    }

    // 规则5 用神
    private fun ruleYongshen(a1: BaziAnalyzer.Analysis, a2: BaziAnalyzer.Analysis): RuleResult {
        val ys1 = a1.yongshen; val ys2 = a2.yongshen
        var complement = false
        ys1.forEach { x -> ys2.forEach { y -> if (WX_SHENG[y] == x || WX_SHENG[x] == y) complement = true } }
        val same = ys1.any { it in ys2 }
        ys1.forEach { x -> ys2.forEach { y -> if (x != y && WX_SHENG[y] == x) complement = true } }
        var conflict = false
        ys1.forEach { x -> ys2.forEach { y -> if (WX_KE[x] == y || WX_KE[y] == x) conflict = true } }
        val pass = (complement || same) && !conflict
        val label = fun(s: String) = if (s == "身强") "身强" else if (s == "身弱") "身弱" else "平衡"
        val detail = "男方${label(a1.wangshuai)}，用神${ys1.joinToString("")}；女方${label(a2.wangshuai)}，用神${ys2.joinToString("")}"
        val passDesc = if (pass) {
            if (same && complement) "双方用神一致且互补，为吉"
            else if (same) "双方用神一致，为吉" else "双方用神互补，为吉"
        } else {
            if (conflict && complement) "双方用神虽有互补但存在冲克，为凶"
            else if (conflict) "双方用神互相排斥，为凶" else "双方用神既不一致也不互补，为凶"
        }
        return RuleResult("用神", "双方用神一致或互补为吉，互相排斥为凶", detail, emptyList(), pass, passDesc)
    }

    // 规则6 十神
    private fun ruleShishen(man: Bazi, woman: Bazi): RuleResult {
        val manBijie = countShishen(man, "比肩") + countShishen(man, "劫财")
        val womanBijie = countShishen(woman, "比肩") + countShishen(woman, "劫财")
        val womanShangguan = countShishen(woman, "伤官") + countShishen(woman, "食神")
        val manGuansha = countShishen(man, "正官") + countShishen(man, "七杀")
        val manKeWife = manBijie >= 3
        val womanKeHusband = womanBijie >= 3
        var detail = "男方比劫${manBijie}个${if (manKeWife) "（重）" else "（轻）"}；"
        detail += "女方比劫${womanBijie}个${if (womanKeHusband) "（重）" else "（轻）"}；"
        var hardMatch = false
        if (manKeWife && womanKeHusband) { hardMatch = true; detail += "双方互克，属于硬配；" }
        if (womanShangguan >= 3 && womanBijie >= 3) detail += "女方伤官旺且比劫重，主固执霸道克夫；"
        if (manGuansha >= 3 && manBijie >= 3) detail += "男方官杀旺且比劫重，主固执霸道克妻；"
        var pass = !manKeWife && !womanKeHusband
        if (hardMatch) pass = false
        val passDesc = if (hardMatch) "硬配（双方互克），为不得已的选择"
        else if (pass) "双方十神配置平和，为吉" else "一方克性较重，为凶"
        return RuleResult("十神", "比劫重、伤官旺或官杀旺为凶；硬配为不得已", detail, emptyList(), pass, passDesc)
    }

    // 规则7 调候
    private fun ruleTiaohou(a1: BaziAnalyzer.Analysis, a2: BaziAnalyzer.Analysis): RuleResult {
        val th1 = a1.tiaohou; val th2 = a2.tiaohou
        var detail = "男方调候用神${if (th1.isNotEmpty()) th1.joinToString("") else "无"}；"
        detail += "女方调候用神${if (th2.isNotEmpty()) th2.joinToString("") else "无"}"
        if (th1.isEmpty() || th2.isEmpty())
            return RuleResult("调候", "双方调候用神要能互补", detail, emptyList(), false, "无法判断", false)
        var complement = false
        th1.forEach { x -> th2.forEach { y -> if (x != y && (WX_SHENG[x] == y || WX_SHENG[y] == x)) complement = true } }
        val same = th1.any { it in th2 }
        var conflict = false
        th1.forEach { x -> th2.forEach { y -> if (WX_KE[x] == y || WX_KE[y] == x) conflict = true } }
        val pass = (complement || same) && !conflict
        val passDesc = if (pass) {
            if (same && complement) "调候用神一致且互补，为吉"
            else if (same) "调候用神一致，为吉" else "调候互补，为吉"
        } else {
            if (conflict && complement) "调候虽有互补但存在冲克，为凶"
            else if (conflict) "调候用神互相排斥，为凶" else "调候不能互补，为凶"
        }
        return RuleResult("调候", "双方调候用神要能互补", detail, emptyList(), pass, passDesc)
    }

    // 规则8 空亡
    private fun ruleKongwang(b1: Bazi, isMan1: Boolean, b2: Bazi, isMan2: Boolean): RuleResult {
        val spouse1 = findSpouseStar(b1, isMan1); val spouse2 = findSpouseStar(b2, isMan2)
        val no1 = spouse1.isEmpty(); val no2 = spouse2.isEmpty()
        val kw1 = JishiyuBase.queryKongwang(b1.dayGan + b1.dayZhi)
        val kw2 = JishiyuBase.queryKongwang(b2.dayGan + b2.dayZhi)
        val hgk1 = kw1.isNotEmpty() && kw1.contains(b1.dayZhi)
        val hgk2 = kw2.isNotEmpty() && kw2.contains(b2.dayZhi)
        val spk1 = spouse1.any { it.zhi.isNotEmpty() && kw1.contains(it.zhi) }
        val spk2 = spouse2.any { it.zhi.isNotEmpty() && kw2.contains(it.zhi) }
        var detail = ""
        detail += "男方${if (isMan1) "财星(妻星)" else "官星(夫星)"}${if (no1) "无" else "有" + spouse1.size + "个"}，"
        detail += "日柱空亡${kw1.ifEmpty { "无" }}，婚宫坐空亡${if (hgk1) "是" else "否"}，夫妻星坐空亡${if (spk1) "是" else "否"}；"
        detail += "女方${if (isMan2) "财星(妻星)" else "官星(夫星)"}${if (no2) "无" else "有" + spouse2.size + "个"}，"
        detail += "日柱空亡${kw2.ifEmpty { "无" }}，婚宫坐空亡${if (hgk2) "是" else "否"}，夫妻星坐空亡${if (spk2) "是" else "否"}"
        val pass = !no1 && !no2 && !hgk1 && !hgk2 && !spk1 && !spk2
        val reasons = mutableListOf<String>()
        if (no1) reasons.add("男方无${if (isMan1) "财" else "官"}星")
        if (no2) reasons.add("女方无${if (isMan2) "财" else "官"}星")
        if (hgk1 || hgk2) reasons.add("婚宫坐空亡")
        if (spk1 || spk2) reasons.add("夫妻星坐空亡")
        return RuleResult("空亡", "无财星/官星、夫妻星或婚姻宫坐空亡为凶", detail, emptyList(), pass,
            if (pass) "夫妻星俱在且不坐空亡，为吉" else "${reasons.joinToString("，")}，为凶")
    }

    // 规则9 性格
    private data class Person(val core: String, val dominantWuxing: String, val dominantShishen: String, val style: String)
    private fun ruleXingge(a1: BaziAnalyzer.Analysis, a2: BaziAnalyzer.Analysis): RuleResult {
        val p1 = Person(a1.personality, a1.dominantWuxing, a1.dominantShishen, a1.style)
        val p2 = Person(a2.personality, a2.dominantWuxing, a2.dominantShishen, a2.style)
        fun similarity(): Int {
            var s = 0
            if (p1.core == p2.core) s += 30
            if (p1.dominantWuxing == p2.dominantWuxing) s += 30
            if (p1.dominantShishen == p2.dominantShishen) s += 20
            if (p1.style == p2.style) s += 20
            return s
        }
        fun complement(): Int {
            var s = 0
            if (WX_SHENG[p1.dominantWuxing] == p2.dominantWuxing) s += 40
            if (WX_SHENG[p2.dominantWuxing] == p1.dominantWuxing) s += 40
            if (p1.style != p2.style) s += 20
            return s
        }
        fun conflictScore(): Int {
            var s = 0
            if (WX_KE[p1.dominantWuxing] == p2.dominantWuxing) s += 40
            if (WX_KE[p2.dominantWuxing] == p1.dominantWuxing) s += 40
            if ((p1.dominantShishen == "食伤" && p2.dominantShishen == "官杀") ||
                (p2.dominantShishen == "食伤" && p1.dominantShishen == "官杀")) s += 30
            if ((p1.dominantShishen == "比劫" && p2.dominantShishen == "财") ||
                (p2.dominantShishen == "比劫" && p1.dominantShishen == "财")) s += 20
            return s
        }
        val sim = similarity(); val comp = complement(); val con = conflictScore()
        var total = sim * 0.4 + comp * 0.3 - con * 0.3
        total = max(0.0, min(100.0, total))
        val score = round(total).toInt()
        val level = if (total >= 80) "高度契合" else if (total >= 60) "较合适" else if (total >= 40) "一般" else "较多冲突"
        val highlights = mutableListOf<String>(); val risks = mutableListOf<String>()
        if (p1.dominantWuxing == p2.dominantWuxing) highlights.add("性格气质相近，容易相互理解")
        if (WX_SHENG[p1.dominantWuxing] == p2.dominantWuxing) highlights.add("一方对另一方有支持作用")
        if (WX_KE[p1.dominantWuxing] == p2.dominantWuxing) risks.add("容易产生控制或压制关系")
        var detail = "男方${p1.core}，主导${p1.dominantWuxing}${p1.dominantShishen}，${p1.style}；"
        detail += "女方${p2.core}，主导${p2.dominantWuxing}${p2.dominantShishen}，${p2.style}；"
        detail += "契合度${score}分（$level）"
        if (highlights.isNotEmpty()) detail += "，亮点：${highlights.joinToString("；")}"
        if (risks.isNotEmpty()) detail += "，风险：${risks.joinToString("；")}"
        val pass = score >= 40
        return RuleResult("性格", "双方脾气不合的，不能结婚", detail, emptyList(), pass,
            if (pass) "性格契合度${score}分（$level），为吉" else "性格契合度仅${score}分（$level），为凶")
    }

    // 规则10 桃花
    private fun ruleTaohua(b1: Bazi, b2: Bazi): RuleResult {
        val c1 = taohuaCount(b1); val c2 = taohuaCount(b2)
        val bothSevere = c1 >= 2 && c2 >= 2
        return RuleResult("桃花", "双方桃花都特别严重的，不能结婚", "男方桃花${c1}个，女方桃花${c2}个", emptyList(), !bothSevere,
            if (bothSevere) "双方桃花均较重，为凶"
            else if (c1 >= 2 || c2 >= 2) "一方桃花较重，需注意" else "双方桃花不重，为吉")
    }

    // 规则11 神煞
    private fun ruleShensha(b1: Bazi, b2: Bazi): RuleResult {
        var bad1 = mutableListOf<String>(); var bad2 = mutableListOf<String>()
        b1.zhis().forEach { z ->
            if (HONGYAN[b1.dayGan] == z) bad1.add("红艳")
            if (LIUXIA[b1.dayGan] == z) bad1.add("流霞")
        }
        if (b1.dayGan + b1.dayZhi in YINYANG_CHACUO) bad1.add("阴阳差错")
        b2.zhis().forEach { z ->
            if (HONGYAN[b2.dayGan] == z) bad2.add("红艳")
            if (LIUXIA[b2.dayGan] == z) bad2.add("流霞")
        }
        if (b2.dayGan + b2.dayZhi in YINYANG_CHACUO) bad2.add("阴阳差错")
        bad1 = unique(bad1).toMutableList(); bad2 = unique(bad2).toMutableList()
        val hasBad = bad1.isNotEmpty() || bad2.isNotEmpty()
        val goodSha = (TIANYI[b1.yearGan]?.contains(b2.yearZhi) ?: false) &&
                      (TIANYI[b2.yearGan]?.contains(b1.yearZhi) ?: false)
        var detail = "男方不利神煞${if (bad1.isNotEmpty()) bad1.joinToString("、") else "无"}；"
        detail += "女方不利神煞${if (bad2.isNotEmpty()) bad2.joinToString("、") else "无"}；"
        detail += "年柱互为天乙贵人${if (goodSha) "是" else "否"}"
        val pass = !hasBad || goodSha
        val passDesc = if (goodSha) "年柱互为天乙贵人，为吉"
        else if (hasBad) "带有不利婚姻的神煞${unique(bad1 + bad2).joinToString("、")}，为凶"
        else "无不利婚姻神煞，为吉"
        return RuleResult("神煞", "女命忌红艳、流霞；男女忌阴阳差错日；年柱互为天乙贵人为吉", detail, emptyList(), pass, passDesc)
    }
}
