import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yc.eauxiliary.R
import com.yc.eauxiliary.REQUEST_CODE_STORAGE


class PermissionFragment : Fragment() {
    private lateinit var permissionStatusTextView: TextView // 用于显示权限状态

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_permission, container, false)
        permissionStatusTextView = view.findViewById(R.id.permission_status_textview)
        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (hasAllPermissions()) { // 检查是否已经拥有所有权限
            showPermissionsGranted() // 显示“已完成授权”
        } else {
            checkPermissions() // 请求权限
        }
    }

    private fun hasAllPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        } else {
            return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivityForResult(intent, REQUEST_CODE_STORAGE)
            } else {
                permissionStatusTextView.visibility = View.VISIBLE  // 显示 TextView
                permissionStatusTextView.text = "已完成授权，前往下一项吧"
                navigateToNextStep()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_CODE_STORAGE
                )
            } else {
                permissionStatusTextView.visibility = View.VISIBLE  // 显示 TextView
                permissionStatusTextView.text = "已完成授权，前往下一项吧"
                navigateToNextStep()
            }
        }

    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_STORAGE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                navigateToNextStep()
            } else {
                // 处理权限请求被拒绝的情况，显示对话框告诉用户
                showPermissionDeniedDialog()
            }
        }


    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                navigateToNextStep()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun navigateToNextStep() {
        permissionStatusTextView.visibility = View.VISIBLE  // 显示 TextView
        permissionStatusTextView.text = "已完成授权，前往下一项吧"
        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.view_pager)
        viewPager.currentItem = 1 // 切换到下一个 Fragment
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("权限被拒绝")
            .setMessage("应用需要这些权限才能正常工作。请在设置中手动授予所有文件权限")
            .setPositiveButton("前往设置") { _, _ ->
                // 打开应用设置页面
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("退出") { _, _ ->
                // 处理取消的情况，直接关闭应用
                requireActivity().finish()
            }
            .show()
    }

    private fun showPermissionsGranted() {
        permissionStatusTextView.visibility = View.VISIBLE  // 显示 TextView
        permissionStatusTextView.text = "已完成授权，前往下一项吧"


        // 延迟一段时间后切换到下一个 Fragment
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextStep()
        }, 500)
    }


}