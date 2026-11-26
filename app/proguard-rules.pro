# R8 优化规则
# 数据类保留：字段可能被 SharedPreferences / SQLite 按名读写，避免 R8 裁剪或重命名
-keep class com.fortune.stall.data.** { *; }

# Activity 入口：Android 框架按类名反射实例化
-keep class com.fortune.stall.MainActivity { *; }

# 术数引擎为纯计算代码、全部是静态直接调用（无反射 / Gson / JNI），
# 交由 R8 内联与裁剪：减小 classes.dex、加快冷启动 dexopt。
# Compose 自身的 consumer rules 会保留 @Composable 入口，无需额外 keep。
