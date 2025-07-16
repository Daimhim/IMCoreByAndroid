package com.zjkj.im_core

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import org.daimhim.container.ContextHelper
import org.daimhim.imc_core.*
import timber.multiplatform.log.Timber

class FrogServiceNative : Service() {
    companion object{
        const val ACTION = "com.zjkj.im_core.action.TRANSCEIVER_RECEIVER"
        fun getIntent(context: Context):Intent{
            val intent = Intent(context,FrogServiceNative::class.java)
            intent.putExtra(ConfigureConstants.PROCESS_NAME,ContextHelper.getProcessName())
            return intent
        }
    }

    private val bindBindMap = HashMap<String,BindFrogServiceIBinder>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        val processName = intent?.getStringExtra("ProcessName")?:return null
        val bindFrogServiceIBinder =
            bindBindMap[processName] ?: BindFrogServiceIBinder(processName)
        bindBindMap[processName] = bindFrogServiceIBinder
        bindFrogServiceIBinder.onBind()
        Timber.i("onBind ${intent?.action} ${intent?.data} ${intent?.getStringExtra("ProcessName")}")
        preAutomaticConnection(processName)
        return bindFrogServiceIBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        val processName = intent?.getStringExtra("ProcessName")?:return false
        bindBindMap.remove(processName)?.onUnbind()
        Timber.i("onUnbind ${intent?.action} ${intent?.data} ${intent?.getStringExtra("ProcessName")}")
        return true
    }

    /**
     * 预自动连接
     */
    private fun preAutomaticConnection(bindId: String){
        val serverAddress = IMSPUtils.getInstance().get("${bindId}_${ConfigureConstants.SERVER_ADDRESS}","")
        if (serverAddress.isNullOrEmpty()){
            return
        }
        val bindFrogServiceIBinder = bindBindMap[bindId]?:return
        if (bindFrogServiceIBinder.engineState() == IEngineState.ENGINE_OPEN){
            return
        }
        bindFrogServiceIBinder.engineOn(serverAddress)
    }


    class BindFrogServiceIBinder(private val bindId:String) : FrogService.Stub() {

        override fun engineOn(key: String?) {
            Timber.i("frogServiceIBinder $key")
            IMSPUtils
                .getInstance()
                .put("${bindId}_${ConfigureConstants.SERVER_ADDRESS}",key?:"")
            FSNConfig
                .getIEngine()
                .engineOn(key ?: throw RemoteException("key 不可为空"))
            IMSPUtils.getInstance().put(ConfigureConstants.BIND_ID,bindId)
            RegularInspectionConnectWorker.start(ContextHelper.getApplication())
        }

        override fun engineOff() {
            pingBinder()
            FSNConfig
                .getIEngine()
                .engineOff()
            IMSPUtils
                .getInstance()
                .remove("${bindId}_${ConfigureConstants.SERVER_ADDRESS}")
            IMSPUtils
                .getInstance()
                .remove(ConfigureConstants.BIND_ID)
            RegularInspectionConnectWorker.stop(ContextHelper.getApplication())
        }

        override fun engineState(): Int {
            return FSNConfig
                .getIEngine()
                .engineState()
        }

        override fun makeConnection() {
            FSNConfig
                .getIEngine()
                .onNetworkChange(-1)
        }

        override fun onChangeMode(mode: Int) {
            FSNConfig
                .getIEngine()
                .onChangeMode(mode)
        }

        override fun onNetworkChange(mode: Int) {
            FSNConfig
                .getIEngine()
                .onNetworkChange(mode)
        }

        override fun setSharedParameters(parameters: MutableMap<String, String>?) {
            FSNConfig.setSharedParameters(parameters ?: mutableMapOf())
        }

        override fun sendByte(md5: String?, index: Int, length: Int, data: ByteArray): Boolean {
            var isSuccess = false
            BigDataSplitUtil
                .dataAssemblyByte(md5, index, length, data) {
                    isSuccess = FSNConfig
                        .getIEngine()
                        .send(it)
                }
            return isSuccess
        }

        override fun sendString(md5: String?, index: Int, length: Int, data: ByteArray): Boolean {
            var isSuccess = false
            BigDataSplitUtil
                .dataAssemblyStr(md5, index, length, data) {
                    isSuccess = FSNConfig
                        .getIEngine()
                        .send(it)
                }
            return isSuccess
        }

        /**
         * 消息分发 代理
         */
        private val newImcListener = object : V2IMCListener{
            override fun onMessage(text: String) {
                proxyRemoteV2IMCListener?:return
                BigDataSplitUtil.dataSplitting(text) { p0, p1, p2, p3 ->
                    proxyRemoteV2IMCListener?.onMessageString(p0, p1, p2, p3)
                }
            }

            override fun onMessage(byteArray: ByteArray) {
                proxyRemoteV2IMCListener?:return
                BigDataSplitUtil.dataSplitting(byteArray) { p0, p1, p2, p3 ->
                    proxyRemoteV2IMCListener?.onMessageByte(p0, p1, p2, p3)
                }
            }
        }
        private var proxyRemoteV2IMCListener : RemoteV2IMCListener? = null
        override fun addIMCListener(listener: RemoteV2IMCListener?) {
            proxyRemoteV2IMCListener = listener
        }
        override fun removeIMCListener(listener: RemoteV2IMCListener?) {
            proxyRemoteV2IMCListener = null
        }

        /**
         * 消息拦截器
         */
        private var level = Int.MAX_VALUE
        private val newImcSocketListener = object : V2IMCSocketListener{
            override fun onMessage(iEngine: IEngine, bytes: ByteArray): Boolean {
                var isSuccess = false
                BigDataSplitUtil.dataSplitting(bytes) { p0, p1, p2, p3 ->
                    isSuccess = proxyRemoteSocketV2IMCListener?.onMessageByte(p0, p1, p2, p3) ?: false
                }
                return isSuccess
            }

            override fun onMessage(iEngine: IEngine, text: String): Boolean {
                var isSuccess = false
                BigDataSplitUtil.dataSplitting(text) { p0, p1, p2, p3 ->
                    isSuccess = proxyRemoteSocketV2IMCListener?.onMessageString(p0, p1, p2, p3) ?: false
                }
                return isSuccess
            }
        }
        private var proxyRemoteSocketV2IMCListener : RemoteV2IMCListener? = null
        private var isSetSocketV2IMCListener = false
        override fun addIMCSocketListener(level: Int, listener: RemoteV2IMCListener?) {
            proxyRemoteSocketV2IMCListener = listener
            if (isSetSocketV2IMCListener && level > this.level){
                return
            }
            isSetSocketV2IMCListener = true
            this.level = level
            FSNConfig
                .getIEngine()
                .removeIMCSocketListener(this.newImcSocketListener)
            FSNConfig
                .getIEngine()
                .addIMCSocketListener(this.level, this.newImcSocketListener)
        }
        override fun removeIMCSocketListener(listener: RemoteV2IMCListener?) {
            proxyRemoteSocketV2IMCListener = null
            isSetSocketV2IMCListener = false
            this.level = Int.MAX_VALUE
            FSNConfig
                .getIEngine()
                .removeIMCSocketListener(this.newImcSocketListener)
        }


        /**
         * 引擎状态监听代理
         */
        private val newIMCStatusListener = object : IMCStatusListener{
            override fun connectionClosed(code: Int, reason: String?) {
                Timber.i("FrogServiceNative connectionClosed")
                proxyRemoteIMCStatusListener?.connectionClosed()
            }

            override fun connectionLost(throwable: Throwable) {
                Timber.i("FrogServiceNative connectionLost")
                proxyRemoteIMCStatusListener?.connectionLost()
            }

            override fun connectionSucceeded() {
                Timber.i("FrogServiceNative connectionSucceeded")
                proxyRemoteIMCStatusListener?.connectionSucceeded()
            }

        }
        private var proxyRemoteIMCStatusListener : RemoteIMCStatusListener? = null
        override fun setIMCStatusListener(listener: RemoteIMCStatusListener?) {
            proxyRemoteIMCStatusListener = listener
            listener?:return
            when(engineState()){
                IEngineState.ENGINE_OPEN->{
                    listener.connectionSucceeded()
                }
                IEngineState.ENGINE_CLOSED,IEngineState.ENGINE_FAILED->{
                    listener.connectionClosed()
                }
                else->{
                    listener.connectionLost()
                }
            }
        }

        fun onBind() {
            FSNConfig
                .getIEngine()
                .addIMCListener(newImcListener)
            FSNConfig
                .getIEngine()
                .setIMCStatusListener(this.newIMCStatusListener)
        }

        fun onUnbind() {
            FSNConfig
                .getIEngine()
                .removeIMCListener(newImcListener)
            FSNConfig
                .getIEngine()
                .setIMCStatusListener(null)
            isSetSocketV2IMCListener = false
            this.level = Int.MAX_VALUE
            FSNConfig
                .getIEngine()
                .removeIMCSocketListener(this.newImcSocketListener)
        }
    }
}