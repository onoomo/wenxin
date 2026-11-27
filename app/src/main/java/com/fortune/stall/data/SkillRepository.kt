package com.fortune.stall.data

import android.content.Context

/**
 * 技能包心法资料库 —— 从 assets/skills 读取 divination-skills 的 SKILL.md 与分册。
 *
 * 内存优化：文档正文不再随 packs 一次性读入内存常驻，改为点开时按需读取；
 * 标题只读首行；正文用小 LRU 缓存保留最近 2 篇，返回列表不立即释放、重进不重读。
 */
class SkillRepository(private val context: Context) {

    /** 文档元数据：标题与路径。正文通过 readContent() 按需获取，不常驻内存。 */
    data class Doc(val title: String, val path: String) {
        fun readContent(repo: SkillRepository): String = repo.contentOf(path)
    }

    data class Pack(val key: String, val name: String, val subtitle: String, val skill: Doc, val refs: List<Doc>)

    /** 仅保留最近 2 篇正文，控制常驻内存 */
    private val cache = object : LinkedHashMap<String, String>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 2
    }

    private fun openText(path: String) = try {
        context.assets.open(path).bufferedReader(Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }

    /** 只读第一行标题（# xxx），避免为取标题而读整篇 */
    private fun titleOf(path: String): String {
        val br = openText(path) ?: return path.substringAfterLast('/').removeSuffix(".md")
        return br.useLines { lines ->
            lines.firstOrNull { it.startsWith("# ") }?.removePrefix("# ")?.trim()
        } ?: path.substringAfterLast('/').removeSuffix(".md")
    }

    /** 按需读取正文（含 2 篇 LRU 缓存） */
    fun contentOf(path: String): String {
        cache[path]?.let { return it }
        val text = openText(path)?.use { it.readText() }
            ?: "# 资料缺失\n\n无法读取 $path\n\n请将 divination-skills 技能包放入 assets/skills 目录。"
        cache[path] = text
        return text
    }

    /** 只扫描文件名构建目录，零全文读取 */
    val packs: List<Pack> by lazy {
        val defs = listOf(
            Triple("xiaoliuren", "小六壬", "快卦 · 三宫掐指"),
            Triple("tarot", "塔罗", "心镜 · 七十八张牌"),
            Triple("liuyao", "六爻", "深卦 · 纳甲装卦")
        )
        defs.map { (key, name, subtitle) ->
            val skillPath = "skills/$key/SKILL.md"
            val refDir = "skills/$key/references"
            val refFiles = try { context.assets.list(refDir)?.filter { it.endsWith(".md") }?.sorted() ?: emptyList() }
                           catch (e: Exception) { emptyList() }
            Pack(
                key = key,
                name = name,
                subtitle = subtitle,
                skill = Doc("心法总纲 · $name", skillPath),
                refs = refFiles.map { Doc(titleOf("$refDir/$it"), "$refDir/$it") }
            )
        }
    }
}
