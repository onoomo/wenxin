package com.fortune.stall.engine

import com.fortune.stall.util.LunarCalendar
import java.time.LocalDateTime

/**
 * 小六壬（马前课）起卦引擎 —— 完整移植 divination-skills/xiaoliuren/scripts/qigua.py。
 * 纯函数：给定参数输出完整卦面文本（技术底稿），符合「卦面即印章」。
 */
object XiaoLiuRen {
    private val GONGS = arrayOf("大安", "留连", "速喜", "赤口", "小吉", "空亡")
    private val WUXING = mapOf("大安" to "木", "留连" to "土", "速喜" to "火", "赤口" to "金", "小吉" to "水", "空亡" to "土")
    private val TU_TYPE = mapOf("留连" to "四方阴土", "空亡" to "中央阳土")
    private val JIXIONG = mapOf("大安" to "大吉", "速喜" to "中吉", "小吉" to "小吉", "留连" to "小凶", "赤口" to "中凶", "空亡" to "大凶")
    private val SISHEN = mapOf("大安" to "青龙", "留连" to "腾蛇", "速喜" to "朱雀", "赤口" to "白虎", "小吉" to "玄武", "空亡" to "勾陈")
    private val GONGZHI = mapOf(
        "大安" to "事业宫(暗藏命宫)", "留连" to "田宅宫(暗藏奴仆宫)", "速喜" to "感情宫(暗藏夫妻宫)",
        "赤口" to "疾厄宫(暗藏兄弟宫)", "小吉" to "驿马宫(暗藏子女宫)", "空亡" to "福德宫(暗藏父母宫)"
    )
    private val SHUZI = mapOf(
        "大安" to "1、4、5(隐7)", "留连" to "2、7、8(隐8)", "速喜" to "3、6、9(隐9)",
        "赤口" to "4、1、2(隐10)", "小吉" to "5、3、8(隐11)", "空亡" to "6、5、10(隐12)"
    )
    private val CANGZHI = mapOf("大安" to "寅卯", "留连" to "戌未", "速喜" to "午巳", "赤口" to "申酉", "小吉" to "亥子", "空亡" to "辰丑")
    private val BAGUA = mapOf("大安" to "震巽", "留连" to "坤", "速喜" to "离", "赤口" to "乾兑", "小吉" to "坎", "空亡" to "艮")
    private val FANGWEI = mapOf("大安" to "东方", "留连" to "四角(东南等偏方)", "速喜" to "南方", "赤口" to "西方", "小吉" to "北方", "空亡" to "中央")
    private val GONG_YY_TIYONG = mapOf("大安" to "阳", "速喜" to "阳", "赤口" to "阳", "空亡" to "阳", "小吉" to "阴", "留连" to "阴")
    private val GONG_YY_ZEFA = mapOf("大安" to "阳", "速喜" to "阳", "小吉" to "阳", "留连" to "阴", "赤口" to "阴", "空亡" to "阴")
    private val ZEFA_ZHI = mapOf(
        "大安" to mapOf("阳" to "寅", "阴" to "卯"), "留连" to mapOf("阳" to "戌", "阴" to "未"),
        "速喜" to mapOf("阳" to "午", "阴" to "巳"), "赤口" to mapOf("阳" to "申", "阴" to "酉"),
        "小吉" to mapOf("阳" to "子", "阴" to "亥"), "空亡" to mapOf("阳" to "辰", "阴" to "丑")
    )

