package com.yiyou.ga.net.protocol;

import android.app.Application;
import android.os.SystemClock;

import androidx.annotation.Keep;

public class YProtocol {
    public static Application sContextHolder;
    public static native String native_TT_a(PInt pInt);

    private static native int native_TT_b(int i, long j, long j2, String str);

    private static native int native_TT_c(int i, long j, PByteArray pByteArray);

    public static native int native_TT_d(String str, String str2, String str3);

    public static native String native_TT_e();

    public static native boolean native_init(Object obj, boolean z);

    public static native boolean native_setClientVersion(int i);

    public static native boolean native_setDeviceId(String str);

    public static native boolean native_setSessionKey(String str);

    private static native boolean native_setUid(int i);

    public static native boolean pack(int cmd, byte[] data, PByteArray outByteArray, boolean z, int i2);

    public static native boolean unpack(int i, byte[] bArr, PInt pInt, PByteArray pByteArray);

    static {
        System.loadLibrary("protocol");
    }

    @Keep
    public static String getInternalDir() {
        return sContextHolder.getFilesDir().getAbsolutePath();
    }

    public static boolean setUid(int i) {
        boolean native_setUid = native_setUid(i);
        return native_setUid;
    }

    public static boolean initAuthToken(int uid, long resp_timestamp, String str) {
        int native_TT_b = native_TT_b(uid, resp_timestamp, SystemClock.elapsedRealtime(), str);
        return native_TT_b == 0;
    }

    public static String getWebToken(int i) {
        PByteArray pByteArray = new PByteArray();
        native_TT_c(i, SystemClock.elapsedRealtime(), pByteArray);
        return pByteArray.value == null ? "" : new String(pByteArray.value);
    }
}

