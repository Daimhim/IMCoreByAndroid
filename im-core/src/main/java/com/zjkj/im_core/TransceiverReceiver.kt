package com.zjkj.im_core

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import org.daimhim.container.ContextHelper
import org.daimhim.imc_core.IEngineState
import timber.multiplatform.log.Timber

class TransceiverReceiver : BroadcastReceiver() {
    companion object {
        const val FROG_SERVICE_ACTION = "com.zjkj.im_core.action.BACKGROUND_STATUS_RECOVERY"
        fun sendTransceiverReceiver(context: Context){
            val intent = Intent(FROG_SERVICE_ACTION)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.setComponent(ComponentName(
                context.packageName,
                "com.zjkj.im_core.TransceiverReceiver"
            ))
            intent.setPackage("com.zjkj.im_core")
            context.sendBroadcast(intent)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Timber.i("onReceive ${intent?.action}")
        // 接收到广播，说明子进程已经启动
        try {
            // 获取绑定ID
            val bindId = IMSPUtils.getInstance().get(ConfigureConstants.BIND_ID, "")
            if (bindId.isNullOrEmpty()){
                return
            }
            val serverAddress = IMSPUtils.getInstance()
                .get("${bindId}_${ConfigureConstants.SERVER_ADDRESS}", "")
            // 获取服务地址
            if (serverAddress.isNullOrEmpty()){
                return
            }
            val iEngine = FSNConfig
                .getIEngine()
            if (iEngine.engineState() == IEngineState.ENGINE_OPEN){
                iEngine.makeConnection()
                return
            }
            iEngine
                .engineOn(serverAddress)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
}