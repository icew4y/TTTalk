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

import com.protobuf.protos.GreetingResp;
import com.test.tttalk.db.DBManager;

import java.io.IOException;
import java.io.PipedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import abhishekti7.unicorn.filepicker.UnicornFilePicker;
import abhishekti7.unicorn.filepicker.utils.Constants;

public class BatchMessageActivity extends AppCompatActivity {
    private String TAG = "BatchMessageActivity";

    private Button btnSelectMsgFile, btnSendBatchMsgs, btnStopBatchMsgs;
    private TextView textFilePath;
    private String filePath = "";
    private ArrayList<String> messages = new ArrayList<>();

    private final static int msg_log = 1;
    private final static int msg_toast = 2;
    private final static int msg_need_sent = 3;
    private final static int msg_sent = 4;

    private AtomicBoolean isStopSending = new AtomicBoolean(false);

    private static BatchMessageActivity batchMessageInstance = null;
    public static BatchMessageActivity getInstance() {
        return batchMessageInstance;
    }

    public void sendMsg(Message msg) {
        this.messageHandler.sendMessage(msg);
    }
    private void LogInfo(String info, int cmd) {
        Message msg = new Message();
        msg.what = cmd;
        msg.obj = info;
        BatchMessageActivity.getInstance().sendMsg(msg);
        Log.d(TAG, info);
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
                case msg_need_sent:{
                    ((TextView) findViewById(R.id.textNeedSendNum)).setText((String) msg.obj);
                    break;
                }
                case msg_sent:{
                    ((TextView) findViewById(R.id.textSentNum)).setText((String) msg.obj);
                    break;
                }
                default:break;
            }
            return false;
        }
    });

    public BatchMessageActivity() {
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
        setContentView(R.layout.activity_batchmessage);
        batchMessageInstance = this;

        long count = DBManager.getInstance(getApplication()).count();
        ((TextView)findViewById(R.id.txtCurrentNumber)).setText(String.valueOf(count));
        textFilePath = (TextView) findViewById(R.id.textFilePath);

        btnSelectMsgFile = (Button) findViewById(R.id.btnSelectMsgFile);
        btnSelectMsgFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UnicornFilePicker.from(BatchMessageActivity.this)
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

        btnSendBatchMsgs = (Button) findViewById(R.id.btnSendBatchMsgs);
        btnSendBatchMsgs.setOnClickListener(new View.OnClickListener() {
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
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            LogInfo("开始发送消息！", msg_toast);
                            Cursor fetch = DBManager.getInstance(getApplication())
                                    .fetch();
                            LogInfo(fetch.getCount() + "", msg_need_sent);
                            int count = 0;
                            for (fetch.moveToFirst(); !fetch.isAfterLast(); fetch.moveToNext()){
                                String sendAccount = fetch.getString(1);
                                //System.out.println(account);
                                Random random = new Random();
                                random.setSeed(System.currentTimeMillis());
                                int i = random.nextInt(messages.size());
                                String content = messages.get(i);
                                MainActivity.getMainInstance().getTTalk().greet(sendAccount, content, new TTalk.ICallback() {
                                    @Override
                                    public void callback(Object o) {
                                        GreetingResp greetingResp = (GreetingResp)o;
                                        if (greetingResp.getBaseResp().getErrCode() == 0) {
                                            LogInfo("私信成功! " + sendAccount, msg_log);
                                        }else{
                                            LogInfo("私信失败:" + sendAccount + "," + greetingResp.getBaseResp().getErrCode() +
                                                    ", 错误信息:" + greetingResp.getBaseResp().getErrMsg().toStringUtf8(), msg_log);
                                        }
                                    }
                                });
                                count ++;
                                LogInfo(count + "", msg_sent);
                                try {
                                    Thread.sleep(interval);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
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

        btnStopBatchMsgs = (Button) findViewById(R.id.btnStopBatchMsgs);
        btnStopBatchMsgs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isStopSending.set(true);
            }
        });
    }
}
