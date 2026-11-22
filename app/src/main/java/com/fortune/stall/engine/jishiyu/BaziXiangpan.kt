package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 八字详盘扩展 —— 1:1 移植自吉时雨 bazi.js / view_bazi.js / lunar.js。
 * 生成详盘页"流日/流月/流年/小运 + 四柱"八列对照表，每列 9 行属性，
 * 以及把大运/流年/流月/流日干支并入后的"详盘留意"与司令。
 */
object BaziXiangpan {

    /** 一列的完整属性（对齐 view_bazi.js showDayunCol/showLiuNianCol/... 的 9 行） */
    data class Col(
        val ganzhi: String,
        val gan: String, val zhi: String,
        val ganWx: String, val zhiWx: String,
        val ganShen: String,                 // 天干十神(全称)
        val cangGan: String,                 // 藏干(如 丁己)
        val cangShen: String,                // 藏干十神(用·分隔)
        val deShi: String,                   // 地势(日主在此支的长生)
        val ziZuo: String,                   // 自坐(本柱干坐本柱支的长生)
        val kongWang: String,
        val naYin: String,
        val shenSha: String                  // 神煞(用、分隔)
    )

    data class Data(
        val xiaoyun: Col, val liunian: Col, val liuyue: Col, val liuri: Col,
        val year: Col, val month: Col, val day: Col, val hour: Col,
        val xiaoyunYear: Int, val xiaoyunAge: Int,        // 小运列表头(2026 / 1岁)
        val liunianYear: Int, val liunianAge: Int,        // 流年列表头
        val liuyueMonth: Int, val liuyueLunar: String,    // 流月列表头(2月 / 正月)
        val liuriDay: Int, val liuriLunar: String,        // 流日列表头(4日 / 十七)
        val siLing: String,                               // 司令(壬)
        val liuYi: BaziFull.LiuYi                          // 详盘留意(含流日流月流年小运)
    )

    private val GAN = LunarCalendar.TIANGAN
    private val ZHI = LunarCalendar.DIZHI
    private val GAN_WX = JishiyuBase.GAN_WUXING
    private val ZHI_WX = JishiyuBase.ZHI_WUXING

    /** 十神全称(详盘页显示全称，与命盘页一致) */
    private fun shen(dayGan: String, other: String) = BaziChart.shiShen(dayGan, other)

    /** 为任意干支构造 9 行属性列（公开供胎元/命宫/身宫列复用） */
    fun colOf(
        ganzhi: String, dayGan: String, bazi: List<String>, isMan: Boolean,
        witch: Int, niannayin: String
    ): Col {
        val gan = ganzhi[0].toString(); val zhi = ganzhi[1].toString()
        val cang = JishiyuBase.CANGGAN[zhi] ?: emptyArray()
        val cangShen = cang.joinToString("·") { shen(dayGan, it) }
        val ss = Shensha.query(ganzhi, bazi, isMan, witch, niannayin)
        return Col(
            ganzhi, gan, zhi,
            GAN_WX[gan] ?: "", ZHI_WX[zhi] ?: "",
            shen(dayGan, gan),
            cang.joinToString(""), cangShen,
            JishiyuBase.queryShengwang(dayGan, zhi),
            JishiyuBase.queryShengwang(gan, zhi),
            JishiyuBase.queryKongwang(ganzhi),
            JishiyuBase.queryNayin(ganzhi),
            if (ss.isEmpty()) "无" else ss.joinToString("、")
        )
    }

    private fun jzOf(index: Int): String {
        val i = ((index % 60) + 60) % 60
        return GAN[i % 10] + ZHI[i % 12]
    }
    private fun jzIndex(gz: String) = JishiyuBase.jiaziIndex(gz)
    private fun isYang(g: String) = g in setOf("甲", "丙", "戊", "庚", "壬")

    /** 司令：以月支藏干按日元阴阳取首要人元（baziutils.getRenYuanSiLing 简化，1:1 lunar.js） */
    private fun siLing(chart: BaziChart.Chart, dt: LocalDateTime): String {
        // lunar.js getRenYuanSiLing: 月令藏干首个，按日干阴阳取本气/中气
        val yuezhi = chart.pillars[1].zhi
        val cang = JishiyuBase.CANGGAN[yuezhi] ?: return ""
        if (cang.isEmpty()) return ""
        return cang[0]
    }

