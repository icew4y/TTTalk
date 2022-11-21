package com.test.tttalk;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.protobuf.ByteString;
import com.protobuf.protos.ChatMessage;
import com.protobuf.protos.ChatMessageOrBuilder;
import com.protobuf.protos.EnterChannelRequest;
import com.protobuf.protos.requestSuperChannelSearch;
import com.test.tttalk.databinding.ActivityMainBinding;
import com.yiyou.ga.net.protocol.PByteArray;
import com.yiyou.ga.net.protocol.YProtocol;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'tttalk' library on application startup.
    static {
        System.loadLibrary("c++_shared");
        System.loadLibrary("marsxlog");
        System.loadLibrary("tttalk");
    }

    public TTSocketChannel ttSocketChannel = new TTSocketChannel();

    public String DeviceId = "";

    public static byte[] pack_header(int cmd, int seq, short unknown, byte[] byteArray) {
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
            if(byteArray != null) {
                dataOutputStream0.write(byteArray);
            }

            dataOutputStream0.close();
            Log.d("Packer", "bodyLen = ".concat(String.valueOf(msglen)));
        }
        catch(IOException e) {
            e.printStackTrace();
        }

        byte[] arr_b = byteArrayOutputStream0.toByteArray();
        try {
            byteArrayOutputStream0.close();
            return arr_b;
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    private ActivityMainBinding binding;

    private static final String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void requestPermissionAtFirst() throws JSONException {
        requestPermissions(permissions, 1);

    }

    public String getDeviceId() {
        //http://fp-it.fengkongcloud.com/deviceprofile/v4

        //Content-Type: application/octet-stream
        //Content-Length: 4067
        //User-Agent: Dalvik/2.1.0 (Linux; U; Android 10; AOSP on crosshatch Build/QP1A.190711.020)
        //Host: fp-it.fengkongcloud.com
        //Accept-Encoding: gzip
        //Connection: close
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("Content-Type", "application/octet-stream");
            headers.put("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 10; AOSP on crosshatch Build/QP1A.190711.020)");
            headers.put("Accept-Encoding", "gzip");
            headers.put("Connection", "close");

            String data = "{\"organization\":\"gwQtVOYOBNLLqhbQmcV8\",\"os\":\"android\",\"appId\":\"default\",\"encode\":2,\"compress\":3,\"data\":\"EtY7sxZDcXbuWDKZuhMkvr0DLZ9JXU8Ah6\\/Ub3WxutdXGioVgfRf+RFFbpSmC\\/d6XNJvDdfi9OmO43VydywjBdpLNPYzZCe1ohqaikXrMVlf+6YjcOc31z80eNGuv4vTFPJQ6S0L0TEK28sIJiw\\/8KlPfJcbYvBpmhlgeiakVkwLVjcTPEO\\/nJOkWRhwcgKH4MCyRLDuWpg0Co9z9Hv\\/PG\\/QrVHCb7JkFXORo0uUZVqMbP5Y11fDaiXRePFJbAi2\\/swrqfujMPkEewUm3nuQVmfS8De7J6ExGxg7e\\/PJvr+6QQ65Exw++WWDrkMRYMyV\\/AznalG5Jmx8NU79\\/Hgp3OlbfJH3\\/3Z33gERYRdEkxN+GuoIcu5snezWKtwy68Ao9NZNn4uYHz8nz0nvB6CoWZFf3KoDSfwwsvAMhB8mpuB8r7xCLaHQ47ru9Vv2rg1aeW2HWH28Yg1yVh15XbLPYx3P84RUJlr8QMk7oCTE8R6qgdP4H7blbmhHHxzktpAAo4x2UQEe53SvTxemqQLhS8tB83BM\\/skktEJUAc2HqG0mUhELhuJaoXj5NVrojYlf3jcnMof2cnQItOLXe+PpM7SfqQWzLjf3MhPtefaN77amFH\\/c\\/8A\\/SRI6NrvbFRsWZ3kcP2O7nE4LfKxE44zNSky8WFecC4O7U4AJzGMFastX3D74llh7vWpohMvcsdolM8vTgBncM2rqF12rj2nz2461l79Df4dPY3u6vP3TObelLEvQbR2E7M3guQyWhqWh6c22H1RXEPNAlEWfdt\\/xXhs+Dx0Omn0XZStFuLFH8c5BYTSz+7+h7z5nBm2wAB+GZOT3WcIQNQe+NS6QwaHx49mCb3o+cklWpG4bEarg7mESZJsNqgK++cjaCvnsR3pOuGCQ2BeIj\\/H6I5KdEmi6k4LUPlrYCSkLbr34Dsbp2k6ff4uiY4XeUmEHfzWWAmu7V7kEO0mlxfjAGY8yk6ELsVcOkwteKcHV393RTgxEcM3sOvT6rNHx7YSyXfL\\/PAriKrkhZaABSytewZyiauqTwnpLwzLr9e+DK4L0tRN\\/Dsjz\\/aKWaIVlk8iAsfCJPqUIDhpF+QPZtPuPnOtajUOLvjoS9ONWyyhEW0nqc4JxGXXbMrfl67TncE2sm+aIOXsMVQZPaNUL5MzioaCXNSctqd\\/ms+0kLZMrajHZgowD6fHb+HnNNluZZrw+2iLbXMUXV4PKGqDKEd9ncbtl5ep5IsWdxTy7VuhhlVxnhN3ftgzLhYqlypMmOujbIffaZE\\/gyNzVo1MZj\\/glq0OqAfCLC9p4\\/HoNwX6qb2zvHEyw+0wsgjMbyABnFg\\/jR1frrNpZ+c5wcAGY8UpxSXzqSLMgC23Sdsz6feR4xZcHA1dCWCK4MASSmmS0OVgKwDLguOZIWOgzzW4cHrTeizs7wj\\/ja1VFA2lEaD\\/zLMJSz+yRFXNYKugOVCH0zT61ZCBkJUdXEOKWqruPVMP2gN+glse5nVOx15U5R9jXXsHTdEo1WHXULvkxJFQ6OoW7QMTq4WuaihMTOzEjIIFc1P5CMqTunyvIH0kKgtsg3JsBzTBFQvWEHcUnaH1wNK3o6u9xxDswlXAO8pfwiHEGprdK0qE4l9EZdO7IbVtGJ9HO2HvlmzXzlcU+3sMmpSkCCMK5hthin0iQT4U8WyiXkugJXD5EWMgX8\\/zZd1DxCW3J3JwUjMDGII61RhAPPuBaE+X+pLTXAHqnFMhqZyUEtgbcBBF1pNKacgSQz8xX7O85z2aFmW+Qk+JJsw30J6Ur7Y\\/jQAbzZiBAdo+95kjiQ4qPcCBIgOmAypQcsg52eWx9ra0A5iyjmzyxGSJDLUbG2hQ6xqykxypmnQBziYzUz\\/SfNF2BsDERsk1ZVnw2HReJAV12oQsZz65xbrSI8mSvDQN2Zah4wk2gK5CFp3druMuhRplmDxGfCRPKZYZhYHaTBr1jOmYKTtpKfwEHtg7nTXgHbA6tPjwxE5H2KAaj\\/gCwJlSwDylyoArMi0wXtmjNK\\/J8k6eakmMSqLjZAQRwUnrlrokoGUNd++s\\/PxMggiRbucmH89AcsDBjfvuOb8NNh0Lheo9JyM5WIBMi6gk7FWeMcg7TAy0vMR1s3\\/UsIiSK0B3cENBTWmqFNgTLOFZ7taNXyIEP6vlP1fEK5FFR5+b1Xh5AJq1RtHobLPz8m1apAntk9ELf7uHDlk6fJ+qFCEM7pO+l1Epn4wgDpZbZnDKtjXg9AEF+4FmiLtijtyRjP18C6deK4ZQKKcjc\\/HN5ahPXPbk\\/fr2YT9D5TnPq7assCRIAtB6Iv2qNY3p85DayEvSAX8xdYsDX2B4zCINg8u1znUOHVQi6gpMgMHl5efpdk3GFAi3bir\\/YhCsbdUI5+36d6U8sI8m3PqNji8tvMKOIw1HZFeaFNQnJEU5W7JqRBAYoQqrIjhwYZjZNXRcPN7uESSTPKdbxHSoFNDaf0B\\/is9Tu3yNSel7LFYnJlVaMxqhoDloj26MF9SZZh5+oXFc4T1eVIgr3fT4lQDLWuScZwxizACPOJv0AQfbSIl55d0HlxcrTeexRylP+a6GdFqHEqH4nxca5ooO+4DkJqo4NVApSCf\\/645\\/8ilP1uVf+Swt+doQBrLI6QZuvA+XrGo7Asg4VhtxLDGYPQGGORFimhBexDpncM9EXwZiVZmNG2eLJer5snaWdTXTJXx6CVlCRaWd6t9I+kXr+OXRJNFd4VhwIV+3Jw+Gga8DdNEXzKqRvYCZwAfGBDcSLyQ1UwSUqvti96nsKepM7L2FaWhXuHaJZeHlWAYMJkCzck6xuv2JH\\/OP\\/8XxBaue\\/KKQNVONbx55ZlWUoJGMT6rXMqWs65uR976WSrZiWKXGMXk0bgIzfj8sriqJNEj9lNN6VCJi7Z7sGSJpIfiP5rz5Z6G7h490mfMkxQdvpBWLDoIh9fI2afIMz0Z34P9LTsqN\\/N5R8agF0kl7JifhTkS504hn6jBSUKKQ8TRP69hZso4qzu1W1zkxELno0g1Rdmp7d0ukh9HBGofdo+49rf2YjySgIRQsfON3X+WTd\\/NkYhskwtLygrAhYjULh1NWXaOeK+TKBRxZLYBy90IyyBj6NDz03F2W\\/13nkIDyjdFrEv1XKw5BR90oauu7Cdlo7XRGvNkcC+MdLk04KXTvaB2iE+N6hDb9htPuHBupl77GSLePDkRgk\",\"tn\":\"WYU12yv7i8pCDn\\/sc6kF+e6yg6N0W09y2ZPh6FBAyeysmnbxDiKY8OWbmkW0YHj1LdPmOwEbsRCeuqj0MjIPpiC6Pb009v8dwQfr10TsaeaRPPikskWl30Mt7yvqmf2uJ3Wa9XTiWc05IeIiolwMwiCkXTXIIEjuu+CMVQqov\\/jlTBTbx\\/\\/KRSiBwiID+aEerJbKWuyX4PuGi6Cx0FRmkSffWBMpXHTS9y3LdiVJX2Itc8\\/2edXyS2G3cjuW5HudC6sa68G6sZm+p+8CQDwETvLBF4ONTlA7hJKppyRbdvrzAaTLaZGuQsX428omGaL0b5WZH9BUKIxj2VT8mclsBw==\",\"ep\":\"N+xHvTAmY0nvX6hHg+dJpGRnp9\\/8o+fQQkSyC812eMP9xnTMH+AEMJphFedzB4kLghhSpdHnETXkcp4s++uJP\\/pG+CwFnOBYvcoKMkTSf4mnjOnBeQMsLY58w4hrOTUsU7n1bQzAh5w0xRdtL+OSvVuiPpJZuJ6uFgaY\\/r0yjoCDe\\/9YMBLlSpXKLM0Pw34rE41UvTsadV5ffBaN6Ffk7vVaUH6geR4SIFMIYtnwsrP8Owf\\/ijFhD76dcC2Q77rafs0jD55dWfCSA07TMYy+2VYaBMk+j1dhIqQZCMeRuk\\/2N91RwqAS48vKjlzlE63qxCNWXSDZm4C0N4spDxXF1Q==\"}";

            Response response = OkhttpUtil.post("http://fp-it.fengkongcloud.com/deviceprofile/v4", headers, data.getBytes(StandardCharsets.UTF_8),"application/octet-stream");
            if (response.isSuccessful()) {
                String resp = response.body().string();
                //{"code":1100,"detail":{"deviceId":"emXt5Ee+44aoH+Ics/BzYturzX11OlIxowJXuraAvtuQIZjrOD5QPBVEN7dIEfZT/OBgtrgznSY70rkKgFh77g=="},"requestId":"d932e03f3f978d6ce164f0918b087e4c"}
                JSONObject jsonObject = new JSONObject(resp);{
                    if (jsonObject.getInt("code") == 1100) {
                        return jsonObject.getJSONObject("detail").getString("deviceId");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        YProtocol.sContextHolder = getApplication();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                requestPermissionAtFirst();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Example of a call to a native method

        // I have patched libprotocol.so in order to make this call possible
        // detect_emulator is called inside native_init, just simply return 0
        // or 'nop' it by using IDAPro

        // in the version 6.10 of TT Talk, I patch the return value of detect_emulator
        // MOV W0, #0
        // #0 means no emulator have found
        boolean ret = YProtocol.native_init(getApplication(), true);
        System.out.println("native_init:" + ret);

        ret = YProtocol.native_setDeviceId("17b6d6731ed4f34acc0d0d8cdc202b88");
        ret = YProtocol.native_setClientVersion(101318657);
                // make sure the app has the permissions, it won't get the correct device id otherwise
        // it won't be able to login or do something else
        String deviceId = YProtocol.native_TT_e();
        System.out.println("native_TT_e(deviceId):" + deviceId);

        // I guess this is a key that is used to encrypt the session
        YProtocol.native_setSessionKey("555f6144739d62894deff36b0d0b62ec");
        //YProtocol.setUid(290440381);

        AtomicBoolean isDone = new AtomicBoolean();
        new Thread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.DeviceId = MainActivity.this.getDeviceId();
                isDone.set(true);
            }
        }).start();

        while (!isDone.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Network operations must not be running in the main thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                ttSocketChannel.main();
            }
        }).start();

        Button btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Auto login package
                        byte[] data = ByteHexStr.hexToByteArray("0a091a002a038201003200120b31333538303539303632301a203535356636313434373339643632383934646566663336623064306236326563200328015a0231306207416e64726f69646a12414f5350206f6e2063726f737368617463689a01086f6666696369616caa01086f6666696369616cb201e1010a0012006ada0122c5014d6f7a696c6c612f352e3020284c696e75783b20416e64726f69642031303b20414f5350206f6e2063726f73736861746368204275696c642f515031412e3139303731312e3032303b20777629204170706c655765624b69742f3533372e333620284b48544d4c2c206c696b65204765636b6f292056657273696f6e2f342e30204368726f6d652f37342e302e333732392e313836204d6f62696c65205361666172692f3533372e333620545456657273696f6e2f362e31302e3120545446726f6d2f7474321066626464633061363461313962303937");
                        PByteArray outByteArray = new PByteArray();
                        boolean ret = YProtocol.pack(10, data, outByteArray, true, 0);
                        String packed = ByteHexStr.bytetoHexString_(outByteArray.value);
                        System.out.println(packed);
                        byte[] pack_header_bytes = MainActivity.pack_header(10, 2, (short) 0, outByteArray.value);
                        String packed2 = ByteHexStr.bytetoHexString_(pack_header_bytes);
                        System.out.println(packed2);
                        ByteBuffer byteBuffer = ByteBuffer.wrap(pack_header_bytes);
                        ttSocketChannel.write(byteBuffer);
                    }
                }).start();

            }
        });


        Button btnEnterChannel = (Button) findViewById(R.id.btnEnterChannel);
        btnEnterChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

