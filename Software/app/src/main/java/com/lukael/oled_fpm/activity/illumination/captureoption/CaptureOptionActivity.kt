/**
 * @author Jaewoo Lukael Jung
 * @email lukael.jung@yonsei.ac.kr
 * @create date 2019-12-10 21:48:32
 * @modify date 2019-12-10 21:48:32
 * @desc Setting main function
 */
package com.lukael.oled_fpm.activity.illumination.captureoption

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.lukael.oled_fpm.R
import com.lukael.oled_fpm.activity.illumination.IlluminationActivity
import com.lukael.oled_fpm.model.CaptureSetting
import kotlinx.android.synthetic.main.action_bar.*
import kotlinx.android.synthetic.main.activity_captureoption.*

class CaptureOptionActivity : AppCompatActivity() {
    private val viewModel: CaptureOptionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_captureoption)
        initView()
    }

    private fun initView() {
        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setCustomView(R.layout.action_bar)

        start_recon_button.setOnClickListener{
            val captureMode = viewModel.captureModeLiveData.value
            if (captureMode == null) Toast.makeText(this, "Select Capture Mode.", Toast.LENGTH_SHORT).show()
            else startCaptureActivity(captureMode)
        }
        back_button.setOnClickListener { finish() }

        tv_capture_mode.setAdapter(ArrayAdapter(this, R.layout.item_capture_mode, CaptureMode.values().map { it.displayName }))
        tv_capture_mode.onItemClickListener = object: AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val mode = CaptureMode.values()[position]
                viewModel.setCaptureMode(mode)
            }
        }
    }

    private fun startCaptureActivity(captureMode: CaptureMode) {
        val intent = Intent(applicationContext, IlluminationActivity::class.java)
        val captureSetting = CaptureSetting.Default // TODO: default 를 고정으로 묶어서 안전하게 관리

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
        captureSetting.captureMode = captureMode

        // Transfer to next activity
        intent.putExtra(IlluminationActivity.CAPTURE_SETTING, captureSetting)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.fade_out)
    }
}