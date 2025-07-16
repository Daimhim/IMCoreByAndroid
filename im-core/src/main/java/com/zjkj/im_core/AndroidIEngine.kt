package com.zjkj.im_core

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.ArrayMap
import org.daimhim.container.ContextHelper
import org.daimhim.imc_core.*
import timber.multiplatform.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

val IEngineState.ENGINE_SERVICE_NOT_CONNECTED: Int
    get() {
        return 4
    }

class AndroidIEngine : IEngine {
    private val executor = Executors.newSingleThreadExecutor()
    private val aWait = Object()

    override fun engineOff() {
        Thread(Runnable {
            connect()?.engineOff()
        })
            .start()
    }

    override fun engineOn(key: String) {
        executor.submit {
            connect()?.engineOn(key)
        }
    }

    override fun engineState(): Int {
        return connect()?.engineState()?:IEngineState.ENGINE_SERVICE_NOT_CONNECTED
    }

    override fun makeConnection() {
        executor.submit {
            connect()!!.makeConnection()
            Timber.i("makeConnection")
        }
    }

    override fun onChangeMode(mode: Int) {
        executor.submit {
            connect()!!.onChangeMode(mode)
            Timber.i("onChangeMode $mode")
        }
    }

    override fun onNetworkChange(networkState: Int) {
        executor.submit {
            connect()?.onNetworkChange(networkState)
        }
    }

    /**
     * 消息发送
     */
    override fun send(text: String): Boolean {
        Timber.i("send $text ${frogService == null}")
        if (frogService == null){
            return false
        }
        var isSuccess = false
        BigDataSplitUtil
            .dataSplitting(text) { p0, p1, p2, p3 ->
                isSuccess = frogService
                    ?.sendString(p0, p1, p2, p3) ?: false
            }
        return isSuccess
    }

    override fun send(byteArray: ByteArray): Boolean {
        var isSuccess = false
        BigDataSplitUtil
            .dataSplitting(byteArray) { p0, p1, p2, p3 ->
                isSuccess = frogService
                    ?.sendByte(p0, p1, p2, p3) ?: false
            }
        return isSuccess
    }


    /***
     * 引擎状态监听
     */
    private var newImcStatusListener : IMCStatusListener? = null
    private var proxyRemoteIMCStatusListener = object : RemoteIMCStatusListener.Stub() {

        override fun connectionClosed() {
            Timber.i("AndroidIEngine connectionClosed")
            newImcStatusListener?.connectionClosed(0, "")
        }

        override fun connectionLost() {
            Timber.i("AndroidIEngine connectionLost")
            newImcStatusListener?.connectionLost(RemoteException("咋回事啊！"))
        }

        override fun connectionSucceeded() {
            Timber.i("AndroidIEngine connectionSucceeded")
            newImcStatusListener?.connectionSucceeded()
        }
    }
    override fun setIMCStatusListener(listener: IMCStatusListener?) {
        newImcStatusListener = listener
    }

    /**
     * 消息监听
     */
    private val newImcListeners = ArrayList<V2IMCListener>()
    private val proxyRemoteV2IMCListener = object  : RemoteV2IMCListener.Stub() {
        override fun onMessageByte(
            md5: String?,
            index: Int,
            length: Int,
            data: ByteArray?
        ): Boolean {
            BigDataSplitUtil.dataAssemblyByte(md5, index, length, data ?: byteArrayOf()) {
                for (newImcListener in newImcListeners) {
                    newImcListener.onMessage(it)
                }
            }
            return true
        }

        override fun onMessageString(
            md5: String?,
            index: Int,
            length: Int,
            data: ByteArray?
        ): Boolean {
            BigDataSplitUtil.dataAssemblyStr(md5, index, length, data ?: byteArrayOf()) {
                for (newImcListener in newImcListeners) {
                    newImcListener.onMessage(it)
                }
            }
            return true
        }

    }
    override fun addIMCListener(imcListener: V2IMCListener) {
        Timber.i("addIMCListener ${imcListener.hashCode()}")
        if (newImcListeners.contains(imcListener)){return}
        newImcListeners.add(imcListener)
    }
    override fun removeIMCListener(imcListener: V2IMCListener) {
        newImcListeners.remove(imcListener)
        if (newImcListeners.isEmpty()){
            frogService?.removeIMCListener(proxyRemoteV2IMCListener)
        }
    }

