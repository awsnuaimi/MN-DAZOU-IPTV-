package com.dazou.iptvplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * ✅ يحسب مقدار "الزوم" اللازم لأي صورة (علم/شعار) تلقائيًا وقت التشغيل، عن
 * طريق فحص الصورة نفسها ولقاء حدود المحتوى الحقيقي (بعيدًا عن أي هامش شفاف
 * حواليه)، بدل الاعتماد على قيم محسوبة يدويًا لكل صورة لحالها (وسهل ننسى
 * وحدة زي ما صار). بيشتغل تلقائيًا مع أي علم جديد ينضاف بالمستقبل كمان.
 * النتيجة بتتخزّن مؤقتًا (Cache) عشان ما نكرر فحص نفس الصورة أكتر من مرة.
 */
object FlagZoomCalculator {

    private val cache = mutableMapOf<Int, Float>()

    fun zoomFor(context: Context, resId: Int): Float {
        cache[resId]?.let { return it }

        val zoom = try {
            val bitmap = BitmapFactory.decodeResource(context.resources, resId)
            if (bitmap != null) computeZoom(bitmap) else 1.4f
        } catch (e: Exception) {
            1.4f // احتياط لو صار خطأ غير متوقع بفك الصورة
        }

        cache[resId] = zoom
        return zoom
    }

    private fun computeZoom(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        if (width == 0 || height == 0) return 1.4f

        var minX = width
        var minY = height
        var maxX = 0
        var maxY = 0
        var foundContent = false

        // ✅ نفحص الصورة بخطوات (Step) بدل بكسل بكسل — أسرع بكتير وكافي تمامًا
        // لتحديد حدود المحتوى بدقة كافية لأيقونة صغيرة
        val step = maxOf(1, minOf(width, height) / 100)
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val alpha = (bitmap.getPixel(x, y) ushr 24) and 0xFF
                if (alpha > 15) {
                    foundContent = true
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (!foundContent) return 1.4f

        val contentWidthRatio = (maxX - minX + 1).toFloat() / width.toFloat()
        val contentHeightRatio = (maxY - minY + 1).toFloat() / height.toFloat()
        val limitingRatio = minOf(contentWidthRatio, contentHeightRatio).coerceAtLeast(0.3f)

        // ✅ هامش أمان بسيط (5%) عشان ما نقص لصق حواف العلم بالظبط عند الحد
        return (1f / limitingRatio) * 1.05f
    }
}
