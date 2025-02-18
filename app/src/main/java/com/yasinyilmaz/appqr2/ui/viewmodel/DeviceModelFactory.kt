package com.yasinyilmaz.appqr2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yasinyilmaz.appqr2.data.repository.DeviceRepository

class DeviceViewModelFactory(private val repository: DeviceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceViewModel::class.java)) {
            return DeviceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}