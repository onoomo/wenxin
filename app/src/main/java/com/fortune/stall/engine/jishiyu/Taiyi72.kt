package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.util.SolarTerm
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 太乙神数 · 传统七十二局（独立模块）。
 *
 * 移植自开源项目「太卜 taibu」packages/core/src/domains/taiyi/traditional.ts（MIT 许可），
 * 立成表采用《太乙金鏡式經》四库全书本卷三的阴/阳七十二局。
 * 规则版本 taiyi-skqs-modern-calendar-2026.10。
 *
 * 与旧版太乙算法的区别：
 *  - 本类为「传统太乙神数」正统七十二局查表法（金镜本），积数纪元 1936557 / 10153917；
 *  - Taiyi 为三式合盘的太乙部分，积数为四柱乘积 + 转身数算法。两者流派不同，数值不同。
 *
 * 输出字段：积数、局数、阴阳遁、太乙宫位、文昌、始击、计神、合神、太岁、
 * 主算/客算及主大将/主参将/客大将/客参将、十六方位（含中宫）星曜落点。
 */
object Taiyi72 {

    /** 起局尺度 */
    enum class Mode(val label: String) {
        YEAR("年盘"), MONTH("月盘"), DAY("日盘"), HOUR("时盘")
    }

    /** 积数纪元流派 */
    enum class Tradition(val label: String) {
        JINJING("金镜"), TONGZONG("统宗")
    }

    // 《太乙金鏡式經》四库本卷三 阳/阴七十二局立成（主算、客算，各 72 项）
    private val YANG_HOME = intArrayOf(
        7,6,1,25,25,25,8,1,3,1,4,37,18,10,9,1,7,7,8,7,2,16,16,16,39,32,31,14,13,10,33,25,24,26,25,25,
        1,6,25,27,27,27,8,33,32,5,4,1,24,6,15,39,38,38,16,15,10,12,12,12,33,26,25,16,15,12,25,17,16,30,29,29
    )
    private val YANG_AWAY = intArrayOf(
        13,1,40,17,14,10,25,22,15,12,4,1,19,9,7,33,27,26,32,26,17,30,23,17,40,31,28,9,39,32,10,8,3,4,28,27,
        7,35,34,19,16,12,17,14,7,16,8,5,25,15,13,31,25,24,3,34,25,26,19,13,34,25,22,11,1,34,2,8,32,4,32,31
    )
    private val YIN_HOME = intArrayOf(
        5,4,1,25,25,17,2,1,7,1,6,35,12,12,11,1,3,3,14,13,10,24,24,16,21,31,39,8,7,2,27,27,26,26,25,25,
        1,4,37,33,33,25,2,39,38,7,6,1,16,16,15,33,32,32,16,15,12,18,18,10,27,26,25,16,15,10,25,25,24,22,31,31
    )
    private val YIN_AWAY = intArrayOf(
        29,17,16,33,30,26,3,7,33,34,36,23,37,27,29,15,9,8,16,10,1,14,7,1,16,7,4,25,15,8,28,26,18,22,10,9,
        25,13,12,1,38,34,38,31,31,2,32,29,1,31,29,7,1,8,18,12,3,8,1,35,12,3,4,33,23,16,26,24,16,28,16,15
    )

    // 十六方位（顺时针自子起，含四维艮巽坤乾）：子丑艮寅卯辰巽巳午未坤申酉戌乾亥
    private val POSITIONS: List<String> = "子丑艮寅卯辰巽巳午未坤申酉戌乾亥".map { it.toString() }
    private val GOD_NAMES = listOf(
        "地主","阳德","和德","吕申","高丛","太阳","大炅","大神",
        "大威","天道","大武","武德","太簇","阴主","阴德","大义"
    )
    private val NUMBER_POSITION = mapOf(
        1 to "乾", 2 to "午", 3 to "艮", 4 to "卯", 5 to "中",
        6 to "酉", 7 to "坤", 8 to "子", 9 to "巽"
    )
    private val TAIYI_CYCLE = intArrayOf(1, 2, 3, 4, 6, 7, 8, 9)
    private val YANG_EYES = "申酉戌乾乾亥子丑艮寅卯辰巽巳午未坤坤".toList()
    private val YIN_EYES = "寅卯辰巽巽巳午未坤申酉戌乾亥子丑艮艮".toList()
    private val STRIKE = "坤戌亥丑寅辰巳坤酉乾丑寅辰午坤酉亥子艮辰巳未申戌亥艮卯巽未申酉子艮卯巳午坤戌亥丑寅辰巳坤酉乾丑寅辰午坤戌亥子艮辰巳未申戌亥艮卯巽未申乾子艮卯巳午".toList()
    private val DI_ZHI = LunarCalendar.DIZHI

