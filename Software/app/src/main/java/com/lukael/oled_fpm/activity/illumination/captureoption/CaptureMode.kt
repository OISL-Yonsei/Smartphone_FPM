package com.lukael.oled_fpm.activity.illumination.captureoption

import java.io.Serializable

enum class CaptureMode(val displayName: String, val defaultSetting: CaptureSettings) : Serializable {
    CaptureBrightField("Capture(BF)", CaptureSettings.CaptureBrightField),
    CaptureDarkField("Capture(DF)", CaptureSettings.CaptureDarkField),
    CapturePhase("Capture(Phase)", CaptureSettings.CapturePhase),
    ReconstructionMono("FP(Mono)", CaptureSettings.ReconstructionMono),
    ReconstructionRGB("FP(RGB)", CaptureSettings.ReconstructionRGB);

    val isReconstructMode get() = this == ReconstructionRGB || this == ReconstructionMono
    val isCaptureMode get() = this == CaptureBrightField || this == CaptureDarkField || this == CapturePhase
}
