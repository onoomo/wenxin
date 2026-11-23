package com.fortune.stall.util

/**
 * 全局问 AI 桥接：由 MainActivity 注入导航实现，各排盘结果页直接调用，
 * 避免为本功能给每一个 Screen 层层追加 onAskAi 参数。
 */
object AiBridge {
    var askAi: ((String) -> Unit)? = null
}
