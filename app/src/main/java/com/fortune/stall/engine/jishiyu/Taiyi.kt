package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.util.SolarTerm
import java.time.LocalDateTime
import kotlin.math.ceil
import kotlin.math.floor

/**
 * 太乙神数 —— 1:1 移植自吉时雨 taiyi.js (AGPL-3.0, xianbo.chen@gmail.com)
 * 积数、太乙宫位、主客算、十六神、十神。常规盘(非刻盘)。
 */
object Taiyi {
    private val GAN = LunarCalendar.TIANGAN
    private val ZHI = LunarCalendar.DIZHI

    /**
     * 模拟 JS String.substring 的容错语义：越界/负数会被裁剪，起止倒置会交换。
     * 原版 taiyi.js 大量使用 substring，JS 对负数/超界返回空串不报错；
     * Kotlin 原生 substring 会抛 StringIndexOutOfBoundsException 导致闪退。
     * 这里统一走容错版本，保证任何输入时间都不崩。
     */
    private fun String.jsSub(from: Int, to: Int): String {
        val len = length
        var a = from.coerceIn(0, len)
        var b = to.coerceIn(0, len)
        if (a > b) { val t = a; a = b; b = t }
        return substring(a, b)
    }

    private fun zjzhuan8(d: Int): Int = mapOf(8 to 1, 3 to 2, 4 to 3, 9 to 4, 2 to 5, 7 to 6, 6 to 7, 5 to 9)[d] ?: 8
    private fun zhuan8(d: String): Int = mapOf(
        "地主" to 8, "阳德" to 8, "和德" to 3, "吕申" to 3, "高从" to 4, "太阳" to 4,
        "大炅" to 9, "大神" to 9, "大威" to 2, "天道" to 2, "大武" to 7, "武德" to 7,
        "太簇" to 6, "阴主" to 6)[d] ?: 1
    private fun ggwy(wys: Int): Int { val r = Math.floorMod(8 - wys, 8); return if (r == 0) 8 else r }
    private fun gwxc(g: Int): Int = (ceil(g / 2.0).toInt() % 5) + 1
    private fun yys(sw: Int): Int = kotlin.math.abs(sw) % 2

    private fun xdj(y: String, x: String): Int {
        val wx = mapOf("太乙" to 2, "小游" to 2, "客参将" to 2, "君基" to 4, "文昌" to 4, "臣基" to 4,
            "五福" to 4, "民基" to 4, "计神" to 4, "地乙" to 4, "始击" to 3, "飞符" to 3,
            "主大将" to 5, "天乙" to 5, "客大将" to 1, "主参将" to 1, "四神" to 1)[x] ?: 2
        val wang = mapOf(1 to "申子辰亥", 2 to "亥卯未寅", 3 to "寅午戌巳", 4 to "辰丑戌", 5 to "酉丑申巳")
        val xiu = mapOf(1 to "丑戌未", 2 to "申酉", 3 to "亥子", 4 to "寅卯", 5 to "午")
        if (wang[wx]?.contains(y) == true) return 1
        if (xiu[wx]?.contains(y) == true) return 3
        return 2
    }

    private fun slxwx(g: String): String = mapOf(
        "太乙" to "12", "小游" to "12", "君基" to "14", "文昌" to "14", "臣基" to "14", "五福" to "14",
        "始击" to "13", "主大将" to "15", "客大将" to "11", "民基" to "04", "计神" to "04", "地乙" to "04",
        "主参将" to "01", "四神" to "01", "客参将" to "02", "天乙" to "05", "飞符" to "03")[g] ?: ""

    private fun shishenl(twx: Int, rgwx: Int, twyy: Int, rgyy: Int): String {
        val names = arrayOf(arrayOf("比", "劫"), arrayOf("枭", "印"), arrayOf("杀", "官"), arrayOf("食", "伤"), arrayOf("才", "财"))
        val diff = Math.floorMod(twx - rgwx, 5)
        return names[diff][if (twyy == rgyy) 0 else 1]
    }

    private val SQSP = arrayOf("", "太乙", "小游", "君基", "文昌", "臣基", "五福", "始击", "主大将",
        "客大将", "民基", "计神", "地乙", "主参将", "四神", "客参将", "天乙", "飞符")

