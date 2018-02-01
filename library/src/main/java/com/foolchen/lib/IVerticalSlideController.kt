package com.foolchen.lib

/**
 *
 * 仅在翻页效果使用触摸事件与外部布局的事件冲突时才使用该接口
 *
 * 使需要协同使用的可滑动控件实现该接口，在不需要触发翻页效果时调用[setIVerticalSlideController]方法禁用翻页
 *
 * @author chenchong
 * 2018/2/1
 * 下午2:15
 */
interface IVerticalSlideController {
  /**
   * 在放过触摸事件，交由父布局处理时，如果满足了翻页的条件则将翻页效果置为true，否则置为false。
   * 防止与右划返回等事件冲突
   *
   * 该方法仅供可滑动控件调用，如果需要手动禁用翻页，请调用[VerticalSlideLayout.setSlideEnable]
   */
  fun controlSlideEnable(enable: Boolean)
}