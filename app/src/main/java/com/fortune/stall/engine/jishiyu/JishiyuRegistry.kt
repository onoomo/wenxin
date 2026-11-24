package com.fortune.stall.engine.jishiyu

import com.fortune.stall.util.LunarCalendar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 吉时雨术数模块注册表。每个模块 = 元数据 + 输入字段 + 排盘函数。
 * 通用 UI 依据此表渲染表单与结果。后续批次在此追加即可。
 */
object JishiyuRegistry {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** 全部模块（按移植批次与难度排序） */
    val modules: List<ModuleDef> = listOf(
        chenggu(), shuZi(), shiShen(), yiZhangJing(), meiHuaTime(), meiHuaNum(), liuYao(), nineStar(), shensha(), daLiuRen(), qimen(), shanXiangQimen(), ziwei(),
        wanNianLi(), laoHuangLi()
    )

    private fun parseDt(s: String): LocalDateTime =
        try { LocalDateTime.parse(s.trim(), fmt) } catch (e: Exception) { LocalDateTime.now() }

    // ===== 称骨算命 =====
    private fun chenggu() = ModuleDef(
        key = "chenggu", name = "称骨算命", eyebrow = "袁 天 罡",
        desc = "按农历年月日时称骨定重，对照歌诀断一生福禄。",
        fields = listOf(
            InputField("dt", "出生时间", FieldType.DATETIME, LocalDateTime.now().format(fmt)),
            InputField("isman", "性别", FieldType.GENDER, "男")
        )
    ) { m ->
        val isMan = m["isman"] != "女"
        val r = Chenggu.cast(parseDt(m["dt"] ?: ""), isMan)
        CastResult(
            title = "称 骨 算 命", subtitle = "${r.yearGanZhi}年 · 农历${r.lunarMD} ${r.hourZhi}时",
            sections = listOf(
                CastSection("骨重", listOf("命重" to r.weight, "性别" to if (isMan) "男" else "女")),
                CastSection("歌诀", paragraphs = listOf(r.verse), highlighted = true),
                CastSection("详解", paragraphs = r.detail.split("<br/>").map { it.trim() }.filter { it.isNotEmpty() })
            )
        )
    }

    // ===== 数字吉凶 =====
    private fun shuZi() = ModuleDef(
        key = "shuzi", name = "数字吉凶", eyebrow = "磁 场 数 理",
        desc = "手机号八星磁场分析：延年天医生气为吉，六煞五鬼祸害绝命为凶。",
        fields = listOf(
            InputField("phone", "手机号码", FieldType.TEXT, "", "输入 11 位手机号"),
            InputField("isman", "性别", FieldType.GENDER, "男")
        )
    ) { m ->
        val phone = (m["phone"] ?: "").filter { it.isDigit() }
        if (phone.length < 4) {
            CastResult("数 字 吉 凶", subtitle = "请输入至少 4 位数字", sections = emptyList())
        } else {
            val a = ShuZiJiXiong.analyze(phone, m["isman"] != "女")
            val secs = mutableListOf<CastSection>()
            secs.add(CastSection("磁场分组", listOf("分组" to a.groups.joinToString(" "))))
            if (a.resultsShen.isNotEmpty()) {
                secs.add(CastSection("命中磁场", a.resultsShen.mapIndexed { i, s ->
                    s to (a.resultsType.getOrNull(i) ?: "")
                }))
            }
            // 每个吉凶神的象义
            val detail = mutableListOf<String>()
            a.resultsShen.forEach { shen ->
                val arr = ShuZiJiXiong.JI_SHEN[shen] ?: ShuZiJiXiong.XIONG_SHEN[shen]
                if (arr != null) {
                    detail.add("【$shen】")
                    detail.addAll(arr)
                }
            }
            if (detail.isNotEmpty()) secs.add(CastSection("象义详解", paragraphs = detail))
            if (a.tailZero.isNotEmpty()) secs.add(CastSection("尾数提示", paragraphs = a.tailZero))
            CastResult("数 字 吉 凶", subtitle = phone, sections = secs)
        }
    }

