package com.lukael.oled_fpm.model

import com.lukael.oled_fpm.activity.illumination.captureoption.CaptureMode
import java.io.Serializable

class CaptureSetting(
    var dotsInRow: Int,
    var dotRadius: Int,
    var dotInnerRadius: Int,
    var stepSize: Int,
    var exposureTime: Long,
    var centerX: Int,
    var centerY: Int,
    var sampleheight: Int,
    var colorCode: Int,
    var illuminationMode: Int,
    var captureMode: CaptureMode
) : Serializable {
    companion object {
        val Default = CaptureSetting(
            3,
            20,
            10,
            35,
            200000,
            878,
            678,
            24,
            1,
            0,
            CaptureMode.ReconstructionMono
        )
    }
}