    private val DIZHI = LunarCalendar.DIZHI
    private val ZHI_WUXING = LunarCalendar.ZHI_WUXING
    private val ZHI_YINYANG = DIZHI.associateWith { if (DIZHI.indexOf(it) % 2 == 0) "阳" else "阴" }
    private val SHENG = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木")
    private val KE = mapOf("木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木")
    private val MU = mapOf("木" to "未", "火" to "戌", "金" to "丑", "水" to "辰", "土" to "辰")
    private val CS_START = mapOf("木" to "亥", "火" to "寅", "金" to "巳", "水" to "申", "土" to "申")
    private val CS_NAMES = arrayOf("长生", "沐浴", "冠带", "临官", "帝旺", "衰", "病", "死", "墓", "绝", "胎", "养")
    private val HUOSHEN = arrayOf("青龙", "朱雀", "勾陈", "白虎", "玄武", "腾蛇")
    // jishiyu 风格宫格：五星（道家小六壬体系）
    private val STARS = mapOf("大安" to "辅木", "留连" to "芮土", "速喜" to "英火", "赤口" to "柱金", "小吉" to "蓬水", "空亡" to "任空")

    /** jishiyu 风格单宫显示数据 */
    data class GongCell(
        val gong: String,      // 宫名（大安/留连…）
        val position: String,  // 天/地/人
        val shen: String,      // 六神（四神）
        val xing: String,      // 五星
        val zhi: String,       // 藏支
        val zhiWx: String,     // 藏支五行
        val liuqin: String,    // 六亲
        val wuxing: String,    // 宫五行（底部色带）
        val jixiong: String,   // 吉凶
        val mark: String       // 打标（如"人宫·落宫"简写）
    )

    /**
     * 生成 jishiyu 风格宫格数据：三宫（天/地/人）各一张卡片。
     * 六亲以人宫(落宫)为我推算；六神/五星/藏支沿用本体系既有映射。
     */
    fun gongCells(gua: List<String>, hourZhi: String? = null): List<GongCell> {
        val ren = gua[2]
        val luoWx = WUXING[ren]!!
        val positions = listOf("天", "地", "人")
        return gua.mapIndexed { i, g ->
            val wx = WUXING[g]!!
            val zhi = CANGZHI[g]!!.first().toString()
            val zhiWx = ZHI_WUXING[zhi] ?: ""
            val lq = when {
                i == 2 -> "自身"
                wx == luoWx -> "兄弟"
                else -> liuqin(luoWx, g, wx).substringBefore("(")
            }
            GongCell(
                gong = g,
                position = positions[i],
                shen = SISHEN[g]!!,
                xing = STARS[g]!!,
                zhi = zhi,
                zhiWx = zhiWx,
                liuqin = lq,
                wuxing = wx,
                jixiong = JIXIONG[g]!!,
                mark = positions[i]
            )
        }
    }

    private fun wxRelation(a: String, b: String): String = when {
        a == b -> "比和"
        SHENG[a] == b -> "生"
        SHENG[b] == a -> "被生"
        KE[a] == b -> "克"
        KE[b] == a -> "被克"
        else -> "?"
    }

    private fun changsheng(wx: String, zhi: String): String {
        val start = DIZHI.indexOf(CS_START[wx])
        val idx = (DIZHI.indexOf(zhi) - start + 12) % 12
        return CS_NAMES[idx]
    }

    private fun wangshuai(gongWx: String, shiWx: String): String = when {
        gongWx == shiWx -> "旺"
        SHENG[shiWx] == gongWx -> "相"
        SHENG[gongWx] == shiWx -> "休"
        KE[gongWx] == shiWx -> "囚"
        else -> "死"
    }

    private fun liuqin(luoWx: String, otherGong: String, otherWx: String): String = when {
        otherWx == luoWx -> "小人(同五行异宫)"
        SHENG[otherWx] == luoWx -> "父母(生我)"
        SHENG[luoWx] == otherWx -> "子孙(我生)"
        KE[otherWx] == luoWx -> "官鬼(克我)"
        KE[luoWx] == otherWx -> "妻财(我克)"
        else -> "?"
    }

    /** 连续起法/独立起法：三数 → 天宫地宫人宫 */
    fun castFromCounts(n1: Int, n2: Int, n3: Int, independent: Boolean = false): List<String> {
        val i1 = (n1 - 1).mod(6)
        val i2 = if (independent) (n2 - 1).mod(6) else (i1 + n2 - 1).mod(6)
        val i3 = if (independent) (n3 - 1).mod(6) else (i2 + n3 - 1).mod(6)
        return listOf(GONGS[i1], GONGS[i2], GONGS[i3])
    }

