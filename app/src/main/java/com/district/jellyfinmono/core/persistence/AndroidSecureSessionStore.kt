package com.district.jellyfinmono.core.persistence

import android.content.Context
import android.util.Base64
import com.district.jellyfinmono.domain.AuthSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

class AndroidSecureSessionStore(context: Context) : SessionStore {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("secure_session", Context.MODE_PRIVATE)

    override suspend fun load(): AuthSession? = withContext(Dispatchers.IO) {
        val encrypted = preferences.getString(KEY_PAYLOAD, null) ?: return@withContext null
        val iv = preferences.getString(KEY_IV, null) ?: return@withContext null
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv.decodeBase64()))
            val json = JSONObject(String(cipher.doFinal(encrypted.decodeBase64()), Charsets.UTF_8))
            AuthSession(
                serverUrl = json.getString("serverUrl"),
                accessToken = json.getString("accessToken"),
                userId = json.getString("userId"),
                username = json.optString("username"),
                deviceId = json.getString("deviceId"),
            )
        } catch (_: Exception) {
            preferences.edit().clear().apply()
            null
        }
    }

    override suspend fun save(session: AuthSession) = withContext(Dispatchers.IO) {
        val json = JSONObject()
            .put("serverUrl", session.serverUrl)
            .put("accessToken", session.accessToken)
            .put("userId", session.userId)
            .put("username", session.username)
            .put("deviceId", session.deviceId)
            .toString()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString(KEY_PAYLOAD, encrypted.encodeBase64())
            .putString(KEY_IV, cipher.iv.encodeBase64())
            .apply()
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        preferences.edit().clear().apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun ByteArray.encodeBase64(): String =
        Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.decodeBase64(): ByteArray =
        Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val KEY_ALIAS = "district_jellyfin_session"
        const val KEY_PAYLOAD = "payload"
        const val KEY_IV = "iv"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
