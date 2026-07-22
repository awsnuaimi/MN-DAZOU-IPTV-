package com.dazou.iptvplayer.utils

import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.model.XtreamCategory

/**
 * يربط مجلدات معيّنة (دول أو قنوات) بصور حقيقية (أعلام/لوغوهات) بدل رموز الأعلام النصية (Emoji).
 */
object CategoryIconMapper {

    // مجلدات الدول (بالكود الخاص فيها)
    private val codeIcons = mapOf(
        "AR" to R.drawable.flag_ar,
        "LT" to R.drawable.flag_lt,
        "JP" to R.drawable.flag_jp
    )

    // مجلدات حقيقية — مطابقة "يحتوي على" بدل "يطابق بالضبط"، عشان تتحمّل أي بادئة
    // أو رموز أو مسافات زيادة بالاسم الأصلي بالسيرفر
    private val nameContainsIcons = listOf(
        "bein sports max 8k" to R.drawable.logo_bein_sports,
        "bein sports max fm" to R.drawable.logo_bein_sports,
        "bein sports max be" to R.drawable.logo_bein_sports,
        "bein sports max g" to R.drawable.logo_bein_sports,
        "bein sports max nm" to R.drawable.logo_bein_sports,
        "world cup 2026" to R.drawable.logo_world_cup,
        "world cup replay" to R.drawable.logo_world_cup,
        "world cup" to R.drawable.logo_world_cup // احتياط شامل لو الاسم مختلف شوي عن التوقع
    )

    private val bracketCodeRegex = Regex("^\\[([A-Za-z]{2,8})\\]")

    fun iconFor(category: XtreamCategory): Int? {
        if (category.categoryId.startsWith("GROUP:")) {
            val code = category.categoryId.removePrefix("GROUP:")
            return codeIcons[code]
        }

        val rawName = category.categoryName.trim()

        // ✅ يتحقق من رمز الدولة حتى لو المجلد لسا يحمل بادئة [XX] الأصلية
        // (يعني ما انضم لمجموعة لأنه كان الوحيد بهيك رمز بالسيرفر)
        val bracketMatch = bracketCodeRegex.find(rawName)
        if (bracketMatch != null) {
            val code = bracketMatch.groupValues[1].uppercase()
            codeIcons[code]?.let { return it }
        }

        val lowerName = rawName.lowercase()
        for ((key, icon) in nameContainsIcons) {
            if (lowerName.contains(key)) return icon
        }

        return null
    }

    // ✅ كل صورة (علم/شعار) عندها هامش شفاف مختلف حوالين المحتوى الفعلي —
    // هالخريطة بتحدد مقدار "الزوم" المطلوب لكل صورة عشان تملأ الدائرة كاملة
    // بدل ما يبين هامش فاضي حواليها. القيم محسوبة فعليًا من أبعاد كل صورة.
    private val iconZoomMap = mapOf(
        R.drawable.flag_ar to 1.55f,
        R.drawable.flag_lt to 1.7f,
        R.drawable.flag_jp to 1.55f,
        R.drawable.logo_bein_sports to 1.75f,
        R.drawable.logo_world_cup to 1.2f
    )

    /** ✅ يرجع مقدار الزوم المخصص لصورة معيّنة، أو 1.4 كقيمة افتراضية معقولة
     * لأي صورة جديدة تنضاف بالمستقبل وما اتحسب زومها بعد */
    fun zoomFor(resId: Int): Float = iconZoomMap[resId] ?: 1.4f
}