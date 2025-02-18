package com.yasinyilmaz.appqr2.data.repository

import com.yasinyilmaz.appqr2.data.local.DatabaseHelper
import com.yasinyilmaz.appqr2.data.model.Device

class DeviceRepository(private val databaseHelper: DatabaseHelper) {

    fun getAllDevices(): List<Device> {
        return databaseHelper.getAllDevices()
    }

    fun insertDevice(device: Device) {
        databaseHelper.insertDevice(device)
    }

    fun deleteDeviceById(deviceId: String) {
        databaseHelper.deleteDeviceById(deviceId)
    }

    fun updateDeviceName(deviceId: String, newDeviceName: String) {
        databaseHelper.updateDeviceName(deviceId, newDeviceName)
    }
}