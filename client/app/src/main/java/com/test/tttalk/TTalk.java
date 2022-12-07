package com.test.tttalk;

import android.app.Application;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.protobuf.InvalidProtocolBufferException;
import com.protobuf.protos.ChatMessageResp;
import com.protobuf.protos.CheckSyncKeyResp;
import com.protobuf.protos.EmptyMessage;
import com.protobuf.protos.EnterChannelResponse;
import com.protobuf.protos.FollowUserResp;
import com.protobuf.protos.GreetingResp;
import com.protobuf.protos.LeaveChannelResponse;
import com.protobuf.protos.LoginResp;
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
import java.util.HashMap;
import java.util.Iterator;

public class TTalk extends HandlerThread {
    interface ICallback{
        public void callback(Object o);
    }

    private Application application;
    private String TAG = "TTTalk";
    private int seq = 4;
    private TTProtocol ttProtocol;
    private SocketChannel socketChannel;
    private Selector selector;
    private boolean finishConnect = false;
    private boolean isLoggedIn = false;
    private boolean isDisconnected = false;
    private String loginKey = "";
    private SendThread sendThread = new SendThread("SendThread");
    private HashMap<Long, Long> channelIds = new HashMap();

    public String getAccount() {
        return account;
    }

    //TTalk account Id
    private String account = "";

    public HashMap<Long, RespNewGameChannelList.ChannelList> getChannels() {
        return channels;
    }

    private HashMap<Long, RespNewGameChannelList.ChannelList> channels = new HashMap<>();
    private int totalRead = 0;
    private MessageHeader msghead;

    private static final int READING_HEADER = 1;
    private static final int READING_NEW_VERSION_HEADER = 2;
    private static final int READING_BODY = 3;
    private static final int FINISHED = 4;
    private static final int ERROR = 5;

    private ICallback channelListCb = null;
    private ICallback channelMemberListCb = null;
    private ICallback greetsCb = null;
    private ICallback publicChatCb = null;
    private ICallback followCb = null;
    private int bodylen;

    private int limitation = 16;
    private int stage = READING_HEADER;
    private ByteBuffer readBuffer = ByteBuffer.allocate(1480);
    private ByteBuffer tempBuffer;
    private Handler handler;
    public Handler getHandler() {
        return handler;
    }

    public Application getApplication() {
        return application;
    }

    private void setApplication(Application application) {
        this.application = application;
    }

    public TTalk() {
        super("TTThread");
    }

