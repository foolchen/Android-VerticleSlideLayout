package com.foolchen.lib.wrv

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.foolchen.lib.VerticalSlideLayout
import com.foolchen.lib.view.IVerticalPageListener
import com.foolchen.lib.view.VerticalSlideWebView
import kotlinx.android.synthetic.main.fragment_web_view.*

class WebViewFragment : Fragment(), IVerticalPageListener {

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View {
    val view = inflater.inflate(R.layout.fragment_web_view, container,
        false)!!
    mWv = view.findViewById(R.id.wv)
    return view
  }

  private var mWv: VerticalSlideWebView? = null


  override fun onResume() {
    super.onResume()
    if (activity is MainActivity) {
      mWv?.setIVerticalSlideController(
          (activity as MainActivity).findViewById<VerticalSlideLayout>(R.id.vsl))
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val wv = view.findViewById<WebView>(R.id.wv)
    val settings = wv.settings
    settings.setSupportZoom(false)
    settings.textZoom = 100// 默认文字缩放比为100%(该属性仅api14及以上可使用)
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.domStorageEnabled = true
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    }

    wv.webViewClient = ViewClient()
    wv.webChromeClient = ChromeClient()

    wv.loadUrl("https://www.kotlincn.net/docs/reference/")
  }

  override fun onPageUp(currentPage: Int, futurePage: Int) {
    if (currentPage == 0) {
      wv.scrollTo(0, 0)
    } else if (currentPage == 1) {
      wv.scrollTo(0, wv.getTotalHeight())
    }
  }

  override fun onPageDown(currentPage: Int, futurePage: Int) {
    // 由于该页面本身是需要滑动到底部才会执行onPageDown的，故此处不需要执行操作
    //wv.scrollTo(0, wv.getTotalHeight())
  }

  private inner class ViewClient : WebViewClient()

  private inner class ChromeClient : WebChromeClient()
}