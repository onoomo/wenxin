package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.util.SolarTerm
import java.time.LocalDateTime

/**
 * 大六壬 —— 1:1 移植自吉时雨 da6ren.js (AGPL-3.0, xianbo.chen@gmail.com)
 * 月将天盘、贵人、六亲、四课、三传(720课)、十二宫、行年。
 */
object DaLiuRen {
    private val _12YUEJIANG = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val _12DIZHI = arrayOf("寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥", "子", "丑")
    private val SHI_ZHI = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val YUE_ZHI = SHI_ZHI // 索引方式一致(用indexOf)
    private val _10TIANGAN = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸", "〇", "〇")
    private val _12SHEN = arrayOf("贵", "蛇", "朱", "合", "勾", "龙", "空", "虎", "常", "玄", "阴", "后")
    private val _HOUR_ZHI = mapOf("子" to 23, "丑" to 1, "寅" to 3, "卯" to 5, "辰" to 7, "巳" to 9,
        "午" to 11, "未" to 13, "申" to 15, "酉" to 17, "戌" to 19, "亥" to 21)
    private val JIGONG = mapOf("甲" to "寅", "乙" to "辰", "丙" to "巳", "丁" to "未", "戊" to "巳",
        "己" to "未", "庚" to "申", "辛" to "戌", "壬" to "亥", "癸" to "丑")
    private val YUE_JIANG = mapOf(
        "雨水" to arrayOf("亥", "登明"), "春分" to arrayOf("戌", "河魁"), "谷雨" to arrayOf("酉", "从魁"),
        "小满" to arrayOf("申", "传送"), "夏至" to arrayOf("未", "小吉"), "大暑" to arrayOf("午", "胜光"),
        "处暑" to arrayOf("巳", "太乙"), "秋分" to arrayOf("辰", "天罡"), "霜降" to arrayOf("卯", "太冲"),
        "小雪" to arrayOf("寅", "功曹"), "冬至" to arrayOf("丑", "大吉"), "大寒" to arrayOf("子", "神后"))
    private val YUE_JIANG2 = arrayOf(
        arrayOf("亥", "登明"), arrayOf("戌", "河魁"), arrayOf("酉", "从魁"), arrayOf("申", "传送"),
        arrayOf("未", "小吉"), arrayOf("午", "胜光"), arrayOf("巳", "太乙"), arrayOf("辰", "天罡"),
        arrayOf("卯", "太冲"), arrayOf("寅", "功曹"), arrayOf("丑", "大吉"), arrayOf("子", "神后"))

    private val TIANYI = mapOf(
        "甲" to arrayOf("丑", "未"), "戊" to arrayOf("丑", "未"), "庚" to arrayOf("丑", "未"),
        "乙" to arrayOf("子", "申"), "己" to arrayOf("子", "申"), "丙" to arrayOf("亥", "酉"),
        "丁" to arrayOf("亥", "酉"), "壬" to arrayOf("巳", "卯"), "癸" to arrayOf("巳", "卯"),
        "辛" to arrayOf("午", "寅"))

    /** 原版节气行：前节气+日期 ~ 后节气+日期（24节气，取"节"非"气"） */
    private fun jieqiInfo(dt: LocalDateTime): String {
        val jieNames = arrayOf("立春","惊蛰","清明","立夏","芒种","小暑","立秋","白露","寒露","立冬","大雪","小寒")
        var prev: Pair<String, LocalDateTime>? = null
        var next: Pair<String, LocalDateTime>? = null
        for (y in (dt.year - 1)..(dt.year + 1)) for (name in jieNames) {
            val tm = SolarTerm.termTime(y, name) ?: continue
            if (!tm.isAfter(dt) && (prev == null || tm.isAfter(prev.second))) prev = name to tm
            if (tm.isAfter(dt) && (next == null || tm.isBefore(next.second))) next = name to tm
        }
        fun fmt(p: Pair<String, LocalDateTime>?) = p?.let {
            "${it.first}${it.second.year}-${it.second.monthValue.toString().padStart(2,'0')}-${it.second.dayOfMonth.toString().padStart(2,'0')} " +
                "%02d:%02d".format(it.second.hour, it.second.minute)
        } ?: ""
        return "${fmt(prev)} ~ ${fmt(next)}"
    }

