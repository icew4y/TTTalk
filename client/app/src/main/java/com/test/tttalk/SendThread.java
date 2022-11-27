package com.test.tttalk;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;

public class SendThread extends HandlerThread {

    public Handler getHandler() {
        return handler;
    }

    private Handler handler;
    public SendThread(String name) {
        super(name);
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        handler = new Handler(getLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
            }
        };
    }
}
