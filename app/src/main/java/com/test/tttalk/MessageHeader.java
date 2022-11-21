package com.test.tttalk;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;

public final class MessageHeader {
    public int tl; //tl len??
    public int hl;//head len??
    public int v; // version?
    public int cmd;
    public int seq;
    public int f;
    public int g;

    private MessageHeader() {
    }

    MessageHeader(ByteBuffer byteBuffer0) {
        this.tl = byteBuffer0.getInt();
        this.hl = byteBuffer0.getShort();
        this.v = byteBuffer0.getShort();
        this.cmd = byteBuffer0.getInt();
        this.seq = byteBuffer0.getInt();
        byteBuffer0.mark();
    }

    @NonNull
    public final MessageHeader a() {
        try {
            return (MessageHeader)super.clone();
        }
        catch(CloneNotSupportedException unused_ex) {
            return new MessageHeader();
        }
    }

    @Override
    @NonNull
    protected final Object clone() throws CloneNotSupportedException {
        return this.a();
    }
}
