package com.yc.eauxiliary

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.transition.TransitionInflater
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// 常量定义
const val REQUEST_CODE_STORAGE = 1
private const val REQUEST_CODE_DOCUMENT_TREE = 2
const val SHARED_PREFS_NAME = "app_prefs"
const val KEY_DIRECTORY_URI = "directory_uri"
private const val KEY_IS_FIRST_TIME_LAUNCH = "isFirstTimeLaunch"
private const val KEY_STUDENT_NAME = "student_name"
private const val KEY_IS_SINGLE_ANSWER_MODE = "isSingleAnswerMode"
private const val UPDATE_CHECK_URL =
    "https://gitee.com/asdasasdasdasfsdf/version-check/raw/master/version.html"
private const val DOWNLOAD_FILE_NAME = "Eauxiliary.apk"
private const val FILE_PROVIDER_AUTHORITY = "com.yc.at_ets.fileprovider"
val supabaseUrl = "https://bydbvhsknggjkyifhywq.supabase.co" // Supabase URL 数据库的url
val supabaseKey =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ5ZGJ2aHNrbmdnamt5aWZoeXdxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mjk5OTY0ODIsImV4cCI6MjA0NTU3MjQ4Mn0.ojALKrXWOJE3z0-WwObcg9p3wPHNgEddGy0nIWoXbdk" // Supabase Key
val client = OkHttpClient()
private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

// 文件夹路径常量
const val FOLDER_FILES = "files"
const val FOLDER_DOWNLOAD = "Download"
const val FOLDER_ETS_SECONDARY = "ETS_SECONDARY"
private const val FOLDER_RESOURCE = "resource"
private const val FOLDER_TEMP = "temp"

// 用户类型枚举
private enum class UserType {
    BLACKLIST, WHITELIST, NORMAL
}

// 工具类：管理首次运行状态
class FirstRunCheck(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)

    fun isFirstRun(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_FIRST_TIME_LAUNCH, true)
    }

    fun setNotFirstRun() {
        sharedPreferences.edit().putBoolean(KEY_IS_FIRST_TIME_LAUNCH, false).apply()
    }
}

// 工具类：管理状态栏颜色
fun setStatusBarTextColor(window: Window, isDark: Boolean) {
    val decorView = window.decorView
    if (isDark) {
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
    } else {
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }
    window.statusBarColor = Color.TRANSPARENT
}

fun updateStatusBarTextColor(window: Window) {
    val currentNightMode =
        window.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    when (currentNightMode) {
        Configuration.UI_MODE_NIGHT_NO -> setStatusBarTextColor(
            window,
            true
        ) // 夜晚模式
        Configuration.UI_MODE_NIGHT_YES -> setStatusBarTextColor(
            window,
            false
        ) // 白天模式
    }
}


// 主活动类
class MainActivity : AppCompatActivity() {
    // UI 元素
    private lateinit var bottomNavigation: LinearLayout
    private lateinit var pageTitle: TextView
    private lateinit var backArrow: ImageView
    private lateinit var textView: TextView
    private lateinit var container: RecyclerView // 使用 RecyclerView 显示文件夹列表
    private lateinit var folderAdapter: FolderAdapter // 文件夹列表适配器
    private lateinit var recyclerViewContainer: FrameLayout // FrameLayout 容器

    // 数据和状态
    private var directoryUri: Uri? = null
    private var isSingleAnswerMode = false
    private var currentSnackbar: Snackbar? = null
    private var isSnackbarShowing = false
    private var studentName: String? = null
    private var userType: UserType =
        UserType.NORMAL // 用户类型

    private val blacklistedUsers: List<String>
        get() = SecureStorageUtils.getBlacklist(this)

    private val whitelistedUsers: List<String>
        get() = SecureStorageUtils.getWhitelist(this)

    // 错误日志
    companion object {
        private const val TAG = "MainActivity"
    }

    private val mainScope = CoroutineScope(Dispatchers.Main + Job())

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化UI元素
        initUI()

        // 设置状态栏颜色
        updateStatusBarTextColor(window)

        // 从 SharedPreferences 中读取设置
        loadSettings()

        textView.gravity = Gravity.CENTER

        // 预加载学生姓名和用户类型
        loadStudentData()

        findViewById<TextView>(R.id.homeButton).isSelected = true
        findViewById<TextView>(R.id.settingsButton).isSelected = false

