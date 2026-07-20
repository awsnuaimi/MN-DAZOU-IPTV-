package com.dazou.iptvplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.model.XtreamCategory
import com.dazou.iptvplayer.utils.FocusAnimator

class CategoryAdapter(
    private val categories: List<XtreamCategory>,
    private val nextFocusRightId: Int? = null,
    private val nextFocusLeftId: Int? = null,
    private val onCategoryClick: (XtreamCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: TextView = view.findViewById(R.id.tvCategoryIcon)
        val nameView: TextView = view.findViewById(R.id.tvCategoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)

        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.isClickable = true
        FocusAnimator.attach(view)

        nextFocusRightId?.let { view.nextFocusRightId = it }
        nextFocusLeftId?.let { view.nextFocusLeftId = it }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        val (icon, name) = splitIconAndName(category.categoryName)
        holder.iconView.text = icon
        holder.nameView.text = name
        holder.itemView.setOnClickListener { onCategoryClick(category) }
    }

    override fun getItemCount() = categories.size

    /**
     * يفصل رمز العلم/الأيقونة عن اسم المجلد نفسه، حتى يظهروا بمكانين منفصلين
     * بالتصميم الجديد بدل ما يكونوا ملزوقين بنفس السطر.
     * لو ما لقى رمز واضح بأول النص، يستخدم أيقونة مجلد افتراضية 📁.
     */
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