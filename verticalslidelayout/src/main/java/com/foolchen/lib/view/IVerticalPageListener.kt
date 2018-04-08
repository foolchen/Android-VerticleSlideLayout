package com.foolchen.lib.view

interface IVerticalPageListener {
  fun onPageUp(currentPage: Int, futurePage: Int)
  fun onPageDown(currentPage: Int, futurePage: Int)
}