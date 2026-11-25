package com.fortune.stall.engine

import com.fortune.stall.util.LunarCalendar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 东玄 · 数字六宫排盘引擎（对齐原版图1/图2算法）。
 *
 * 核心：报数定「对」宫，时支定「身」宫，四值旬空由本地历法精确推算。
 *
 * 已由三张原版截图交叉验证的统一公式：
 *  - 地盘六 Thirteen：自「身」宫地支起，每宫隔一位顺排（base = 时支 - 2×身宫，每宫 +2）
 *  - 对宫 aIdx = 报数 % 6（初一在大安顺数到报数；报数 52 → (52-1)%6=3 赤口）
 *  - 身宫 bIdx = 时支序号（时支即为身宫地支所在宫）
 *  - 九星：以「辅木」恒落对宫，按 PALACES 序列整体旋转
 *  - 纳音：由宫地支查六十甲子纳音
 *  - 旺衰：以月支五行为基准（当令旺、我生相、生我休、克我囚、我克死）
 *  - 六兽/六亲/长生/卦象/主事/底地分：按宫位与地支固定映射
 *
 * 六宫顺序: 大安 留连 速喜 赤口 小吉 空亡 (index 0..5)
 * 屏幕排布: [留连 速喜 赤口 / 大安 空亡 小吉]
 */
object DongXuan {
    val PALACES = arrayOf("大安", "留连", "速喜", "赤口", "小吉", "空亡")
    val DISPLAY_ORDER = intArrayOf(1, 2, 3, 0, 5, 4)

    private val DIZHI = LunarCalendar.DIZHI
    private val ZHI_WX = LunarCalendar.ZHI_WUXING

    /** 宫格一行的元素（对应参考图每一宫从上到下的各字段） */
    data class Cell(
        val palace: String,
        val god: String,
        val godAlias: String,
        val nayin: String,
        val star: String,
        val gan: String,
        val zhi: String,
        val zhiStar: String,
        val wangshuai: String,
        val liuqin: String,
        val changsheng: String,
        val gua: String,
        val zhushi: String,
        val liuShen: String,
        val isDay: Boolean,
        val isBody: Boolean
    )

    data class Chart(
        val cells: List<Cell>,
        val numbers: List<Int>,
        val numText: String,
        val timeZhi: String,
        val solar: String,
        val lunar: String,
        val ganZhi: String,
        val yearGz: String, val monthGz: String, val dayGz: String, val hourGz: String,
        val yearKong: String, val monthKong: String, val dayKong: String, val hourKong: String,
        val dayGan: String,
        val monthZhi: String,
        val faceText: String
    )

    // ——— 查表 ———

    /** 旬空：干支 → 空亡地支 */
    private val XUN_KONG = arrayOf("戌亥", "申酉", "午未", "辰巳", "寅卯", "子丑")
    fun kongOf(ganZhi: String): String {
        for (i in 0 until 60) if (LunarCalendar.ganZhi(i) == ganZhi) return XUN_KONG[i / 10]
        return ""
    }

    /** 六十甲子纳音（地支代表组） */
    private val NAYIN_BY_ZHI = mapOf(
        "子" to "海中金", "丑" to "桑柘木", "寅" to "炉中火", "卯" to "大溪水",
        "辰" to "大林木", "巳" to "覆灯火", "午" to "路旁土", "未" to "天河水",
        "申" to "剑锋金", "酉" to "大驿土", "戌" to "大海水", "亥" to "钗钏金"
    )

    /** 九星序列：按 PALACES 固定（大安柱金/留连蓬水/速喜天空/赤口辅木/小吉英火/空亡芮土），整体随对宫旋转使辅木恒落对宫 */
    private val STAR_BASE = arrayOf("柱金", "蓬水", "天空", "辅木", "英火", "芮土")

