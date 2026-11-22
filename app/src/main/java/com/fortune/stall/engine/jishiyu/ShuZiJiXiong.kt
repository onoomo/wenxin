package com.fortune.stall.engine.jishiyu

/** 数字吉凶(手机号磁场) —— 1:1 移植自吉时雨 sj.js (AGPL-3.0, xianbo.chen@gmail.com) */
object ShuZiJiXiong {
    val JI_SHEN = mapOf(
        "延年" to arrayOf("有能力，有领导力，德高望重，有远见和格局，爱操心，凡事喜欢亲力亲为", "做事情具有运筹帷幄的能力，能挣钱，一吉多者，不是老板就是高管", "有责任心，喜欢照顾人，爱操心，有些大男子主义平时压力大，注意颈椎病", "女人在此磁场比较强势，不利婚姻感情"),
        "天医" to arrayOf("天医磁场代表婚姻，健康，财富，此磁场多者，心地善良，但是容易被骗", "人很好，乐于助人，人际关系好", "小孩在此磁场学习较好", "此磁场多者也要多注意血压和血液循环方面的疾病"),
        "生气" to arrayOf("爱面子，讲诚信，重名不重利，对朋友够义气", "爱学习，易清高，容易自以为是", "乐观主义，随缘心比较强，缺乏理财观念", "事业上容易有贵人相助", "此磁场多者，易发胖，易有糖尿病等富贵病"),
    )
    val XIONG_SHEN = mapOf(
        "六煞" to arrayOf("财运：生活开支大，不存钱，攒不住钱。 有钱就有事，一年到头白忙", "健康：容易手脚冰凉，肠胃疾病，泌尿系统疾病，肾病，妇科疾病", "事业：情商高，适合做服务性行业，抗压能力弱", "职业：适合服务性行业，与客户直接接触的行业，客服，售后，接待等", "性格：容易情绪化，感性，抑郁，容易为情所困，异性缘好", "正派女人对孩子有影响(流产，孩子走丢，家人走失)，容易有血光之灾(手术，意外，红伤等)"),
        "五鬼" to arrayOf("财运：容易出现投资失误，债务纠纷，负债，官司，借出去的钱收不回来", "健康：容易有心脑血管疾病。女性的妇科:盆腔炎，附件炎，等各种炎症问题，严重的导致癌变", "事业：付出多收获少，有志难伸，能看到钱但是拿不到钱", "职业：适合从事思考类型的工作，律师、会计、设计师、编辑等等", "性格：聪明，应变能力强。对家人容易发脾气属于无名火"),
        "祸害" to arrayOf("财运：适合以口来财，但是财运不佳", "健康：口腔，咽喉，淋巴，属于药罐子老年人慎用", "事业：百事不顺，犯小人，遭人暗算，被别人打小报告，抢客户等等", "职业：适合以口为业，讲师、销售、老师、律师等", "性格：爱面子，说话直来直去不考虑别人感受，较真，钻牛角尖，很难接受别人的意见，只认可自己的想法"),
        "绝命" to arrayOf("财运：大的投资失误，最后竹篮打水一场空", "健康：颈椎病，肝肾疾病，泌尿系统疾病，糖尿病，以及各种癌症", "事业：白手起家，大起大落，事业不稳定，容易丢官罢职，破财伤人", "性格：容易冲动，脾气不好，讲义气，重感情，容易在朋友身上吃亏", "婚姻不和睦（分居离婚 丧偶等情况）"),
    )
    val TAIL_ZERO = arrayOf("孩子不听话，或者孩子身体差，容易生病", "夫妻之间总吵架，容易离婚，易单身", "自己付出多，到头一场空，或自己身体容易出问题", "兄妹手足之间无助力，靠自己打拼", "双方父母辈身体差，易出现先走一方")
    val TY_MID_ZERO = arrayOf("资金被套，夫妻容易短暂异地分居")
    val YN_MID_ZERO = arrayOf("事业停止，卡顿，难以突破")
    val JI_DIGIT = mapOf(
        "19" to "延年",
        "91" to "延年",
        "78" to "延年",
        "87" to "延年",
        "34" to "延年",
        "43" to "延年",
        "26" to "延年",
        "62" to "延年",
        "13" to "天医",
        "31" to "天医",
        "68" to "天医",
        "86" to "天医",
        "49" to "天医",
        "94" to "天医",
        "27" to "天医",
        "72" to "天医",
        "14" to "生气",
        "41" to "生气",
        "67" to "生气",
        "76" to "生气",
        "39" to "生气",
        "93" to "生气",
        "28" to "生气",
        "82" to "生气",
    )
    val XIONG_DIGIT = mapOf(
        "16" to "六煞",
        "61" to "六煞",
        "29" to "六煞",
        "92" to "六煞",
        "47" to "六煞",
        "74" to "六煞",
        "38" to "六煞",
        "83" to "六煞",
        "18" to "五鬼",
        "81" to "五鬼",
        "79" to "五鬼",
        "97" to "五鬼",
        "63" to "五鬼",
        "36" to "五鬼",
        "42" to "五鬼",
        "24" to "五鬼",
        "17" to "祸害",
        "71" to "祸害",
        "89" to "祸害",
        "98" to "祸害",
        "46" to "祸害",
        "64" to "祸害",
        "23" to "祸害",
        "32" to "祸害",
        "12" to "绝命",
        "21" to "绝命",
        "37" to "绝命",
        "73" to "绝命",
        "48" to "绝命",
        "84" to "绝命",
        "96" to "绝命",
        "69" to "绝命",
    )
    val SHEN_TYPE = mapOf(
        "延年" to "事业/健康",
        "天医" to "财富/婚姻",
        "生气" to "贵人/人脉",
        "六煞" to "情商/桃花",
        "五鬼" to "智慧/灵性",
        "祸害" to "口才/疾病",
        "绝命" to "投资/情绪",
    )

