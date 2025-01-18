package com.yc.eauxiliary

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible

class AnswerActivity : AppCompatActivity() {

    private lateinit var rootView: View
    private lateinit var answerTextView: TextView
    private lateinit var pagetitle: TextView
    private var cx: Int = 0
    private var cy: Int = 0
    private lateinit var answerContainer: FrameLayout // 用于动画的容器

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_answer) // 先设置布局

        answerContainer = findViewById(R.id.answer_container) // 再查找 View

        rootView = findViewById(android.R.id.content)
        answerTextView = findViewById(R.id.answer_text_view)
        pagetitle = findViewById(R.id.answer_page_title)

        updateStatusBarTextColor(window)

        // 适配导航栏小横条
        makeNavigationBarTransparentAndKeepSpace()

        val title = intent.getStringExtra("TITLE") ?: "答案"
        pagetitle.text = title

        // 获取传递的数据
        val answers = intent.getStringExtra("ANSWERS") ?: ""
        Log.d("AnswerActivity", "Intent extras: ${intent.extras}")
        cx = intent.getIntExtra("CX", 0)
        cy = intent.getIntExtra("CY", 0)

        // 设置答案文本
        answerTextView.text = answers

        // 在布局完成后启动进入动画
        rootView.post { startEnterAnimation() }

        // 设置返回按钮点击事件
        val backButton: ImageView = findViewById(R.id.answer_back_button)
        backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun startEnterAnimation() {
        val left = intent.getIntExtra("CARD_VIEW_LEFT", 0)
        val top = intent.getIntExtra("CARD_VIEW_TOP", 0)
        val width = intent.getIntExtra("CARD_VIEW_WIDTH", 0)
        val height = intent.getIntExtra("CARD_VIEW_HEIGHT", 0)

        val cardRect = Rect(left, top, left + width, top + height)

        // 设置初始布局参数
        answerContainer.layoutParams = FrameLayout.LayoutParams(
            cardRect.width(),
            cardRect.height()
        ).apply {
            topMargin = cardRect.top
            leftMargin = cardRect.left
        }
        answerContainer.isVisible = true

        // 动画
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200 // 动画时长

            addUpdateListener { valueAnimator ->
                val animatedValue = valueAnimator.animatedValue as Float
                val newLayoutParams = answerContainer.layoutParams as FrameLayout.LayoutParams
                newLayoutParams.width =
                    (rootView.width * animatedValue + cardRect.width() * (1 - animatedValue)).toInt()
                newLayoutParams.height =
                    (rootView.height * animatedValue + cardRect.height() * (1 - animatedValue)).toInt()
                newLayoutParams.topMargin = (cardRect.top * (1 - animatedValue)).toInt()
                newLayoutParams.leftMargin = (cardRect.left * (1 - animatedValue)).toInt()
                answerContainer.layoutParams = newLayoutParams
            }

            doOnEnd {
                // 动画结束后将 answerContainer 的大小重置为 match_parent
                answerContainer.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }.start()
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("RestrictedApi")
    override fun onBackPressed() {
        val left = intent.getIntExtra("CARD_VIEW_LEFT", 0)
        val top = intent.getIntExtra("CARD_VIEW_TOP", 0)
        val width = intent.getIntExtra("CARD_VIEW_WIDTH", 0)
        val height = intent.getIntExtra("CARD_VIEW_HEIGHT", 0)

        val cardRect = Rect(left, top, left + width, top + height)

        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 200 // 动画时长

            addUpdateListener { valueAnimator ->
                val animatedValue = valueAnimator.animatedValue as Float
                val newLayoutParams = answerContainer.layoutParams as FrameLayout.LayoutParams
                newLayoutParams.width =
                    (rootView.width * animatedValue + cardRect.width() * (1 - animatedValue)).toInt()
                newLayoutParams.height =
                    (rootView.height * animatedValue + cardRect.height() * (1 - animatedValue)).toInt()
                newLayoutParams.topMargin = (cardRect.top * (1 - animatedValue)).toInt()
                newLayoutParams.leftMargin = (cardRect.left * (1 - animatedValue)).toInt()
                answerContainer.layoutParams = newLayoutParams
            }

            doOnEnd {
                finish()
                overridePendingTransition(0, R.anim.fade_out)
                super.onBackPressed() // 调用 super.onBackPressed()
            }
        }.start()
    }
}