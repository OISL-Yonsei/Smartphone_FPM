package com.lukael.oled_fpm.activity.capture

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.lukael.oled_fpm.R
import com.lukael.oled_fpm.activity.illumination.captureoption.CaptureMode
import com.lukael.oled_fpm.databinding.ActivityCaptureResultBinding
import com.lukael.oled_fpm.util.FileSaver
import kotlinx.android.synthetic.main.action_bar.*
import java.text.SimpleDateFormat
import java.util.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs

class CaptureResultActivity : AppCompatActivity() {
    private val viewModel: CaptureResultViewModel by viewModels()
    private val fileSaver = FileSaver()

    private var bitmap1: Bitmap? = null
    private var bitmap2: Bitmap? = null

    init { OpenCVLoader.initDebug() } // load and init openCV

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityCaptureResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView(binding)
        initObserve(binding)
    }

    private fun initView(binding: ActivityCaptureResultBinding) {
        supportActionBar?.apply {
            displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
            setCustomView(R.layout.action_bar)
        }

        binding.btnSave.setOnClickListener {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREAN).format(Date())
            val captureMode = viewModel.captureMode
            val modeString = captureMode.displayName
            val extString = ".jpg"

            bitmap1?.let {
                var fileName = "${modeString}_${timeStamp}"
                if (bitmap2 != null) fileName += "_1"
                fileName += extString
                fileSaver.saveBmpImage(applicationContext, it, fileName)
            }

            bitmap2?.let {
                var fileName = "${modeString}_${timeStamp}"
                fileName += "_2"
                fileName += extString
                fileSaver.saveBmpImage(applicationContext, it, fileName)
            }
        }

        back_button.setOnClickListener {
            finish()
        }
    }

    private fun initObserve(binding: ActivityCaptureResultBinding) {
        viewModel.cacheFilesLiveData.observe(this) { files ->
            updateBitmaps(binding, files[0], files[1])
        }
    }

    private fun updateBitmaps(binding: ActivityCaptureResultBinding, file1: String?, file2: String?) {
        when (viewModel.captureMode) {
            CaptureMode.CaptureBrightField, CaptureMode.CaptureDarkField -> {
                val mat1 = Imgcodecs.imread(file1, Imgcodecs.IMREAD_COLOR)
                Core.extractChannel(mat1, mat1, 1) // green
                bitmap1 = Bitmap.createBitmap(mat1.cols(), mat1.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(mat1, bitmap1)
                binding.ivTopImage.setImageBitmap(bitmap1)
            }
            CaptureMode.CapturePhase -> {
                val mat1 = Imgcodecs.imread(file1, Imgcodecs.IMREAD_COLOR)
                val mat2 = Imgcodecs.imread(file2, Imgcodecs.IMREAD_COLOR)
                val addMat = Mat.zeros(mat1.cols(), mat1.rows(), CvType.CV_32F)
                val subMat = Mat.zeros(mat1.cols(), mat1.rows(), CvType.CV_32F)

                Core.extractChannel(mat1, mat1, 1) // green
                Core.extractChannel(mat2, mat2, 1) // green

                Core.add(mat1, mat2, addMat)
                Core.subtract(mat1, mat2, subMat)

//            Core.normalize(mat, mat, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC3)
                bitmap1 = Bitmap.createBitmap(mat1.cols(), mat1.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(addMat, bitmap1)
                binding.ivTopImage.setImageBitmap(bitmap1)

//            Core.normalize(mat, mat, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC3)
                bitmap2 = Bitmap.createBitmap(mat2.cols(), mat2.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(subMat, bitmap2)
                binding.ivBottomImage.setImageBitmap(bitmap2)
            }
            else -> {} // do nothing. these cases go to RecnstructActivity, not CaptureResultActivity.
        }
    }

    companion object {
        const val FILE_PATH_1 = "file_path_1"
        const val FILE_PATH_2 = "file_path_2"
        const val CAPTURE_MODE = "capture_mode"
    }
}