package com.foolchen.lib.wrv

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.foolchen.lib.VerticalSlideLayout
import com.foolchen.lib.view.IVerticalPageListener
import com.foolchen.lib.view.VerticalSlideRecyclerView
import kotlinx.android.synthetic.main.fragment_recycler_view.*

class RecyclerViewFragment : Fragment(), IVerticalPageListener {
  private lateinit var mLayoutManager: LinearLayoutManager
  private var mRv: VerticalSlideRecyclerView? = null
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View {
    val view = inflater.inflate(R.layout.fragment_recycler_view, container,
        false)!!
    mRv = view.findViewById(R.id.rv)
    return view
  }


  override fun onResume() {
    super.onResume()
    if (activity is MainActivity) {
      mRv?.setIVerticalSlideController(
          (activity as MainActivity).findViewById<VerticalSlideLayout>(R.id.vsl))
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    mLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    val rv = view.findViewById<RecyclerView>(R.id.rv)
    rv.layoutManager = mLayoutManager
    rv.adapter = DemoAdapter()
  }

  override fun onPageUp(currentPage: Int, futurePage: Int) {
    rv.scrollToPosition(0)
  }

  override fun onPageDown(currentPage: Int, futurePage: Int) {
    // 此处可执行一些初始化及加载操作
  }

}