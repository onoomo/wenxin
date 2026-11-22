package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import com.fortune.stall.util.SolarTerm
import java.time.LocalDateTime

/**
 * 奇门遁甲(阴盘) —— 1:1 移植自吉时雨 qimen.js (AGPL-3.0, xianbo.chen@gmail.com)
 * 地盘、旬首、值符值使、八神、天盘九星、八门、隐干、马星空亡等。常规时盘(非刻盘/非山向)。
 */
object Qimen {
    data class Cell(
        var dipan: String = "", var dipan12: String = "", var dipanJi: String = "", var dipanJi12: String = "",
        var tianpan: String = "", var tianpan12: String = "", var tianpanJi: String = "", var tianpanJi12: String = "",
        var jiuxing: String = "", var men: String = "", var bashen: String = "",
        var yingan: String = "", var yinganJi: String = "", var diShen: String = "",
        var ma: Boolean = false, var kongwang: Boolean = false,
        var dipan_jixing: Boolean = false, var tianpan_jixing: Boolean = false,
        var dipan_rumu: Boolean = false, var tianpan_rumu: Boolean = false,
        var men_po: Boolean = false
    )

    data class Result(
        val date: String, val siZhu: List<String>, val jieqi: String,
        val yangDun: Boolean, val jushu: Int, val xunhead: String, val kongwang: String,
        val zhifu: String, val zhishi: String, val zhifuGong: String, val zhishiGong: String,
        val maxing: String, val yueJiang: String, val yueJiangName: String,
        val pan: Map<String, Cell>, val rows: List<Pair<String, String>>,
        // 山向奇门附加信息（常规盘为 null）
        val shanxiang: ShanxiangQimen.Def? = null
    )

    private fun <T> circ(arr: List<T>, start: Int, forward: Boolean = true): Iterator<T> = object : Iterator<T> {
        var idx = start
        override fun hasNext() = true
        override fun next(): T { val v = arr[idx]; idx = if (forward) (idx + 1) % arr.size else (idx - 1 + arr.size) % arr.size; return v }
    }

