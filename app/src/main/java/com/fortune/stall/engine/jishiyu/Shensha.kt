package com.fortune.stall.engine.jishiyu

/**
 * 神煞查询 —— 1:1 移植自吉时雨 shensha.js (AGPL-3.0, xianbo.chen@gmail.com)
 * 60+ 神煞判定由 ShenshaTable 真值表驱动（JS 原函数穷举生成），
 * query 主逻辑逐条对应原版 queryShenSha。
 */
object Shensha {
    private fun b(v: Boolean) = if (v) "1" else "0"

    /** 德秀贵人（第二参为四柱天干数组，特殊处理） */
    private fun dexiuguiren(yuezhi: String, gans: List<String>): Boolean {
        fun has(targets: List<String>) = gans.any { it in targets }
        fun combo(a: String, b: String) = gans.contains(a) && gans.contains(b)
        return when (yuezhi) {
            in listOf("寅", "午", "戌") -> has(listOf("丙", "丁")) && combo("戊", "癸")
            in listOf("申", "子", "辰") -> has(listOf("壬", "癸", "戊", "己")) &&
                    (combo("丙", "辛") || combo("甲", "己"))
            in listOf("巳", "酉", "丑") -> has(listOf("庚", "辛")) && combo("乙", "庚")
            in listOf("亥", "卯", "未") -> has(listOf("甲", "乙")) && combo("丁", "壬")
            else -> false
        }
    }

    private fun tianganYinyang(g: String) = g in listOf("甲", "丙", "戊", "庚", "壬")
    private fun dizhiWx(z: String) = JishiyuBase.ZHI_WUXING[z] ?: ""

