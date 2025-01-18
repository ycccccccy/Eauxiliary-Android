
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yc.eauxiliary.R

class PermissionFragment : Fragment() {

    private lateinit var welcomeTextView: TextView
    private val REQUEST_CODE_MANAGE_ALL_FILES = 1022

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_permission, container, false)
        welcomeTextView = view.findViewById(R.id.welcome_textview)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        welcomeTextView.visibility = View.VISIBLE
        welcomeTextView.text = "欢迎使用本应用，点击屏幕以继续"

        // 添加点击事件，点击后请求权限
        view.setOnClickListener {
            requestAllFilesAccessPermission()
        }
    }

    private fun requestAllFilesAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivityForResult(intent, REQUEST_CODE_MANAGE_ALL_FILES)
            } else {
                // 已经拥有权限
                showPermissionGrantedAndAllowNavigation()
            }
        } else {
            // Android 11 以下
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_MANAGE_ALL_FILES
                )
            } else {
                // 已经拥有权限
                showPermissionGrantedAndAllowNavigation()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_MANAGE_ALL_FILES && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // 用户已授权
                showPermissionGrantedAndAllowNavigation()
            } else {
                // 用户未授权
                showPermissionDeniedDialog()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_MANAGE_ALL_FILES) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户已授权
                showPermissionGrantedAndAllowNavigation()
            } else {
                // 用户未授权
                showPermissionDeniedDialog()
            }
        }
    }

    private fun showPermissionGrantedAndAllowNavigation() {
        welcomeTextView.text = "已获得权限，点击跳转到下一步"
        view?.setOnClickListener {
            navigateToNextStep()
        }
    }

    private fun navigateToNextStep() {
        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.view_pager)
        viewPager.currentItem = 1 // 切换到下一个 Fragment
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("权限被拒绝")
            .setMessage("应用需要“所有文件”的访问权限才能正常工作。请在设置中手动授予权限。")
            .setPositiveButton("前往设置") { _, _ ->
                // 打开应用设置页面
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("退出") { _, _ ->
                // 退出应用
                requireActivity().finish()
            }
            .show()
    }
}