package com.lukael.oled_fpm.activity.illumination.captureoption

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CaptureOptionViewModel: ViewModel() {
    val captureModeLiveData: LiveData<CaptureMode?> get() = _captureModeLiveData
    private val _captureModeLiveData = MutableLiveData(CaptureMode.MonoReconstruction)

    fun setCaptureMode(mode: CaptureMode?) {
        _captureModeLiveData.value = mode
    }
}