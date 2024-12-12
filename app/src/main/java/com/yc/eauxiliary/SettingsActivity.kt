package com.yc.eauxiliary

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class SettingsActivity : AppCompatActivity() {
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switchSingleAnswerMode: Switch
    private val REQUEST_CODE = 0
    var directoryUri: Uri? = null
    private var currentSnackbar: Snackbar? = null
    private var isSnackbarShowing = false

    @SuppressLint("RestrictedApi")
    private fun showCustomSnackbar(message: String, type: String, color: Int) {
        // 如果当前有 Snackbar 显示，直接返回，不显示新的 Snackbar
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 读取URI
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val uriString = sharedPreferences.getString("directory_uri", null)
        if (uriString != null) {
            directoryUri = Uri.parse(uriString)
        }

        updateStatusBarTextColor(window)

        findViewById<TextView>(R.id.homeButton).isSelected = false
        findViewById<TextView>(R.id.settingsButton).isSelected = true


        switchSingleAnswerMode = findViewById(R.id.switchSingleAnswerMode)
        val isSingleAnswerMode = sharedPreferences.getBoolean("isSingleAnswerMode", false)
        switchSingleAnswerMode.isChecked = isSingleAnswerMode

        // 设置开关的监听器
        switchSingleAnswerMode.setOnCheckedChangeListener { _, isChecked ->
            // 将修改后的值存入SharedPreferences
            sharedPreferences.edit().putBoolean("isSingleAnswerMode", isChecked).apply()
        }


        val textView = findViewById<TextView>(R.id.hellouser)
        if (directoryUri != null) {
            fetchAndDisplayStudentName(directoryUri!!, textView)
        } else {
            textView.text = ""
        }

    }

    //手册页面
    fun showUserManualDialog(view: View) {
        val intent = Intent(this, Shouce::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    //关于页面
    fun about(view: View) {
        val intent = Intent(this, About::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    //激活
    fun jihuo(view: View) {
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_image_title_input, null)
        val dialogImage = dialogLayout.findViewById<ImageView>(R.id.dialog_image)
        val dialogTitle = dialogLayout.findViewById<TextView>(R.id.dialog_title)
        val dialogInput = dialogLayout.findViewById<EditText>(R.id.dialog_input)

        MaterialAlertDialogBuilder(this)
            .setView(dialogLayout)
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
                val userInput = dialogInput.text.toString()
                val username = getStudentName(this)
                if (username != null && validateInput(userInput, this, username)) {
                    SecureStorageUtils.setActivated(this, true)
                    showCustomSnackbar(
                        "激活成功",
                        "success",
                        resources.getColor(R.color.colorSuccess)
                    )
                } else {
                    showCustomSnackbar(
                        "输入无效或信息异常",
                        "error",
                        resources.getColor(R.color.colorError)
                    )
                }
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    // AES加密函数
    fun aesEncrypt(input: String, key: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKeySpec = SecretKeySpec(key.toByteArray(), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
        val encryptedBytes = cipher.doFinal(input.toByteArray())
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    // 验证输入内容的算法
    fun validateInput(input: String, context: Context, username: String): Boolean {
        val secretKey = "wocaonimagezhizx" // 固定密钥
        val encryptedUsername = aesEncrypt(username, secretKey)
        return input.trim() == encryptedUsername.trim()
    }


    //重新授权
    @RequiresApi(Build.VERSION_CODES.O)
    fun chongxinshouquan(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            } else {

            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    1
                )
            } else {

            }
        }

        val uriBuilder = Uri.Builder()
            .scheme("content")
            .authority("com.android.externalstorage.documents")
            .appendPath("tree")
            .appendPath("primary:A\u200Bndroid/data")
            .appendPath("document")
            .appendPath("primary:A\u200Bndroid/data/com.ets100.secondary")
        directoryUri = uriBuilder.build()

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, directoryUri)
        startActivityForResult(intent, REQUEST_CODE)
    }

    private fun fetchDataAndUpdateUI() {
        Thread {
            val stringBuilder = StringBuilder()

            // 逐级查找目标文件夹
            var currentDirectory = DocumentFile.fromTreeUri(this, directoryUri!!)
            val targetPath =
                listOf("com.ets100.secondary", "files", "Download", "ETS_SECONDARY", "resource")
            for (folderName in targetPath) {
                currentDirectory = currentDirectory?.findFile(folderName)
                if (currentDirectory == null) {
                    stringBuilder.append("出错于：$folderName\n")
                    break
                }
            }

            // 确保定位到了 resource 文件夹
            currentDirectory?.let { resourceDirectory ->
                // 遍历 resource 文件夹下的所有文件和文件夹
                resourceDirectory.listFiles().forEach { file ->
                    // 检查是否是文件夹且名称不是 "common"
                    if (file.isDirectory && file.name != "common") {
                        // 删除文件夹及其内容
                        deleteDirectory(file)
                    }
                }
            }

            runOnUiThread {
                MaterialAlertDialogBuilder(this)
                    .setTitle("提示")
                    .setMessage("删除完成！")
                    .setPositiveButton("好！") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }.start()
    }

    private fun deleteDirectory(directory: DocumentFile) {
        directory.listFiles().forEach { file ->
            if (file.isDirectory) {
                deleteDirectory(file)
            } else {
                file.delete()
            }
        }
        directory.delete()
    }

    fun qingchuhuancun(view: View) {
        MaterialAlertDialogBuilder(this)
            .setTitle("注意")
            .setMessage("这里只是清除本工具内的答案缓存，可能解决答案显示错误的问题，如果仍然错误则需要清除E听说下载的试题，你需要前往E听说应用-我的-清除缓存") // 更正提示信息
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()

                val sharedPrefs = SecureStorageUtils.getSharedPrefs(this)
                if (sharedPrefs != null) {
                    sharedPrefs.edit().clear().apply()
                    showCustomSnackbar(
                        "缓存已清除",
                        "success",
                        resources.getColor(R.color.colorSuccess)
                    )
                    // 可选：在此处添加重新加载数据和更新 UI 的逻辑，如果需要的话
                    fetchDataAndUpdateUI() //如果需要的话，可以在这里重新加载数据和更新UI
                } else {
                    showCustomSnackbar(
                        "清除缓存失败",
                        "error",
                        resources.getColor(R.color.colorError)
                    )
                }
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    //这是一个
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            directoryUri = resultData?.data

            // 修改为永久请求
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(directoryUri!!, takeFlags)

            // 保存URI
            val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("directory_uri", directoryUri.toString())
            editor.apply()
        }
    }


    fun onSettingsClick(view: View) {
        findViewById<TextView>(R.id.homeButton).isSelected = false
        findViewById<TextView>(R.id.settingsButton).isSelected = true
    }

    fun onHomeClick(view: View) {
        findViewById<TextView>(R.id.homeButton).isSelected = true
        findViewById<TextView>(R.id.settingsButton).isSelected = false
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }


    fun saveStudentName(context: Context, studentName: String) {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("student_name", studentName)
        editor.apply()
    }

    fun getStudentName(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("student_name", null)
    }


    @SuppressLint("SetTextI18n")
    fun fetchAndDisplayStudentName(directoryUri: Uri, textView: TextView) {
        CoroutineScope(Dispatchers.IO).launch {
            var studentName = getStudentName(textView.context)
            val stringBuilder = StringBuilder()

            if (studentName == null) {
                // 逐级查找目标文件夹
                var currentDirectory = DocumentFile.fromTreeUri(textView.context, directoryUri)
                val targetPath =
                    listOf("com.ets100.secondary", "files", "Download", "ETS_SECONDARY", "temp")
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
                            val inputStream =
                                textView.context.contentResolver.openInputStream(file.uri)
                            val data =
                                JSONObject(inputStream?.bufferedReader().use { it?.readText() })
                            studentName =
                                data.getJSONArray("data").getJSONObject(0).getString("student_name")
                            saveStudentName(textView.context, studentName) // 保存到 SharedPreferences
                        } catch (e: Exception) {
                            stringBuilder.append("错误: $e\n")
                        }
                    }
                }
            }

            // 随时间而变
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val greeting = when {
                hour < 4 -> "晚上好"
                hour < 12 -> "早上好"
                hour < 14 -> "中午好"
                hour < 18 -> "下午好"
                else -> "晚上好"
            }
            stringBuilder.append("$greeting~ $studentName")

            // 更新UI
            withContext(Dispatchers.Main) {
                textView.text = stringBuilder.toString()
            }
        }
    }


    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}

