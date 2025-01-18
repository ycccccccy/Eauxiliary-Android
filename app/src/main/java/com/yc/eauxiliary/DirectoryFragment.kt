
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private val TARGET_PACKAGE_NAME = "com.ets100.secondary"
    private val ZERO_WIDTH_SPACE = "\u200B"
    private lateinit var resultTextView: TextView

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

        resultTextView = view.findViewById(R.id.selected_directory_textview)
        resultTextView.visibility = View.VISIBLE
        resultTextView.text = "正在自动进行环境准备..."

        // 使用 Handler 延迟执行构建目录操作
        Handler(Looper.getMainLooper()).post {
            buildDirectoryInBackground()
        }
    }

    private fun buildDirectoryInBackground() {
        val targetDirectoryPath = getTargetPathWithWorkaround()
        if (targetDirectoryPath != null) {
            saveDirectoryPath(targetDirectoryPath)
            showSuccessAndNavigate(targetDirectoryPath)
        } else {
            showFailureMessage()
        }
    }

    private fun getTargetPathWithWorkaround(): String? {
        val directPath =
            "${Environment.getExternalStorageDirectory().absolutePath}/A${ZERO_WIDTH_SPACE}ndroid/data/$TARGET_PACKAGE_NAME"
        return if (File(directPath).exists()) directPath else null
    }

    private fun saveDirectoryPath(path: String) {
        val sharedPreferences = requireContext().getSharedPreferences(
            SHARED_PREFS_NAME,
            AppCompatActivity.MODE_PRIVATE
        )
        sharedPreferences.edit().putString(KEY_DIRECTORY_URI, path).apply()
    }

    private fun showSuccessAndNavigate(path: String) {
        requireActivity().runOnUiThread {
            resultTextView.text = "我们已完成了环境准备\n\n3秒后自动前往主界面"
            // 3 秒后跳转到 MainActivity
            Handler(Looper.getMainLooper()).postDelayed({
                startMainActivity()
            }, 3000)
        }
    }

    private fun showFailureMessage() {
        requireActivity().runOnUiThread {
            resultTextView.text = "无法自动构建目录，可能是权限不足或设备不支持。"
        }
    }

    private fun startMainActivity() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }
}