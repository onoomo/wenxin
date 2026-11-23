package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import java.time.LocalDateTime

/**
 * 紫微斗数 —— Kotlin 1:1 完整移植自吉时雨 iztro (ASTRO 算法, 默认配置)。
 * 坐标系与 iztro 完全一致：所有星曜/宫位数组索引「以寅为 0」(寅=0,卯=1,...,丑=11)。
 * 地支绝对索引(子=0)：abs(rel) = (rel+2)%12。
 * 默认配置: yearDivide=normal(农历年), algorithm=default, dayDivide=forward。
 */
object Ziwei {
    private val GAN = LunarCalendar.TIANGAN
    private val ZHI = LunarCalendar.DIZHI
    private val ZI = listOf("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥")

    private fun fix(i: Int) = ((i % 12) + 12) % 12
    private fun fix10(i: Int) = ((i % 10) + 10) % 10
    /** 相对寅宫索引 -> 地支绝对索引(子=0) */
    private fun abs(rel: Int) = fix(rel + 2)
    /** 地支绝对索引 -> 相对寅宫索引 */
    private fun rel(a: Int) = fix(a - 2)

    // iztro 英文地支 -> 绝对索引(子=0)
    private val EN = mapOf(
        "zi" to 0,"chou" to 1,"yin" to 2,"mao" to 3,"chen" to 4,"si" to 5,
        "woo" to 6,"wei" to 7,"shen" to 8,"you" to 9,"xu" to 10,"hai" to 11
    )
    /** fixEarthlyBranchIndex: 英文地支 -> 相对寅索引 */
    private fun fbi(en: String) = rel(EN[en] ?: 0)

    private val SIHUA: Map<String, List<String>> = mapOf(
        "甲" to listOf("廉贞", "破军", "武曲", "太阳"),
        "乙" to listOf("天机", "天梁", "紫微", "太阴"),
        "丙" to listOf("天同", "天机", "文昌", "廉贞"),
        "丁" to listOf("太阴", "天同", "天机", "巨门"),
        "戊" to listOf("贪狼", "太阴", "右弼", "天机"),
        "己" to listOf("武曲", "贪狼", "天梁", "文曲"),
        "庚" to listOf("太阳", "武曲", "太阴", "天同"),
        "辛" to listOf("巨门", "太阳", "文曲", "文昌"),
        "壬" to listOf("天梁", "紫微", "左辅", "武曲"),
        "癸" to listOf("破军", "巨门", "太阴", "贪狼")
    )
    private val SOUL_BY_ZHI = mapOf(
        "子" to "贪狼","丑" to "巨门","寅" to "禄存","卯" to "文曲","辰" to "廉贞","巳" to "武曲",
        "午" to "破军","未" to "武曲","申" to "廉贞","酉" to "文曲","戌" to "禄存","亥" to "巨门"
    )
    private val BODY_BY_ZHI = mapOf(
        "子" to "火星","丑" to "天相","寅" to "天梁","卯" to "天同","辰" to "文昌","巳" to "天机",
        "午" to "火星","未" to "天相","申" to "天梁","酉" to "天同","戌" to "文昌","亥" to "天机"
    )
    /** iztro PALACES 顺序 (宫位索引 寅=0 起)：命宫,父母,福德,田宅,官禄,仆役,迁移,疾厄,财帛,子女,夫妻,兄弟 */
    val GONG_NAMES = listOf("命宫", "父母", "福德", "田宅", "官禄", "仆役", "迁移", "疾厄", "财帛", "子女", "夫妻", "兄弟")

    data class Star(val name: String, val type: String, val brightness: String, val mutagen: String)
    data class Palace(
        val index: Int, val name: String, val gan: String, val zhi: String, val isBody: Boolean,
        val major: List<Star>, val minor: List<Star>, val adj: List<String>,
        val changsheng: String, val boshi: String, val jiangqian: String, val suiqian: String,
        val decadal: Pair<Int, Int>, val ages: List<Int>
    )
    data class Result(
        val solarDate: String, val lunarDate: String, val chineseDate: String,
        val soul: String, val body: String, val wuxingju: Int, val juName: String, val yearGan: String,
        val zodiac: String, val sign: String, val timeRange: String,
        val ebSoul: String, val ebBody: String, val palaces: List<Palace>, val rows: List<Pair<String, String>>,
        val isMan: Boolean = true
    )
    /** 天干四化：[禄, 权, 科, 忌] 对应星名 */
    fun sihua(gan: String): List<String> = SIHUA[gan] ?: listOf("", "", "", "")
    /** 某星曜落在哪个宫（返回地支） */
    fun starZhi(r: Result, name: String): String? =
        r.palaces.firstOrNull { p -> (p.major + p.minor).any { it.name == name } }?.zhi

