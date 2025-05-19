package com.example.imcore

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.kongqw.network.monitor.NetworkMonitorManager
import com.zjkj.im_core.AutoConnectAlarmTimeoutScheduler
import com.zjkj.im_core.FSNConfig
import com.zjkj.im_core.FullLifecycleHandler
import com.zjkj.im_core.HeartbeatAlarmTimeoutScheduler
import com.zjkj.im_core.TransceiverReceiver
import org.daimhim.container.ContextHelper
import org.daimhim.imc_core.ProgressiveAutoConnect
import org.daimhim.imc_core.RRFTimeoutScheduler
import org.daimhim.imc_core.TimberIMCLog
import org.daimhim.imc_core.V2FixedHeartbeat
import org.daimhim.imc_core.V2JavaWebEngine
import org.daimhim.imc_core.V2SmartHeartbeat
import timber.multiplatform.log.DebugTree
import timber.multiplatform.log.Timber

class StartApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ContextHelper.init(this)
        Timber.plant(DebugTree())
        if (ContextHelper.isMainProcess()){
            // 主进程
            registerActivityLifecycleCallbacks(FullLifecycleHandler())
            NetworkMonitorManager.getInstance().init(this)
//            TransceiverReceiver.sendTransceiverReceiver(this)
            return
        }
        WorkManager.initialize(this,
            Configuration
                .Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()
        )
        FSNConfig
            .setIEngine(V2JavaWebEngine
                .Builder()
                .setIMCLog(TimberIMCLog("V2JavaWebEngine"))
                .addHeartbeatMode(
                    MainActivity.FIXED_HEARTBEAT,
                    V2FixedHeartbeat
                        .Builder()
                        .setCurHeartbeat(5)
                        .build()
                )
                .addHeartbeatMode(
                    MainActivity.SMART_HEARTBEAT,
                    V2SmartHeartbeat
                        .Builder()
                        .setHeartbeatStep(10)
                        .setMinHeartbeat(35L)
                        .setInitialHeartbeat(45L)
                        .setTimeoutScheduler(HeartbeatAlarmTimeoutScheduler("智能心跳"))
                        .build()
                )
                .setAutoConnect(
                    ProgressiveAutoConnect
                        .Builder()
                        .setTimeoutScheduler(AutoConnectAlarmTimeoutScheduler("自动连接"))
                        .build()
                )
                .build())
    }
}