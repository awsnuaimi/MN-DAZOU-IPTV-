package com.dazou.iptvplayer.utils

import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.model.XtreamCategory

/**
 * يربط مجلدات معيّنة (دول أو قنوات) بصور حقيقية (أعلام/لوغوهات) بدل رموز الأعلام النصية (Emoji)،
 * لأن بعض أجهزة التلفاز ما بتدعم رموز الأعلام وبتظهرها فاضية أو مربع.
 */
object CategoryIconMapper {

    // مجلدات الدول (بالكود الخاص فيها، متل GROUP:AR أو GROUP:LT)
    private val codeIcons = mapOf(
        "AR" to R.drawable.flag_ar,
        "LT" to R.drawable.flag_lt,
        "JP" to R.drawable.flag_jp
    )

    // مجلدات حقيقية بأسمائها بالضبط (بدون علاقة بكود الدولة)
    private val nameIcons = mapOf(
        "bein sports max 8k" to R.drawable.logo_bein_sports,
        "bein sports max fm" to R.drawable.logo_bein_sports,
        "bein sports max be" to R.drawable.logo_bein_sports,
        "bein sports max g" to R.drawable.logo_bein_sports,
        "bein sports max nm" to R.drawable.logo_bein_sports,
        "world cup 2026" to R.drawable.logo_world_cup,
        "world cup replay" to R.drawable.logo_world_cup
    )

    /** يرجع رقم الصورة المناسبة للمجلد، أو null لو ما في صورة مخصصة له (يستخدم الأيقونة الافتراضية 📁) */
    fun iconFor(category: XtreamCategory): Int? {
        if (category.categoryId.startsWith("GROUP:")) {
            val code = category.categoryId.removePrefix("GROUP:")
            return codeIcons[code]
        }
        val key = category.categoryName.trim().lowercase()
        return nameIcons[key]
    }
}