//                        requestSuperChannelSearch searchRequest = requestSuperChannelSearch.newBuilder()
//                                .setUb1(requestSuperChannelSearch.unknown_obj1.newBuilder().build())
//                                .setKeyWord(ByteString.copyFrom("158887230".getBytes(StandardCharsets.UTF_8)))
//                                .setUnknown3(1)
//                                .setUnknown4(100).build();

                        EnterChannelRequest enterChannelRequest = EnterChannelRequest.newBuilder().setUb1(EnterChannelRequest.unknown_obj1.newBuilder())
                                .setRoomId(164351337)
                                .setUb3(EnterChannelRequest.unknown_obj3.newBuilder().setUnknownInt1(3))
                                .setDisplayRoomId(158887230)
                                .setUnknownInt7(12)
                                .build();
                        String t = ByteHexStr.bytetoHexString_(enterChannelRequest.toByteArray());
                        PByteArray outByteArray = new PByteArray();
                        boolean ret = YProtocol.pack(423, enterChannelRequest.toByteArray(), outByteArray, false, 0);
                        String packed = ByteHexStr.bytetoHexString_(outByteArray.value);
                        System.out.println(packed);
                        byte[] pack_header_bytes = MainActivity.pack_header(423, 4, (short) 0, outByteArray.value);
                        String packed2 = ByteHexStr.bytetoHexString_(pack_header_bytes);
                        System.out.println(packed2);
                        ByteBuffer byteBuffer = ByteBuffer.wrap(pack_header_bytes);
                        ttSocketChannel.write(byteBuffer);
                    }
                }).start();

            }
        });

        Button btnLeaveChannel = (Button) findViewById(R.id.btnLeaveRoom);
        btnLeaveChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ttSocketChannel.leaveRoom(0);
                    }
                }).start();

            }
        });


        Button btnChatText = (Button) findViewById(R.id.btnChatText);
        btnChatText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ChatMessage.DeviceInfo.Builder builder = ChatMessage.DeviceInfo.newBuilder().setDevicesm(
                                ChatMessage.DeviceInfo.DeviceShumei.newBuilder().setDeviceId(
                                        ChatMessage.DeviceInfo.DeviceShumei.DeviceId.newBuilder().setDeviceIdStr(ByteString.copyFrom(
                                                MainActivity.this.DeviceId.getBytes(StandardCharsets.UTF_8)
                                        ))
                                )
                        );
                        ChatMessage.Builder chatMessageBuilder = ChatMessage.newBuilder()
                                .setDeviceInfo(builder.build())
                                .setRoomRealId(164351337)
                                .setTextMessage(ByteString.copyFrom("你好".getBytes(StandardCharsets.UTF_8)))
                                .setUnknown4(1)
                                .setUnknown5(1);
                        ChatMessage chatMessage = chatMessageBuilder.build();
                        String t = ByteHexStr.bytetoHexString_(chatMessage.toByteArray());
                        ttSocketChannel.chat_text(chatMessage.toByteArray());
                    }
                }).start();

            }
        });




    }

    /**
     * A native method that is implemented by the 'tttalk' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}