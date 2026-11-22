package com.fortune.stall.util

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 全屏沉浸工具 —— 隐藏状态栏/导航栏，适配挖孔屏，兼容 API 24-36。
 *
 * - API 24-26：通过 WindowManager flag 实现基础全屏
 * - API 27+：主题中 windowLayoutInDisplayCutoutMode=shortEdges 让内容延伸到挖孔
 * - API 29+：主题中关闭 enforceStatusBar/NavigationBarContrast，避免系统加灰遮罩
 * - 使用 WindowInsetsControllerCompat 统一处理 hide/show，手势导航下点击边缘可唤出
 */
object Immersive {

    /** 进入全屏：隐藏状态栏与导航栏 */
    fun enter(activity: Activity) {
        val window = activity.window
        val view = window.decorView

        // 让内容绘制到系统栏背后（edge-to-edge）
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 状态栏/导航栏透明
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // API 27+ 挖孔屏：允许内容进入挖孔短边区域
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        // API 29+ 关闭系统对比遮罩
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        val controller = WindowInsetsControllerCompat(window, view)
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // 低版本 flag 兜底（API 24-26）
        @Suppress("DEPRECATION")
        view.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
    }

    /** 从沉浸式状态恢复（如弹出输入框需要键盘时可选调用） */
    fun exit(activity: Activity) {
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
    }

    /**
     * 只显示系统底部导航栏（手势横条），状态栏仍保持隐藏。
     * 用于个别希望用户能用系统返回手势的页面（如传承页）。
     * 离开该页面时调用 [enter] 恢复全屏沉浸。
     */
    fun showNavigationBar(activity: Activity) {
        val window = activity.window
        val view = window.decorView
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val controller = WindowInsetsControllerCompat(window, view)
        controller.show(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // 低版本 flag 兜底：仍全屏隐藏状态栏，但不隐藏导航栏（去掉 HIDE_NAVIGATION）
        @Suppress("DEPRECATION")
        view.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
    }

    /**
     * 获取需要避让的安全区顶部/底部像素（挖孔、圆角）。
     * 全屏下大多数区域可直接利用；若某些内容需避让刘海，可用此值加 padding。
     */
    fun cutoutInsets(view: View): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return 0 to 0
        val cutout = view.rootWindowInsets?.displayCutout ?: return 0 to 0
        return cutout.safeInsetTop to cutout.safeInsetBottom
    }
}
