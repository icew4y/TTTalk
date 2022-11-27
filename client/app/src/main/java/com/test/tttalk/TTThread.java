package com.test.tttalk;

import android.app.Application;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Printer;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.TextFormat;
import com.protobuf.protos.AutoLogin;
import com.protobuf.protos.ChatMessage;
import com.protobuf.protos.ChatMessageResp;
import com.protobuf.protos.CheckSyncKey;
import com.protobuf.protos.CheckSyncKeyResp;
import com.protobuf.protos.DeviceInfo;
import com.protobuf.protos.EnterChannelRequest;
import com.protobuf.protos.EnterChannelResponse;
import com.protobuf.protos.FollowUser;
import com.protobuf.protos.FollowUserResp;
import com.protobuf.protos.Greeting;
import com.protobuf.protos.GreetingResp;
import com.protobuf.protos.LeaveChannelRequest;
import com.protobuf.protos.LeaveChannelResponse;
import com.protobuf.protos.LoginResp;
import com.protobuf.protos.RequestSuperChannelSearch;
import com.protobuf.protos.RespNewGameChannelList;
import com.protobuf.protos.ResponseChannelUnderMicMemberList;
import com.protobuf.protos.ResponseSuperChannelSearch;
import com.test.tttalk.protocol.TTProtocol;
import com.yiyou.ga.net.protocol.YProtocol;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;

public class TTThread extends HandlerThread {
    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    private Application application;
    private String TAG = "TTTalk";
    private int seq = 4;
    public TTProtocol ttProtocol;
    private SocketChannel channel;
    private Selector selector;
    private boolean finishConnect = false;
    private String loginKey = "";
    private SendThread sendThread = new SendThread("SendThread");
    private static HashMap<Long, Long> channelIds = new HashMap();
    private int totalRead = 0;
    private MessageHeader msghead;

    private static final int num_one = 1;
    private static final int num_two = 2;
    private static final int num_three = 3;
    private static final int num_four = 4;
    private static final int num_five = 5;

    private int bodylen;

    private int limitation = 16;
    private int stage = num_one;
    private ByteBuffer readBuffer = ByteBuffer.allocate(1480);
    private ByteBuffer tempBuffer;

    public Handler getHandler() {
        return handler;
    }

    private Handler handler;
    public TTThread(String name) {
        super(name);
    }

    private void LogInfo(String info) {
        Message msg = new Message();
        msg.what = 1;
        msg.obj = info;
        MainActivity.getMainInstance().sendMsg(msg);
        Log.d(TAG, info);
    }