    /** 取最近的前一个"气"(12气: 冬至大寒雨水春分谷雨小满夏至大暑处暑秋分霜降小雪) */
    private fun prevQi(dt: LocalDateTime): String {
        val qiNames = arrayOf("冬至", "大寒", "雨水", "春分", "谷雨", "小满", "夏至", "大暑", "处暑", "秋分", "霜降", "小雪")
        // 收集前后三年的气时刻，取 <= dt 的最近一个
        var best: Pair<String, LocalDateTime>? = null
        for (y in (dt.year - 1)..(dt.year + 1)) {
            for (name in qiNames) {
                val t = SolarTerm.termTime(y, name) ?: continue
                if (!t.isAfter(dt) && (best == null || t.isAfter(best.second))) best = name to t
            }
        }
        return best?.first ?: "冬至"
    }

    private fun xing(ez: String): String = when (ez) {
        "子" -> "卯"; "丑" -> "戌"; "寅" -> "巳"; "卯" -> "子"; "巳" -> "申"
        "未" -> "丑"; "申" -> "寅"; "戌" -> "未"; "辰" -> "戌"; "午" -> "子"
        "酉" -> "卯"; "亥" -> "巳"; else -> ""
    }

    private fun liuQin(gan: String, zhi: String): String {
        val wx1 = JishiyuBase.GAN_WUXING[gan] ?: return ""
        val wx2 = JishiyuBase.ZHI_WUXING[zhi] ?: return ""
        // wuxingRelations[wx1] 中匹配 wx2
        val shengWo = mapOf("木" to "水", "火" to "木", "土" to "火", "金" to "土", "水" to "金")
        val woSheng = mapOf("木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木")
        val keWo = mapOf("木" to "金", "火" to "水", "土" to "木", "金" to "火", "水" to "土")
        val woKe = mapOf("木" to "土", "火" to "金", "土" to "水", "金" to "木", "水" to "火")
        return when (wx2) {
            wx1 -> "兄"
            woSheng[wx1] -> "子"
            keWo[wx1] -> "官"
            woKe[wx1] -> "财"
            shengWo[wx1] -> "父"
            else -> ""
        }
    }

    /** 循环列表：从 curIndex 开始依次 next() */
    private class Circular(private val arr: Array<String>, cur: Int, forward: Boolean = true) {
        var idx = cur
        val fwd = forward
        fun next(): String {
            val item = arr[idx]
            idx = if (fwd) (idx + 1) % arr.size else (idx - 1 + arr.size) % arr.size
            return item
        }
    }

    data class Result(
        val date: String, val siZhu: List<String>, val zhanbuTime: String,
        val yueJiang: String, val isMan: Boolean, val yearGanzhi: String, val xingYear: String,
        val isDaytime: Boolean, val kongwang: String,
        val tianPan: Map<String, String>, val guiShen: Map<String, String>, val tianGan: Map<String, String>,
        val fourKe: List<List<String>>, val threeChuan: List<List<String>>,
        val nianSha: Map<String, String>, val gong12: Map<String, String>,
        val sym3chuan: Map<String, List<String>>?,
        val jieqi: String = ""
    )

