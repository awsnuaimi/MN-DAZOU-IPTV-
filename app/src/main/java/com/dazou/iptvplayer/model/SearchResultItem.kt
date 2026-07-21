package com.dazou.iptvplayer.model

/**
 * ✅ يمثّل عنصر نتيجة بحث موحّد، يقدر يكون قناة مباشرة أو فيلم أو مسلسل —
 * بيسمح لنا نعرض ونفلتر الثلاث أنواع مع بعض بقائمة نتائج واحدة.
 */
sealed class SearchResultItem {
    data class Channel(val channel: XtreamChannel) : SearchResultItem()
    data class Movie(val movie: XtreamMovie) : SearchResultItem()
    data class Series(val series: XtreamSeries) : SearchResultItem()
}