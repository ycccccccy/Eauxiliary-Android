package com.yc.eauxiliary

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class EULA : AppCompatActivity() {

    private var hasReadPrivacyPolicy = false
    private var sourceActivity: String? = null

    private var currentSnackbar: Snackbar? = null
    private var isSnackbarShowing = false

    @SuppressLint("RestrictedApi")
    private fun showCustomSnackbar(message: String, type: String, color: Int) {
        // 确保只显示一个
        if (isSnackbarShowing) {
            currentSnackbar?.dismiss()
            return
        }

        val inflater = layoutInflater
        val layout: View =
            inflater.inflate(R.layout.custom_snackbar, findViewById(R.id.custom_snackbar_container))

        val icon: ImageView = layout.findViewById(R.id.icon)
        val text: TextView = layout.findViewById(R.id.message)

        text.text = message
        layout.setBackgroundColor(color)

        when (type) {
            "warning" -> icon.setImageResource(R.drawable.ic_warning)
            "error" -> icon.setImageResource(R.drawable.ic_error)
            "success" -> icon.setImageResource(R.drawable.ic_success)
        }

        val snackbar = Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
        val snackbarLayout = snackbar.view as Snackbar.SnackbarLayout
        snackbarLayout.addView(layout, 0)

        // 设置 Snackbar 的宽度和位置
        val params = snackbar.view.layoutParams as FrameLayout.LayoutParams
        params.width = FrameLayout.LayoutParams.WRAP_CONTENT
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.bottomMargin = 200 // 调整底部间距
        snackbar.setBackgroundTint(color)
        snackbar.view.layoutParams = params

        // 显示新的 Snackbar，并保存引用
        snackbar.show()
        currentSnackbar = snackbar
        isSnackbarShowing = true

        // 当 Snackbar 消失时重置标志变量
        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                isSnackbarShowing = false
            }
        })
    }

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eula)

        updateStatusBarTextColor(window)

        sourceActivity = intent.getStringExtra("source")

        val sharedPreferences = getSharedPreferences("global_storage", Context.MODE_PRIVATE)
        hasReadPrivacyPolicy = sharedPreferences.getBoolean("hasReadPrivacyPolicy", false)

        val pageTitle = findViewById<TextView>(R.id.pageTitle)
        // 更新标题栏并添加动画效果
        pageTitle.animate().alpha(0f).setDuration(250).withEndAction {
            pageTitle.text = "隐私协议"
            pageTitle.animate().alpha(1f).setDuration(100).start()
        }.start()
        val backArrow = findViewById<ImageView>(R.id.backArrow)
        backArrow?.visibility = View.VISIBLE

        val privacyScrollView = findViewById<ScrollView>(R.id.privacyScrollView)
        val confirmButton = findViewById<Button>(R.id.confirmButton)

        if (!hasReadPrivacyPolicy) {
            confirmButton.visibility = View.VISIBLE
            privacyScrollView.viewTreeObserver.addOnScrollChangedListener {
                val view = privacyScrollView.getChildAt(privacyScrollView.childCount - 1) as View
                val diff = view.bottom - (privacyScrollView.height + privacyScrollView.scrollY)
                if (diff == 0) {
                    // 用户已经滚动到页面底部
                    confirmButton.visibility = View.VISIBLE
                }
            }
        }

        // 拦截系统返回键
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasReadPrivacyPolicy) {
                    navigateBack()
                } else {
                    showCustomSnackbar(
                        "请阅读完隐私协议后再退出",
                        "warning",
                        resources.getColor(R.color.colorWarning)
                    )
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    fun onBackArrowClick(view: View) {
        if (hasReadPrivacyPolicy) {
            navigateBack()
        } else {
            showCustomSnackbar(
                "请阅读完隐私协议后再退出",
                "warning",
                resources.getColor(R.color.colorWarning)
            )
        }
    }

    fun onConfirmButtonClick(view: View) {
        hasReadPrivacyPolicy = true
        val sharedPreferences = getSharedPreferences("global_storage", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("hasReadPrivacyPolicy", true)
        editor.apply()
        showCustomSnackbar("已确认隐私协议", "success", resources.getColor(R.color.colorSuccess))
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun navigateBack() {
        when (sourceActivity) {
            "MainActivity" -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }

            "SettingsActivity" -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }

            else -> {
                finish()
            }
        }
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
