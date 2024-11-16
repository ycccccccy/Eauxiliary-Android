package com.yc.eauxiliary

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.MessageDigest


object SecureStorageUtils {
    private const val PREFS_FILE_NAME = "secure_prefs" //  更改文件名

    fun getSharedPrefs(context: Context) = try {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("SecureStorage", "Error creating shared prefs: ${e.message}", e)
        null
    }

    private fun generateKey(group: List<DocumentFile>): String {
        val folderNames = group.joinToString(",") { it.name ?: "" }
        return hashString(folderNames)  // 使用文件夹名称生成哈希键
    }

    private fun hashString(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digested = md.digest(input.toByteArray())
        return digested.joinToString("") { String.format("%02x", it.toInt() and 0xff) }
    }

    fun getCachedAnswers(context: Context, group: List<DocumentFile>): String? {
        val key = generateKey(group)
        return getSharedPrefs(context)?.getString(key, null)
    }

    fun cacheAnswers(context: Context, group: List<DocumentFile>, answers: String) {
        val key = generateKey(group)
        getSharedPrefs(context)?.edit()?.putString(key, answers)?.apply()
    }

    fun isActivated(context: Context): Boolean {
        return getSharedPrefs(context)?.getBoolean("is_activated", false) ?: false
    }

    fun setActivated(context: Context, isActivated: Boolean) {
        getSharedPrefs(context)?.edit()?.putBoolean("is_activated", isActivated)?.apply()
    }


    fun getBlacklist(context: Context): List<String> {
        return getSharedPrefs(context)?.getStringSet("blacklist", setOf())?.toList() ?: emptyList()
    }


    fun setBlacklist(context: Context, blacklist: List<String>) {
        getSharedPrefs(context)?.edit()?.putStringSet("blacklist", blacklist.toSet())?.apply()
    }


    fun getWhitelist(context: Context): List<String> {
        return getSharedPrefs(context)?.getStringSet("whitelist", setOf())?.toList() ?: emptyList()
    }


    fun setWhitelist(context: Context, whitelist: List<String>) {
        getSharedPrefs(context)?.edit()?.putStringSet("whitelist", whitelist.toSet())?.apply()
    }
}