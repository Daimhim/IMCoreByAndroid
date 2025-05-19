package com.zjkj.im_core;

interface RemoteIMCStatusListener {
    void connectionClosed();
    void connectionLost();
    void connectionSucceeded();
}