package com.test.tttalk;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.protobuf.protos.ChatMessageResp;
import com.protobuf.protos.GreetingResp;
import com.protobuf.protos.RespNewGameChannelList;
import com.protobuf.protos.ResponseChannelUnderMicMemberList;
import com.test.tttalk.db.DBManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import abhishekti7.unicorn.filepicker.UnicornFilePicker;
import abhishekti7.unicorn.filepicker.utils.Constants;

public class ChannelMessageActivity extends AppCompatActivity {
    private String TAG = "ChannelMessageActivity";

    private static ChannelMessageActivity channelMessageInstance = null;
    public static ChannelMessageActivity getInstance() {
        return channelMessageInstance;
    }

    private AtomicBoolean isStopSending = new AtomicBoolean(false);

    private TextView textFilePath;
    private String filePath = "";
    private ArrayList<String> messages = new ArrayList<>();

    private final static int msg_log = 1;
    private final static int msg_toast = 2;
    private final static int msg_sent_suc = 3;
    private final static int msg_sent_failed = 4;
    public void sendMsg(Message msg) {
        this.messageHandler.sendMessage(msg);
    }
    private void LogInfo(String info, int cmd) {
        Message msg = new Message();
        msg.what = cmd;
        msg.obj = info;
        ChannelMessageActivity.getInstance().sendMsg(msg);
        Log.d(TAG, info);
    }

