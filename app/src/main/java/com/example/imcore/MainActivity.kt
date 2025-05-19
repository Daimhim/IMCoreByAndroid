package com.example.imcore

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.kongqw.network.monitor.NetworkMonitorManager
import com.kongqw.network.monitor.enums.NetworkState
import com.kongqw.network.monitor.interfaces.NetworkMonitor
import com.zjkj.im_core.AndroidIEngine
import com.zjkj.im_core.FullLifecycleHandler
import com.zjkj.im_core.TransceiverReceiver
import org.daimhim.imc_core.IEngine
import org.daimhim.container.ContextHelper
import org.daimhim.imc_core.IEngineState
import org.daimhim.imc_core.IMCStatusListener
import org.daimhim.imc_core.RapidResponseForceV2
import org.daimhim.imc_core.V2IMCListener
import timber.multiplatform.log.Timber

class MainActivity : Activity() {
    companion object{
        val iEngine: IEngine = AndroidIEngine()

        const val SMART_HEARTBEAT = 1
        const val FIXED_HEARTBEAT = 0

        const val DELAYED_SWITCHING_SMART_HEARTBEAT = "DELAYED_SWITCHING_SMART_HEARTBEAT"

        const val DELAY_SWITCHING_TIME = 30 * 1000L
    }
    var baseUrl = "https://8f1f-117-22-144-152.ngrok-free.app"
    private val rapidResponseForceV2 = RapidResponseForceV2()

