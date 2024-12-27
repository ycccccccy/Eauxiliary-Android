import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.yc.eauxiliary.KEY_DIRECTORY_URI
import com.yc.eauxiliary.MainActivity
import com.yc.eauxiliary.R
import com.yc.eauxiliary.SHARED_PREFS_NAME
import java.io.File

class DirectoryFragment : Fragment() {

    private var directoryUri: Uri? = null
    private val REQUEST_CODE_DOCUMENT_TREE = 2
    private val TARGET_PACKAGE_NAME = "com.ets100.secondary"
    private val ZERO_WIDTH_SPACE = "\u200B"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_directory, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.select_directory_button).setOnClickListener {
            // 使用 File 的方式获取路径
            val targetDirectoryPath = getTargetPathWithWorkaround()
            if (targetDirectoryPath != null) {
                saveDirectoryPathAndStartMainActivity(targetDirectoryPath)
            } else {
                // 处理无法获取路径的情况，例如可以提示用户
                view.findViewById<TextView>(R.id.selected_directory_textview).apply {
                    visibility = View.VISIBLE
                    text = "无法获取目标路径，请手动选择或检查权限"
                }
            }
        }
    }

    private fun getTargetPathWithWorkaround(): String? {
        // 尝试直接构建路径
        val directPath =
            "${Environment.getExternalStorageDirectory().absolutePath}/A${ZERO_WIDTH_SPACE}ndroid/data/$TARGET_PACKAGE_NAME"
        if (File(directPath).exists()) {
            return directPath
        }
        return null
    }

    private fun saveDirectoryPathAndStartMainActivity(path: String) {
        // 保存路径到 SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences(
            SHARED_PREFS_NAME,
            AppCompatActivity.MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()
        editor.putString(KEY_DIRECTORY_URI, path)
        editor.apply()

        // 显示已选择的目录路径
        view?.findViewById<TextView>(R.id.selected_directory_textview)?.apply {
            visibility = View.VISIBLE
            text = "已选择目录: $path"
        }

        // 启动 MainActivity
        startMainActivity()
    }

    // 启动MainActivity并结束OnboardingActivity
    private fun startMainActivity() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }
}