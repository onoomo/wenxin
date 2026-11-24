# ===== 极限压缩（R8 full mode） =====
# 本项目无反射、无 Gson/Moshi/kotlinx.serialization、无 Parcelable 按类名传递，
# 引擎与结果类全部为编译期静态调用，R8 可安全裁剪与混淆，无需整包 keep。
-optimizationpasses 10
-allowaccessmodification

# MainActivity 由 AndroidManifest 引用（显式保留，防止极端情况下被裁）
-keep class com.fortune.stall.MainActivity { *; }

# 抑制第三方库的无害告警
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.compose.**
-dontwarn kotlin.**
