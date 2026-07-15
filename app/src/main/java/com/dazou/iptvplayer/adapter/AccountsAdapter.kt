package com.dazou.iptvplayer.adapter

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dazou.iptvplayer.model.XtreamServer

class AccountsAdapter(
    private val onAccountClick: (XtreamServer, Int) -> Unit
) : RecyclerView.Adapter<AccountsAdapter.ViewHolder>() {

    private var accounts = listOf<XtreamServer>()

    fun submitList(list: List<XtreamServer>) {
        accounts = list
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view as LinearLayout
        val tvName: TextView = container.getChildAt(0) as TextView
        val tvUrl: TextView = container.getChildAt(1) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
            setBackgroundColor(Color.parseColor("#1A1A35"))
            isFocusable = true
            isFocusableInTouchMode = true
            setOnFocusChangeListener { view, hasFocus ->
                view.setBackgroundColor(if (hasFocus) Color.parseColor("#2D2D5E") else Color.parseColor("#1A1A35"))
            }
        }
        val tvName = TextView(parent.context).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val tvUrl = TextView(parent.context).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#AAAAAA"))
            setPadding(0, 4, 0, 0)
        }
        layout.addView(tvName)
        layout.addView(tvUrl)
        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val account = accounts[position]
        holder.tvName.text = "👤 ${account.username}"
        holder.tvUrl.text = account.url
        holder.container.setOnClickListener { onAccountClick(account, position) }
    }

    override fun getItemCount() = accounts.size
}