    /**
     * 排盘。isMan 性别, yearMing 出生年, yueJiangMethod 1节气/2取余,
     * guirenMethod 1卯酉/2昼/3夜, guirenSunni 1地盘/2男女, zhanbuTime 占卜时支(空=当前), yongShen 用神干支(阴盘)
     */
    fun paipan(
        dt: LocalDateTime, isMan: Boolean, yearMing: Int,
        yueJiangMethod: Int = 1, guirenMethod: Int = 1, guirenSunni: Int = 1,
        zhanbuTime: String = "", yongShen: String = "", longitude: Double? = null
    ): Result {
        val t = if (longitude != null) dt.plusMinutes(((longitude - 120.0) * 4.0).toLong()) else dt
        val lun = LunarCalendar.fromDateTime(t)
        val nianZhu = arrayOf(lun.yearGanZhi[0].toString(), lun.yearGanZhi[1].toString())
        val yueZhu = arrayOf(lun.monthGanZhi[0].toString(), lun.monthGanZhi[1].toString())
        val riZhu = arrayOf(lun.dayGanZhi[0].toString(), lun.dayGanZhi[1].toString())
        val shiZhu = arrayOf(lun.hourGanZhi[0].toString(), lun.hourGanZhi[1].toString())
        val zhanTime = zhanbuTime.ifEmpty { lun.hourZhi }

        // 行年
        val jiaziYear = LunarCalendar.ganZhi(yearMing - 4)
        val age = java.time.LocalDate.now().year - yearMing + 1
        val start = if (isMan) 2 else 32
        val cyc = Circular(JishiyuBase.JIAZHI, start)
        var xn = ""
        repeat(age) { xn = cyc.next() }

        // 月将(天盘)
        val yueJiang: String
        if (yueJiangMethod == 1) {
            yueJiang = YUE_JIANG[prevQi(t)]?.get(0) ?: "丑"
        } else {
            val yIdx = YUE_ZHI.indexOf(lun.yearGanZhi[1].toString()) + 1
            val mIdx = YUE_ZHI.indexOf(lun.monthGanZhi[1].toString()) + 1
            val dIdx = YUE_ZHI.indexOf(lun.dayGanZhi[1].toString()) + 1
            val hIdx = SHI_ZHI.indexOf(lun.hourZhi) + 1
            val total = yIdx + mIdx + dIdx + hIdx
            val mod = if (total < 12) 12 - total else (if (total % 12 == 0) 12 else total % 12)
            yueJiang = YUE_JIANG2[mod - 1][0]
        }
        val yjIter = Circular(_12YUEJIANG, _12YUEJIANG.indexOf(yueJiang))
        val yueJiangList = linkedMapOf<String, String>()
        val idx = _12DIZHI.indexOf(zhanTime)
        for (i in idx until 12) yueJiangList[_12DIZHI[i]] = yjIter.next()
        for (i in 0 until idx) yueJiangList[_12DIZHI[i]] = yjIter.next()

        // 贵人(人盘)
        val hour = _HOUR_ZHI[zhanTime] ?: 0
        val isDaytime = when (guirenMethod) { 1 -> hour >= 5 && hour < 17; 2 -> true; else -> false }
        val guiren = TIANYI[riZhu[0]]?.let { if (isDaytime) it[0] else it[1] } ?: "丑"
        val guiShenList = linkedMapOf<String, String>()
        for (i in 0 until 12) {
            val dz = _12DIZHI[i]
            if (yueJiangList[dz] == guiren) {
                val forward = if (guirenSunni == 1) "亥子丑寅卯辰".contains(dz) else isMan
                val gsIter = Circular(_12SHEN, 0, forward)
                for (x in i until 12) guiShenList[_12DIZHI[x]] = gsIter.next()
                for (x in 0 until i) guiShenList[_12DIZHI[x]] = gsIter.next()
                break
            }
        }

        // 天干
        val tianganList = linkedMapOf<String, String>()
        for (i in 0 until 12) {
            val dz = _12DIZHI[i]
            if (yueJiangList[dz] == riZhu[1]) {
                val forward = if (guirenSunni == 1) true else isMan
                val tgIter = Circular(_10TIANGAN, _10TIANGAN.indexOf(riZhu[0]), forward)
                for (x in i until 12) tianganList[_12DIZHI[x]] = tgIter.next()
                for (x in 0 until i) tianganList[_12DIZHI[x]] = tgIter.next()
                break
            }
        }
        // 上面 idx2 仅为占位，实际循环范围就是 0 until i；修正
        // （Kotlin 已用 i，无需 idx2）—— 见下方重写

        // 四课
        val fourKe = mutableListOf<List<String>>()
        val ji = JIGONG[riZhu[0]] ?: "寅"
        fun locateAndGet(dz: String): Pair<String, String> {
            val i = _12DIZHI.indexOf(dz)
            return Pair(yueJiangList[_12DIZHI[i]] ?: "", guiShenList[_12DIZHI[i]] ?: "")
        }
        val k1 = locateAndGet(ji)
        fourKe.add(listOf(riZhu[0], k1.first, k1.second))
        val k2 = locateAndGet(k1.first)
        fourKe.add(listOf(k1.first, k2.first, k2.second))
        val k3 = locateAndGet(riZhu[1])
        fourKe.add(listOf(riZhu[1], k3.first, k3.second))
        val k4 = locateAndGet(k3.first)
        fourKe.add(listOf(k3.first, k4.first, k4.second))

        // 三传(720课)
        val threeChuan = mutableListOf<List<String>>()
        val sanStr = DaLiuRenData.KE720[lun.dayGanZhi]?.get(k1.first) ?: "子子子"
        for (ch in sanStr) {
            val s = ch.toString()
            val i = _12DIZHI.indexOfFirst { yueJiangList[it] == s }
            if (i >= 0) {
                threeChuan.add(listOf(s, tianganList[_12DIZHI[i]] ?: "", guiShenList[_12DIZHI[i]] ?: "", liuQin(riZhu[0], s)))
            } else {
                threeChuan.add(listOf(s, "", "", liuQin(riZhu[0], s)))
            }
        }

        // 十二宫
        val xiazhi = SolarTerm.termTime(t.year, "夏至")
        val dongzhi = SolarTerm.termTime(t.year, "冬至")
        val isYang = !(xiazhi != null && dongzhi != null && t.isAfter(xiazhi) && t.isBefore(dongzhi))
        val mingZhiIndex = _12DIZHI.indexOf(yueJiang) + 1 + _12DIZHI.indexOf(lun.hourZhi) + 1 - 4
        // 原版 JS 数组负索引返回 undefined 不崩；Kotlin 需按 12 循环归一化（floorMod）避免越界闪退。
        // 例：小雪后月将寅 + 卯时 → mingZhiIndex=-1，旧代码 _12DIZHI[-2] 直接崩溃。
        val mIdxAdj = Math.floorMod(mingZhiIndex - 1, 12)
        val mingZhi = _12DIZHI[mIdxAdj]
        val palaces = arrayOf("命", "兄", "妻", "子", "财", "疾", "迁", "仆", "禄", "田", "德", "母")
        val gong12 = linkedMapOf<String, String>()
        val base = _12DIZHI.indexOf(mingZhi)
        for (i in 0 until 12) {
            val d = if (isYang) (base + i) % 12 else (base - i + 12) % 12
            gong12[_12DIZHI[d]] = palaces[i]
        }

        // 十二年煞
        val nianShaList = arrayOf("太岁", "太阳", "丧门", "合神", "官符", "小耗", "大耗", "年墓", "白虎", "德神", "吊客", "病符")
        val yearStart = SHI_ZHI.indexOf(lun.yearGanZhi[1].toString())
        val nianSha = linkedMapOf<String, String>()
        for (i in 0 until 12) nianSha[SHI_ZHI[(yearStart + i) % 12]] = nianShaList[i]

        // 阴盘三传(用神)
        var sym3chuan: Map<String, List<String>>? = null
        if (yongShen.length >= 2) {
            val sym = listOf(lun.hourZhi, yueJiang, jiaziYear[1].toString())
            val tg = mutableListOf<String>()
            tg.add(yongShen[0].toString())
            var a = yueJiangList[JIGONG[yongShen[0].toString()]] ?: ""
            tg.add(a); a = yueJiangList[a] ?: ""; tg.add(a); a = yueJiangList[a] ?: ""; tg.add(a)
            if (yueJiang == lun.hourZhi) { tg[2] = xing(tg[2]); tg[3] = xing(tg[2]) }
            val dz = mutableListOf<String>()
            dz.add(yongShen[1].toString())
            var b = yueJiangList[dz[0]] ?: ""; dz.add(b); b = yueJiangList[b] ?: ""; dz.add(b); b = yueJiangList[b] ?: ""; dz.add(b)
            if (yueJiang == lun.hourZhi) { dz[2] = xing(dz[2]); dz[3] = xing(dz[2]) }
            sym3chuan = mapOf("天干三传" to tg, "地支三传" to dz, "时运命" to sym)
        }

        val dateText = "${t.year}年${t.monthValue}月${t.dayOfMonth}日 ${t.hour}时${t.minute}分(农历${lun.month}月${lun.day}日 ${lun.hourZhi}时)"
        return Result(dateText, listOf(lun.yearGanZhi, lun.monthGanZhi, lun.dayGanZhi, lun.hourGanZhi),
            zhanTime, yueJiang, isMan, jiaziYear, xn, isDaytime,
            JishiyuBase.queryKongwang(lun.dayGanZhi),
            yueJiangList, guiShenList, tianganList, fourKe, threeChuan, nianSha, gong12, sym3chuan,
            jieqiInfo(t))
    }
}
