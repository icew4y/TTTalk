package com.test.tttalk;
import android.util.Log;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.protobuf.protos.ChatMessageResp;
import com.protobuf.protos.CheckSyncKey;
import com.protobuf.protos.CheckSyncKeyResp;
import com.protobuf.protos.EnterChannelResponse;
import com.protobuf.protos.FollowUserResp;
import com.protobuf.protos.GreetingResp;
import com.protobuf.protos.LeaveChannelResponse;
import com.protobuf.protos.LoginResp;
import com.yiyou.ga.net.protocol.PByteArray;
import com.yiyou.ga.net.protocol.YProtocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

public class TTSocketChannel {
    private static String TAG = "TTSocketChannel";


    public static int seq = 4;
    private SocketChannel channel;
    private Selector selector;
    private boolean finishConnect = false;

    public void CheckSyncKey() {
        this.seq ++;
        CheckSyncKey.Builder syncKeyBuilder = CheckSyncKey.newBuilder()
                .setUnkobj1(CheckSyncKey.Unknown1.newBuilder().build())
                .setUnknown4(1)
                .setVersion(ByteString.copyFromUtf8("6.10.1"))
                .setVersionCode(16243)
                .setOfficial(ByteString.copyFromUtf8("official"));

        CheckSyncKey checkSyncKey = syncKeyBuilder.build();

        //byte[] data = ByteHexStr.hexToByteArray("0a0020012a06362e31302e3130f37e3a086f6666696369616c");
        PByteArray outByteArray = new PByteArray();
        boolean ret = YProtocol.pack(Commands.cmd_check_sync_key, checkSyncKey.toByteArray(), outByteArray, false, 0);
        byte[] pack_header_bytes = MainActivity.pack_header(Commands.cmd_check_sync_key, this.seq, (short) 0, outByteArray.value);
        ByteBuffer byteBuffer = ByteBuffer.wrap(pack_header_bytes);
        this.write(byteBuffer);
    }

    public void test_cmd30330() {
        this.seq ++;
        int cmd = 30330;
        byte[] data = ByteHexStr.hexToByteArray("0a00");
        PByteArray outByteArray = new PByteArray();
        boolean ret = YProtocol.pack(cmd, data, outByteArray, false, 0);
        byte[] pack_header_bytes = MainActivity.pack_header(cmd, this.seq, (short) 0, outByteArray.value);
        ByteBuffer byteBuffer = ByteBuffer.wrap(pack_header_bytes);
        this.write(byteBuffer);
    }

    public void leaveRoom(int roomId){
        this.seq ++;
        int cmd = 424;
        byte[] data = ByteHexStr.hexToByteArray("0a0010e99aaf4e");
        PByteArray outByteArray = new PByteArray();
        boolean ret = YProtocol.pack(cmd, data, outByteArray, false, 0);
        byte[] pack_header_bytes = MainActivity.pack_header(cmd, this.seq, (short) 0, outByteArray.value);
        ByteBuffer byteBuffer = ByteBuffer.wrap(pack_header_bytes);
        this.write(byteBuffer);
    }
    public void chat_text(byte[] chatBytes){
        this.seq ++;
        int cmd = Commands.cmd_public_chat;
        //byte[] data = ByteHexStr.hexToByteArray(chatBytes);
        PByteArray outByteArray = new PByteArray();
        boolean ret = YProtocol.pack(cmd, chatBytes, outByteArray, false, 0);
        byte[] pack_header_bytes = MainActivity.pack_header(cmd, this.seq, (short) 0, outByteArray.value);
        ByteBuffer byteBuffer = ByteBuffer.wrap(pack_header_bytes);
        this.write(byteBuffer);
    }
    public void test_cmd3580() {
        this.seq ++;
        int cmd = 3580;
        byte[] data = ByteHexStr.hexToByteArray("0a0010bd89bf8a01");
        PByteArray outByteArray = new PByteArray();
        boolean ret = YProtocol.pack(cmd, data, outByteArray, false, 0);
        byte[] pack_header_bytes = MainActivity.pack_header(cmd, this.seq, (short) 0, outByteArray.value);
        ByteBuffer byteBuffer = ByteBuffer.wrap(pack_header_bytes);
        this.write(byteBuffer);
    }

    public void test_cmd401() {
        this.seq ++;
        int cmd = 401;
        byte[] data = ByteHexStr.hexToByteArray("0a0010011a20636666633263353630613166636265626466656331613731346431653166343720032a023238420c636f6d2e7969796f752e67614804");
        PByteArray outByteArray = new PByteArray();
        boolean ret = YProtocol.pack(cmd, data, outByteArray, false, 0);
        byte[] pack_header_bytes = MainActivity.pack_header(cmd, this.seq, (short) 0, outByteArray.value);
        ByteBuffer byteBuffer = ByteBuffer.wrap(pack_header_bytes);
        this.write(byteBuffer);
    }

