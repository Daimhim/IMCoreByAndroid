package com.zjkj.im_core

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import com.zjkj.im_core.HeartbeatAlarmTimeoutScheduler.AlarmTimeoutBroadcastReceiver
import com.zjkj.im_core.HeartbeatAlarmTimeoutScheduler.Companion.HEARTBEAT_ALARM_TIMEOUT_ACTION
import org.daimhim.container.ContextHelper
import org.daimhim.imc_core.IEngineState
import timber.multiplatform.log.Timber

class TransceiverReceiver : BroadcastReceiver() {
    companion object {
        const val FROG_SERVICE_ACTION = "com.zjkj.im_core.action.BACKGROUND_STATUS_RECOVERY"
        const val TRANSCEIVER_RECEIVER_ACTION = "com.zjkj.im_core.action.TRANSCEIVER_RECEIVER"
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

        fun sendRevivalBroadcast(){
            Timber.i("sendRevivalBroadcast")
            val intent = Intent(TRANSCEIVER_RECEIVER_ACTION).apply {
                setPackage(ContextHelper.getApplication().packageName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                ContextHelper.getApplication(),
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
            val triggerTime = SystemClock.elapsedRealtime() + (3 * 60 * 1000)
            val alarmManager = ContextHelper
                .getApplication()
                .getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Timber.i("IMC TransceiverReceiver onReceive  action:${intent?.action}")
        if (intent?.action == TRANSCEIVER_RECEIVER_ACTION){
            sendRevivalBroadcast()
        }
        // 接收到广播，说明子进程已经启动
        try {
            CentralizedProcessingCenter.start(context)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
}