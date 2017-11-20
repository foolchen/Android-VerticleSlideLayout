package com.foolchen.lib.wrv

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.item_demo.view.*

class DemoAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  private val mArray = arrayListOf("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9",
      "A10", "B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8", "B9", "B10", "C1", "C2", "C3", "C4",
      "C5", "C6", "C7", "C8", "C9", "C10")

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    return DemoHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_demo, parent, false))
  }

  override fun getItemCount(): Int = mArray.size

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    if (holder is WebViewHolder) {
      // 此处不处理
    } else if (holder is DemoHolder) {
      holder.mTextView.text = mArray[position]
    }
  }

}

class DemoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  var mTextView: TextView = itemView.tv
}

class WebViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)