    private fun positiveMod(value: Long, divisor: Int): Int = ((value % divisor) + divisor).toInt() % divisor

    /** 十六方位上的星曜落点 */
    data class Palace(val position: String, val deity: String, val stars: List<String>)

    data class General(val count: Int, val general: Int, val assistant: Int)

    /** 圆盘星将落点：name 星将名, position 十六方位字, tone 配色(0黑 1绿 2黄 3红) */
    data class StarLanding(val name: String, val position: String, val tone: Int)

    /** 干支旬空：甲子旬空戌亥、甲戌旬空申酉……按每柱自身干支取空亡 */
    private fun xunKong(gz: String): String = JishiyuBase.queryKongwang(gz)

    data class Result(
        val mode: Mode,
        val tradition: Tradition,
        val accumulatedNumber: Long,
        val accumulationSource: String,
        val bureau: Int,
        val yangDun: Boolean,
        val ruleVersion: String,
        val yearEpoch: Int,
        // 太乙宫位（宫数 + 方位）
        val taiyiPalace: Int,
        val taiyiPosition: String,
        // 星将落宫（方位字）
        val wenChang: String,
        val shiJi: String,
        val jiShen: String,
        val heShen: String,
        val taiSui: String,
        val home: General,
        val away: General,
        val palaces: List<Palace>,
        // 圆盘星将落点（17星将，供圆形盘渲染）
        val starLandings: List<StarLanding>,
        // 四柱旬空
        val kongWang: List<String>,
        // 上下文
        val siZhu: List<String>,
        val lunarText: String,
        val jieqi: String,
        val solarText: String,
        val jieqiDetail: String = "",
        val dateCn: String = ""
    ) {
        val dunLabel: String get() = if (yangDun) "阳遁" else "阴遁"
        val traditionLabel: String get() = tradition.label
        val modeLabel: String get() = mode.label
    }

