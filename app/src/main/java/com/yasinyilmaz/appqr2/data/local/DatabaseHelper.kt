package com.yasinyilmaz.appqr2.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.yasinyilmaz.appqr2.data.model.Device

class DatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_DEVICEID TEXT PRIMARY KEY, 
                $COLUMN_DEVICENAME TEXT,
                $COLUMN_RAW_QR_DATA TEXT
            )
        """
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_RAW_QR_DATA TEXT DEFAULT ''")
        }
    }

    fun insertDevice(device: Device) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DEVICEID, device.deviceId)
            put(COLUMN_DEVICENAME, device.deviceName)
            put(COLUMN_RAW_QR_DATA, device.rawQrData)
        }
        db.insert(TABLE_NAME, null, values)
    }

    fun getAllDevices(): List<Device> {
        val deviceList = mutableListOf<Device>()
        val db = readableDatabase
        val cursor: Cursor = db.query(TABLE_NAME, null, null, null, null, null, null)

        cursor.use {
            while (it.moveToNext()) {
                val deviceId = it.getString(it.getColumnIndexOrThrow(COLUMN_DEVICEID))
                val deviceName = it.getString(it.getColumnIndexOrThrow(COLUMN_DEVICENAME))
                val rawQrData = it.getString(it.getColumnIndexOrThrow(COLUMN_RAW_QR_DATA))
                deviceList.add(Device(deviceId, deviceName, rawQrData))
            }
        }
        return deviceList
    }

    fun deleteDeviceById(deviceId: String) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_DEVICEID = ?", arrayOf(deviceId))
    }

    fun updateDeviceName(deviceId: String, newDeviceName: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DEVICENAME, newDeviceName)
        }
        val result = db.update(TABLE_NAME, values, "$COLUMN_DEVICEID = ?", arrayOf(deviceId))
        return result > 0
    }

    companion object {
        private const val DATABASE_NAME = "devices.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_NAME = "devices"
        private const val COLUMN_DEVICEID = "deviceid"
        private const val COLUMN_DEVICENAME = "devicename"
        private const val COLUMN_RAW_QR_DATA = "raw_qr_data"

        @Volatile
        private var INSTANCE: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}