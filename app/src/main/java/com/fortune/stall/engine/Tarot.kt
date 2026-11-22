package com.fortune.stall.engine

import kotlin.random.Random

/**
 * 塔罗抽牌引擎 —— 完整移植 divination-skills/tarot/scripts/draw.py。
 * Rider-Waite-Smith 78 张牌体系，含正逆位、元素分布、相邻元素互动、大阿卡纳提示。
 */
object Tarot {
    data class Card(
        val name: String,       // 英文名
        val cn: String,         // 中文名
        val number: String,     // 编号/点数
        val type: String,       // Major / Minor
        val suit: String? = null,
        val suitCn: String? = null,
        val element: String? = null,
        val elementCn: String? = null,
        val domain: String? = null,
        val rank: String? = null,
        val rankCn: String? = null
    ) {
        val roman: String get() = if (type == "Major") number else ""
    }

    data class Drawn(val card: Card, val reversed: Boolean, val position: String)

    private val MAJOR = arrayOf(
        "The Fool|0|愚者", "The Magician|I|魔术师", "The High Priestess|II|女祭司", "The Empress|III|女皇",
        "The Emperor|IV|皇帝", "The Hierophant|V|教皇", "The Lovers|VI|恋人", "The Chariot|VII|战车",
        "Strength|VIII|力量", "The Hermit|IX|隐士", "Wheel of Fortune|X|命运之轮", "Justice|XI|正义",
        "The Hanged Man|XII|倒吊人", "Death|XIII|死神", "Temperance|XIV|节制", "The Devil|XV|恶魔",
        "The Tower|XVI|塔", "The Star|XVII|星星", "The Moon|XVIII|月亮", "The Sun|XIX|太阳",
        "Judgement|XX|审判", "The World|XXI|世界"
    )

    data class SuitDef(val en: String, val cn: String, val elem: String, val elemCn: String, val domain: String)
    private val SUITS = listOf(
        SuitDef("Wands", "权杖", "Fire", "火", "行动/激情/创造力/意志"),
        SuitDef("Cups", "圣杯", "Water", "水", "情感/关系/直觉/内心"),
        SuitDef("Swords", "宝剑", "Air", "风", "思维/冲突/决断/沟通"),
        SuitDef("Pentacles", "星币", "Earth", "土", "物质/财务/工作/身体")
    )
    private val SUIT_SYM = mapOf("Swords" to "🜁", "Cups" to "🜄", "Wands" to "🜂", "Pentacles" to "🜃")

    private val RANKS = arrayOf(
        "Ace|A|王牌", "Two|2|二", "Three|3|三", "Four|4|四", "Five|5|五", "Six|6|六", "Seven|7|七",
        "Eight|8|八", "Nine|9|九", "Ten|10|十", "Page|Pg|侍从", "Knight|Kn|骑士", "Queen|Qn|王后", "King|Kg|国王"
    )

    val DECK: List<Card> = buildDeck()

    private fun buildDeck(): List<Card> {
        val d = mutableListOf<Card>()
        MAJOR.forEach { s ->
            val p = s.split("|")
            d.add(Card(p[0], p[2], p[1], "Major"))
        }
        SUITS.forEach { suit ->
            RANKS.forEach { r ->
                val p = r.split("|")
                d.add(Card(
                    name = "${p[0]} of ${suit.en}", cn = "${suit.cn}${p[2]}", number = p[1], type = "Minor",
                    suit = suit.en, suitCn = suit.cn, element = suit.elem, elementCn = suit.elemCn,
                    domain = suit.domain, rank = p[0], rankCn = p[2]
                ))
            }
        }
        return d
    }

    /** 牌阵定义（位置名） */
    val SPREADS = mapOf(
        "one" to Spread("单牌占", listOf("当下的核心信息")),
        "three" to Spread("三牌阵(时间线)", listOf("过去(根源/背景)", "现在(当下状态)", "未来(走向/建议)")),
        "celtic" to Spread("凯尔特十字(Celtic Cross)", listOf(
            "①现状(核心问题)", "②阻碍(挑战/交叉牌)", "③远因(深层根源)", "④近因(近期影响)",
            "⑤意识(目标/可能结果)", "⑥近未来(即将发生)", "⑦自我(内心态度/立场)", "⑧环境(外部影响/他人)",
            "⑨希望与恐惧", "⑩最终结果"
        ))
    )
    data class Spread(val name: String, val positions: List<String>)

    private val ELEMENT_REL = mapOf(
        "Fire|Fire" to "同元素增强", "Water|Water" to "同元素增强", "Air|Air" to "同元素增强", "Earth|Earth" to "同元素增强",
        "Fire|Air" to "友好(火风相助)", "Air|Fire" to "友好(风火相助)", "Water|Earth" to "友好(水土相助)", "Earth|Water" to "友好(土水相助)",
        "Fire|Water" to "对立(火水相克)", "Water|Fire" to "对立(水火相克)", "Air|Earth" to "对立(风土相克)", "Earth|Air" to "对立(土风相克)",
        "Fire|Earth" to "中性", "Earth|Fire" to "中性", "Air|Water" to "中性", "Water|Air" to "中性"
    )

    /** 抽牌（随机：不重复 + 正逆位） */
    fun drawCards(n: Int, random: Random): List<Pair<Card, Boolean>> {
        val chosen = DECK.shuffled(random).take(n.coerceAtMost(DECK.size))
        return chosen.map { it to random.nextBoolean() }
    }

