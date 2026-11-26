package com.fortune.stall.engine

import kotlin.random.Random

/**
 * 雷诺曼牌（Petit Lenormand）引擎 —— 移植自 Brhiza/mingyu
 * packages/core/src/divination/{lenormand-data, algorithms/lenormand, lenormand-grid}.ts
 *
 * 36 张固定牌序 + 8 种牌阵 + 固定两牌组合 + 相邻合读 + 九宫十字距离。
 * 抽牌为 Fisher-Yates 洗牌，同一次牌阵不重复；大桌按 9 列排布并标注宫位牌。
 */
object Lenormand {

    data class Card(val id: Int, val name: String, val keywords: List<String>, val meaning: String)

    data class Drawn(
        val card: Card,
        val position: String,
        val house: String? = null,
        val row: Int? = null,
        val column: Int? = null
    )

    data class Combination(
        val card1: String, val card2: String,
        val position1: String, val position2: String,
        val relation: String, val rowDistance: Int, val columnDistance: Int,
        val meaning: String, val source: String
    )

    data class Spread(val key: String, val name: String, val positions: List<String>)

    data class GridDistance(val card: Card, val distance: Int, val relationship: String)

    data class CrossAnalysis(
        val centerCard: Card,
        val topCard: Card, val bottomCard: Card, val leftCard: Card, val rightCard: Card,
        val diagonal: Map<String, Card>,
        val distances: List<GridDistance>,
        val summary: String
    )

    data class Result(
        val spreadKey: String,
        val spreadName: String,
        val cards: List<Drawn>,
        val combinations: List<Combination>,
        val layoutEvidence: List<String>
    )

    /** 36 张通行雷诺曼牌（牌号=牌序，唯一） */
    val DECK: List<Card> = listOf(
        Card(1, "骑士", listOf("消息", "到来", "进展"), "消息抵达，事情开始移动。"),
        Card(2, "三叶草", listOf("机会", "短暂好运", "轻松"), "短期机会出现，宜快不宜拖。"),
        Card(3, "船", listOf("远方", "变化", "出行"), "有距离、迁移或方向转换。"),
        Card(4, "房子", listOf("家庭", "稳定", "根基"), "关注安全感、住所和基础条件。"),
        Card(5, "树", listOf("成长", "健康", "长期"), "事情发展较慢，但有长期根系。"),
        Card(6, "云", listOf("不明朗", "混乱", "遮蔽"), "信息不清，需要先辨别真假。"),
        Card(7, "蛇", listOf("复杂", "诱惑", "迂回"), "关系或路径有绕行与隐藏动机。"),
        Card(8, "棺材", listOf("结束", "停滞", "转折"), "旧阶段需要收尾，不能强行续命。"),
        Card(9, "花束", listOf("喜悦", "礼物", "好感"), "有善意、邀请或被认可的机会。"),
        Card(10, "镰刀", listOf("切断", "突发", "决断"), "快速分割或突然变化，需要果断。"),
        Card(11, "鞭子", listOf("争执", "重复", "压力"), "反复拉扯，容易因沟通产生摩擦。"),
        Card(12, "鸟", listOf("沟通", "焦虑", "讨论"), "消息频繁，但情绪也容易放大。"),
        Card(13, "孩子", listOf("新开始", "单纯", "试探"), "事情尚早，适合从小步尝试。"),
        Card(14, "狐狸", listOf("策略", "警惕", "工作"), "需要看清利益结构，避免被套路。"),
        Card(15, "熊", listOf("力量", "资源", "保护"), "资源和权力是关键，也可能有人强势介入。"),
        Card(16, "星星", listOf("希望", "愿景", "网络"), "目标感增强，远景或线上资源有帮助。"),
        Card(17, "鹳", listOf("变化", "迁移", "改善"), "适合调整环境，局势有改善空间。"),
        Card(18, "狗", listOf("忠诚", "朋友", "支持"), "可信任的人或长期关系会提供支持。"),
        Card(19, "塔", listOf("机构", "隔离", "规则"), "制度、边界或距离感是重点。"),
        Card(20, "花园", listOf("社交", "公开", "圈层"), "事情会进入公开场域或社交圈。"),
        Card(21, "山", listOf("阻碍", "延迟", "困难"), "推进受阻，需要绕路或等待。"),
        Card(22, "路", listOf("选择", "分岔", "决定"), "关键在选择路径，不能两头都要。"),
        Card(23, "老鼠", listOf("消耗", "损失", "焦虑"), "小问题会持续消耗，需要止损。"),
        Card(24, "心", listOf("感情", "喜欢", "热情"), "情感动机强，适合看真实心意。"),
        Card(25, "戒指", listOf("承诺", "契约", "循环"), "关系、合同或重复模式成为主轴。"),
        Card(26, "书", listOf("秘密", "知识", "未知"), "仍有未公开信息，需继续了解。"),
        Card(27, "信", listOf("文本", "通知", "文件"), "书面消息、通知或证据很重要。"),
        Card(28, "男士", listOf("男性", "主动方", "本人"), "男性角色或主动方成为焦点。"),
        Card(29, "女士", listOf("女性", "接收方", "本人"), "女性角色或接收方成为焦点。"),
        Card(30, "百合", listOf("成熟", "平和", "伦理"), "需要成熟处理，重视体面与长期安稳。"),
        Card(31, "太阳", listOf("成功", "清晰", "能量"), "局势转明，推进条件增强。"),
        Card(32, "月亮", listOf("情绪", "名声", "直觉"), "情绪和外界评价会影响判断。"),
        Card(33, "钥匙", listOf("答案", "突破", "关键"), "关键条件出现，问题有解。"),
        Card(34, "鱼", listOf("金钱", "流动", "资源"), "钱、资源和流动性是重点。"),
        Card(35, "锚", listOf("稳定", "工作", "坚持"), "稳定和长期承诺能带来结果。"),
        Card(36, "十字架", listOf("压力", "宿命感", "责任"), "责任较重，需要承担代价或接受现实。")
    )

