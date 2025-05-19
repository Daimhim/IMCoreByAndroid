package com.zjkj.im_core

import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object GZipUtil {

    fun String.gzip(): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter(Charsets.UTF_8).use { it.write(this) }
        return bos.toByteArray()
    }

    fun ByteArray.gzipToString(): String =
        GZIPInputStream(inputStream()).bufferedReader(Charsets.UTF_8).use { it.readText() }

    fun ByteString.unGzipToInputStream(): InputStream = GZIPInputStream(toByteArray().inputStream())

    fun ByteString.unGzipToByteArray(): ByteArray = GZIPInputStream(toByteArray().inputStream())
        .readBytes()

    fun ByteString.unGzipToString(): String = GZIPInputStream(toByteArray().inputStream())
        .bufferedReader(Charsets.UTF_8).use { it.readText() }
    fun ByteArray.unGzipToString(): String = GZIPInputStream(inputStream())
        .bufferedReader(Charsets.UTF_8).use { it.readText() }
}