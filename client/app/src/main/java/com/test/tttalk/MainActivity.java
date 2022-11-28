package com.test.tttalk;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.protobuf.InvalidProtocolBufferException;
import com.protobuf.protos.ReqNewGameChannelList;
import com.protobuf.protos.RespNewGameChannelList;
import com.test.tttalk.databinding.ActivityMainBinding;
import com.test.tttalk.protocol.TTProtocol;
import com.yiyou.ga.net.protocol.YProtocol;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import okhttp3.Response;
import com.protobuf.protos.GameChannelRequestService;
public class MainActivity extends AppCompatActivity {

    // Used to load the 'tttalk' library on application startup.
    static {
        System.loadLibrary("c++_shared");
        System.loadLibrary("marsxlog");
        System.loadLibrary("tttalk");
    }
    private static MainActivity mainInstance = null;

    //store in shared_prefs/pref_outside_share_info.xml
    public String key_web_ua = "Mozilla/5.0 (Linux; Android 10; AOSP on crosshatch Build/QP1A.190711.020; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/74.0.3729.186 Mobile Safari/537.36 TTVersion/6.10.1 TTFrom/tt";
    //shared_prefs/BUGLY_COMMON_VALUES.xml
    //android have no effect to login action, no matter what it's given
    //even by a random string, Auto login still works
    public String androidid = "fbddc0a64a19b097";
    private static String TAG = "TTTalk";
    private TTThread ttThread;

