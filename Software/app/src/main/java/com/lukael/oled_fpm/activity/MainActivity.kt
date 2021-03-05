package com.lukael.oled_fpm.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.lukael.oled_fpm.R
import com.lukael.oled_fpm.activity.illumination.FocusActivity
import com.lukael.oled_fpm.activity.reconstruct.SelectImageSetActivity
import com.lukael.oled_fpm.activity.test.TestActivity
import kotlinx.android.synthetic.main.activity_select.*

class MainActivity : AppCompatActivity() {
    //Requesting permission.
    private val permissions: Unit
        get() {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //Requesting permission.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)
        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setCustomView(R.layout.action_bar_no_backbutton)
        permissions // permission

        // init buttons
        select_capture_button.setOnClickListener { startIlluminationPart() }
        select_reconstruct_button.setOnClickListener { startReconstructPart() }
    }

    private fun startIlluminationPart() {
        val intent = Intent(applicationContext, FocusActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, R.anim.fade_out)
    }

    private fun startReconstructPart() {
        val intent = Intent(applicationContext, SelectImageSetActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, R.anim.fade_out)
    }
}