    // ===== 十神对照 =====
    private fun shiShen() = ModuleDef(
        key = "shishen", name = "十神详解", eyebrow = "十 神 辞 典",
        desc = "查十神含义与五行生克关系：比劫、食伤、财星、官杀、印星。",
        fields = listOf(
            InputField("name", "十神", FieldType.TEXT, "比肩", "输入十神名，如 正官、七杀")
        )
    ) { m ->
        val name = (m["name"] ?: "").trim()
        val info = Shishen.info(name)
        CastResult(
            title = "十 神 辞 典", subtitle = name,
            sections = listOf(
                CastSection(name, paragraphs = if (info.isNotEmpty()) info.split("<br/>").map { it.trim() }.filter { it.isNotEmpty() } else listOf("未收录该十神，请输入：${Shishen.SHISHEN.joinToString("、")}"))
            )
        )
    }

    // ===== 达摩一掌经 =====
    private fun yiZhangJing() = ModuleDef(
        key = "yizhangjing", name = "达摩一掌经", eyebrow = "六 道 轮 回",
        desc = "以年月日时地支落宫，查六道、神煞，断一生因果与性情。",
        fields = listOf(
            InputField("dt", "出生时间", FieldType.DATETIME, LocalDateTime.now().format(fmt)),
            InputField("isman", "性别", FieldType.GENDER, "男")
        )
    ) { m ->
        val r = YiZhangJing.cast(parseDt(m["dt"] ?: ""), m["isman"] != "女")
        CastResult(
            title = "达 摩 一 掌 经", subtitle = "${r.yearGan}年 · ${if (r.isSun) "顺行" else "逆行"}",
            sections = listOf(
                CastSection("四宫", rows = listOf(
                    "年宫" to "${r.nian.zhi} · ${r.nian.shensha} · ${r.nian.liudao}",
                    "月宫" to "${r.yue.zhi} · ${r.yue.shensha} · ${r.yue.liudao}",
                    "日宫" to "${r.ri.zhi} · ${r.ri.shensha} · ${r.ri.liudao}",
                    "时宫" to "${r.shi.zhi} · ${r.shi.shensha} · ${r.shi.liudao}"
                )),
                CastSection("宫位详解", paragraphs = listOf(
                    "年：${r.nian.gongInfo}", "月：${r.yue.gongInfo}",
                    "日：${r.ri.gongInfo}", "时：${r.shi.gongInfo}"
                ).filter { it.length > 3 }),
                CastSection("神煞", paragraphs = listOf(
                    "年逢${r.nian.shensha}：${r.nian.shenshaInfo}",
                    "月逢${r.yue.shensha}：${r.yue.shenshaInfo}",
                    "日逢${r.ri.shensha}：${r.ri.shenshaInfo}",
                    "时逢${r.shi.shensha}：${r.shi.shenshaInfo}"
                ))
            )
        )
    }

    // ===== 梅花易数·时间起卦 =====
    private fun meiHuaTime() = ModuleDef(
        key = "meihua-time", name = "梅花·时间卦", eyebrow = "梅 花 易 数",
        desc = "年月日时起卦：年支数加农历月日取上卦，加时支取下卦与动爻。",
        fields = listOf(InputField("dt", "起卦时间", FieldType.DATETIME, LocalDateTime.now().format(fmt)))
    ) { m ->
        val g = MeiHua.byTime(parseDt(m["dt"] ?: ""))
        meiHuaResult(g)
    }

    // ===== 梅花易数·数字起卦 =====
    private fun meiHuaNum() = ModuleDef(
        key = "meihua-num", name = "梅花·数字卦", eyebrow = "梅 花 易 数",
        desc = "报数以奇偶分两段取上下卦，相加取动爻，可加时辰数。",
        fields = listOf(
            InputField("digits", "报数", FieldType.TEXT, "123456", "输入任意位数字"),
            InputField("dt", "起卦时间(定动爻时辰)", FieldType.DATETIME, LocalDateTime.now().format(fmt))
        )
    ) { m ->
        val g = MeiHua.byNumber(m["digits"] ?: "", true, parseDt(m["dt"] ?: ""))
        meiHuaResult(g)
    }

