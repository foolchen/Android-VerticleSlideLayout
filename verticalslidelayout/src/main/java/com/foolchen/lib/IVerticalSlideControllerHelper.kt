package com.foolchen.lib

/**
 *
 * 仅在翻页效果使用触摸事件与外部布局的事件冲突时才使用该接口
 *
 * 使需要协同使用的可滑动控件实现该接口，向可滑动控件设置一个[IVerticalSlideController]
 *
 * @author chenchong
 * 2018/2/1
 * 下午2:15
 */
interface IVerticalSlideControllerHelper {
  fun setIVerticalSlideController(controller: IVerticalSlideController?)
}