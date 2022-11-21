package com.test.tttalk;

import com.yiyou.ga.net.protocol.PByteArray;
import com.yiyou.ga.net.protocol.YProtocol;

import java.nio.ByteBuffer;

public final class MessageBody {
    protected ByteBuffer buffer;
    public MessageBody(ByteBuffer byteBuffer0) {
        this.buffer = byteBuffer0;
    }

    public static boolean call_native_unpack(byte[] arr_b, MessageContent agqz0) {
        if(agqz0 == null) {
            agqz0 = new MessageContent();
        }

        PByteArray pByteArray0 = agqz0.get_byteArray();
        return YProtocol.unpack(0, arr_b, agqz0.len, pByteArray0);
    }

    public final MessageContent unpack_body() {
        MessageContent agqz0 = new MessageContent();
        return call_native_unpack(this.buffer.array(), agqz0) ? agqz0 : null;
    }

    public final int getBufferLength() {
        return this.buffer == null ? 0 : this.buffer.array().length;
    }

    public final ByteBuffer reset_position() {
        this.buffer.position(0);
        return this.buffer;
    }
}