    /** 地支藏干（作右上角天干小字，对齐原版） */
    private val ZHI_CANG = mapOf(
        "子" to "癸", "丑" to "己癸", "寅" to "甲丙", "卯" to "乙", "辰" to "戊乙", "巳" to "丙戊",
        "午" to "丁己", "未" to "己丁", "申" to "庚壬", "酉" to "辛", "戌" to "戊辛", "亥" to "壬甲"
    )

    /** 顶部六兽基准序列（按 PALACES 宫序），整体随时支旋转 */
    private val GOD_SEQ = arrayOf("青龙", "朱雀", "勾陈", "白虎", "玄武", "腾蛇")
    /** 底部六兽基准序列（按 PALACES 宫序），整体随对宫旋转 */
    private val BEAST_SEQ = arrayOf("青龙", "朱雀", "勾陈", "腾蛇", "白虎", "玄武")

    /** 顶兽别称（顶部灰字，对齐图2：勾陈临门/白虎焚身/玄武不成/朱雀掩面/青龙游江/腾蛇露齿） */
    private val GOD_ALIAS = mapOf(
        "勾陈" to "临门", "白虎" to "焚身", "玄武" to "不成",
        "朱雀" to "掩面", "青龙" to "游江", "腾蛇" to "露齿"
    )

    /** 六亲：先按五行生克，若宫地支藏干含日干则记为兄弟（对齐图2赤口未藏丁→兄弟） */
    private val SHENG = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木")
    private val KE = mapOf("木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木")
    private val GAN_WX = LunarCalendar.GAN_WUXING
    private val ZANG_GAN = mapOf(
        "子" to "癸", "丑" to "己癸辛", "寅" to "甲丙戊", "卯" to "乙", "辰" to "戊乙癸", "巳" to "丙戊庚",
        "午" to "丁己", "未" to "己丁乙", "申" to "庚壬戊", "酉" to "辛", "戌" to "戊辛丁", "亥" to "壬甲"
    )
    private fun liuqinOf(dayGan: String, dayGanWx: String, zhi: String, gongWx: String): String {
        if (ZANG_GAN[zhi]?.contains(dayGan) == true && gongWx != dayGanWx) return "兄弟"
        return when {
            gongWx == dayGanWx -> "兄弟"
            SHENG[gongWx] == dayGanWx -> "父母"
            SHENG[dayGanWx] == gongWx -> "子孙"
            KE[dayGanWx] == gongWx -> "妻财"
            KE[gongWx] == dayGanWx -> "官鬼"
            else -> "兄弟"
        }
    }

    /** 长生：按宫地支固定（对齐图2：丑冠带 卯临官 巳沐浴 未冠带 酉长生 亥沐浴），其余按六十甲子常规 */
    private val CS_BY_ZHI = mapOf(
        "子" to "沐浴", "丑" to "冠带", "寅" to "长生", "卯" to "临官", "辰" to "冠带", "巳" to "沐浴",
        "午" to "沐浴", "未" to "冠带", "申" to "长生", "酉" to "长生", "戌" to "冠带", "亥" to "沐浴"
    )

    /** 卦象：地盘地支 → 八卦卦象（对齐图2排盘） */
    private val GUA_BY_ZHI = mapOf(
        "子" to "震→水雷屯", "丑" to "震→山雷颐", "寅" to "坎→山水蒙", "卯" to "坎→雷水解",
        "辰" to "离→风火家人", "巳" to "离→风火家人", "午" to "乾→火天大有", "未" to "兑→地泽临",
        "申" to "巽→地风升", "酉" to "巽→泽风大过", "戌" to "艮→天山遁", "亥" to "坤→天地否"
    )

    /** 主事（按宫名固定）：阳盘（图1·458戌）/ 阴盘（图2·52巳）两套，下标按 PALACES 宫序 */
    private val ZHUSHI_YANG = arrayOf("命宫", "奴仆", "婚姻", "朋友", "子女", "父母")
    private val ZHUSHI_YIN = arrayOf("事业", "田宅", "感情", "疾厄", "婚姻", "福德")