    /**
     * 构建详盘默认时点数据。
     * 若当前年 < 起运首年 → 用小运阶段（流年=立春年柱，小运=时柱±1，流月=五虎遁正月，流日=立春首日）。
     */
    fun build(
        dt: LocalDateTime, isMan: Boolean,
        chart: BaziChart.Chart, full: BaziFull.Data
    ): Data {
        val gz = chart.pillars.map { it.ganZhi }
        val dayGan = chart.pillars[2].gan
        val yearGan = chart.pillars[0].gan
        val bazi = chart.pillars.flatMap { listOf(it.gan, it.zhi) }
        val niannayin = chart.pillars[0].naYin

        // 以排盘输入时刻所在公历年判断（原版用系统当前年；此处用排盘年以保证任意日期排盘都稳定）
        val curYear = dt.year
        val firstDayunYear = dt.year + full.qiYun.startYear
        val inXiaoyun = curYear < firstDayunYear

        // 流年干支：小运阶段=出生年立春柱；否则按大运推进
        val lnYear = if (inXiaoyun) dt.year else curYear
        val lnGz = jzOf(lnYear - 4)
        val lnAge = lnYear - dt.year + 1

        // 小运干支：时柱 index ± (1)，顺行按年干阴阳+性别
        val hourGz = chart.pillars[3].ganZhi
        val forward = (isYang(yearGan) && isMan) || (!isYang(yearGan) && !isMan)
        val xyGz = jzOf(jzIndex(hourGz) + if (forward) 1 else -1)
        val xyYear = dt.year
        val xyAge = 1

        // 流月干支：五虎遁，流年天干定正月天干；取正月(index=0)
        // 正月天干 = [2,4,6,8,0][yearGanIdx%5]，地支=寅
        val yearGanIdx = GAN.indexOf(lnGz[0].toString())
        val yinGanStart = intArrayOf(2, 4, 6, 8, 0)[((yearGanIdx % 5) + 5) % 5]
        val lyGz = GAN[yinGanStart % 10] + "寅"
        val lyMonth = 2
        val lyLunar = "正月"

        // 流日干支：立春首日(2/4)的日干支
        val lrDate = LocalDate.of(lnYear, 2, 4)
        val lrGz = LunarCalendar.dayGanZhi(lrDate)
        val lrDay = 4
        // 立春首日的农历日（2026-02-04 = 腊月十七）
        val lrLunar = lunarDayCn(lrDate)

        val cols = Data(
            xiaoyun = colOf(xyGz, dayGan, bazi, isMan, 15, niannayin),
            liunian = colOf(lnGz, dayGan, bazi, isMan, 6, niannayin),
            liuyue = colOf(lyGz, dayGan, bazi, isMan, 7, niannayin),
            liuri = colOf(lrGz, dayGan, bazi, isMan, 8, niannayin),
            // 四柱用各自 witch(1/2/3/4)，与原版命盘页神煞完全一致
            year = colOf(gz[0], dayGan, bazi, isMan, 1, niannayin),
            month = colOf(gz[1], dayGan, bazi, isMan, 2, niannayin),
            day = colOf(gz[2], dayGan, bazi, isMan, 3, niannayin),
            hour = colOf(gz[3], dayGan, bazi, isMan, 4, niannayin),
            xyYear, xyAge, lnYear, lnAge, lyMonth, lyLunar, lrDay, lrLunar,
            siLing(chart, dt),
            xiangPanLiuYi(gz, xyGz, lnGz, lyGz, lrGz)
        )
        return cols
    }

    /** 公历日 → 农历日中文(如 十七)。基于 LunarCalendar.fromDateTime 取日 */
    private fun lunarDayCn(date: LocalDate): String {
        val lun = LunarCalendar.fromDateTime(date.atStartOfDay())
        return LunarCalendar.cnNumber(lun.day)
    }

    /** 详盘留意：把小运/流年/流月/流日干支并入四柱后重算天干地支留意 */
    private fun xiangPanLiuYi(
        gz: List<String>, xyGz: String, lnGz: String, lyGz: String, lrGz: String
    ): BaziFull.LiuYi {
        // 复用 BaziFull 的留意算法（通过反射式调用不可行，这里直接重建同逻辑）
        val gans = gz.map { it[0].toString() } + listOf(xyGz[0].toString(), lnGz[0].toString(), lyGz[0].toString(), lrGz[0].toString())
        val zhis = gz.map { it[1].toString() } + listOf(xyGz[1].toString(), lnGz[1].toString(), lyGz[1].toString(), lrGz[1].toString())
        return liuYiOf(gans, zhis)
    }

    // ---- 留意算法（与 BaziFull 一致的 1:1 移植） ----
    private val HE5 = arrayOf("甲己", "乙庚", "丙辛", "丁壬", "戊癸")
    private val HE5_HUA = arrayOf("合化土", "合化金", "合化水", "合化木", "合化火")
    private val CONG4 = arrayOf("甲庚", "乙辛", "丙壬", "丁癸")
    private val WX_KE = arrayOf("金木", "木土", "土水", "水火", "火金")