        // 检查版本更新
        checkForUpdateAsync(packageManager.getPackageInfo(packageName, 0).versionName)

        // 设置进入和退出动画
        window.enterTransition =
            TransitionInflater.from(this).inflateTransition(R.transition.circular_reveal_enter)
        window.reenterTransition =
            TransitionInflater.from(this).inflateTransition(R.transition.circular_reveal_enter)
        window.exitTransition =
            TransitionInflater.from(this).inflateTransition(R.transition.circular_reveal_exit)


        // 首次运行检查
        val firstRunCheck = FirstRunCheck(this)
        if (firstRunCheck.isFirstRun()) {
            startActivity(Intent(this, OnboardingActivity::class.java)) // 启动 OnboardingActivity
            finish()
        } else {
            fetchFoldersAndUpdateUI()
        }

        SecureStorageUtils.setBlacklist(this, listOf("黑名单在这", "null"))
        SecureStorageUtils.setWhitelist(
            this,
            listOf(
                "在这里写白名单"
            )
        ) //  白名单用户


        // 设置为非首次运行
        firstRunCheck.setNotFirstRun()
    }

    // 初始化 UI 元素
    private fun initUI() {
        bottomNavigation = findViewById(R.id.bottom_navigation)
        pageTitle = findViewById(R.id.pageTitle)
        backArrow = findViewById(R.id.backArrow)
        textView = findViewById(R.id.textView)
        container = findViewById(R.id.container)
        recyclerViewContainer = findViewById(R.id.recycler_view_container)

        // 设置 RecyclerView 的布局管理器和适配器
        container.layoutManager = LinearLayoutManager(this)
        folderAdapter = FolderAdapter(this) { group, cardView ->
            onFolderClick(group, cardView) // 正确的参数
        }
        container.adapter = folderAdapter

        backArrow.setOnClickListener { onBackArrowClick() }
    }

    private fun hasReadPrivacyPolicy(): Boolean {
        val sharedPrefs = getSharedPreferences("global_storage", Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean("hasReadPrivacyPolicy", false)
    }


    // 加载设置
    private fun loadSettings() {
        val mainPreferences = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
        isSingleAnswerMode = mainPreferences.getBoolean(KEY_IS_SINGLE_ANSWER_MODE, false)
        val uriString = mainPreferences.getString(KEY_DIRECTORY_URI, null)
        if (uriString != null) {
            directoryUri = Uri.parse(uriString)
        } else {
            // 处理 uriString 为 null 的情况
            directoryUri = null
        }
    }

    // 预加载学生姓名和用户类型
    private fun loadStudentData() {
        studentName = getStudentName()
        userType = getUserType(studentName)
    }

    // 处理活动结果
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_STORAGE -> {
                    // 处理存储权限请求结果
                }

                REQUEST_CODE_DOCUMENT_TREE -> {
                    // 处理 Document Tree URI 请求结果
                    handleDocumentTreeUriResult(resultData)
                }
            }
        }
    }

    // 处理 Document Tree URI 请求结果
    private fun handleDocumentTreeUriResult(resultData: Intent?) {
        directoryUri = resultData?.data

        // 修改为永久请求
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        directoryUri?.let {
            contentResolver.takePersistableUriPermission(it, takeFlags)

            // 保存 URI
            val sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString(KEY_DIRECTORY_URI, it.toString())
            editor.apply()
            fetchFoldersAndUpdateUI()
        }

        // 显示隐私协议对话框
        showEULADialog()
    }

    // 显示隐私协议对话框
    private fun showEULADialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("提示")
            .setMessage("接下来让我们一起看看隐私协议吧！")
            .setPositiveButton("好！") { dialog, _ ->
                dialog.dismiss()
                // 启动 EULA
                val intent = Intent(this, EULA::class.java)
                intent.putExtra("source", "MainActivity")
                startActivity(intent)
            }
            .show()
    }

    private suspend fun uploadOrUpdateAnswer(
        folderName: String,
        answerContent: String,
        studentName: String?
    ) {
        withContext(Dispatchers.IO) {
            try {
                val student = studentName ?: "Unknown User" // 使用 studentName 或默认值

                // 1. 查询答案是否存在
                val queryUrl = "$supabaseUrl/rest/v1/answers?question_id=eq.$folderName"
                val queryRequest = Request.Builder()
                    .url(queryUrl)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .get()
                    .build()

                val queryResponse = client.newCall(queryRequest).execute()
                val queryResponseBody = queryResponse.body?.string()

                if (queryResponse.isSuccessful && queryResponseBody != null) {
                    val jsonArray = JSONArray(queryResponseBody)

                    if (jsonArray.length() > 0) {
                        // 2. 答案存在，更新贡献者
                        val existingAnswer = jsonArray.getJSONObject(0)
                        val existingContributors = mutableListOf<String>()
                        for (i in 0 until existingAnswer.getJSONArray("contributors").length()) {
                            existingContributors.add(
                                existingAnswer.getJSONArray("contributors").getString(i)
                            )
                        }

                        if (!existingContributors.contains(student)) {
                            existingContributors.add(student)
                        }

                        val updateUrl =
                            "$supabaseUrl/rest/v1/answers?id=eq.${existingAnswer.getInt("id")}"
                        val updateJson = buildJsonObject {
                            put(
                                "contributors",
                                JsonArray(existingContributors.map { JsonPrimitive(it) })
                            )
                        }.toString()

                        val updateRequest = Request.Builder()
                            .url(updateUrl)
                            .addHeader("apikey", supabaseKey)
                            .addHeader("Authorization", "Bearer $supabaseKey")
                            .addHeader("Prefer", "return=representation")
                            .patch(updateJson.toRequestBody(jsonMediaType))
                            .build()

                        client.newCall(updateRequest).execute()

                    } else {
                        // 3. 答案不存在，上传新答案
                        val insertUrl = "$supabaseUrl/rest/v1/answers"
                        val insertJson = buildJsonObject {
                            put("question_id", folderName)
                            put("answer", answerContent)
                            put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()))
                            put(
                                "contributors",
                                buildJsonArray { add(JsonPrimitive(student)) }) // 直接构建 JsonArray
                        }.toString()


                        val insertRequest = Request.Builder()
                            .url(insertUrl)
                            .addHeader("apikey", supabaseKey)
                            .addHeader("Authorization", "Bearer $supabaseKey")
                            .addHeader("Prefer", "return=representation")
                            .post(insertJson.toRequestBody(jsonMediaType))
                            .build()

                        client.newCall(insertRequest).execute()
                    }


                } else {
                    // 处理查询/上传/更新失败的情况
                    Log.e("Supabase", "Error: ${queryResponse.code} ${queryResponseBody}")

                    // 在此处可以添加失败后显示错误信息给用户
                    withContext(Dispatchers.Main) {
                        showCustomSnackbar(
                            "答案上传/更新失败",
                            "error",
                            resources.getColor(R.color.colorError)
                        )
                    }
                }

            } catch (e: Exception) {
                // 处理其他异常
                Log.e("Supabase", "Exception: ${e.message}")

                // 异常处理逻辑
                withContext(Dispatchers.Main) {
                    showCustomSnackbar(
                        "发生错误: ${e.message}",
                        "error",
                        resources.getColor(R.color.colorError)
                    )
                }
            }
        }
    }

    // 获取学生姓名
    private fun getStudentName(): String? {
        var studentName: String? = null
        val sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
        studentName = sharedPreferences.getString(KEY_STUDENT_NAME, null)
        if (studentName == null) {
            // 逐级查找目标文件夹
            var currentDirectory = directoryUri?.let { DocumentFile.fromTreeUri(this, it) }
            val targetPath =
                listOf(FOLDER_FILES, FOLDER_DOWNLOAD, FOLDER_ETS_SECONDARY, FOLDER_TEMP)
            for (folderName in targetPath) {
                currentDirectory = currentDirectory?.findFile(folderName)
                if (currentDirectory == null) {
                    break
                }
            }

            // 读取文件中的学生姓名
            val files = currentDirectory?.listFiles()
            if (files != null && files.isNotEmpty()) {
                val file = files[0]
                if (file.exists()) {
                    try {
                        val inputStream = contentResolver.openInputStream(file.uri)
                        val data = JSONObject(inputStream?.bufferedReader().use { it?.readText() })
                        studentName =
                            data.getJSONArray("data").getJSONObject(0).getString("student_name")
                        saveStudentName(studentName) // 保存到 SharedPreferences
                    } catch (e: Exception) {
                        // 处理异常
                        Log.e(TAG, "获取学生姓名时发生错误: ${e.message}")
                    }
                }
            }
        }
        return studentName
    }

    // 获取用户类型
    private fun getUserType(username: String?): UserType {
        return when {
            username == null -> UserType.NORMAL
            whitelistedUsers.contains(username) -> UserType.WHITELIST
            blacklistedUsers.contains(username) -> UserType.BLACKLIST
            else -> UserType.NORMAL
        }
    }

    // 保存用户名
    private fun saveStudentName(studentName: String) {
        val sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(KEY_STUDENT_NAME, studentName)
        editor.apply()
        this.studentName = studentName
    }

    // 获取文件夹数据并更新UI
    private fun fetchFoldersAndUpdateUI() {
        // 检查是否已阅读隐私协议
        if (!hasReadPrivacyPolicy()) {
            startActivity(Intent(this, EULA::class.java).apply {
                putExtra("source", "MainActivity") // 添加来源信息
            })
            return // 停止 MainActivity 的后续初始化操作
        }

        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        mainScope.launch { // 使用 mainScope 进行 UI 更新
            try {
                progressBar.visibility = View.VISIBLE // 显示 ProgressBar
                updateScanningProgress(0, "环境准备中...耐心等待") // 初始化进度

                val resourceFolder =
                    withContext(Dispatchers.IO) { findResourceFolder() } // 在后台线程查找文件夹

                if (resourceFolder != null) {
                    val folders =
                        withContext(Dispatchers.IO) { getSortedResourceFolders(resourceFolder) }

                    val groupedFolders = groupResourceFoldersByTime(folders) // 在主线程中执行分组

                    // 在主线程中更新UI
                    withContext(Dispatchers.Main) {
                        textView.gravity = Gravity.CENTER

                        // 获取用户名
                        studentName = getStudentName()
                        userType = getUserType(studentName)

                        // 检查用户类型
                        if (userType == UserType.BLACKLIST) {
                            showCustomSnackbar(
                                "一边去",
                                "error",
                                resources.getColor(R.color.colorError)
                            )
                            return@withContext // 终止后续操作
                        }

                        // 检查激活状态 - 使用 ActivationUtils
                        //val isActivated = SecureStorageUtils.isActivated(this@MainActivity) // 注意上下文
                        val isActivated = true // 所有用户都激活

                        if (userType == UserType.NORMAL && !isActivated) {
                            showCustomSnackbar(
                                "未激活，限制使用",
                                "warning",
                                resources.getColor(R.color.colorWarning)
                            )
                        }

                        // 根据用户类型和激活状态过滤文件夹组
                        val displayGroups =
                            filterFolderGroups(groupedFolders, isActivated) // 传递 isActivated

                        // 标记题库类型并更新文件夹列表适配器
                        val markedGroups = markAndFilterGroups(displayGroups)
                        folderAdapter.updateData(markedGroups) // 更新适配器的数据

                        progressBar.visibility = View.GONE // 隐藏 ProgressBar
                        updateScanningProgress(100, "扫描完成!") // 更新进度到 100%
                    }
                } else {
                    Log.e(TAG, "未找到资源文件夹")
                    withContext(Dispatchers.Main) {
                        showCustomSnackbar(
                            "授权异常，检查授权",
                            "error",
                            resources.getColor(R.color.colorError)
                        )
                        progressBar.visibility = View.GONE // 隐藏 ProgressBar
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取文件夹数据时发生错误: ${e.message}")
                withContext(Dispatchers.Main) {
                    showCustomSnackbar(
                        "发生错误，请重试",
                        "error",
                        resources.getColor(R.color.colorError)
                    )
                    progressBar.visibility = View.GONE
                }
            }
        }
    }


    // 过滤文件夹组
    private fun filterFolderGroups(
        groupedFolders: List<List<DocumentFile>>,
        isActivated: Boolean
    ): List<List<DocumentFile>> {
        return when {
            userType == UserType.WHITELIST -> groupedFolders
            userType == UserType.NORMAL && isActivated -> groupedFolders // 使用 isActivated 参数
            else -> if (groupedFolders.isNotEmpty()) listOf(groupedFolders.first()) else emptyList()
        }
    }

    // 查找资源文件夹
    private fun findResourceFolder(): DocumentFile? {
        var currentDirectory = directoryUri?.let { DocumentFile.fromTreeUri(this, it) }
        val targetPath =
            listOf(FOLDER_FILES, FOLDER_DOWNLOAD, FOLDER_ETS_SECONDARY, FOLDER_RESOURCE)
        for (folderName in targetPath) {
            currentDirectory = currentDirectory?.findFile(folderName)
            if (currentDirectory == null) {
                return null
            }
        }
        return currentDirectory
    }

    // 获取排序后的资源文件夹列表
    private fun getSortedResourceFolders(resourceFolder: DocumentFile): List<DocumentFile> {
        return resourceFolder.listFiles()
            .filter {
                it.isDirectory && it.name != "common" && it.listFiles()
                    .any { it.name == "content.json" }
            } // 添加过滤条件
            .sortedByDescending { it.lastModified() }
    }

    // 标记题库类型并更新文件夹列表适配器
    private fun markAndFilterGroups(groupedFolders: List<List<DocumentFile>>): List<Pair<List<DocumentFile>, String>> { // 修改返回值类型
        return groupedFolders.map { group ->
            val tag = if (group.size == 3) "高中" else if (group.size == 7) "初中" else "未知"
            Pair(group, tag) // 返回 Pair，包含文件夹组和标签
        }
    }

    // 按时间分组资源文件夹 (在主线程中执行)
    private suspend fun groupResourceFoldersByTime(folders: List<DocumentFile>): List<List<DocumentFile>> {
        return withContext(Dispatchers.Main) {
            val groupedFolders = mutableListOf<List<DocumentFile>>()
            var tempGroup = mutableListOf<DocumentFile>()

            for (i in folders.indices) {
                val currentFolder = folders[i]

                if (tempGroup.isEmpty()) {
                    tempGroup.add(currentFolder)
                } else {
                    val lastFolder = tempGroup.last()
                    val timeDiff =
                        Math.abs(currentFolder.lastModified() - lastFolder.lastModified())

                    if (timeDiff <= 60 * 1000) { // 同一分钟内
                        tempGroup.add(currentFolder) // 先添加到临时分组
                    } else {
                        // 不同时间，处理 tempGroup
                        groupedFolders.addAll(splitGroupByType(tempGroup)) // 根据数量拆分
                        tempGroup = mutableListOf(currentFolder) // 开始新的分组
                    }
                }
            }

            // 处理最后一个 tempGroup
            if (tempGroup.isNotEmpty()) {
                groupedFolders.addAll(splitGroupByType(tempGroup))
            }

            groupedFolders
        }
    }

    private fun splitGroupByType(group: List<DocumentFile>): List<List<DocumentFile>> {
        val result = mutableListOf<List<DocumentFile>>()
        when {
            group.size % 3 == 0 -> {  // 高中
                for (i in group.indices step 3) {
                    result.add(group.subList(i, minOf(i + 3, group.size)))
                }
            }

            group.size % 7 == 0 -> {  // 初中
                for (i in group.indices step 7) {
                    result.add(group.subList(i, minOf(i + 7, group.size)))
                }
            }

            else -> { // 其他情况或未知题型
                result.add(group)  // 直接添加到结果列表中
            }
        }
        return result
    }

    private fun onFolderClick(group: List<DocumentFile>, cardView: View) {
        val answers = getAnswersFromFolderGroup(group)

        updateScanningProgress(0, " ")

        // 获取卡片在屏幕中的位置
        val cardRect = Rect()
        cardView.getGlobalVisibleRect(cardRect) // 使用 getGlobalVisibleRect()

        val intent = Intent(this, AnswerActivity::class.java)
        intent.putExtra("ANSWERS", answers) //  恢复这一行，传递答案数据
        intent.putExtra("CARD_VIEW_LEFT", cardRect.left)
        intent.putExtra("CARD_VIEW_TOP", cardRect.top)
        intent.putExtra("CARD_VIEW_WIDTH", cardRect.width())
        intent.putExtra("CARD_VIEW_HEIGHT", cardRect.height())

        // 使用卡片 View 作为共享元素
        val options =
            ActivityOptionsCompat.makeSceneTransitionAnimation(this, cardView, "card_transition")
        startActivity(intent, options.toBundle())

        // 异步上传或更新答案
        val folderName = group[0].name ?: return
        lifecycleScope.launch(Dispatchers.IO) { // 使用 lifecycleScope
            uploadOrUpdateAnswer(folderName, answers, studentName)
        }
    }

    //读取答案数据
    private fun getAnswersFromFolderGroup(group: List<DocumentFile>): String {
        val listeningChoiceBuilder = StringBuilder()
        val answeringQuestionsBuilder = StringBuilder()
        val storyBuilder = StringBuilder()
        val askingQuestionsBuilder = StringBuilder()
        val questionBuilder = StringBuilder()

        updateScanningProgress(0, "开始读取答案...")

        val cachedAnswers = SecureStorageUtils.getCachedAnswers(this, group)
        if (cachedAnswers != null) {
            return cachedAnswers // 如果缓存存在，直接返回缓存的答案
        }

        val totalFiles = group.sumOf { it.listFiles().size }
        var processedFiles = 0

        for (folder in group) {
            for (file in folder.listFiles()) {
                if (file.name == "content.json") {
                    try {
                        contentResolver.openInputStream(file.uri)?.use { inputStream ->
                            val data = inputStream.bufferedReader().use { it.readText() }
                            val jsonData = JSONObject(data)

                            when (jsonData.getString("structure_type")) {
                                "collector.role" -> { // 初中
                                    questionBuilder.append(parseQuestionData(jsonData, false))
                                }

                                "collector.3q5a" -> { // 高中
                                    questionBuilder.append(parseQuestionData(jsonData, true))
                                }

                                "collector.picture" -> {
                                    storyBuilder.append(parseStoryData(jsonData))
                                }

                                else -> {
                                    Log.e(
                                        "AnswerActivity",
                                        "未知的 structure_type: ${jsonData.getString("structure_type")}"
                                    )
                                }
                            }
                        }

                        processedFiles++
                        val progress = (processedFiles.toFloat() / totalFiles) * 100
                        updateScanningProgress(
                            progress.toInt(),
                            "正在读取答案... ${progress.toInt()}%"
                        )

                    } catch (e: JSONException) {
                        Log.e("AnswerActivity", "JSON 解析错误: ${e.message}", e)
                    } catch (e: Exception) {
                        Log.e("AnswerActivity", "读取文件时发生错误: ${e.message}", e)
                    }
                }
            }
        }

        val combinedAnswers = questionBuilder.toString() +
                listeningChoiceBuilder.toString() +
                answeringQuestionsBuilder.toString() +
                storyBuilder.toString() +
                askingQuestionsBuilder.toString()

        SecureStorageUtils.cacheAnswers(this, group, combinedAnswers)

        updateScanningProgress(100, "解析完成!")

        return combinedAnswers
    }


    private fun parseQuestionData(data: JSONObject, isHighSchool: Boolean): String {
        val builder = StringBuilder()
        val info = data.getJSONObject("info")
        val questions = info.getJSONArray("question")

        for (j in 0 until questions.length()) {
            val question = questions.getJSONObject(j)
            val stdAnswers = question.getJSONArray("std")


            if (isHighSchool) { // 高中
                builder.appendLine("角色扮演 ${j + 1}:\n\n") //
            } else { // 初中
                val ask = question.getString("ask")
                when {
                    ask.contains("(") && ask.contains(")") -> {
                        builder.appendLine("听选信息 ${j + 1}: ${parseQuestion(ask)}\n\n")
                    }

                    ask.contains("问题") -> {
                        builder.appendLine("提问 ${j + 1}: ${parseQuestion(ask)}\n\n")
                    }

                    else -> {
                        builder.appendLine("回答问题 ${j + 1}: ${parseQuestion(ask)}\n\n")
                    }
                }
            }

            for (k in 0 until stdAnswers.length()) {
                val answer = stdAnswers.getJSONObject(k)
                val answerText = answer.getString("value")
                val plainText = removeHtmlTags(answerText)
                builder.append("${k + 1}. $plainText\n\n")
                if (isSingleAnswerMode) break
            }
            builder.append("\n") 
        }

        return builder.toString()
    }


    // 解析问题，去除HTML标签和多余的换行符
    private fun parseQuestion(questionText: String): String {
        return questionText
            .replace(Regex("<.*?>"), "")
            .replace(Regex("</?br>"), "")
            .replace(Regex("ets_th[12]"), "")
            .replace("\n", "") // 去除所有换行符
            .trim() // 去除首尾空格
    }


    // 解析短文数据
    private fun parseStoryData(data: JSONObject): String {
        val builder = StringBuilder()
        val info = data.optJSONObject("info")  // 使用 optJSONObject 避免空指针异常

        if (!isSingleAnswerMode && info != null && info.has("value")) {
            val originalText = info.getString("value")
            val plainOriginalText = removeHtmlTags(originalText)
            builder.appendLine("\n短文复述原文：\n$plainOriginalText\n\n")
        }

        if (info != null && info.has("std")) {
            builder.appendLine("短文复述答案：\n")
            val stdArray = info.getJSONArray("std")
            val numAnswers = if (isSingleAnswerMode) 1 else stdArray.length()

            for (i in 0 until numAnswers) {
                val stdObject = stdArray.getJSONObject(i)
                if (stdObject.has("value")) {
                    val answerText = stdObject.getString("value")
                    val plainAnswerText = removeHtmlTags(answerText)
                    builder.append("${i + 1}. $plainAnswerText\n\n")
                }
            }
        }

        return builder.toString()
    }

    // 返回按钮点击事件
    @SuppressLint("SetTextI18n")
    private fun onBackArrowClick() {
        // 显示文件夹组按钮并添加动画效果
        container.visibility = View.VISIBLE
        container.alpha = 0f
        container.animate().alpha(1f).setDuration(300).start()

        // 恢复标题栏并添加动画效果
        pageTitle.animate().alpha(0f).setDuration(250).withEndAction {
            pageTitle.text = "EAuxiliary"
            pageTitle.animate().alpha(1f).setDuration(100).start()
        }.start()
        backArrow.visibility = View.GONE

        // 清空答案显示区域并添加动画效果
        textView.animate().alpha(0f).setDuration(300).withEndAction {
            textView.text = ""
            textView.visibility = View.GONE
        }.start()

        // 显示文件夹组
        container.visibility = View.VISIBLE

        showBottomNavigation()
    }

    // 显示底部导航栏
    private fun showBottomNavigation() {
        bottomNavigation.visibility = View.VISIBLE
        bottomNavigation.translationY = bottomNavigation.height.toFloat()
        bottomNavigation.animate()
            .translationY(0f)
            .setDuration(300)
            .start()
    }

    // 移除 HTML 标签
    private fun removeHtmlTags(text: String): String {
        return text.replace(Regex("<.*?>"), "")
    }


    // 底部导航栏点击事件：设置
    fun onSettingsClick(view: View) {
        findViewById<TextView>(R.id.homeButton).isSelected = false
        findViewById<TextView>(R.id.settingsButton).isSelected = true

        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    // 底部导航栏点击事件：主页
    fun onHomeClick(view: View) {
        findViewById<TextView>(R.id.homeButton).isSelected = true
        findViewById<TextView>(R.id.settingsButton).isSelected = false
    }

    // 显示自定义 Snackbar
    @SuppressLint("RestrictedApi")
    private fun showCustomSnackbar(message: String, type: String, color: Int) {
        // 如果当前有 Snackbar 显示，直接返回，不显示新的 Snackbar
        if (isSnackbarShowing) {
            currentSnackbar?.dismiss()
            return
        }

        val inflater = layoutInflater
        val layout: View = inflater.inflate(
            R.layout.custom_snackbar,
            findViewById(R.id.custom_snackbar_container)
        )

        val icon: ImageView = layout.findViewById(R.id.icon)
        val text: TextView = layout.findViewById(R.id.message)

        text.text = message
        layout.setBackgroundColor(color)

        when (type) {
            "warning" -> icon.setImageResource(R.drawable.ic_warning)
            "error" -> icon.setImageResource(R.drawable.ic_error)
            "success" -> icon.setImageResource(R.drawable.ic_success)
        }

        val snackbar =
            Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
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

    // 异步检查版本更新
    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkForUpdateAsync(currentVersion: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val latestVersion = fetchLatestVersion()
            val changelog = fetchChangelog()
            if (latestVersion != null && latestVersion > currentVersion) {
                withContext(Dispatchers.Main) {
                    showUpdateDialog(latestVersion, changelog)
                }
            }
        }
    }

    // 获取最新版本号
    private fun fetchLatestVersion(): String? {
        return try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(UPDATE_CHECK_URL)
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string()
            val doc = Jsoup.parse(html)
            doc.getElementById("version")?.text()
        } catch (e: Exception) {
            Log.e(TAG, "获取最新版本号时发生错误: ${e.message}")
            null
        }
    }

    // 获取更新日志
    private fun fetchChangelog(): String? {
        return try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(UPDATE_CHECK_URL)
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string()
            val doc = Jsoup.parse(html)
            val changelogElements = doc.select("#changelog ul li")
            changelogElements.joinToString("\n") { it.text() }
        } catch (e: Exception) {
            Log.e(TAG, "获取更新日志时发生错误: ${e.message}")
            null
        }
    }

    // 显示更新对话框
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showUpdateDialog(latestVersion: String, changelog: String?) {
        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle("有新版本咯")
            .setMessage("发现新版本 $latestVersion，快下载更新吧！\n\n 更新内容：\n $changelog")
            .setPositiveButton("马上更新") { _, _ ->
                // 清除缓存
                val sharedPrefs = SecureStorageUtils.getSharedPrefs(this)
                sharedPrefs?.edit()?.clear()?.apply()
                startDownload("https://8128.kstore.space/EAuxiliary.apk")
            }
            .setNegativeButton("下次一定", null)
            .show()
    }

    // 开始下载
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startDownload(url: String) {
        showCustomSnackbar("已开始下载", "success", resources.getColor(R.color.colorSuccess))
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("下载更新")
            .setDescription("正在下载新版本...")
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                DOWNLOAD_FILE_NAME
            )
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val downloadManager =
            getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // 监听下载完成
        registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        // 下载完成，提示用户安装
                        val file = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            DOWNLOAD_FILE_NAME
                        )
                        val apkUri: Uri =
                            FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(apkUri, "application/vnd.android.package-archive")
                            flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }

                        startActivity(installIntent)
                    }
                }
            },
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            RECEIVER_NOT_EXPORTED
        )
    }

    // 更新扫描进度
    @SuppressLint("SetTextI18n")
    private fun updateScanningProgress(progress: Int, message: String) {
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        mainScope.launch {
            textView.text = "$message"
            progressBar.progress = progress // 更新 ProgressBar 的进度
        }
    }
}

