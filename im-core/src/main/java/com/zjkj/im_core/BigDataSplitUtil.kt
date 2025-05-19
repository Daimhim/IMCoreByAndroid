package com.zjkj.im_core

import android.util.Log
import android.util.LruCache

object BigDataSplitUtil {
    private const val SLICE_INTERVAL = 1024 * 720 // (720kb)

    /**
     * 数据拆分发送
     */
    fun dataSplitting(
        text: String,
        callback: ((
            String,
            Int,
            Int,
            ByteArray
        ) -> Unit),
    ){
        Log.i("BigDataSplitUtil",
            "dataSplitting  ${Thread.currentThread().name} ${mergeCache.size()} $text")
        val toByteArray = text.toByteArray(Charsets.UTF_8)
        dataSplitting(toByteArray, callback)
    }
    fun dataSplitting(
        toByteArray: ByteArray,
        callback: ((
            String,
            Int,
            Int,
            ByteArray
        ) -> Unit),
    ){
        val mD5 = MD5Utils.getMD5(toByteArray)?:""
        val size = toByteArray.size
        if (size <= SLICE_INTERVAL){
            callback.invoke(mD5,0,size,toByteArray)
            return
        }
        val toInt = Math.ceil((size / SLICE_INTERVAL).toDouble()).toInt()
        var start = 0
        var end = SLICE_INTERVAL
        var sliceInterval : ByteArray
        for (i in 0 until toInt){
            sliceInterval = toByteArray.sliceArray(start until end)
            callback.invoke(mD5,start,size,sliceInterval)
            start = end
            end += SLICE_INTERVAL
            if (end > size){
                end = size
            }
        }
    }
    private val mergeCache = LruCache<String,ByteArray>(35)

    /**
     * 在内存中数据组装，慎用
     */
    fun dataAssemblyByte(
        md5: String?,
        index: Int,
        length: Int,
        data: ByteArray,
        callback:((ByteArray)->Unit)? = null
    ){
        val value = mergeCache[md5] ?: ByteArray(length)
        System.arraycopy(data,0,value,index,data.size)
        val mD5 = MD5Utils.getMD5(value)
        Log.i("BigDataSplitUtil",
            "dataAssembly $md5 $mD5 $length")
        if (mD5 != md5){
            return
        }
        mergeCache.remove(md5)
        callback?.invoke(value)
    }
    fun dataAssemblyStr(
        md5: String?,
        index: Int,
        length: Int,
        data: ByteArray,
        callback:((String)->Unit)? = null
    ){
        val value = mergeCache[md5] ?: ByteArray(length)
        System.arraycopy(data,0,value,index,data.size)
        val mD5 = MD5Utils.getMD5(value)
        Log.i("BigDataSplitUtil",
            "dataAssembly $md5 $mD5 $length")
        if (mD5 != md5){
            return
        }
        mergeCache.remove(md5)
        callback?.invoke(value.toString(Charsets.UTF_8))
    }

}