    private Handler messageHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case 1:{
                    //Toast.makeText(getApplication(), (String)msg.obj, Toast.LENGTH_SHORT).show();
                    TextView logEdt = (TextView) findViewById(R.id.edtLog);
                    String logstr = logEdt.getText().toString();
                    logstr += (String) msg.obj;
                    logEdt.setText(logstr + "\n");
                    break;
                }
                case 2:{
                    EditText editChannelId = (EditText) findViewById(R.id.edtResponseChannelId);
                    editChannelId.setText((String)msg.obj);
                    break;
                }
                default:break;
            }
            return false;
        }
    });

    public static MainActivity getMainInstance() {
        return mainInstance;
    }
    public void sendMsg(Message msg) {
        this.messageHandler.sendMessage(msg);
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

            Response response = OkhttpUtil.post("http://fp-it.fengkongcloud.com/deviceprofile/v4", headers, data.getBytes(StandardCharsets.UTF_8), "application/octet-stream");
            if (response.isSuccessful()) {
                String resp = response.body().string();
                //{"code":1100,"detail":{"deviceId":"emXt5Ee+44aoH+Ics/BzYturzX11OlIxowJXuraAvtuQIZjrOD5QPBVEN7dIEfZT/OBgtrgznSY70rkKgFh77g=="},"requestId":"d932e03f3f978d6ce164f0918b087e4c"}
                JSONObject jsonObject = new JSONObject(resp);
                {
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
        mainInstance = this;
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                requestPermissionAtFirst();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
//        AtomicBoolean isDone = new AtomicBoolean();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                MainActivity.this.DeviceId = MainActivity.this.getDeviceId();
//                isDone.set(true);
//            }
//        }).start();
//
//        while (!isDone.get()) {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }


        //----ishumei return:{"code":1100,"detail":{"deviceId":"B6nWouzehOiCoX9epMn5W0ovRA3W7PcYE\/aOcs0R9ggf9COWLNa7+t8tQL4do5gJzI3U0\/94bJjd9w1Ynlii8w=="},"requestId":"06b184412d1545fe5e713a64afc2b693"}
        //----before getPreSharedXML:73c8e35220bc684412f1bd86b04785ca.xml
        //MainActivity.this.DeviceId = "F0QbUvikXMCkKayVrQ3DtAgxB8pnIrY7/NrKaUXvw+7RH54uBLofmz0/Ft5wz35O7zctESDu15aXoRvKPFx5CQ==";

        String ck = "{\"acc\": \"13580590620\", \"uid\": \"296475024\", \"acc_type\": \"1\", \"pwd\": \"555f6144739d62894deff36b0d0b62ec\", \"deviceID\": \"BDtLXJ5ss8Df97P7qM7JMD4NMyrc+fa4Fj3q/qeLa9GmfC8K6kfiOsybHgHvslhSkECpmWQ+aHTSj6zDbef9E9A==\", \"deviceIdV2\": \"17b6d6731ed4f34acc0d0d8cdc202b88\", \"androidid\": \"fbddc0a64a19b097\", \"key_web_ua\": \"Mozilla/5.0 (Linux; Android 10; AOSP on crosshatch Build/QP1A.190711.020; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/74.0.3729.186 Mobile Safari/537.36 TTVersion/6.10.1 TTFrom/tt\"}";
        this.ttThread = new TTThread();
        this.ttThread.setAccountCookie(getApplication(), ck);
        this.ttThread.start();


        TextView logEdt = (TextView) findViewById(R.id.edtLog);
        logEdt.setMovementMethod(new ScrollingMovementMethod());


        Button btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Auto login package
                ttThread.auto_login();
            }
        });
        Button btnSearchChannel = (Button) findViewById(R.id.btnSearchChannel);
        btnSearchChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String channelStr = ((EditText) findViewById(R.id.edtChannelSearch)).getText().toString();
                ttThread.search_channel(channelStr);
            }
        });

        Button btnEnterChannel = (Button) findViewById(R.id.btnEnterChannel);
        btnEnterChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String displayId = ((EditText) findViewById(R.id.edtChannelSearch)).getText().toString();
                ttThread.enter_channel(Long.valueOf(displayId));
            }
        });

        Button btnLeaveChannel = (Button) findViewById(R.id.btnLeaveRoom);
        btnLeaveChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String displayId = ((EditText) findViewById(R.id.edtChannelSearch)).getText().toString();
                ttThread.leave_channel(Long.valueOf(displayId));
            }
        });

        Button btnFollow = (Button) findViewById(R.id.btnFollow);
        btnFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //tt317892845
                String displayId = ((EditText) findViewById(R.id.edtChannelSearch)).getText().toString();
                String AccountId = ((EditText) findViewById(R.id.edtAccountId)).getText().toString();
                ttThread.follow_user(AccountId, Long.valueOf(displayId), "王者荣耀");
            }
        });


        Button btnChatText = (Button) findViewById(R.id.btnChatText);
        btnChatText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String displayId = ((EditText) findViewById(R.id.edtChannelSearch)).getText().toString();
                String content = ((EditText) findViewById(R.id.edtPublichChatMessage)).getText().toString();
                ttThread.channel_chat_text(Long.valueOf(displayId), content);
            }
        });


        Button btnGreets = (Button) findViewById(R.id.btnGreets);
        btnGreets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String greetToAccountId = ((EditText) findViewById(R.id.edtGreetToAccountId)).getText().toString();
                String greetMessage = ((EditText) findViewById(R.id.edtGreetMessage)).getText().toString();
                ttThread.greet(greetToAccountId, greetMessage);
            }
        });

        Button btnReqChannelList = (Button) findViewById(R.id.btnReqChannelList);
        btnReqChannelList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String edtTagId = ((EditText) findViewById(R.id.edtTagId)).getText().toString();
                ttThread.req_new_game_channel_list(Integer.valueOf(edtTagId), 2, 20);
                //ttThread.req_new_game_channel_list(TagIds.getId("全房间"), 2, 20);
            }
        });
        Button btnReqMicMember = (Button) findViewById(R.id.btnReqMicMember);
        btnReqMicMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String channelId = ((EditText) findViewById(R.id.edtChannelId)).getText().toString();
                ttThread.req_channel_under_mic_member_list(Long.valueOf(channelId));
//                if (ttThread.getChannels().size() > 0) {
//                    Iterator<Map.Entry<Long, RespNewGameChannelList.ChannelList>> iterator = ttThread.getChannels().entrySet().iterator();
//                    while (iterator.hasNext()) {
//                        Map.Entry<Long, RespNewGameChannelList.ChannelList> ch = iterator.next();
//                        ttThread.req_channel_under_mic_member_list(Long.valueOf(ch.getValue().getChannelId()));
//                    }
//                }
            }
        });


    }

    /**
     * A native method that is implemented by the 'tttalk' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}