package com.fortune.stall.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.stall.data.SkillRepository
import com.fortune.stall.ui.components.Starfield
import com.fortune.stall.ui.theme.AppColors

/** 心法页：三套技能包的 SKILL.md 与分册资料，内置查阅 */
@Composable
fun SkillsScreen(repo: SkillRepository, onBack: () -> Unit) {
    var viewing by remember { mutableStateOf<SkillRepository.Doc?>(null) }
    // 阅读文档时，系统返回键先回到技能包列表；在列表时才交给 MainActivity 退出技能包
    BackHandler(enabled = viewing != null) { viewing = null }
    Box(Modifier.fillMaxSize()) {
        Starfield(Modifier.fillMaxSize())
        if (viewing == null) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(52.dp))
                Spacer(Modifier.height(6.dp))
                Text("心法 · 技能包", color = AppColors.Paper, fontSize = 26.sp, fontFamily = FontFamily.Serif,
                    letterSpacing = 3.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("小六壬 · 六爻 · 塔罗 的断卦工作流与资料分册", color = AppColors.Muted, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))

                repo.packs.forEach { pack ->
                    Text(pack.name + " · " + pack.subtitle, color = AppColors.Lantern, fontSize = 13.sp,
                        letterSpacing = 2.sp, modifier = Modifier.padding(vertical = 8.dp))
                    DocRow(pack.skill.title) { viewing = pack.skill }
                    pack.refs.forEach { ref -> DocRow(ref.title) { viewing = ref } }
                    Spacer(Modifier.height(16.dp))
                }
                Spacer(Modifier.height(60.dp))
            }
        } else {
            DocReader(doc = viewing!!, repo = repo, onBack = { viewing = null })
        }
    }
}

@Composable
private fun DocRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp)).background(Color(0x0DFFFFFF)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = AppColors.Paper, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
        Text("›", color = AppColors.Dim, fontSize = 16.sp)
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun DocReader(doc: SkillRepository.Doc, repo: SkillRepository, onBack: () -> Unit) {
    // 正文按需读取：进入时才加载这一篇，后台线程读取，不阻塞主线程
    var content by remember(doc.path) { mutableStateOf("") }
    LaunchedEffect(doc.path) {
        content = withContext(Dispatchers.IO) { doc.readContent(repo) }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(52.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹ 返回", color = AppColors.Muted, fontSize = 15.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onBack)
                    .padding(horizontal = 10.dp, vertical = 12.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(doc.title, color = AppColors.Lantern, fontSize = 18.sp, fontFamily = FontFamily.Serif,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
        Text(
            content, color = AppColors.Paper, fontSize = 12.5.sp, lineHeight = 21.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)
        )
    }
}
