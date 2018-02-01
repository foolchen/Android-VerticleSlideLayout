package com.foolchen.lib.view

import android.content.Context
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.util.AttributeSet
import android.view.MotionEvent
import com.foolchen.lib.IVerticalSlideController
import com.foolchen.lib.IVerticalSlideControllerHelper
import com.foolchen.lib.VerticalSlideLayout

/**
 * 与[VerticalSlideLayout]组合使用，用于实现翻页效果
 * 该View能够根据是否到达了顶部/底部，决定是否将事件交由父布局处理
 * @author chenchong
 * 2017/11/17
 * 上午10:09
 */
open class VerticalSlideRecyclerView : RecyclerView, IVerticalSlideView, IVerticalSlideControllerHelper {
  val TAG = "VerticalSlideRecyclerView"
  private var mDownX = 0F
  private var mDownY = 0F
  private var mIVerticalSlideController: IVerticalSlideController? = null

  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs,
      defStyle)

  override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
    with(ev) {
      val ac = action and MotionEvent.ACTION_MASK
      when (ac) {
        MotionEvent.ACTION_DOWN -> {
          mDownX = x
          mDownY = y

          // 默认不让父布局处理触摸事件，待ACTION_MOVE中判断满足条件后再交由父布局处理
          parent.requestDisallowInterceptTouchEvent(true)
        }
        MotionEvent.ACTION_MOVE -> {
          val allowParentTouchEvent: Boolean
          val dx = x - mDownX
          val dy = y - mDownY

          if (Math.abs(dy) > Math.abs(dx)) {
            // Y轴方向位移>X轴方向位移，则认为是垂直方向发生了位移
            allowParentTouchEvent = if (dy > 0) {
              // 当前坐标>上次坐标，向下拖动View
              // 此时如果View已经滑动到顶部，则可以向下拖拽，交由父布局处理
              checkIsTop()
            } else {
              // 当前坐标<上次坐标，向上拖动View
              // 此时如果View已经滑动到底部，则可以向上拖拽，交由父布局处理
              checkIsBottom()
            }
            mIVerticalSlideController?.controlSlideEnable(true)
            parent.requestDisallowInterceptTouchEvent(!allowParentTouchEvent)
          } else {
            // Y轴方向位移<X轴方向位移时，则允许父布局获取事件，防止与手势右划返回冲突
            mIVerticalSlideController?.controlSlideEnable(false)
            parent.requestDisallowInterceptTouchEvent(false)
          }
        }

      }
    }

    return super.dispatchTouchEvent(ev)
  }

  override fun checkCanDrag(direction: Int): Boolean = when (direction) {
    DRAG_UP -> checkIsBottom()
    DRAG_DOWN -> checkIsTop()
    else -> throw IllegalArgumentException("Incorrect direction $direction")
  }

  override fun checkIsTop(): Boolean {
    var firstCompleteVisiblePosition = 0
    val layoutManager = layoutManager
    with(layoutManager)
    {

      when {
        this is LinearLayoutManager -> {
          firstCompleteVisiblePosition = findFirstCompletelyVisibleItemPosition()
        }

        this is GridLayoutManager -> {
          firstCompleteVisiblePosition = findFirstCompletelyVisibleItemPosition()
        }

        this is StaggeredGridLayoutManager -> {
          val spanCount = spanCount
          val firstCompleteVisiblePositions = IntArray(spanCount)
          val lastCompleteVisiblePositionss = IntArray(spanCount)
          findFirstVisibleItemPositions(firstCompleteVisiblePositions)
          findLastVisibleItemPositions(lastCompleteVisiblePositionss)
          // 取所有完全可见的位置中最小的一个
          firstCompleteVisiblePosition = firstCompleteVisiblePositions.min() ?: 0
        }

        else -> {
          throw RuntimeException("Unsupported layoutManager $javaClass")
        }
      }
    }

    return firstCompleteVisiblePosition == 0
  }

  override fun checkIsBottom(): Boolean {
    var lastCompleteVisiblePosition = 0
    val layoutManager = layoutManager
    with(layoutManager)
    {

      when {
        this is LinearLayoutManager -> {
          lastCompleteVisiblePosition = findLastVisibleItemPosition()
        }

        this is GridLayoutManager -> {
          lastCompleteVisiblePosition = findLastVisibleItemPosition()
        }

        this is StaggeredGridLayoutManager -> {
          val spanCount = spanCount
          val lastCompleteVisiblePositions = IntArray(spanCount)
          findLastVisibleItemPositions(lastCompleteVisiblePositions)
          // 取所有可见的位置中最大的一个
          lastCompleteVisiblePosition = lastCompleteVisiblePositions.max() ?: 0
        }

        else -> {
          throw RuntimeException("Unsupported layoutManager $javaClass")
        }
      }
    }

    return lastCompleteVisiblePosition >= layoutManager.itemCount - 1
  }

  override fun setIVerticalSlideController(controller: IVerticalSlideController?) {
    mIVerticalSlideController = controller
  }
}