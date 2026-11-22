package com.fortune.stall.engine.jishiyu

/**
 * 八字旺衰/格局/用神分析 —— 1:1 移植自吉时雨 bazianalyzer.js (AGPL-3.0, xianbo.chen@gmail.com)
 * 入参为四柱干支数组 ["年干支","月干支","日干支","时干支"]。
 */
object BaziAnalyzer {
    private val WX = mapOf("甲" to "木","乙" to "木","丙" to "火","丁" to "火","戊" to "土","己" to "土","庚" to "金","辛" to "金","壬" to "水","癸" to "水")
    private val ZHI_EL = mapOf("子" to "水","丑" to "土","寅" to "木","卯" to "木","辰" to "土","巳" to "火","午" to "火","未" to "土","申" to "金","酉" to "金","戌" to "土","亥" to "水")
    private val GEN = mapOf("木" to "火","火" to "土","土" to "金","金" to "水","水" to "木")
    private val CTRL = mapOf("木" to "土","土" to "水","水" to "火","火" to "金","金" to "木")
    private val HIDDEN = mapOf(
        "子" to arrayOf("癸"), "丑" to arrayOf("己","癸","辛"), "寅" to arrayOf("甲","丙","戊"), "卯" to arrayOf("乙"),
        "辰" to arrayOf("戊","乙","癸"), "巳" to arrayOf("丙","戊","庚"), "午" to arrayOf("丁","己"), "未" to arrayOf("己","丁","乙"),
        "申" to arrayOf("庚","壬","戊"), "酉" to arrayOf("辛"), "戌" to arrayOf("戊","辛","丁"), "亥" to arrayOf("壬","甲"))
    private val SEASON = mapOf("寅" to "warm","卯" to "warm","巳" to "hot","午" to "hot","申" to "cool","酉" to "cool",
        "亥" to "cold","子" to "cold","辰" to "neutral","戌" to "neutral","丑" to "cold","未" to "hot")

    data class P(val gan: String, val zhi: String)
    data class Analysis(
        val wangshuai: String, val congge: String?, val geju: String?,
        val yongshen: List<String>, val tiaohou: List<String>,
        val score: Int, val detail: List<String>,
        val personality: String, val dominantWuxing: String, val dominantShishen: String,
        val behavior: String, val style: String, val risks: List<String>
    )

    private fun relation(day: String, other: String): String? = when {
        day == other -> "比劫"
        GEN[other] == day -> "印"
        GEN[day] == other -> "食伤"
        CTRL[day] == other -> "财"
        CTRL[other] == day -> "官杀"
        else -> null
    }

