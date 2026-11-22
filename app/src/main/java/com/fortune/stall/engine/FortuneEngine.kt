package com.fortune.stall.engine

import com.fortune.stall.util.LunarCalendar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 起卦统一门面 —— 对应服务端 fortune.py 的 cast()/roll()，落实「卦面即印章」：
 * 所有随机（骰子/铜钱/抽牌）在此发生并当场生成完整卦面文本，UI 只负责呈现与动画。
 */
object FortuneEngine {
    enum class Method(val key: String) { XIAOLIUREN("xiaoliuren"), LIUYAO("liuyao"), TAROT("tarot") }

    data class CastOutput(
        val face: String,          // 卦面全文（技术底稿）
        val seal: String,          // 印章落款
        val method: Method,
        val mode: String,
        val extras: Map<String, String> = emptyMap()  // 骰子点数、铜钱背面数等动画数据
    )

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** 印章落款：公历 + 农历 + 干支 + 生肖 */
    fun sealLine(now: LocalDateTime): String {
        val lun = LunarCalendar.fromDateTime(now)
        return listOf(
            now.format(fmt),
            "${lun.month}月${lun.day}日" + if (lun.isLeap) "(闰)" else "",
            "${lun.yearGanZhi}年[${lun.zodiac}] ${lun.monthGanZhi}月 ${lun.dayGanZhi}日"
        ).joinToString(" · ")
    }

    /** 组装最终卦面：与 fortune.py cast() 的返回格式一致 */
    private fun wrap(sid: Int, seal: String, question: String, face: String, warn: String = ""): String =
        buildString {
            append("🎐 卦局 #$sid 已落印 · $seal\n")
            append("问: $question\n")
            append("─".repeat(24)).append("\n")
            append(face).append(warn).append("\n")
            append("─".repeat(24)).append("\n")
            append("卦面为服务端印章, 只断不改。")
        }

    /**
     * 确定性/随机起卦统一入口。
     * @param sessionId 卦局编号（由卦账写入时分配）
     */
    fun cast(
        sessionId: Int,
        method: Method,
        mode: String,
        question: String,
        now: LocalDateTime = LocalDateTime.now(),
        a: Int? = null, b: Int? = null, c: Int? = null,
        hourZhi: String? = null,
        gender: String? = null,
       guaNames: List<String>? = null,
        cardNames: List<String>? = null,
        random: Random = Random
    ): CastOutput {
        val seal = sealLine(now)
        val hz = hourZhi ?: LunarCalendar.hourToZhi(now.hour)
        var face: String
        val extras = mutableMapOf<String, String>()

        when (method) {
            Method.XIAOLIUREN -> {
                face = when (mode) {
                    "lunar" -> XiaoLiuRen.castByLunar(a ?: 1, b ?: 1, hz, gender)
                    "numbers" -> XiaoLiuRen.castByNumbers(a ?: 1, b ?: 1, c ?: 1, hz, gender)
                    "gua" -> XiaoLiuRen.castByGua(guaNames ?: listOf("大安", "速喜", "小吉"), hz, gender)
                    "dice" -> {
                        val pips = listOf(random.nextInt(1, 7), random.nextInt(1, 7), random.nextInt(1, 7))
                        extras["pips"] = pips.joinToString(",")
                        XiaoLiuRen.castByNumbers(pips[0], pips[1], pips[2], hz, gender)
                    }
                    else -> XiaoLiuRen.castByTime(now, gender).second
                }
            }
            Method.LIUYAO -> {
                face = when (mode) {
                    "numbers" -> LiuYao.castByNumbers(a ?: 1, b ?: 1, c ?: 1, now)
                    "gua" -> {
                        val (xia, shang) = LiuYao.nameToGua(guaNames?.firstOrNull() ?: "天火同人")
                        if (xia != null && shang != null) LiuYao.zhuangGua(xia, shang, emptyList(), now)
                        else "找不到卦: ${guaNames?.firstOrNull()}"
                    }
                    "coin" -> {
                        val (text, tosses) = LiuYao.castByCoin(now, random)
                        extras["tosses"] = tosses.joinToString(",")
                        text
                    }
                    else -> LiuYao.castByTime(now)
                }
            }
            Method.TAROT -> {
                face = when (mode) {
                    "interpret" -> Tarot.interpret(cardNames ?: emptyList(), question)
                    else -> {
                        val n = if (mode == "custom") (a ?: 3) else 0
                        val (drawn, text) = Tarot.castBySpread(mode, question, random, n)
                        // 供 UI 解析渲染牌面：cn;name;number;suitCn;reversed;position;isMajor;suit;rank
                        extras["cards"] = drawn.joinToString("|") {
                            "${it.card.cn};${it.card.name};${it.card.number};${it.card.suitCn ?: ""};" +
                                    "${if (it.reversed) 1 else 0};${it.position};${if (it.card.type == "Major") 1 else 0};" +
                                    "${it.card.suit ?: ""};${it.card.rank ?: ""}"
                        }
                        text
                    }
                }
            }
        }
        // 小六壬：从卦面解析三宫，编码 jishiyu 风格宫格数据供 UI 渲染
        if (method == Method.XIAOLIUREN) {
            val gua = parseGuaFromFace(face)
            if (gua != null && gua.size == 3) {
                val cells = XiaoLiuRen.gongCells(gua, hz)
                extras["xlrGongs"] = cells.joinToString("|") { c ->
                    listOf(c.gong, c.position, c.shen, c.xing, c.zhi, c.zhiWx,
                        c.liuqin, c.wuxing, c.jixiong, c.mark).joinToString(";")
                }
            }
        }
        return CastOutput(wrap(sessionId, seal, question, face), seal, method, mode, extras)
    }

    /** 从卦面文本解析三宫名：【卦象】 天宫 地宫 人宫 … */
    private fun parseGuaFromFace(face: String): List<String>? {
        val line = face.lines().firstOrNull { it.contains("【卦象】") } ?: return null
        val names = line.substringAfter("【卦象】").trim().split(Regex("\\s+"))
            .filter { it in listOf("大安", "留连", "速喜", "赤口", "小吉", "空亡") }
        return names.take(3).ifEmpty { null }
    }
}