    /** 农历月日时 → 三宫（月上起日、日上起时，正月起大安） */
    fun castFromLunar(lm: Int, ld: Int, hourZhi: String): List<String> {
        val i1 = (lm - 1).mod(6)
        val i2 = (i1 + ld - 1).mod(6)
        val i3 = (i2 + LunarCalendar.zhiOrdinal(hourZhi) - 1).mod(6)
        return listOf(GONGS[i1], GONGS[i2], GONGS[i3])
    }

    private fun zhuanTaiji(gua: List<String>, shiZhi: String?, gender: String?): Pair<Map<String, List<String>>, String>? {
        if (shiZhi == null || gender.isNullOrBlank()) return null
        val sy = ZHI_YINYANG[shiZhi] ?: return null
        val tuGong = if ((sy == "阳" && gender == "男") || (sy == "阴" && gender == "女")) "留连" else "空亡"
        val wxToGong = mapOf("木" to "大安", "火" to "速喜", "金" to "赤口", "水" to "小吉", "土" to tuGong)
        fun revSheng(w: String) = SHENG.entries.first { it.value == w }.key
        fun revKe(w: String) = KE.entries.first { it.value == w }.key
        val res = linkedMapOf<String, List<String>>()
        res["父母卦(生我者)"] = gua.map { wxToGong[revSheng(WUXING[it]!!)]!! }
        res["子孙卦(我生者)"] = gua.map { wxToGong[SHENG[WUXING[it]]!!]!! }
        res["妻财卦(我克者)"] = gua.map { wxToGong[KE[WUXING[it]]!!]!! }
        res["官鬼卦(克我者)"] = gua.map { wxToGong[revKe(WUXING[it]!!)]!! }
        return res to tuGong
    }

    private fun huoshen(shiZhi: String): Map<String, String> {
        val qlIdx = (LunarCalendar.zhiOrdinal(shiZhi) - 1).mod(6)
        return GONGS.mapIndexed { k, g -> g to HUOSHEN[(qlIdx + k).mod(6)] }.toMap()
    }

    private fun zefa(gua: List<String>): List<String> {
        val posYy = listOf("阳", "阴", "阳")
        return gua.mapIndexed { i, g ->
            val gy = GONG_YY_ZEFA[g]!!
            val pick = if (gy == posYy[i]) "阴" else "阳"
            ZEFA_ZHI[g]!![pick]!!
        }
    }