    fun paipan(dtInput: LocalDateTime, isman: Boolean, cusJushu: Int = 0, longitude: Double? = null,
               shanDegree: Int? = null): Result {
        val dt = if (longitude != null) dtInput.plusMinutes(((longitude - 120.0) * 4.0).toLong()) else dtInput
        val lun = LunarCalendar.fromDateTime(dt)
        val sx = shanDegree?.let { ShanxiangQimen.findByDegree(it) }
        // 山向模式：以年柱代日柱、年干五鼠遁按「向」支取时柱
        val yearZhuForSx = ShanxiangQimen.yearGanZhi(dt.year)
        var shiZhu = arrayOf(lun.hourGanZhi[0].toString(), lun.hourGanZhi[1].toString())
        if (sx != null) {
            val sz = ShanxiangQimen.shiZhuByYearGan(yearZhuForSx[0].toString(), sx)
            if (sz.length == 2) shiZhu = arrayOf(sz[0].toString(), sz[1].toString())
        }
        val yIdx = LunarCalendar.DIZHI.indexOf(lun.yearGanZhi[1].toString()) + 1
        val mIdx = kotlin.math.abs(lun.month)
        val dIdx = lun.day + if (dt.hour == 23) 1 else 0
        val hIdx = LunarCalendar.DIZHI.indexOf(lun.hourZhi) + 1

        val yangDun: Boolean; val jushu: Int
        if (sx != null) {
            jushu = kotlin.math.abs(sx.ju)
            yangDun = sx.ju > 0
        } else if (cusJushu == 0) {
            jushu = let { val j = (yIdx + mIdx + dIdx + hIdx) % 9; if (j == 0) 9 else j }
            val xz = SolarTerm.termTime(dt.year, "夏至"); val dz = SolarTerm.termTime(dt.year, "冬至")
            yangDun = !(xz != null && dz != null && dt.isAfter(xz) && dt.isBefore(dz))
        } else { yangDun = cusJushu > 0; jushu = kotlin.math.abs(cusJushu) }

        // 月将
        val qiNames = arrayOf("冬至", "大寒", "雨水", "春分", "谷雨", "小满", "夏至", "大暑", "处暑", "秋分", "霜降", "小雪")
        var prevQi = "冬至"; var best: LocalDateTime? = null
        for (y in (dt.year - 1)..dt.year) for (nm in qiNames) {
            val tt = SolarTerm.termTime(y, nm) ?: continue
            if (!tt.isAfter(dt) && (best == null || tt.isAfter(best))) { best = tt; prevQi = nm }
        }
        val yueJiang = QimenData.YUE_JIANG[prevQi]?.get(0) ?: "丑"
        val yueJiangName = QimenData.YUE_JIANG[prevQi]?.get(1) ?: "大吉"

        val pan = linkedMapOf<String, Cell>()
        QimenData._8GUA.forEach { pan[it] = Cell() }

        // 地盘
        val Q3 = QimenData._3Q6Y; val G8 = QimenData._8GUA
        val ZS12 = QimenData.ZHANG_SHENG_12
        if (yangDun) {
            // 阳遁：从 jushu 宫起，按 G8 顺序（索引 0..8）顺布九宫
            var idx = (jushu - 1) % 9
            for (i in 0 until 9) {
                val gong = G8[idx]
                pan[gong]!!.dipan = Q3[i]
                if (gong != "中") pan[gong]!!.dipan12 = ZS12[Q3[i]]?.get(gong) ?: ""
                idx = (idx + 1) % 9
            }
        } else {
            // 阴遁：从 jushu 宫起，按 G8 逆序布九宫
            var idx = (jushu - 1) % 9
            for (i in 0 until 9) {
                val gong = G8[idx]
                pan[gong]!!.dipan = Q3[i]
                if (gong != "中") pan[gong]!!.dipan12 = ZS12[Q3[i]]?.get(gong) ?: ""
                idx = (idx - 1 + 9) % 9
            }
        }
        if (pan["中"]!!.dipan.isNotEmpty()) {
            pan["坤"]!!.dipanJi = pan["中"]!!.dipan
            pan["坤"]!!.dipanJi12 = ZS12[pan["中"]!!.dipan]?.get("坤") ?: ""
        }

        // 旬首
        val hourCol = shiZhu.joinToString("")
        var xunhead = "戊"
        for (k in QimenData._XUNHEAD.keys) if (hourCol in QimenData._XUNHEAD[k]!!) { xunhead = k; break }

        // 值符值使
        var zhifu = ""; var zhishi = ""
        for (key in pan.keys) if (pan[key]!!.dipan == xunhead) {
            var idx = G8.indexOf(key)
            zhifu = QimenData._9STAR[idx]
            if (idx == 4) idx = 1
            zhishi = QimenData._8MEN[idx]
            break
        }

        // 八神
        val hourGan = shiZhu[0]
        val G8S = QimenData._8GUA_S; val G8N = QimenData._8GUA_N
        val S8S = QimenData._8SHEN_S; val S8N = QimenData._8SHEN_N
        var zhifuGong = ""
        for (key in pan.keys) if (pan[key]!!.dipan == (if (hourGan == "甲") xunhead else hourGan)) {
            if (yangDun) {
                val idx = G8S.indexOf(if (key == "中") "坤" else key)
                for (i in idx until 8) { val g = G8S[i]; pan[g]!!.bashen = S8S[i - idx]; if (S8S[i - idx] == "符") zhifuGong = g }
                if (idx > 0) for (i in 0 until idx) { val j = 8 - idx + i; pan[G8S[i]]!!.bashen = S8S[j]; if (S8S[j] == "符") zhifuGong = G8S[i] }
            } else {
                val idx = G8N.indexOf(if (key == "中") "坤" else key)
                for (i in idx until 8) { val g = G8N[i]; pan[g]!!.bashen = S8S[i - idx]; if (S8N[i - idx] == "符") zhifuGong = g }
                if (idx > 0) for (i in 0 until idx) { val j = 8 - idx + i; pan[G8N[i]]!!.bashen = S8S[j]; if (S8N[j] == "符") zhifuGong = G8N[i] }
            }
            break
        }

        // 天盘 & 九星
        fun findGuaIdx(dipanVal: String): Int {
            for (k in G8S.indices) if (pan[G8S[k]]!!.dipan == dipanVal) return k
            if (pan["中"]!!.dipan == dipanVal) return G8S.indexOf("坤")
            return -1
        }
        val midx = findGuaIdx(xunhead)
        val orderedM = G8S.drop(midx) + G8S.take(midx)
        val dipanList = orderedM.map { pan[it]!!.dipan }.toMutableList()
        val tidx = findGuaIdx(if (hourGan == "甲") xunhead else hourGan)
        val orderedT = G8S.drop(tidx) + G8S.take(tidx)
        for (gua in orderedT) {
            val tp = if (dipanList.isNotEmpty()) dipanList.removeAt(0) else ""
            pan[gua]!!.tianpan = tp
            if (gua != "中") pan[gua]!!.tianpan12 = ZS12[tp]?.get(gua) ?: ""
            if (tp == pan["坤"]!!.dipan) {
                pan[gua]!!.tianpanJi = pan["中"]!!.dipan
                pan[gua]!!.tianpanJi12 = ZS12[pan[gua]!!.tianpanJi]?.get(gua) ?: ""
            }
        }
        val S9 = QimenData._9STAR_S
        val starArr = S9.drop(midx) + S9.take(midx)
        orderedT.forEachIndexed { i, g -> pan[g]!!.jiuxing = starArr[i] }

        // 八门
        var zhishiGong = ""
        for (key in pan.keys) if (pan[key]!!.dipan == xunhead) {
            val gua8: List<String> = if (yangDun) G8.toList() else G8.reversed()
            val idx = gua8.indexOf(key)
            val xidx = QimenData._XUNHEAD[xunhead]!!.indexOf(hourCol)
            var mgidx = idx + xidx; if (mgidx > 8) mgidx %= 9
            zhishiGong = gua8[mgidx]
            var midx2 = mgidx; if (midx2 == 4) midx2 = 1
            var i0 = G8S.indexOf(gua8[midx2])
            if (!yangDun && zhishiGong == "中") i0 = G8S.indexOf("坤")
            var midx3 = G8S.indexOf(if (key == "中") "坤" else key)
            var j = midx3
            val M8 = QimenData._8MEN_S
            for (i in i0 until 8) { if (j >= 8) j = 0; pan[G8S[i]]!!.men = M8[j]; j++ }
            if (i0 > 0) for (i in 0 until i0) { if (j >= 8) j = 0; pan[G8S[i]]!!.men = M8[j]; j++ }
            break
        }

        // 隐干
        val isFuyin = pan["乾"]!!.tianpan == pan["乾"]!!.dipan
        val isFullFuyin = isFuyin && pan["坎"]!!.men == "休" && pan["坎"]!!.jiuxing == "蓬"
        val qiyi0 = if (yangDun) Q3.toList() else Q3.reversed()
        fun yingan1() {
            val gans = mutableListOf<String>(); var tp = ""; var tpj = ""
            for (k in G8S.indices) if (pan[G8S[k]]!!.tianpan == hourGan || pan[G8S[k]]!!.tianpanJi == hourGan) {
                val idx = if (G8S[k] == "中") G8S.indexOf("坤") else k
                val ordered = G8S.drop(idx) + G8S.take(idx)
                for (g in ordered) { gans.add(pan[g]!!.tianpan); if (pan[g]!!.tianpanJi.isNotEmpty()) { tp = pan[g]!!.tianpan; tpj = pan[g]!!.tianpanJi } }
                break
            }
            val zi = G8S.indexOf(if (zhishiGong == "中") "坤" else zhishiGong)
            val orderedG = G8S.drop(zi) + G8S.take(zi)
            orderedG.forEachIndexed { i, g -> pan[g]!!.yingan = gans.getOrNull(i) ?: ""; if (gans.getOrNull(i) == tp) pan[g]!!.yinganJi = tpj }
        }
        if (!isFuyin) yingan1() else {
            val searchKey = when {
                hourGan == "甲" && zhishiGong != "中" -> xunhead
                (hourGan == "甲" && zhishiGong == "中") || (isFullFuyin && zhishiGong == "中") -> pan["坤"]!!.dipan
                isFullFuyin && hourGan != "甲" && zhishiGong != "中" -> hourGan
                else -> ""
            }
            if (searchKey.isEmpty()) yingan1() else {
                var qiyi = qiyi0.toMutableList()
                val idx = qiyi.indexOf(searchKey); val ji = qiyi.getOrNull(idx) ?: ""
                qiyi = (qiyi.drop(idx + 1) + qiyi.take(idx)).toMutableList()
                for (i in 5 until 9) { pan[G8[i]]!!.yingan = qiyi.getOrNull(i - 5) ?: ""; if (G8[i] == "坤") pan[G8[i]]!!.yinganJi = ji }
                for (i in 0 until 4) { pan[G8[i]]!!.yingan = qiyi.getOrNull(i + 4) ?: ""; if (G8[i] == "坤") pan[G8[i]]!!.yinganJi = ji }
            }
        }

        // 地盘八神：取值符宫天盘干 → 找地盘=该干的宫为起点 → 阳遁顺(CLOCKWISE)阴遁逆排 符蛇阴六白玄九天
        run {
            val zg = if (zhifuGong == "中" || zhifuGong.isEmpty()) "坤" else zhifuGong
            val zfGan = pan[zg]!!.tianpan
            if (zfGan.isNotEmpty()) {
                var startGong = ""
                for (g in G8S) if (pan[g]!!.dipan == zfGan) { startGong = g; break }
                if (startGong.isEmpty() && pan["中"]!!.dipan == zfGan) startGong = "坤"
                if (startGong.isNotEmpty()) {
                    val seq = if (yangDun) G8S.toList() else G8S.reversed()
                    val si = seq.indexOf(startGong)
                    if (si >= 0) {
                        val diGods = arrayOf("符", "蛇", "阴", "六", "白", "玄", "九", "天")
                        for (i in 0 until 8) pan[seq[(si + i) % 8]]!!.diShen = diGods[i]
                    }
                }
            }
        }

        // 门破/击刑/入墓/马/空亡
        val menpoMap = mapOf("乾" to "景", "坎" to "生死", "艮" to "伤杜", "震" to "开惊", "巽" to "开惊", "离" to "休", "坤" to "伤杜", "兑" to "景")
        for (k in pan.keys) if (k != "中" && menpoMap[k]?.contains(pan[k]!!.men) == true) pan[k]!!.men_po = true
        val jixingMap = mapOf("艮" to "庚", "震" to "戊", "巽" to "壬癸", "离" to "辛", "坤" to "己")
        for (k in pan.keys) if (k != "中" && jixingMap[k] != null) {
            val gs = jixingMap[k]!!
            if (gs.contains(pan[k]!!.dipan)) pan[k]!!.dipan_jixing = true
            if (gs.contains(pan[k]!!.tianpan)) pan[k]!!.tianpan_jixing = true
        }
        val rumuMap = mapOf("乾" to "乙丙戊", "艮" to "丁己庚", "巽" to "辛壬", "坤" to (if (zhifuGong == "坤") xunhead else "") + "癸")
        for (k in pan.keys) if (k != "中" && rumuMap[k] != null) {
            val gs = rumuMap[k]!!
            if (gs.contains(pan[k]!!.dipan)) pan[k]!!.dipan_rumu = true
            if (gs.contains(pan[k]!!.tianpan)) pan[k]!!.tianpan_rumu = true
        }
        val maMap = mapOf("亥卯未" to arrayOf("巽", "巳"), "申子辰" to arrayOf("艮", "寅"), "寅午戌" to arrayOf("坤", "申"), "巳酉丑" to arrayOf("乾", "亥"))
        var maxing = ""
        for (k in maMap.keys) if (k.contains(shiZhu[1])) { pan[maMap[k]!![0]]!!.ma = true; maxing = maMap[k]!![1]; break }
        val kwMap = mapOf("戊" to arrayOf("乾"), "癸" to arrayOf("坎", "艮"), "壬" to arrayOf("艮", "震"), "辛" to arrayOf("巽"), "庚" to arrayOf("离", "坤"), "己" to arrayOf("坤", "兑"))
        kwMap[xunhead]?.forEach { pan[it]!!.kongwang = true }

        // 展示行（九宫洛书）
        val visual = arrayOf(arrayOf("巽", "离", "坤"), arrayOf("震", "中", "兑"), arrayOf("艮", "坎", "乾"))
        val rows = visual.map { row ->
            val sb = StringBuilder()
            for (g in row) { val c = pan[g]!!; sb.append("[$g ${c.tianpan}${c.jiuxing}${c.men}${c.bashen}·${c.dipan}]") }
            "" to sb.toString()
        }.toMutableList()

        val range = SolarTerm.rangeAt(dt)
        val jieqi = range?.let { "${it.from.name} ~ ${it.to.name}" } ?: ""
        val dateText = "${dt.year}年${dt.monthValue}月${dt.dayOfMonth}日 ${dt.hour}时${dt.minute}分(农历${lun.month}月${lun.day}日)"
        // 山向模式：展示四柱 = 年柱、月柱、年柱(代日柱)、五鼠遁时柱(代时柱)
        val siZhuShow = if (sx != null)
            listOf(yearZhuForSx, lun.monthGanZhi, yearZhuForSx, shiZhu.joinToString(""))
        else listOf(lun.yearGanZhi, lun.monthGanZhi, lun.dayGanZhi, lun.hourGanZhi)
        return Result(dateText, siZhuShow,
            jieqi, yangDun, jushu, QimenData.XUNHEAD[xunhead] ?: "", QimenData.KONGWANG[xunhead] ?: "",
            "天$zhifu", "${zhishi}门", zhifuGong, zhishiGong, maxing, yueJiang, yueJiangName, pan, rows, sx)
    }
}
