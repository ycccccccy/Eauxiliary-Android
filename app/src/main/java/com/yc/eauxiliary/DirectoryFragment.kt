import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.yc.eauxiliary.KEY_DIRECTORY_URI
import com.yc.eauxiliary.MainActivity
import com.yc.eauxiliary.R
import com.yc.eauxiliary.SHARED_PREFS_NAME

class DirectoryFragment : Fragment() {

    private var directoryUri: Uri? = null
    private val REQUEST_CODE_DOCUMENT_TREE = 2


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_directory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        view.findViewById<Button>(R.id.select_directory_button).setOnClickListener {
            requestDocumentTreeUri()
        }


    }


    private fun requestDocumentTreeUri() {
        val uriBuilder = Uri.Builder()
            .scheme("content")
            .authority("com.android.externalstorage.documents")
            .appendPath("tree")
            .appendPath("primary:A\\u200Bndroid/data")
            .appendPath("document")
            .appendPath("primary:A\\u200Bndroid/data/com.ets100.secondary")
        directoryUri = uriBuilder.build()

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, directoryUri)
        startActivityForResult(intent, REQUEST_CODE_DOCUMENT_TREE)

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CODE_DOCUMENT_TREE && resultCode == Activity.RESULT_OK) {

            // 处理 Document Tree URI 请求结果
            handleDocumentTreeUriResult(resultData)


        }
    }


    @SuppressLint("SetTextI18n")
    private fun handleDocumentTreeUriResult(resultData: Intent?) {
        directoryUri = resultData?.data

        view?.findViewById<TextView>(R.id.selected_directory_textview)?.apply {
            visibility = View.VISIBLE
            text = "已选择目录: ${directoryUri?.path}"
        }

        // 修改为永久请求
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        directoryUri?.let {
            requireContext().contentResolver.takePersistableUriPermission(it, takeFlags)

            // 保存 URI
            val sharedPreferences = requireContext().getSharedPreferences(
                SHARED_PREFS_NAME,
                AppCompatActivity.MODE_PRIVATE
            )
            val editor = sharedPreferences.edit()
            editor.putString(KEY_DIRECTORY_URI, it.toString())
            editor.apply()
        }
        startMainActivity()
    }


    // 启动MainActivity并结束OnboardingActivity
    private fun startMainActivity() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

}