package com.dazou.iptvplayer.utils

import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.model.XtreamCategory

/** ✅ نوع الأيقونة: إما صورة محلية بالمشروع (شعارات زي Bein/كأس العالم)،
 * أو رابط علم رسمي مسطّح مستطيل من خدمة Flagpedia (مجانية، Public Domain) —
 * بدل الأعلام "المرفرفة" المحلية اللي كانت هوامشها غير موحّدة ومموّجة. */
sealed class CategoryIcon {
    data class Local(val resId: Int) : CategoryIcon()
    data class Remote(val url: String) : CategoryIcon()
}

/**
 * يربط مجلدات معيّنة (دول أو قنوات) بصور حقيقية (أعلام/لوغوهات) بدل رموز الأعلام النصية (Emoji).
 */
object CategoryIconMapper {

    // ✅ يربط كود المجموعة الداخلي (المستخدم بالتطبيق لتجميع الدول) برمز
    // الدولة الرسمي (ISO 3166-1 alpha-2) المستخدم من خدمة Flagpedia — عشان
    // نجيب علم مسطّح رسمي حقيقي بدل الصور المحلية القديمة المموّجة
    private val groupCodeToIsoCode = mapOf(
        "ALB" to "al",
        "LT" to "lt",
        "JAPAN" to "jp",
        "JP" to "jp",
        "PL" to "pl",
        "SA" to "sa",
        "KSA" to "sa",
        "SAUDI" to "sa",
        "AE" to "ae",
        "UAE" to "ae",
        "QA" to "qa",
        "KW" to "kw",
        "BH" to "bh",
        "OM" to "om",
        "EG" to "eg",
        "LB" to "lb",
        "JO" to "jo",
        "SY" to "sy",
        "IQ" to "iq",
        "MA" to "ma",
        "TN" to "tn",
        "DZ" to "dz",
        "TR" to "tr",
        "US" to "us",
        "USA" to "us",
        "UK" to "gb",
        "DE" to "de",
        "FR" to "fr",
        "IT" to "it",
        "ES" to "es",
        "NL" to "nl",
        "RU" to "ru",
        "IN" to "in",
        "PK" to "pk",
        "GR" to "gr",
        "PT" to "pt"
    )

    private fun flagUrl(isoCode: String) = "https://flagpedia.net/data/flags/w80/$isoCode.png"

    // مجلدات حقيقية (شعارات مش أعلام) — تضل صور محلية بالمشروع، مطابقة
    // "يحتوي على" بدل "يطابق بالضبط"، عشان تتحمّل أي بادئة أو رموز أو مسافات
    // زيادة بالاسم الأصلي بالسيرفر
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

    fun iconFor(category: XtreamCategory): CategoryIcon? {
        if (category.categoryId.startsWith("GROUP:")) {
            val code = category.categoryId.removePrefix("GROUP:")
            groupCodeToIsoCode[code]?.let { return CategoryIcon.Remote(flagUrl(it)) }
            return null
        }

        val rawName = category.categoryName.trim()

        // ✅ يتحقق من رمز الدولة حتى لو المجلد لسا يحمل بادئة [XX] الأصلية
        // (يعني ما انضم لمجموعة لأنه كان الوحيد بهيك رمز بالسيرفر)
        val bracketMatch = bracketCodeRegex.find(rawName)
        if (bracketMatch != null) {
            val code = bracketMatch.groupValues[1].uppercase()
            groupCodeToIsoCode[code]?.let { return CategoryIcon.Remote(flagUrl(it)) }
        }

        val lowerName = rawName.lowercase()
        for ((key, icon) in nameContainsIcons) {
            if (lowerName.contains(key)) return CategoryIcon.Local(icon)
        }

        return null
    }
}
