package com.lukael.oled_fpm.activity.illumination.captureoption

import java.io.Serializable

enum class CaptureMode(val displayName: String): Serializable {
    BrightFieldCapture("BF"),
    DarkFieldCapture("DF"),
    PhaseCapture("Phase"),
    MonoReconstruction("FP(Mono)"),
    RGBReconstruction("FP(RGB)")
}