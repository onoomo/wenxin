package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.util.SolarTerm
import java.time.Duration
import java.time.LocalDateTime

/**
 * 奇门遁甲「局数」计算 —— 1:1 移植自吉时雨 qimendunjia.js (AGPL-3.0)。
 * 三种起局方式：拆补(1) / 置闰(2) / 茅山(3)，局数由节气 + 符头 + 三元严格推算，
 * 每种方式在同一时刻可算出不同局数。
 *
 * 输出 [JuInfo] 供阴盘时盘引擎 Qimen.paipan 以 cusJushu 重排九宫。
 */
object QimenJu {
    /** 局数信息 */
    data class JuInfo(
        val yangDun: Boolean,   // true 阳遁 / false 阴遁
        val num: Int,           // 1..9
        val yuan: String,       // 上 / 中 / 下
        val name: String,       // 阴遁九局中（原版 panJu）
        val methodName: String  // 拆补 / 置闰 / 茅山
    ) {
        /** 传给 Qimen.paipan 的自定义局：正数阳遁、负数阴遁 */
        val cusJushu: Int get() = if (yangDun) num else -num
    }

    // ---- 数据表（照搬 qimendunjia.js） ----
    /** 原版节气名顺序：小寒开头 */
    private val JIEQI_NAME = arrayOf(
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
        "小暑", "大暑", "立秋", "处暑", "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    )
    private val YANG_DUN = setOf(
        "冬至", "小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨", "立夏", "小满", "芒种"
    )
    /** 节气 → 上/中/下三元局码 */
    private val JQ_CODE = mapOf(
        "冬至" to "一七四", "惊蛰" to "一七四", "小寒" to "二八五", "大寒" to "三九六", "春分" to "三九六",
        "立春" to "八五二", "雨水" to "九六三", "清明" to "四一七", "立夏" to "四一七", "谷雨" to "五二八",
        "小满" to "五二八", "芒种" to "六三九", "夏至" to "九三六", "白露" to "九三六", "小暑" to "八二五",
        "大暑" to "七一四", "秋分" to "七一四", "立秋" to "二五八", "处暑" to "一四七", "霜降" to "五八二",
        "小雪" to "五八二", "寒露" to "六九三", "立冬" to "六九三", "大雪" to "四七一"
    )
    private val CN_NUM = "一二三四五六七八九"
    private val LUNAR_MONTH_CN = arrayOf("", "正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月")
    /** 旬首 → 值符天干 */
    private val JUN_SHOU_GAN = mapOf("甲子" to "戊", "甲戌" to "己", "甲申" to "庚", "甲午" to "辛", "甲辰" to "壬", "甲寅" to "癸")
    /** 符头 → 三元（置闰用） */
    private val JU_DAY_YUAN = mapOf(
        "甲子" to "上元", "甲午" to "上元", "己卯" to "上元", "己酉" to "上元",
        "甲寅" to "中元", "甲申" to "中元", "己巳" to "中元", "己亥" to "中元",
        "甲辰" to "下元", "甲戌" to "下元", "己丑" to "下元", "己未" to "下元"
    )

    private fun jiazi(): List<String> = (0 until 60).map { LunarCalendar.gan(it % 10) + LunarCalendar.zhi(it % 12) }
    private val JZ = jiazi()

    // ---- 基础工具 ----
    /** 当前节气名（= 上一个已到的节气），对应 lunar.getPrevJieQi() */
    private fun jq(dt: LocalDateTime): String = SolarTerm.rangeAt(dt)?.from?.name ?: ""
    /** 上一个节气起始时刻（拆补/茅山用） */
    private fun jqStartPrev(dt: LocalDateTime): LocalDateTime? = SolarTerm.rangeAt(dt)?.from?.time

    /** 置闰用起点：若今天恰逢某节气则用该节气时刻，否则用上一个节气（对应 getJieqiStartDate4zhirun） */
    private fun jqStartZhirun(dt: LocalDateTime): LocalDateTime? {
        val d = dt.toLocalDate()
        // 今天是否有节气（getJieQi 语义：同年月日的节气）
        for (y in (d.year - 1)..(d.year + 1)) {
            for (i in 0..23) {
                val t = SolarTerm.termTime(y, i) ?: continue
                if (t.toLocalDate() == d) return t
            }
        }
        return jqStartPrev(dt)
    }

    /** 四柱（23 点日柱进一位），对应 gangzhi() */
    private fun gangzhi(dt: LocalDateTime): List<String> {
        val dayDate = if (dt.hour == 23) dt.toLocalDate().plusDays(1) else dt.toLocalDate()
        val lun = LunarCalendar.fromDateTime(dt)
        return listOf(lun.yearGanZhi, lun.monthGanZhi, LunarCalendar.dayGanZhi(dayDate), lun.hourGanZhi)
    }

    /** 日柱在六十甲子中的索引（23 点进位） */
    private fun dayGzIndex(dt: LocalDateTime): Int {
        val dayDate = if (dt.hour == 23) dt.toLocalDate().plusDays(1) else dt.toLocalDate()
        val gz = LunarCalendar.dayGanZhi(dayDate)
        return JZ.indexOf(gz).let { if (it >= 0) it else 0 }
    }

    /** 三元：日柱所在符头组 (idx/5)%3 → 上/中/下 */
    private fun findYuan(dt: LocalDateTime): String {
        val names = arrayOf("上", "中", "下")
        return names[(dayGzIndex(dt) / 5) % 3]
    }

    /** 值符天干：由时柱所属旬推定 */
    private fun zhifuTianGan(dt: LocalDateTime): String {
        val hourGz = gangzhi(dt)[3]
        for (jun in JUN_SHOU_GAN.keys) {
            val start = JZ.indexOf(jun)
            if (hourGz in JZ.subList(start, start + 10)) return JUN_SHOU_GAN[jun]!!
        }
        return "戊"
    }

    private fun yinYangDun(jieqi: String): String = if (jieqi in YANG_DUN) "阳遁" else "阴遁"

    /** 从局名解析阴阳遁、局数、三元 */
    private fun parseJu(name: String, method: Int): JuInfo {
        val yang = name.startsWith("阳遁")
        var num = 1
        for (i in name.indices) {
            val k = CN_NUM.indexOf(name[i])
            if (k >= 0) { num = k + 1; break }
        }
        val yuan = when {
            '上' in name -> "上"
            '中' in name -> "中"
            else -> "下"
        }
        return JuInfo(yang, num, yuan, name, methodName(method))
    }

    fun methodName(method: Int): String = when (method) {
        1 -> "拆补"; 2 -> "置闰"; else -> "茅山"
    }

    /** 主入口：算局 */
    fun ju(dt: LocalDateTime, method: Int): JuInfo {
        val name = when (method) {
            1 -> chaibu(dt)
            2 -> zhirun(dt)
            else -> maoshan(dt)
        }
        return parseJu(name, method)
    }

    // ---- 1) 拆补法 ----
    private fun chaibu(dt: LocalDateTime): String {
        val jieqi = jq(dt)
        val yy = yinYangDun(jieqi)
        val yuan = findYuan(dt)
        val code = JQ_CODE[jieqi] ?: "一七四"
        val num = when (yuan) { "上" -> code[0]; "中" -> code[1]; else -> code[2] }
        return yy + num + "局" + yuan
    }

    // ---- 3) 茅山法 ----
    private fun maoshan(dt: LocalDateTime): String {
        val jieqi = jq(dt)
        val yy = yinYangDun(jieqi)
        val start = jqStartPrev(dt) ?: dt
        val hours = Duration.between(start, dt).toMillis() / 3_600_000.0
        val shichen = maxOf(0, Math.floor(hours / 2).toInt())
        val yuan = if (shichen < 60) "上" else if (shichen < 120) "中" else "下"
        val code = JQ_CODE[jieqi] ?: "一七四"
        val num = when (yuan) { "上" -> code[0]; "中" -> code[1]; else -> code[2] }
        return yy + num + "局" + yuan
    }

    // ---- 2) 置闰法 ----
    private data class ZhirunRaw(
        val jieqi: String, val diffDays: Int, val threeYuan: String,
        val zftg: String, val lunarMonth: String, val solarMonth: Int, val lunarDay: Int,
        val curJu: String, val chaoJu: String, val otherJu: String, val otherJu1: String,
        val yinYang: String
    )

    /** 取环形列表从 o 开始旋转后的第 idx 项 */
    private fun rotated(list: List<String>, o: String, idx: Int): String {
        val s = list.indexOf(o)
        if (s < 0) return list[0]
        return list[(s + idx) % list.size]
    }

    private fun zhirunRaw(dt: LocalDateTime): ZhirunRaw {
        val Jieqi = jq(dt)
        val newJq = rotated(JIEQI_NAME.toList(), Jieqi, 1)              // 下一个节气
        val newJq1 = rotated(JIEQI_NAME.toList(), Jieqi, 0)             // 当前节气(=Jieqi)
        val newJq2 = rotated(JIEQI_NAME.toList(), Jieqi, JIEQI_NAME.size - 1) // 上一个节气
        val yinYang = yinYangDun(newJq1)

        val jieqiCode = JQ_CODE[Jieqi] ?: "一七四"
        val dgz = gangzhi(dt)[2]
        // 符头：日柱所在 5 日组的首日
        val fd = JZ[(dayGzIndex(dt) / 5) * 5]
        val threeYuan = JU_DAY_YUAN[fd] ?: "上元"
        val zftg = zhifuTianGan(dt)

        val start = jqStartZhirun(dt) ?: dt
        val diffDays = Math.floor(Duration.between(start, dt).toMillis() / 86_400_000.0).toInt()

        fun codeNum(code: String): String = when (threeYuan) {
            "上元" -> code[0].toString(); "中元" -> code[1].toString(); else -> code[2].toString()
        }
        val kooks = codeNum(jieqiCode)
        val code1 = JQ_CODE[newJq] ?: "一七四"
        val code2 = JQ_CODE[newJq1] ?: "一七四"
        val code0 = JQ_CODE[newJq2] ?: "一七四"
        val kooks1 = codeNum(code1)
        val kooks2 = codeNum(code2)
        val kooks3 = codeNum(code0)

        val lun = LunarCalendar.fromDateTime(dt)
        val lunarMonth = LUNAR_MONTH_CN.getOrElse(lun.month) { "" }
        // 注意：原版 qimendunjia.js 里的变量名 solarMonth 实际取的是 lunar.getMonth()（农历月数字），
        // 并非公历月。下方判断必须用农历月，否则置闰分支会走错。
        val solarMonth = lun.month
        val lunarDay = lun.day
        val newJqYinYang = yinYangDun(newJq)

        return ZhirunRaw(
            Jieqi, diffDays, threeYuan, zftg, lunarMonth, solarMonth, lunarDay,
            "${yinYang}${kooks}局", "${newJqYinYang}${kooks1}局", "${yinYang}${kooks3}局", "${newJqYinYang}${kooks}局",
            yinYang
        )
    }

    private fun zhirun(dt: LocalDateTime): String {
        val q = zhirunRaw(dt)
        val jQ = q.jieqi
        val d = q.diffDays
        val lunarMonth = q.lunarMonth
        val solarMonth = q.solarMonth
        val lunarDay = q.lunarDay
        val isWuji = q.zftg in setOf("戊", "己", "庚", "辛", "壬", "癸")

        if (d == 0) {
            if (lunarMonth == "腊月" || lunarMonth == "冬月") {
                return (if (lunarMonth == "腊月") q.otherJu1 else q.curJu) + q.threeYuan
            }
            return (if (solarMonth > 9) q.chaoJu else q.curJu) + q.threeYuan
        }
        if (d <= 6 && d > 1) {
            if (lunarMonth == "腊月" || lunarMonth == "冬月") {
                val ju = if (lunarMonth == "腊月") q.otherJu1
                         else if (jQ == "冬至") q.otherJu else q.curJu
                return ju + q.threeYuan
            }
            if (solarMonth >= 9) {
                if (lunarDay < 15) return q.otherJu1 + q.threeYuan
                return (if (isWuji) q.curJu else q.otherJu) + q.threeYuan
            }
            if (lunarMonth == "正月") {
                if (lunarDay < 10 && !isWuji) return q.otherJu + q.threeYuan
                if (isWuji) {
                    if (lunarDay < 20) return q.otherJu1 + q.threeYuan
                    if (lunarDay > 20 && lunarDay <= 26) return q.otherJu + q.threeYuan
                    return q.otherJu1 + q.threeYuan
                }
            }
            if (lunarMonth != "腊月" && lunarMonth != "冬月" && lunarMonth != "正月") {
                if (lunarDay < 15) return q.curJu + q.threeYuan
            }
            if (lunarDay >= 15) return q.otherJu1 + q.threeYuan
            return q.chaoJu + q.threeYuan
        }
        if (d <= 9 && d > 1) {
            if (lunarMonth == "腊月" || lunarMonth == "冬月") {
                return (if (lunarMonth == "腊月") q.curJu else q.otherJu1) + q.threeYuan
            }
            if (lunarMonth == "正月") {
                val ju = if (solarMonth <= 9 && lunarDay >= 15) q.otherJu1
                         else if (isWuji) q.otherJu1 else q.chaoJu
                return ju + q.threeYuan
            }
            if (solarMonth <= 6) {
                if (lunarDay <= 10) return q.otherJu1 + q.threeYuan
                if (isWuji) return (if (lunarDay < 20) q.chaoJu else q.otherJu1) + q.threeYuan
                return q.curJu + q.threeYuan
            }
            if (solarMonth <= 9) {
                if (lunarDay < 15) return q.chaoJu + q.threeYuan
                return (if (isWuji || lunarDay >= 20) q.otherJu1 else q.curJu) + q.threeYuan
            }
            return q.chaoJu + q.threeYuan
        }
        if (d <= 15 && d > 1) {
            if (lunarMonth == "腊月" || lunarMonth == "冬月") {
                val ju = if (lunarMonth == "腊月" || jQ != "冬至") q.otherJu1
                         else if (d <= 12) q.otherJu1 else q.curJu
                return ju + q.threeYuan
            }
            if (solarMonth > 9) return q.otherJu1 + q.threeYuan
            if (lunarMonth == "正月") return q.curJu + q.threeYuan
            if (lunarMonth != "正月" && lunarMonth != "腊月" && lunarMonth != "冬月") return q.curJu + q.threeYuan
            return q.chaoJu + q.threeYuan
        }
        if (d < 0) return q.chaoJu + q.threeYuan
        return q.curJu + q.threeYuan
    }
}