    public void setAccountCookie(Application application, String jsonCookie) {
        try {
            this.setApplication(application);
            JSONObject jsonObject = new JSONObject(jsonCookie);

            String acc = jsonObject.getString("acc");
            String uid = jsonObject.getString("uid");
            String acc_type = jsonObject.getString("acc_type");
            String pwd = jsonObject.getString("pwd");
            String deviceID = jsonObject.getString("deviceID");
            String deviceIdV2 = jsonObject.getString("deviceIdV2");
            String androidid = jsonObject.getString("androidid");
            String key_web_ua = jsonObject.getString("key_web_ua");
            String deviceModel = "Google Pixel3 XL";

            this.init(getApplication(),acc, uid, acc_type,
                    pwd, deviceID,
                    deviceIdV2, androidid,
                    key_web_ua,
                    deviceModel);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void LogInfo(String info) {
        Message msg = new Message();
        msg.what = 1;
        msg.obj = info;
        MainActivity.getMainInstance().sendMsg(msg);
        Log.d(TAG, info);
    }

    private void setChannelId(String ChannelId) {
        Message msg = new Message();
        msg.what = 2;
        msg.obj = ChannelId;
        MainActivity.getMainInstance().sendMsg(msg);
    }

    public void write(ByteBuffer buffer) {
        try {
            this.socketChannel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doReceive(SelectionKey selectionKey0) {

        SocketChannel socketChannel0 = (SocketChannel)selectionKey0.channel();

        try {
            int read = 0;
            if (this.stage == READING_HEADER) {
                read = this.read_Head(socketChannel0);
            }
            if (this.stage == READING_NEW_VERSION_HEADER) {
                read = this.read_new_version_head(socketChannel0);
            }
            if (this.stage == READING_BODY) {
                read = this.readBody(socketChannel0);
            }
            if (read <= 0) {
                LogInfo("[-DEBUG] receive size <= 0, socket might be down");
                selectionKey0.cancel();
                socketChannel0.close();
                if (socketChannel.isOpen() && socketChannel != null) {
                    socketChannel.close();
                }
                if (selector.isOpen() && selector != null) {
                    selector.close();
                }
            }
            if (this.stage == FINISHED) {
                byte[] arr_b = new byte[this.bodylen];
                System.arraycopy(this.tempBuffer.array(), this.limitation, arr_b, 0, this.bodylen);
                ByteBuffer byteBuffer0 = ByteBuffer.wrap(arr_b);
                Log.d(TAG, "cmd:" + this.msghead.cmd + ", bodylen:" + this.bodylen);
                this.onResponse(this.msghead.cmd, byteBuffer0);
                this.init_buffer();
            }

            if (this.stage == ERROR) {
                LogInfo("[-DEBUG] Error occur, close socket");
                selectionKey0.cancel();
                socketChannel0.close();
                if (socketChannel.isOpen() && socketChannel != null) {
                    socketChannel.close();
                }
                if (selector.isOpen() && selector != null) {
                    selector.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //private MessageHeader messageHeader;
    public void init_buffer() {
        this.stage = READING_HEADER;
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
            this.stage = this.FINISHED;
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
        this.stage = this.READING_BODY;
    }

    public final synchronized int read_Head(SocketChannel socketChannel){
        try {
            int has_read = socketChannel.read(this.readBuffer);
            if (has_read <= 0) {
                this.stage = ERROR;
                return has_read;
            }
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
                    this.stage = READING_NEW_VERSION_HEADER;
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
            if (has_read <= 0) {
                this.stage = ERROR;
                return has_read;
            }
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
                this.stage = READING_HEADER;
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
            if (has_read <= 0) {
                this.stage = ERROR;
                return has_read;
            }
            this.totalRead += has_read;
            if(this.totalRead >= this.msghead.tl) {
                this.stage = this.FINISHED;
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
                        //Log.d("BaseSocket", "key isConnectable");
                        Log.d("BaseSocket", "doConnect");
                        SocketChannel socketChannel0 = (SocketChannel)selectionKey0.channel();
                        if(!selectionKey0.isValid()) {
                            selectionKey0.cancel();
                            continue;
                        }

                        selectionKey0.interestOps(1);
                        try {
                            this.finishConnect = socketChannel0.finishConnect();
                            //Log.d(TAG, "finishConnect!");
                            LogInfo("Connected to lvs.52tt.com!");
                            this.init_buffer();
                            continue;
                        }
                        catch(IOException unused_ex) {
                            return;
                        }
                    }

                    if(selectionKey0.isReadable()) {
                        //Log.d("BaseSocket", "key.isReadable");
                        this.doReceive(selectionKey0);
                        continue;
                    }

                    if(selectionKey0.isWritable()) {
                        //Log.d("BaseSocket", "key.isWritable");
                        continue;
                    }

                    //Log.d("BaseSocket", "key.others");
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

    private byte[] unpack(ByteBuffer cipher) {
        try {
            MessageBody messageBody = new MessageBody(cipher);
            MessageContent messageContent = messageBody.unpack_body();
            return messageContent.getBytes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void onResponse(int cmd, ByteBuffer data) {
        try {
            //cmd 6 should be notification
            if (cmd == 6) {
                return;
            }
            byte[] bodyBytes = unpack(data);
            if (bodyBytes == null) {
                LogInfo("unpack failed! cmd:" + cmd);
                return;
            }
            switch (cmd){
                case Commands.cmd_auto_login:{//Auto Login Response
                    this.on_logged_in(bodyBytes);
                    break;
                }
                case Commands.cmd_super_channel_search:{
                    this.on_super_channel_search(bodyBytes);
                    break;
                }
                case Commands.cmd_enter_channel:{//OnEnterChannel
                    this.on_enter_channel(bodyBytes);
                    break;
                }case Commands.cmd_check_sync_key:{//unknown
                    this.on_check_sync_key(bodyBytes);
                    break;
                }
                case Commands.cmd_public_chat:{
                    this.on_public_chat_reponse(bodyBytes);
                    break;
                }
                case Commands.cmd_leave_channel:{
                    this.on_leave_channel(bodyBytes);
                    break;
                }
                case Commands.cmd_follow_user:{
                    this.on_follow_user_response(bodyBytes);
                    break;
                }
                case Commands.cmd_greetings:{
                    this.on_greetings_response(bodyBytes);
                    break;
                }
                case Commands.cmd_req_game_channel_list:{
                    this.on_game_channel_list(bodyBytes);
                    break;
                }
                case Commands.cmd_req_under_mic_member_list:{
                    this.on_game_channel_member_list(bodyBytes);
                    break;
                }
                default:
                    EmptyMessage emptyMessage = EmptyMessage.parseFrom(bodyBytes);
                    Log.d(TAG, "unknown response cmd:" + cmd + ", message:" + emptyMessage.toString());
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void on_greetings_response(byte[] streamData) {
        try {
            if (this.greetsCb != null) {
                GreetingResp greetingResp = GreetingResp.parseFrom(streamData);
                this.greetsCb.callback(greetingResp);
            }
//            GreetingResp greetingResp = GreetingResp.parseFrom(streamData);
//            if (greetingResp.getBaseResp().getErrCode() == 0) {
//                LogInfo("Greetings message sent successfully! ");
//            }else{
//                LogInfo("Greetings message sent failed! errCode:" + greetingResp.getBaseResp().getErrCode() +
//                        ", errMsg:" + greetingResp.getBaseResp().getErrMsg().toStringUtf8());
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void on_follow_user_response(byte[] streamData) {
        try {
            FollowUserResp followUserResp = FollowUserResp.parseFrom(streamData);
            if (this.followCb != null) {
                this.followCb.callback(followUserResp);
            }
//            if (followUserResp.getBaseResp().getErrCode() == 0) {
//                LogInfo("follow User successfully! followed User Id:" + followUserResp.getFollowedUserId());
//            }else{
//                LogInfo("followUser failed! errCode:" + followUserResp.getBaseResp().getErrCode() +
//                        ", errMsg:" + followUserResp.getBaseResp().getErrMsg().toStringUtf8());
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void on_leave_channel(byte[] streamData) {
        try {
            LeaveChannelResponse leaveChannelResponse = LeaveChannelResponse.parseFrom(streamData);
            if (leaveChannelResponse.getBaseResp().getErrCode() == 0) {
                LogInfo("leaveChannelResponse successfully!");
            }else{
                LogInfo("leaveChannelResponse failed! errCode:" + leaveChannelResponse.getBaseResp().getErrCode() +
                        ", errMsg:" + leaveChannelResponse.getBaseResp().getErrMsg().toStringUtf8());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void on_public_chat_reponse(byte[] streamData) {
        try {
            if (this.publicChatCb != null) {
                ChatMessageResp chatMessageResp = ChatMessageResp.parseFrom(streamData);
                this.publicChatCb.callback(chatMessageResp);
            }
//            if (chatMessageResp.getBaseResp().getErrCode() == 0) {
//                LogInfo("ChatMessage message sent successfully! ");
//            }else{
//                LogInfo("ChatMessage message sent failed! errCode:" + chatMessageResp.getBaseResp().getErrCode() +
//                        ", errMsg:" + chatMessageResp.getBaseResp().getErrMsg().toStringUtf8());
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void on_check_sync_key(byte[] streamData) {
        try {
            CheckSyncKeyResp checkSyncKeyResp = CheckSyncKeyResp.parseFrom(streamData);
            if (checkSyncKeyResp.getBaseResp().getErrCode() == 0) {
                LogInfo("CheckSyncKey successfully! ");
            }else{
                LogInfo("CheckSyncKey failed! errCode:" + checkSyncKeyResp.getBaseResp().getErrCode() +
                        ", errMsg:" + checkSyncKeyResp.getBaseResp().getErrMsg().toStringUtf8());
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    private void on_enter_channel(byte[] streamData) {
        try {
            EnterChannelResponse enterChannelResponse = EnterChannelResponse.parseFrom(streamData);
            if (enterChannelResponse.getBaseResp().getErrCode() == 0) {
                LogInfo("enterChannel successfully! ");
            }else{
                LogInfo("enterChannel failed! errCode:" + enterChannelResponse.getBaseResp().getErrCode() +
                        ", errMsg:" + enterChannelResponse.getBaseResp().getErrMsg().toStringUtf8());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void on_super_channel_search(byte[] streamData) {
        try {
            ResponseSuperChannelSearch responseSuperChannelSearch = ResponseSuperChannelSearch.parseFrom(streamData);
            if (responseSuperChannelSearch.getBaseResp().getErrCode() == 0) {
                ResponseSuperChannelSearch.SearchResp.ChannelInfo channelInfo = responseSuperChannelSearch.getSearchResp().getChannelInfo();
                this.channelIds.put(channelInfo.getDisplayId(), channelInfo.getChannelId());
                setChannelId(String.valueOf(channelInfo.getChannelId()));
                LogInfo("ResponseSuperChannelSearch, DisplayId:" + channelInfo.getDisplayId() + ", ChannelId:" + channelInfo.getChannelId());
            }else{
                LogInfo("ResponseSuperChannelSearch failed! errCode:" + responseSuperChannelSearch.getBaseResp().getErrCode() +
                        ", errMsg:" + responseSuperChannelSearch.getBaseResp().getErrMsg().toStringUtf8());
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

    public void enter_channel(long channelId){
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.enter_channel(channelId, 0))
                );
            }
        });
    }



//    public void leave_channel(long displayId) {
//        if (!this.channelIds.containsKey(displayId)){
//            LogInfo("ChannelId doesn't exist in the channelIds!" + displayId);
//            return;
//        }
//        Long channelId = this.channelIds.get(displayId);
//        this.sendThread.getHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                write(
//                        ByteBuffer.wrap(ttProtocol.leave_channel(channelId))
//                );
//            }
//        });
//
//    }

    public void leave_channel(long channelId) {
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.leave_channel(channelId))
                );
            }
        });

    }

//    public void follow_user(String followUserAccount, long displayId, String tagName) {
//        String _dislayId = String.valueOf(displayId);
//        if (!this.channelIds.containsKey(Long.valueOf(_dislayId))){
//            LogInfo("ChannelId doesn't exist in the channelIds!" + _dislayId);
//            return;
//        }
//        Long channelId = this.channelIds.get(Long.valueOf(_dislayId));
//        this.sendThread.getHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                write(
//                        ByteBuffer.wrap(ttProtocol.follow_user(followUserAccount, channelId, displayId, tagName))
//                );
//            }
//        });
//
//    }

    public void follow_user(String followUserAccount, long channelId, String tagName, ICallback callback) {
        if (this.followCb == null)
            this.followCb = callback;
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.follow_user(followUserAccount, channelId, 0, tagName))
                );
            }
        });

    }

//    public void channel_chat_text(long displayId, String content) {
//        String _dislayId = String.valueOf(displayId);
//        if (!this.channelIds.containsKey(Long.valueOf(_dislayId))){
//            LogInfo("ChannelId doesn't exist in the channelIds!" + _dislayId);
//            return;
//        }
//        Long channelId = this.channelIds.get(Long.valueOf(_dislayId));
//        this.sendThread.getHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                write(
//                        ByteBuffer.wrap(ttProtocol.channel_chat_text(channelId, content))
//                );
//            }
//        });
//    }

    public void channel_chat_text(long channelId, String content) {
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.channel_chat_text(channelId, content))
                );
            }
        });
    }

    public void channel_chat_text(long channelId, String content, ICallback callback) {
        if (this.publicChatCb == null)
            this.publicChatCb = callback;
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

    public void greet(String toAccount, String content, ICallback callback) {
        if (this.greetsCb == null)
            this.greetsCb = callback;
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.greet(toAccount, content, loginKey))
                );
            }
        });
    }

    public void req_new_game_channel_list(int tagId, int getMode, int count) {
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                channels.clear();
                write(
                        ByteBuffer.wrap(ttProtocol.req_new_game_channel_list(tagId, getMode, count))
                );
            }
        });
    }

    public void req_new_game_channel_list(int tagId, int getMode, int count, ICallback callback) {
        if (this.channelListCb == null)
            this.channelListCb = callback;
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                channels.clear();
                write(
                        ByteBuffer.wrap(ttProtocol.req_new_game_channel_list(tagId, getMode, count))
                );
            }
        });
    }

    public void req_channel_under_mic_member_list(long channelId) {
        this.sendThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                write(
                        ByteBuffer.wrap(ttProtocol.req_channel_under_mic_member_list(channelId))
                );
            }
        });
    }

