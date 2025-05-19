package com.zjkj.im_core

import android.content.Context
import org.daimhim.container.ContextHelper
import org.daimhim.imc_core.IEngine
import org.daimhim.imc_core.IEngineState


interface FSNConfig {
    companion object {
        val PACKAGE_NAME = ""
        private var mainIEngine: IEngine? = null
        fun getIEngine(): IEngine {
            if (mainIEngine == null) {
                throw NullPointerException("IEngine 未初始化")
            }
          return mainIEngine!!
        }

        fun setIEngine(iEngine: IEngine) {
            mainIEngine = iEngine
        }
        private val sharedParameters = mutableMapOf<String,String>()
        @JvmStatic
        fun getSharedParameters(key:String):String?{
            return sharedParameters[key]
        }
        @JvmStatic
        fun setSharedParameters(parameters:MutableMap<String,String>){
            for (parameter in parameters) {
                if (sharedParameters.containsKey(parameter.key) && parameter.value.isEmpty()){
                    sharedParameters.remove(parameter.key)
                }else{
                    sharedParameters.put(parameter.key,parameter.value)
                }
            }
            sharedParameters.putAll(parameters)
        }
    }
}