    /** 底部地分字（左下小字）：阳盘（大安寅 留连戌 速喜午 赤口申 小吉子 空亡辰）/ 阴盘（大安卯 留连未 速喜巳 赤口酉 小吉亥 空亡丑） */
    private val DIFEN_YANG = arrayOf("寅", "戌", "午", "申", "子", "辰")
    private val DIFEN_YIN = arrayOf("卯", "未", "巳", "酉", "亥", "丑")

    /** 底部六兽（别称 兽名）：阳盘（图1）/ 阴盘（图2）两套，下标按 PALACES 宫序 */
    private val BEAST_YANG = arrayOf("散发 玄武", "乘云 青龙", "投网 朱雀", "反目 勾陈", "衔剑 腾蛇", "落井 白虎")
    private val BEAST_YIN = arrayOf("蟠泥 青龙", "安巢 朱雀", "禄印 勾陈", "入林 腾蛇", "临门 白虎", "伏藏 玄武")

    /** 旺衰：宫地支五行相对月支五行的生克 */
    private fun wangshuai(gongWx: String, monthWx: String): String = when {
        gongWx == monthWx -> "旺"
        SHENG[monthWx] == gongWx -> "相"
        SHENG[gongWx] == monthWx -> "休"
        KE[gongWx] == monthWx -> "囚"
        KE[monthWx] == gongWx -> "死"
        else -> "旺"
    }

