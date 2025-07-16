package com.zjkj.im_core

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.daimhim.imc_core.IEngineState
import timber.multiplatform.log.Timber

class RegularInspectionConnectWorker(private val context: Context, private val workerParams: WorkerParameters) : Worker(context, workerParams) {
    companion object{
        private const val REGULAR_INSPECTION_CONNECT_TAG = "REGULAR_INSPECTION_CONNECT_TAG"
        var startOrNot = false
        @Synchronized
        fun start(context: Context){
            if (startOrNot){
                return
            }
            startOrNot = true
            val workManager = androidx.work.WorkManager.getInstance(context)
            val workRequest = PeriodicWorkRequest
                .Builder(RegularInspectionConnectWorker::class.java, 15, java.util.concurrent.TimeUnit.MINUTES)
                .build()

            workManager
                .enqueueUniquePeriodicWork(REGULAR_INSPECTION_CONNECT_TAG,
                    androidx.work.ExistingPeriodicWorkPolicy.REPLACE, workRequest)
        }
        @Synchronized
        fun stop(context: Context){
            val workManager = androidx.work.WorkManager.getInstance(context)
            workManager.cancelUniqueWork(REGULAR_INSPECTION_CONNECT_TAG)
            startOrNot = false
        }
    }

    override fun doWork(): Result {
        Timber.i("IMC RegularInspectionConnectWorker doWork startOrNot${startOrNot}")
        // 接收到广播，说明子进程已经启动
        try {
            CentralizedProcessingCenter.start(context)
        }catch (e:Exception){
            e.printStackTrace()
        }
        return Result.success()
    }
}