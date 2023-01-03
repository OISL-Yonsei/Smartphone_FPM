package com.lukael.oled_fpm.activity.illumination.captureoption

import android.graphics.Color
import java.io.Serializable

class CaptureSettings private constructor() : Serializable {
    var dotsInRow: Int
        get() = (dotsInRowUsage as? CaptureOption.Use)?.value?.value ?: 0
        set(value) {
            dotsInRowUsage = CaptureOption.Use(CaptureSettingType.DotsInRow(value))
        }
    var dotRadius: Int
        get() = (dotRadiusUsage as? CaptureOption.Use)?.value?.value ?: 0
        set(value) {
            dotRadiusUsage = CaptureOption.Use(CaptureSettingType.DotRadius(value))
        }
    var dotInnerRadius: Int
        get() = (dotInnerRadiusUsage as? CaptureOption.Use)?.value?.value ?: 0
        set(value) {
            dotInnerRadiusUsage = CaptureOption.Use(CaptureSettingType.DotInnerRadius(value))
        }
    var stepSize: Int
        get() = (stepSizeUsage as? CaptureOption.Use)?.value?.value ?: 0
        set(value) {
            stepSizeUsage = CaptureOption.Use(CaptureSettingType.StepSize(value))
        }
    var exposureTime: Long
        get() = (exposureTimeUsage as? CaptureOption.Use)?.value?.value ?: 0
        set(value) {
            exposureTimeUsage = CaptureOption.Use(CaptureSettingType.ExposureTime(value))
        }
    var centerX: Int
        get() = (centerXUsage as? CaptureOption.Use)?.value?.value ?: 0
        set(value) {
            centerXUsage = CaptureOption.Use(CaptureSettingType.CenterX(value))
        }
    var centerY: Int
        get() = (centerYUsage as? CaptureOption.Use)?.value?.value ?: 0
        set(value) {
            centerYUsage = CaptureOption.Use(CaptureSettingType.CenterY(value))
        }
    var sampleHeight: Int
        get() = (sampleHeightUsage as? CaptureOption.Use)?.value?.value ?: 0
        set(value) {
            sampleHeightUsage = CaptureOption.Use(CaptureSettingType.SampleHeight(value))
        }
    var illuminationColor: CaptureSettingType.IlluminationColor
        get() = (illuminationColorUsage as? CaptureOption.Use)?.value
            ?: CaptureSettingType.IlluminationColor.Green
        set(value) {
            illuminationColorUsage = CaptureOption.Use(value)
        }
    var illuminationMode: CaptureSettingType.IlluminationMode
        get() = (illuminationModeUsage as? CaptureOption.Use)?.value
            ?: CaptureSettingType.IlluminationMode.Uniform
        set(value) {
            illuminationModeUsage = CaptureOption.Use(value)
        }

    private var dotsInRowUsage: CaptureOption<CaptureSettingType.DotsInRow> = CaptureOption.NotUse
    private var dotRadiusUsage: CaptureOption<CaptureSettingType.DotRadius> = CaptureOption.NotUse
    private var dotInnerRadiusUsage: CaptureOption<CaptureSettingType.DotInnerRadius> =
        CaptureOption.NotUse
    private var stepSizeUsage: CaptureOption<CaptureSettingType.StepSize> = CaptureOption.NotUse
    private var exposureTimeUsage: CaptureOption<CaptureSettingType.ExposureTime> =
        CaptureOption.NotUse
    private var centerXUsage: CaptureOption<CaptureSettingType.CenterX> = CaptureOption.NotUse
    private var centerYUsage: CaptureOption<CaptureSettingType.CenterY> = CaptureOption.NotUse
    private var sampleHeightUsage: CaptureOption<CaptureSettingType.SampleHeight> =
        CaptureOption.NotUse
    private var illuminationColorUsage: CaptureOption<CaptureSettingType.IlluminationColor> =
        CaptureOption.NotUse
    private var illuminationModeUsage: CaptureOption<CaptureSettingType.IlluminationMode> =
        CaptureOption.NotUse

