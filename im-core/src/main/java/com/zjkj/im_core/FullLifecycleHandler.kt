package com.zjkj.im_core

import android.app.Activity
import android.app.Application
import android.os.Bundle

class FullLifecycleHandler : Application.ActivityLifecycleCallbacks  {
    private var numStarted = 0
    private var isForeground = false
    companion object{
        private val foregroundCallbacks = mutableListOf<Comparable<Boolean>>()

        /**
         * true 前台
         * false 后台
         */
        fun registerForegroundCallback(comparator:Comparable<Boolean>){
            foregroundCallbacks.add(comparator)
        }
        fun unregisterForegroundCallback(comparator:Comparable<Boolean>){
            foregroundCallbacks.remove(comparator)
        }
    }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
        numStarted++;
        if (!isForeground) {
            isForeground = true;
            // 应用程序进入前台
            foregroundCallbacks.forEach { it.compareTo(isForeground) }
        }
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
        numStarted--
        if (numStarted == 0) {
            isForeground = false
            // 应用程序进入后台
            foregroundCallbacks.forEach { it.compareTo(isForeground) }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}