    public void test_cmd31200() {
        this.seq ++;
        int cmd = 31200;
        byte[] data = ByteHexStr.hexToByteArray("0a00");
        PByteArray outByteArray = new PByteArray();
        boolean ret = YProtocol.pack(cmd, data, outByteArray, false, 0);
        byte[] pack_header_bytes = MainActivity.pack_header(cmd, this.seq, (short) 0, outByteArray.value);
        ByteBuffer byteBuffer = ByteBuffer.wrap(pack_header_bytes);
        this.write(byteBuffer);
    }
    public void doReceive(SelectionKey selectionKey0) {

        SocketChannel socketChannel0 = (SocketChannel)selectionKey0.channel();

        try {
            if (this.read_Head(socketChannel0) > 0) {
                if (this.stage == num_two) {
                    this.read_new_version_head(socketChannel0);
                }
                if (this.stage == num_three) {
                    this.readBody(socketChannel0);
                }
                if (this.stage == num_four) {
                    byte[] arr_b = new byte[this.bodylen];
                    System.arraycopy(this.tempBuffer.array(), this.limitation, arr_b, 0, this.bodylen);
                    ByteBuffer byteBuffer0 = ByteBuffer.wrap(arr_b);
                    System.out.println("cmd:" + this.msghead.cmd + ", bodylen:" + this.bodylen);
                    switch (this.msghead.cmd){
                        case Commands.cmd_auto_login:{//Auto Login Response
                            MessageBody messageBody = new MessageBody(byteBuffer0);
                            MessageContent messageContent = messageBody.unpack_body();
                            String s = ByteHexStr.bytetoHexString_(messageContent.getBytes());
                            LoginResp loginResp = LoginResp.parseFrom(messageContent.getBytes());
                            if (loginResp.getBaseResp().getErrCode() == 0){
                                LoginResp.AuthInfo authInfo = loginResp.getAuthInfo();

                                int uid = authInfo.getUserId();
                                String sessionKey = authInfo.getSessionKey().toStringUtf8();
                                long timestamp = authInfo.getTimestamp();
                                String authToken = authInfo.getAuthToken().toStringUtf8();
                                MainActivity.loginKey = authInfo.getLoginkey().toStringUtf8();
                                Log.d(TAG, "Login successfully! uid:" + uid + ", sessionKey:" + sessionKey + ", timestamp:" + timestamp + ", authToken:" + authToken);
                                YProtocol.setUid(uid);
                                YProtocol.native_setSessionKey(sessionKey);
                                YProtocol.initAuthToken(uid, timestamp, authToken);
                                System.out.println(s);


                                //Sync the keys and tokens, even though I still have no idea
                                //what are these items used for, but it's better than doing nothing.
                                this.CheckSyncKey();
//                                this.test_cmd31200();
//                                this.test_cmd30330();
//                                this.test_cmd3580();
//                                this.test_cmd401();
                            }else{
                                Log.d(TAG, "Login failed! errCode:" + loginResp.getBaseResp().getErrCode() + ", errMsg:" + loginResp.getBaseResp().getErrMsg().toStringUtf8());
                            }


                            break;
                        }
                        case Commands.cmd_enter_channel:{//OnEnterChannel
                            MessageBody messageBody = new MessageBody(byteBuffer0);
                            MessageContent messageContent = messageBody.unpack_body();

                            EnterChannelResponse enterChannelResponse = EnterChannelResponse.parseFrom(messageContent.getBytes());

                            if (enterChannelResponse.getBaseResp().getErrCode() == 0) {
                                Log.d(TAG, "enterChannel successfully! ");
                            }else{
                                Log.d(TAG, "enterChannel failed! errCode:" + enterChannelResponse.getBaseResp().getErrCode() + ", errMsg:" + enterChannelResponse.getBaseResp().getErrMsg().toStringUtf8());
                            }
                            break;
                        }case Commands.cmd_check_sync_key:{//unknown
                            MessageBody messageBody = new MessageBody(byteBuffer0);
                            byte[] messageContent = messageBody.unpack_body().getBytes();
                            CheckSyncKeyResp checkSyncKeyResp = CheckSyncKeyResp.parseFrom(messageContent);
                            if (checkSyncKeyResp.getBaseResp().getErrCode() == 0) {
                                Log.d(TAG, "CheckSyncKey successfully! ");
                            }else{
                                Log.d(TAG, "CheckSyncKey failed! errCode:" + checkSyncKeyResp.getBaseResp().getErrCode() + ", errMsg:" + checkSyncKeyResp.getBaseResp().getErrMsg().toStringUtf8());
                            }
                            break;
                        }
                        case Commands.cmd_public_chat:{
                            MessageBody messageBody = new MessageBody(byteBuffer0);
                            byte[] messageContent = messageBody.unpack_body().getBytes();
                            ChatMessageResp chatMessageResp = ChatMessageResp.parseFrom(messageContent);
                            if (chatMessageResp.getBaseResp().getErrCode() == 0) {
                                Log.d(TAG, "ChatMessage message sent successfully! ");
                            }else{
                                Log.d(TAG, "ChatMessage message sent failed! errCode:" + chatMessageResp.getBaseResp().getErrCode() + ", errMsg:" + chatMessageResp.getBaseResp().getErrMsg().toStringUtf8());
                            }
                            break;
                        }
                        case Commands.cmd_leave_channel:{
                            MessageBody messageBody = new MessageBody(byteBuffer0);
                            MessageContent messageContent = messageBody.unpack_body();
                            LeaveChannelResponse leaveChannelResponse = LeaveChannelResponse.parseFrom(messageContent.getBytes());
                            if (leaveChannelResponse.getBaseResp().getErrCode() == 0) {
                                Log.d(TAG, "leaveChannelResponse successfully!");
                            }else{
                                Log.d(TAG, "leaveChannelResponse failed! errCode:" + leaveChannelResponse.getBaseResp().getErrCode() + ", errMsg:" + leaveChannelResponse.getBaseResp().getErrMsg().toStringUtf8());
                            }
                            break;
                        }
                        case Commands.cmd_follow_user:{
                            MessageBody messageBody = new MessageBody(byteBuffer0);
                            MessageContent messageContent = messageBody.unpack_body();
                            FollowUserResp followUserResp = FollowUserResp.parseFrom(messageContent.getBytes());
                            if (followUserResp.getBaseResp().getErrCode() == 0) {
                                Log.d(TAG, "follow User successfully! followed User Id:" + followUserResp.getFollowedUserId());
                            }else{
                                Log.d(TAG, "followUser failed! errCode:" + followUserResp.getBaseResp().getErrCode() + ", errMsg:" + followUserResp.getBaseResp().getErrMsg().toStringUtf8());
                            }
//
                            break;
                        }
                        case Commands.cmd_greetings:{
                            MessageBody messageBody = new MessageBody(byteBuffer0);
                            byte[] messageContent = messageBody.unpack_body().getBytes();
                            GreetingResp greetingResp = GreetingResp.parseFrom(messageContent);
                            if (greetingResp.getBaseResp().getErrCode() == 0) {
                                Log.d(TAG, "Greetings message sent successfully! ");
                            }else{
                                Log.d(TAG, "Greetings message sent failed! errCode:" + greetingResp.getBaseResp().getErrCode() + ", errMsg:" + greetingResp.getBaseResp().getErrMsg().toStringUtf8());
                            }
                            break;
                        }
                        case 6:{//unknown
                            break;
                        }
                        default:
                            Log.d(TAG, "unknown response cmd:" + this.msghead.cmd);
                            break;
                    }
                    this.init_buffer();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public final void connect(long timeout) throws IOException, ClosedSelectorException {
        int v1 = 1;
        if(this.selector != null && (this.selector.isOpen())) {
            while (this.selector.select() > 0) {
                Iterator iterator0 = this.selector.selectedKeys().iterator();
                while(iterator0.hasNext()) {
                    Object object0 = iterator0.next();
                    SelectionKey selectionKey0 = (SelectionKey)object0;
                    iterator0.remove();
                    if(!selectionKey0.isValid()) {
                        Log.d("BaseSocket", selectionKey0 + " is not valid");
                        continue;
                    }

                    if(selectionKey0.isConnectable()) {
                        Log.d("BaseSocket", "key isConnectable");
                        Log.d("BaseSocket", "doConnect");
                        SocketChannel socketChannel0 = (SocketChannel)selectionKey0.channel();
                        if(!selectionKey0.isValid()) {
                            selectionKey0.cancel();
                            continue;
                        }

                        selectionKey0.interestOps(1);
                        try {
                            this.finishConnect = socketChannel0.finishConnect();
                            Log.d(TAG, "finishConnect!");
                            this.init_buffer();
                            continue;
                        }
                        catch(IOException unused_ex) {
                            return;
                        }
                    }

                    if(selectionKey0.isReadable()) {
                        Log.d("BaseSocket", "key.isReadable");
                        this.doReceive(selectionKey0);
                        continue;
                    }

                    if(selectionKey0.isWritable()) {
                        Log.d("BaseSocket", "key.isWritable");
                        continue;
                    }

                    Log.d("BaseSocket", "key.others");
                }
            }
            if(!this.finishConnect) {
                Log.d("BaseSocket", "connect to %s:%d timeout");
            }

            return;
        }

        StringBuilder stringBuilder0 = new StringBuilder("doSelector err, selector == null []");
        if(this.selector != null) {
            v1 = 0;
        }

        stringBuilder0.append(v1);
        Log.d("BaseSocket", stringBuilder0.toString());
    }

    public void write(ByteBuffer buffer) {
        try {
            this.channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void main() {
        try {
            int port = 8080;
            this.channel = SocketChannel.open();
            this.selector = Selector.open();
            // we open this channel in non blocking mode
            channel.configureBlocking(false);
            this.channel.connect(new InetSocketAddress("lvs.52tt.com", port));
            this.channel.register(this.selector, SelectionKey.OP_CONNECT);
            connect(1000);



        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int totalRead = 0;
    public MessageHeader msghead;

    public static final int num_one = 1;
    public static final int num_two = 2;
    public static final int num_three = 3;
    public static final int num_four = 4;
    public static final int num_five = 5;

    public int bodylen;

    public int limitation = 16;
    public int stage = num_one;
    public ByteBuffer readBuffer = ByteBuffer.allocate(1480);
    public ByteBuffer tempBuffer;
    //private MessageHeader messageHeader;
    public void init_buffer() {
        this.stage = num_one;
        this.totalRead = 0;
        this.limitation = 16;
        if(this.tempBuffer != null) {
            this.tempBuffer.clear();
            this.tempBuffer = null;
        }
        this.readBuffer.clear();
        this.readBuffer.limit(this.limitation);
        this.readBuffer.position(0);
    }

    private void b() {
        this.bodylen = this.msghead.tl - this.msghead.hl;
        if(this.bodylen <= 0) {
            this.stage = this.num_four;
            this.tempBuffer = this.readBuffer;
            return;
        }

        int v = this.limitation;
        int v1 = this.bodylen;
        if(this.readBuffer.capacity() - v >= v1) {
            this.readBuffer.position(this.totalRead);
            this.tempBuffer = this.readBuffer;
        }
        else if(this.msghead.tl > 1480) {
            this.tempBuffer = ByteBuffer.allocate(this.msghead.tl);
            this.tempBuffer.put(this.readBuffer.array(), 0, this.msghead.hl);
            //hat.b.a(agqx.g, "allocate temp buffer, temp buffer size = %d", new Object[]{((int)this.tempBuffer.capacity())});
        }
        else {
            ByteBuffer byteBuffer0 = ByteBuffer.allocate(this.msghead.tl);
            this.tempBuffer = byteBuffer0;
            this.tempBuffer.put(this.readBuffer.array(), 0, this.msghead.hl);
            this.readBuffer = byteBuffer0;
            //hat.b.a(agqx.g, "larger buffer, new buffer size = %d", new Object[]{((int)this.tempBuffer.capacity())});
        }

        this.tempBuffer.position(this.limitation);
        this.tempBuffer.limit(this.msghead.tl);
        this.stage = this.num_three;
    }

    public final synchronized int read_Head(SocketChannel socketChannel){
        try {
            int has_read = socketChannel.read(this.readBuffer);
            this.totalRead += has_read;
            if (has_read == 16) {
                this.readBuffer.position(0);
                this.msghead = new MessageHeader(this.readBuffer);
                if(this.msghead.hl != 20 && this.msghead.hl != 16 || this.msghead.tl < this.msghead.hl) {
                    Log.d(TAG, "receive invalid data");
                    this.init_buffer();
                    return 0;
                }
                Log.d(TAG, "msghead.cmd:" + msghead.cmd + ", total_len:" + msghead.tl);
                if (this.msghead.hl > 16) {
                    this.limitation = this.msghead.hl;
                    this.readBuffer.limit(this.limitation);
                    this.stage = num_two;
                }else{
                    this.b();
                }
            }

            return this.totalRead;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this.totalRead;
    }

    public final synchronized int read_new_version_head(SocketChannel socketChannel) {
        int has_read = 0;
        try {
            has_read = socketChannel.read(this.readBuffer);
            if (has_read > 0) {
                this.totalRead += has_read;
            }

            if (this.totalRead == this.limitation) {
                this.readBuffer.position(16);
                MessageHeader messageHeader = this.msghead;
                ByteBuffer byteBuffer = this.readBuffer;
                if (messageHeader.v == 2) {
                    messageHeader.f = byteBuffer.getShort();
                    messageHeader.g = byteBuffer.getShort();
                }
                this.b();
            }else{
                this.readBuffer.position(this.totalRead);
                this.stage = num_one;
            }
            return this.totalRead;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return has_read;
    }

    public final int readBody(SocketChannel socketChannel) {
        int has_read = 0;
        try {
            ByteBuffer byteBuffer0 = this.tempBuffer;
            has_read = socketChannel.read(this.readBuffer);
            this.totalRead += has_read;
            if(this.totalRead >= this.msghead.tl) {
                this.stage = this.num_four;
            }
            else {
                byteBuffer0.position(this.totalRead);
            }
            return this.totalRead;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return has_read;
    }
}