    fun isUseDotsInRow() = dotsInRowUsage is CaptureOption.Use
    fun isUseDotRadius() = dotRadiusUsage is CaptureOption.Use
    fun isUseDotInnerRadius() = dotInnerRadiusUsage is CaptureOption.Use
    fun isUseStepSize() = stepSizeUsage is CaptureOption.Use
    fun isUseExposureTime() = exposureTimeUsage is CaptureOption.Use
    fun isUseCenterX() = centerXUsage is CaptureOption.Use
    fun isUseCenterY() = centerYUsage is CaptureOption.Use
    fun isUseSampleHeight() = sampleHeightUsage is CaptureOption.Use
    fun isUseIlluminationColor() = illuminationColorUsage is CaptureOption.Use
    fun isUseIlluminationMode() = illuminationModeUsage is CaptureOption.Use

    class Builder {
        private val captureSetting = CaptureSettings()
        fun build() = captureSetting

        fun dotsInRow(value: Int) = this.apply {
            captureSetting.dotsInRow = value
        }

        fun dotRadius(value: Int) = this.apply {
            captureSetting.dotRadius = value
        }

        fun dotInnerRadius(value: Int) = this.apply {
            captureSetting.dotInnerRadius = value
        }

        fun stepSize(value: Int) = this.apply {
            captureSetting.stepSize = value
        }

        fun exposureTime(value: Long) = this.apply {
            captureSetting.exposureTime = value
        }

        fun centerX(value: Int) = this.apply {
            captureSetting.centerX = value
        }

        fun centerY(value: Int) = this.apply {
            captureSetting.centerY = value
        }

        fun sampleHeight(value: Int) = this.apply {
            captureSetting.sampleHeight = value
        }

        fun illuminationColor(value: CaptureSettingType.IlluminationColor) = this.apply {
            captureSetting.illuminationColor = value
        }

        fun illuminationMode(value: CaptureSettingType.IlluminationMode) = this.apply {
            captureSetting.illuminationMode = value
        }
    }

    companion object {
        val ReconstructionMono
            get() = Builder().dotsInRow(3).dotRadius(15).stepSize(35).exposureTime(200000)
                .centerX(878).centerY(678).sampleHeight(25)
                .illuminationMode(CaptureSettingType.IlluminationMode.Uniform)
                .illuminationColor(CaptureSettingType.IlluminationColor.Green).build()

        val ReconstructionRGB
            get() = Builder().dotsInRow(3).dotRadius(15).stepSize(35).exposureTime(200000)
                .centerX(878).centerY(678).sampleHeight(25)
                .illuminationMode(CaptureSettingType.IlluminationMode.Uniform).build()

        val CaptureBrightField
            get() = Builder().dotRadius(100).exposureTime(200000).centerX(878).centerY(678)
                .sampleHeight(25).build()

        val CaptureDarkField
            get() = Builder().dotRadius(100).dotInnerRadius(50).exposureTime(200000).centerX(878)
                .centerY(678).sampleHeight(25).build()

        val CapturePhase
            get() = Builder().dotRadius(100).exposureTime(200000).centerX(878).centerY(678)
                .sampleHeight(25).build()
    }
}

sealed class CaptureOption<out T : CaptureSettingType> : Serializable {
    data class Use<T : CaptureSettingType>(var value: T) : CaptureOption<T>()
    object NotUse : CaptureOption<Nothing>()

    fun isEnabled() = this is Use
}

sealed class CaptureSettingType: Serializable {
    data class DotsInRow(val value: Int) : CaptureSettingType()
    data class DotRadius(val value: Int) : CaptureSettingType()
    data class DotInnerRadius(val value: Int) : CaptureSettingType()
    data class StepSize(val value: Int) : CaptureSettingType()
    data class ExposureTime(val value: Long) : CaptureSettingType()
    data class CenterX(val value: Int) : CaptureSettingType()
    data class CenterY(val value: Int) : CaptureSettingType()
    data class SampleHeight(val value: Int) : CaptureSettingType()

    sealed class IlluminationColor : CaptureSettingType() {
        object Red : IlluminationColor()
        object Green : IlluminationColor()
        object Blue : IlluminationColor()
        object White : IlluminationColor()

        fun toColorInt(): Int = when (this) {
            Blue -> Color.BLUE
            Green -> Color.GREEN
            Red -> Color.RED
            White -> Color.WHITE
        }
    }

    sealed class IlluminationMode : CaptureSettingType() {
        object Uniform : IlluminationMode()
        object KUniform : IlluminationMode()
        object NonUniform : IlluminationMode()
    }
}