    /**
     * 起局。
     * @param dt 公历时间
     * @param mode 年/月/日/时盘
     * @param tradition 积数纪元流派（金镜/统宗）
     * @param manualAccumulated 手工积数（可选，用于复核其他历书）
     * @param manualYangDun 手工阴阳遁（可选，null=自动）
     */
    fun paipan(
        dt: LocalDateTime,
        mode: Mode = Mode.HOUR,
        tradition: Tradition = Tradition.JINJING,
        manualAccumulated: Long? = null,
        manualYangDun: Boolean? = null
    ): Result {
        val lun = LunarCalendar.fromDateTime(dt)
        val yearEpoch = if (tradition == Tradition.JINJING) 1936557 else 10153917

        // 年积数：纪元 + 农历年
        val yearAccumulation = yearEpoch.toLong() + lun.year

        // 公历民用日（午夜为界），用于日/时积数
        val civilDay = dt.toLocalDate()
        fun daysFrom(y: Int, m: Int, d: Int): Long =
            ChronoUnit.DAYS.between(LocalDate.of(y, m, d), civilDay)

        // 月积数：(年积数-1)*12 + 2 + |农历月|（闰月复用本月，取绝对值）
        val monthAccumulation = (yearAccumulation - 1) * 12 + 2 + kotlin.math.abs(lun.month)
        // 日积数：金镜纪元基准 708010920，减去流派修正，加公历日差（1900-06-19 起）
        val dayAccumulation = 708010920L - (if (tradition == Tradition.JINJING) 1L else 0L) + daysFrom(1900, 6, 19)
        // 时积数：(基准 + 日差) * 12 + floor((hour+1)/2) + 1
        val hourAccumulation = (708011104L + daysFrom(1900, 12, 21)) * 12 + ((dt.hour + 1) / 2) + 1

        val accumulationSource: String
        val accumulatedNumber: Long = if (manualAccumulated != null) {
            accumulationSource = "manual"
            manualAccumulated
        } else {
            accumulationSource = "calendar-epoch"
            when (mode) {
                Mode.YEAR -> yearAccumulation
                Mode.MONTH -> monthAccumulation
                Mode.DAY -> dayAccumulation
                Mode.HOUR -> hourAccumulation
            }
        }

        // 局数 = ((积数-1) mod 72) + 1
        val bureau = positiveMod(accumulatedNumber - 1, 72) + 1
        val index = bureau - 1

        // 阴阳遁：时盘按最近冬至/夏至判定；年/月/日默认阳遁。可手工覆盖。
        val yangDun = manualYangDun ?: run {
            if (mode == Mode.HOUR) {
                val winter = SolarTerm.termTime(dt.year, "冬至")
                val summer = SolarTerm.termTime(dt.year, "夏至")
                when {
                    summer != null && dt.isAfter(summer) && (winter == null || dt.isBefore(winter)) -> false // 夏至后→阴遁
                    winter != null && dt.isBefore(winter) && (summer == null || dt.isAfter(summer)) -> false // 夏冬至之间→阴遁
                    else -> true
                }
            } else true
        }

        // 太乙宫位：阳遁顺行 TAIYI_CYCLE，阴遁逆行；每 3 局换一宫
        val cycle = if (yangDun) TAIYI_CYCLE else TAIYI_CYCLE.reversed().toIntArray()
        val taiyiPalace = cycle[(index % 24) / 3]
        val taiyiPosition = NUMBER_POSITION[taiyiPalace] ?: "乾"

        // 文昌：阳遁/阴遁十八神目
        val wenChang = (if (yangDun) YANG_EYES else YIN_EYES)[index % 18].toString()

        // 始击：阴遁三局有特殊落点覆盖（37→午, 43→坤, 44→酉）
        val shiJi = if (!yangDun) {
            when (bureau) {
                37 -> "午"; 43 -> "坤"; 44 -> "酉"; else -> STRIKE[index].toString()
            }
        } else STRIKE[index].toString()

        // 主客算：查立成表
        val homeCount = (if (yangDun) YANG_HOME else YIN_HOME)[index]
        val awayCount = (if (yangDun) YANG_AWAY else YIN_AWAY)[index]
        fun generals(count: Int): General {
            val g = if (count % 10 == 0) 1 else count % 10
            val assistant = (g * 3) % 10
            return General(count, g, if (assistant == 0) 5 else assistant)
        }
        val home = generals(homeCount)
        val away = generals(awayCount)

        // 太岁：按盘级取四柱地支
        val taiSui = when (mode) {
            Mode.YEAR -> lun.yearGanZhi[1].toString()
            Mode.MONTH -> lun.monthGanZhi[1].toString()
            Mode.DAY -> lun.dayGanZhi[1].toString()
            Mode.HOUR -> lun.hourGanZhi[1].toString()
        }

        // 计神：阳遁从丑(12序2)起逆数积数，阴遁从未(序8)起逆数
        val jiShen = DI_ZHI[positiveMod(((if (yangDun) 2 else 8) - positiveMod(accumulatedNumber - 1, 12)).toLong(), 12)]

        // 合神：太岁的地支六合（子丑合、寅亥合…），原版用 1 - index(太岁)
        val heShen = DI_ZHI[positiveMod((1 - DI_ZHI.indexOf(taiSui)).toLong(), 12)]

        // 十六方位（含中宫）星曜落点
        val starsByPos = mutableMapOf<String, MutableList<String>>()
        fun add(pos: String, name: String) {
            starsByPos.getOrPut(pos) { mutableListOf() }.add(name)
        }
        add(NUMBER_POSITION[taiyiPalace] ?: "乾", "太乙")
        add(wenChang, "文昌")
        add(shiJi, "始击")
        add(jiShen, "计神")
        add(heShen, "合神")
        add(taiSui, "太岁")
        add(NUMBER_POSITION[home.general] ?: "乾", "主大将")
        add(NUMBER_POSITION[home.assistant] ?: "乾", "主参将")
        add(NUMBER_POSITION[away.general] ?: "乾", "客大将")
        add(NUMBER_POSITION[away.assistant] ?: "乾", "客参将")

        val palaces = (POSITIONS + "中").mapIndexed { i, pos ->
            Palace(pos, GOD_NAMES.getOrNull(i) ?: "", starsByPos[pos] ?: emptyList())
        }

        // ===== 圆盘17星将落点（供圆形盘渲染）=====
        // 方位字 → 宫数映射（用于把吉凶星按宫数落位）
        val palaceOfNumber = NUMBER_POSITION
        fun posOfNum(n: Int) = palaceOfNumber[n] ?: "乾"
        val landings = mutableListOf<StarLanding>()
        fun land(name: String, pos: String, tone: Int = 0) { landings.add(StarLanding(name, pos, tone)) }
        // 图片配色：太乙绿(1) 文昌黄(2) 始击红(3)，其余黑
        land("太乙", NUMBER_POSITION[taiyiPalace] ?: "乾", 1)
        land("文昌", wenChang, 2)
        land("始击", shiJi, 3)
        land("计神", jiShen)
        land("合神", heShen)
        land("太岁", taiSui)
        land("主大将", posOfNum(home.general))
        land("主参将", posOfNum(home.assistant))
        land("客大将", posOfNum(away.general))
        land("客参将", posOfNum(away.assistant))
        // 用积数补算传统九星（君基/臣基/民基/五福/小游/四神/天乙/地乙/飞符）
        val n = accumulatedNumber
        fun cycZhi(seq: String, n0: Int): String { var x = n0; while (x > 12) x -= 12; while (x < 1) x += 12; return seq[x - 1].toString() }
        val zhiSeq = "子丑寅卯辰巳午未申酉戌亥"
        var junji = (positiveMod(n, 360) / 30) + 1
        if (positiveMod(n, 360) % 30 > 0) junji += 1
        var chenji = (positiveMod(n, 36) / 3) + 1
        if (positiveMod(n, 36) % 3 > 0) chenji += 1
        val minji = positiveMod(n, 12) + 1
        var wufu = positiveMod(n / 45, 5) + 1
        val xiaoyou = positiveMod(n / 24, 8) + 1
        val yunxing = "亥午寅卯辰酉申子巳戌未丑"
        var stdf = positiveMod(n, 36) % 3
        var stdfs = positiveMod(n, 36) / 3 + 1
        val sishen = cycZhi(yunxing, stdfs)
        val tianyi = cycZhi(yunxing, stdfs + 5)
        val diyi = cycZhi(yunxing, stdfs + 8)
        val feifu = cycZhi(yunxing, stdfs + 4)
        land("君基", cycZhi(zhiSeq, junji))
        land("臣基", cycZhi(zhiSeq, chenji))
        land("民基", cycZhi(zhiSeq, minji))
        land("五福", posOfNum(wufu))
        land("小游", posOfNum(xiaoyou))
        land("四神", sishen)
        land("天乙", tianyi)
        land("地乙", diyi)
        land("飞符", feifu)

        val siZhuLocal = listOf(lun.yearGanZhi, lun.monthGanZhi, lun.dayGanZhi, lun.hourGanZhi)
        val lunarTextLocal = "农历${lun.month}月${lun.day}日"
        val kongWang = siZhuLocal.map { xunKong(it) }
        val range = SolarTerm.rangeAt(dt)
        val jieqi = range?.let { r -> "${r.from.name} ~ ${r.to.name}" } ?: ""
        val jieqiDetail = range?.let { r ->
            "${r.from.name}${r.from.time.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))} ~ " +
            "${r.to.name}${r.to.time.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))}"
        } ?: ""
        val dateCn = "${dt.year}年${dt.monthValue}月${dt.dayOfMonth}日 ${dt.hour}时${String.format("%02d", dt.minute)}分".let { base ->
            // 用真实农历日替换（lunarText 形如 农历X月X日）
            val ld = Regex("农历(\\d+)月(\\d+)日").find(lunarTextLocal)
            if (ld != null) "$base(${ld.groupValues[1]}月${ld.groupValues[2]}日)" else base
        }
        val solarText = "${dt.year}-${String.format("%02d", dt.monthValue)}-${String.format("%02d", dt.dayOfMonth)} " +
            "${String.format("%02d", dt.hour)}:${String.format("%02d", dt.minute)}"

        return Result(
            mode = mode,
            tradition = tradition,
            accumulatedNumber = accumulatedNumber,
            accumulationSource = accumulationSource,
            bureau = bureau,
            yangDun = yangDun,
            ruleVersion = "taiyi-skqs-modern-calendar-2026.10",
            yearEpoch = yearEpoch,
            taiyiPalace = taiyiPalace,
            taiyiPosition = taiyiPosition,
            wenChang = wenChang,
            shiJi = shiJi,
            jiShen = jiShen,
            heShen = heShen,
            taiSui = taiSui,
            home = home,
            away = away,
            palaces = palaces,
            starLandings = landings,
            kongWang = kongWang,
            siZhu = siZhuLocal,
            lunarText = lunarTextLocal,
            jieqi = jieqi,
            solarText = solarText,
            jieqiDetail = jieqiDetail,
            dateCn = dateCn
        )
    }
}
