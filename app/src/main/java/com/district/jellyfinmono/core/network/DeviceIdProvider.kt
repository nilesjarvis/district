package com.district.jellyfinmono.core.network

import android.content.Context
import java.util.UUID

interface DeviceIdProvider {
    fun deviceId(): String
}

class AndroidDeviceIdProvider(private val context: Context) : DeviceIdProvider {
    private val preferences = context.applicationContext.getSharedPreferences("device_identity", Context.MODE_PRIVATE)

    override fun deviceId(): String {
        preferences.getString(KEY_DEVICE_ID, null)?.let { return it }
        val generated = UUID.randomUUID().toString()
        preferences.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
    }
}

class FixedDeviceIdProvider(private val id: String) : DeviceIdProvider {
    override fun deviceId(): String = id
}
