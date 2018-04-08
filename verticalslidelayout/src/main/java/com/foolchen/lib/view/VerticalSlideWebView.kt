package com.foolchen.lib.view

import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.webkit.*
import com.foolchen.lib.IVerticalSlideController
import com.foolchen.lib.IVerticalSlideControllerHelper
import com.foolchen.lib.VerticalSlideLayout

/**
 * 与[VerticalSlideLayout]组合使用，实现WebView与其他View翻页的效果
 * 该View能够根据是否到达了顶部/底部，决定是否将事件交由父布局处理
 * @author chenchong
 * 2017/11/16
 * 下午2:10
 */
open class VerticalSlideWebView : WebView, IVerticalSlideView, IVerticalSlideControllerHelper {
  val TAG = "VerticalSlideWebView"
  private val HORIZONTAL_SLOP = 60
  private var mDownX = 0F
  private var mDownY = 0F
  private var mScale: Float = scale
  private var mWebViewClientWrapper: WebViewClientWrapper? = null
  private var mIVerticalSlideController: IVerticalSlideController? = null

  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs,
      defStyleAttr)

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
      context, attrs, defStyleAttr, defStyleRes)

  override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
    with(ev)
    {
      val ac = action and MotionEvent.ACTION_MASK

      when (ac) {
        MotionEvent.ACTION_DOWN -> {
          mDownX = x
          mDownY = y

          // 在触发ACTION_DOWN时还无法得知WebView是否滑动到了顶部/底部，此时禁止父布局处理触摸事件
          // 防止错误的进行了翻页
          parent.requestDisallowInterceptTouchEvent(true)
        }
        MotionEvent.ACTION_MOVE -> {
          val allowParentTouchEvent: Boolean
          val dx = x - mDownX
          val dy = y - mDownY

          // 在Y轴方向的位移>在X轴方向的位移时，认为发生了垂直方向的拖动
          if (Math.abs(dy) <= Math.abs(dx) && Math.abs(dx) > HORIZONTAL_SLOP) {
            // Y轴方向位移<X轴方向位移时，则允许父布局获取事件，防止与手势右划返回冲突
            mIVerticalSlideController?.controlSlideEnable(false)
            parent.requestDisallowInterceptTouchEvent(false)
          } else {
            allowParentTouchEvent = if (dy > 0) {
              // 当前坐标>上次坐标，为向下拖动View
              // 此时如果已经位于顶部，则当前View不消费事件，交给父布局
              checkIsTop()
            } else if (dy < 0) {
              // 当前坐标<上次坐标，为向上拖动
              // 此时如果已经位于底部，则当前View不消费事件，交给父布局
              checkIsBottom()
            } else {
              false
            }
            mIVerticalSlideController?.controlSlideEnable(true)
            parent.requestDisallowInterceptTouchEvent(!allowParentTouchEvent)
          }
        }
      }

    }

    return super.dispatchTouchEvent(ev)
  }

  /**
   * 判断内容高度是否小于WebView的高度
   * 如果返回值为true，则证明内容高度小于View的高度，此时内容被限制在了View内，内部不可滑动，故不处理滑动手势
   */
  fun isContentRestricted(): Boolean = computeVerticalScrollRange() <= height

  override fun checkCanDrag(direction: Int): Boolean {
    // 在内容高度被限制在了WebView中时，则可以向上下拖动
    if (isContentRestricted()) {
      return true
    }

    if (direction == DRAG_UP) {
      // 检查是否能够向上拖动整个View，此时如果内容不能向上滑动，则返回true
      return checkIsBottom()
    } else if (direction == DRAG_DOWN) {
      // 检查是否能够向下拖动整个View，此时如果内容不能向下拖动，则返回true
      return checkIsTop()
    }

    throw IllegalArgumentException("Incorrect direction $direction")
  }

  override fun checkIsTop(): Boolean = this.scrollY <= 0

  /**
   * 由于部分机型对于WebView的内容高度计算不准确，故此处做两次判断，防止判断在部分机型上失效
   */
  override fun checkIsBottom(): Boolean = this.scrollY + measuredHeight >= getTotalHeight() || this.scrollY + measuredHeight >= getTotalHeight() - 5

  override fun setWebViewClient(client: WebViewClient?) {
    if (client == null) {
      super.setWebViewClient(client)
    } else {
      if (mWebViewClientWrapper == null) {// 如果原先未设置WebViewClient，则此处创建新的包装类并设置
        mWebViewClientWrapper = WebViewClientWrapper(client)
      } else {
        if (mWebViewClientWrapper?.client !== client) {// 如果设置的WebViewClient没有改变，则不需要创建新的包装类
          mWebViewClientWrapper = WebViewClientWrapper(client)
        }
      }
      super.setWebViewClient(mWebViewClientWrapper)
    }
  }

  fun getTotalHeight(): Int = Math.floor((contentHeight * mScale).toDouble()).toInt()

  override fun setIVerticalSlideController(controller: IVerticalSlideController?) {
    mIVerticalSlideController = controller
  }

  /**
   * [WebViewClient]的装饰类，用于包装直接设置进来的[WebViewClient]
   * 由于[getScale]方法已失效，目前该类的主要作用是用于获取新的[mScale]值，具体参考[WebViewClientWrapper.onScaleChanged]方法
   */
  private inner class WebViewClientWrapper(val client: WebViewClient?) : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
      client?.onPageFinished(view, url)
    }

    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? =
        client?.shouldInterceptRequest(view, url)

    override fun shouldInterceptRequest(view: WebView?,
        request: WebResourceRequest?): WebResourceResponse? =
        client?.shouldInterceptRequest(view, request)

    override fun shouldOverrideKeyEvent(view: WebView?, event: KeyEvent?): Boolean =
        client?.shouldOverrideKeyEvent(view, event) == true

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
      client?.doUpdateVisitedHistory(view, url, isReload)
    }

    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?,
        failingUrl: String?) {
      client?.onReceivedError(view, errorCode, description, failingUrl)
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?,
        error: WebResourceError?) {
      client?.onReceivedError(view, request, error)
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean =
        client?.onRenderProcessGone(view, detail) == true

    override fun onReceivedLoginRequest(view: WebView?, realm: String?, account: String?,
        args: String?) {
      client?.onReceivedLoginRequest(view, realm, account, args)
    }

    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?,
        errorResponse: WebResourceResponse?) {
      client?.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
      client?.onPageStarted(view, url, favicon)
    }

    override fun onScaleChanged(view: WebView?, oldScale: Float, newScale: Float) {
      client?.onScaleChanged(view, oldScale, newScale)
      mScale = newScale
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean =
        client?.shouldOverrideUrlLoading(view, url)!!

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
        client?.shouldOverrideUrlLoading(view, request)!!

    override fun onPageCommitVisible(view: WebView?, url: String?) {
      client?.onPageCommitVisible(view, url)
    }

    override fun onUnhandledKeyEvent(view: WebView?, event: KeyEvent?) {
      client?.onUnhandledKeyEvent(view, event)
    }

    override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
      client?.onReceivedClientCertRequest(view, request)
    }

    override fun onReceivedHttpAuthRequest(view: WebView?, handler: HttpAuthHandler?, host: String?,
        realm: String?) {
      client?.onReceivedHttpAuthRequest(view, handler, host, realm)
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
      client?.onReceivedSslError(view, handler, error)
    }

    override fun onTooManyRedirects(view: WebView?, cancelMsg: Message?, continueMsg: Message?) {
      client?.onTooManyRedirects(view, cancelMsg, continueMsg)
    }

    override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
      client?.onFormResubmission(view, dontResend, resend)
    }

    override fun onLoadResource(view: WebView?, url: String?) {
      client?.onLoadResource(view, url)
    }
  }
}