    /** 渲染完整卦面 —— 与 qigua.py render() 一致 */
    fun render(
        gua: List<String>,
        shiZhi: String? = null,
        meta: List<String>? = null,
        gender: String? = null,
        dayGanZhi: String? = null
    ): String {
        val (tian, di, ren) = gua
        val L = mutableListOf<String>()
        meta?.let { L.addAll(it) }
        L.add("")
        L.add("【卦象】 $tian $di $ren" + (if (shiZhi != null) " ${shiZhi}时" else ""))
        L.add("")
        L.add("── 三宫基本信息 ──")
        val posLabels = listOf("天宫(起因/过去/对方)", "地宫(经过/发展/关系)", "人宫·落宫(结果/当下/自身)")
        gua.forEachIndexed { i, g ->
            val wx = WUXING[g]!! + (if (g in TU_TYPE) "[${TU_TYPE[g]}]" else "")
            L.add("$posLabels[i]: $g | 五行$wx | ${JIXIONG[g]} | ${SISHEN[g]} | ${GONGZHI[g]}")
            L.add("    主数${SHUZI[g]} | 藏支${CANGZHI[g]} | 八卦${BAGUA[g]} | ${FANGWEI[g]}")
        }
        L.add("")
        L.add("── 三宫生克(象义流向) ──")
        data class Rel(val pa: String, val ga: String, val pb: String, val gb: String)
        val pairs = listOf(Rel("天", tian, "地", di), Rel("地", di, "人", ren), Rel("天", tian, "人", ren))
        for ((pa, ga, pb, gb) in pairs) {
            val rel = wxRelation(WUXING[ga]!!, WUXING[gb]!!)
            when (rel) {
                "比和" -> {
                    val same = if (ga == gb) "(同宫)" else "(同五行异宫→小人/竞争之象)"
                    L.add("${pa}宫$ga 与 ${pb}宫$gb: 比和$same")
                }
                "生" -> L.add("${pa}宫$ga(${WUXING[ga]}) 生 ${pb}宫$gb(${WUXING[gb]})")
                "被生" -> L.add("${pb}宫$gb(${WUXING[gb]}) 生 ${pa}宫$ga(${WUXING[ga]})")
                "克" -> L.add("${pa}宫$ga(${WUXING[ga]}) 克 ${pb}宫$gb(${WUXING[gb]})")
                "被克" -> L.add("${pb}宫$gb(${WUXING[gb]}) 克 ${pa}宫$ga(${WUXING[ga]})")
            }
        }
        when (gua.distinct().size) {
            1 -> L.add("※ 三宫相同: 直看落宫即可")
            2 -> {
                val dup = GONGS.first { gua.count { g -> g == it } == 2 }
                L.add("※ 两宫相同($dup): 注意小人/朋友/第三者/竞争之象, 感情占尤要留意")
            }
        }
        L.add("")
        if (shiZhi != null) {
            val swx = ZHI_WUXING[shiZhi]!!
            L.add("── 时辰参断 (时辰 $shiZhi, 五行$swx, ${ZHI_YINYANG[shiZhi]}支) ──")
            val luoWx = WUXING[ren]!!
            when (val rel = wxRelation(luoWx, swx)) {
                "比和" -> {
                    val gy = GONG_YY_TIYONG[ren]!!
                    val zy = ZHI_YINYANG[shiZhi]!!
                    if (gy != zy) L.add("体用: 落宫$ren(${gy}${luoWx}) 与 时辰${shiZhi}(${zy}${swx}) 阴阳相异 → 比助(吉, 两相互助)")
                    else L.add("体用: 落宫$ren(${gy}${luoWx}) 与 时辰${shiZhi}(${zy}${swx}) 阴阳相同 → 比劫(凶, 竞争互损)")
                }
                "生" -> L.add("体用: 体生用(${ren}${luoWx}生时辰${swx}) → 小凶, 泄耗/付出/投资之象")
                "被生" -> L.add("体用: 用生体(时辰${swx}生${ren}${luoWx}) → 大吉, 外力相助, 发展趋势好")
                "克" -> L.add("体用: 体克用(${ren}${luoWx}克时辰${swx}) → 小吉, 主动努力可得")
                "被克" -> L.add("体用: 用克体(时辰${swx}克${ren}${luoWx}) → 大凶, 受阻; 亦可解为考察/压力/克制")
            }
            L.add("各宫得令与长生状态:")
            listOf("天" to gua[0], "地" to gua[1], "人" to gua[2]).forEach { (pos, g) ->
                val gwx = WUXING[g]!!
                val ws = wangshuai(gwx, swx)
                val cs = changsheng(gwx, shiZhi)
                val muFlag = if (MU[gwx] == shiZhi) " ⚠入墓(困顿/收藏/闭塞/昏沉之象)" else ""
                L.add("  ${pos}宫$g($gwx): $ws | 十二长生临[$cs]$muFlag")
            }
            L.add("各宫与时辰生克:")
            listOf("天" to gua[0], "地" to gua[1], "人" to gua[2]).forEach { (pos, g) ->
                val r = wxRelation(WUXING[g]!!, swx)
                val desc = when (r) {
                    "生" -> "${g}生时辰"; "被生" -> "时辰生$g"; "克" -> "${g}克时辰"
                    "被克" -> "时辰克$g"; else -> "与时辰比和"
                }
                L.add("  ${pos}宫$g: $desc")
            }
            val hs = huoshen(shiZhi)
            L.add("活六神(以${shiZhi}时起): " + gua.joinToString(" ") { g -> "${g}临${hs[g]}" })
            L.add("(活神看内在: 性格/心情/实力/潜在想法; 与死六神合宫取象)")
            L.add("")
        }
        val zf = zefa(gua)
        L.add("── 三宫地支择法(主用于应期/路线, 忌在三宫内论刑冲破害合) ──")
        L.add("天${gua[0]}取${zf[0]} | 地${gua[1]}取${zf[1]} | 人${gua[2]}取${zf[2]}")
        L.add("")
        L.add("── 六亲(以落宫为我) ──")
        for ((pos, g) in listOf("天" to tian, "地" to di)) {
            when {
                g == ren -> L.add("  ${pos}宫$g: 兄弟(同宫)")
                WUXING[g] == WUXING[ren] -> L.add("  ${pos}宫$g: 小人(同五行异宫)")
                else -> L.add("  ${pos}宫$g: ${liuqin(WUXING[ren]!!, g, WUXING[g]!!)}")
            }
        }
        if (dayGanZhi != null) {
            L.add("")
            val zhi = dayGanZhi[1].toString()
            L.add("── 日干支: $dayGanZhi (日支$zhi·${ZHI_WUXING[zhi]}, 天气占/应期可用) ──")
        }
        if (!gender.isNullOrBlank() && shiZhi != null) {
            val zt = zhuanTaiji(gua, shiZhi, gender)
            if (zt != null) {
                val (res, tuGong) = zt
                L.add("")
                L.add("── 六亲转太极(土宫取$tuGong) ──")
                res.forEach { (name, arr) -> L.add("  $name: ${arr.joinToString(" ")}") }
            }
        }
        return L.joinToString("\n")
    }

