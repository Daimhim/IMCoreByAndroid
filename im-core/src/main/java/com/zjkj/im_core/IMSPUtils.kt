package com.zjkj.im_core

import android.content.Context
import android.content.SharedPreferences
import org.daimhim.container.ContextHelper

class IMSPUtils {
    companion object {
        private var instance: IMSPUtils? = null

        fun getInstance(): IMSPUtils {
            if (instance == null) {
                instance = IMSPUtils()
            }
            return instance!!
        }
    }

    private val SP_FILE_NAME = "IMStateRecovery"
    private val sharedPreferences: SharedPreferences =
        ContextHelper.getApplication().getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE)

    fun put(key: String, value: Int) {
        sharedPreferences
            .edit()
            .putInt(key, value)
            .apply()
    }

    fun get(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    // 重载 put 方法
    fun put(key: String, value: Long) {
        sharedPreferences
            .edit()
            .putLong(key, value)
            .apply()
    }

    fun put(key: String, value: Float) {
        sharedPreferences
            .edit()
            .putFloat(key, value)
            .apply()
    }

    fun put(key: String, value: String) {
        sharedPreferences
            .edit()
            .putString(key, value)
            .apply()
    }

    fun put(key: String, value: Boolean) {
        sharedPreferences
            .edit()
            .putBoolean(key, value)
            .apply()
    }

    fun put(key: String, value: Set<String>) {
        sharedPreferences
            .edit()
            .putStringSet(key, value)
            .apply()
    }

    // 重载 get 方法
    fun get(key: String, defaultValue: Long): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    fun get(key: String, defaultValue: Float): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    fun get(key: String, defaultValue: String?): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    fun get(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun get(key: String, defaultValue: Set<String>?): Set<String>? {
        return sharedPreferences.getStringSet(key, defaultValue)
    }

    fun remove(key: String) {
        sharedPreferences
            .edit()
            .remove(key)
            .apply()
    }

    fun clear() {
        sharedPreferences
            .edit()
            .clear()
            .apply()
    }

    fun contains(key: String): Boolean {
        return sharedPreferences
            .contains(key)
    }
}