    fun analyze(arr: List<String>): Analysis {
        val bz = arr.map { P(it[0].toString(), it[1].toString()) }
        val year=bz[0]; val month=bz[1]; val day=bz[2]; val hour=bz[3]
        val dayEl = WX[day.gan] ?: "木"

        // 基础旺衰
        var score = 0
        val detail = mutableListOf<String>()
        val monthEl = ZHI_EL[month.zhi] ?: "木"
        val m = when { monthEl==dayEl -> 5; GEN[monthEl]==dayEl -> 3; CTRL[monthEl]==dayEl -> -4; GEN[dayEl]==monthEl -> -2; else -> 0 }
        score += m; detail.add("月令 $m")
        for ((pos, p) in listOf("年" to year,"月" to month,"日" to day,"时" to hour)) {
            val rel = relation(dayEl, ZHI_EL[p.zhi] ?: "")
            val sc = if (rel=="比劫"||rel=="印") 2 else if(rel=="官杀") -2 else -1
            score += sc; detail.add("${pos}支($rel) $sc")
        }
        for ((pos, p) in listOf("年" to year,"月" to month,"时" to hour)) {
            val rel = relation(dayEl, WX[p.gan] ?: "")
            val sc = if (rel=="比劫"||rel=="印") 1 else -1
            score += sc; detail.add("${pos}干($rel) $sc")
        }
        // 通根
        fun rootScore(zhi: String): Double {
            var s=0.0
            HIDDEN[zhi]?.forEachIndexed { idx, g ->
                val w = if(idx==0) 1.5 else if(idx==1) 1.0 else 0.5
                when (relation(dayEl, WX[g] ?: "")) {
                    "比劫","印" -> s += 2*w
                    "食伤","财" -> s -= 1*w
                    "官杀" -> s -= 1.5*w
                }
            }
            return s
        }
        val root = listOf(year,month,day,hour).sumOf { rootScore(it.zhi) }
        val total = score + root.toInt()

        // 从格
        var support=0; var oppose=0
        listOf(year,month,day,hour).forEach { p -> HIDDEN[p.zhi]?.forEach { g ->
            if (relation(dayEl, WX[g] ?: "") in listOf("比劫","印")) support++ else oppose++
        }}
        val cong = if (support>=8 && oppose<=2) "从强" else if (oppose>=8 && support<=2) "从弱" else null

        // 格局
        val geju = when (relation(dayEl, WX[month.gan] ?: "")) {
            "官杀" -> "官格"; "财" -> "财格"; "食伤" -> "食伤格"; else -> null
        }

        // 调候
        val baseTemp = SEASON[month.zhi] ?: "neutral"
        var hot=0; var cold=0
        listOf(year,month,day,hour).forEach { p ->
            val eg=WX[p.gan] ?: ""; val ez=ZHI_EL[p.zhi] ?: ""
            if(eg=="火") hot+=2; if(eg=="土") hot+=1; if(eg=="水") cold+=2; if(eg=="金") cold+=1
            if(ez=="火") hot+=2; if(ez=="土") hot+=1; if(ez=="水") cold+=2; if(ez=="金") cold+=1
        }
        val diff = hot-cold
        val primary = mutableListOf<String>(); val secondary = mutableListOf<String>()
        if(baseTemp=="cold") primary.add("火"); if(baseTemp=="hot") primary.add("水")
        if(baseTemp=="cool") secondary.add("火"); if(baseTemp=="warm") secondary.add("水")
        if(diff>=4) { primary.clear(); primary.add("水") } else if(diff<=-4) { primary.clear(); primary.add("火") }
        if(dayEl=="木" && (baseTemp=="cold"||diff < -2)) primary.add(0,"火")
        if(dayEl=="金" && (baseTemp=="hot"||diff > 2)) primary.add(0,"水")
        if(dayEl=="水" && diff < -3) { primary.add(0,"土"); primary.add(1,"火") }
        if(dayEl=="火" && diff > 3) primary.add(0,"水")
        val tiaohou = (primary + secondary).distinct()

        // 用神
        var yongshen: List<String> = emptyList()
        val shishen = when {
            cong=="从强" -> listOf("比劫","印")
            cong=="从弱" -> listOf("财","官杀","食伤")
            total>=5 -> listOf("食伤","财","官杀")
            total<=-5 -> listOf("印","比劫")
            else -> emptyList()
        }
        if (shishen.isNotEmpty()) {
            val beGen = GEN.entries.first { it.value==dayEl }.key
            val beCtrl = CTRL.entries.first { it.value==dayEl }.key
            val map = mapOf("比劫" to dayEl,"印" to beGen,"食伤" to (GEN[dayEl]?:""),"财" to (CTRL[dayEl]?:""),"官杀" to beCtrl)
            yongshen = shishen.map { map[it] ?: "" }.distinct()
        } else {
            yongshen = listOf("调和")
        }

        // 人格
        val wangshuai = if(total>=5) "strong" else if(total<=-5) "weak" else "balanced"
        val wxStats = mutableMapOf("木" to 0,"火" to 0,"土" to 0,"金" to 0,"水" to 0)
        listOf(year,month,day,hour).forEach { p ->
            val gk = WX[p.gan] ?: ""; val zk = ZHI_EL[p.zhi] ?: ""
            wxStats[gk] = (wxStats[gk] ?: 0) + 1
            wxStats[zk] = (wxStats[zk] ?: 0) + 1
        }
        val ssStats = mutableMapOf("比劫" to 0,"印" to 0,"食伤" to 0,"财" to 0,"官杀" to 0)
        listOf(year,month,day,hour).forEach { p -> HIDDEN[p.zhi]?.forEach { g -> relation(dayEl, WX[g] ?: "")?.let { ssStats[it]=(ssStats[it] ?: 0)+1 } } }
        val domWx = wxStats.maxByOrNull { it.value }?.key ?: "木"
        val domSs = ssStats.maxByOrNull { it.value }?.key ?: "比劫"
        val core = mapOf("木" to "成长型人格（理想、上进、直）","火" to "表达型人格（外向、热情、表现欲）","土" to "稳定型人格（务实、包容、责任）","金" to "规则型人格（理性、自律、原则）","水" to "思考型人格（敏感、灵活、多变）")[dayEl] ?: ""
        val behavior = mapOf("比劫" to "自我意识强，独立性高，竞争心强","印" to "重安全感，偏内向，依赖性较强","食伤" to "表达欲强，有创造力，略叛逆","财" to "现实务实，重资源与控制","官杀" to "自律守规，有压力感，责任强")[domSs] ?: ""
        val style = when(wangshuai) { "strong" -> "外向主动型"; "weak" -> "内敛被动型"; else -> "平衡型" }
        val risks = mutableListOf<String>()
        if((ssStats["食伤"]?:0)>=4) risks.add("情绪外放，易冲动")
        if((ssStats["官杀"]?:0)>=4) risks.add("压力大，易焦虑")
        if((ssStats["比劫"]?:0)>=4) risks.add("固执，好胜")
        if((wxStats["火"]?:0)>=4) risks.add("急躁")
        if((wxStats["水"]?:0)>=4) risks.add("多思敏感")

        return Analysis(
            if(total>=5) "身强" else if(total<=-5) "身弱" else "中和",
            cong, geju, yongshen, tiaohou, total, detail,
            core, domWx, domSs, behavior, style, risks)
    }
}