    public void req_channel_under_mic_member_list(long channelId, ICallback callback) {
        if (this.channelMemberListCb == null)
            this.channelMemberListCb = callback;
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
                    socketChannel = SocketChannel.open();
                    selector = Selector.open();
                    // we open this channel in non blocking mode
                    socketChannel.configureBlocking(false);
                    socketChannel.connect(new InetSocketAddress("lvs.52tt.com", port));
                    socketChannel.register(selector, SelectionKey.OP_CONNECT);
                    connect(1000);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void on_logged_in(byte[] streamData) {

        try {
            LoginResp loginResp = LoginResp.parseFrom(streamData);
            if (loginResp.getBaseResp().getErrCode() == 0){
                LoginResp.AuthInfo authInfo = loginResp.getAuthInfo();
                this.account = authInfo.getAccount().toStringUtf8();
                int uid = authInfo.getUserId();
                String sessionKey = authInfo.getSessionKey().toStringUtf8();
                long timestamp = authInfo.getTimestamp();
                String authToken = authInfo.getAuthToken().toStringUtf8();
                this.loginKey = authInfo.getLoginkey().toStringUtf8();
                LogInfo("Login successfully! uid:" + uid + ", sessionKey:" + sessionKey + ", timestamp:" + timestamp + ", authToken:" + authToken);
                //LogInfo(loginResp.toString());
                YProtocol.setUid(uid);
                YProtocol.native_setSessionKey(sessionKey);
                YProtocol.initAuthToken(uid, timestamp, authToken);
                //Sync the keys and tokens, even though I still have no idea
                //what are these items used for, but it's better than doing nothing.
                this.check_sync_keys();
    //            this.test_cmd31200();
    //            this.test_cmd30330();
    //            this.test_cmd3580();
    //            this.test_cmd401();
            }else{
                LogInfo("Login failed! errCode:" + loginResp.getBaseResp().getErrCode() +
                        ", errMsg:" + loginResp.getBaseResp().getErrMsg().toStringUtf8());
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    private void on_game_channel_list(byte[] streamData) {
        try {
            RespNewGameChannelList channelList = RespNewGameChannelList.parseFrom(streamData);
            if (this.channelListCb != null)
                this.channelListCb.callback(channelList);
//            if (channelList.getBaseResp().getErrCode() == 0) {
//                LogInfo("RespNewGameChannelList successfully! ");
//                //LogInfo(channelList.toString());
//                if (channelList.getChannelListCount() > 0) {
//                    for (RespNewGameChannelList.ChannelList channel : channelList.getChannelListList()){
//                        LogInfo("ChannelId:" + channel.getChannelId() + ", Name:" + channel.getChannelName().toStringUtf8());
//                        this.channels.put(channel.getChannelId(), channel);
//                    }
//                }
//            }else{
//                LogInfo("RespNewGameChannelList failed! errCode:" + channelList.getBaseResp().getErrCode() +
//                        ", errMsg:" + channelList.getBaseResp().getErrMsg().toStringUtf8());
//            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    private void on_game_channel_member_list(byte[] streamData) {
        try {
            ResponseChannelUnderMicMemberList memberList = ResponseChannelUnderMicMemberList.parseFrom(streamData);
            if (this.channelMemberListCb != null)
                this.channelMemberListCb.callback(memberList);
//            if (memberList.getBaseResp().getErrCode() == 0) {
//                LogInfo("ResponseChannelUnderMicMemberList successfully!");
//                //LogInfo(memberList.toString());
//                LogInfo("ChannelId:" + memberList.getChannelId() + ", MemberCount:" + memberList.getCurrMemberTotal());
//                if (memberList.getCurrMemberTotal() > 0) {
//                    for (ResponseChannelUnderMicMemberList.ChannelMemberInfo memberInfo : memberList.getChannelMemberInfoList()) {
//                        LogInfo("   channelMember:" + memberInfo.getNickName().toStringUtf8() + ", Account:" + memberInfo.getAccount().toStringUtf8());
//                    }
//                }
//            }else{
//                LogInfo("ResponseChannelUnderMicMemberList failed! errCode:" + memberList.getBaseResp().getErrCode() +
//                        ", errMsg:" + memberList.getBaseResp().getErrMsg().toStringUtf8());
//            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    public void init(Application application, String acc, String uid, String acc_type, String pwd, String deviceID,
                     String deviceIDV2, String androidid, String key_web_ua, String deviceModel){
        this.ttProtocol = new TTProtocol(acc, uid, acc_type,
                pwd, deviceID,
                deviceIDV2, androidid,
                key_web_ua,
                deviceModel);
        this.ttProtocol.init(application);
    }
}
