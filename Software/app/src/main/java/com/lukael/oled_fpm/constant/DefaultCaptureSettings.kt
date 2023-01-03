package com.lukael.oled_fpm.constant

import com.lukael.oled_fpm.activity.illumination.captureoption.CaptureSettingType
import com.lukael.oled_fpm.activity.illumination.captureoption.CaptureSettings

object DefaultCaptureSettings {
    val ReconstructionMono
        get() = CaptureSettings.Builder()
            .dotsInRow(3)
            .dotRadius(15)
            .stepSize(35)
            .exposureTime(200000)
            .centerX(878)
            .centerY(678)
            .sampleHeight(25)
            .illuminationMode(CaptureSettingType.IlluminationMode.Uniform)
            .illuminationColor(CaptureSettingType.IlluminationColor.Green)
            .build()

    val ReconstructionRGB
        get() = CaptureSettings.Builder()
            .dotsInRow(3)
            .dotRadius(15)
            .stepSize(35)
            .exposureTime(200000)
            .centerX(878)
            .centerY(678)
            .sampleHeight(25)
            .illuminationMode(CaptureSettingType.IlluminationMode.Uniform)
            .build()

    val CaptureBrightField
        get() = CaptureSettings.Builder()
            .dotRadius(100)
            .exposureTime(200000)
            .centerX(878)
            .centerY(678)
            .sampleHeight(25)
            .build()

    val CaptureDarkField
        get() = CaptureSettings.Builder()
            .dotRadius(100)
            .dotInnerRadius(50)
            .exposureTime(200000)
            .centerX(878)
            .centerY(678)
            .sampleHeight(25)
            .build()

    val CapturePhase
        get() = CaptureSettings
            .Builder()
            .dotRadius(100)
            .exposureTime(200000)
            .centerX(878)
            .centerY(678)
            .sampleHeight(25)
            .build()
}