package com.yasinyilmaz.appqr2.data.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.yasinyilmaz.appqr2.data.local.DatabaseHelper
import com.yasinyilmaz.appqr2.data.model.Device
import kotlinx.coroutines.tasks.await

class DeviceRepository(private val databaseHelper: DatabaseHelper) {

    private val database = FirebaseDatabase.getInstance().reference.child("devices")

    fun getAllDevices(): List<Device> {
        return databaseHelper.getAllDevices()
    }

    suspend fun insertDevice(device: Device) {
        // Önce SQLite'e ekle
        databaseHelper.insertDevice(device)

        // Sonra Firebase'e ekle
        try {
            database.child(device.deviceId).setValue(device).await()
        } catch (e: Exception) {
            // Hata yönetimi
            println("Firebase'e eklenirken hata oluştu: ${e.message}")
            // Kullanıcıya geri bildirim ver
            // Örneğin: Toast.makeText(context, "Firebase'e eklenirken hata oluştu", Toast.LENGTH_SHORT).show()
            // İşlemi geri almayı düşünebilirsin
            // Örneğin: databaseHelper.deleteDeviceById(device.deviceId)
        }
    }

    suspend fun deleteDeviceById(deviceId: String) {
        // Önce SQLite'den sil
        databaseHelper.deleteDeviceById(deviceId)

        // Sonra Firebase'den sil
        try {
            database.child(deviceId).removeValue().await()
        } catch (e: Exception) {
            // Hata yönetimi
            println("Firebase'den silinirken hata oluştu: ${e.message}")
            // Kullanıcıya geri bildirim ver
            // Örneğin: Toast.makeText(context, "Firebase'den silinirken hata oluştu", Toast.LENGTH_SHORT).show()
            // İşlemi geri almayı düşünebilirsin
            // Örneğin: databaseHelper.insertDevice(device)
        }
    }

    suspend fun updateDeviceName(deviceId: String, newDeviceName: String) {
        // Önce SQLite'de güncelle
        val isUpdated = databaseHelper.updateDeviceName(deviceId, newDeviceName)

        // Sonra Firebase'de güncelle
        if (isUpdated) {
            try {
                database.child(deviceId).child("deviceName").setValue(newDeviceName).await()
            } catch (e: Exception) {
                // Hata yönetimi
                println("Firebase'de güncellenirken hata oluştu: ${e.message}")
                // Kullanıcıya geri bildirim ver
                // Örneğin: Toast.makeText(context, "Firebase'de güncellenirken hata oluştu", Toast.LENGTH_SHORT).show()
            }
        }
    }
    suspend fun updateDeviceSwitchState(deviceId: String, isOn: Boolean) {
        // Önce SQLite'de güncelle
        val isUpdated = databaseHelper.updateDeviceSwitchState(deviceId, isOn)

        // Sonra Firebase'de güncelle
        if (isUpdated) {
            try {
                database.child(deviceId).child("isOn").setValue(isOn).await()
            } catch (e: Exception) {
                // Hata yönetimi
                println("Firebase'de güncellenirken hata oluştu: ${e.message}")
                // Kullanıcıya geri bildirim ver
                // Örneğin: Toast.makeText(context, "Firebase'de güncellenirken hata oluştu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Firebase'den veri çekme fonksiyonu
    fun fetchDevicesFromFirebase(onSuccess: (List<Device>) -> Unit, onFailure: (Exception) -> Unit) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val devices = mutableListOf<Device>()
                for (deviceSnapshot in snapshot.children) {
                    val device = deviceSnapshot.getValue(Device::class.java)
                    device?.let { devices.add(it) }
                }
                onSuccess(devices)
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure(error.toException())
            }
        })
    }

    suspend fun syncLocalDevicesToFirebase() {
        val localDevices = databaseHelper.getAllDevices()
        for (device in localDevices) {
            try {
                database.child(device.deviceId).setValue(device).await()
                Log.d("DeviceRepository", "syncLocalDevicesToFirebase: Cihaz Firebase'e eklendi: ${device.deviceName}")
            } catch (e: Exception) {
                Log.e("DeviceRepository", "syncLocalDevicesToFirebase: Cihaz Firebase'e eklenirken hata oluştu: ${device.deviceName}, Hata: ${e.message}")
                // Hata durumunda kullanıcıya geri bildirim ver
                // Örneğin: Toast.makeText(context, "Firebase'e eklenirken hata oluştu", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d("DeviceRepository", "syncLocalDevicesToFirebase: Tüm cihazlar Firebase'e eklendi.")
    }
}