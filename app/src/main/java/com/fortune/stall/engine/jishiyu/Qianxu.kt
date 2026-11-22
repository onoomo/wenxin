package com.fortune.stall.engine.jishiyu

import kotlin.random.Random

/**
 * 潜虚筮法引擎 —— 由 yu2102897706.github.io/qianxu (APK 潜虚_1) 反编译还原。
 * 算法、55 名数据表、七位爻辞、吉凶显幽翻转规则均与原版一致。
 *
 * 核心：取七十策，虚其五（不用）；分而为二 → 取一挂一 → 以十(定名)/七(求变)为一揲数之 → 归余。
 * 定名得左右余数 a、b，数合 key = a|b，查 55 名；求变得 1~7 位，查该位爻辞。
 */
object Qianxu {

    // ===== 原版常量集 =====
    private val SHENG_PURE = listOf("11", "22", "33", "44", "55")   // 生数纯 · 从阳
    private val CHENG_PURE = listOf("66", "77", "88", "99", "1010") // 成数纯 · 从阴
    val NO_VAR = listOf("11", "69", "55")                            // 元/餘/齊 无变不占
    val POS_NAME = mapOf(1 to "初", 2 to "二", 3 to "三", 4 to "四", 5 to "五", 6 to "六", 7 to "上")
    private val POS_DESC = mapOf(1 to "事之始", 7 to "事之终")

    val WX_COLOR = mapOf("水" to "#2F4F6B", "火" to "#A83A2A", "木" to "#4A6B3A",
        "金" to "#7C7667", "土" to "#8F6E33")

    /** 解析数合 key → [左,右]。'12'→[1,2]；'101'→[10,1]；'1010'→[10,10] */
    fun pairOf(key: String): Pair<Int, Int> = when (key.length) {
        2 -> (key[0] - '0') to (key[1] - '0')
        3 -> if (key.startsWith("10")) 10 to (key[2] - '0') else (key[0] - '0') to 10
        else -> 10 to 10
    }

    /** 由左右余数查名：允许 a、b 顺序互换 */
    fun findMing(a: Int, b: Int): QianxuData.Ming = QianxuData.DATA.first {
        val (x, y) = pairOf(it.key)
        (x == a && y == b) || (x == b && y == a)
    }

    /** 阴阳/纯卦判定 */
    data class Mode(val yin: Boolean, val pure: String, val yang: Boolean)
    fun modeOf(m: QianxuData.Ming, pairMatched: Boolean): Mode = when {
        m.key in SHENG_PURE -> Mode(yin = false, pure = "生纯", yang = true)
        m.key in CHENG_PURE -> Mode(yin = true, pure = "成纯", yang = false)
        pairMatched -> Mode(yin = false, pure = "", yang = true)
        else -> Mode(yin = true, pure = "", yang = false)
    }

    /** 吉凶显幽翻转：吉↔凶、臧↔否、平不变；"吉凶不现"直读 */
    fun resolveJi(ji: String, rev: Boolean): String {
        if (ji == "吉凶不现" || !rev) return ji
        return when (ji) {
            "吉" -> "凶"; "凶" -> "吉"; "臧" -> "否"; "否" -> "臧"; else -> ji
        }
    }
    fun revJi(ji: String): String = when (ji) {
        "吉" -> "凶"; "凶" -> "吉"; "臧" -> "否"; "否" -> "臧"; else -> ji
    }

    // ===== 一次起卦的完整状态 =====
    data class State(
        val ming: QianxuData.Ming,
        val a: Int, val b: Int,
        val l1: Int, val r1: Int, val l2: Int, val r2: Int,
        val yang: Boolean,
        val mode: Mode,
        val question: String = "",
        val pos: Int = 0,          // 求变位 1~7，0=未求变
        val ji: String = "",       // 判读后的吉凶
        val jiRaw: String = ""     // 原始吉凶
    ) {
        val noVar: Boolean get() = ming.key in NO_VAR
    }

    /** 用随机数模拟"分而为二"的左右策数（原版 rand(0x45)=rand(69) → 1..69，左右合 70 策） */
    private fun split(rnd: Random): Int = 1 + rnd.nextInt(69) // 1..69