    data class Analysis(val groups: List<String>, val results: List<String>,
        val resultsShen: List<String>, val resultsType: List<String>,
        val tailZero: List<String>, val counts: Map<String,Int>)

    /** isMan: 男把5当2, 女把5当8 */
    fun analyze(phoneNo0: String, isMan: Boolean): Analysis {
        var phoneNo = if (isMan) phoneNo0.replace("5","2") else phoneNo0.replace("5","8")
        val nums = phoneNo.toCharArray().map { it.toString() }
        val groups = mutableListOf<String>()
        val results1 = mutableListOf<String>()
        val results2 = mutableListOf<String>()
        var i = 1
        while (i < nums.size) {
            val cur = nums[i]; val prev = nums[i-1]
            if (cur != "0" && prev != cur) { groups.add(prev+cur) }
            else if (i != 1 && prev == cur) { if (groups.isNotEmpty()) groups.add(groups.last()) }
            else if (cur == "0") {
                val d = prev; var z = 0
                while (i < nums.size) {
                    if (nums[i] == "0") { z++; if (i == nums.size-1) break }
                    else {
                        if (nums[i] == d) { repeat(z+1){ if(groups.isNotEmpty()) groups.add(groups.last()) }; break }
                        else {
                            val dit = ""+d+nums[i]
                            if (JI_DIGIT[dit]=="天医" && results1.isEmpty()) results1.add(TY_MID_ZERO[0])
                            if (JI_DIGIT[dit]=="延年" && results2.isEmpty()) results2.add(YN_MID_ZERO[0])
                            repeat(z+1){ groups.add(""+d+nums[i]) }
                            break
                        }
                    }
                    i++
                }
            }
            i++
        }
        val delta = 10 - groups.size
        if (delta == 1) groups.add(phoneNo.substring(nums.size-2))
        else if (delta > 1) {
            val st = nums.size-(delta+1)
            groups.add(phoneNo.substring(st, st+2))
            repeat(delta-1){ groups.add("00") }
        }
        // results: 从后往前按神名去重吉/凶(JS 用数组引用去重实际无效, 这里按神名去重更符合语义)
        val results = mutableListOf<String>(); val resultsShen=mutableListOf<String>(); val resultsType=mutableListOf<String>()
        for (idx in groups.indices.reversed()) {
            val g = groups[idx]
            val jiName = JI_DIGIT[g]
            if (jiName != null && JI_SHEN.containsKey(jiName) && jiName !in results) {
                results.add(jiName); resultsShen.add(jiName); resultsType.add(SHEN_TYPE[jiName] ?: "")
            }
            val xiongName = XIONG_DIGIT[g]
            if (xiongName != null && XIONG_SHEN.containsKey(xiongName) && xiongName !in results) {
                results.add(xiongName); resultsShen.add(xiongName); resultsType.add(SHEN_TYPE[xiongName] ?: "")
            }
        }
        // counts
        val counts = mutableMapOf<String,Int>()
        for (g in groups) {
            for (k in listOf(JI_DIGIT[g], XIONG_DIGIT[g])) if (k!=null) counts[k]=(counts[k]?:0)+1
        }
        // 尾数0释义
        val tailZero = mutableListOf<String>()
        for (k in 1..5) if (nums.size-k >= 0 && nums[nums.size-k]=="0") tailZero.add(TAIL_ZERO[k-1])
        return Analysis(groups, results, resultsShen, resultsType, tailZero, counts)
    }
}