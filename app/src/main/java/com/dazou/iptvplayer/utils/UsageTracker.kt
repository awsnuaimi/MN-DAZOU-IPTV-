package com.dazou.iptvplayer.utils

import android.content.Context

/**
 * ✅ "الذكاء البسيط" — تتبّع محلي بالكامل لعدد مرات مشاهدة كل قناة/محتوى،
 * بدون أي اتصال بالإنترنت أو سيرفر خارجي. البيانات كلها محفوظة على الجهاز
 * نفسه بس (SharedPreferences)، ومالها أي علاقة بذكاء اصطناعي حقيقي —
 * هي حسابات بسيطة (عدّاد + ترتيب) بس مفيدة عمليًا.
 *
 * تُستخدم من قبل:
 * - قسم "الأكثر مشاهدة" بالشاشة الرئيسية
 * - اقتراح إضافة للمفضلة بعد عدد معيّن من المشاهدات
 */
object UsageTracker {
    private const val PREFS_NAME = "dazou_usage_prefs"
    private const val SUGGESTION_THRESHOLD = 5 // بعد كم مشاهدة نقترح إضافة للمفضلة

    /** يسجّل مشاهدة جديدة لعنصر معيّن (قناة، فيلم، مسلسل...) بالنوع المحدد */
    fun recordView(context: Context, type: String, id: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "views_${type}_$id"
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }

    /** يرجع عدد مرات مشاهدة عنصر معيّن */
    fun getViewCount(context: Context, type: String, id: Int): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("views_${type}_$id", 0)
    }

    /** ✅ يرتّب أي قائمة IDs حسب عدد المشاهدات تنازليًا (الأكتر مشاهدة أول) */
    fun <T> sortByMostWatched(
        context: Context,
        items: List<T>,
        type: String,
        idOf: (T) -> Int
    ): List<T> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return items.sortedByDescending { prefs.getInt("views_${type}_${idOf(it)}", 0) }
    }

    /** ✅ بيرجع true بس أول مرة توصل فيها المشاهدات لعتبة معيّنة (مش كل مرة) —
     * مفيد لعرض اقتراح "ضيفها للمفضلة" مرة وحدة بس، مش إزعاج متكرر */
    fun shouldSuggestFavorite(context: Context, type: String, id: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt("views_${type}_$id", 0)
        val alreadySuggested = prefs.getBoolean("suggested_${type}_$id", false)
        if (count == SUGGESTION_THRESHOLD && !alreadySuggested) {
            prefs.edit().putBoolean("suggested_${type}_$id", true).apply()
            return true
        }
        return false
    }
}
