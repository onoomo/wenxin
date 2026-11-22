package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import java.time.LocalDateTime

/** 六爻排盘 —— 1:1 移植自吉时雨 6yao.js (AGPL-3.0, xianbo.chen@gmail.com) */
object LiuYaoJishiyu {
    val GUA_CODE = mapOf(
        "111" to "乾",
        "000" to "坤",
        "001" to "震",
        "010" to "坎",
        "100" to "艮",
        "110" to "巽",
        "101" to "离",
        "011" to "兑",
    )
    val ZHI_WX = mapOf(
        "子" to "水",
        "丑" to "土",
        "寅" to "木",
        "卯" to "木",
        "辰" to "土",
        "巳" to "火",
        "午" to "火",
        "未" to "土",
        "申" to "金",
        "酉" to "金",
        "戌" to "土",
        "亥" to "水",
    )
    val GUA_WX = mapOf(
        "乾" to "金",
        "兑" to "金",
        "艮" to "土",
        "坤" to "土",
        "震" to "木",
        "巽" to "木",
        "坎" to "水",
        "离" to "火",
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
    val GAN_MAP = mapOf(
        "乾" to Pair("甲", "壬"),
        "坤" to Pair("乙", "癸"),
        "艮" to Pair("丙", "丙"),
        "兑" to Pair("丁", "丁"),
        "坎" to Pair("戊", "戊"),
        "离" to Pair("己", "己"),
        "震" to Pair("庚", "庚"),
        "巽" to Pair("辛", "辛"),
    )
    val ZHI_MAP = mapOf(
        "乾" to Pair(arrayOf("子", "寅", "辰"), arrayOf("午", "申", "戌")),
        "兑" to Pair(arrayOf("巳", "卯", "丑"), arrayOf("亥", "酉", "未")),
        "离" to Pair(arrayOf("卯", "丑", "亥"), arrayOf("酉", "未", "巳")),
        "震" to Pair(arrayOf("子", "寅", "辰"), arrayOf("午", "申", "戌")),
        "巽" to Pair(arrayOf("丑", "亥", "酉"), arrayOf("未", "巳", "卯")),
        "坎" to Pair(arrayOf("寅", "辰", "午"), arrayOf("申", "戌", "子")),
        "艮" to Pair(arrayOf("辰", "午", "申"), arrayOf("戌", "子", "寅")),
        "坤" to Pair(arrayOf("未", "巳", "卯"), arrayOf("丑", "亥", "酉")),
    )
    data class WxRel(val sheng: String, val ke: String, val beiSheng: String, val beiKe: String)
    val WUXING = mapOf(
        "木" to WxRel("火", "土", "水", "金"),
        "火" to WxRel("土", "金", "木", "水"),
        "土" to WxRel("金", "水", "火", "木"),
        "金" to WxRel("水", "木", "土", "火"),
        "水" to WxRel("木", "火", "金", "土"),
    )
    val GUIHUN = arrayOf(
        Pair("离", "乾"),
        Pair("震", "兑"),
        Pair("乾", "离"),
        Pair("兑", "震"),
        Pair("艮", "巽"),
        Pair("坤", "坎"),
        Pair("巽", "艮"),
        Pair("坎", "坤"),
    )
    val YOUHUN = arrayOf(
        Pair("离", "坤"),
        Pair("震", "艮"),
        Pair("乾", "坎"),
        Pair("兑", "巽"),
        Pair("艮", "震"),
        Pair("坤", "离"),
        Pair("巽", "兑"),
        Pair("坎", "乾"),
    )
    val LIUCHONG = arrayOf(
        Pair("乾", "乾"),
        Pair("坎", "坎"),
        Pair("艮", "艮"),
        Pair("震", "震"),
        Pair("巽", "巽"),
        Pair("离", "离"),
        Pair("坤", "坤"),
        Pair("兑", "兑"),
        Pair("震", "乾"),
        Pair("乾", "震"),
    )
    val LIUHE = arrayOf(
        Pair("坤", "震"),
        Pair("震", "坤"),
        Pair("坎", "兑"),
        Pair("兑", "坎"),
        Pair("艮", "离"),
        Pair("离", "艮"),
        Pair("坤", "乾"),
        Pair("乾", "坤"),
    )
    /** 神煞地支映射(27个纯查表神煞) */
    val SHENSHA = mapOf(
        "驿马" to mapOf("申" to "寅", "子" to "寅", "辰" to "寅", "寅" to "申", "午" to "申", "戌" to "申", "亥" to "巳", "卯" to "巳", "未" to "巳", "巳" to "亥", "酉" to "亥", "丑" to "亥"),
        "桃花" to mapOf("申" to "酉", "子" to "酉", "辰" to "酉", "寅" to "卯", "午" to "卯", "戌" to "卯", "巳" to "午", "酉" to "午", "丑" to "午", "亥" to "子", "卯" to "子", "未" to "子"),
        "日禄" to mapOf("甲" to "寅", "乙" to "卯", "丙" to "巳", "丁" to "午", "戊" to "巳", "己" to "午", "庚" to "申", "辛" to "酉", "壬" to "亥", "癸" to "子"),
        "日德" to mapOf("甲" to "寅", "乙" to "申", "丙" to "巳", "丁" to "亥", "戊" to "巳", "己" to "寅", "庚" to "申", "辛" to "巳", "壬" to "亥", "癸" to "巳"),
        "破碎" to mapOf("寅" to "酉", "申" to "酉", "巳" to "酉", "亥" to "酉", "子" to "巳", "午" to "巳", "卯" to "巳", "酉" to "巳", "辰" to "丑", "戌" to "丑", "丑" to "丑", "未" to "丑"),
        "文昌" to mapOf("甲" to "已", "乙" to "午", "丙" to "申", "戊" to "申", "庚" to "亥", "辛" to "子", "丁" to "酉", "己" to "酉", "壬" to "寅", "癸" to "卯"),
        "天医" to mapOf("寅" to "辰", "卯" to "巳", "辰" to "午", "巳" to "未", "午" to "申", "未" to "酉", "申" to "戌", "酉" to "亥", "戌" to "子", "亥" to "丑", "子" to "寅", "丑" to "卯"),
        "地医" to mapOf("寅" to "戌", "卯" to "亥", "辰" to "子", "巳" to "丑", "午" to "寅", "未" to "卯", "申" to "辰", "酉" to "巳", "戌" to "午", "亥" to "未", "子" to "申", "丑" to "酉"),
        "天马" to mapOf("寅" to "午", "卯" to "申", "辰" to "戌", "巳" to "子", "午" to "寅", "未" to "辰", "申" to "午", "酉" to "申", "戌" to "戌", "亥" to "子", "子" to "寅", "丑" to "辰"),
        "天喜" to mapOf("寅" to "戌", "卯" to "戌", "辰" to "戌", "巳" to "丑", "午" to "丑", "未" to "丑", "申" to "辰", "酉" to "辰", "戌" to "辰", "亥" to "未", "子" to "未", "丑" to "未"),
        "月德" to mapOf("寅" to "丙", "午" to "丙", "戌" to "丙", "申" to "壬", "子" to "壬", "辰" to "壬", "亥" to "甲", "卯" to "甲", "未" to "甲", "巳" to "庚", "酉" to "庚", "丑" to "庚"),
        "灾煞" to mapOf("寅" to "子", "午" to "子", "戌" to "子", "申" to "午", "子" to "午", "辰" to "午", "亥" to "酉", "卯" to "酉", "未" to "酉", "巳" to "卯", "酉" to "卯", "丑" to "卯"),
        "劫煞" to mapOf("寅" to "亥", "午" to "亥", "戌" to "亥", "申" to "巳", "子" to "巳", "辰" to "巳", "亥" to "申", "卯" to "申", "未" to "申", "巳" to "寅", "酉" to "寅", "丑" to "寅"),
        "将星" to mapOf("申" to "子", "子" to "子", "辰" to "子", "寅" to "午", "午" to "午", "戌" to "午", "巳" to "酉", "酉" to "酉", "丑" to "酉", "亥" to "卯", "卯" to "卯", "未" to "卯"),
        "华盖" to mapOf("申" to "辰", "子" to "辰", "辰" to "辰", "寅" to "戌", "午" to "戌", "戌" to "戌", "巳" to "丑", "酉" to "丑", "丑" to "丑", "亥" to "未", "卯" to "未", "未" to "未"),
        "羊刃" to mapOf("甲" to "卯", "乙" to "寅", "丙" to "午", "丁" to "巳", "戊" to "午", "己" to "巳", "庚" to "酉", "辛" to "申", "壬" to "子", "癸" to "亥"),
        "生气" to mapOf("寅" to "子", "卯" to "丑", "辰" to "寅", "巳" to "卯", "午" to "辰", "未" to "巳", "申" to "午", "酉" to "未", "戌" to "申", "亥" to "酉", "子" to "戌", "丑" to "亥"),
        "死气" to mapOf("寅" to "午", "卯" to "未", "辰" to "申", "巳" to "酉", "午" to "戌", "未" to "亥", "申" to "子", "酉" to "丑", "戌" to "寅", "亥" to "卯", "子" to "辰", "丑" to "巳"),
        "亡神" to mapOf("寅" to "巳", "卯" to "寅", "辰" to "亥", "巳" to "申", "午" to "巳", "未" to "寅", "申" to "亥", "酉" to "申", "戌" to "巳", "亥" to "寅", "子" to "亥", "丑" to "申"),
        "官符" to mapOf("寅" to "午", "卯" to "未", "辰" to "申", "巳" to "酉", "午" to "戌", "未" to "亥", "申" to "子", "酉" to "丑", "戌" to "寅", "亥" to "卯", "子" to "辰", "丑" to "巳"),
        "游都" to mapOf("甲" to "丑", "己" to "丑", "乙" to "子", "庚" to "子", "丙" to "寅", "辛" to "寅", "丁" to "巳", "壬" to "巳", "戊" to "申", "癸" to "申"),
        "飞符" to mapOf("甲" to "巳", "己" to "午", "乙" to "辰", "庚" to "未", "丙" to "卯", "辛" to "申", "丁" to "寅", "壬" to "酉", "戊" to "丑", "癸" to "戌"),
        "血支" to mapOf("寅" to "丑", "卯" to "寅", "辰" to "卯", "巳" to "辰", "午" to "巳", "未" to "午", "申" to "未", "酉" to "申", "戌" to "酉", "亥" to "戌", "子" to "亥", "丑" to "子"),
        "寡宿" to mapOf("寅" to "丑", "卯" to "丑", "辰" to "丑", "巳" to "辰", "午" to "辰", "未" to "辰", "申" to "未", "酉" to "未", "戌" to "未", "亥" to "戌", "子" to "戌", "丑" to "戌"),
        "孤辰" to mapOf("寅" to "巳", "卯" to "巳", "辰" to "巳", "巳" to "申", "午" to "申", "未" to "申", "申" to "亥", "酉" to "亥", "戌" to "亥", "亥" to "寅", "子" to "寅", "丑" to "寅"),
        "死神" to mapOf("寅" to "巳", "卯" to "午", "辰" to "未", "巳" to "申", "午" to "酉", "未" to "戌", "申" to "亥", "酉" to "子", "戌" to "丑", "亥" to "寅", "子" to "卯", "丑" to "辰"),
    )

    // ===== 装卦算法 =====
    data class Yao(
        var yao: Int, var type: String, var isShi: Boolean = false, var isYing: Boolean = false,
        var gan: String = "", var zhi: String = "", var liuQin: String = "", var fushen: String = ""
    )
    data class ShenSha(val name: String, val zhi: String)
    data class Result(
        val baseName: String, val bianName: String, val alias: String,
        val baseGong: String, val bianGong: String,
        val yaoInfo: List<Yao>, val bianYao: List<Yao>, val liuShen: List<String>,
        val shenSha: List<ShenSha>, val hasBian: Boolean, val siZhu: List<String>
    )

    private val TIANGAN = LunarCalendar.TIANGAN
    private val DIZHI = LunarCalendar.DIZHI

    private fun guaName(list: List<Yao>): Pair<String, String> {
        val inner = list.take(3).map { it.yao }.reversed().joinToString("")
        val outer = list.drop(3).map { it.yao }.reversed().joinToString("")
        return Pair(GUA_CODE[inner] ?: "乾", GUA_CODE[outer] ?: "乾")
    }

    private fun fullName(list: List<Yao>): String = GUA64[list.map { it.yao }.reversed().joinToString("")] ?: ""

    private fun calcShi(list: List<Yao>): Int {
        val (inner, outer) = guaName(list)
        val i = GUA_CODE.entries.first { it.value == inner }.key.toCharArray()
        val o = GUA_CODE.entries.first { it.value == outer }.key.toCharArray()
        if (i[0]==o[0] && i[1]==o[1] && i[2]==o[2]) return 5
        if (i[0]!=o[0] && i[1]!=o[1] && i[2]!=o[2]) return 2
        if (i[0]==o[0] && i[1]!=o[1] && i[2]!=o[2]) return 1
        if (i[0]!=o[0] && i[1]==o[1] && i[2]==o[2]) return 4
        if (i[0]!=o[0] && i[1]!=o[1] && i[2]==o[2]) return 3
        if (i[0]==o[0] && i[1]==o[1] && i[2]!=o[2]) return 0
        if (i[0]!=o[0] && i[1]==o[1] && i[2]!=o[2]) return 3
        if (i[0]==o[0] && i[1]!=o[1] && i[2]==o[2]) return 2
        return 5
    }

    private fun setupGanzhi(list: MutableList<Yao>) {
        val (inner, outer) = guaName(list)
        for (k in 0..5) {
            val gua = if (k<3) inner else outer
            val pos = if (k<3) "inner" else "outer"
            val idx = if (k<3) k else k-3
            val gm = GAN_MAP[gua]!!; val zm = ZHI_MAP[gua]!!
            list[k].gan = if (pos=="inner") gm.first else gm.second
            val zhis = if (pos=="inner") zm.first else zm.second
            list[k].zhi = zhis[idx]
        }
    }

    private fun calcGuaGong(list: List<Yao>, inner: String, outer: String): String {
        if (GUIHUN.any { it.first==inner && it.second==outer }) return inner
        val shi = list.indexOfFirst { it.isShi }
        return if (shi in listOf(0,1,2,5)) outer
        else {
            val rev = list.take(3).map { if(it.yao==1) 0 else 1 }.reversed().joinToString("")
            GUA_CODE[rev] ?: outer
        }
    }

    private fun liuQinOf(gong: String, zhi: String): String {
        val self = GUA_WX[gong] ?: return ""
        val target = ZHI_WX[zhi] ?: return ""
        val rel = WUXING[self] ?: return ""
        return when {
            rel.sheng == target -> "孙"
            rel.beiSheng == target -> "父"
            rel.ke == target -> "财"
            rel.beiKe == target -> "官"
            self == target -> "兄"
            else -> ""
        }
    }

    private fun installLiuShen(riGan: String): List<String> {
        val order = arrayOf("青龙","朱雀","勾陈","螣蛇","白虎","玄武")
        val start = mapOf("甲" to 0,"乙" to 0,"丙" to 1,"丁" to 1,"戊" to 2,"己" to 3,"庚" to 4,"辛" to 4,"壬" to 5,"癸" to 5)[riGan] ?: 0
        return (0..5).map { order[(start+it)%6] }
    }

    /** yaoList: 六爻字符串数组, 初爻到上爻。类型: "1"阳 "0"阴 "1o"老阳 "0x"老阴 */
    fun cast(dt: LocalDateTime, yaoList: List<String>): Result {
        val lun = LunarCalendar.fromDateTime(dt)
        val yearGz = lun.yearGanZhi; val monthGz = lun.monthGanZhi
        val dayGz = lun.dayGanZhi; val hourGz = lun.hourGanZhi
        val riGan = dayGz.substring(0,1); val riZhi = dayGz.substring(1)
        val yueZhi = monthGz.substring(1); val nianZhi = yearGz.substring(1)

        val base = yaoList.map { tp -> Yao(if (tp=="1"||tp=="1o") 1 else 0, tp) }.toMutableList()
        val hasBian = yaoList.any { it.contains("o") || it.contains("x") }
        val bian = yaoList.map { tp -> Yao(if (tp=="1o") 0 else if(tp=="0x") 1 else if(tp=="1") 1 else 0, tp) }.toMutableList()

        val (inner, outer) = guaName(base)
        val shi = calcShi(base)
        val ying = if (shi>2) shi%3 else shi%3+3
        base[shi].isShi = true; base[ying].isYing = true
        setupGanzhi(base); setupGanzhi(bian)
        val bShi = calcShi(bian); val bYing = if(bShi>2) bShi%3 else bShi%3+3
        bian[bShi].isShi = true; bian[bYing].isYing = true

        val gong = calcGuaGong(base, inner, outer)
        base.forEach { it.liuQin = liuQinOf(gong, it.zhi) }
        bian.forEach { it.liuQin = liuQinOf(gong, it.zhi) }
        var alias = ""
        if (GUIHUN.any{it.first==inner&&it.second==outer}) alias="归魂"
        if (YOUHUN.any{it.first==inner&&it.second==outer}) alias="游魂"
        if (LIUHE.any{it.first==inner&&it.second==outer}) alias="六合"
        if (LIUCHONG.any{it.first==inner&&it.second==outer}) alias="六冲"

        // 伏神: 本卦宫首卦六亲补全
        val gongCode = GUA_CODE.entries.firstOrNull{it.value==gong}?.key ?: "111"
        val gongYao = (gongCode+gongCode).map { Yao(if(it=='1') 1 else 0, it.toString()) }.reversed().toMutableList()
        setupGanzhi(gongYao)
        gongYao.forEach { it.liuQin = liuQinOf(gong, it.zhi) }
        for (q in listOf("孙","父","兄","财","官")) {
            if (base.none { it.liuQin==q }) {
                gongYao.forEachIndexed { i, y -> if (y.liuQin==q) base[i].fushen = "$q${y.gan}${y.zhi}" }
            }
        }

        // 神煞(严格按 6yao.js 的顺序与取键 1:1)
        val ss = mutableListOf<ShenSha>()
        fun tab(name: String, key: String): String = SHENSHA[name]?.get(key) ?: ""
        // 卦身(特殊): 阳世起子阴起午, 从初爻数到世爻
        val guaShenZhi = run {
            val shiYao = base[shi]; val start = if (shiYao.yao==1) "子" else "午"
            DIZHI[(DIZHI.indexOf(start)+shi)%12]
        }
        ss.add(ShenSha("卦身", guaShenZhi))
        ss.add(ShenSha("驿马", tab("驿马", riZhi)))     // 日支
        ss.add(ShenSha("桃花", tab("桃花", riZhi)))     // 日支
        ss.add(ShenSha("日禄", tab("日禄", riGan)))     // 日干
        ss.add(ShenSha("日德", tab("日德", riGan)))     // 日干
        ss.add(ShenSha("破碎", tab("破碎", riZhi)))     // 日支
        ss.add(ShenSha("文昌", tab("文昌", riGan)))     // 日干
        // 贵人: 原版 guiren(riZhu[0]) 漏传时辰参数, 实际恒取阴贵(1:1 复刻)
        val guirenYin = mapOf("甲" to "未","戊" to "未","乙" to "子","己" to "子","丙" to "酉","丁" to "酉","壬" to "巳","癸" to "巳","庚" to "寅","辛" to "寅")
        ss.add(ShenSha("贵人", guirenYin[riGan] ?: ""))
        ss.add(ShenSha("天医", tab("天医", yueZhi)))    // 月支
        ss.add(ShenSha("地医", tab("地医", yueZhi)))    // 月支
        ss.add(ShenSha("天马", tab("天马", yueZhi)))    // 月支
        ss.add(ShenSha("天喜", tab("天喜", yueZhi)))    // 月支
        ss.add(ShenSha("月德", tab("月德", yueZhi)))    // 月支
        ss.add(ShenSha("灾煞", tab("灾煞", nianZhi)))   // 年支
        ss.add(ShenSha("劫煞", tab("劫煞", nianZhi)))   // 年支
        ss.add(ShenSha("将星", tab("将星", riZhi)))     // 日支
        ss.add(ShenSha("华盖", tab("华盖", riZhi)))     // 日支
        // 香闺/床帐(特殊): 卦身五行所克者为香闺, 所生者为床帐
        val wuxingKe = setOf("金木","木土","土水","水火","火金")
        val wuxingSheng = setOf("金水","水木","木火","火土","土金")
        val guaShenWx = ZHI_WX[guaShenZhi] ?: ""
        var xg = ""; var cz = ""
        for (y in base) {
            val yw = ZHI_WX[y.zhi] ?: ""
            if (guaShenWx + yw in wuxingKe) xg += y.zhi
            if (guaShenWx + yw in wuxingSheng) cz += y.zhi
        }
        ss.add(ShenSha("香闺", xg.ifEmpty { "无" }))
        ss.add(ShenSha("床帐", cz.ifEmpty { "无" }))
        ss.add(ShenSha("羊刃", tab("羊刃", riGan)))     // 日干
        ss.add(ShenSha("生气", tab("生气", yueZhi)))    // 月支
        ss.add(ShenSha("死气", tab("死气", yueZhi)))    // 月支
        ss.add(ShenSha("亡神", tab("亡神", yueZhi)))    // 月支
        ss.add(ShenSha("病符", DIZHI[(DIZHI.indexOf(nianZhi)-1+12)%12])) // 年支
        ss.add(ShenSha("丧门", DIZHI[(DIZHI.indexOf(nianZhi)+2)%12]))    // 年支
        ss.add(ShenSha("吊客", DIZHI[(DIZHI.indexOf(nianZhi)-2+12)%12])) // 年支
        ss.add(ShenSha("官符", tab("官符", yueZhi)))    // 月支
        ss.add(ShenSha("游都", tab("游都", riGan)))     // 日干
        ss.add(ShenSha("飞符", tab("飞符", riGan)))     // 日干
        ss.add(ShenSha("血支", tab("血支", yueZhi)))    // 月支
        ss.add(ShenSha("孤辰", tab("孤辰", yueZhi)))    // 月支
        ss.add(ShenSha("寡宿", tab("寡宿", yueZhi)))    // 月支
        ss.add(ShenSha("死神", tab("死神", yueZhi)))    // 月支

        return Result(fullName(base), fullName(bian), alias, gong, calcGuaGong(bian, guaName(bian).first, guaName(bian).second),
            base, bian, installLiuShen(riGan), ss, hasBian,
            listOf(yearGz, monthGz, dayGz, hourGz))
    }
}