    /** 统一入口：按时起卦（公历自动转农历） */
    fun castByTime(now: LocalDateTime, gender: String? = null): Pair<List<String>, String> {
        val lun = LunarCalendar.fromDateTime(now)
        val gua = castFromLunar(lun.month, lun.day, lun.hourZhi)
        val meta = listOf(
            "公历 ${now.year}-${String.format("%02d", now.monthValue)}-${String.format("%02d", now.dayOfMonth)} ${String.format("%02d", now.hour)}:${String.format("%02d", now.minute)} → 农历${lun.year}年${lun.month}月${lun.day}日" + (if (lun.isLeap) "(闰月,按本月数)" else "") + " ${lun.hourZhi}时",
            "起法: 月上起日、日上起时(正月起大安)",
            "四柱: ${lun.yearGanZhi} ${lun.monthGanZhi} ${lun.dayGanZhi} ${lun.hourGanZhi}"
        )
        val text = render(gua, lun.hourZhi, meta, gender, lun.dayGanZhi)
        return gua to text
    }

    /** 报数起卦 */
    fun castByNumbers(n1: Int, n2: Int, n3: Int, hourZhi: String, gender: String? = null): String {
        val gua = castFromCounts(n1, n2, n3)
        val meta = listOf("数字起卦: $n1 $n2 $n3 | 连续起法(次数从前一落宫续数)")
        return render(gua, hourZhi, meta, gender)
    }

    /** 农历起卦 */
    fun castByLunar(lm: Int, ld: Int, hourZhi: String, gender: String? = null): String {
        val gua = castFromLunar(lm, ld, hourZhi)
        val meta = listOf("农历${lm}月${ld}日 ${hourZhi}时 (月上起日、日上起时)")
        return render(gua, hourZhi, meta, gender)
    }

    /** 直接报三宫卦名 */
    fun castByGua(names: List<String>, hourZhi: String?, gender: String? = null): String {
        val alias = mapOf("留恋" to "留连", "流连" to "留连", "榴莲" to "留连")
        val gua = names.map { alias[it] ?: it }
        return render(gua, hourZhi, listOf("直接输入卦象"), gender)
    }
}