    /** 8 种牌阵（key -> 名称 + 牌位序列） */
    val SPREADS: List<Spread> = listOf(
        Spread("single", "单牌线索", listOf("核心线索")),
        Spread("three", "三牌事件线", listOf("起因", "现状", "走向")),
        Spread("five", "五牌十字阵", listOf("过去背景", "当前处境", "隐藏因素", "外在助力", "最终走向")),
        Spread("relationship", "关系牌阵", listOf("你的状态", "对方状态", "关系纽带", "隐藏因素", "后续走向")),
        Spread("decision", "选择牌阵", listOf("当前处境", "选择A", "选择A走向", "选择B", "选择B走向", "关键建议")),
        Spread("nine", "九宫牌阵", listOf("左上", "上方", "右上", "左侧", "核心", "右侧", "左下", "下方", "右下")),
        Spread("element", "元素牌阵", listOf("火（行动/能量）", "水（情感/直觉）", "风（思维/沟通）", "土（物质/根基）")),
        Spread("grandTableau", "大桌牌阵", (1..36).map { "第${it}宫（${DECK[it - 1].name}宫）" })
    )

    fun spreadOf(key: String): Spread = SPREADS.firstOrNull { it.key == key } ?: SPREADS.first()

    /** 固定两牌组合（含先后语义，反向查表时按 DIRECTIONAL 过滤） */
    val FIXED_COMBINATIONS: Map<String, String> = mapOf(
        "骑士+心" to "消息带来感情进展",
        "心+戒指" to "感情的承诺或婚约",
        "心+花束" to "被人喜欢或表白",
        "心+百合" to "成熟的感情关系",
        "戒指+花束" to "订婚或喜讯",
        "戒指+房子" to "家庭契约/购房",
        "花束+花园" to "社交上受到欢迎",
        "房子+锚" to "稳定安家",
        "锚+星星" to "目标明确并趋于稳定",
        "星星+月亮" to "直觉准确的时期",
        "月亮+太阳" to "从迷茫走向清晰",
        "蛇+狐狸" to "欺骗与策略",
        "云+蛇" to "隐藏在迷雾中的欺骗",
        "棺材+十字架" to "沉重的结束与考验",
        "镰刀+棺材" to "突然的结束/切割",
        "老鼠+十字架" to "消耗性的压力",
        "鱼+星星" to "通过网络/远程获利",
        "鱼+船" to "跨国或远距离财运",
        "船+鹳" to "改善环境的搬迁",
        "书+信" to "秘密文件或消息",
        "钥匙+心" to "关键的情感答案",
        "狗+花束" to "朋友的善意",
        "孩子+房子" to "家庭添丁",
        "塔+山" to "制度性阻碍",
        "路+十字架" to "两难选择",
        "鸟+蛇" to "流言蜚语",
        "鞭子+鸟" to "争吵与焦虑",
        "熊+鱼" to "资源或资金充裕",
        "树+心" to "深厚的感情基础"
    )

    private val DIRECTIONAL_KEYS = setOf("骑士+心", "月亮+太阳", "星星+月亮", "锚+星星", "船+鹳")