    /** 定名：两次分合，各取左余 a=(L-1)%10、右余 b=(R+1)%10，0 作 10 */
    fun castMing(rnd: Random = Random.Default): State {
        val l1 = split(rnd); val r1 = 70 - l1
        var a = (l1 - 1) % 10; if (a == 0) a = 10
        val l2 = split(rnd); val r2 = 70 - l2
        var b = (r2 + 1) % 10; if (b == 0) b = 10
        val ming = findMing(a, b)
        val (x, y) = pairOf(ming.key)
        val matched = (a == x && b == y)
        return State(ming, a, b, l1, r1, l2, r2, matched, modeOf(ming, matched))
    }

    /** 求变：以七为揲，阳(生纯/阳名)从左取 pos=(L-1)%7，阴从右 pos=(R+1)%7 的补——原版 castBian(yang) */
    fun castBian(state: State, rnd: Random = Random.Default): State {
        if (state.noVar) return state
        val l = split(rnd); val r = 70 - l
        var pos = if (state.mode.yang) (l - 1) % 7 else (r - 1) % 7
        if (pos == 0) pos = 7
        // 保留本次求变的分策数，供自动演示舞台按真实左右堆显示
        return resolveBian(state.copy(l1 = l, r1 = r), pos)
    }

    /**
     * 手动演卦：由用户点击的策数直接驱动。
     * @param a 定名左余 1..10；@param b 定名右余 1..10
     */
    fun castMingManual(a0: Int, b0: Int, l1: Int, l2: Int): State {
        var a = a0; if (a == 0) a = 10
        var b = b0; if (b == 0) b = 10
        val ming = findMing(a, b)
        val (x, y) = pairOf(ming.key)
        val matched = (a == x && b == y)
        return State(ming, a, b, l1, 70 - l1, l2, 70 - l2, matched, modeOf(ming, matched))
    }

    /** 求变判读：由已得 pos 计算吉凶（rev = 该名属阴取幽） */
    fun resolveBian(s: State, pos: Int): State {
        val name = POS_NAME[pos] ?: "初"
        val info = s.ming.pos[name]
        val raw = info?.ji ?: "吉凶不现"
        val rev = s.mode.yin
        val ji = resolveJi(raw, rev)
        return s.copy(pos = pos, ji = ji, jiRaw = raw)
    }

    /** 七位爻辞是否齐备（初、上有，且非无变名） */
    fun hasSevenPos(m: QianxuData.Ming) = m.pos.containsKey("初")

    /** 结果展示文本（供存档/复制） */
    fun faceText(s: State, time: String): String {
        val sb = StringBuilder()
        sb.append("【潜虚筮法】${time}\n")
        if (s.question.isNotBlank()) sb.append("所问：${s.question}\n")
        val (x, y) = pairOf(s.ming.key)
        sb.append("定名：${s.ming.name}（${s.ming.key}）左${x}右${y} · 五行${s.ming.wx}\n")
        val modeText = when {
            s.noVar -> "${s.ming.name}·无变·不占"
            s.ming.key in SHENG_PURE -> "生数纯·从阳取显"
            s.ming.key in CHENG_PURE -> "成数纯·从阴取幽"
            s.mode.yang -> "阳名·先主后客·用显"
            else -> "阴名·先客后主·用幽"
        }
        sb.append(modeText).append("\n")
        if (s.ming.total.isNotBlank()) sb.append("总辞：${s.ming.total.replace("||", " · ")}\n")
        sb.append("名辞：${s.ming.def}\n")
        sb.append("白话：${s.ming.yi}\n")
        if (!s.noVar && s.pos in 1..7 && s.pos !in listOf(1, 7)) {
            val name = POS_NAME[s.pos]
            val info = s.ming.pos[name]
            sb.append("求变：${name}位 · ${s.ji}\n")
            if (info != null) {
                sb.append("爻辞：${info.ci}\n大意：${info.dayi}\n解曰：${info.xiang}\n")
                if (s.mode.yin) sb.append("幽读：${s.jiRaw} → ${s.ji}\n")
            }
        }
        return sb.toString().trim()
    }
}