    private val WUXING_NUM = mapOf("水二局" to 2, "木三局" to 3, "金四局" to 4, "土五局" to 5, "火六局" to 6)
    private val CS_START = mapOf(2 to "shen", 3 to "hai", 4 to "si", 5 to "shen", 6 to "yin")

    /** 五行局名 getFiveElementsClass */
    private fun fiveElementsClass(gGan: String, gZhi: String): String {
        val table = listOf("木三局","金四局","水二局","火六局","土五局")
        val ganNum = GAN.indexOf(gGan) / 2 + 1
        val zhiNum = (fix(ZI.indexOf(gZhi)) % 6) / 2 + 1
        var index = ganNum + zhiNum
        while (index > 5) index -= 5
        return table[index - 1]
    }

    private fun mutagen(name: String, yearGan: String): String {
        val m = SIHUA[yearGan] ?: return ""
        return when (name) { m[0] -> "禄"; m[1] -> "权"; m[2] -> "科"; m[3] -> "忌"; else -> "" }
    }
    /** 亮度表索引以寅=0 (相对宫位索引), 与 iztro getBrightness 一致 */
    private fun brightness(name: String, relIdx: Int): String = ZiweiData.BRIGHTNESS[name]?.getOrNull(fix(relIdx)) ?: ""

    // ==== 安星函数 (返回相对寅索引) ====