    // ===== 八字旺衰分析 =====
    private fun baziAnalysis() = ModuleDef(
        key = "bazi-analysis", name = "八字分析", eyebrow = "旺 衰 用 神",
        desc = "四柱旺衰评分、格局、从格、调候用神与性格分析。",
        fields = listOf(InputField("dt", "出生时间", FieldType.DATETIME, LocalDateTime.now().format(fmt)))
    ) { m ->
        val lun = LunarCalendar.fromDateTime(parseDt(m["dt"] ?: ""))
        val arr = listOf(lun.yearGanZhi, lun.monthGanZhi, lun.dayGanZhi, lun.hourGanZhi)
        val a = BaziAnalyzer.analyze(arr)
        CastResult("八 字 分 析", subtitle = arr.joinToString("  "),
            sections = listOf(
                CastSection("旺衰格局", rows = listOf(
                    "旺衰" to a.wangshuai, "评分" to "${a.score}", "格局" to (a.geju ?: "无"),
                    "从格" to (a.congge ?: "无"), "用神" to a.yongshen.joinToString("、"),
                    "调候" to a.tiaohou.joinToString("、")
                ), highlighted = true),
                CastSection("性格", rows = listOf(
                    "核心" to a.personality, "主导五行" to a.dominantWuxing,
                    "主导十神" to a.dominantShishen, "行为" to a.behavior, "类型" to a.style
                )),
                CastSection("提示", paragraphs = if (a.risks.isEmpty()) listOf("五行较为均衡，无明显偏颇。") else a.risks),
                CastSection("评分明细", paragraphs = a.detail)
            ))
    }

    // ===== 六爻 =====
    private fun liuYao() = ModuleDef(
        key = "liuyao", name = "六爻排盘", eyebrow = "纳 甲 装 卦",
        desc = "铜钱摇卦纳甲装卦：六亲、六神、世应、动变、伏神、神煞。输入六爻(初→上)，1阳0阴，o老阳x老阴。",
        fields = listOf(
            InputField("yao", "六爻", FieldType.TEXT, "111000", "如 111000；老阳1o 老阴0x"),
            InputField("dt", "起卦时间", FieldType.DATETIME, LocalDateTime.now().format(fmt))
        )
    ) { m ->
        val raw = (m["yao"] ?: "").replace(" ", "")
        // 解析: 支持 1/0 单字符, 或 1o/0x 双字符
        val yaoList = mutableListOf<String>()
        var i = 0
        while (i < raw.length) {
            if (raw[i]=='1' || raw[i]=='0') {
                if (i+1 < raw.length && (raw[i+1]=='o' || raw[i+1]=='x')) { yaoList.add(""+raw[i]+raw[i+1]); i+=2 }
                else { yaoList.add(raw[i].toString()); i++ }
            } else i++
        }
        if (yaoList.size != 6) {
            CastResult("六 爻 排 盘", subtitle = "请输入 6 个爻(初爻到上爻)", sections = emptyList())
        } else {
            val r = LiuYaoJishiyu.cast(parseDt(m["dt"] ?: ""), yaoList)
            val yaoRows = mutableListOf<Pair<String,String>>()
            val labels = listOf("上爻","五爻","四爻","三爻","二爻","初爻")
            for (k in 5 downTo 0) {
                val y = r.yaoInfo[k]
                val mark = buildString { if(y.isShi) append("世"); if(y.isYing) append("应") }
                val sym = if (y.yao==1) "———" else "- - -"
                val dong = if (y.type.contains("o")||y.type.contains("x")) " ○动" else ""
                val fu = if (y.fushen.isNotEmpty()) " 伏:${y.fushen}" else ""
                yaoRows.add("${labels[k]} ${r.liuShen[k]}" to "$sym ${y.liuQin}${y.gan}${y.zhi} $mark$dong$fu")
            }
            val secs = mutableListOf<CastSection>()
            secs.add(CastSection("卦象", rows = listOf(
                "本卦" to r.baseName + if(r.alias.isNotEmpty()) "(${r.alias})" else "",
                "变卦" to r.bianName, "卦宫" to r.baseGong, "四柱" to r.siZhu.joinToString(" ")
            ), highlighted = true))
            secs.add(CastSection("六爻", rows = yaoRows))
            val ssRows = r.shenSha.map { it.name to it.zhi }
            if (ssRows.isNotEmpty()) secs.add(CastSection("神煞", rows = ssRows))
            CastResult("六 爻 排 盘", subtitle = "${r.baseName} 之 ${r.bianName}" + if(r.hasBian) " · 有动爻" else "", sections = secs)
        }
    }

