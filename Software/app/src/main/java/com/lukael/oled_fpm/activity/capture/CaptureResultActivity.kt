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

            tempFile1?.let {
                fileSaver.saveTempImage(applicationContext, it, "file_1.jpg")
            }

            tempFile2?.let {
                fileSaver.saveTempImage(applicationContext, it, "file_2.jpg")
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
    }
}