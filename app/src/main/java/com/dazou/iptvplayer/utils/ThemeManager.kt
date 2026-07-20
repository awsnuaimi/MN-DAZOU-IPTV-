package com.dazou.iptvplayer.utils

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import com.dazou.iptvplayer.databinding.ActivityMainBinding

/**
 * يدير لون العلامة التجارية (Accent) للتطبيق — يبني ألوان الفوكس والتمييز برمجيًا
 * وقت التشغيل بدل الاعتماد على ألوان ثابتة بملفات XML، حتى نقدر نبدّل التيم بسهولة.
 */
object ThemeManager {

    enum class AppTheme(val id: String, val accent: Int, val accentDark: Int) {
        BURGUNDY("burgundy", Color.parseColor("#7A1F3D"), Color.parseColor("#5C1730")),
        BLACK("black", Color.parseColor("#2E2E2E"), Color.parseColor("#161616")),
        BLUE("blue", Color.parseColor("#1F3A7A"), Color.parseColor("#152852")),
        GREEN("green", Color.parseColor("#1F5C34"), Color.parseColor("#123D22")),
        RED("red", Color.parseColor("#7A1F1F"), Color.parseColor("#521414"))
    }

    private const val PREFS = "dazou_prefs"
    private const val KEY_THEME = "app_accent_theme"

    fun getSavedTheme(context: Context): AppTheme {
        val id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_THEME, AppTheme.BURGUNDY.id)
        return AppTheme.values().firstOrNull { it.id == id } ?: AppTheme.BURGUNDY
    }

    fun saveTheme(context: Context, theme: AppTheme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_THEME, theme.id).apply()
    }

    private fun dp(activity: Activity, value: Float): Float = value * activity.resources.displayMetrics.density

    /** خلفية مستطيلة بزوايا مدوّرة — لعناصر القائمة النصية (الرئيسية/القنوات/أفلام...) */
    private fun rectFocusSelector(theme: AppTheme, radiusDp: Float): StateListDrawable {
        val focused = GradientDrawable().apply {
            setColor(theme.accent)
            cornerRadius = radiusDp
        }
        val normal = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = radiusDp
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(intArrayOf(), normal)
        }
    }

    /** حلقة دائرية رفيعة — لأيقونات الشريط العلوي المستديرة */
    private fun ringFocusSelector(theme: AppTheme): StateListDrawable {
        val pressed = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(theme.accentDark)
        }
        val focused = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
            setStroke(4, theme.accent)
        }
        val normal = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressed)
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(intArrayOf(), normal)
        }
    }

    /** خلفية مستطيلة مليانة (للأزرار العريضة زي "القنوات المباشرة") */
    private fun filledFocusSelector(theme: AppTheme, radiusDp: Float): StateListDrawable {
        val focused = GradientDrawable().apply {
            setColor(theme.accent)
            cornerRadius = radiusDp
            setStroke(2, Color.WHITE)
        }
        val normal = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = radiusDp
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(intArrayOf(), normal)
        }
    }

    /** يطبّق التيم المحفوظ على كل عناصر الشريط العلوي وعناصر التحكم الأساسية بالمرحلة الأولى */
    fun applyToMainScreen(activity: Activity, binding: ActivityMainBinding) {
        val theme = getSavedTheme(activity)
        val radius18 = dp(activity, 18f)
        val radius12 = dp(activity, 12f)

        listOf(binding.menuHome, binding.menuLive, binding.menuMovies, binding.menuSeries, binding.menuEpg)
            .forEach { it.background = rectFocusSelector(theme, radius18) }

        listOf(
            binding.account, binding.themeToggle, binding.settings, binding.searchIcon,
            binding.channelsPanelBack, binding.btnPrev, binding.btnPlayPause, binding.btnNext,
            binding.btnVolume, binding.btnFullscreen, binding.btnPip
        ).forEach { it.background = ringFocusSelector(theme) }

        binding.sidebarLiveButton.background = filledFocusSelector(theme, radius12)

        binding.appLogo.setTextColor(theme.accent)
        binding.channelsPanelBack.setTextColor(theme.accent)

        val accentList = ColorStateList.valueOf(theme.accent)
        binding.pbNowProgress.progressTintList = accentList
        binding.pbControlsProgress.progressTintList = accentList
        binding.playerLoading.indeterminateTintList = accentList
    }
}