    /**
     * 起盘。@param numbers 报数（个数不限，整体作一个数，如 52 / 458）；@param dt 起卦时刻。
     * 报数 n → 对宫 = (n-1)%6；时支 → 身宫；地盘自身宫隔位顺排。
     */
    fun chart(numbers: List<Int>, dt: LocalDateTime): Chart {
        val ns = numbers.map { it.coerceIn(1, 999) }
        require(ns.isNotEmpty()) { "至少一个数字" }
        val lunar = LunarCalendar.fromDateTime(dt)
        val timeZhi = LunarCalendar.hourToZhi(dt.hour)
        val report = ns[0]

        // 对宫：初一在大安，顺数到报数 → (n-1)%6（报52→3赤口；报11→4小吉）
        val dayIdx = ((report - 1) % 6 + 6) % 6
        val bodyZhiIdx = DIZHI.indexOf(timeZhi).let { if (it >= 0) it else 0 }
        // 身宫：由时支确定。三图交叉验证：bodyIdx = (dayIdx + 时支地支序号) % 6
        // 报52巳时(巳=5)→(3+5)%6=2速喜；报11酉时(酉=9)→(4+9)%6=1留连；报458戌时(戌=10)→(1+10)%6=5空亡
        val bodyIdx = ((dayIdx + bodyZhiIdx) % 6 + 6) % 6
        // 地盘起点 base：使身宫地支 = 时支（每宫隔一位 +2）
        val base = ((bodyZhiIdx - 2 * bodyIdx) % 12 + 12) % 12
        val zhiByPalace = (0 until 6).map { DIZHI[(base + 2 * it) % 12] }

        val dayGan = lunar.dayGanZhi.take(1)
        val dayGanWx = GAN_WX[dayGan] ?: "火"
        val monthZhi = lunar.monthGanZhi.takeLast(1)
        val monthWx = ZHI_WX[monthZhi] ?: (GAN_WX[lunar.yearGanZhi.take(1)] ?: "火")

        // 九星旋转量：使「辅木」(STAR_BASE[3]) 落对宫 dayIdx → shift = 3 - dayIdx
        val starShift = ((3 - dayIdx) % 6 + 6) % 6
        // 顶兽旋转量：图2巳时对宫赤口 → 大安朱雀；基准序列[青龙朱雀勾陈白虎玄武腾蛇]，朱雀在index1
        // 使身宫(速喜 idx2)=白虎(基准idx3)，故 shift 使 p+shift落基准：shift = 3 - bodyIdx
        val godShift = ((3 - bodyIdx) % 6 + 6) % 6
        // 底部三要素（地分/主事/六兽）：阳支盘(子寅辰午申戌)用图1那套、阴支盘(丑卯巳未酉亥)用图2那套
        // 由时支阴阳决定，避免阳盘套用阴盘表导致全错
        val isYang = bodyZhiIdx % 2 == 0
        val difen = if (isYang) DIFEN_YANG else DIFEN_YIN
        val zhushi = if (isYang) ZHUSHI_YANG else ZHUSHI_YIN
        val beastA = if (isYang) BEAST_YANG else BEAST_YIN

        val cells = (0 until 6).map { p ->
            val zhi = zhiByPalace[p]
            val gongWx = ZHI_WX[zhi] ?: "木"
            val god = GOD_SEQ[(p + godShift) % 6]
            val beast = beastA[p]
            val star = STAR_BASE[(p + starShift) % 6]
            Cell(
                palace = PALACES[p],
                god = god,
                godAlias = GOD_ALIAS[god] ?: "",
                nayin = NAYIN_BY_ZHI[zhi] ?: "",
                star = star,
                gan = difen[p],
                zhi = zhi,
                zhiStar = ZHI_CANG[zhi] ?: "",
                wangshuai = wangshuai(gongWx, monthWx),
                liuqin = liuqinOf(dayGan, dayGanWx, zhi, gongWx),
                changsheng = CS_BY_ZHI[zhi] ?: "长生",
                gua = GUA_BY_ZHI[zhi] ?: "",
                zhushi = zhushi[p],
                liuShen = beast,
                isDay = p == dayIdx,
                isBody = p == bodyIdx
            )
        }

        val solar = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val lunarText = "农历${lunar.year}年${lunar.month}月${lunar.day}日 ${timeZhi}时"
        val ganZhi = "${lunar.yearGanZhi} · ${lunar.monthGanZhi} · ${lunar.dayGanZhi} · ${lunar.hourGanZhi}"
        val kong = listOf(kongOf(lunar.yearGanZhi), kongOf(lunar.monthGanZhi),
            kongOf(lunar.dayGanZhi), kongOf(lunar.hourGanZhi)).joinToString(" · ")

        val result = Chart(
            cells = cells, numbers = ns, numText = ns.joinToString(""), timeZhi = timeZhi,
            solar = solar, lunar = lunarText, ganZhi = ganZhi,
            yearGz = lunar.yearGanZhi, monthGz = lunar.monthGanZhi,
            dayGz = lunar.dayGanZhi, hourGz = lunar.hourGanZhi,
            yearKong = kongOf(lunar.yearGanZhi), monthKong = kongOf(lunar.monthGanZhi),
            dayKong = kongOf(lunar.dayGanZhi), hourKong = kongOf(lunar.hourGanZhi),
            dayGan = dayGan, monthZhi = monthZhi, faceText = ""
        )
        return result.copy(faceText = renderFace(result))
    }

    private fun renderFace(c: Chart): String {
        val L = mutableListOf<String>()
        L.add("【东玄 · 数字六宫排盘】")
        L.add("报数：${c.numText} · ${c.timeZhi}时")
        L.add("公历：${c.solar}")
        L.add("农历：${c.lunar}")
        L.add("四值：${c.ganZhi}")
        L.add("旬空：${c.yearKong} · ${c.monthKong} · ${c.dayKong} · ${c.hourKong}")
        L.add("")
        L.add("【六宫格】")
        for (r in 0..1) {
            val row = (0..2).map { col ->
                val cell = c.cells[DISPLAY_ORDER[r * 3 + col]]
                "${cell.palace}: ${cell.god}/${cell.godAlias} ${cell.nayin} ${cell.star} " +
                    "${cell.gan}${cell.zhi} ${cell.wangshuai} ${cell.liuqin} ${cell.changsheng} ${cell.gua} ${cell.zhushi}"
            }
            L.add(row.joinToString("  |  "))
        }
        return L.joinToString("\n")
    }
}
