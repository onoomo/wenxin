package com.fortune.stall.ui.screen

import com.fortune.stall.R

/** 首页术法条目：名称 + 目标 key + 图标 + 眉标 + 详细说明 */
data class HomeTile(
    val label: String, val key: String, val glyph: Glyph,
    val eyebrow: String, val desc: String
)

/** 首页术法清单（默认顺序）。图标/详细两种模式、排序与隐藏设置共用这一份。 */
val HOME_TILES = listOf(
    HomeTile("八字命理", "bazi-paipan", Glyph.BAZI, "四 柱 命 理",
        "以生辰八字排定年月日时四柱，配十神藏干、五行旺衰、神煞纳音与大运流年，看一生格局起伏与喜用取舍。"),
    HomeTile("命理奇门", "mingliqimen", Glyph.MINGLI, "终 身 奇 门",
        "以生辰起终身奇门盘，配大运流年逐限推移，看一生富贵吉凶与关键时刻的天地人三才格局。"),
    HomeTile("阴盘奇门", "yinpanqimen", Glyph.YINPAN, "阴 盘 起 局",
        "阴盘奇门时盘起局，布三奇六仪、八神九星八门，隐干、马星、空亡一并排出，用于占断当下事体。"),
    HomeTile("奇门遁甲", "qimen", Glyph.QIMEN, "九 宫 时 盘",
        "按时辰起局，拆补、置闰、茅山定阴阳遁局数，值符值使、天盘地盘九宫齐备，问事择方的经典术数。"),
    HomeTile("紫微斗数", "ziwei", Glyph.ZIWEI, "十 八 飞 星",
        "以生辰安命身宫，布十四主星、辅弼魁钺与四化，十二宫逐格推演，细看性情、事业、姻缘与财运。"),
    HomeTile("六爻", "liuyao", Glyph.LIUYAO, "纳 甲 装 卦",
        "铜钱摇卦、纳甲装卦，六亲六神、世应动变、伏神神煞俱全，大事慢问，一爻一爻看事体成败。"),
    HomeTile("梅花易数", "meihua-time", Glyph.MEIHUA, "观 象 起 卦",
        "时间、报数、手动三种起卦，取上下卦与动爻，推本卦、互卦、变卦、错综卦，以体用生克断吉凶。"),
    HomeTile("大六壬", "daliuren", Glyph.DALIUREN, "三 式 之 首",
        "四课三传、天盘月将、贵人六亲与十二宫行年，占事时辰可灵活设定，断事精细，被誉为三式之首。"),
    HomeTile("潜虚筮法", "qianxu", Glyph.QIANXU, "七 十 演 策",
        "取七十策虚其五，分二挂一揲十定名，得五十五名之一；再揲以求变位，按阴阳取显幽，断吉凶臧否。"),
    HomeTile("塔罗牌占卜", "tarot", Glyph.TAROT, "心 象 之 镜",
        "七十八张牌照心象，单张、三牌或凯尔特十字多种牌阵，随问随翻，照见当下心境与走向。"),
    HomeTile("雷诺曼", "lenormand", Glyph.LENORMAND, "三 十 六 牌",
        "三十六张雷诺曼牌，单牌、三牌、九宫到大桌多种牌阵，以牌位与相邻合读照见事情来龙去脉。"),
    HomeTile("小六壬", "xiaoliuren", Glyph.XIAOLIUREN, "掐 指 快 卦",
        "掐指三宫，问急事小事。按时起课或摇骰报数，一掐即得答案，是随身携带的快捷占法。"),
    HomeTile("戚都翁", "qiduweng", Glyph.QIDU, "未 卜 先 知",
        "以时间起课，定体用星、月令旺衰与五行类象，断来人办事的成败迟速，属古法未卜先知。"),
    HomeTile("奇门穿壬", "chuanren", Glyph.CHUANREN, "奇 壬 合 参",
        "奇门九宫与大六壬贵神、月将、年煞、命宫同盘合参，二式互证，占断更为周全。"),
    HomeTile("山向奇门", "shanxiangqimen", Glyph.SHANXIANG, "二 十 四 山",
        "按罗盘度数查七十二局，以年柱代日、五鼠遁定时，布阳阴遁九宫盘，专用于立向择方。"),
    HomeTile("玄空飞星", "ninestar", Glyph.FEIXING, "洛 书 飞 泊",
        "年、月、日、时四颗飞星入九宫，飞泊洛书配九星吉凶，可结合经度校正真太阳时，看方位旺衰。"),
    HomeTile("达摩一掌经", "yizhangjing", Glyph.YIZHANG, "六 道 轮 回",
        "以生辰年月日时地支落宫，查六道与神煞，断一生因果、性情与早年晚景，一掌之中见轮回。"),
    HomeTile("万年历", "wannianli", Glyph.WANNIANLI, "公 农 对 照",
        "整月日历逐日列出农历、干支、节气与生肖，点选任一天即可查当日老黄历，日期查询的入口。"),
    HomeTile("老黄历", "laohuangli", Glyph.LAOHUANGLI, "择 日 经 典",
        "宜忌、冲煞、值神建星、二十八宿、纳音、吉神凶煞、彭祖百忌与财喜福神方位，择日办事的参照。"),
    HomeTile("手机号预测", "shuzi", Glyph.PHONE, "磁 场 数 理",
        "手机号八星磁场分析，延年、天医、生气为吉，六煞、五鬼、祸害、绝命为凶，逐位解读尾数与象义。")
)

/** 图标类型 */
enum class Glyph {
    BAZI, MINGLI, YINPAN, QIMEN, ZIWEI, LIUYAO, MEIHUA, DALIUREN, XIAOLIUREN,
    QIDU, CHUANREN, SHANXIANG, FEIXING, YIZHANG, WANNIANLI, LAOHUANGLI, HEHUN, PHONE, TAROT, QIANXU, LENORMAND
}

/** 原版 ji.js.cn 的 SVG 图标资源（1:1 移植） */
fun glyphRes(g: Glyph): Int = when (g) {
    Glyph.BAZI -> R.drawable.ic_g_bazi
    Glyph.MINGLI -> R.drawable.ic_g_mingli
    Glyph.YINPAN -> R.drawable.ic_g_yinpan
    Glyph.QIMEN -> R.drawable.ic_g_qimen
    Glyph.ZIWEI -> R.drawable.ic_g_ziwei
    Glyph.LIUYAO -> R.drawable.ic_g_liuyao
    Glyph.MEIHUA -> R.drawable.ic_g_meihua
    Glyph.DALIUREN -> R.drawable.ic_g_daliuren
    Glyph.XIAOLIUREN -> R.drawable.ic_g_xiaoliuren
    Glyph.QIDU -> R.drawable.ic_g_qidu
    Glyph.CHUANREN -> R.drawable.ic_g_chuanren
    Glyph.SHANXIANG -> R.drawable.ic_g_shanxiang
    Glyph.FEIXING -> R.drawable.ic_g_feixing
    Glyph.YIZHANG -> R.drawable.ic_g_yizhang
    Glyph.WANNIANLI -> R.drawable.ic_g_wannianli
    Glyph.LAOHUANGLI -> R.drawable.ic_g_laohuangli
    Glyph.HEHUN -> R.drawable.ic_g_hehun
    Glyph.PHONE -> R.drawable.ic_g_phone
    Glyph.TAROT -> R.drawable.ic_g_tarot
    Glyph.QIANXU -> R.drawable.ic_g_qianxu
    Glyph.LENORMAND -> R.drawable.ic_g_tarot
}