    /**
     * 拦截器
     */
    private val newImcSocketListeners = ArrayMap<Int,ArrayList<V2IMCSocketListener>>()
    private var level :Int = Int.MAX_VALUE
    private val proxySocketRemoteV2IMCListener = object :RemoteV2IMCListener {
        override fun onMessageByte(
            md5: String?,
            index: Int,
            length: Int,
            data: ByteArray?
        ): Boolean {
            var isSuccess = false
            BigDataSplitUtil.dataAssemblyByte(md5, index, length, data ?: byteArrayOf()) {
                for (newImcSocketListener in newImcSocketListeners) {
                    for (v2IMCSocketListener in newImcSocketListener.value) {
                        isSuccess = v2IMCSocketListener.onMessage(this@AndroidIEngine, it)
                        if (isSuccess){
                            return@dataAssemblyByte
                        }
                    }
                }
            }
            return isSuccess
        }

        override fun onMessageString(
            md5: String?,
            index: Int,
            length: Int,
            data: ByteArray?
        ): Boolean {
            var isSuccess = false
            BigDataSplitUtil.dataAssemblyStr(md5, index, length, data ?: byteArrayOf()) {
                for (newImcSocketListener in newImcSocketListeners) {
                    for (v2IMCSocketListener in newImcSocketListener.value) {
                        isSuccess = v2IMCSocketListener.onMessage(this@AndroidIEngine, it)
                        if (isSuccess){
                            return@dataAssemblyStr
                        }
                    }
                }
            }
            return isSuccess
        }

        override fun asBinder(): IBinder? {
            return null
        }

    }

    override fun addIMCSocketListener(level: Int, imcSocketListener: V2IMCSocketListener) {
        val list = newImcSocketListeners[level]?:ArrayList()
        list.add(imcSocketListener)
        newImcSocketListeners[level] = list
        // 是否更新最新拦截等级
        if (this.level <= level){
            return
        }
        this.level = level
        frogService?.removeIMCSocketListener(proxySocketRemoteV2IMCListener)
        frogService?.addIMCSocketListener(this.level,proxySocketRemoteV2IMCListener)
    }
    override fun removeIMCSocketListener(imcSocketListener: V2IMCSocketListener) {
        val iterator = newImcSocketListeners.iterator()
        var next: MutableMap.MutableEntry<Int, ArrayList<V2IMCSocketListener>>
        while (iterator.hasNext()){
            next = iterator.next()
            next.value.remove(imcSocketListener)
            if (next.value.isEmpty()){
                iterator.remove()
            }
        }
        if (newImcSocketListeners.isEmpty()){
            frogService?.removeIMCSocketListener(proxySocketRemoteV2IMCListener)
            level = Int.MAX_VALUE
        }
    }

    fun setSharedParameters(parameters: MutableMap<String, String>) {
        connect()?.setSharedParameters(parameters)
    }

    // 添加一个标志位来跟踪服务是否已经绑定
    private var isServiceBound = false
    private var frogService: FrogService? = null
    private val connectService : ServiceConnection = object : ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                frogService = FrogService.Stub.asInterface(service)
                Timber.i("FrogServiceNative main onServiceConnected  ${Thread.currentThread().name}")
                isServiceBound = true
                initListener()
            } finally {
                synchronized(aWait) {
                    aWait.notify()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            try {
                Timber.i("FrogServiceNative main onServiceDisconnected ${Thread.currentThread().name}")
                frogService = null
                isServiceBound = false
            } finally {
                synchronized(aWait) {
                    aWait.notify()
                }
            }
        }



    }

    /**
     * 连接子进程服务
     */
    private fun connect(): FrogService? {
        if (isServiceBound){
            return frogService
        }
        synchronized(aWait) {
            try {
                // 永存启动
                Timber.i("connect ${Thread.currentThread().name}")
                val intent = FrogServiceNative.getIntent(ContextHelper.getApplication())

                // 临绑启动
                ContextHelper
                    .getApplication()
                    .bindService(intent, this.connectService, Context.BIND_AUTO_CREATE)
                // 等待
                aWait.wait(5000)
            } catch (e:Exception){
                e.printStackTrace()
                newImcStatusListener?.connectionLost(e)
            } finally {
                aWait.notify()
            }
        }

        return frogService
    }
    fun initListener(){
        //设立监听
        frogService?.addIMCListener(proxyRemoteV2IMCListener)
        frogService?.setIMCStatusListener(proxyRemoteIMCStatusListener)
        frogService?.addIMCSocketListener(level,proxySocketRemoteV2IMCListener)
    }

    fun preBindingService(){
        executor.submit {
            connect()
        }
    }
    fun disConnectService(){
        Timber.i("disConnectService $isServiceBound")
        ContextHelper
            .getApplication()
            .unbindService(this.connectService)
    }
}