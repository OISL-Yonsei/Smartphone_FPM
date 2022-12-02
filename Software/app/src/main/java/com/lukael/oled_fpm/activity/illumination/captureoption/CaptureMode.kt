package com.lukael.oled_fpm.activity.illumination.captureoption

import java.io.Serializable

enum class CaptureMode(val displayName: String, val requiredOptions: List<CaptureSettingType>) : Serializable {
    CaptureBrightField("Capture(BF)", listOf(
        CaptureSettingType.XPos,
        CaptureSettingType.YPos,
        CaptureSettingType.Radius,
        CaptureSettingType.SampleHeight,
        CaptureSettingType.ExposureTime,
        CaptureSettingType.IlluminationColor
    )),
    CaptureDarkField("Capture(DF)", listOf(
        CaptureSettingType.XPos,
        CaptureSettingType.YPos,
        CaptureSettingType.Radius,
        CaptureSettingType.InnerRadius,
        CaptureSettingType.SampleHeight,
        CaptureSettingType.ExposureTime,
        CaptureSettingType.IlluminationColor
    )),
    CapturePhase("Capture(Phase)", listOf(
        CaptureSettingType.XPos,
        CaptureSettingType.YPos,
        CaptureSettingType.Radius,
        CaptureSettingType.SampleHeight,
        CaptureSettingType.ExposureTime,
        CaptureSettingType.IlluminationColor
    )),
    ReconstructionMono("FP(Mono)", listOf(
        CaptureSettingType.DotsInRow,
        CaptureSettingType.XPos,
        CaptureSettingType.YPos,
        CaptureSettingType.Radius,
        CaptureSettingType.SampleHeight,
        CaptureSettingType.StepSize,
        CaptureSettingType.ExposureTime,
        CaptureSettingType.IlluminationColor,
        CaptureSettingType.UniformType
    )),
    ReconstructionRGB("FP(RGB)", listOf(
        CaptureSettingType.DotsInRow,
        CaptureSettingType.XPos,
        CaptureSettingType.YPos,
        CaptureSettingType.Radius,
        CaptureSettingType.SampleHeight,
        CaptureSettingType.StepSize,
        CaptureSettingType.ExposureTime,
        CaptureSettingType.UniformType
    ));

    val isReconstructMode get() = this == ReconstructionRGB || this == ReconstructionMono
    val isCaptureMode get() = this == CaptureBrightField || this == CaptureDarkField || this == CapturePhase
}

enum class CaptureSettingType {
    DotsInRow, Radius, InnerRadius, StepSize, ExposureTime, XPos, YPos, SampleHeight, IlluminationColor, UniformType
}