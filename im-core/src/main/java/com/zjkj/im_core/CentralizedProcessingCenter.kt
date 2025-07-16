package com.zjkj.im_core

import android.content.Context
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.internal.notify
import okhttp3.internal.wait
import org.daimhim.imc_core.IEngineState
import timber.multiplatform.log.Timber
import java.util.concurrent.Executors

class CentralizedProcessingCenter(private val context: Context, private val workerParams: WorkerParameters) : Worker(context, workerParams) {
    companion object{
        private var isRun = false
        private var syncLock = Any()
        @Synchronized
        fun start(context: Context){
            synchronized(syncLock) {
                if (isRun){
                    syncLock.notify()
                    return
                }
                val workManager = androidx.work.WorkManager.getInstance(context)
                val workRequest = androidx.work.OneTimeWorkRequest
                    .Builder(CentralizedProcessingCenter::class.java)
                    .build()
                workManager
                    .enqueue(workRequest)
                    .result
                    .addListener(object : Runnable{
                        override fun run() {
                            Timber.i("IMC CentralizedProcessingCenter Runnable")
                        }
                    },Executors.newSingleThreadExecutor())
                isRun = true
            }
        }

        private val TIME_OUT = 4 * 1000
    }
    private var time : Long = 0;
    override fun doWork(): Result {
        time = System.currentTimeMillis()
        synchronized(syncLock) {
            isRun = true
            while (TIME_OUT > (System.currentTimeMillis() - time)){
                Timber.i("IMC CentralizedProcessingCenter doWork startOrNot:${RegularInspectionConnectWorker.startOrNot}")
                // 接收到广播，说明子进程已经启动
                tryStartEngine()
                Timber.i("IMC CentralizedProcessingCenter doWork wait start")
                syncLock.wait()
                Timber.i("IMC CentralizedProcessingCenter doWork wait end")
            }
            isRun = false
        }
        return Result.success()
    }


    fun tryStartEngine(){
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
            if (iEngine.engineState() == IEngineState.ENGINE_OPEN
                || iEngine.engineState() == IEngineState.ENGINE_FAILED
                || iEngine.engineState() == IEngineState.ENGINE_CONNECTING){
                iEngine.makeConnection()
                return
            }
            Timber.i("serverAddress $serverAddress")
            iEngine
                .engineOn(serverAddress)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
}