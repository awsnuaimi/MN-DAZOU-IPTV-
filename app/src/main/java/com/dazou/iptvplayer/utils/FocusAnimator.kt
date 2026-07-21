package com.dazou.iptvplayer.utils

import android.view.View

/**
 * يضيف تأثير تكبير بسيط وسلس عند وقوع الفوكس على أي عنصر بالريموت،
 * يخلي العين تلاحق مكان التركيز بسهولة أكبر بقوائم التلفاز.
 *
 * ✅ بيلغي أي أنيميشن سابق (cancel) قبل ما يبلش الجديد — يمنع تراكم الحركات
 * وتقطيعها لما الفوكس يتنقل بسرعة بين عناصر كتير بقائمة طويلة.
 */
object FocusAnimator {
    fun attach(
        view: View,
        scale: Float = 1.05f,
        durationMs: Long = 120L,
        onFocusChanged: ((Boolean) -> Unit)? = null
    ) {
        view.setOnFocusChangeListener { v, hasFocus ->
            v.animate().cancel()
            v.animate()
                .scaleX(if (hasFocus) scale else 1f)
                .scaleY(if (hasFocus) scale else 1f)
                .setDuration(durationMs)
                .start()
            onFocusChanged?.invoke(hasFocus)
        }
    }
}