// 文件夹列表适配器
class FolderAdapter(
    private val context: Context,
    private val onFolderClick: (group: List<DocumentFile>, cardView: View) -> Unit // 修改参数类型
) : RecyclerView.Adapter<FolderAdapter.ViewHolder>() {

    private val folderGroups = mutableListOf<Pair<List<DocumentFile>, String>>()
    private val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())

    fun updateData(newFolderGroups: List<Pair<List<DocumentFile>, String>>) { // 修改参数类型
        folderGroups.clear()
        folderGroups.addAll(newFolderGroups)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = folderGroups[position]
        holder.bind(group, position)
    }

    override fun getItemCount(): Int {
        return folderGroups.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val buttonLayout: LinearLayout = itemView.findViewById(R.id.button_layout)
        private val titleTextView: TextView = itemView.findViewById(R.id.title_text_view)
        private val timeTextView: TextView = itemView.findViewById(R.id.time_text_view)
        private val tagTextView: TextView =
            itemView.findViewById(R.id.tag_text_view) // 添加标签 TextView

        fun bind(group: Pair<List<DocumentFile>, String>, position: Int) {
            val (folders, tag) = group // 解构 Pair
            val folderName = folders[0].name?.substring(0, 6) ?: "文件夹"
            titleTextView.text = "模考 $folderName "
            timeTextView.text = "时间:${sdf.format(Date(folders[0].lastModified()))}"
            tagTextView.text = tag // 设置标签


            buttonLayout.setOnClickListener {
                // 获取被点击的按钮
                val buttonView = it as View

                // 获取按钮在屏幕中的坐标
                val buttonLocation = IntArray(2)
                buttonView.getLocationOnScreen(buttonLocation)

                // 将 buttonLayout 作为 cardView 参数传递
                onFolderClick(group.first, it) // 将 buttonLayout 本身作为 cardView 参数传递
            }

            // 添加淡入动画
            val fadeInAnimation = AlphaAnimation(0f, 1f).apply {
                duration = 100 // 动画持续时间
                startOffset = position * 50L // 设置每个按钮的动画延迟
                fillAfter = true // 动画结束后保持最终状态
            }
            buttonLayout.startAnimation(fadeInAnimation)
        }

    }
}