    public void sleep(long millsecond) {
        try {
            Thread.sleep(millsecond);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Handler messageHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case msg_log:{
                    //Toast.makeText(getApplication(), (String)msg.obj, Toast.LENGTH_SHORT).show();
                    TextView logEdt = (TextView) findViewById(R.id.edtLog);
                    String logstr = logEdt.getText().toString();
                    logstr += (String) msg.obj;
                    logEdt.setText(logstr + "\n");
                    break;
                }
                case msg_toast:{
                    Toast.makeText(getApplication(), (String)msg.obj, Toast.LENGTH_SHORT).show();
                    TextView logEdt = (TextView) findViewById(R.id.edtLog);
                    String logstr = logEdt.getText().toString();
                    logstr += (String) msg.obj;
                    logEdt.setText(logstr + "\n");
                    break;
                }
                case msg_sent_suc:{
                    TextView textView = ((TextView) findViewById(R.id.textSentNum));
                    int num = Integer.parseInt(textView.getText().toString());
                    num = num + 1;
                    textView.setText(String.valueOf(num));
                    break;
                }
                case msg_sent_failed:{
                    TextView textView = ((TextView) findViewById(R.id.textSentFailure));
                    int num = Integer.parseInt(textView.getText().toString());
                    num = num + 1;
                    textView.setText(String.valueOf(num));
                    break;
                }
                default:break;
            }
            return false;
        }
    });

    public ChannelMessageActivity() {
        super();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQ_UNICORN_FILE && resultCode == RESULT_OK) {
            ArrayList<String> files = data.getStringArrayListExtra("filePaths");
            for(String file : files){
                Log.e(TAG, file);
            }
            filePath = files.get(0);
            textFilePath.setText(filePath);
        }
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channelmessage);
        channelMessageInstance = this;

        textFilePath = (TextView) findViewById(R.id.textFilePath);
        ((TextView) findViewById(R.id.textSentNum)).setText("0");
        ((TextView) findViewById(R.id.textSentFailure)).setText("0");

        Button btnSelectMsgFile = (Button) findViewById(R.id.btnSelectMsgFile);
        btnSelectMsgFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UnicornFilePicker.from(ChannelMessageActivity.this)
                        .addConfigBuilder()
                        .selectMultipleFiles(false)
                        .showOnlyDirectory(false)
                        .setRootDirectory(Environment.getExternalStorageDirectory().getAbsolutePath())
                        .showHiddenFiles(true)
                        .setFilters(new String[]{"txt"})
                        .addItemDivider(true)
                        .build()
                        .forResult(Constants.REQ_UNICORN_FILE);
            }
        });

        Button btnSendChannelMsgs = (Button) findViewById(R.id.btnSendChannelMsgs);
        btnSendChannelMsgs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int interval = Integer.parseInt(((EditText) findViewById(R.id.edtSendInterval)).getText().toString()) * 1000;
                if (interval <= 0) {
                    LogInfo("间隔不能等于少于0!", msg_toast);
                    return;
                }
                if (!isStopSending.get()) {
                    try {
                        if (filePath != null && !filePath.equals("")) {
                            String content = new String(FileUtils.readFile(filePath));
                            content = content.replaceAll("\r", "");
                            String[] msgs = content.split("\n");
                            messages.clear();
                            Collections.addAll(messages, msgs);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (messages.size()  == 0) {
                        LogInfo("发送的消息数量不能为0!", msg_toast);
                        return;
                    }
                    List<Long> RespChannelIds = new ArrayList<>();
                    AtomicBoolean isDoneRound = new AtomicBoolean(false);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            LogInfo("开始发送消息！", msg_toast);
                            while (true) {

                                isDoneRound.set(false);
                                LogInfo("获取直播间...", msg_log);
                                MainActivity.getMainInstance().getTTalk().req_new_game_channel_list(0, 2, 10, new TTalk.ICallback() {
                                    @Override
                                    public void callback(Object o) {
                                        LogInfo("RespNewGameChannelList callback", msg_log);
                                        RespNewGameChannelList channelList = (RespNewGameChannelList)o;
                                        if (channelList.getBaseResp().getErrCode() == 0) {
                                            LogInfo("channelList.getBaseResp().getErrCode == 0", msg_log);
                                            for (RespNewGameChannelList.ChannelList channel : channelList.getChannelListList()){
                                                RespChannelIds.add(channel.getChannelId());
                                            }
                                        }else{
                                            LogInfo("获取直播间失败! errCode:" + channelList.getBaseResp().getErrCode() +
                                                    ", errMsg:" + channelList.getBaseResp().getErrMsg().toStringUtf8(), msg_log);
                                        }
                                        isDoneRound.set(true);
                                    }
                                });

                                int count = 20;
                                while (!isDoneRound.get()) {
                                    LogInfo("isDoneRound.get()", msg_log);
                                    sleep(1000);
                                    count --;
                                    if (count >= 20) {
                                        break;
                                    }
                                }
                                LogInfo("获取直播间返回，数量:" + RespChannelIds.size(), msg_log);

                                for (long channelId : RespChannelIds) {
                                    //EnterChannel
                                    MainActivity.getMainInstance().getTTalk().enter_channel(channelId);
                                    sleep(2000);
                                    Random random = new Random();
                                    random.setSeed(System.currentTimeMillis());
                                    int i = random.nextInt(messages.size());
                                    String content = messages.get(i);
                                    LogInfo("直播间消息: 房间ID:" + channelId + ", 内容:" + content, msg_log);
                                    MainActivity.getMainInstance().getTTalk().channel_chat_text(channelId, content, new TTalk.ICallback() {
                                        @Override
                                        public void callback(Object o) {
                                            ChatMessageResp chatMessageResp = (ChatMessageResp)o;
                                            if (chatMessageResp.getBaseResp().getErrCode() == 0) {
                                                LogInfo("直播间消息发送成功!", msg_log);
                                                LogInfo("", msg_sent_suc);
                                            }else{
                                                LogInfo("直播间消息发送失败:" + chatMessageResp.getBaseResp().getErrCode() +
                                                        ", 错误信息:" + chatMessageResp.getBaseResp().getErrMsg().toStringUtf8(), msg_log);
                                                LogInfo("", msg_sent_failed);
                                            }
                                        }
                                    });
                                    sleep(800);
                                    MainActivity.getMainInstance().getTTalk().leave_channel(channelId);
                                    sleep(800);

                                    if (isStopSending.get()) {
                                        break;
                                    }
                                }
                                sleep(interval);
                                RespChannelIds.clear();
                                //退出线程
                                if (isStopSending.get()) {
                                    LogInfo("停止发送成功！", msg_toast);
                                    isStopSending.set(false);
                                    break;
                                }
                            }
                        }
                    }).start();
                }


            }
        });

        Button btnStopChannelMsgs = (Button) findViewById(R.id.btnStopChannelMsgs);
        btnStopChannelMsgs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isStopSending.set(true);
            }
        });
    }
}
