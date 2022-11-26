package com.test.tttalk;

import com.yiyou.ga.net.protocol.PByteArray;
import com.yiyou.ga.net.protocol.PInt;

public final class MessageContent{
    protected PByteArray byteArray;
    protected PInt len;

    public MessageContent() {
        this.byteArray = new PByteArray();
        this.len = new PInt();
    }

    public MessageContent(byte[] arr_b) {
        this.byteArray = new PByteArray();
        this.len = new PInt();
        this.a(arr_b);
    }

    protected final void a(byte[] arr_b) {
        PByteArray pByteArray0 = this.byteArray;
        pByteArray0.value = new byte[arr_b.length];
        System.arraycopy(arr_b, 0, pByteArray0.value, 0, arr_b.length);
    }

    public final byte[] getBytes() {
        return this.getValueBytes();
    }

    public final int b() {
        return this.getValueBytes() == null ? 0 : this.getValueBytes().length;
    }

    protected final PByteArray get_byteArray() {
        return this.byteArray;
    }

    public final byte[] getValueBytes() {
        return this.byteArray == null ? null : this.byteArray.value;
    }

    public final int e() {
        return this.len == null ? -999 : this.len.value;
    }
}
