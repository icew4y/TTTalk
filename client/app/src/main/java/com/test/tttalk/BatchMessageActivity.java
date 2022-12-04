package com.test.tttalk;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.test.tttalk.db.DBManager;

public class BatchMessageActivity extends AppCompatActivity {

    private Button btnSendBatchMsgs;

    public BatchMessageActivity() {
        super();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batchmessage);

        long count = DBManager.getInstance(getApplication()).count();
        ((TextView)findViewById(R.id.txtCurrentNumber)).setText(String.valueOf(count));

        btnSendBatchMsgs = (Button) findViewById(R.id.btnSendBatchMsgs);
        btnSendBatchMsgs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Cursor fetch = DBManager.getInstance(getApplication())
                        .fetch();

                for (fetch.moveToFirst(); !fetch.isAfterLast(); fetch.moveToNext()){
                    String account = fetch.getString(1);
                    System.out.println(account);
                    fetch.moveToNext();
                    //MainActivity.getMainInstance().getTTalk().greet(account, "Hello!您好！");
                }
            }
        });
    }
}
