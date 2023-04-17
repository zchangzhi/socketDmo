package com.example.chat;

import static android.text.format.DateUtils.FORMAT_SHOW_TIME;
import static android.text.format.DateUtils.formatDateTime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.chat.services.TCPService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private String TAG = "zcz";
    private static final int MESSAGE_RECEIVE_NEW_MSG = 1;
    private static final int MESSAGE_SOCKET_CONNECTED = 2;

    private Button mSendButton;
    private TextView mMessageTextView;
    private EditText mMessageEditText;

    private PrintWriter mPrintWriter;
    private Socket mClientSocket;

    @SuppressLint("HandlerLeak")
    private Handler mhandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case MESSAGE_RECEIVE_NEW_MSG:
                    mMessageTextView.setText(mMessageTextView.getText() + (String)msg.obj);
                    break;
                case MESSAGE_SOCKET_CONNECTED:
                    //mPrintWriter.println(msg);
                    mSendButton.setEnabled(true);
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMessageTextView = findViewById(R.id.msg_container);
        mSendButton = findViewById(R.id.send);
        mMessageEditText = findViewById(R.id.msg);
        mSendButton.setOnClickListener(this);
        Intent service = new Intent(this, TCPService.class);
        startService(service);
        SystemClock.sleep(1000);
        Log.d(TAG, "onCreate: service 被调用！");
        new Thread(){
            @Override
            public void run() {
                connectTCPService();
            }
        }.start();

    }

    @Override
    protected void onDestroy() {
        if (mClientSocket != null){
            try {
                mClientSocket.shutdownInput();
                mClientSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v == mSendButton){
            final String msg = mMessageEditText.getText().toString();
            if (!TextUtils.isEmpty(msg) && mPrintWriter != null){
                mMessageEditText.setText("");
                String time = formatDateTime(this,System.currentTimeMillis(),FORMAT_SHOW_TIME);
                final String showedMsg = "self:" + time + ":" + msg + "\n";
                mMessageTextView.setText(mMessageTextView.getText() + showedMsg);
                Log.d(TAG, "connectTCPService: PrintWriter2 = " + mPrintWriter);
                Log.d(TAG, "connectTCPService: msg = " + msg);
                SystemClock.sleep(1000);
                //mhandler.sendEmptyMessage(MESSAGE_SOCKET_CONNECTED);
                //mPrintWriter.println(msg);
                new Thread(){
                    @Override
                    public void run() {
                        mPrintWriter.println(msg);
                        super.run();
                    }
                }.start();

            }
        }
    }

    private void deoPrintWriter() throws IOException {

        mPrintWriter.println("msg");
    }
    private void connectTCPService() {
        Socket socket = null;
        while (socket == null){
            try {
                socket = new Socket("localhost",8688);
                mClientSocket = socket;
                mPrintWriter = new PrintWriter(mClientSocket.getOutputStream(),true);
                //mPrintWriter.println("msg");
                //deoPrintWriter();
                mhandler.sendEmptyMessage(MESSAGE_SOCKET_CONNECTED);
                Log.d(TAG, "connectTCPService: connect server success ");
            } catch (IOException e) {
                Log.d(TAG, "connectTCPService: connect server fail ");
                SystemClock.sleep(1000);
                throw new RuntimeException(e);
            }
        }
        Log.d(TAG, "connectTCPService: PrintWriter1 = " + mPrintWriter);

        try {
            BufferedReader br =new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (!MainActivity.this.isFinishing()){
                String msg = br.readLine();
                Log.d(TAG, "connectTCPService: receive :" + msg);
                if (msg != null){
                    String time = formatDateTime(this,System.currentTimeMillis(),FORMAT_SHOW_TIME);
                    final String showedMsg = "server" + time + ":" + msg + "\n";
                    mhandler.obtainMessage(MESSAGE_RECEIVE_NEW_MSG,showedMsg).sendToTarget();
                }
            }
            Log.d(TAG, "connectTCPService: quit...");
            //mPrintWriter.close();
            br.close();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}