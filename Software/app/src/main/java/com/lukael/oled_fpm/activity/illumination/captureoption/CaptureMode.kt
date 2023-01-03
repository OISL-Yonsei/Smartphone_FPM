package com.lukael.oled_fpm.activity.illumination.captureoption

import com.lukael.oled_fpm.constant.DefaultCaptureSettings
import java.io.Serializable

enum class CaptureMode(val displayName: String, val defaultSetting: CaptureSettings) : Serializable {
    CaptureBrightField("Capture(BF)", DefaultCaptureSettings.CaptureBrightField),
    CaptureDarkField("Capture(DF)", DefaultCaptureSettings.CaptureDarkField),
    CapturePhase("Capture(Phase)", DefaultCaptureSettings.CapturePhase),
    ReconstructionMono("FP(Mono)", DefaultCaptureSettings.ReconstructionMono),
    ReconstructionRGB("FP(RGB)", DefaultCaptureSettings.ReconstructionRGB);

    val isReconstructMode get() = this == ReconstructionRGB || this == ReconstructionMono
    val isCaptureMode get() = this == CaptureBrightField || this == CaptureDarkField || this == CapturePhase
}
