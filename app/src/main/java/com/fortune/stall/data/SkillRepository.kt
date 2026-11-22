package com.fortune.stall.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/** 技能包心法资料库 —— 从 assets/skills 读取 divination-skills 的 SKILL.md 与分册 */
class SkillRepository(private val context: Context) {

    data class Doc(val title: String, val path: String, val content: String)
    data class Pack(val key: String, val name: String, val subtitle: String, val skill: Doc, val refs: List<Doc>)

    private fun readAsset(path: String): String = try {
        context.assets.open(path).use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { br ->
                br.readText()
            }
        }
    } catch (e: Exception) {
        "# 资料缺失\n\n无法读取 $path\n\n请将 divination-skills 技能包放入 assets/skills 目录。"
    }

    private fun titleOf(path: String): String {
        val content = readAsset(path)
        return content.lines().firstOrNull { it.startsWith("# ") }?.removePrefix("# ")?.trim()
            ?: path.substringAfterLast('/').removeSuffix(".md")
    }

    val packs: List<Pack> by lazy {
        val defs = listOf(
            Triple("xiaoliuren", "小六壬", "快卦 · 三宫掐指"),
            Triple("liuyao", "六爻", "深卦 · 纳甲装卦"),
            Triple("tarot", "塔罗", "心镜 · 七十八张牌")
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
                skill = Doc("心法总纲 · $name", skillPath, readAsset(skillPath)),
                refs = refFiles.map { Doc(titleOf("$refDir/$it"), "$refDir/$it", readAsset("$refDir/$it")) }
            )
        }
    }
}
