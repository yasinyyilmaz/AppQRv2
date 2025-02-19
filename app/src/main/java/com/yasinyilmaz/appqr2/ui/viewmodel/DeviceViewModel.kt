package com.yasinyilmaz.appqr2.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yasinyilmaz.appqr2.data.model.Device
import com.yasinyilmaz.appqr2.data.repository.DeviceRepository
import kotlinx.coroutines.launch

class DeviceViewModel(private val deviceRepository: DeviceRepository) : ViewModel() {

    private val _deviceList = MutableLiveData<List<Device>>()
    val deviceList: LiveData<List<Device>> = _deviceList

    init {
        viewModelScope.launch {
            deviceRepository.syncLocalDevicesToFirebase()
        }
    }

    fun loadDevices() {
        viewModelScope.launch {
            _deviceList.value = deviceRepository.getAllDevices()
        }
    }

    fun addDevice(device: Device) {
        viewModelScope.launch {
            deviceRepository.insertDevice(device)
            loadDevices() // Cihaz eklendikten sonra listeyi yenile
        }
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            deviceRepository.deleteDeviceById(deviceId)
            loadDevices() // Cihaz silindikten sonra listeyi yenile
        }
    }

    fun updateDeviceSwitchState(deviceId: String, isChecked: Boolean) {
        viewModelScope.launch {
            deviceRepository.updateDeviceSwitchState(deviceId, isChecked)
            loadDevices() // Switch durumu güncellendikten sonra listeyi yenile
        }
    }

    fun updateDeviceName(deviceId: String, newDeviceName: String) {
        viewModelScope.launch {
            deviceRepository.updateDeviceName(deviceId, newDeviceName)
            loadDevices() // Cihaz adı güncellendikten sonra listeyi yenile
        }
    }
}