    /**
     * 查询某柱神煞。
     * @param ganzhi 该柱干支(两字)
     * @param bazi 八字数组 8 元素：[年干,年支,月干,月支,日干,日支,时干,时支]
     * @param isman 性别
     * @param witch 第几柱 1/2/3/4=年/月/日/时，其他=大运流年等
     * @param niannayin 年柱纳音(查学堂词馆童子用)
     */
    fun query(ganzhi: String, bazi: List<String>, isman: Boolean, witch: Int, niannayin: String): List<String> {
        val list = mutableListOf<String>()
        val niangan = bazi[0]; val nianzhi = bazi[1]
        val yuegan = bazi[2]; val yuezhi = bazi[3]
        val rigan = bazi[4]; val rizhi = bazi[5]
        val shigan = bazi[6]; val shizhi = bazi[7]
        val gan = ganzhi[0].toString(); val zhi = ganzhi[1].toString()
        fun H(fn: String, vararg a: String): Boolean = ShenshaTable.hit(fn, *a)

        if (H("tianyiguiren", rigan, zhi) || H("tianyiguiren", niangan, zhi)) list.add("天乙")
        if (H("taijiguiren", rigan, zhi) || H("taijiguiren", niangan, zhi)) list.add("太极")
        if (H("tiandeguiren", yuezhi, gan) || H("tiandeguiren", yuezhi, zhi)) list.add("天德")
        if (H("yuede", yuezhi, gan)) list.add("月德")
        if (dexiuguiren(yuezhi, listOf(niangan, yuegan, rigan, shigan))) list.add("德秀")
        if (H("tiandehe", yuezhi, gan) || H("tiandehe", yuezhi, zhi)) list.add("天德合")
        if (H("yuedehe", yuezhi, gan)) list.add("月德合")
        if (H("fuxing", niangan, zhi) || H("fuxing", rigan, zhi)) list.add("福星")
        if (H("wenchang", rigan, zhi) || H("wenchang", niangan, zhi)) list.add("文昌")
        if (witch != 3 && H("xuetang", niannayin, gan, zhi)) list.add("学堂")
        if (witch != 3 && H("ciguan", niannayin, gan, zhi)) list.add("词馆")
        if (witch == 3 && H("kuigang", rigan, rizhi)) list.add("魁罡")
        if (H("guoying", rigan, zhi) || H("guoying", niangan, zhi)) list.add("国印")
        if ((witch != 3 && H("yima", rizhi, zhi)) || (witch != 1 && H("yima", nianzhi, zhi))) list.add("驿马")
        if ((witch != 3 && H("huagai", rizhi, zhi)) || (witch != 1 && H("huagai", nianzhi, zhi))) list.add("华盖")
        if ((witch != 3 && H("jiangxing", rizhi, zhi)) || (witch != 1 && H("jiangxing", nianzhi, zhi))) list.add("将星")
        if (H("jingyu", rigan, zhi) || H("jingyu", niangan, zhi)) list.add("金舆")
        if ((witch == 3 && H("jinshen", rigan, rizhi)) || (witch == 4 && H("jinshen", shigan, shizhi))) list.add("金神")
        if (witch != 2 && H("wugui", yuezhi, zhi)) list.add("五鬼")
        if (witch != 2 && H("tianyi", yuezhi, zhi)) list.add("天医")
        if (H("lushen", rigan, zhi)) list.add("禄神")
        if (H("tianshe", yuezhi, rigan, rizhi)) list.add("天赦")
        if (witch != 1 && H("hongluan", nianzhi, zhi)) list.add("红鸾")
        if (witch != 1 && H("tianxi", nianzhi, zhi)) list.add("天喜")
        if (H("liuxia", rigan, zhi)) list.add("流霞")
        if (H("hongyan", rigan, zhi)) list.add("红艳")
        if ((witch != 3 && H("tianluo", rizhi, zhi)) || (witch != 1 && H("tianluo", nianzhi, zhi))) list.add("天罗")
        if ((witch != 3 && H("diwang", rizhi, zhi)) || (witch != 1 && H("diwang", nianzhi, zhi))) list.add("地网")
        if (H("yangren", rigan, zhi)) list.add("羊刃")
        if (H("feiren", rigan, zhi)) list.add("飞刃")
        if (H("xueren", yuezhi, zhi)) list.add("血刃")
        if (witch == 3 && H("bazhuan", rigan, rizhi)) list.add("八专")
        if (witch == 3 && H("jiuchou", rigan, rizhi)) list.add("九丑")
        if (H("jiesha", rizhi, zhi) || H("jiesha", nianzhi, zhi)) list.add("劫煞")
        if (H("zaisha", nianzhi, zhi)) list.add("灾煞")
        if (witch != 1 && H("yuancheng", nianzhi, zhi, b(isman), b(tianganYinyang(niangan)))) list.add("元辰")
        if ((witch != 3 && H("kongwang", rigan + rizhi, zhi)) || (witch != 1 && H("kongwang", niangan + nianzhi, zhi))) list.add("空亡")
        if ((witch == 3 && H("tongzi", yuezhi, niannayin, rizhi)) || (witch == 4 && H("tongzi", yuezhi, niannayin, shizhi))) list.add("童子")
        if (H("tianchu", niangan, rigan, zhi)) list.add("天厨")
        if (witch != 1 && H("gucheng", nianzhi, zhi)) list.add("孤辰")
        if (witch != 1 && H("guashu", nianzhi, zhi)) list.add("寡宿")
        if ((witch != 3 && H("wangshen", rizhi, zhi)) || (witch != 1 && H("wangshen", nianzhi, zhi))) list.add("亡神")
        if (witch == 3 && H("shiedabai", rigan, rizhi)) list.add("十恶大败")
        if (H("taohua", rizhi, zhi) || H("taohua", nianzhi, zhi)) list.add("桃花")
        if (witch == 3 && H("guluan", rigan, rizhi)) list.add("孤鸾")
        if (witch == 3 && H("yingyangchacuo", rigan, rizhi)) list.add("阴差阳错")
        if (witch == 3 && H("sifei", yuezhi, rigan, rizhi)) list.add("四废")
        if (witch != 1 && H("shangmen", nianzhi, zhi)) list.add("丧门")
        if (witch != 1 && H("diaoke", nianzhi, zhi)) list.add("吊客")
        if (witch != 1 && H("pima", nianzhi, zhi)) list.add("披麻")
        if (witch == 3 && H("shiling", rigan, rizhi)) list.add("十灵")
        return list
    }

    /** 四柱神煞全表：返回 map 柱名->神煞列表 */
    fun castFourPillars(pillars: List<String>, isman: Boolean): List<Pair<String, String>> {
        // pillars: [年干支,月干支,日干支,时干支]
        val bazi = mutableListOf<String>()
        pillars.forEach { bazi.add(it[0].toString()); bazi.add(it[1].toString()) }
        val niannayin = JishiyuBase.queryNayin(pillars[0])
        val names = listOf("年柱", "月柱", "日柱", "时柱")
        return pillars.mapIndexed { i, gz ->
            val ss = query(gz, bazi, isman, i + 1, niannayin)
            names[i] to if (ss.isEmpty()) "无" else ss.joinToString("、")
        }
    }
}
