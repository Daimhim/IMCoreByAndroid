// FrogService.aidl
package com.zjkj.im_core;

import com.zjkj.im_core.RemoteV2IMCListener;
import com.zjkj.im_core.RemoteIMCStatusListener;

// Declare any non-default types here with import statements
interface FrogService {
    // 打开
    void engineOn(in String key);
    // 关闭
    void engineOff();
    // 获取状态
    int engineState();
    // 检测连接，断开则重连
    void makeConnection();
    // 切换场景
    void onChangeMode(in int mode);
    // 切换网络
    void onNetworkChange(in int mode);
    // 传递子进程共享参数
    void setSharedParameters(in Map<String,String> parameters);
    // 发送byte
    boolean sendByte(
        String md5,
        int index,
        int length,
        in byte[] data
    );
    // 发送str
    boolean sendString(
        String md5,
        int index,
        int length,
        in byte[] data
    );
    // 添加监听
    void addIMCListener(RemoteV2IMCListener listener);
    // 移除消息监听
    void removeIMCListener(RemoteV2IMCListener listener);
    // 监听状态监听
    void setIMCStatusListener(RemoteIMCStatusListener listener);
    // 添加拦截
    void addIMCSocketListener(in int level,RemoteV2IMCListener listener);
    // 移除拦截
    void removeIMCSocketListener(RemoteV2IMCListener listener);
}