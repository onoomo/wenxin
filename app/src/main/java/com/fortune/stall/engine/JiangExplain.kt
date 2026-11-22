package com.fortune.stall.engine

/**
 * 江氏小六壬 · 宫位含义解释库
 *
 * 完全照搬反编译 APK「江氏小六壬 1.0」(uni-app pages/result/result.vue) 的宫位弹窗数据：
 * - palaceInfo：六宫口诀(meaning) + 详解(detail)
 * - palaceDetailInfo：六宫详细类象(palace)
 * - 六神/六亲/五星含义三张表
 *
 * APK 弹窗标题「XX 宫位详解」，正文拼装顺序：
 * 口诀 → 六宫含义 → 六神含义 → 六亲含义 → 五星含义（无地支项）。
 */
object JiangExplain {
    data class Term(
        val category: String,      // 六宫 / 六神 / 六亲 / 五星
        val name: String,
        val desc: String,          // 主述
        val fields: List<Pair<String, String>> = emptyList()
    )

    /** 六宫口诀 meaning（APK palaceInfo） */
    private val PALACE_MEANING = mapOf(
        "大安" to "大安事事昌，求财在坤方，失物去不远，宅舍保安康",
        "留连" to "留恋事难成，求谋日未明，官事只宜缓，去者未回程",
        "速喜" to "速喜喜来临，求财向南行，失物申未午，逢人路上寻",
        "赤口" to "赤口主口舌，官非切要防，失物急去寻，行人有惊慌",
        "小吉" to "小吉最吉昌，路上好商量，阴人来报信，失物在坤方",
        "空亡" to "空亡事不祥，阴人多乖张，求财无利益，行人有灾殃"
    )

    /** 六宫详解 detail（APK palaceInfo） */
    private val PALACE_DETAIL = mapOf(
        "大安" to "大安为吉神，主平安、安定、稳定。事业顺利，感情和睦，身体健康。",
        "留连" to "留恋主迟滞、拖延、不顺。事情进展缓慢，需要耐心等待。",
        "速喜" to "速喜主快速、喜悦、成功。事情进展迅速，会有好消息传来。",
        "赤口" to "赤口主口舌是非、争执、不和。需要谨言慎行，避免冲突。",
        "小吉" to "小吉主小的吉利、顺利。虽不如大安稳定，但也是吉神。",
        "空亡" to "空亡主空虚、失落、不实。事情容易落空，需要谨慎行事。"
    )

    /** 六宫详细类象 palace（APK palaceDetailInfo） */
    private val PALACE_CLASS = mapOf(
        "大安" to "配震卦，五行木，为青/绿色，为东方，为长男，数为147。主安静、不动、稳定、吉祥、顺利。",
        "留连" to "配坎卦，五行水，为黑/蓝色，为北方，为中男，数为258。主暧昧不明，隐情、牵连、拖延、反复、不确定。",
        "速喜" to "配离卦，五行火，为红/紫色，为南方，为中女，数为369。主快速、喜庆、主动、时机已到、久不宜。",
        "赤口" to "配乾兑卦，五行金，为白/银色，为西方/西北方，为少女/老男，数为147。主不顺、坎坷、牵绊、惊吓、凶险、伤害、口舌、纠纷。",
        "小吉" to "配巽卦，五行木，为青/绿色，为东南方，为长女，数为258。为六合，主和睦、和谐、吉利、顺当、桃花。",
        "空亡" to "配坤艮卦，五行土，为黄色，为西南方/东北方，为老女/少男，数为369。主不吉、不实、忧虑、空虚、没有、消散、无结果。"
    )