    private fun getLuYangTuoMa(yearGan: String, yearZhi: String): IntArray {
        val ma = fbi(when (yearZhi) {
            "寅","午","戌" -> "shen"; "申","子","辰" -> "yin"
            "巳","酉","丑" -> "hai"; "亥","卯","未" -> "si"; else -> "zi"
        })
        val lu = fbi(when (yearGan) {
            "甲" -> "yin"; "乙" -> "mao"; "丙","戊" -> "si"; "丁","己" -> "woo"
            "庚" -> "shen"; "辛" -> "you"; "壬" -> "hai"; "癸" -> "zi"; else -> "zi"
        })
        return intArrayOf(lu, ma, fix(lu + 1), fix(lu - 1))
    }
    private fun getKuiYue(yearGan: String): Pair<Int, Int> {
        val (k, y) = when (yearGan) {
            "甲","戊","庚" -> "chou" to "wei"; "乙","己" -> "zi" to "shen"
            "辛" -> "woo" to "yin"; "丙","丁" -> "hai" to "you"
            "壬","癸" -> "mao" to "si"; else -> "zi" to "zi"
        }
        return fbi(k) to fbi(y)
    }
    private fun getZuoYou(month: Int) = fix(fbi("chen") + month - 1) to fix(fbi("xu") - (month - 1))
    private fun getChangQu(ti: Int) = fix(fbi("xu") - fix(ti)) to fix(fbi("chen") + fix(ti))
    private fun getKongJie(ti: Int) = fix(fbi("hai") - fix(ti)) to fix(fbi("hai") + fix(ti))
    private fun getHuoLing(yearZhi: String, ti: Int): Pair<Int, Int> {
        val t = fix(ti)
        val (h, l) = when (yearZhi) {
            "寅","午","戌" -> fbi("chou") + t to fbi("mao") + t
            "申","子","辰" -> fbi("yin") + t to fbi("xu") + t
            "巳","酉","丑" -> fbi("mao") + t to fbi("xu") + t
            "亥","未","卯" -> fbi("you") + t to fbi("xu") + t
            else -> 0 to 0
        }
        return fix(h) to fix(l)
    }
    private fun getLuanXi(yearZhi: String): Pair<Int, Int> {
        val h = fix(fbi("mao") - ZI.indexOf(yearZhi))
        return h to fix(h + 6)
    }
    private fun getHuagaiXianchi(yearZhi: String): Pair<Int, Int> = when (yearZhi) {
        "寅","午","戌" -> fbi("xu") to fbi("mao"); "申","子","辰" -> fbi("chen") to fbi("you")
        "巳","酉","丑" -> fbi("chou") to fbi("woo"); "亥","未","卯" -> fbi("wei") to fbi("zi")
        else -> 0 to 0
    }
    private fun getGuGua(yearZhi: String): Pair<Int, Int> = when (yearZhi) {
        "寅","卯","辰" -> fbi("si") to fbi("chou"); "巳","午","未" -> fbi("shen") to fbi("chen")
        "申","酉","戌" -> fbi("hai") to fbi("wei"); "亥","子","丑" -> fbi("yin") to fbi("xu")
        else -> 0 to 0
    }
    private fun getJiangqianStart(yearZhi: String) = when (yearZhi) {
        "寅","午","戌" -> fbi("woo"); "申","子","辰" -> fbi("zi")
        "巳","酉","丑" -> fbi("you"); "亥","卯","未" -> fbi("mao"); else -> 0
    }
    private fun getJieshaAdj(yearZhi: String) = when (yearZhi) {
        "申","子","辰" -> 3; "亥","卯","未" -> 6; "寅","午","戌" -> 9; "巳","酉","丑" -> 0; else -> 0
    }
    private fun getNianjie(yearZhi: String): Int {
        val a = listOf("xu","you","shen","wei","woo","si","chen","mao","yin","chou","zi","hai")
        return fbi(a[ZI.indexOf(yearZhi)])
    }
    private fun getDahao(yearZhi: String): Int {
        val a = listOf("wei","woo","you","shen","hai","xu","chou","zi","mao","yin","si","chen")
        return fix(fbi(a[ZI.indexOf(yearZhi)]) - 2)
    }
    private fun getMonthlyStar(mi: Int): Map<String, Int> {
        val jie = listOf("shen","xu","zi","yin","chen","woo")
        return mapOf(
            "yuejie" to fbi(jie[mi / 2]),
            "tianyao" to fix(fbi("chou") + mi),
            "tianxing" to fix(fbi("you") + mi),
            "yinsha" to fbi(listOf("yin","zi","xu","shen","woo","chen")[mi % 6]),
            "tianyue" to fbi(listOf("xu","si","chen","yin","wei","mao","hai","wei","yin","woo","xu","yin")[mi]),
            "tianwu" to fbi(listOf("si","shen","yin","hai")[mi % 4])
        )
    }
    private fun getYearlyStar(soulRel: Int, bodyRel: Int, yearGan: String, yearZhi: String, isMan: Boolean): Map<String, Int> {
        val yN = ZI.indexOf(yearZhi) // EARTHLY_BRANCHES.indexOf (绝对)
        val gI = GAN.indexOf(yearGan)
        val (huagai, xianchi) = getHuagaiXianchi(yearZhi)
        val (guchen, guasu) = getGuGua(yearZhi)
        val tiancai = fix(soulRel + yN)
        val tianshou = fix(bodyRel + yN)
        val tianchu = fbi(listOf("si","woo","zi","si","woo","shen","yin","woo","you","hai")[gI])
        val posui = fbi(listOf("si","chou","you")[yN % 3])
        val feilian = fbi(listOf("shen","you","xu","si","woo","wei","yin","mao","chen","hai","zi","chou")[yN])
        val longchi = fix(fbi("chen") + yN)
        val fengge = fix(fbi("xu") - yN)
        val tianku = fix(fbi("woo") - yN)
        val tianxu = fix(fbi("woo") + yN)
        val tianguan = fbi(listOf("wei","chen","si","yin","mao","you","hai","you","xu","woo")[gI])
        val tianfu = fbi(listOf("you","shen","zi","hai","mao","yin","woo","si","woo","si")[gI])
        val tiande = fix(fbi("you") + yN)
        val yuede = fix(fbi("si") + yN)
        val tiankong = fix(rel(yN) + 1)
        val jielu = fbi(listOf("shen","woo","chen","yin","zi")[gI % 5])
        val kongwang = fbi(listOf("you","wei","si","mao","chou")[gI % 5])
        var xunkong = fix(rel(yN) + 9 - gI + 1) // fixEarthlyBranchIndex(年支)
        val yinyang = yN % 2
        if (yinyang != xunkong % 2) xunkong = fix(xunkong + 1)
        val jiekong = if (yinyang == 0) jielu else kongwang
        // 天伤/天使: PALACES.indexOf(friendsPalace=仆役=5)+soulIndex ; healthPalace=疾厄=7+soulIndex (默认派无交换)
        val tianshang = fix(5 + soulRel)
        val tianshi = fix(7 + soulRel)
        return mapOf(
            "xianchi" to xianchi, "huagai" to huagai, "guchen" to guchen, "guasu" to guasu,
            "tiancai" to tiancai, "tianshou" to tianshou, "tianchu" to tianchu,
            "posui" to posui, "feilian" to feilian, "longchi" to longchi, "fengge" to fengge,
            "tianku" to tianku, "tianxu" to tianxu, "tianguan" to tianguan, "tianfu" to tianfu,
            "tiande" to tiande, "yuede" to yuede, "tiankong" to tiankong,
            "jielu" to jielu, "kongwang" to kongwang, "xunkong" to xunkong,
            "tianshang" to tianshang, "tianshi" to tianshi, "jiekong" to jiekong,
            "jieshaAdj" to getJieshaAdj(yearZhi), "nianjie" to getNianjie(yearZhi), "dahaoAdj" to getDahao(yearZhi)
        )
    }
    private fun getDailyStar(lunarDay: Int, monthIndex: Int, ti: Int): Map<String, Int> {
        val (zuo, you) = getZuoYou(monthIndex + 1)
        val (chang, qu) = getChangQu(ti)
        val dayIndex = if (ti >= 12) lunarDay else lunarDay - 1
        return mapOf(
            "santai" to fix((zuo + dayIndex) % 12), "bazuo" to fix((you - dayIndex) % 12),
            "enguang" to fix(((chang + dayIndex) % 12) - 1), "tiangui" to fix(((qu + dayIndex) % 12) - 1)
        )
    }
    private fun getYearly12(yearZhi: String): Pair<List<String>, List<String>> {
        val sq = listOf("岁建","晦气","丧门","贯索","官符","小耗","大耗","龙德","白虎","天德","吊客","病符")
        val jq = listOf("将星","攀鞍","岁驿","息神","华盖","劫煞","灾煞","天煞","指背","咸池","月煞","亡神")
        val c = getJiangqianStart(yearZhi)
        val z = rel(ZI.indexOf(yearZhi)) // fixEarthlyBranchIndex
        val sa = MutableList(12){""}; val ja = MutableList(12){""}
        for (i in 0 until 12) sa[fix(z + i)] = sq[i]
        for (i in 0 until 12) ja[fix(c + i)] = jq[i]
        return sa to ja
    }
    private fun getAgeIdx(yearZhi: String) = when (yearZhi) {
        "寅","午","戌" -> fbi("chen"); "申","子","辰" -> fbi("xu")
        "巳","酉","丑" -> fbi("wei"); "亥","卯","未" -> fbi("chou"); else -> 0
    }
    private fun zodiacOf(zhi: String) = listOf("鼠","牛","虎","兔","龙","蛇","马","羊","猴","鸡","狗","猪")[ZI.indexOf(zhi)]
    private fun signOf(m: Int, d: Int): String {
        val md = m*100+d
        return when { md>=1222||md<=119->"摩羯座"; md<220->"水瓶座"; md<321->"双鱼座"; md<421->"白羊座"
            md<521->"金牛座"; md<622->"双子座"; md<723->"巨蟹座"; md<823->"狮子座"; md<923->"处女座"
            md<1024->"天秤座"; md<1123->"天蝎座"; else->"射手座" }
    }
    private fun timeRangeOf(ti: Int) = listOf(
        "00:00~01:00","01:00~03:00","03:00~05:00","05:00~07:00","07:00~09:00","09:00~11:00",
        "11:00~13:00","13:00~15:00","15:00~17:00","17:00~19:00","19:00~21:00","21:00~23:00","23:00~24:00"
    )[ti.coerceIn(0,12)]

