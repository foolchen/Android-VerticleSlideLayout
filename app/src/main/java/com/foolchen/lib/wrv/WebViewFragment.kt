package com.foolchen.lib.wrv

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

class WebViewFragment : Fragment() {

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_web_view, container,
      false)!!

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
    wv.webChromeClient = ChromClient()

    wv.loadUrl("https://www.kotlincn.net/docs/reference/")
  }

  private inner class ViewClient : WebViewClient()

  private inner class ChromClient : WebChromeClient()
}