    public void write(ByteBuffer buffer) {
        try {
            this.channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                    Log.d(TAG, "cmd:" + this.msghead.cmd + ", bodylen:" + this.bodylen);
                    this.onRespose(this.msghead.cmd, byteBuffer0);
                    this.init_buffer();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


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
            has_read = socketChannel.read(byteBuffer0);
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


    private void onRespose(int cmd, ByteBuffer data) {
        try {
            switch (this.msghead.cmd){
                case Commands.cmd_auto_login:{//Auto Login Response
                    MessageBody messageBody = new MessageBody(data);
                    MessageContent messageContent = messageBody.unpack_body();
                    String s = ByteHexStr.bytetoHexString_(messageContent.getBytes());
                    LoginResp loginResp = LoginResp.parseFrom(messageContent.getBytes());
                    if (loginResp.getBaseResp().getErrCode() == 0){
                        LoginResp.AuthInfo authInfo = loginResp.getAuthInfo();
                        int uid = authInfo.getUserId();
                        String sessionKey = authInfo.getSessionKey().toStringUtf8();
                        long timestamp = authInfo.getTimestamp();
                        String authToken = authInfo.getAuthToken().toStringUtf8();
                        this.loginKey = authInfo.getLoginkey().toStringUtf8();
                        LogInfo("Login successfully! uid:" + uid + ", sessionKey:" + sessionKey + ", timestamp:" + timestamp + ", authToken:" + authToken);
                        LogInfo(loginResp.toString());
                        YProtocol.setUid(uid);
                        YProtocol.native_setSessionKey(sessionKey);
                        YProtocol.initAuthToken(uid, timestamp, authToken);
                        //Sync the keys and tokens, even though I still have no idea
                        //what are these items used for, but it's better than doing nothing.
                        this.check_sync_keys();
    //                                this.test_cmd31200();
    //                                this.test_cmd30330();
    //                                this.test_cmd3580();
    //                                this.test_cmd401();
                    }else{
                        LogInfo("Login failed! errCode:" + loginResp.getBaseResp().getErrCode() + ", errMsg:" + loginResp.getBaseResp().getErrMsg().toStringUtf8());
                    }


                    break;
                }
                case Commands.cmd_super_channel_search:{
                    MessageBody messageBody = new MessageBody(data);
                    MessageContent messageContent = messageBody.unpack_body();

                    ResponseSuperChannelSearch responseSuperChannelSearch = ResponseSuperChannelSearch.parseFrom(messageContent.getBytes());
                    String s = ByteHexStr.bytetoHexString_(messageContent.getBytes());
                    if (responseSuperChannelSearch.getBaseResp().getErrCode() == 0) {
                        ResponseSuperChannelSearch.SearchResp.ChannelInfo channelInfo = responseSuperChannelSearch.getSearchResp().getChannelInfo();
                        this.channelIds.put(channelInfo.getDisplayId(), channelInfo.getChannelId());
                        LogInfo("ResponseSuperChannelSearch successfully! ");
                    }else{
                        LogInfo("ResponseSuperChannelSearch failed! errCode:" + responseSuperChannelSearch.getBaseResp().getErrCode() + ", errMsg:" + responseSuperChannelSearch.getBaseResp().getErrMsg().toStringUtf8());
                    }
                    break;
                }
                case Commands.cmd_enter_channel:{//OnEnterChannel
                    MessageBody messageBody = new MessageBody(data);
                    MessageContent messageContent = messageBody.unpack_body();

                    EnterChannelResponse enterChannelResponse = EnterChannelResponse.parseFrom(messageContent.getBytes());

                    if (enterChannelResponse.getBaseResp().getErrCode() == 0) {
                        LogInfo("enterChannel successfully! ");
                    }else{
                        LogInfo("enterChannel failed! errCode:" + enterChannelResponse.getBaseResp().getErrCode() + ", errMsg:" + enterChannelResponse.getBaseResp().getErrMsg().toStringUtf8());
                    }
                    break;
                }case Commands.cmd_check_sync_key:{//unknown
                    MessageBody messageBody = new MessageBody(data);
                    byte[] messageContent = messageBody.unpack_body().getBytes();
                    CheckSyncKeyResp checkSyncKeyResp = CheckSyncKeyResp.parseFrom(messageContent);
                    if (checkSyncKeyResp.getBaseResp().getErrCode() == 0) {
                        LogInfo("CheckSyncKey successfully! ");
                    }else{
                        LogInfo("CheckSyncKey failed! errCode:" + checkSyncKeyResp.getBaseResp().getErrCode() + ", errMsg:" + checkSyncKeyResp.getBaseResp().getErrMsg().toStringUtf8());
                    }
                    break;
                }
                case Commands.cmd_public_chat:{
                    String s = ByteHexStr.bytetoHexString_(data.array());
                    MessageBody messageBody = new MessageBody(data);
                    byte[] messageContent = messageBody.unpack_body().getBytes();
                    ChatMessageResp chatMessageResp = ChatMessageResp.parseFrom(messageContent);
                    if (chatMessageResp.getBaseResp().getErrCode() == 0) {
                        LogInfo("ChatMessage message sent successfully! ");
                    }else{
                        LogInfo("ChatMessage message sent failed! errCode:" + chatMessageResp.getBaseResp().getErrCode() + ", errMsg:" + chatMessageResp.getBaseResp().getErrMsg().toStringUtf8());
                    }
                    break;
                }
                case Commands.cmd_leave_channel:{
                    MessageBody messageBody = new MessageBody(data);
                    MessageContent messageContent = messageBody.unpack_body();
                    LeaveChannelResponse leaveChannelResponse = LeaveChannelResponse.parseFrom(messageContent.getBytes());
                    if (leaveChannelResponse.getBaseResp().getErrCode() == 0) {
                        LogInfo("leaveChannelResponse successfully!");
                    }else{
                        LogInfo("leaveChannelResponse failed! errCode:" + leaveChannelResponse.getBaseResp().getErrCode() + ", errMsg:" + leaveChannelResponse.getBaseResp().getErrMsg().toStringUtf8());
                    }
                    break;
                }
                case Commands.cmd_follow_user:{
                    MessageBody messageBody = new MessageBody(data);
                    MessageContent messageContent = messageBody.unpack_body();
                    FollowUserResp followUserResp = FollowUserResp.parseFrom(messageContent.getBytes());
                    if (followUserResp.getBaseResp().getErrCode() == 0) {
                        LogInfo("follow User successfully! followed User Id:" + followUserResp.getFollowedUserId());
                    }else{
                        LogInfo("followUser failed! errCode:" + followUserResp.getBaseResp().getErrCode() + ", errMsg:" + followUserResp.getBaseResp().getErrMsg().toStringUtf8());
                    }
    //
                    break;
                }
                case Commands.cmd_greetings:{
                    MessageBody messageBody = new MessageBody(data);
                    byte[] messageContent = messageBody.unpack_body().getBytes();
                    GreetingResp greetingResp = GreetingResp.parseFrom(messageContent);
                    if (greetingResp.getBaseResp().getErrCode() == 0) {
                        LogInfo("Greetings message sent successfully! ");
                    }else{
                        LogInfo("Greetings message sent failed! errCode:" + greetingResp.getBaseResp().getErrCode() +
                                ", errMsg:" + greetingResp.getBaseResp().getErrMsg().toStringUtf8());
                    }
                    break;
                }
                case Commands.cmd_req_game_channel_list:{
                    MessageBody messageBody = new MessageBody(data);
                    byte[] messageContent = messageBody.unpack_body().getBytes();
                    RespNewGameChannelList respNewGameChannelList = RespNewGameChannelList.parseFrom(messageContent);
                    this.on_game_channel_list(respNewGameChannelList);
                    break;
                }
                case Commands.cmd_req_under_mic_member_list:{
                    MessageBody messageBody = new MessageBody(data);
                    byte[] messageContent = messageBody.unpack_body().getBytes();
                    ResponseChannelUnderMicMemberList respMicMemberList = ResponseChannelUnderMicMemberList.parseFrom(messageContent);
                    this.on_game_channel_member_list(respMicMemberList);
                    break;
                }
                case 6:{//unknown
                    break;
                }
                default:
                    Log.d(TAG, "unknown response cmd:" + this.msghead.cmd);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        this.handler = new Handler(getLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
            }
        };

        this.init(getApplication(),"13580590620", "296475024", "1",
                "555f6144739d62894deff36b0d0b62ec", "BW82Bo7zrUTj2zq4puKdTJMjOmOV+SqsEhkiHJlgX7MeFlSa3Ex8p+kZiabXs32rc0qkPuXHnTSD9kWlbDpFR6Q==",
                "17b6d6731ed4f34acc0d0d8cdc202b88", "fbddc0a64a19b097",
                "Mozilla/5.0 (Linux; Android 10; AOSP on crosshatch Build/QP1A.190711.020; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/74.0.3729.186 Mobile Safari/537.36 TTVersion/6.10.1 TTFrom/tt",
                "Google Pixel3 XL");
        this.start_connect_and_work();
    }


    public void auto_login() {
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.auto_login())
                );
            }
        });
    }

    public void search_channel(String keyword){
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.search_channel(keyword))
                );
            }
        });

    }

    public void enter_channel(long displayId){
        String dislayId = String.valueOf(displayId);
        if (!this.channelIds.containsKey(Long.valueOf(dislayId))){
            LogInfo("ChannelId doesn't exist in the channelIds!" + dislayId);
            return;
        }
        Long channelId = this.channelIds.get(Long.valueOf(dislayId));
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.enter_channel(channelId, displayId))
                );
            }
        });

    }

    public void leave_channel(long displayId) {
        String dislayId = String.valueOf(displayId);
        if (!this.channelIds.containsKey(Long.valueOf(dislayId))){
            LogInfo("ChannelId doesn't exist in the channelIds!" + dislayId);
            return;
        }
        Long channelId = this.channelIds.get(Long.valueOf(dislayId));
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.leave_channel(channelId))
                );
            }
        });

    }

    public void follow_user(String followUserAccount, long displayId, String tagName) {
        String _dislayId = String.valueOf(displayId);
        if (!this.channelIds.containsKey(Long.valueOf(_dislayId))){
            LogInfo("ChannelId doesn't exist in the channelIds!" + _dislayId);
            return;
        }
        Long channelId = this.channelIds.get(Long.valueOf(_dislayId));
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.follow_user(followUserAccount, channelId, displayId, tagName))
                );
            }
        });

    }

    public void channel_chat_text(long displayId, String content) {
        String _dislayId = String.valueOf(displayId);
        if (!this.channelIds.containsKey(Long.valueOf(_dislayId))){
            LogInfo("ChannelId doesn't exist in the channelIds!" + _dislayId);
            return;
        }
        Long channelId = this.channelIds.get(Long.valueOf(_dislayId));
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.channel_chat_text(channelId, content))
                );
            }
        });

    }

    public void greet(String toAccount, String content) {
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.greet(toAccount, content, loginKey))
                );
            }
        });

    }

    public void req_new_game_channel_list(int tagId, int gameMode, int count) {
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.req_new_game_channel_list(tagId, gameMode, count))
                );
            }
        });

    }

    public void req_channel_under_mic_member_list(long displayId) {
        String _dislayId = String.valueOf(displayId);
        if (!this.channelIds.containsKey(Long.valueOf(_dislayId))){
            LogInfo("ChannelId doesn't exist in the channelIds!" + _dislayId);
            return;
        }
        Long channelId = this.channelIds.get(Long.valueOf(_dislayId));
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.req_channel_under_mic_member_list(channelId))
                );
            }
        });

    }

    public void check_sync_keys() {
        this.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.check_sync_keys())
                );
            }
        });
    }

    public void start_connect_and_work() {
        this.getHandler().post(new Runnable() {
            @Override
            public void run() {
                sendThread.start();
                try {
                    int port = 8080;
                    channel = SocketChannel.open();
                    selector = Selector.open();
                    // we open this channel in non blocking mode
                    channel.configureBlocking(false);
                    channel.connect(new InetSocketAddress("lvs.52tt.com", port));
                    channel.register(selector, SelectionKey.OP_CONNECT);
                    connect(1000);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void on_game_channel_list(RespNewGameChannelList channelList) {
        if (channelList.getBaseResp().getErrCode() == 0) {
            LogInfo("RespNewGameChannelList successfully! ");
            LogInfo(channelList.toString());
//            if (channelList.getChannelListCount() > 0) {
//                for (RespNewGameChannelList.ChannelList channel : channelList.getChannelListList()){
//                    LogInfo("ChannelId:" + channel.getChannelId() + ", Name:" + channel.getChannelName().toStringUtf8());
//                }
//
//            }
        }else{
            LogInfo("RespNewGameChannelList failed! errCode:" + channelList.getBaseResp().getErrCode() +
                    ", errMsg:" + channelList.getBaseResp().getErrMsg().toStringUtf8());
        }
    }

    private void on_game_channel_member_list(ResponseChannelUnderMicMemberList memberList) {
        if (memberList.getBaseResp().getErrCode() == 0) {
            LogInfo("ResponseChannelUnderMicMemberList successfully!");
            LogInfo(memberList.toString());
//            LogInfo("ChannelId:" + memberList.getChannelId() + ", MemberCount:" + memberList.getCurrMemberTotal());
//            if (memberList.getCurrMemberTotal() > 0) {
//                for (ResponseChannelUnderMicMemberList.ChannelMemberInfo memberInfo : memberList.getChannelMemberInfoList()) {
//                    LogInfo("channelMemberInfo:" + memberInfo.toString());
//                }
//            }
        }else{
            LogInfo("ResponseChannelUnderMicMemberList failed! errCode:" + memberList.getBaseResp().getErrCode() +
                    ", errMsg:" + memberList.getBaseResp().getErrMsg().toStringUtf8());
        }
    }

    public void init(Application application, String acc, String uid, String acc_type, String pwd, String deviceID,
                     String deviceIDV2, String androidid, String key_web_ua, String deviceModel){
        this.ttProtocol = new TTProtocol("13580590620", "296475024", "1",
                "555f6144739d62894deff36b0d0b62ec", "BW82Bo7zrUTj2zq4puKdTJMjOmOV+SqsEhkiHJlgX7MeFlSa3Ex8p+kZiabXs32rc0qkPuXHnTSD9kWlbDpFR6Q==",
                "17b6d6731ed4f34acc0d0d8cdc202b88", "fbddc0a64a19b097",
                "Mozilla/5.0 (Linux; Android 10; AOSP on crosshatch Build/QP1A.190711.020; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/74.0.3729.186 Mobile Safari/537.36 TTVersion/6.10.1 TTFrom/tt",
                "Google Pixel3 XL");
        this.ttProtocol.init(application);
    }
}
