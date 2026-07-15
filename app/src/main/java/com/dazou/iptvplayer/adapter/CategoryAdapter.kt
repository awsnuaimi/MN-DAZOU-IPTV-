package com.dazou.iptvplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dazou.iptvplayer.model.XtreamCategory

class CategoryAdapter(
    private val categories: List<XtreamCategory>,
    private val onCategoryClick: (XtreamCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)

        view.isFocusable = true
        view.isClickable = true
        view.setBackgroundResource(android.R.drawable.list_selector_background)

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