    /** 六神含义（APK 六神表） */
    private val LIUSHEN_MEANING = mapOf(
        "青龙" to "五行木，为东方。主喜庆、吉利、正义、喜事、好事、贵人、主新，能逢凶化吉；主酒色财气、桃花、感情。",
        "朱雀" to "五行火，为南方。主表达、口舌官非，人事纠纷；主通讯、信息、文书、文化。",
        "勾陈" to "五行土，为中间。主牵连、勾连、多人参与、阻力、慢；主旧、老的、过去的；主阴宅、为坟墓。",
        "白虎" to "五行金，为西方。主凶猛、斗争、伤灾、破坏、官司、横祸、惊吓；主刀、主血；主偏财、横财，代表实力、强势。",
        "玄武" to "五行水、为北方。主骗子，小人，盗贼、欺诈、贪婪、爱财；主虚假，不真实、暧昧不明、隐私之事。代表轻浮，华而不实、无信誉，明一套暗一套；主情欲、贪色、男女私情。",
        "腾蛇" to "五行土，为中间。主虚空少实、不真实、猜忌多疑、虚情假意、有心机、变来变去、为小人；代表细长之物、弯曲之物；代表套住、捆住、牵连。"
    )

    /** 六亲含义（APK 六亲表，含「身」） */
    private val LIUQIN_MEANING = mapOf(
        "父母" to "代表长辈、贵人、文化、知识、学习、学业、信息、文书、证件、工作、单位、交通工具、房屋、服装等。",
        "兄弟" to "代表兄妹、同事、朋友、同学、战友、同行、同类、同源、合作、竞争等。",
        "子孙" to "代表孩子、晚辈、学生、客户、宠物、娱乐、享受、僧尼道、医药、医生、医院、警察、军人等。",
        "妻财" to "代表妻子、金钱、工资、珠宝、财物、资产、佣人、员工等。",
        "官鬼" to "代表丈夫、领导、法官、工作、升职、考试、技术、是非、官司、疾病、灾祸、罪犯、地痞、流氓等。",
        "身" to "代表自己、本人、主体等。"
    )

    /** 五星含义（APK 五星表） */
    private val WUXING_MEANING = mapOf(
        "木星" to "吉星，凡事勿急，好事自成。和顺吉祥，家宅安宁。",
        "火星" to "凶星，女人多灾难，男人不利行。安分守己，防止口舌是非。",
        "土星" to "凶星，谋事受阻，凡事拖延，防止小人，出行不利。",
        "金星" to "凶星，性情刚烈，求谋难成，宜生灾祸，凡事谨慎。",
        "水星" to "吉星，男人多福，女人安宁，办事遂心，添人进财。",
        "天空" to "凶星，主空虚、失落、不实。事情容易落空，需要谨慎行事。"
    )

    /**
     * 按 APK 逻辑拼装某一宫的完整解释，返回供 UI 卡片渲染的术语列表。
     * 顺序：六宫（口诀+详解+类象）→ 六神 → 六亲 → 五星。
     */
    fun termsFor(palaceName: String, liuShen: String, liuQin: String, wuXing: String): List<Term> {
        val out = mutableListOf<Term>()
        val fields = mutableListOf<Pair<String, String>>()
        PALACE_DETAIL[palaceName]?.let { fields.add("详解" to it) }
        PALACE_CLASS[palaceName]?.let { fields.add("六宫类象" to it) }
        out.add(Term("六宫", palaceName, PALACE_MEANING[palaceName] ?: "", fields))
        if (liuShen.isNotBlank()) {
            out.add(Term("六神", liuShen, LIUSHEN_MEANING[liuShen] ?: ""))
        }
        if (liuQin.isNotBlank()) {
            out.add(Term("六亲", liuQin, LIUQIN_MEANING[liuQin] ?: ""))
        }
        if (wuXing.isNotBlank()) {
            out.add(Term("五星", wuXing, WUXING_MEANING[wuXing] ?: ""))
        }
        return out
    }

    /** 兼容旧调用：按名称取单项解释 */
    fun of(name: String): Term? {
        PALACE_MEANING[name]?.let {
            val fields = mutableListOf<Pair<String, String>>()
            PALACE_DETAIL[name]?.let { d -> fields.add("详解" to d) }
            PALACE_CLASS[name]?.let { c -> fields.add("六宫类象" to c) }
            return Term("六宫", name, it, fields)
        }
        LIUSHEN_MEANING[name]?.let { return Term("六神", name, it) }
        LIUQIN_MEANING[name]?.let { return Term("六亲", name, it) }
        WUXING_MEANING[name]?.let { return Term("五星", name, it) }
        return null
    }
}
