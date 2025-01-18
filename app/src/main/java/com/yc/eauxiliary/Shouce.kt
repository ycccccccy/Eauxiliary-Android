package com.yc.eauxiliary

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity


class Shouce : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shouce)

        updateStatusBarTextColor(window)

        // 适配导航栏小横条
        makeNavigationBarTransparentAndKeepSpace()

        val pageTitle = findViewById<TextView>(R.id.pageTitle)
        // 更新标题栏并添加动画效果
        pageTitle.animate().alpha(0f).setDuration(250).withEndAction {
            pageTitle.text = "使用手册"
            pageTitle.animate().alpha(1f).setDuration(100).start()
        }.start()
        val backArrow = findViewById<ImageView>(R.id.backArrow)
        backArrow?.visibility = View.VISIBLE
    }


    @SuppressLint("SetTextI18n")
    fun onBackArrowClick(view: View) {
        // 恢复标题栏并添加动画效果
        val pageTitle = findViewById<TextView>(R.id.pageTitle)
        pageTitle.animate().alpha(0f).setDuration(250).withEndAction {
            pageTitle.text = "设置"
            pageTitle.animate().alpha(1f).setDuration(100).start()
        }.start()
        val backArrow = findViewById<ImageView>(R.id.backArrow)
        backArrow?.visibility = View.GONE

        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

    }


}