    // ===== 合婚 =====
    private fun castOf(s: String): List<String> {
        val lun = LunarCalendar.fromDateTime(parseDt(s))
        return listOf(lun.yearGanZhi, lun.monthGanZhi, lun.dayGanZhi, lun.hourGanZhi)
    }

    private fun hehun() = ModuleDef(
        key = "hehun", name = "八字合婚", eyebrow = "天 作 之 合",
        desc = "按双方八字比对十二项：生肖、纳音、婚宫、时柱、年柱、用神、十神、调候、空亡、性格、桃花、神煞。",
        fields = listOf(
            InputField("dt1", "甲方出生时间", FieldType.DATETIME, LocalDateTime.now().format(fmt)),
            InputField("isman1", "甲方性别", FieldType.GENDER, "男"),
            InputField("dt2", "乙方出生时间", FieldType.DATETIME, LocalDateTime.now().format(fmt)),
            InputField("isman2", "乙方性别", FieldType.GENDER, "女")
        )
    ) { m ->
        val r = Hehun.hehun(
            castOf(m["dt1"] ?: ""), m["isman1"] != "女",
            castOf(m["dt2"] ?: ""), m["isman2"] != "女"
        )
        val secs = mutableListOf<CastSection>()
        secs.add(CastSection("合婚结论", rows = listOf(
            "通过" to "${r.passCount}/${r.totalRules} 条",
            "等级" to r.grade.grade
        ), paragraphs = listOf(r.grade.desc), highlighted = true))
        for (rule in r.rules) {
            val lines = mutableListOf(rule.desc, rule.detail, rule.passDesc)
            secs.add(CastSection(
                "${if (rule.pass) "✓" else "✗"} ${rule.name}",
                paragraphs = lines
            ))
        }
        CastResult("八 字 合 婚", subtitle = "${r.grade.grade} · 通过${r.passCount}/${r.totalRules}条", sections = secs)
    }

    // ===== 紫微斗数 =====
    private fun ziwei() = ModuleDef(
        key = "ziwei", name = "紫微斗数", eyebrow = "十 八 飞 星",
        desc = "命宫身宫、五行局、十二宫天干、十四主星安星与亮度、四化。时辰索引0早子1丑2寅...11亥12晚子，留空按当前时辰。",
        fields = listOf(
            InputField("dt", "出生时间(公历)", FieldType.DATETIME, LocalDateTime.now().format(fmt)),
            InputField("isman", "性别", FieldType.GENDER, "男"),
            InputField("ti", "时辰索引(0-12,留空自动)", FieldType.NUMBER, ""),
            InputField("lon", "出生地经度(真太阳时,留空用钟表时)", FieldType.TEXT, "", "如 116.4 北京 / 121.5 上海")
        )
    ) { m ->
        val ti = (m["ti"] ?: "").trim().toIntOrNull()?.takeIf { it in 0..12 } ?: -1
        val lon = (m["lon"] ?: "").trim().toDoubleOrNull()
        val r = Ziwei.paipan(parseDt(m["dt"] ?: ""), m["isman"] != "女", ti, lon)
        val secs = mutableListOf<CastSection>()
        secs.add(CastSection("命盘", rows = listOf(
            "公历" to r.solarDate, "农历" to r.lunarDate, "干支" to r.chineseDate,
            "命宫" to r.ebSoul, "身宫" to r.ebBody, "命主" to r.soul, "身主" to r.body,
            "五行局" to r.juName, "年干" to r.yearGan
        ) + (if (r.solarInfo.isNotBlank()) listOf("时间校正" to r.solarInfo) else emptyList()), highlighted = true))
        secs.add(CastSection("十二宫", rows = r.rows))
        CastResult("紫 微 斗 数", subtitle = "命宫${r.ebSoul} · ${r.juName}", sections = secs)
    }

