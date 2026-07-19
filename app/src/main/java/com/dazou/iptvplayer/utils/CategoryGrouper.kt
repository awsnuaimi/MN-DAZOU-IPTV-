package com.dazou.iptvplayer.utils

import com.dazou.iptvplayer.model.XtreamCategory

data class GroupOverride(val flag: String?, val name: String, val suppressFlag: Boolean = false)

/** منطق تجميع المجموعات المتكررة بنفس رمز البلد [XX] بمجلد واحد بعلم — منطق بيانات صرف، بلا أي علاقة بالواجهة */
object CategoryGrouper {

    private val groupOverrides = mapOf(
        "AR" to GroupOverride(null, "عربي", suppressFlag = true),
        "ARABIC" to GroupOverride(null, "عربي", suppressFlag = true),
        "ALB" to GroupOverride("🇦🇱", "ألبانيا"),
        "JAPAN" to GroupOverride("🇯🇵", "اليابان"),
        "JP" to GroupOverride("🇯🇵", "اليابان"),
        "PL" to GroupOverride("🇵🇱", "بولندا"),
        "SA" to GroupOverride("🇸🇦", "السعودية"),
        "AE" to GroupOverride("🇦🇪", "الإمارات"),
        "UAE" to GroupOverride("🇦🇪", "الإمارات"),
        "QA" to GroupOverride("🇶🇦", "قطر"),
        "KW" to GroupOverride("🇰🇼", "الكويت"),
        "BH" to GroupOverride("🇧🇭", "البحرين"),
        "OM" to GroupOverride("🇴🇲", "عُمان"),
        "EG" to GroupOverride("🇪🇬", "مصر"),
        "LB" to GroupOverride("🇱🇧", "لبنان"),
        "JO" to GroupOverride("🇯🇴", "الأردن"),
        "SY" to GroupOverride("🇸🇾", "سوريا"),
        "IQ" to GroupOverride("🇮🇶", "العراق"),
        "MA" to GroupOverride("🇲🇦", "المغرب"),
        "TN" to GroupOverride("🇹🇳", "تونس"),
        "DZ" to GroupOverride("🇩🇿", "الجزائر"),
        "TR" to GroupOverride("🇹🇷", "تركيا"),
        "US" to GroupOverride("🇺🇸", "أمريكا"),
        "USA" to GroupOverride("🇺🇸", "أمريكا"),
        "UK" to GroupOverride("🇬🇧", "بريطانيا"),
        "DE" to GroupOverride("🇩🇪", "ألمانيا"),
        "FR" to GroupOverride("🇫🇷", "فرنسا"),
        "IT" to GroupOverride("🇮🇹", "إيطاليا"),
        "ES" to GroupOverride("🇪🇸", "إسبانيا"),
        "NL" to GroupOverride("🇳🇱", "هولندا"),
        "RU" to GroupOverride("🇷🇺", "روسيا"),
        "IN" to GroupOverride("🇮🇳", "الهند"),
        "PK" to GroupOverride("🇵🇰", "باكستان"),
        "GR" to GroupOverride("🇬🇷", "اليونان"),
        "PT" to GroupOverride("🇵🇹", "البرتغال")
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
     * يبني قائمة عرض من المجموعات الخام. يملأ categoryGroupMap بخرائط
     * (رمز البلد → قائمة معرّفات المجموعات الأصلية) لاستخدامها لاحقًا بالتحميل المدمج.
     */
    fun buildDisplayCategories(
        categories: List<XtreamCategory>,
        categoryGroupMap: MutableMap<String, List<String>>
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
        val result = mutableListOf<XtreamCategory>()
        groups.forEach { (code, catsInGroup) ->
            if (catsInGroup.size >= 2) {
                val override = groupOverrides[code]
                val flag = when {
                    override != null && override.suppressFlag -> null
                    override != null -> override.flag
                    else -> countryCodeToFlagEmoji(code)
                }
                val name = override?.name
                val label = when {
                    flag != null && name != null -> "$flag $name"
                    flag != null -> "$flag $code"
                    name != null -> "📁 $name"
                    else -> "📁 $code"
                }
                result.add(XtreamCategory("GROUP:$code", label, 0))
                categoryGroupMap[code] = catsInGroup.map { it.categoryId }
            } else {
                result.addAll(catsInGroup)
            }
        }
        result.addAll(ungrouped)
        return result
    }
}