    private lateinit var et_base_url : EditText
    private lateinit var et_msg_input : EditText
    private lateinit var bt_action : Switch
    private lateinit var tv_state : TextView
    private var heartbeatMode: Int = 0
    private val mainAdapter = MainAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        initListener()
    }


    private fun initView() {
        et_base_url = findViewById<EditText>(R.id.et_base_url)
        et_msg_input = findViewById<EditText>(R.id.et_msg_input)
        bt_action = findViewById<Switch>(R.id.bt_action)
        tv_state = findViewById<TextView>(R.id.tv_state)


        et_base_url.setText(baseUrl)
        et_msg_input.setText("${System.currentTimeMillis()}")


        val lv_list = findViewById<ListView>(R.id.lv_list)
        lv_list.adapter = mainAdapter

    }

    private fun initListener() {
        rapidResponseForceV2.timeoutCallback(object : Comparable<Pair<String,Any?>>{
            override fun compareTo(other: Pair<String, Any?>): Int {
                Timber.i("rapidResponseForceV2 compareTo")
                Timber.i("AndroidIEngine 置入后台 切换心跳模式")
                iEngine.onChangeMode(SMART_HEARTBEAT)
                return 0
            }
        })
        // 行动
        bt_action.setOnClickListener{
                //
                val url = et_base_url.text.toString()
                if (url.isEmpty()){
                    Toast.makeText(
                        ContextHelper.getApplication(),
                        "请输入url", Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                Thread(kotlinx.coroutines.Runnable {
                    try {
                        Timber.i("FrogServiceNative ${MainActivity.iEngine.engineState()}")
                        if (bt_action.isChecked){
                            iEngine.engineOn(url)
                        }else{
                            iEngine.engineOff()
                        }
//                        val engineState = iEngine.engineState()
//                        if (engineState == IEngineState.ENGINE_CLOSED || engineState == IEngineState.ENGINE_CLOSED_FAILED){
//                            iEngine.engineOn(url)
//                        }else{
//                            iEngine.engineOff()
//                        }
                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                })
                    .start()
            }
        //状态监听
        iEngine
            .setIMCStatusListener(object : IMCStatusListener {
                override fun connectionClosed(code: Int, reason: String?) {
                    val name = Thread.currentThread().name
                    runOnUiThread {
                        bt_action.isChecked = false
                        tv_state.text = "connectionClosed ${name}"
                    }
                }

                override fun connectionLost(throwable: Throwable) {
                    val name = Thread.currentThread().name
                    runOnUiThread {
                        bt_action.isChecked = true
                        tv_state.text = "connectionLost ${name}"
                    }
                }

                override fun connectionSucceeded() {
                    val name = Thread.currentThread().name
                    runOnUiThread {
                        bt_action.isChecked = true
                        tv_state.text = "connectionSucceeded ${name}"
                    }
                }

            })
        //新消息监听
        iEngine
            .addIMCListener(object : V2IMCListener {
                override fun onMessage(text: String) {
                    runOnUiThread {
                        Timber.i("MainActivity onMessage $text")
                        val message = Message()
                        message.sendName = "TA"
                        message.text = text + Thread.currentThread().name
                        mainAdapter.addItem(message)
                    }
                }
            })
        //智能心跳
        findViewById<Button>(R.id.bt_heartbeat_mode)
            .setOnClickListener {
                heartbeatMode = if (heartbeatMode == 0) 1 else 0
                iEngine.onChangeMode(heartbeatMode)
                (it as Button).text = if (heartbeatMode == 0) "固定心跳" else "智能心跳"
            }
        //重置连接
        findViewById<Button>(R.id.bt_rest)
            .setOnClickListener {
                iEngine
                    .makeConnection()
            }
        // 链接服务
        findViewById<Button>(R.id.bt_link_service)
            .setOnClickListener {
                (iEngine as AndroidIEngine).preBindingService()
            }
        // 断开服务
        findViewById<Button>(R.id.bt_break_service)
            .setOnClickListener {
                (iEngine as AndroidIEngine).disConnectService()
            }
        // 发送
        findViewById<Button>(R.id.bt_send)
            .setOnClickListener {
                val msg = et_msg_input.text.toString()
                val message = Message()
                message.sendName = "ME"
                message.text = msg
                mainAdapter.addItem(message)
                iEngine.send(msg)
                et_msg_input.setText("")
                et_msg_input.setHint("${System.currentTimeMillis()}")
            }
        // 发送复活广播
        findViewById<Button>(R.id.bt_send_resurrection_broadcast)
            .setOnClickListener {
                TransceiverReceiver.sendTransceiverReceiver(this)
            }
        // 监听生命周期
        FullLifecycleHandler
            .registerForegroundCallback(object : Comparable<Boolean>{
                override fun compareTo(other: Boolean): Int {
                    Timber.i("registerForegroundCallback compareTo $other")
                    if(other){
                        Timber.i("AndroidIEngine 置入前台 切换固定心跳模式")
                        // 前台  心跳切换为 固定
                        rapidResponseForceV2.unRegister(DELAYED_SWITCHING_SMART_HEARTBEAT)
                        iEngine.onChangeMode(FIXED_HEARTBEAT)
                        return 0
                    }
                    Timber.i("AndroidIEngine 置入后台 计时切换心跳模式")
                    // 后台 延迟切换为智能心跳
                    rapidResponseForceV2.register(DELAYED_SWITCHING_SMART_HEARTBEAT,null,DELAY_SWITCHING_TIME)
                    return 0
                }
            })
        // 网络变化
        NetworkMonitorManager
            .getInstance()
            .register(this)
    }
    @NetworkMonitor
    fun onNetworkChange(networkState: NetworkState){
        when(networkState){
            NetworkState.WIFI->{
                iEngine.onNetworkChange(1)
            }
            NetworkState.CELLULAR->{
                iEngine.onNetworkChange(2)
            }
            else->{
                iEngine.onNetworkChange(0)
            }
        }
    }
    class MainAdapter : BaseAdapter() {
        private val data = mutableListOf<Message>()
        override fun getCount(): Int {
            return data.size
        }

        override fun getItem(position: Int): Message {
            return data[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val item = getItem(position)
            val context = parent!!.context
            val linearLayout = LinearLayout(context)
                .also {
                    it.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                    it.orientation = LinearLayout.HORIZONTAL
                    it.gravity = if (item.sendName == "TA") Gravity.START else Gravity.END
                }
            TextView(context)
                .also {
                    it.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                    linearLayout.addView(it)
                    if (item.sendName == "TA") {
                        it.text = item.sendName
                    }else{
                        it.text = item.text
                    }
                }
            TextView(context)
                .also {
                    it.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                    linearLayout.addView(it)
                    if (item.sendName == "TA") {
                        it.text = item.text
                    }else{
                        it.text = item.sendName
                    }
                }


            return linearLayout
        }

        fun addItem(msg:Message){
            data.add(0,msg)
            notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NetworkMonitorManager
            .getInstance()
            .unregister(this)
    }
}

class Message {
    var sendName: String = ""
    var text: String = ""
}