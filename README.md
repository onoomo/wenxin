# 问心处 · FortuneStall (Android)

夜市尽头的一张算卦小摊 —— 自托管占卜 App 的 Android 原生版。**小六壬 / 六爻 / 塔罗**三味齐备，深空星幕界面，卦面即印章。

> 界面与玩法参考 [wave2234/fortune-stall](https://github.com/wave2234/fortune-stall)，占卜内核移植自 [wave2234/divination-skills](https://github.com/wave2234/divination-skills)（MIT）。

## 技术栈（指定版本）

| 组件 | 版本 |
|---|---|
| Kotlin | 2.4.20 |
| Compose Compiler | 2.4.20（`org.jetbrains.kotlin.plugin.compose`） |
| Android Gradle Plugin | 9.5.0 |
| Gradle | 9.9.1 |
| Compose BOM | 2026.11.01 |
| minSdk / targetSdk / compileSdk | 24 / 36 / 36 |

全部依赖通过 `gradle/libs.versions.toml`（version catalog）集中管理。

## 核心特性

### 全屏沉浸 + 挖孔屏适配（API 24–36）
- 隐藏状态栏与导航栏，`WindowCompat` + `WindowInsetsControllerCompat` 实现 edge-to-edge
- 手势导航下从边缘滑动可临时唤出系统栏（`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`）
- **API 27+**：主题 `windowLayoutInDisplayCutoutMode=shortEdges`，内容延伸到刘海/挖孔短边区域
- **API 29+**：关闭 `enforceStatusBar/NavigationBarContrast`，避免系统叠加灰遮罩
- 分版本资源：`values/`（24–26）、`values-v27/`、`values-v29/`

### 三法起卦（卦面即印章）
所有随机只在本地引擎发生，落定即锁，写入 SQLite 卦账；UI 只呈现结果与动画。

- **小六壬**：按时 / 农历 / 报数 / 掷骰（三宫掐指，含体用、旺衰、十二长生、活六神、地支择法、六亲、转太极）
- **六爻**：按时 / 报数 / 摇铜钱（八宫六十四卦自动生成，纳甲纳支、世应、六亲六神、旬空月破、伏神、动变关系）
- **塔罗**：单牌 / 三牌 / 凯尔特十字 / 自定张数 / 自报牌名（RWS 78 张，正逆位、元素分布与互动）
- 农历/干支换算为自实现算法（`LunarCalendar`），已与 cnlunar 交叉校验

### 界面
- 深空星幕：Canvas 绘制双层漂移星尘 + 闪烁 + 偶发流星
- 问心处首页、三大摊位、卦账、心法（技能包资料）四个页面
- 仪式动画：骰子摇动落定、方孔铜钱逐爻显现（铸「元亨利貞」）、塔罗 3D 翻牌 + 逆位倒转
- 结果卡片：印章落款、卦面全文折叠、一键复制卦面给 AI 断

### 资料内嵌
`assets/skills/` 内置三套技能包的 `SKILL.md`、`references/` 与 `scripts/` 原文，App 内「心法」页可直接查阅。

## 构建

```bash
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

需要 Android SDK Platform 36 + Build Tools 36.0.0，JDK 17+。

## 目录结构

```
app/src/main/
├── java/com/fortune/stall/
│   ├── MainActivity.kt              # 导航 + 全屏沉浸入口
│   ├── engine/                      # 三套起卦引擎 + 统一门面
│   │   ├── XiaoLiuRen.kt / LiuYao.kt / Tarot.kt / FortuneEngine.kt
│   ├── data/                        # SQLite 卦账 + 技能包资料库
│   ├── util/                        # 农历干支换算 + 全屏工具
│   ├── ui/theme/                    # 深空配色 + 字体 + 主题
│   ├── ui/components/               # 星幕/骰子/铜钱/塔罗牌/结果卡片
│   └── ui/screen/                   # 首页/起卦/卦账/心法
├── res/                             # 主题(分版本)/图标/字符串
└── assets/skills/                   # divination-skills 原文
```

## 免责
占卜是文化实践与自省工具，不构成医疗、法律、投资或任何专业建议。请勿据此做重大决策。

## 许可
MIT