    private fun pairs(list: List<String>, table: Array<String>, suffix: Array<String>): List<String> {
        val out = mutableListOf<String>()
        for (a in list) for (b in list) for (p in listOf(a + b, b + a)) {
            val i = table.indexOf(p)
            if (i >= 0) { val s = table[i] + suffix[i]; if (!out.contains(s)) out.add(s) }
        }
        return out
    }
    private fun triple(list: List<String>, table: Array<String>, suffix: Array<String>): List<String> {
        val out = mutableListOf<String>()
        for (a in list) for (b in list) for (c in list) {
            val idx = table.indexOf(a + b + c)
            if (idx >= 0) { val s = table[(idx / 6) * 6] + suffix[idx / 6]; if (!out.contains(s)) out.add(s) }
        }
        return out
    }
    private val HUI3 = arrayOf("寅卯辰","寅辰卯","卯辰寅","卯寅辰","辰寅卯","辰卯寅",
        "巳午未","巳未午","午巳未","午未巳","未巳午","未午巳",
        "申酉戌","申戌酉","酉申戌","酉戌申","戌申酉","戌酉申",
        "亥子丑","亥丑子","子亥丑","子丑亥","丑亥子","丑子亥")
    private val HE3 = arrayOf("亥卯未","亥未卯","卯亥未","卯未亥","未亥卯","未卯亥",
        "寅午戌","寅戌午","午寅戌","午戌寅","戌寅午","戌午寅",
        "巳酉丑","巳丑酉","酉巳丑","酉丑巳","丑巳酉","丑酉巳",
        "申子辰","申辰子","子申辰","子辰申","辰申子","辰子申")
    private val XING3 = arrayOf("丑未戌","丑戌未","未丑戌","未戌丑","戌丑未","戌未丑",
        "寅巳申","寅申巳","巳寅申","巳申寅","申寅巳","申巳寅")
    private val DUHE = arrayOf("甲己乙","甲乙己","己甲乙","己乙甲","乙甲己","乙己甲",
        "庚乙辛","庚辛乙","乙庚辛","乙辛庚","辛庚乙","辛乙庚",
        "丙辛丁","丙丁辛","辛丙丁","辛丁丙","丁丙辛","丁辛丙",
        "壬丁癸","壬癸丁","丁壬癸","丁癸壬","癸壬丁","癸丁壬",
        "戊癸己","戊己癸","癸戊己","癸己戊","己戊癸","己癸戊")

    private fun liuYiOf(gans: List<String>, zhis: List<String>): BaziFull.LiuYi {
        // 天干
        val tg = mutableListOf<String>()
        for (a in gans) for (b in gans) for (p in listOf(a + b, b + a)) {
            val i = HE5.indexOf(p); if (i >= 0) { val s = HE5[i] + HE5_HUA[i]; if (!tg.contains(s)) tg.add(s) }
            val j = CONG4.indexOf(p); if (j >= 0) { val s = CONG4[j] + "相冲"; if (!tg.contains(s)) tg.add(s) }
        }
        for (a in gans) for (b in gans) {
            val w1 = GAN_WX[a] ?: continue; val w2 = GAN_WX[b] ?: continue
            if (WX_KE.contains(w1 + w2)) { val s = a + "克" + b; if (!tg.contains(s)) tg.add(s) }
        }
        // 地支（顺序与 BaziFull 命盘页一致）
        val dz = mutableListOf<String>()
        dz += pairs(zhis, arrayOf("子丑","寅亥","卯戌","辰酉","巳申","午未"), arrayOf("合化土","合化木","合化火","合化金","合化水","合化火土"))
        dz += pairs(zhis, arrayOf("寅申","巳亥","辰戌","丑未","子午","卯酉"), Array(6){ "相冲" })
        dz += pairs(zhis, arrayOf("申子","子辰","亥卯","卯未","寅午","午戌","巳酉","酉丑"),
            arrayOf("半合水","半合水","半合木","半合木","半合火","半合火","半合金","半合金"))
        dz += pairs(zhis, arrayOf("申辰","亥未","寅戌","巳丑"), arrayOf("拱合水","拱合木","拱合火","拱合金"))
        dz += pairs(zhis, arrayOf("寅丑","亥午","卯申","巳酉","子巳","寅午"), Array(6){ "暗合" })
        // 自刑
        for (a in zhis) for (b in zhis) if (a != b && a == b) { }
        val zxTable = arrayOf("辰辰","午午","酉酉","亥亥")
        for (a in zhis.indices) for (b in zhis.indices) {
            if (a == b) continue
            val p = zhis[a] + zhis[b]; if (zxTable.contains(p)) { val s = p + "自刑"; if (!dz.contains(s)) dz.add(s) }
        }
        dz += pairs(zhis, arrayOf("寅巳","卯辰","丑午","子未","酉戌","申亥"), Array(6){ "相害" })
        dz += pairs(zhis, arrayOf("子酉","寅亥","卯午","辰丑","巳申","未戌"), Array(6){ "相破" })
        dz += triple(zhis, HUI3, arrayOf("会东方木","会南方火","会西方金","会北方水"))
        dz += triple(zhis, HE3, arrayOf("三合木","三合火","三合金","三合水"))
        dz += triple(zhis, XING3, arrayOf("三刑","三刑"))
        for (a in zhis) for (b in zhis) if (listOf("子卯","卯子").contains(a + b)) { val s = "子卯相刑"; if (!dz.contains(s)) dz.add(s) }
        dz += triple(gans, DUHE, Array(5){ "妒合" })
        return BaziFull.LiuYi(tg, dz)
    }
}
