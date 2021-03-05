/**
 * @author Jaewoo Lukael Jung
 * @email lukael.jung@yonsei.ac.kr
 * @create date 2019-12-10 21:48:32
 * @modify date 2019-12-10 21:48:32
 * @desc Setting main function
 */
package com.lukael.oled_fpm.activity.illumination

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.lukael.oled_fpm.R
import com.lukael.oled_fpm.model.CaptureSetting
import kotlinx.android.synthetic.main.action_bar.*
import kotlinx.android.synthetic.main.activity_captureoption.*

class CaptureOptionActivity : AppCompatActivity() {
    private var captureSetting = CaptureSetting(
            3, 20, 35, 200000, 878, 678, 24,1, 0,0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_captureoption)
        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setCustomView(R.layout.action_bar)

        start_mono_button.setOnClickListener{ startCaptureActivity(0) }
        start_rgb_button.setOnClickListener{ startCaptureActivity(1) }
        back_button.setOnClickListener { finish() }
    }

    private fun startCaptureActivity(reconstructMode: Int) {
        val intent = Intent(applicationContext, IlluminationActivity::class.java)

        // Get setting data
        if (editText_row.text.isNotEmpty()) captureSetting.dotsInRow = editText_row.text.toString().toInt()
        if (editText_size.text.isNotEmpty()) captureSetting.dotRadius = editText_size.text.toString().toInt()
        if (editText_step.text.isNotEmpty()) captureSetting.stepSize = editText_step.text.toString().toInt()
        if (editText_exp.text.isNotEmpty()) captureSetting.exposureTime = editText_exp.text.toString().toLong()
        if (editText_center_x.text.isNotEmpty()) captureSetting.centerX = editText_center_x.text.toString().toInt()
        if (editText_center_y.text.isNotEmpty()) captureSetting.centerY = editText_center_y.text.toString().toInt()
        if (editText_sample_height.text.isNotEmpty()) captureSetting.sampleheight = editText_sample_height.text.toString().toInt()
        when (radio_group.checkedRadioButtonId) {
            R.id.radio_red -> captureSetting.colorCode = 0
            R.id.radio_green -> captureSetting.colorCode = 1
            R.id.radio_blue -> captureSetting.colorCode = 2
            R.id.radio_white -> captureSetting.colorCode = 3
        }
        when (capture_radio_group.checkedRadioButtonId) {
            R.id.capture_radio_normal -> captureSetting.illuminationMode = 0
            R.id.capture_radio_uniform -> captureSetting.illuminationMode = 1
            R.id.capture_radio_non_uniform -> captureSetting.illuminationMode = 2
        }
        captureSetting.reconstructMode = reconstructMode

        // Transfer to next activity
        intent.putExtra("capture_setting", captureSetting)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.fade_out)
    }
}