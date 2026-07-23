package com.dazou.iptvplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.model.XtreamCategory
import com.dazou.iptvplayer.utils.CategoryIcon
import com.dazou.iptvplayer.utils.CategoryIconMapper
import com.dazou.iptvplayer.utils.FlagZoomCalculator
import com.dazou.iptvplayer.utils.FocusAnimator
import com.dazou.iptvplayer.utils.ThemeManager

class CategoryAdapter(
    private val categories: List<XtreamCategory>,
    private val nextFocusRightId: Int? = null,
    // ✅ بدل ما نشاور على حاوية القائمة كلها (وأندرويد يخمّن مين ياخد الفوكس)،
    // بنستدعي هالدالة لما يوصل المستخدم لأول عنصر ويضغط يمين — والطرف التاني
    // (MainActivity) هو يلي بيقرر بالضبط مين ياخد الفوكس (نفس المكان يلي جاي منه)
    private val onRequestFocusLeft: (() -> Unit)? = null,
    // ✅ بيبلّغ MainActivity أي عنصر بالظبط عليه الفوكس هلق — عشان نقدر نرجعله
    // بدقة لما المستخدم يرجع من قائمة تانية بعدين
    private val onItemFocused: ((Int) -> Unit)? = null,
    private val onCategoryClick: (XtreamCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: TextView = view.findViewById(R.id.tvCategoryIcon)
        val imageIconView: ImageView = view.findViewById(R.id.ivCategoryIcon)
        val nameView: TextView = view.findViewById(R.id.tvCategoryName)
        val borderOverlay: View = view.findViewById(R.id.vCategoryIconBorder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)

        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.isClickable = true

        // ✅ لون التيم المختار حاليًا بدل لون ثابت
        ThemeManager.applyCardFocusBackground(view)

        nextFocusRightId?.let { view.nextFocusRightId = it }

        val holder = ViewHolder(view)

        FocusAnimator.attach(view) { hasFocus ->
            if (hasFocus) onItemFocused?.invoke(holder.bindingAdapterPosition)
        }

        if (onRequestFocusLeft != null) {
            view.setOnKeyListener { _, keyCode, event ->
                if (event.action == android.view.KeyEvent.ACTION_DOWN &&
                    keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT
                ) {
                    onRequestFocusLeft.invoke()
                    true
                } else false
            }
        }

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        val (icon, name) = splitIconAndName(category.categoryName)

        // ✅ نلوّن الإطار المرسوم فوق الصورة بلون التيم المختار حاليًا —
        // طبقة منفصلة عن الصورة نفسها، فمضمون الظهور دايمًا بغض النظر عن
        // مصدر الصورة (رابط أو محلية) أو أي تعقيد بمستوى الـView clipping
        val theme = ThemeManager.getSavedTheme(holder.itemView.context)
        val strokeWidthPx = (1.5f * holder.itemView.resources.displayMetrics.density).toInt()
        (holder.borderOverlay.background?.mutate() as? android.graphics.drawable.GradientDrawable)
            ?.setStroke(strokeWidthPx, theme.accent)

        // ✅ نصف قطر التدوير بالبكسل — نفس القيمة المستخدمة بشكل الإطار (7dp)
        val cornerRadiusPx = (7f * holder.itemView.resources.displayMetrics.density).toInt()

        val customIcon = CategoryIconMapper.iconFor(category)
        when (customIcon) {
            is CategoryIcon.Remote -> {
                // ✅ علم رسمي مسطّح مستطيل من Flagpedia — بندوّر زواياه فعليًا
                // على مستوى البكسل عبر Glide (مضمون الظهور بأي جهاز). نظيف
                // بدون هامش شفاف داخلي، فما في داعي لأي زوم إضافي هون
                holder.imageIconView.scaleX = 1f
                holder.imageIconView.scaleY = 1f
                Glide.with(holder.imageIconView.context)
                    .load(customIcon.url)
                    .transform(CenterCrop(), RoundedCorners(cornerRadiusPx))
                    .into(holder.imageIconView)
                holder.imageIconView.visibility = View.VISIBLE
                holder.iconView.visibility = View.GONE
            }
            is CategoryIcon.Local -> {
                // ✅ شعارات محلية (Bein/كأس العالم/الدول العربية) فيها هامش
                // شفاف داخلي، فلازم نكبّرها بمقدار محسوب تلقائيًا زي قبل،
                // مع نفس أسلوب Glide لتدوير الزوايا بشكل مضمون
                val zoom = FlagZoomCalculator.zoomFor(holder.imageIconView.context, customIcon.resId)
                holder.imageIconView.scaleX = zoom
                holder.imageIconView.scaleY = zoom
                Glide.with(holder.imageIconView.context)
                    .load(customIcon.resId)
                    .transform(CenterCrop(), RoundedCorners(cornerRadiusPx))
                    .into(holder.imageIconView)
                holder.imageIconView.visibility = View.VISIBLE
                holder.iconView.visibility = View.GONE
            }
            null -> {
                holder.iconView.text = icon
                holder.iconView.visibility = View.VISIBLE
                holder.imageIconView.visibility = View.GONE
            }
        }

        holder.nameView.text = name
        holder.itemView.setOnClickListener { onCategoryClick(category) }
    }

    override fun getItemCount() = categories.size

    private fun splitIconAndName(raw: String): Pair<String, String> {
        val trimmed = raw.trim()
        val firstSpace = trimmed.indexOf(' ')
        if (firstSpace in 1..4) {
            val possibleIcon = trimmed.substring(0, firstSpace).trim()
            val rest = trimmed.substring(firstSpace + 1).trim()
            if (possibleIcon.isNotBlank() && rest.isNotBlank()) {
                return possibleIcon to rest
            }
        }
        return "📁" to trimmed
    }
}
