package com.example.chat.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class TCPService extends Service {
    private String TAG = "zcz";
    private  boolean mIsServiceDestoryed = false;
    private String[] mDefineMessages = new String[]{
            "你好啊，哈哈哈",
            "请问你叫什么名字？",
            "今天北京天气不错，shy",
            "你知道吗？我可是可以和多个人聊天的哦",
            "给你讲个笑话：据说爱笑的人运气不会太差，不知道真假。"
    };

    @Override
    public void onCreate() {
        new Thread(new TcpService()).start();
        super.onCreate();
    }

    public TCPService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onDestroy() {
        mIsServiceDestoryed = true;
        super.onDestroy();
    }

    private class TcpService implements Runnable {
        @SuppressWarnings("resource")
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket =new ServerSocket(8688);
            } catch (IOException e) {
                Log.d(TAG, "run: establish tcp server failed,port:8688");
                throw new RuntimeException(e);
            }
            Log.d(TAG, "TCPService: run!!!");
            while (!mIsServiceDestoryed){
                try {
                    final Socket client = serverSocket.accept();
                    Log.d(TAG, "run: accept");
                    new Thread(){
                        @Override
                        public void run() {
                            try {
                                responseClient(client);
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        };
                    }.start();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void responseClient(Socket client) throws IOException {
        //用于接收客户端信息
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        //用于向客户端发送信息
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())),true);
        out.println("欢迎来到聊天室！");
        while(!mIsServiceDestoryed){
            String str = in.readLine();
            Log.d(TAG, "responseClient: msg from client:" + str);
            if (str == null){
                //客户端断开连接
                break;
            }
            int i = new Random().nextInt(mDefineMessages.length);
            String msg = mDefineMessages[i];
            out.println(msg);
            Log.d(TAG, "responseClient: send :" + msg);

        }
        Log.d(TAG, "responseClient: client quit.");
        //关闭流
        out.close();
        in.close();
        client.close();
    }
}