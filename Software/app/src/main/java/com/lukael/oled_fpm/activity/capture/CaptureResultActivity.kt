package com.lukael.oled_fpm.activity.capture

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.lukael.oled_fpm.R
import com.lukael.oled_fpm.databinding.ActivityCaptureResultBinding
import com.lukael.oled_fpm.util.FileSaver
import kotlinx.android.synthetic.main.action_bar.*
import java.text.SimpleDateFormat
import java.util.*

class CaptureResultActivity : AppCompatActivity() {
    private val viewModel: CaptureResultViewModel by viewModels()
    private val fileSaver = FileSaver()

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
            val tempFile1 = viewModel.cacheFile1LiveData.value
            val tempFile2 = viewModel.cacheFile2LiveData.value

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREAN).format(Date())
            val captureMode = viewModel.captureMode
            val modeString = captureMode?.displayName ?: "unknown"
            val extString = ".jpg"
            var fileName = "${modeString}_${timeStamp}"

            tempFile1?.let {
                if (tempFile2 != null) fileName += "_1"
                fileName += extString
                fileSaver.saveTempImage(applicationContext, it, fileName)
            }

            tempFile2?.let {
                fileName += "_2"
                fileName += extString
                fileSaver.saveTempImage(applicationContext, it, fileName)
            }
        }

        back_button.setOnClickListener {
            finish()
        }
    }

    private fun initObserve(binding: ActivityCaptureResultBinding) {
        viewModel.cacheFile1LiveData.observe(this) {
            Glide.with(this).load(it).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(binding.ivTopImage) // TODO check for memory/disk cache
        }
        viewModel.cacheFile2LiveData.observe(this) {
            Glide.with(this).load(it).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(binding.ivBottomImage)
        }
    }

    companion object {
        const val FILE_PATH_1 = "file_path_1"
        const val FILE_PATH_2 = "file_path_2"
        const val CAPTURE_MODE = "capture_mode"
    }
}