    private fun fixedMeaning(first: String, second: String): String? {
        FIXED_COMBINATIONS["$first+$second"]?.let { return it }
        val reverse = "$second+$first"
        return if (reverse in DIRECTIONAL_KEYS) null else FIXED_COMBINATIONS[reverse]
    }

    private fun cardById(id: Int): Card? = DECK.firstOrNull { it.id == id }

    /** 手工按牌号录入（1-36，不重复） */
    fun cardsFromIds(ids: List<Int>): List<Card>? {
        if (ids.size != ids.distinct().size) return null
        return ids.map { cardById(it) ?: return null }
    }

    /** 洗牌抽取：Fisher-Yates，取前 n 张 */
    fun shuffleDraw(n: Int, random: Random): List<Card> =
        DECK.toMutableList().apply {
            for (i in size - 1 downTo 1) {
                val j = random.nextInt(i + 1)
                val t = this[i]; this[i] = this[j]; this[j] = t
            }
        }.take(n.coerceAtMost(DECK.size))

    /** 组合生成：网格牌阵取相邻牌，其余取牌序相邻 */
    private fun buildCombinations(spreadKey: String, cards: List<Drawn>): List<Combination> {
        data class PairCandidate(val first: Drawn, val second: Drawn, val relation: String,
                                 val rowDistance: Int, val columnDistance: Int)
        val candidates = if (spreadKey == "nine" || spreadKey == "grandTableau") {
            val pairs = mutableListOf<PairCandidate>()
            for (i in cards.indices) for (j in i + 1 until cards.size) {
                val a = cards[i]; val b = cards[j]
                val rd = kotlin.math.abs((a.row ?: 0) - (b.row ?: 0))
                val cd = kotlin.math.abs((a.column ?: 0) - (b.column ?: 0))
                if (rd <= 1 && cd <= 1 && !(rd == 0 && cd == 0)) {
                    val rel = if (rd == 0) "横向相邻" else if (cd == 0) "纵向相邻" else "对角相邻"
                    pairs.add(PairCandidate(a, b, rel, rd, cd))
                }
            }
            pairs
        } else {
            cards.drop(1).mapIndexed { idx, second ->
                PairCandidate(cards[idx], second, "牌序相邻", 0, 0)
            }.filterNot {
                spreadKey == "decision" && it.first.position == "选择A走向" && it.second.position == "选择B"
            }
        }
        val out = mutableListOf<Combination>()
        candidates.forEach { c ->
            val fm = fixedMeaning(c.first.card.name, c.second.card.name)
            val personNeighbor = spreadKey == "grandTableau" &&
                (c.first.card.name in setOf("男士", "女士") || c.second.card.name in setOf("男士", "女士"))
            if (spreadKey == "grandTableau" && fm == null && !personNeighbor) return@forEach
            val kw1 = c.first.card.keywords.take(2).joinToString("、")
            val kw2 = c.second.card.keywords.take(2).joinToString("、")
            val cleanFirst = c.first.card.meaning.trimEnd('。', '！', '？')
            val meaning = fm ?: if (c.relation == "牌序相邻") {
                if (spreadKey == "three")
                    "${c.first.position}${c.first.card.name}的“${kw1}”与${c.second.position}${c.second.card.name}的“${kw2}”前后相接，先按${cleanFirst}，再看${c.second.card.meaning}"
                else
                    "${c.first.position}${c.first.card.name}（${kw1}）与${c.second.position}${c.second.card.name}（${kw2}）是牌序相邻的两组线索，可按各自牌位并读"
            } else {
                "${c.first.position}${c.first.card.name}与${c.second.position}${c.second.card.name}为${c.relation}，互参“${kw1}”与“${kw2}”两组线索"
            }
            out.add(Combination(
                c.first.card.name, c.second.card.name, c.first.position, c.second.position,
                c.relation, c.rowDistance, c.columnDistance, meaning,
                if (fm != null) "固定组合" else "相邻牌义合读"
            ))
        }
        return out
    }

