package com.foolchen.lib

import android.content.Context
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.ViewCompat
import android.support.v4.widget.ViewDragHelper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.foolchen.lib.view.IVerticalPageListener
import com.foolchen.lib.view.VerticalSlideRecyclerView
import com.foolchen.lib.view.VerticalSlideWebView

/**
 * 该布局可以添加两个子View，并且实现了两个View之间的翻页
 * 两个View都可以是可滑动的布局
 *
 * 目前支持[VerticalSlideWebView]、[VerticalSlideRecyclerView]
 *
 * 注意：两个View都要需要填充该布局
 *
 * @author chenchong
 * 2017/11/17
 * 上午9:42
 */
class VerticalSlideLayout : ViewGroup {

  private val VEL_THRESHOLD = 6000 // 滑动速度的阈值，在手指离开屏幕时，如果Y轴的滑动速度超过该值，则认定拖动事件有效
  private val DISTANCE_THRESHOLD: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
      60F,
      resources.displayMetrics) // 当滑动速度为达到阈值VEL_THRESHOLD时，则通过该值（最小的位移距离）来判断是否将View恢复到边缘位置

  private val mDragHelper: ViewDragHelper
  private val mGestureDetector: GestureDetectorCompat

  private lateinit var mViewTop: View
  private lateinit var mViewBottom: View

  private var mViewHeight = 0
  private var mPage = 0
  private var mAnimationFinished = true// 用于标识View的移动动画是否已结束

  private var mIVerticalPageListener: IVerticalPageListener? = null


  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs,
      defStyleAttr)

  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
      context, attrs, defStyleAttr, defStyleRes)

  init {
    mDragHelper = ViewDragHelper.create(this, 10F, DragCallBack())
    mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_BOTTOM)
    mGestureDetector = GestureDetectorCompat(context, YScrollDetector())
    mPage = 0
  }

  override fun onFinishInflate() {
    super.onFinishInflate()

    if (childCount < 2) {
      throw IllegalArgumentException(
          "This layout should has two children and there is $childCount currently")
    }
    mViewTop = getChildAt(0)
    mViewBottom = getChildAt(1)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    measureChildren(widthMeasureSpec, heightMeasureSpec)// 此处需要测量子View，以便在其他位置使用时能够获取到准确的高度
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    if (mViewTop.top == 0) {
      // 顶部的View的上边缘位置y=0，此时顶部的View在第一页
      // 设置View的位置为默认位置
      mViewTop.layout(0, 0, r, b)
      mViewBottom.layout(0, 0, r, b)
      // 设置底部View与顶部View的相对位置
      mViewHeight = mViewTop.measuredHeight
      mViewBottom.offsetTopAndBottom(
          mViewHeight)// 将底部View的上边缘和下边缘都偏移顶部View的高度值，此时底部View的位置恰好位于顶部View底边缘之下
    } else {
      // 底部的View在第一页（此时顶部的View在-height的位置）
      // 此时的位置由拖动后释放事件时确定，直接指定对应的位置即可，不需要再进行手动计算
      with(mViewTop) {
        layout(left, top, right, bottom)
      }
      with(mViewBottom)
      {
        layout(left, top, right, bottom)
      }
    }
  }

  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    if (!mAnimationFinished) {
      // 在View的移动动画未结束时直接阻断事件
      return true
    }

    var shouldIntercept = false
    val yScroll = mGestureDetector.onTouchEvent(ev)
    try {
      shouldIntercept = mDragHelper.shouldInterceptTouchEvent(ev)
    } catch (e: Exception) {
      // 在使用DragHelper处理触摸事件时，可能会出现异常，此处屏蔽该异常，防止崩溃
    }
    return shouldIntercept && yScroll
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (!mAnimationFinished) {
      // 在View的移动动画未结束时直接阻断事件
      return true
    }

    try {
      mDragHelper.processTouchEvent(event)
    } catch (e: Exception) {
    }

    return true
  }

  private inner class DragCallBack : ViewDragHelper.Callback() {

    override fun tryCaptureView(child: View,
        pointerId: Int): Boolean =// 两个子View的触摸事件都要进行处理，故此处直接返回true
        true

    override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
      // 此处根据一个View位置的改变，动态改变另一个View的相对位置
      if (changedView == mViewTop) {
        mViewBottom.offsetTopAndBottom(dy)
      } else if (changedView == mViewBottom) {
        mViewTop.offsetTopAndBottom(dy)
      }

      // 此处强制重绘，防止拖动过程中View不显示
      ViewCompat.postInvalidateOnAnimation(this@VerticalSlideLayout)
    }

    override fun getViewVerticalDragRange(child: View): Int =// 此处确定了View可拖动的距离
        child.height

    override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
      // 此处在拖动的View被释放后做处理
      var finalTopOffset = 0// 此处定义变量，用于存储View释放后最终要设置的上边缘位置
      if (releasedChild == mViewTop) {
        // 此时为第一个View被释放
        if (yvel < -VEL_THRESHOLD || releasedChild.top < -DISTANCE_THRESHOLD) {
          // 如果向上的速度足够大（向上的加速度为负值），或者当前View向上偏移的距离足够大
          // 则认为向上翻页有效，将顶部View的上边缘偏移量设置为-mViewHeight（父布局上边缘外）
          finalTopOffset = -mViewHeight
          mIVerticalPageListener?.onPageDown(mPage, (mPage + 1) % 2)
        }
      } else {
        // 此时为第二个View被释放
        if (yvel > VEL_THRESHOLD || releasedChild.top > DISTANCE_THRESHOLD) {
          // 如果向下的速度足够大，或者当前View向下偏移的距离足够大
          // 则认为向下翻页有效，将底部View的上边缘偏移量设置为mViewHeight（当前布局初始化时的位置）
          finalTopOffset = mViewHeight
          mIVerticalPageListener?.onPageUp(mPage, (mPage + 1) % 2)
        }
      }

      // 使被释放View滑动到如上计算的偏移位置
      if (mDragHelper.smoothSlideViewTo(releasedChild, 0, finalTopOffset)) {
        // 此处触发View的滑动事件，后续的处理交给computeScroll()方法进行处理
        ViewCompat.postInvalidateOnAnimation(this@VerticalSlideLayout)
        mAnimationFinished = false // 开始动画时将标识置为false，用于屏蔽错误的触摸事件
      }
    }

    override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
      if (child == mViewTop && top >= 0 && dy > 0) {
        // 如果当前拖动的是顶部View，并且已经到达了最顶部，并且还在向下继续拖动，则限制最顶部边缘的位置为0
        return 0
      } else if (child == mViewBottom && top <= 0 && dy < 0) {
        // 如果当前拖动的是底部View，并且已经可见，并且已经滑动到了底部，并且还在继续向上拖动，则限制最顶部边缘的位置为0
        return 0
      }

      // 其他情况下，则不断根据dy的值改变顶部边缘的位置，直到达到上述两个分支的条件
      // 此处对dy进行折半，用于增加阻尼
      return child.top + (dy / 2f).toInt()
    }
  }

  override fun computeScroll() {
    // 此处接onViewReleased()中的动画处理
    if (mDragHelper.continueSettling(true)) {
      // 如果continueSettling()返回true，则继续处理
      ViewCompat.postInvalidateOnAnimation(this)
      if (mViewTop.top == 0) {
        mPage = 0
      } else if (mViewBottom.top == 0) {
        mPage = 1
      }
    } else {
      // 直到continueSettling()返回false，则处理完成，将标识修改
      mAnimationFinished = true
    }
  }

  fun pageUp() {
    mIVerticalPageListener?.onPageUp(mPage, 0)
    if (mPage == 1) {
      // 此处主动触发View的滑动
      if (mDragHelper.smoothSlideViewTo(mViewBottom, 0, mViewHeight)) {// 将底部的View位置复位
        ViewCompat.postInvalidateOnAnimation(this)
        mAnimationFinished = false
      }
    }
  }

  fun pageDown() {
    mIVerticalPageListener?.onPageDown(mPage, 1)
    if (mPage == 0) {
      // 此处主动触发View的滑动
      if (mDragHelper.smoothSlideViewTo(mViewTop, 0, -mViewHeight)) {
        ViewCompat.postInvalidateOnAnimation(this)
        mAnimationFinished = false
      }
    }
  }

  /**
   * 设置监听页面切换的接口
   */
  fun setVerticalPageListener(listener: IVerticalPageListener) {
    mIVerticalPageListener = listener
  }

  private class YScrollDetector : GestureDetector.SimpleOnGestureListener() {
    override fun onScroll(e1: MotionEvent, e2: MotionEvent, dx: Float,
        dy: Float): Boolean = Math.abs(dy) > Math.abs(dx)// 垂直滑动时dy > dx，才被认定是Y轴方向拖动
  }
}