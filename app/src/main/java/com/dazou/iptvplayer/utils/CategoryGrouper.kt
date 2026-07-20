package com.dazou.iptvplayer.utils

import android.content.Context
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.model.XtreamCategory

data class GroupOverride(val flag: String?, val nameResId: Int, val suppressFlag: Boolean = false)

/** منطق تجميع المجموعات المتكررة بنفس رمز البلد [XX] بمجلد واحد بعلم — الأسماء هلق بتترجم حسب لغة التطبيق */
object CategoryGrouper {

    private val groupOverrides = mapOf(
        "AR" to GroupOverride(null, R.string.country_ar, suppressFlag = true),
        "ARABIC" to GroupOverride(null, R.string.country_ar, suppressFlag = true),
        "ALB" to GroupOverride("🇦🇱", R.string.country_albania),
        "LT" to GroupOverride(null, R.string.country_lithuania, suppressFlag = true),
        "JAPAN" to GroupOverride(null, R.string.country_japan, suppressFlag = true),
        "JP" to GroupOverride(null, R.string.country_japan, suppressFlag = true),
        "PL" to GroupOverride("🇵🇱", R.string.country_poland),
        "SA" to GroupOverride("🇸🇦", R.string.country_saudi),
        "AE" to GroupOverride("🇦🇪", R.string.country_uae),
        "UAE" to GroupOverride("🇦🇪", R.string.country_uae),
        "QA" to GroupOverride("🇶🇦", R.string.country_qatar),
        "KW" to GroupOverride("🇰🇼", R.string.country_kuwait),
        "BH" to GroupOverride("🇧🇭", R.string.country_bahrain),
        "OM" to GroupOverride("🇴🇲", R.string.country_oman),
        "EG" to GroupOverride("🇪🇬", R.string.country_egypt),
        "LB" to GroupOverride("🇱🇧", R.string.country_lebanon),
        "JO" to GroupOverride("🇯🇴", R.string.country_jordan),
        "SY" to GroupOverride("🇸🇾", R.string.country_syria),
        "IQ" to GroupOverride("🇮🇶", R.string.country_iraq),
        "MA" to GroupOverride("🇲🇦", R.string.country_morocco),
        "TN" to GroupOverride("🇹🇳", R.string.country_tunisia),
        "DZ" to GroupOverride("🇩🇿", R.string.country_algeria),
        "TR" to GroupOverride("🇹🇷", R.string.country_turkey),
        "US" to GroupOverride("🇺🇸", R.string.country_usa),
        "USA" to GroupOverride("🇺🇸", R.string.country_usa),
        "UK" to GroupOverride("🇬🇧", R.string.country_uk),
        "DE" to GroupOverride("🇩🇪", R.string.country_germany),
        "FR" to GroupOverride("🇫🇷", R.string.country_france),
        "IT" to GroupOverride("🇮🇹", R.string.country_italy),
        "ES" to GroupOverride("🇪🇸", R.string.country_spain),
        "NL" to GroupOverride("🇳🇱", R.string.country_netherlands),
        "RU" to GroupOverride("🇷🇺", R.string.country_russia),
        "IN" to GroupOverride("🇮🇳", R.string.country_india),
        "PK" to GroupOverride("🇵🇰", R.string.country_pakistan),
        "GR" to GroupOverride("🇬🇷", R.string.country_greece),
        "PT" to GroupOverride("🇵🇹", R.string.country_portugal)
    )

    fun countryCodeToFlagEmoji(code: String): String? {
        if (code.length != 2) return null
        val c1 = code[0].uppercaseChar()
        val c2 = code[1].uppercaseChar()
        if (c1 !in 'A'..'Z' || c2 !in 'A'..'Z') return null
        val base = 0x1F1E6 - 'A'.code
        return try {
            String(Character.toChars(base + c1.code)) + String(Character.toChars(base + c2.code))
        } catch (e: Exception) { null }
    }

    /**
     * يبني قائمة عرض من المجموعات الخام. لازم نمرر Context هلق حتى نقدر نترجم أسماء الدول
     * حسب لغة التطبيق الحالية بدل أسماء عربية ثابتة.
     */
    fun buildDisplayCategories(
        context: Context,
        categories: List<XtreamCategory>,
        categoryGroupMap: MutableMap<String, List<String>>,
        categoryGroupDetailsMap: MutableMap<String, List<XtreamCategory>>
    ): List<XtreamCategory> {
        val prefixRegex = Regex("^\\[([A-Za-z]{2,8})\\]")
        val groups = LinkedHashMap<String, MutableList<XtreamCategory>>()
        val ungrouped = mutableListOf<XtreamCategory>()

        categories.forEach { cat ->
            val match = prefixRegex.find(cat.categoryName.trim())
            if (match != null) {
                val code = match.groupValues[1].uppercase()
                groups.getOrPut(code) { mutableListOf() }.add(cat)
            } else {
                ungrouped.add(cat)
            }
        }

        categoryGroupMap.clear()
        categoryGroupDetailsMap.clear()
        val result = mutableListOf<XtreamCategory>()
        groups.forEach { (code, catsInGroup) ->
            if (catsInGroup.size >= 2) {
                val override = groupOverrides[code]
                val flag = when {
                    override != null && override.suppressFlag -> null
                    override != null -> override.flag
                    else -> countryCodeToFlagEmoji(code)
                }
                val name = override?.let { context.getString(it.nameResId) }
                val label = when {
                    flag != null && name != null -> "$flag $name"
                    flag != null -> "$flag $code"
                    name != null -> "📁 $name"
                    else -> "📁 $code"
                }
                result.add(XtreamCategory("GROUP:$code", label, 0))
                categoryGroupMap[code] = catsInGroup.map { it.categoryId }
                categoryGroupDetailsMap[code] = catsInGroup
            } else {
                result.addAll(catsInGroup)
            }
        }
        result.addAll(ungrouped)
        return result
    }

    fun stripCountryPrefix(categoryName: String): String {
        val regex = Regex("^\\[([A-Za-z]{2,8})\\]\\s*")
        val cleaned = regex.replace(categoryName.trim(), "").trim()
        return cleaned.ifEmpty { categoryName }
    }
}