    // ===== 奇门遁甲 =====
    private fun qimen() = ModuleDef(
        key = "qimen", name = "奇门遁甲", eyebrow = "阴 盘 奇 门",
        desc = "时盘排局：地盘三奇六仪、旬首值符、八神九星八门、隐干马星空亡。局数0自动。",
        fields = listOf(
            InputField("dt", "起局时间", FieldType.DATETIME, LocalDateTime.now().format(fmt)),
            InputField("isman", "性别", FieldType.GENDER, "男"),
            InputField("jushu", "局数(0自动,正阳遁负阴遁)", FieldType.NUMBER, "0")
        )
    ) { m ->
        val r = Qimen.paipan(
            parseDt(m["dt"] ?: ""), m["isman"] != "女",
            (m["jushu"] ?: "0").trim().toIntOrNull() ?: 0
        )
        val gongRows = QimenData._8GUA_S.map { g ->
            val c = r.pan[g]!!
            g to "天盘:${c.tianpan} 九星:${c.jiuxing} 八门:${c.men} 八神:${c.bashen} 地盘:${c.dipan} 隐干:${c.yingan}"
        }
        CastResult("奇 门 遁 甲", subtitle = "${if (r.yangDun) "阳遁" else "阴遁"}${r.jushu}局 · ${r.xunhead}",
            sections = listOf(
                CastSection("局式", rows = listOf(
                    "时间" to r.date, "四柱" to r.siZhu.joinToString(" "),
                    "节气" to r.jieqi, "局式" to "${if (r.yangDun) "阳遁" else "阴遁"}${r.jushu}局",
                    "月将" to "${r.yueJiang}(${r.yueJiangName})", "旬首" to r.xunhead, "空亡" to r.kongwang,
                    "值符" to r.zhifu + "(" + r.zhifuGong + ")", "值使" to r.zhishi + "(" + r.zhishiGong + ")",
                    "马星" to r.maxing
                ), highlighted = true),
                CastSection("九宫(天盘/九星/八门/八神/地盘)", rows = gongRows),
                CastSection("洛书九宫图", rows = r.rows)
            ))
    }

    // ===== 大六壬 =====
    private fun daLiuRen() = ModuleDef(
        key = "daliuren", name = "大六壬", eyebrow = "三 式 之 首",
        desc = "四课三传、天盘月将、贵人六亲、十二宫行年。占卜时辰留空则用当前时辰，用神留空为常规排盘。",
        fields = listOf(
            InputField("dt", "起课时间", FieldType.DATETIME, LocalDateTime.now().format(fmt)),
            InputField("isman", "性别", FieldType.GENDER, "男"),
            InputField("yearMing", "出生年份(行年用)", FieldType.NUMBER, "1990"),
            InputField("zhanTime", "占卜时辰(支,留空取当前)", FieldType.TEXT, "", "如 子、卯、午"),
            InputField("yongShen", "用神干支(阴盘,留空不排)", FieldType.TEXT, "", "如 甲子")
        )
    ) { m ->
        val r = DaLiuRen.paipan(
            parseDt(m["dt"] ?: ""), m["isman"] != "女",
            (m["yearMing"] ?: "1990").trim().toIntOrNull() ?: 1990,
            zhanbuTime = (m["zhanTime"] ?: "").trim(),
            yongShen = (m["yongShen"] ?: "").trim()
        )
        val secs = mutableListOf<CastSection>()
        secs.add(CastSection("课式", rows = listOf(
            "时间" to r.date, "四柱" to r.siZhu.joinToString(" "),
            "占时" to r.zhanbuTime, "月将" to r.yueJiang,
            "昼夜" to if (r.isDaytime) "昼" else "夜", "空亡" to r.kongwang,
            "年命" to r.yearGanzhi, "行年" to r.xingYear
        ), highlighted = true))
        // 天盘/贵人/天干
        val panRows = _12DIZHI_ORDER.map { d -> d to "天盘:${r.tianPan[d] ?: ""} 贵:${r.guiShen[d] ?: ""} 干:${r.tianGan[d] ?: ""}" }
        secs.add(CastSection("三盘(按地盘)", rows = panRows))
        // 四课
        secs.add(CastSection("四课", rows = r.fourKe.mapIndexed { i, k ->
            "第${i + 1}课" to k.joinToString(" ")
        }))
        // 三传
        secs.add(CastSection("三传", rows = r.threeChuan.mapIndexed { i, c ->
            (arrayOf("初传", "中传", "末传").getOrNull(i) ?: "传${i + 1}") to c.joinToString(" ")
        }))
        secs.add(CastSection("十二宫", rows = r.gong12.map { it.key + "宫" to it.value }))
        secs.add(CastSection("十二年煞", rows = r.nianSha.map { it.key to it.value }))
        r.sym3chuan?.let { m2 ->
            secs.add(CastSection("阴盘三传", rows = m2.map { it.key to it.value.joinToString(" ") }))
        }
        CastResult("大 六 壬", subtitle = "月将${r.yueJiang} · 占时${r.zhanbuTime}", sections = secs)
    }