    /** timeIndex: 0早子 1丑 ... 11亥 12晚子 */
    fun paipan(dt: LocalDateTime, isMan: Boolean, timeIndex: Int = -1): Result {
        val lun = LunarCalendar.fromDateTime(dt)
        val lunarDay = lun.day
        var effMonth = if (lun.isLeap && lunarDay > 15) lun.month + 1 else lun.month
        if (effMonth > 12) effMonth -= 12
        val ti = if (timeIndex in 0..12) timeIndex else {
            val h = ZI.indexOf(lun.hourZhi); if (h < 0) 0 else h
        }
        // 晚子时(12)按次日的农历日安紫微
        val lunarDayNorm = if (ti == 12) LunarCalendar.fromDateTime(dt.plusDays(1)).day else lunarDay

        val yearGan = lun.yearGanZhi[0].toString()
        val yearZhi = lun.yearGanZhi[1].toString()

        // monthIndex (寅=0): 农历月-1; 闰月下半月+1
        val monthIndex = fix(effMonth - 1)
        // 时辰地支绝对序号 (晚子时=子=0)
        val timeZhiAbs = if (ti == 12) 0 else ti
        // 命宫/身宫 (相对寅)
        val soulRel = fix(monthIndex - timeZhiAbs)
        val bodyRel = fix(monthIndex + timeZhiAbs)
        val tigerStart = GAN.indexOf(when (yearGan) {
            "甲","己" -> "丙"; "乙","庚" -> "戊"; "丙","辛" -> "庚"; "丁","壬" -> "壬"; "戊","癸" -> "甲"; else -> "丙"
        })
        val mingGanAbs = fix10(tigerStart + soulRel)
        val juName = fiveElementsClass(GAN[mingGanAbs], ZI[abs(soulRel)])
        val ju = WUXING_NUM[juName] ?: 5

        // 紫微落宫 getStartIndex (相对寅)
        var offset = -1; var q: Int; var dd: Int
        do { offset++; dd = lunarDayNorm + offset; q = dd / ju; dd = dd % ju } while (dd != 0)
        var ziweiRel = q % 12 - 1
        ziweiRel = fix(if (offset % 2 == 0) ziweiRel + offset else ziweiRel - offset)
        val tianfuRel = fix(12 - ziweiRel)

        // 星盘容器: 相对寅索引 (0=寅)
        val majors = List(12){mutableListOf<Star>()}
        val minors = List(12){mutableListOf<Star>()}
        val adjs = List(12){mutableListOf<String>()}
        fun maj(idx: Int, name: String) { val r=fix(idx); majors[r].add(Star(name,"major",brightness(name,r),mutagen(name,yearGan))) }
        fun min(idx: Int, name: String, type: String="soft") { val r=fix(idx); minors[r].add(Star(name,type,brightness(name,r),mutagen(name,yearGan))) }
        fun adj(idx: Int, name: String) { adjs[fix(idx)].add(name) }

        val zArr = listOf("紫微","天机","","太阳","武曲","天同","","","廉贞")
        zArr.forEachIndexed { n, nm -> if (nm.isNotEmpty()) maj(ziweiRel - n, nm) }
        val fArr = listOf("天府","太阴","贪狼","巨门","天相","天梁","七杀","","","","破军")
        fArr.forEachIndexed { n, nm -> if (nm.isNotEmpty()) maj(tianfuRel + n, nm) }

        val (zuo, you) = getZuoYou(monthIndex + 1)
        val (chang, qu) = getChangQu(ti)
        val (kui, yue) = getKuiYue(yearGan)
        val (lu, ma, yang, tuo) = getLuYangTuoMa(yearGan, yearZhi)
        val (kong, jie) = getKongJie(ti)
        val (huo, ling) = getHuoLing(yearZhi, ti)
        min(zuo,"左辅"); min(you,"右弼"); min(chang,"文昌"); min(qu,"文曲")
        min(kui,"天魁"); min(yue,"天钺"); min(lu,"禄存","lucun"); min(ma,"天马","tianma")
        min(kong,"地空","tough"); min(jie,"地劫","tough"); min(huo,"火星","tough"); min(ling,"铃星","tough")
        min(yang,"擎羊","tough"); min(tuo,"陀罗","tough")

        val yearly = getYearlyStar(soulRel, bodyRel, yearGan, yearZhi, isMan)
        val monthly = getMonthlyStar(monthIndex)
        val daily = getDailyStar(lunarDay, monthIndex, ti)
        val (hong, tianxi) = getLuanXi(yearZhi)
        adj(hong,"红鸾"); adj(tianxi,"天喜")
        adj(monthly["tianyao"]!!,"天姚"); adj(yearly["xianchi"]!!,"咸池")
        adj(monthly["yuejie"]!!,"解神")
        adj(daily["santai"]!!,"三台"); adj(daily["bazuo"]!!,"八座")
        adj(daily["enguang"]!!,"恩光"); adj(daily["tiangui"]!!,"天贵")
        adj(yearly["longchi"]!!,"龙池"); adj(yearly["fengge"]!!,"凤阁")
        adj(yearly["tiancai"]!!,"天才"); adj(yearly["tianshou"]!!,"天寿")
        adj(fix(fbi("woo") + fix(ti)),"台辅"); adj(fix(fbi("yin") + fix(ti)),"封诰")
        adj(monthly["tianwu"]!!,"天巫"); adj(yearly["huagai"]!!,"华盖")
        adj(yearly["tianguan"]!!,"天官"); adj(yearly["tianfu"]!!,"天福")
        adj(yearly["tianchu"]!!,"天厨"); adj(monthly["tianyue"]!!,"天月")
        adj(yearly["tiande"]!!,"天德"); adj(yearly["yuede"]!!,"月德")
        adj(yearly["tiankong"]!!,"天空"); adj(yearly["xunkong"]!!,"旬空")
        adj(yearly["jielu"]!!,"截路"); adj(yearly["kongwang"]!!,"空亡")
        adj(yearly["guchen"]!!,"孤辰"); adj(yearly["guasu"]!!,"寡宿")
        adj(yearly["feilian"]!!,"蜚廉"); adj(yearly["posui"]!!,"破碎")
        adj(monthly["tianxing"]!!,"天刑"); adj(monthly["yinsha"]!!,"阴煞")
        adj(yearly["tianku"]!!,"天哭"); adj(yearly["tianxu"]!!,"天虚")
        adj(yearly["tianshi"]!!,"天使"); adj(yearly["tianshang"]!!,"天伤")
        adj(yearly["nianjie"]!!,"年解")

        // 长生/博士/将前/岁前 (相对寅索引数组)
        val shun = (isMan && ZI.indexOf(yearZhi)%2==0) || (!isMan && ZI.indexOf(yearZhi)%2==1)
        val csStart = fbi(CS_START[ju]!!)
        val csNames = listOf("长生","沐浴","冠带","临官","帝旺","衰","病","死","墓","绝","胎","养")
        val csA = MutableList(12){""}; csNames.forEachIndexed { i,nm -> csA[fix(if(shun)csStart+i else csStart-i)]=nm }
        val bsNames = listOf("博士","力士","青龙","小耗","将军","奏书","飞廉","喜神","病符","大耗","伏兵","官府")
        val bsA = MutableList(12){""}; bsNames.forEachIndexed { i,nm -> bsA[fix(if(shun)lu+i else lu-i)]=nm }
        val (sqA, jqA) = getYearly12(yearZhi)

        // 大限 (相对寅)
        val decA = MutableList(12){0 to 0}
        for (i in 0 until 12) {
            val idx = fix(if (shun) soulRel+i else soulRel-i)
            val start = ju + 10*i
            decA[idx] = start to start+9
        }
        // 小限年龄
        val ageIdx = getAgeIdx(yearZhi)
        val agesA = MutableList(12){ listOf<Int>() }
        for (i in 0 until 12) {
            val age = (0 until 10).map { 12*it+i+1 }
            agesA[fix(if(isMan)ageIdx+i else ageIdx-i)] = age
        }

        val palaces = mutableListOf<Palace>()
        for (i in 0 until 12) {
            val gh = fix10(mingGanAbs - soulRel + i)
            palaces.add(Palace(i, GONG_NAMES[i], GAN[gh], ZI[abs(i)], bodyRel==i,
                majors[i], minors[i], adjs[i], csA[i], bsA[i], jqA[i], sqA[i], decA[i], agesA[i]))
        }
        val rows = palaces.map { p ->
            val s = (p.major+p.minor).joinToString(" ") { st ->
                st.name + (if(st.brightness.isNotEmpty())"(${st.brightness})" else "") + (if(st.mutagen.isNotEmpty())"[${st.mutagen}]" else "")
            }
            "${p.name}${p.gan}${p.zhi}" to (if(p.isBody)"身 " else "")+s
        }
        val soulZhi = ZI[abs(soulRel)]; val bodyZhi = ZI[abs(bodyRel)]
        return Result(
            "${dt.year}-${dt.monthValue}-${dt.dayOfMonth}", "${lun.year}年${lun.month}月${lun.day}日",
            "${lun.yearGanZhi} ${lun.monthGanZhi} ${lun.dayGanZhi} ${lun.hourGanZhi}",
            SOUL_BY_ZHI[soulZhi]?:"", BODY_BY_ZHI[yearZhi]?:"", ju, juName, yearGan,
            zodiacOf(yearZhi), signOf(dt.monthValue,dt.dayOfMonth), timeRangeOf(ti),
            soulZhi, bodyZhi, palaces, rows, isMan
        )
    }
}