    /** 布局证据（九宫十字 + 大桌近身牌/归宫） */
    private fun buildLayoutEvidence(spreadKey: String, cards: List<Drawn>): List<String> {
        val out = mutableListOf<String>()
        if (spreadKey == "nine" && cards.size == 9) {
            out.add("中心牌${cards[4].card.name}是九宫主轴；上排为背景与思考，中排为当下，下排为落地走向")
            out.add("横向：${cards.subList(3, 6).joinToString("→") { it.card.name }}；纵向：${listOf(cards[1], cards[4], cards[7]).joinToString("→") { it.card.name }}")
            out.add("对角线：${listOf(cards[0], cards[4], cards[8]).joinToString("→") { it.card.name }}；${listOf(cards[2], cards[4], cards[6]).joinToString("→") { it.card.name }}")
        }
        if (spreadKey == "grandTableau") {
            listOf("男士", "女士").forEach { name ->
                val idx = cards.indexOfFirst { it.card.name == name }
                if (idx < 0) return@forEach
                val c = cards[idx]
                val near = cards.filter { o ->
                    val rd = kotlin.math.abs((o.row ?: 0) - (c.row ?: 0))
                    val cd = kotlin.math.abs((o.column ?: 0) - (c.column ?: 0))
                    rd <= 1 && cd <= 1 && rd + cd > 0
                }
                out.add("${name}落第${idx + 1}宫（${c.house}宫，第${c.row}排第${c.column}列）；近身牌${near.joinToString("、") { it.card.name }}")
            }
            val homes = cards.filter { it.house == it.card.name }
            if (homes.isNotEmpty()) out.add("归宫牌：${homes.joinToString("、") { "${it.card.name}回到本宫" }}")
        }
        return out
    }

    /** 由逐张抽取的 [0,1) 样本序列还原取牌（对应 mingyu resolveInteractiveLenormandCards + secureRandomIndexSample）。
     * 每个样本从当前剩余牌中按 floor(sample * remaining.size) 取一张并移除，保证不重复。 */
    fun revealSamples(spreadKey: String, samples: List<Double>): List<Card> {
        val spread = spreadOf(spreadKey)
        if (samples.size > spread.positions.size)
            throw IllegalArgumentException("${spread.name}最多抽取${spread.positions.size}张牌")
        val remaining = DECK.toMutableList()
        return samples.map { s ->
            if (!s.isFinite() || s < 0.0 || s >= 1.0)
                throw IllegalArgumentException("抽牌随机样本无效")
            val idx = (s * remaining.size).toInt().coerceIn(0, remaining.size - 1)
            remaining.removeAt(idx)
        }
    }

    /** 生成一张逐张抽取的样本（对应 mingyu secureRandomIndexSample，无偏索引转 [0,1)）。 */
    fun nextSample(remainingCount: Int, random: Random = Random): Double {
        val n = remainingCount.coerceAtLeast(1)
        return (random.nextInt(n) + 0.5) / n
    }

    /** 逐张点抽完成后，按样本还原完整牌阵结果。 */
    fun drawFromSamples(spreadKey: String, samples: List<Double>): Result {
        val spread = spreadOf(spreadKey)
        if (samples.size != spread.positions.size)
            throw IllegalArgumentException("${spread.name}需要逐张抽取${spread.positions.size}张牌")
        return assemble(spread, revealSamples(spreadKey, samples))
    }

    /** 组装牌阵结果（随机 / 手录 / 逐张点抽共用） */
    private fun assemble(spread: Spread, picked: List<Card>): Result {
        val key = spread.key
        val columns = if (key == "grandTableau") 9 else if (key == "nine") 3 else 0
        val cards = picked.mapIndexed { i, card ->
            Drawn(
                card = card,
                position = spread.positions[i],
                house = if (key == "grandTableau") DECK[i].name else null,
                row = if (columns > 0) i / columns + 1 else null,
                column = if (columns > 0) i % columns + 1 else null
            )
        }
        return Result(
            key, spread.name, cards,
            buildCombinations(key, cards),
            buildLayoutEvidence(key, cards)
        )
    }

    /** 抽牌主入口：random=随机洗牌，manualIds=按牌号手录 */
    fun draw(spreadKey: String, random: Random = Random, manualIds: List<Int>? = null): Result {
        val spread = spreadOf(spreadKey)
        val picked = if (manualIds != null) cardsFromIds(manualIds)
            ?: throw IllegalArgumentException("手工录入牌号无效或重复")
        else shuffleDraw(spread.positions.size, random)
        if (picked.size != spread.positions.size)
            throw IllegalArgumentException("${spread.name}需要${spread.positions.size}张牌")
        return assemble(spread, picked)
    }

