package com.dazou.iptvplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
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
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)

        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.isClickable = true
        view.setBackgroundResource(R.drawable.tv_button_selector)
        FocusAnimator.attach(view)

        // ✅ توجيه فوكس صريح لكل صف (بدل الاعتماد على البحث التلقائي اللي بيتوه بالـ RTL)
        nextFocusRightId?.let { view.nextFocusRightId = it }
        nextFocusLeftId?.let { view.nextFocusLeftId = it }

        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.setTextColor(ContextCompat.getColor(parent.context, R.color.text_white))
        textView.setPadding(24, 24, 24, 24)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.textView.text = "📁 ${category.categoryName}"
        holder.textView.textSize = 17f
        holder.itemView.setOnClickListener { onCategoryClick(category) }
    }

    override fun getItemCount() = categories.size
}