package com.dazou.iptvplayer.utils

import android.view.View

/**
 * يضيف تأثير تكبير بسيط وسلس عند وقوع الفوكس على أي عنصر بالريموت،
 * يخلي العين تلاحق مكان التركيز بسهولة أكبر بقوائم التلفاز.
 */
object FocusAnimator {
    fun attach(view: View, scale: Float = 1.08f, durationMs: Long = 150L) {
        view.setOnFocusChangeListener { v, hasFocus ->
            v.animate()
                .scaleX(if (hasFocus) scale else 1f)
                .scaleY(if (hasFocus) scale else 1f)
                .setDuration(durationMs)
                .start()
        }
    }
}