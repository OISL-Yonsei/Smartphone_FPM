package com.lukael.oled_fpm.activity.capture

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.lukael.oled_fpm.R
import com.lukael.oled_fpm.databinding.ActivityCaptureResultBinding

class CaptureResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityCaptureResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView(binding)
        loadImages(binding)
    }

    private fun initView(binding: ActivityCaptureResultBinding) {
        supportActionBar?.apply {
            displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
            setCustomView(R.layout.action_bar)
        }

        binding.btnSave.setOnClickListener {

        }
    }

    private fun loadImages(binding: ActivityCaptureResultBinding) {
        val filePath1 = intent.extras?.getString(FILE_PATH_1)
        val filePath2 = intent.extras?.getString(FILE_PATH_2)

        Glide.with(this).load(filePath1).into(binding.ivTopImage)
        Glide.with(this).load(filePath2).into(binding.ivBottomImage)
    }

    companion object {
        const val FILE_PATH_1 = "file_path_1"
        const val FILE_PATH_2 = "file_path_2"
    }
}