    /**
     * 太乙十六神盘（16神层 + 16活神层）。
     * gods: 16 个方位的太乙十六神固定名（含宫数/地支），顺时针。
     * active: 各方位上"活神"的十神标签（太乙/主大/文昌…+十神），可能多行。
     * godShen/godActive 一一对应，长度 16。
     */
    data class Shen16(val gods: List<String>, val active: List<String>)

    data class Result(
        val date: String, val siZhu: List<String>, val jieqi: String,
        val yangDun: Boolean, val jushu: Int, val jishu: Long,
        val tygongwei: Int, val guan: String, val zhusuan: Int, val keshuan: Int,
        val tianma: String, val shengong: String, val lines: List<Pair<String, String>>,
        val shen16: Shen16
    )

    /** 常规盘排布。cusJushu: 0=自动, 正=阳遁局, 负=阴遁局。yongShen: 用神干支(如"甲子") */
    fun paipan(
        dt: LocalDateTime, isman: Boolean, cusJushu: Int = 0, yongShen: String = "甲子",
        longitude: Double? = null
    ): Result {
        val t = if (longitude != null) dt.plusMinutes(((longitude - 120.0) * 4.0).toLong()) else dt
        val lun = LunarCalendar.fromDateTime(t)
        val nianZhu = arrayOf(lun.yearGanZhi[0].toString(), lun.yearGanZhi[1].toString())
        val yueZhu = arrayOf(lun.monthGanZhi[0].toString(), lun.monthGanZhi[1].toString())
        val riZhu = arrayOf(lun.dayGanZhi[0].toString(), lun.dayGanZhi[1].toString())
        val shiZhu = arrayOf(lun.hourGanZhi[0].toString(), lun.hourGanZhi[1].toString())

        val yIdx = ZHI.indexOf(lun.yearGanZhi[1].toString()) + 1
        val mIdx = kotlin.math.abs(lun.month)
        val dIdx = lun.day + if (t.hour == 23) 1 else 0
        val hIdx = ZHI.indexOf(lun.hourZhi) + 1

        val yangDun: Boolean
        val jushu: Int
        if (cusJushu == 0) {
            jushu = let { val j = (yIdx + mIdx + dIdx + hIdx) % 9; if (j == 0) 9 else j }
            val xiazhi = SolarTerm.termTime(t.year, "夏至")
            val dongzhi = SolarTerm.termTime(t.year, "冬至")
            yangDun = !(xiazhi != null && dongzhi != null && t.isAfter(xiazhi) && t.isBefore(dongzhi))
        } else {
            yangDun = cusJushu > 0
            jushu = kotlin.math.abs(cusJushu)
        }
        val isYang = yangDun

        // 积数
        fun jz(gz: String) = JishiyuBase.JIAZHI.indexOf(gz) + 1
        val jsYear = jz(lun.yearGanZhi); val jsMonth = jz(lun.monthGanZhi)
        val jsDay = jz(lun.dayGanZhi); val jsHour = jz(lun.hourGanZhi)
        val jishu = jsYear.toLong() * jsMonth * jsDay * jsHour + (yIdx + mIdx + dIdx + hIdx)
        val js = jishu.toInt()

        // 太乙宫位
        var tygw = Math.floorMod((js % 24), 3)
        if (tygw == 0) tygw = 3
        val guan = if (tygw == 1) "管天" else if (tygw == 2) "管地" else "管人"

        // 小游
        var tygsjw: Int
        val m24 = js % 24
        if (Math.floorMod(m24, 3) == 0) {
            tygsjw = m24 / 3
        } else if (m24 == 0) {
            tygsjw = if (!isYang) 1 else 8
        } else {
            tygsjw = m24 / 3 + 1
        }
        val tygspws = (if (isYang) "12346789" else "98764321").jsSub(tygsjw - 1, tygsjw)
        val tygwzj = zjzhuan8(tygspws.toIntOrNull() ?: 8)
        val xiaoyou = "12345678".jsSub(tygsjw - 1, tygsjw)

        // 文昌
        var wcg = js % 18
        val slgstr = "武德太簇阴主阴德大义地主阳德和德吕申高从太阳大炅大神大威天道大武"
        val slgyy = "阴阳阴阳阴阳阴阳阴阳阴阳阴阳阴阳"
        if (wcg >= 4 || wcg >= 16) wcg -= 1
        val wclg: String; val wclgyy: String
        if (wcg == 0) { wcg = 16; wclg = "大武"; wclgyy = "阳" }
        else { wclg = slgstr.jsSub(2 * wcg - 2, 2 * wcg); wclgyy = slgyy.jsSub(wcg - 1, wcg) }
        val wclgz = zhuan8(wclg)
        val wclgzj = zjzhuan8(wclgz)

        // 计神 / 始计
        val jishenstr = if (isYang) "寅丑子亥戌酉申未午巳辰卯" else "申未午巳辰卯寅丑子亥戌酉"
        val hz = ZHI.indexOf(lun.hourZhi)
        val jishen = jishenstr.jsSub(hz, hz + 1)
        val dzgw = "武德太簇阴主大义地主阳德吕申高从太阳大神大威天道"
        val dzgwz = "申酉戌亥子丑寅卯辰巳午未"
        var i12 = 1
        for (i in 1..12) if (jishen == dzgwz.jsSub(i - 1, i)) { i12 = i; break }
        val jsdy = dzgw.jsSub(i12 * 2 - 2, i12 * 2)
        var p16 = 1
        for (p in 1..16) if (slgstr.jsSub(2 * p - 2, 2 * p) == jsdy) { p16 = p; break }
        var zjA = 9 - p16; if (zjA <= 0) zjA += 16
        var shiji = wcg + zjA - 1; if (shiji > 16) shiji = wcg + zjA - 16 - 1
        val shijilg = slgstr.jsSub(2 * shiji - 2, 2 * shiji)
        val shijilgzj = zjzhuan8(zhuan8(shijilg))
        val shijilgyy = slgyy.jsSub(shiji - 1, shiji)

        // 主算
        fun sumRange(start: Int, len: Int): Int {
            val zsp = IntArray(17)
            for (u in start..(start + len - 1)) if (u in 1..16) zsp[u] = "1234567812345678"[u - 1] - '0'
            return zsp.sum()
        }
        val gongstr0 = "83492761"
        var wys = wclgzj - tygwzj
        var wysp = ggwy(wys)
        if (wys == 0) {
            val ttyz16 = when (tygspws) { "3" -> 8; "9" -> 12; "7" -> 16; "1" -> 4; else -> 0 }
            if (wcg - ttyz16 == 1 || wcg - ttyz16 == -15) wysp = 8
        }
        fun calcZhuan(zsp: IntArray, wysp: Int): Int {
            val zhusuans = zsp.sum()
            val zhusuansp = zhusuans.toString().take(8)
            var zsgg = 0
            for (u in 1..8) {
                val c = zhusuansp.getOrNull(u - 1)?.toString()?.toIntOrNull() ?: 0
                zsgg += if (c in 1..8) (gongstr0[c - 1] - '0') else 0
            }
            val zspp = zsgg.toString().take(wysp)
            var lzp = 0
            for (u in 1..8) lzp += zspp.getOrNull(u - 1)?.toString()?.toIntOrNull() ?: 0
            return lzp
        }
        val zsp = IntArray(17)
        for (u in wclgzj..(wclgzj + wysp - 1)) if (u in 1..16) zsp[u] = "1234567812345678"[u - 1] - '0'
        val lzp = calcZhuan(zsp, wysp)
        val zhusuanstr = if (wclgyy == "阳") (if (lzp + 1 == 41) 1 else lzp + 1) else lzp
        var zdj: Int
        if (zhusuanstr in listOf(10, 20, 30, 40)) zdj = zhusuanstr % 9
        else zdj = zhusuanstr % 10
        var zcj: Any = zdj * 3
        if ((zdj * 3) > 9) zcj = (zdj * 3).toString().jsSub(1, 2)

        // 客算
        var kss = zjzhuan8(shijilgzj) - tygwzj
        var kswysp = ggwy(kss)
        if (kss == 0) {
            val tyz16 = when (tygspws) { "3" -> 8; "9" -> 12; "7" -> 16; "1" -> 4; else -> 0 }
            if (shiji - tyz16 == 1 || shiji - tyz16 == -15) kswysp = 8
        }
        val kzsp = IntArray(17)
        val kstart = zjzhuan8(shijilgzj)
        for (u in kstart..(kstart + kswysp - 1)) if (u in 1..16) kzsp[u] = "1234567812345678"[u - 1] - '0'
        val klzp = calcZhuan(kzsp, kswysp)
        val kzhusuanstr = if (shijilgyy == "阳") (if (klzp + 1 == 41) 1 else klzp + 1) else klzp
        val kzdj = if (kzhusuanstr in listOf(10, 20, 30, 40)) kzhusuanstr % 9 else kzhusuanstr % 10
        val kcj = ((kzdj * 3).toString().takeLast(1)).toInt()

        // 五福三基四神
        val wufu = if (js % 45 == 0) 0 else 1
        var wufugw = js / 45 % 5 + wufu
        if (wufu == 0) wufugw = 5
        var junjigw = (js % 360) / 30
        if (junjigw > 12) junjigw -= 12
        if ((js % 360) % 30 > 0) junjigw += 1
        if (junjigw == 0) junjigw = 12
        var chenjigw = (js % 36) / 3
        if (chenjigw > 12) chenjigw -= 12
        if ((js % 36) % 3 > 0) chenjigw += 1
        if (chenjigw == 0) chenjigw = 12
        var minji = js % 12; if (minji == 0) minji = 12

        val yunxingj = "亥午寅卯辰酉申子巳戌未丑"
        val stdf = js % 36 % 3
        var stdfs = (js % 36) / 3
        if (stdf > 0) stdfs += 1
        var stdfgw = stdfs; if (stdfgw > 12) stdfgw -= 12
        fun cyc(s: String, n0: Int): String { var n = n0; while (n > 12) n -= 12; return s.jsSub(n - 1, n) }
        val sishen = cyc(yunxingj, stdfgw)
        val tianyi = cyc(yunxingj, 6 + stdfgw - 1)
        val diyi = cyc(yunxingj, 9 + stdfgw - 1)
        val feifu = cyc(yunxingj, 5 + stdfgw - 1)

        // 身宫
        var sg1 = ZHI.indexOf(lun.hourZhi) + 1 - (ZHI.indexOf(lun.monthGanZhi[1].toString()) + 1) + (ZHI.indexOf(lun.dayGanZhi[1].toString()) + 1)
        if (sg1 <= 0) sg1 += 12
        if (sg1 > 12) sg1 -= 12
        val shengong = ZHI[sg1 - 1]

        // 天马
        var tmno = mIdx; if (tmno > 6) tmno = mIdx % 6; if (tmno == 0) tmno = 6
        val tianma = "午申戌子寅辰".jsSub(tmno - 1, tmno)

        // 十神(用神)
        val ysg = yongShen[0].toString()
        val gIdx = GAN.indexOf(ysg) + 1
        val rgwx = gwxc(gIdx); val rgyy = yys(gIdx)
        val shishenp = mutableMapOf<Int, String>()
        for (i in 1..17) {
            val ss = slxwx(SQSP[i])
            val rsl = ss.takeLast(1).toIntOrNull() ?: 0; val lsl = ss.jsSub(0, 1).toIntOrNull() ?: 0
            val sc = shishenl(rsl, rgwx, lsl, rgyy)
            shishenp[i] = sc + xdj(lun.monthGanZhi[1].toString(), SQSP[i])
        }

        // ===== 太乙十六神盘（shen16）：灰底16神 + 米黄底活神十神标签 =====
        // 数组下标 1..N，值形如 "太乙比1" "主大食2"（十神取自 shishenp）
        fun arr(n: Int) = MutableList(n + 1) { "" }
        fun put(a: MutableList<String>, i: Int, s: String) { if (i in 1 until a.size) a[i] = s }
        val zcjInt = when (zcj) { is Int -> zcj as Int; is String -> (zcj as String).toIntOrNull() ?: 0; else -> 0 }
        val tysq = arr(16); val xysq = arr(16); val jjsq = arr(16); val wcsq = arr(16)
        val cjsq = arr(16); val wfsq = arr(16); val sjsq = arr(16); val zdjsq = arr(16)
        val kdjsq = arr(16); val mjsq = arr(16); val jssq = arr(16); val zcjsq = arr(16)
        val kcjsq = arr(16); val sqxpl = arr(12)
        val sssq = arr(12); val tiysq = arr(12); val dysq = arr(12); val ffsq = arr(12)

        put(tysq, tygwzj, "太乙" + (shishenp[1] ?: ""))
        put(zdjsq, zjzhuan8(zdj), "主大" + (shishenp[8] ?: ""))
        put(zcjsq, zjzhuan8(zcjInt), "主参" + (shishenp[13] ?: ""))
        put(kdjsq, zjzhuan8(kzdj), "客大" + (shishenp[9] ?: ""))
        put(kcjsq, zjzhuan8(kcj), "客参" + (shishenp[15] ?: ""))
        put(wfsq, wufugw, "五福" + (shishenp[6] ?: ""))
        put(xysq, xiaoyou.toIntOrNull() ?: 0, "小游" + (shishenp[2] ?: ""))
        put(wcsq, wcg, "文昌" + (shishenp[4] ?: ""))
        put(sjsq, shiji, "始击" + (shishenp[7] ?: ""))
        put(jssq, ZHI.indexOf(jishen) + 1, "计神" + (shishenp[11] ?: ""))
        put(jjsq, junjigw, "君基" + (shishenp[3] ?: ""))
        put(cjsq, chenjigw, "臣基" + (shishenp[5] ?: ""))
        put(mjsq, minji, "民基" + (shishenp[10] ?: ""))
        put(sssq, ZHI.indexOf(sishen) + 1, "四神" + (shishenp[14] ?: ""))
        put(tiysq, ZHI.indexOf(tianyi) + 1, "天乙" + (shishenp[16] ?: ""))
        put(dysq, ZHI.indexOf(diyi) + 1, "地乙" + (shishenp[12] ?: ""))
        put(ffsq, ZHI.indexOf(feifu) + 1, "飞符" + (shishenp[17] ?: ""))
        for (i in 1..12) sqxpl[i] = sssq[i] + tiysq[i] + dysq[i] + ffsq[i]

        val arrays = mapOf(
            "tysq" to tysq, "xysq" to xysq, "jjsq" to jjsq, "wcsq" to wcsq,
            "cjsq" to cjsq, "wfsq" to wfsq, "sjsq" to sjsq, "zdjsq" to zdjsq,
            "kdjsq" to kdjsq, "mjsq" to mjsq, "jssq" to jssq, "zcjsq" to zcjsq,
            "kcjsq" to kcjsq, "sqxpl" to sqxpl
        )
        // 每个方位的活神标签，表达式逐条照搬原版 qimen3shi_tpl.html
        fun between(s: String, a: String, b: String): String {
            val st = s.indexOf(a); val en = s.indexOf(b, st + a.length)
            return if (st >= 0 && en > st) s.substring(st + a.length, en) else ""
        }
        fun act(vararg refs: String): String = refs.joinToString("") { ref ->
            val k = ref.substringBefore("["); val i = between(ref, "[", "]").toIntOrNull() ?: 0
            arrays[k]?.getOrNull(i) ?: ""
        }
        // 顺时针自左上角(巽)起：巽 巳 午 未 坤 申 酉 戌 乾 亥 子 丑 艮 寅 卯 辰
        val gods16 = listOf("大灵九巽", "大神九巳", "大威二午", "天道二未", "大武七坤", "武德七申",
            "太簇六酉", "阴主六戌", "阴德一乾", "大义一亥", "地主八子", "阳德八丑",
            "和德三艮", "吕申三寅", "高从四卯", "太阳四辰")
        val active16 = listOf(
            act("wcsq[12]", "sjsq[12]", "zcjsq[4]", "kdjsq[4]", "tysq[4]", "zdjsq[4]", "kcjsq[4]", "wfsq[3]"),
            act("wcsq[13]", "xysq[2]", "jssq[6]", "sjsq[13]", "jjsq[12]", "cjsq[12]", "mjsq[8]", "sqxpl[6]"),
            act("tysq[5]", "wcsq[14]", "jssq[7]", "sjsq[14]", "zcjsq[5]", "kdjsq[5]", "jjsq[1]", "cjsq[1]", "mjsq[9]", "sqxpl[7]", "zdjsq[5]", "kcjsq[5]"),
            act("wcsq[15]", "xysq[6]", "jssq[8]", "sjsq[15]", "jjsq[2]", "cjsq[2]", "mjsq[10]", "sqxpl[8]"),
            act("wcsq[16]", "sjsq[16]", "zcjsq[6]", "kdjsq[6]", "zdjsq[6]", "kcjsq[6]", "tysq[6]", "wfsq[4]"),
            act("wcsq[1]", "xysq[5]", "jssq[9]", "sjsq[1]", "jjsq[3]", "cjsq[3]", "mjsq[11]", "sqxpl[9]"),
            act("tysq[7]", "wcsq[2]", "jssq[10]", "sjsq[2]", "zcjsq[7]", "kdjsq[7]", "jjsq[4]", "cjsq[4]", "mjsq[12]", "sqxpl[10]", "zdjsq[7]", "kcjsq[7]"),
            act("zdjsq[9]", "zcjsq[9]", "kdjsq[9]", "kcjsq[9]", "wcsq[3]", "xysq[1]", "jssq[11]", "sjsq[3]", "jjsq[5]", "cjsq[5]", "mjsq[1]", "sqxpl[11]", "wfsq[5]"),
            act("tysq[8]", "zdjsq[8]", "kcjsq[8]", "wfsq[1]", "wcsq[4]", "sjsq[4]", "zcjsq[8]", "kdjsq[8]"),
            act("wcsq[5]", "xysq[7]", "jssq[12]", "sjsq[5]", "jjsq[6]", "cjsq[6]", "mjsq[2]", "sqxpl[12]"),
            act("tysq[1]", "wcsq[6]", "jssq[1]", "sjsq[6]", "zcjsq[1]", "kdjsq[1]", "jjsq[7]", "cjsq[7]", "mjsq[3]", "sqxpl[1]", "zdjsq[1]", "kcjsq[1]"),
            act("wcsq[7]", "xysq[3]", "jssq[2]", "sjsq[7]", "jjsq[8]", "cjsq[8]", "mjsq[4]", "sqxpl[2]"),
            act("tysq[2]", "zdjsq[2]", "kcjsq[2]", "wfsq[2]", "wcsq[8]", "sjsq[8]", "zcjsq[2]", "kdjsq[2]"),
            act("wcsq[9]", "xysq[4]", "jssq[3]", "sjsq[9]", "jjsq[9]", "cjsq[9]", "mjsq[5]", "sqxpl[3]"),
            act("tysq[3]", "wcsq[10]", "jssq[4]", "sjsq[10]", "zcjsq[3]", "kdjsq[3]", "jjsq[10]", "cjsq[10]", "mjsq[6]", "sqxpl[4]", "zdjsq[3]", "kcjsq[3]"),
            act("wcsq[11]", "xysq[8]", "jssq[5]", "sjsq[11]", "jjsq[11]", "cjsq[11]", "mjsq[7]", "sqxpl[5]")
        )
        val shen16 = Shen16(gods16, active16)

        val lines = mutableListOf<Pair<String, String>>()
        lines.add("局式" to "${if (yangDun) "阳遁" else "阴遁"}${jushu}局")
        lines.add("积数" to "$jishu")
        lines.add("太乙" to "${tygspws}宫($guan) 宫位$tygw")
        lines.add("小游" to "${xiaoyou}宫")
        lines.add("文昌" to "$wclg(${wclgzj}宫 ${wclgyy}) 落$wcg")
        lines.add("计神" to "$jishen")
        lines.add("始击" to "$shijilg(${shijilgzj}宫 ${shijilgyy})")
        lines.add("主算" to "$zhusuanstr 主大将$zdj 主参将$zcj")
        lines.add("客算" to "$kzhusuanstr 客大将$kzdj 客参将$kcj")
        lines.add("五福" to "${wufugw}宫  君基${junjigw}宫 臣基${chenjigw}宫 民基${minji}宫")
        lines.add("四神" to "$sishen  天乙$tianyi 地乙$diyi 飞符$feifu")
        lines.add("身宫" to shengong)
        lines.add("天马" to tianma)
        lines.add("十神" to shishenp.map { "${SQSP[it.key]}:${it.value}" }.joinToString(" "))

        val range = SolarTerm.rangeAt(t)
        val jieqi = range?.let { "${it.from.name} ~ ${it.to.name}" } ?: ""
        val dateText = "${t.year}年${t.monthValue}月${t.dayOfMonth}日 ${t.hour}时${t.minute}分(农历${lun.month}月${lun.day}日)"
        return Result(dateText, listOf(lun.yearGanZhi, lun.monthGanZhi, lun.dayGanZhi, lun.hourGanZhi),
            jieqi, yangDun, jushu, jishu, tygw, guan, zhusuanstr, kzhusuanstr, tianma, shengong, lines, shen16)
    }
}
