package com.zjkj.im_core;

interface RemoteV2IMCListener {
    boolean onMessageByte(
        String md5,
        int index,
        int length,
        in byte[] data
    );
    boolean onMessageString(
        String md5,
        int index,
        int length,
        in byte[] data
    );
}