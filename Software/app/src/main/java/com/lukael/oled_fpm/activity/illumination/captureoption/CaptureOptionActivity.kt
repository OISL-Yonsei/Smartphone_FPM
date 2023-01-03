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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.lukael.oled_fpm.R
import com.lukael.oled_fpm.activity.illumination.IlluminationActivity
import com.lukael.oled_fpm.databinding.ActivityCaptureoptionBinding
import com.lukael.oled_fpm.extension.setGone
import com.lukael.oled_fpm.extension.setVisible
import com.lukael.oled_fpm.extension.setVisibleOrGone
import kotlinx.android.synthetic.main.action_bar.*
import kotlinx.android.synthetic.main.activity_captureoption.*

class CaptureOptionActivity : AppCompatActivity() {
    private val viewModel: CaptureOptionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityCaptureoptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView(binding)
        initObserve(binding)
    }

    private fun initView(binding: ActivityCaptureoptionBinding) {
        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setCustomView(R.layout.action_bar)

        back_button.setOnClickListener { finish() } // TODO...

        binding.startIlluminationButton.setOnClickListener {
            val captureMode = viewModel.captureModeLiveData.value
            if (captureMode == null) Toast.makeText(
                this,
                "Select Capture Mode.",
                Toast.LENGTH_SHORT
            ).show()
            else startIlluminationCaptureActivity(binding)
        }

        binding.tvCaptureMode.setAdapter(
            ArrayAdapter(
                this,
                R.layout.item_capture_mode,
                CaptureMode.values().map { it.displayName })
        )
        binding.tvCaptureMode.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val mode = CaptureMode.values()[position]
                viewModel.setCaptureMode(mode)
            }
    }

    private fun initObserve(binding: ActivityCaptureoptionBinding) {
        viewModel.captureModeLiveData.observe(this) {
            onUpdateCaptureMode(binding, it)
        }
    }

    private fun onUpdateCaptureMode(
        binding: ActivityCaptureoptionBinding,
        captureMode: CaptureMode?
    ) {
        if (captureMode == null) {
            binding.llSettings.setGone()
            binding.startIlluminationButton.setGone()
            return
        }
        binding.startIlluminationButton.setVisible()
        binding.llSettings.setVisible()

        val defaultSettings = captureMode.defaultSetting

        if (defaultSettings.isUseDotsInRow()) {
            binding.llOptionDotsInRow.setVisible()
            binding.etDotsInRow.setText(defaultSettings.dotsInRow.toString())
        } else {
            binding.llOptionDotsInRow.setGone()
        }

        if (defaultSettings.isUseDotRadius()) {
            binding.llOptionDotRadius.setVisible()
            binding.etDotRadius.setText(defaultSettings.dotRadius.toString())
        } else {
            binding.llOptionDotRadius.setGone()
        }

        if (defaultSettings.isUseCenterX()) {
            binding.llOptionCenterX.setVisible()
            binding.etCenterX.setText(defaultSettings.centerX.toString())
        } else {
            binding.llOptionCenterX.setGone()
        }

        if (defaultSettings.isUseCenterY()) {
            binding.llOptionCenterY.setVisible()
            binding.etCenterY.setText(defaultSettings.centerY.toString())
        } else {
            binding.llOptionCenterY.setGone()
        }

        if (defaultSettings.isUseExposureTime()) {
            binding.llOptionExposureTime.setVisible()
            binding.etExposureTime.setText(defaultSettings.exposureTime.toString())
        } else {
            binding.llOptionExposureTime.setGone()
        }

        if (defaultSettings.isUseDotInnerRadius()) {
            binding.llOptionInnerRadius.setVisible()
            binding.etInnerRadius.setText(defaultSettings.dotInnerRadius.toString())
        } else {
            binding.llOptionInnerRadius.setGone()
        }

        if (defaultSettings.isUseSampleHeight()) {
            binding.llOptionSampleHeight.setVisible()
            binding.etSampleHeight.setText(defaultSettings.sampleHeight.toString())
        } else {
            binding.llOptionSampleHeight.setGone()
        }

        if (defaultSettings.isUseStepSize()) {
            binding.llOptionStepSize.setVisible()
            binding.etStepSize.setText(defaultSettings.stepSize.toString())
        } else {
            binding.llOptionStepSize.setGone()
        }

        if (defaultSettings.isUseIlluminationColor()) {
            binding.rgIlluminationColor.setVisible()
            binding.rgIlluminationColor.check(
                when (defaultSettings.illuminationColor) {
                    CaptureSettingType.IlluminationColor.Blue -> R.id.radio_blue
                    CaptureSettingType.IlluminationColor.Green -> R.id.radio_green
                    CaptureSettingType.IlluminationColor.Red -> R.id.radio_red
                    CaptureSettingType.IlluminationColor.White -> R.id.radio_white
                }
            )
        } else {
            binding.rgIlluminationColor.setGone()
        }

        if (defaultSettings.isUseIlluminationMode()) {
            binding.rgIlluminationMode.setVisible()
            binding.rgIlluminationMode.check(
                when (defaultSettings.illuminationMode) {
                    CaptureSettingType.IlluminationMode.Uniform -> R.id.capture_radio_k_uniform
                    CaptureSettingType.IlluminationMode.KUniform -> R.id.capture_radio_uniform
                    CaptureSettingType.IlluminationMode.NonUniform -> R.id.capture_radio_non_uniform
                }
            )
        } else {
            binding.rgIlluminationMode.setGone()
        }
    }

    private fun startIlluminationCaptureActivity(binding: ActivityCaptureoptionBinding) {
        val intent = Intent(applicationContext, IlluminationActivity::class.java)
        val captureMode = viewModel.captureModeLiveData.value
        val captureSetting = CaptureSettings.ReconstructionMono

        // Get setting data
        captureSetting.apply {
            dotsInRow = binding.etDotsInRow.text.toString().toInt()
            dotRadius = binding.etDotRadius.text.toString().toInt()
            dotInnerRadius = binding.etInnerRadius.text.toString().toInt()
            stepSize = binding.etStepSize.text.toString().toInt()
            exposureTime = binding.etExposureTime.text.toString().toLong()
            centerX = binding.etCenterX.text.toString().toInt()
            centerY = binding.etCenterY.text.toString().toInt()
            sampleHeight = binding.etSampleHeight.text.toString().toInt()
            illuminationColor = when (rg_illumination_color.checkedRadioButtonId) {
                R.id.radio_red -> CaptureSettingType.IlluminationColor.Red
                R.id.radio_green -> CaptureSettingType.IlluminationColor.Green
                R.id.radio_blue -> CaptureSettingType.IlluminationColor.Blue
                R.id.radio_white -> CaptureSettingType.IlluminationColor.White
                else -> error("")
            }
            illuminationMode = when (binding.rgIlluminationMode.checkedRadioButtonId) {
                R.id.capture_radio_uniform -> CaptureSettingType.IlluminationMode.Uniform
                R.id.capture_radio_k_uniform -> CaptureSettingType.IlluminationMode.KUniform
                R.id.capture_radio_non_uniform -> CaptureSettingType.IlluminationMode.NonUniform
                else -> error("")
            }
        }

        // Transfer to next activity
        intent.putExtra(IlluminationActivity.CAPTURE_MODE, captureMode)
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