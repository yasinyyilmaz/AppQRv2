package com.yasinyilmaz.appqr2.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yasinyilmaz.appqr2.data.model.Device
import com.yasinyilmaz.appqr2.data.repository.DeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceViewModel(private val repository: DeviceRepository) : ViewModel() {

    private val _deviceList = MutableLiveData<List<Device>>()
    val deviceList: LiveData<List<Device>> = _deviceList

    init {
        loadDevices()
    }

    fun loadDevices() {
        viewModelScope.launch {
            val devices = withContext(Dispatchers.IO) {
                repository.getAllDevices()
            }
            _deviceList.value = devices
            Log.d("DeviceViewModel", "loadDevices: Tüm cihazlar yüklendi. Cihaz sayısı: ${devices.size}")
            devices.forEach {
                Log.d("DeviceViewModel", "loadDevices: Cihaz: ${it.deviceName}")
            }
        }
    }

    fun addDevice(device: Device) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertDevice(device)
            loadDevices()
            Log.d("DeviceViewModel", "addDevice: Yeni cihaz eklendi: ${device.deviceName}")
        }
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDeviceById(deviceId)
            loadDevices()
            Log.d("DeviceViewModel", "deleteDevice: Cihaz silindi. ID: $deviceId")
        }
    }

    fun updateDeviceName(deviceId: String, newDeviceName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDeviceName(deviceId, newDeviceName)
            loadDevices()
            Log.d("DeviceViewModel", "updateDeviceName: Cihaz adı güncellendi. ID: $deviceId, Yeni Ad: $newDeviceName")
        }
    }
}