package com.foolchen.lib.view

/** 表示内容向上滚动（滚动条向下滚动） */
const val DIRECTION_UP = -1
/** 表示内容向下滚动（滚动条向上） */
const val DIRECTION_DOWN = 1

/** 表示向上拖动（滚动条向下滚动）*/
const val DRAG_UP = DIRECTION_DOWN
/** 表示向下拖动（滚动条向上滚动） */
const val DRAG_DOWN = DIRECTION_UP

interface IVerticalSlideView {

  /**
   * 检查是否能够向上/向下拖动
   * @see [DRAG_UP]
   * @see [DRAG_DOWN]
   */
  fun checkCanDrag(direction: Int): Boolean

  /**
   * 检查View是否已经滑动到了最顶部
   */
  fun checkIsTop(): Boolean

  /**
   * 检查View是否已经滑动到了最底部
   */
  fun checkIsBottom(): Boolean
}