    private val _12DIZHI_ORDER = arrayOf("寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥", "子", "丑")

    // ===== 神煞 =====
    private fun shensha() = ModuleDef(
        key = "shensha", name = "八字神煞", eyebrow = "吉 凶 神 煞",
        desc = "按四柱查六十余种神煞：天乙、文昌、驿马、桃花、羊刃、空亡等，逐柱列出。",
        fields = listOf(
            InputField("dt", "出生时间", FieldType.DATETIME, LocalDateTime.now().format(fmt)),
            InputField("isman", "性别", FieldType.GENDER, "男")
        )
    ) { m ->
        val lun = LunarCalendar.fromDateTime(parseDt(m["dt"] ?: ""))
        val pillars = listOf(lun.yearGanZhi, lun.monthGanZhi, lun.dayGanZhi, lun.hourGanZhi)
        val rows = Shensha.castFourPillars(pillars, m["isman"] != "女")
        CastResult("八 字 神 煞", subtitle = pillars.joinToString("  "),
            sections = listOf(CastSection("四柱神煞", rows = rows, highlighted = true)))
    }

    // ===== 山向奇门（专用页分流，此处仅提供注册元数据） =====
    private fun shanXiangQimen() = ModuleDef(
        key = "shanxiangqimen", name = "山向奇门", eyebrow = "二 十 四 山",
        desc = "按罗盘度数查 72 局，以年柱代日、五鼠遁定时，布阳阴遁九宫盘。",
        fields = listOf(
            InputField("degree", "度数(0-360)", FieldType.NUMBER, "180")
        )
    ) { m ->
        val degree = (m["degree"] ?: "180").toIntOrNull() ?: 180
        val def = ShanxiangQimen.findByDegree(degree)
        val dt = LocalDateTime.now()
        val r = Qimen.paipan(dt, true, shanDegree = degree)
        CastResult("山 向 奇 门", subtitle = "${def?.shan}山${def?.xiang}向 · ${if (r.yangDun) "阳遁" else "阴遁"}${r.jushu}局",
            sections = listOf(CastSection("山向", rows = listOf(
                "山向" to "${def?.shan}山${def?.xiang}向", "度数" to "${def?.degree}～${def?.degree?.plus(4)}",
                "黄泉" to (def?.huangquan ?: ""), "局" to "${if (r.yangDun) "阳遁" else "阴遁"}${r.jushu}局"
            ), highlighted = true)))
    }

    // ===== 九星飞泊 =====
    private fun nineStar() = ModuleDef(
        key = "ninestar", name = "九星飞泊", eyebrow = "玄 空 飞 星",
        desc = "年月日时四飞星入九宫：年星、月星、日星、时星飞泊洛书，配九星吉凶含义。",
        fields = listOf(
            InputField("dt", "起局时间", FieldType.DATETIME, LocalDateTime.now().format(fmt)),
            InputField("lon", "经度(真太阳时,留空用钟表时)", FieldType.TEXT, "", "如 116.4 北京 / 121.5 上海")
        )
    ) { m ->
        val lon = (m["lon"] ?: "").trim().toDoubleOrNull()
        NineStar.cast(parseDt(m["dt"] ?: ""), lon)
    }

