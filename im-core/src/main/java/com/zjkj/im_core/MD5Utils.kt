package com.zjkj.im_core

import java.security.MessageDigest

object MD5Utils {
    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()
    fun getStringMD5(value: String?): String? {
        return getMD5(value?.toByteArray(Charsets.UTF_8))
    }

    fun getMD5(source: ByteArray?): String? {
        source?:return null
        val hash = MessageDigest.getInstance("MD5").digest(source)
        val hex  = StringBuilder(hash.size * 2)
        var toInt: Int
        hash.forEach {
            toInt = it.toInt()
            hex.append(HEX_CHARS[toInt shr 4 and 0xf])
            hex.append(HEX_CHARS[toInt and 0xf])
        }
        return hex.toString()
    }
}