    /** 九宫十字距离分析（3x3，中心为第 5 张） */
    fun analyzeNineGrid(cards: List<Card>): CrossAnalysis? {
        if (cards.size != 9) return null
        val grid = listOf(
            cards[0] to (0 to 0), cards[1] to (0 to 1), cards[2] to (0 to 2),
            cards[3] to (1 to 0), cards[4] to (1 to 1), cards[5] to (1 to 2),
            cards[6] to (2 to 0), cards[7] to (2 to 1), cards[8] to (2 to 2)
        )
        val distances = grid.filter { it.second != (1 to 1) }.map { (card, rc) ->
            val d = kotlin.math.abs(rc.first - 1) + kotlin.math.abs(rc.second - 1)
            GridDistance(card, d, if (d == 1) "紧邻" else if (d == 2) "近距" else "远距")
        }.sortedBy { it.distance }
        val center = cards[4]
        val cross = "核心「${center.name}」：上「${cards[1].name}」下「${cards[7].name}」左「${cards[3].name}」右「${cards[5].name}」"
        return CrossAnalysis(
            center, cards[1], cards[7], cards[3], cards[5],
            mapOf("左上" to cards[0], "右上" to cards[2], "左下" to cards[6], "右下" to cards[8]),
            distances,
            "【九宫十字】$cross；紧邻牌：${distances.filter { it.relationship == "紧邻" }.joinToString("、") { it.card.name }}"
        )
    }

    /** 牌面技术底稿（复制给 AI 断） */
    fun render(result: Result, question: String? = null): String = buildString {
        if (!question.isNullOrBlank()) append("【所问】$question\n")
        append("【牌阵】${result.spreadName}\n\n")
        result.cards.forEach { d ->
            append("── ${d.position} ──\n")
            append("  #${d.card.id} ${d.card.name} [${d.card.keywords.joinToString("、")}]\n")
            append("  ${d.card.meaning}\n")
            if (d.house != null) append("  落${d.house}宫")
            if (d.row != null && d.column != null) append(" · 第${d.row}排第${d.column}列")
            if (d.house != null || (d.row != null && d.column != null)) append("\n")
        }
        if (result.layoutEvidence.isNotEmpty()) {
            append("\n── 布局证据 ──\n")
            result.layoutEvidence.forEach { append("  $it\n") }
        }
        if (result.spreadKey == "nine") {
            analyzeNineGrid(result.cards.map { it.card })?.let { append("\n${it.summary}\n") }
        }
        if (result.combinations.isNotEmpty()) {
            append("\n── 两牌组合（${result.combinations.size}）──\n")
            result.combinations.forEach { c ->
                append("  ${c.card1}+${c.card2}（${c.relation}｜${c.source}）：${c.meaning}\n")
            }
        }
        append("\n※ 36 张通行雷诺曼牌序，牌义为传统关键词底稿；请结合牌位、相邻与九宫距离综合解读。")
    }

    /** 把结果编码为 extras 字符串，供 UI 还原牌面（分隔符 ; | # 规避牌名内的标点） */
    fun encode(result: Result): String = buildString {
        append(result.spreadKey).append('#').append(result.spreadName).append('|')
        append(result.cards.joinToString("|") { d ->
            listOf(d.card.id.toString(), d.card.name, d.position,
                d.house ?: "", (d.row ?: 0).toString(), (d.column ?: 0).toString(),
                d.card.keywords.joinToString("、"), d.card.meaning).joinToString(";")
        }).append('|')
        append(result.combinations.joinToString("|") { c ->
            listOf(c.card1, c.card2, c.position1, c.position2, c.relation,
                c.rowDistance.toString(), c.columnDistance.toString(), c.meaning, c.source)
                .joinToString(";")
        })
    }

    data class Decoded(val spreadKey: String, val spreadName: String,
                       val cards: List<Drawn>, val combinations: List<Combination>)

    fun decode(raw: String?): Decoded? {
        if (raw.isNullOrBlank()) return null
        return try {
            val seg = raw.split('|', limit = 3)
            if (seg.size < 2) return null
            val head = seg[0].split('#', limit = 2)
            val cards = if (seg[1].isBlank()) emptyList() else seg[1].split('|').mapNotNull { s ->
                val p = s.split(';')
                if (p.size < 8) return@mapNotNull null
                Drawn(
                    Card(p[0].toInt(), p[1], p[6].split("、").filter { it.isNotEmpty() }, p[7]),
                    p[2], p[3].ifEmpty { null },
                    p[4].toIntOrNull()?.takeIf { it > 0 }, p[5].toIntOrNull()?.takeIf { it > 0 }
                )
            }
            val combos = if (seg.size < 3 || seg[2].isBlank()) emptyList() else seg[2].split('|').mapNotNull { s ->
                val p = s.split(';')
                if (p.size < 9) return@mapNotNull null
                Combination(p[0], p[1], p[2], p[3], p[4],
                    p[5].toIntOrNull() ?: 0, p[6].toIntOrNull() ?: 0, p[7], p[8])
            }
            Decoded(head[0], head.getOrNull(1) ?: "", cards, combos)
        } catch (e: Exception) { null }
    }
}
