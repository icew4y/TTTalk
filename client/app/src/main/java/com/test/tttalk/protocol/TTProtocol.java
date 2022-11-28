package com.test.tttalk.protocol;

import android.app.Application;
import android.util.Log;

import com.google.protobuf.ByteString;
import com.protobuf.protos.AutoLogin;
import com.protobuf.protos.ChatMessage;
import com.protobuf.protos.CheckSyncKey;
import com.protobuf.protos.DeviceInfo;
import com.protobuf.protos.EnterChannelRequest;
import com.protobuf.protos.FollowUser;
import com.protobuf.protos.Greeting;
import com.protobuf.protos.LeaveChannelRequest;
import com.protobuf.protos.ReqNewGameChannelList;
import com.protobuf.protos.RequestChannelUnderMicMemberList;
import com.protobuf.protos.RequestSuperChannelSearch;
import com.test.tttalk.ByteHexStr;
import com.test.tttalk.Commands;
import com.test.tttalk.MainActivity;
import com.yiyou.ga.net.protocol.PByteArray;
import com.yiyou.ga.net.protocol.YProtocol;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TTProtocol {
    private String acc, uid, acc_type, pwd, deviceID, deviceIDV2, androidid, key_web_ua;
    private int seq;
    private String deviceModel;


    public TTProtocol(String acc, String uid, String acc_type, String pwd, String deviceID, String deviceIDV2, String androidid, String key_web_ua, String deviceModel) {
        this.acc = acc;
        this.uid = uid;
        this.acc_type = acc_type;
        this.pwd = pwd;
        this.deviceID = deviceID;
        this.deviceIDV2 = deviceIDV2;
        this.androidid = androidid;
        this.key_web_ua = key_web_ua;
        this.deviceModel = deviceModel;
    }
    public void init(Application application){
        // I have patched libprotocol.so in order to make this call possible
        // detect_emulator is called inside native_init, just simply return 0
        // or 'nop' it by using IDAPro

        // in the version 6.10 of TT Talk, I patch the return value of detect_emulator
        // MOV W0, #0
        // #0 means no emulator have found
        boolean ret = YProtocol.native_init(application, true);
        System.out.println("native_init:" + ret);


        //DeviceId can be found in "shared_prefs/preference_new_deivce_id.xml"
        ret = YProtocol.native_setDeviceId(this.deviceIDV2);
        ret = YProtocol.native_setClientVersion(101318657);

        // make sure the app has the permissions, it won't get the correct device id otherwise
        // it won't be able to login or do something else
        String deviceId = YProtocol.native_TT_e();
        //System.out.println("native_TT_e(deviceId):" + deviceId);

        // I guess this is a key that is used to encrypt the session
        // sessionKey can be found in "shared_prefs/auth.xml"
        // also userName
        YProtocol.native_setSessionKey(this.pwd);
        //YProtocol.setUid(290440381);
    }


    private byte[] pack_header(int cmd, int seq, short unknown, byte[] byteArray) {
        int msglen = byteArray.length;
        ByteArrayOutputStream byteArrayOutputStream0 = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream0 = new DataOutputStream(byteArrayOutputStream0);
        try {
            dataOutputStream0.writeInt(msglen + 20);
            dataOutputStream0.writeShort(20);
            dataOutputStream0.writeShort(2);
            dataOutputStream0.writeInt(cmd);
            dataOutputStream0.writeInt(seq);
            dataOutputStream0.writeShort(unknown);
            dataOutputStream0.writeShort(0);
            if (byteArray != null) {
                dataOutputStream0.write(byteArray);
            }

            dataOutputStream0.close();
            Log.d("Packer", "bodyLen = ".concat(String.valueOf(msglen)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] arr_b = byteArrayOutputStream0.toByteArray();
        try {
            byteArrayOutputStream0.close();
            return arr_b;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] pack_body(int cmd, byte[] data) {
        seq ++;
        String t = ByteHexStr.bytetoHexString_(data);
        PByteArray outByteArray = new PByteArray();
        YProtocol.pack(cmd, data, outByteArray, true, 0);
        return pack_header(cmd, seq, (short) 0, outByteArray.value);
    }

    private byte[] pack_body2(int cmd, byte[] data) {
        seq ++;
        String t = ByteHexStr.bytetoHexString_(data);
        PByteArray outByteArray = new PByteArray();
        YProtocol.pack(cmd, data, outByteArray, false, 0);
        return pack_header(cmd, seq, (short) 0, outByteArray.value);
    }

    public byte[] auto_login() {
        //byte[] data = ByteHexStr.hexToByteArray("0a091a002a038201003200120b31333538303539303632301a203535356636313434373339643632383934646566663336623064306236326563200328015a0231306207416e64726f69646a12414f5350206f6e2063726f737368617463689a01086f6666696369616caa01086f6666696369616cb201e1010a0012006ada0122c5014d6f7a696c6c612f352e3020284c696e75783b20416e64726f69642031303b20414f5350206f6e2063726f73736861746368204275696c642f515031412e3139303731312e3032303b20777629204170706c655765624b69742f3533372e333620284b48544d4c2c206c696b65204765636b6f292056657273696f6e2f342e30204368726f6d652f37342e302e333732392e313836204d6f62696c65205361666172692f3533372e333620545456657273696f6e2f362e31302e3120545446726f6d2f7474321066626464633061363461313962303937");
        AutoLogin.Builder autoLoginBuilder = AutoLogin.newBuilder();
        autoLoginBuilder
                .setBaseReq(
                        AutoLogin.BaseReq.newBuilder()
                                .setUobj3(AutoLogin.BaseReq.Unkobj3.newBuilder().build())
                                .setUobj5(AutoLogin.BaseReq.Unkobj5.newBuilder()
                                        .setUobj16(AutoLogin.BaseReq.Unkobj5.Unkobj16.newBuilder().build())
                                        .build())
                                .setUobj6(AutoLogin.BaseReq.Unkobj6.newBuilder().build()))
                .setUserName(ByteString.copyFromUtf8(this.acc))
                .setSessionKey(ByteString.copyFromUtf8(this.pwd))
                .setLoginType(3)
                .setAccType(1)
                .setSystemVer(ByteString.copyFromUtf8("10"))
                .setSystem(ByteString.copyFromUtf8("Android"))
                .setModel(ByteString.copyFromUtf8(this.deviceModel))
                .setItChannel(ByteString.copyFromUtf8("official"))
                .setDisChn(ByteString.copyFromUtf8("official"))
                .setExtraInfo(
                        AutoLogin.ExtraInfo.newBuilder()
                                .setUnkobj1(AutoLogin.ExtraInfo.Unkobj1.newBuilder().build())
                                .setLotteryInfo(AutoLogin.ExtraInfo.LotteryInfo.newBuilder().build())
                                .setExtraDeviceIds(
                                        AutoLogin.ExtraInfo.ExtraDeviceIds.newBuilder()
                                                .setUserAgent(ByteString.copyFromUtf8(this.key_web_ua))
                                                .setAndroidId(ByteString.copyFromUtf8(this.androidid))
                                )
                );

        AutoLogin autoLogin = autoLoginBuilder.build();
        return pack_body(Commands.cmd_auto_login, autoLogin.toByteArray());
    }

    public byte[] search_channel(String keyword){
        seq++;
        RequestSuperChannelSearch searchRequest = RequestSuperChannelSearch.newBuilder()
                .setUnkobj1(RequestSuperChannelSearch.UnknownObj1.newBuilder().build())
                .setKeyWord(ByteString.copyFrom(keyword.getBytes(StandardCharsets.UTF_8)))
                .setUnknown3(1)
                .setSearchType(100).build();
        return pack_body2(Commands.cmd_super_channel_search, searchRequest.toByteArray());
    }

    public byte[] enter_channel(long channelId, long displayId){
        seq++;
        EnterChannelRequest enterChannelRequest = EnterChannelRequest.newBuilder().setBaseReq(EnterChannelRequest.BaseReq.newBuilder())
                .setRoomId(channelId)
                .setUb3(EnterChannelRequest.unknown_obj3.newBuilder().setUnknownInt1(3))
                .setDisplayRoomId(displayId)
                .setUnknownInt7(12)
                .build();
        return pack_body2(Commands.cmd_enter_channel, enterChannelRequest.toByteArray());
    }

    public byte[] leave_channel(long channelId) {
        LeaveChannelRequest.Builder leaveBuilder = LeaveChannelRequest.newBuilder()
                .setBaseReq(LeaveChannelRequest.BaseReq.newBuilder().build())
                .setChannelId(channelId);

        LeaveChannelRequest leaveChannelRequest = leaveBuilder.build();
        return pack_body2(Commands.cmd_leave_channel, leaveChannelRequest.toByteArray());
    }

    public byte[] follow_user(String followUserAccount, long channelId, long displayID, String tagName) {
        int channelType = 3;
        FollowUser.Builder followUserBuilder = null;
        try {
            JSONObject customSource = new JSONObject();
            customSource.put("tagname", tagName); // e.g. 王者荣耀
            customSource.put("channel_theme", "");
            customSource.put("channel_prefecture", "0");
            followUserBuilder = FollowUser.newBuilder()
                    .setUobj1(FollowUser.Unkobj1.newBuilder().build())
                    .setUserIdentifier(
                            FollowUser.UserIdentifier.newBuilder()
                                    .setAccount(ByteString.copyFromUtf8(followUserAccount))
                                    .build()
                    )
                    .setSource(4)
                    .setCustomSource(ByteString.copyFromUtf8(customSource.toString()))
                    .addDatacenterContextInfo(
                            FollowUser.DatacenterContextInfo.newBuilder()
                                    .setKey(ByteString.copyFromUtf8("displayID"))
                                    .setValue(ByteString.copyFromUtf8(String.valueOf(displayID)))
                                    .build()
                    )
                    .addDatacenterContextInfo(
                            FollowUser.DatacenterContextInfo.newBuilder()
                                    .setKey(ByteString.copyFromUtf8("channelID"))
                                    .setValue(ByteString.copyFromUtf8(String.valueOf(channelId)))
                                    .build()
                    )
                    .setChannelId(channelId)
                    .setChannelType(channelType);
            FollowUser followUser = followUserBuilder.build();
            return pack_body2(Commands.cmd_follow_user, followUser.toByteArray());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] channel_chat_text(long channelId, String content) {

        DeviceInfo.Builder builder = DeviceInfo.newBuilder().setDevicesm(
                DeviceInfo.DeviceShumei.newBuilder().setDeviceId(
                        DeviceInfo.DeviceShumei.DeviceId.newBuilder().setDeviceIdStr(ByteString.copyFrom(
                                this.deviceID.getBytes(StandardCharsets.UTF_8)
                        ))
                )
        );
        ChatMessage.Builder chatMessageBuilder = ChatMessage.newBuilder()
                .setDeviceInfo(builder.build())
                .setRoomRealId(channelId)
                .setTextMessage(ByteString.copyFrom(content.getBytes(StandardCharsets.UTF_8)))
                .setUnknown4(1)
                .setUnknown5(1);
        ChatMessage chatMessage = chatMessageBuilder.build();
        return pack_body2(Commands.cmd_public_chat, chatMessage.toByteArray());
    }

    public byte[] greet(String toAccount, String content, String loginKey) {
        DeviceInfo.Builder deviceBuilder = DeviceInfo.newBuilder().setDevicesm(
                DeviceInfo.DeviceShumei.newBuilder().setDeviceId(
                        DeviceInfo.DeviceShumei.DeviceId.newBuilder().setDeviceIdStr(ByteString.copyFrom(
                                this.deviceID.getBytes(StandardCharsets.UTF_8)
                        ))
                )
        );
        Greeting.Builder greetingBuilder = Greeting.newBuilder()
                .setDeviceInfo(deviceBuilder)
                .setToAccount(ByteString.copyFromUtf8(toAccount))
                .setMessageType(1)
                .setContent(ByteString.copyFromUtf8(content))
                .setCi(3)
                .setClientTime(System.currentTimeMillis() / 1000)
                .setUnknown8(0)
                .setLoginKey(ByteString.copyFromUtf8(loginKey))
                .setUnknown10(1)
                .setUnkonwn12(8);
        Greeting greeting = greetingBuilder.build();
        return pack_body2(Commands.cmd_greetings, greeting.toByteArray());
    }

    public byte[] check_sync_keys() {
        CheckSyncKey.Builder syncKeyBuilder = CheckSyncKey.newBuilder()
                .setUnkobj1(CheckSyncKey.Unknown1.newBuilder().build())
                .setUnknown4(1)
                .setVersion(ByteString.copyFromUtf8("6.10.1"))
                .setVersionCode(16243)
                .setOfficial(ByteString.copyFromUtf8("official"));

        CheckSyncKey checkSyncKey = syncKeyBuilder.build();
        return pack_body2(Commands.cmd_check_sync_key, checkSyncKey.toByteArray());
    }

    public byte[] req_new_game_channel_list(int tagId, int getMode, int count){
        ReqNewGameChannelList reqNewGameChannelList = ReqNewGameChannelList.newBuilder()
                .setBaseReq(ReqNewGameChannelList.BaseReq.newBuilder())
                .setCount(count)
                .setTabId(tagId)
                .setGetMode(getMode)
                .setChannelPackageId(ByteString.copyFromUtf8("official"))
                .build();
        return pack_body2(Commands.cmd_req_game_channel_list, reqNewGameChannelList.toByteArray());
    }

    public byte[] req_channel_under_mic_member_list(long channelId){
        RequestChannelUnderMicMemberList requestChannelUnderMicMemberList = RequestChannelUnderMicMemberList.newBuilder()
                .setBaseReq(RequestChannelUnderMicMemberList.BaseReq.newBuilder())
                .setChannelId(channelId)
                .build();

        return pack_body2(Commands.cmd_req_under_mic_member_list, requestChannelUnderMicMemberList.toByteArray());
    }
}