    /** 中文别名兜底查找 */
    fun findCard(query: String): Card? {
        val q = query.trim()
        val lower = q.lowercase()
        DECK.firstOrNull { it.name.lowercase() == lower }?.let { return it }
        DECK.firstOrNull { it.cn == q }?.let { return it }
        val alias = mapOf("高塔" to "塔", "女教皇" to "女祭司", "教宗" to "教皇", "吊人" to "倒吊人",
            "命运轮" to "命运之轮", "星辰" to "星星", "皇后" to "女皇", "金币" to "星币",
            "钱币" to "星币", "五角星" to "星币", "手杖" to "权杖", "宝杖" to "权杖")
        alias[q]?.let { a -> DECK.firstOrNull { it.cn == a }?.let { return it } }
        // 中文模糊（先长后短）
        DECK.filter { q in it.cn || it.cn in q }.maxByOrNull { it.cn.length }?.let { return it }
        DECK.firstOrNull { lower in it.name.lowercase() || it.name.lowercase() in lower }?.let { return it }
        return null
    }

    /** 牌面渲染（技术底稿），供复制给 AI 断 */
    fun renderDraw(cards: List<Drawn>, spreadName: String, question: String? = null): String {
        val L = mutableListOf<String>()
        if (!question.isNullOrBlank()) L.add("【问题】$question")
        L.add("【牌阵】$spreadName")
        L.add("")
        val elements = mutableListOf<String>()
        cards.forEach { d ->
            val tag = if (d.card.type == "Major") "★大阿卡纳" else "小阿卡纳·${d.card.suitCn ?: ""}"
            L.add("── ${d.position} ──")
            L.add("  ${d.card.cn}(${d.card.name}) [${if (d.reversed) "逆位" else "正位"}]  $tag")
            if (d.card.element != null) {
                L.add("  元素: ${d.card.elementCn}(${d.card.element}) | 领域: ${d.card.domain ?: ""}")
                elements.add(d.card.element!!)
            }
            if (d.card.type == "Major") L.add("  编号: ${d.card.number} | 大阿卡纳代表重大主题/人生转折")
            L.add("")
        }
        if (elements.isNotEmpty()) {
            val ec = elements.groupingBy { it }.eachCount()
            val dist = SUITS.mapNotNull { s ->
                ec[s.elem]?.let { "${s.elemCn}(${s.cn})×$it" }
            }.joinToString(", ")
            if (dist.isNotEmpty()) L.add("── 元素分布: $dist ──")
        }
        if (cards.size >= 2) {
            L.add(""); L.add("── 相邻牌元素互动 ──")
            for (i in 0 until cards.size - 1) {
                val c1 = cards[i].card; val c2 = cards[i + 1].card
                val e1 = c1.element; val e2 = c2.element
                if (e1 != null && e2 != null) {
                    L.add("  ${c1.cn} ($e1) → ${c2.cn} ($e2): ${ELEMENT_REL["$e1|$e2"] ?: "—"}")
                } else if (c1.type == "Major" || c2.type == "Major") {
                    L.add("  ${c1.cn} → ${c2.cn}: 含大阿卡纳(超越元素, 看主题共振)")
                }
            }
        }
        val majorCount = cards.count { it.card.type == "Major" }
        if (majorCount >= 3) L.add("\n※ 大阿卡纳出现${majorCount}张: 强烈的命运/转折信号, 非日常琐事")
        else if (majorCount == 0 && cards.size >= 3) L.add("\n※ 全为小阿卡纳: 聚焦日常实际层面, 当事人有较大主动权")

        val suitCount = cards.mapNotNull { it.card.suit }.groupingBy { it }.eachCount()
        suitCount.filter { it.value >= 2 }.forEach { (s, cnt) ->
            val def = SUITS.first { it.en == s }
            L.add("※ ${def.cn}(${def.domain})出现${cnt}张, 此领域是焦点")
        }
        return L.joinToString("\n")
    }

    /** 便捷：按牌阵 id 抽牌并渲染 */
    fun castBySpread(mode: String, question: String, random: Random, customN: Int = 3): Pair<List<Drawn>, String> {
        val (spreadName, positions) = when (mode) {
            "one" -> SPREADS["one"]!!.let { it.name to it.positions }
            "three", "time" -> SPREADS["three"]!!.let { it.name to it.positions }
            "celtic", "cross" -> SPREADS["celtic"]!!.let { it.name to it.positions }
            "custom" -> "自由抽牌(${customN}张)" to (1..customN).map { "第${it}张" }
            else -> SPREADS["three"]!!.let { it.name to it.positions }
        }
        val drawnPairs = drawCards(positions.size, random)
        val drawn = drawnPairs.mapIndexed { i, p -> Drawn(p.first, p.second, positions[i]) }
        return drawn to renderDraw(drawn, spreadName, question)
    }

    /** 自报牌名解读 */
    fun interpret(names: List<String>, question: String?): String {
        val drawn = names.mapIndexed { i, raw ->
            val rev = raw.startsWith("reversed:")
            val q = if (rev) raw.removePrefix("reversed:") else raw
            val card = findCard(q)
            Drawn(card ?: Card(q, q, "", "Major"), rev, "第${i + 1}张")
        }.filter { it.card.name.isNotEmpty() }
        return renderDraw(drawn, "指定解牌(${drawn.size}张)", question)
    }
}