    // ===== 万年历 =====
    private fun wanNianLi() = ModuleDef(
        key = "wannianli", name = "万年历", eyebrow = "万 年 历",
        desc = "公历农历对照：整月日历逐日列出农历、干支、节气与生肖，点选日期查老黄历。",
        fields = listOf(InputField("dt", "查询月份(任选一天)", FieldType.DATE, java.time.LocalDate.now().toString()))
    ) { m ->
        val date = try { java.time.LocalDate.parse((m["dt"] ?: "").trim()) } catch (e: Exception) { java.time.LocalDate.now() }
        val ym = java.time.YearMonth.from(date)
        val rows = mutableListOf<List<String>>()
        rows.add(listOf("公历", "农历", "干支", "节气/节日"))
        for (d in 1..ym.lengthOfMonth()) {
            val ld = ym.atDay(d)
            val lun = LunarCalendar.fromDateTime(ld.atTime(12, 0))
            var jq = ""
            val range = com.fortune.stall.util.SolarTerm.rangeAt(ld.atTime(12, 0))
            if (range != null && range.from.time.toLocalDate() == ld) jq = range.from.name
            rows.add(listOf(
                "${ld.monthValue}月${d}日",
                (if (lun.isLeap) "闰" else "") + AlmanacData.MONTH[lun.month] + "月" + AlmanacData.DAY[lun.day],
                lun.dayGanZhi,
                jq.ifEmpty { lun.zodiac }
            ))
        }
        CastResult("万 年 历", subtitle = "${ym.year}年${ym.monthValue}月 · 共${ym.lengthOfMonth()}天", sections = listOf(
            CastSection("月历", rows = listOf(
                "公历月份" to "${ym.year}年${ym.monthValue}月",
                "农历年" to LunarCalendar.fromDateTime(date.atTime(12,0)).yearGanZhi + "年",
                "生肖" to LunarCalendar.fromDateTime(date.atTime(12,0)).zodiac
            ), highlighted = true),
            CastSection("逐日历表", table = rows)
        ))
    }

    // ===== 老黄历 =====
    private fun laoHuangLi() = ModuleDef(
        key = "laohuangli", name = "老黄历", eyebrow = "老 黄 历",
        desc = "择日经典：宜忌、冲煞、值神建星、二十八宿、纳音、吉神凶煞、彭祖百忌与财喜福神方位。",
        fields = listOf(InputField("dt", "查询时刻", FieldType.DATETIME, LocalDateTime.now().format(fmt)))
    ) { m ->
        val dt = parseDt(m["dt"] ?: "")
        val a = LunarAlmanac.paipan(dt)
        val secs = mutableListOf<CastSection>()
        secs.add(CastSection("历法", rows = listOf(
            "公历" to a.solarText, "农历" to a.lunarText, "四柱" to a.baziText,
            "节气" to a.jieQi.ifEmpty { "—" }, "生肖" to LunarCalendar.fromDateTime(dt).zodiac
        ), highlighted = true))
        secs.add(CastSection("宜忌", rows = listOf(
            "宜" to a.yi.joinToString(" "), "忌" to a.ji.joinToString(" ")
        )))
        secs.add(CastSection("神煞格局", rows = listOf(
            "冲煞" to a.chongSha, "值神" to (a.zhiShen + a.zhiShenType), "建星" to a.zhiXing,
            "二十八宿" to (a.xiu + a.animal + " · " + a.xiuLuck), "纳音" to a.naYin,
            "太岁" to a.taiShen, "吉神" to a.jiShen.joinToString(" "), "凶煞" to a.xiongSha.joinToString(" ")
        )))
        secs.add(CastSection("方位", rows = listOf(
            "财神" to a.caiShen, "喜神" to a.xiShen, "福神" to a.fuShen, "阳贵" to a.yangGui
        )))
        secs.add(CastSection("彭祖百忌", paragraphs = a.pengZu.split("\n").filter { it.isNotBlank() }))
        CastResult("老 黄 历", subtitle = "${a.lunarText} · ${a.dayGz}日 · ${a.chongSha}", sections = secs)
    }

    private fun meiHuaResult(g: MeiHua.GuaData) = CastResult(
        title = "梅 花 易 数", subtitle = "${g.upper}上${g.lower}下 · ${g.deltaYao}爻动",
        sections = listOf(
            CastSection("卦象", rows = listOf(
                "本卦" to MeiHua.guaName(g.base), "互卦" to MeiHua.guaName(g.hu),
                "变卦" to MeiHua.guaName(g.bian), "错卦" to MeiHua.guaName(g.cuo),
                "综卦" to MeiHua.guaName(g.zong)
            )),
            CastSection("体用", rows = listOf(
                "体卦" to g.tiGua + "(" + g.tiWx + ")", "用卦" to g.yongGua + "(" + g.yongWx + ")",
                "生克" to g.relation
            ), highlighted = true),
            CastSection("卦画", rows = listOf(
                "本" to g.base, "互" to g.hu, "变" to g.bian, "错" to g.cuo, "综" to g.zong
            ))
        )
    )
}
