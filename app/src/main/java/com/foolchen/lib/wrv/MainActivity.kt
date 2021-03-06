package com.foolchen.lib.wrv

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.foolchen.lib.view.IVerticalPageListener
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Slidr.attach(this, SlidrConfig.Builder()
            .position(SlidrPosition.LEFT)
            .sensitivity(1f)
            .edge(true)
            .edgeSize(0.33f)
            .build())

    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)

    fab.setOnClickListener { view ->
      Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
          .setAction("Action", null).show()
    }

    init()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(
      item: MenuItem): Boolean =// Handle action bar item clicks here. The action bar will
      // automatically handle clicks on the Home/Up button, so long
      // as you specify a parent activity in AndroidManifest.xml.
      when (item.itemId) {
        R.id.action_settings -> true
        R.id.action_top -> {
          vsl.pageUp()
          true
        }
        R.id.action_bottom -> {
          vsl.pageDown()
          true
        }
        else -> super.onOptionsItemSelected(item)
      }

  private fun init() {

    val webFragment = WebViewFragment()
    val recycleFragment = RecyclerViewFragment()

    supportFragmentManager.beginTransaction().replace(R.id.frame1, webFragment).replace(
        R.id.frame2,
        recycleFragment).commit()

    vsl.setVerticalPageListener(object : IVerticalPageListener {
      override fun onPageUp(currentPage: Int, futurePage: Int) {
        webFragment.onPageUp(currentPage, futurePage)
        recycleFragment.onPageUp(currentPage, futurePage)
      }

      override fun onPageDown(currentPage: Int, futurePage: Int) {
        webFragment.onPageDown(currentPage, futurePage)
        recycleFragment.onPageDown(currentPage, futurePage)
      }

    })
  }
}
