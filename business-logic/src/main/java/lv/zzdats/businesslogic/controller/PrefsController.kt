// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.controller

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface PrefsController {
    fun contains(key: String): Boolean
    fun clear(key: String)
    fun clearAll()

    fun setString(key: String, value: String)
    fun setLong(key: String, value: Long)
    fun setBool(key: String, value: Boolean)
    fun setInt(key: String, value: Int)
    fun setPublicString(key: String, value: String)

    fun getString(key: String, defaultValue: String): String
    fun getLong(key: String, defaultValue: Long): Long
    fun getBool(key: String, defaultValue: Boolean): Boolean
    fun getInt(key: String, defaultValue: Int): Int
    fun getPublicString(key: String, defaultValue: String): String
}

class PrefsControllerImpl(private val context: Context) : PrefsController {
    companion object {
        private const val SECURE_PREFS_FILE = "secret_shared_prefs"
        private const val PUBLIC_PREFS_FILE = "public_shared_prefs"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefsKeyEncryptionScheme by lazy {
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
    }

    private val prefsValueEncryptionScheme by lazy {
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    }

    private val publicPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PUBLIC_PREFS_FILE, Context.MODE_PRIVATE)
    }

    private fun getSharedPrefs(): SharedPreferences {
        return try {
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_FILE,
                masterKey,
                prefsKeyEncryptionScheme,
                prefsValueEncryptionScheme
            )
        } catch (t: Throwable) {
            Log.e("PrefsControllerImpl", "Failed to initialize encrypted preferences, using fallback", t)
            context.getSharedPreferences(SECURE_PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    override fun contains(key: String): Boolean {
        return getSharedPrefs().contains(key)
    }

    override fun clear(key: String) {
        getSharedPrefs().edit().remove(key).apply()
    }

    override fun clearAll() {
        getSharedPrefs().edit().clear().apply()
    }

    override fun setString(key: String, value: String) {
        getSharedPrefs().edit().putString(key, value).apply()
    }

    override fun setLong(key: String, value: Long) {
        getSharedPrefs().edit().putLong(key, value).apply()
    }

    override fun setBool(key: String, value: Boolean) {
        getSharedPrefs().edit().putBoolean(key, value).apply()
    }

    override fun setInt(key: String, value: Int) {
        getSharedPrefs().edit().putInt(key, value).apply()
    }

    override fun setPublicString(key: String, value: String) {
        publicPrefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String, defaultValue: String): String {
        return getSharedPrefs().getString(key, defaultValue) ?: defaultValue
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return getSharedPrefs().getLong(key, defaultValue)
    }

    override fun getBool(key: String, defaultValue: Boolean): Boolean {
        return getSharedPrefs().getBoolean(key, defaultValue)
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return getSharedPrefs().getInt(key, defaultValue)
    }

    override fun getPublicString(key: String, defaultValue: String): String {
        return publicPrefs.getString(key, defaultValue) ?: defaultValue
    }
}

interface PrefKeys {
    fun getAppInstanceId(): String
    fun setAppInstanceId(value: String)
    fun getBiometricKey(): String
    fun setBiometricKey(key: String)
    fun getHardwareKey(): String
    fun setHardwareKey(key: String)
    fun getLastDocType(): String
    fun setLastDocType(key: String)
    fun getLastIssuanceMethod(): String
    fun setLastIssuanceMethod(method: String)
    fun getAppActivated(): Boolean
    fun hasAppActivated(): Boolean
    fun setAppActivated(key: Boolean)
    fun getWalletInstanceRegistered(): Boolean
    fun setWalletInstanceRegistered(value: Boolean)
    fun setLanguage(language: String)
    fun getLanguage(): String
    fun setPresentationVendorSelections(value: String)
    fun getPresentationVendorSelections(): String
}

class PrefKeysImpl(
    private val prefsController: PrefsController
) : PrefKeys {
    companion object {
        private const val APP_INSTANCE_ID_KEY = "AppInstanceId"
        private const val LANGUAGE_KEY = "SelectedLanguage"
    }

    override fun getAppInstanceId(): String {
        return prefsController.getString(APP_INSTANCE_ID_KEY, "")
    }

    override fun setAppInstanceId(value: String) {
        prefsController.setString(APP_INSTANCE_ID_KEY, value)
    }

    override fun setBiometricKey(key: String) {
        return prefsController.setString("biometric_key", key)
    }

    override fun getBiometricKey(): String {
        return prefsController.getString("biometric_key", "")
    }

    override fun setHardwareKey(key: String) {
        prefsController.setString("HardwareKey", key)
    }

    override fun getHardwareKey(): String {
        return prefsController.getString("HardwareKey", "")
    }

    override fun setLastDocType(key: String) {
        prefsController.setString("LastDocType", key)
    }

    override fun getLastDocType(): String {
        return prefsController.getString("LastDocType", "")
    }

    override fun setLastIssuanceMethod(method: String) {
        prefsController.setString("LastIssuanceMethod", method)
    }

    override fun getLastIssuanceMethod(): String {
        return prefsController.getString("LastIssuanceMethod", "")
    }

    override fun getAppActivated(): Boolean {
        return prefsController.getBool("AppActivated", false)
    }

    override fun hasAppActivated(): Boolean {
        return prefsController.contains("AppActivated")
    }

    override fun setAppActivated(key: Boolean) {
        prefsController.setBool("AppActivated", key)
    }

    override fun getWalletInstanceRegistered(): Boolean {
        return prefsController.getBool("WalletInstanceRegistered", false)
    }

    override fun setWalletInstanceRegistered(value: Boolean) {
        prefsController.setBool("WalletInstanceRegistered", value)
    }

    override fun setLanguage(language: String) {
        prefsController.setPublicString(LANGUAGE_KEY, language)
        // Keep secure prefs in sync for backward compatibility.
        prefsController.setString(LANGUAGE_KEY, language)
    }

    override fun getLanguage(): String {
        val publicValue = prefsController.getPublicString(LANGUAGE_KEY, "")
        if (publicValue.isNotEmpty()) {
            return publicValue
        }

        val secureValue = prefsController.getString(LANGUAGE_KEY, "")
        if (secureValue.isNotEmpty()) {
            prefsController.setPublicString(LANGUAGE_KEY, secureValue)
        }
        return secureValue
    }

    override fun setPresentationVendorSelections(value: String) {
        prefsController.setString("PresentationVendorSelections", value)
    }

    override fun getPresentationVendorSelections(): String {
        return prefsController.getString("PresentationVendorSelections", "")
    }
}
