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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.transition.TransitionInflater
import android.util.Log
import android.util.TypedValue
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private lateinit var homeIcon: ImageView
    private lateinit var settingsIcon: ImageView
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
        homeIcon = findViewById(R.id.homeIcon)
        settingsIcon = findViewById(R.id.settingsIcon)
        pageTitle = findViewById(R.id.pageTitle)
        backArrow = findViewById(R.id.backArrow)
        textView = findViewById(R.id.textView)
        container = findViewById(R.id.container)
        recyclerViewContainer = findViewById(R.id.recycler_view_container)

        // 设置 RecyclerView 的布局管理器和适配器
        container.layoutManager = LinearLayoutManager(this)
        folderAdapter = FolderAdapter(this) { group, cx, cy ->
            onFolderClick(group, cx, cy) // 传递 cx 和 cy 参数
        }
        container.adapter = folderAdapter

        resetIcons()
        homeIcon.setColorFilter(ContextCompat.getColor(this, R.color.my_color))

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

                    //上传/更新成功后提示用户
                    withContext(Dispatchers.Main) {
                        showCustomSnackbar(
                            "答案已成功上传/更新",
                            "success",
                            resources.getColor(R.color.colorSuccess)
                        )
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
                putExtra("source", "MainActivity") //  添加来源信息
            })
            return //  停止 MainActivity 的后续初始化操作
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
                        val isActivated =
                            SecureStorageUtils.isActivated(this@MainActivity) // 注意上下文

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

                        // 更新文件夹列表适配器
                        folderAdapter.updateData(displayGroups)
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
            .filter { it.isDirectory && it.name != "common" }
            .sortedByDescending { it.lastModified() }
    }

    // 按时间分组资源文件夹 (在主线程中执行)
    private suspend fun groupResourceFoldersByTime(folders: List<DocumentFile>): List<List<DocumentFile>> {
        return withContext(Dispatchers.Main) {
            val groupedFolders = mutableListOf<List<DocumentFile>>()
            var tempGroup = mutableListOf<DocumentFile>()
            val totalGroupsToForm = (folders.size + 2) / 3 // 计算需要分的组数
            var processedGroups = 0

            for (i in folders.indices) {
                if (tempGroup.isEmpty()) {
                    tempGroup.add(folders[i])
                } else {
                    val timeDiff =
                        Math.abs(folders[i].lastModified() - tempGroup.last().lastModified())
                    if (timeDiff <= 60 * 1000 && tempGroup.size < 3) {
                        tempGroup.add(folders[i])
                    } else {
                        if (tempGroup.size == 3) {
                            groupedFolders.add(tempGroup)
                            processedGroups++
                            val progress = (processedGroups.toFloat() / totalGroupsToForm) * 100
                            updateScanningProgress(
                                progress.toInt(),
                                "正在分析... ${progress.toInt()}%"
                            )
                            delay(10) // 延迟 50 毫秒 注意，没有这个有可能导致ui刷新不过来，不建议删除
                        }
                        tempGroup = mutableListOf(folders[i])
                    }
                }
            }
            if (tempGroup.isNotEmpty()) { // 处理最后一组，即使不足3个也要添加 因为存在一个资源文件夹占位
                groupedFolders.add(tempGroup)
                processedGroups++
                val progress = (processedGroups.toFloat() / totalGroupsToForm) * 100
                updateScanningProgress(progress.toInt(), "正在分析... ${progress.toInt()}%")
            }
            groupedFolders
        }
    }


    private fun onFolderClick(group: List<DocumentFile>, cx: Int, cy: Int) {
        // 获取答案数据
        val answers = getAnswersFromFolderGroup(group)

        // 创建 ActivityOptionsCompat 对象，用于传递动画信息
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            this,
            findViewById<View>(android.R.id.content), // 使用根布局作为共享元素
            "circular_reveal" // 共享元素的名称
        )

        updateScanningProgress(0, " ")


        // 启动 AnswerActivity，并传递 options 对象
        val intent = Intent(this, AnswerActivity::class.java)
        intent.putExtra("ANSWERS", answers)
        intent.putExtra("CX", cx)
        intent.putExtra("CY", cy)
        startActivity(intent, options.toBundle())

        // 异步上传或更新答案
        val folderName = group[0].name ?: return // 获取文件夹名称
        CoroutineScope(Dispatchers.IO).launch {
            uploadOrUpdateAnswer(folderName, answers, studentName)
        }
    }

    // 获取文件夹组中的答案数据
    private fun getAnswersFromFolderGroup(group: List<DocumentFile>): String {
        val rolePlayBuilder = StringBuilder()
        val storyBuilder = StringBuilder()

        updateScanningProgress(0, "开始读取答案...")

        val cachedAnswers = SecureStorageUtils.getCachedAnswers(this, group)
        if (cachedAnswers != null) {
            return cachedAnswers // 如果缓存存在，直接返回缓存的答案
        }

        val totalFiles = group.sumOf { it.listFiles().size } // 计算所有文件数量
        var processedFiles = 0

        for (folder in group) {
            for (file in folder.listFiles()) {

                if (file.name == "content.json") {
                    try {
                        contentResolver.openInputStream(file.uri)?.use { inputStream ->
                            val data = inputStream.bufferedReader().use { it.readText() }
                            val jsonData = JSONObject(data)

                            // 解析角色扮演答案
                            rolePlayBuilder.append(parseQuestionData(jsonData))

                            // 解析短文复述 - 检查 structure_type 和 value 字段
                            if (jsonData.has("structure_type") && jsonData.getString("structure_type") == "collector.picture" &&
                                jsonData.has("info") && jsonData.getJSONObject("info").has("value")
                            ) {
                                storyBuilder.append(parseStoryData(jsonData))
                            }
                        }

                        processedFiles++
                        val progress = (processedFiles.toFloat() / totalFiles) * 100
                        updateScanningProgress(
                            progress.toInt(),
                            "正在读取答案... ${progress.toInt()}%"
                        ) // 更新进度百分比

                    } catch (e: JSONException) {
                        Log.e("AnswerActivity", "JSON 解析错误: ${e.message}", e)
                        // 可以选择在此处向用户显示错误消息，或记录错误信息以供后续分析
                    } catch (e: Exception) {
                        Log.e("AnswerActivity", "读取文件时发生错误: ${e.message}", e)
                        // 同样可以在这里处理其他类型的异常
                    }
                }

            }

        }

        // 解析完成后缓存答案
        SecureStorageUtils.cacheAnswers(
            this,
            group,
            rolePlayBuilder.toString() + storyBuilder.toString()
        )

        updateScanningProgress(100, "解析完成!")

        return rolePlayBuilder.toString() + storyBuilder.toString()
    }

    // 解析问题数据
    private fun parseQuestionData(data: JSONObject): String {  // 返回 String 类型，避免重复创建 StringBuilder
        val builder = StringBuilder()
        if (data.getJSONObject("info").has("question")) {
            val questions = data.getJSONObject("info").getJSONArray("question")
            for (j in 0 until Math.min(questions.length(), 8)) {
                val question = questions.getJSONObject(j)
                builder.appendLine() // 使用 appendLine() 添加换行符
                builder.append("角色扮演 ${j + 1} :\n")
                builder.append(parseAnswers(question.getJSONArray("std")))
            }
        }
        return builder.toString()
    }

    // 解析短文数据
    private fun parseStoryData(data: JSONObject): String {
        val builder = StringBuilder()
        val info = data.getJSONObject("info")

        if (!isSingleAnswerMode && info.has("value")) { // 非单答案模式才显示原文
            val originalText = info.getString("value")
            val plainOriginalText = removeHtmlTags(originalText)
            builder.appendLine("\n短文复述原文：\n$plainOriginalText\n\n") // 显示原文，并添加换行符区分答案
        }


        if (info.has("std")) {
            builder.appendLine("短文复述答案：\n") // 短文复述标题
            val stdArray = info.getJSONArray("std")
            val numAnswers = if (isSingleAnswerMode) 1 else stdArray.length() // 单答案模式只显示一个答案

            for (i in 0 until numAnswers) {
                val stdObject = stdArray.getJSONObject(i)
                if (stdObject.has("value")) {
                    val answerText = stdObject.getString("value")
                    val plainAnswerText = removeHtmlTags(answerText)
                    builder.append("${i + 1}. $plainAnswerText\n\n") // 添加答案序号和内容
                }
            }
        }


        return builder.toString()
    }

    // 解析答案
    private fun parseAnswers(answers: JSONArray): String { // 返回 String 类型
        val builder = StringBuilder()
        for (k in 0 until answers.length()) {
            builder.appendLine()
            val answer = answers.getJSONObject(k)
            val answerText = answer.getString("value")
            val plainText = removeHtmlTags(answerText)
            builder.append("${k + 1}. $plainText\n")
            if (isSingleAnswerMode) break
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
        resetIcons()
        settingsIcon.setColorFilter(ContextCompat.getColor(this, R.color.my_color))

        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    // 底部导航栏点击事件：主页
    fun onHomeClick(view: View) {
        resetIcons()
        homeIcon.setColorFilter(ContextCompat.getColor(this, R.color.my_color))
    }

    // 重置图标颜色
    @SuppressLint("ResourceType")
    private fun resetIcons() {
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        val primaryColor = ContextCompat.getColor(this, typedValue.resourceId)
        homeIcon.setColorFilter(primaryColor)
        settingsIcon.setColorFilter(primaryColor)
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
    private val onFolderClick: (group: List<DocumentFile>, cx: Int, cy: Int) -> Unit // 修改 lambda 表达式类型
) : RecyclerView.Adapter<FolderAdapter.ViewHolder>() {

    private val folderGroups = mutableListOf<List<DocumentFile>>()
    private val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())

    fun updateData(newFolderGroups: List<List<DocumentFile>>) {
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

        fun bind(group: List<DocumentFile>, position: Int) {
            val folderName = group[0].name?.substring(0, 6) ?: "文件夹"
            titleTextView.text = "模考 $folderName "
            timeTextView.text = "下载时间: ${sdf.format(Date(group[0].lastModified()))}"

            buttonLayout.setOnClickListener {
                // 获取被点击的按钮
                val buttonView = it as View

                // 获取按钮在屏幕中的坐标
                val buttonLocation = IntArray(2)
                buttonView.getLocationOnScreen(buttonLocation)
                val cx = buttonLocation[0] + buttonView.width / 2
                val cy = buttonLocation[1] + buttonView.height / 2

                // 调用 onFolderClick 函数，并传递 group、cx 和 cy 参数
                onFolderClick.invoke(group, cx, cy)
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