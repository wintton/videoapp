package com.example.px.videoapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * 需要注意的是我实验了.mp4,3gp和avi3种格式的视频，在5.1的真机和模拟器上avi格式都是只有声音没有影像，其他两种格式
 * 播放正常。
 */

public class VideoSmipleActivity extends AppCompatActivity{

    private String path ;//本地文件路径
    private Button Play;//播放按钮
    private EditText et;
    private TextView connect_text;
    private FixVideoView videoview;
    private LinearLayout activity_main;
    private boolean      isquit = false;
    private Uri         URI;

    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };
    //检测是否有写的权限


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        //android6.0以后必须动态获取权限
        int permission = ActivityCompat.checkSelfPermission(VideoSmipleActivity.this,
                "android.permission.WRITE_EXTERNAL_STORAGE");
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // 没有写的权限，去申请写的权限，会弹出对话框
            ActivityCompat.requestPermissions(VideoSmipleActivity.this, PERMISSIONS_STORAGE,2);
        }
        initView();
        startConnect();     //与服务器建立连接

        playVideo(Uri.parse("http://mp4.vjshi.com/2016-05-06/6902c47bcc019ffaa4c64c384648782c.mp4"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1){
            if (data != null) {
                // 得到视屏的的全路径
                Uri uri = data.getData();
                et.setText(uri+"");
                playVideo(uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what){
                case 100:{
                    //显示提示信息
                    String msg = message.obj.toString();
                    showToast(msg);
                }
                break;
                case 101:{
                    //播放网络视屏
                    String  string = message.obj.toString();
                    playVideo(Uri.parse(string));
                }
                break;
                case 102:{
                    //显示连接状态
                    String  string = message.obj.toString();
                    connect_text.setText(string);
                }
                break;
                default:
            }
            return true;
        }
    });
    //开始连接
    private void startConnect(){
        ServerThread serverThread = new ServerThread("112.126.83.31",8686);
        serverThread.start();
    }
    public void DoConnectServer(View view){
        if (connect_text.getText().toString().contains("成功") || connect_text.getText().toString().contains("连接中")){
            showToast("已连接，请勿重复连接");
        }else{
            showToast("连接中");
            sendMessage(102,"连接中");
            startConnect();
        }
    }
    //初始化控件，并且为进度条和图像控件添加监听
    private void initView() {
        Play = (Button) findViewById(R.id.play);
        et = (EditText) findViewById(R.id.et);
        videoview = (FixVideoView) findViewById(R.id.videoview);
        activity_main = (LinearLayout) findViewById(R.id.activity_main);
        connect_text = (TextView) findViewById(R.id.connect_text);

        videoview.setSize(videoview.getWidth(),videoview.getHeight());
    }
    private void showToast(String msg){
        Toast.makeText(VideoSmipleActivity.this,msg,Toast.LENGTH_SHORT).show();
    }
    private void sendMessage(int what,String msg){
        Message message = new Message();
        message.what = what;
        message.obj = msg;
        mHandler.sendMessage(message);
    }
    private void playVideo(Uri uri){
//        Uri uri =  null;
//        if (path.contains("file")){
//            path = path.substring("file://".length());
//            uri = Uri.parse(path);
//        }else{
//            uri = Uri.parse(path);
//        }
        URI = uri;
        videoview.setVideoURI(uri);//为视频播放器设置视频路径
        MediaController mediaController = new MediaController(VideoSmipleActivity.this);
        videoview.setMediaController(mediaController);//显示控制栏
        mediaController.setMediaPlayer(videoview);
        int videoViewheight = videoview.getHeight();
        Context mContext = VideoSmipleActivity.this;
        int offset = (int) (10 * mContext.getResources().getDisplayMetrics().density + 0.5f);
        int screenHeight = activity_main.getHeight();
//        mediaController.setPadding(0, 0, 0, screenHeight - videoViewheight - statusBarHeight - et.getHeight());
        mediaController.setPadding(0, 0, 0, screenHeight -  videoViewheight - et.getHeight() - offset);
        videoview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoview.start();//开始播放视频
            }
        });
        videoview.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                //循环播放
                playVideo(URI);
            }
        });
    }
    public void selflie(View view){
        openVideo();
    }
    public  void DoFinish(View view){
        VideoSmipleActivity.this.finish();
    }

    /**
     * 获取状态栏的高度
     * @return 状态栏的高度
     */
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 打开所有视频
     */
    private void openVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        // 开启一个带有返回值的Activity，请求码为PHOTO_REQUEST_GALLERY
        startActivityForResult(intent, 1);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    /**
     * socket 建立与服务器的长连接  基于TCP协议
     */
    class  ServerThread extends Thread{
        Socket socket = null;
        String ip = "112.126.83.31";
        int prot = 80;
        PrintWriter out;
        BufferedReader bufferReader;
        String inputLine = "";
        boolean isquit = false;
        ServerThread(String ip,int prot){
            this.ip  = ip;
            this.prot = prot;
            isquit = false;
        }
        @Override
        public void run(){
            try {
                socket = new Socket(ip,prot);
                if(socket != null){
                    char data[] = new char[1024 * 10];
                    for (int i = 0; i < data.length; i++) {
                        data[i] = (char) i;
                    }
                    InputStream dInputStream = socket.getInputStream();
                    InputStreamReader isr = new InputStreamReader(dInputStream);
                    bufferReader = new BufferedReader(isr);

                    // 声明输出流out，向服务端输出“Output Message！！”
                    StringBuffer buffer = new StringBuffer();
                    Log.w("robin", "try to writer");
                    out = new PrintWriter(socket.getOutputStream(), true);
                    StringBuffer strBuffer = new StringBuffer();
                    sendMessage(102,"与服务器成功建立连接");
                    while (!isquit){
                        //心跳线程
                        Thread.sleep(2000);
                        out.println("/q.x?t=E688BBBBBBBB00000000");
                        try {
                            inputLine = bufferReader.readLine();
                            if (inputLine != null && inputLine.contains("quit")){
                                isquit = true;          //收到quit信息就退出链接
                                //发送断开连接通知
                                sendMessage(100,"服务器断开连接");
                                sendMessage(102,"服务器断开连接");
                            }
                            if (inputLine != null && inputLine.contains("play=")){
                                //发送断开连接通知
                                int remove = inputLine.indexOf("nil");
                                sendMessage(101,inputLine.substring("play=".length(),remove));
                                sendMessage(100,"开始播放网页资源");
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    out.flush();
                    dInputStream.close();
                    isr.close();
                    out.close();
                }else{
                    sendMessage(100,"连接失败");
                    sendMessage(102,"与服务器连接失败");
                }
            } catch (IOException e) {
                e.printStackTrace();
                sendMessage(100,"连接失败");
                sendMessage(102,"与服务器连接失败");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    /**
     *  基于UDP协议 Socket  发送消息
     */
    class  UDPSocketSend extends Thread{
        InetAddress address;
        DatagramSocket socket = null;
        UDPSocketSend(DatagramSocket socket){
            this.socket = socket;
        }
        @Override
        public void run() {
            {
                try {
                    sendMessage(102,"与服务器成功建立连接");
                    while (!isquit){
                        Thread.sleep( 2000);
                        address = InetAddress.getByName("47.92.153.254");
                        int port = 8686;
                        byte[] data = "/q.x?t=E688BBBBBBBB00000000".getBytes();
                        // 2.创建数据报，包含发送的数据信息
                        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                        // 3.创建DatagramSocket对象
                        // 4.向服务器端发送数据报
                        socket.send(packet);
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    socket.close();
                }
            }
        }
    }
    /**
     *  基于UDP协议 Socket   接受消息
     */
    class  UDPSocketReceive extends Thread{
        // 1.创建数据报，用于接收服务器端响应的数据
        byte[] data2 = new byte[1024];
        DatagramSocket socket;
        UDPSocketReceive(DatagramSocket socket){
            this.socket = socket;
        }
        @Override
        public void run() {
            {
                try {
                    socket = new DatagramSocket();
                    DatagramPacket packet2 = new DatagramPacket(data2, data2.length);
                    // 2.接收服务器响应的数据
                    socket.receive(packet2);
                    // 3.读取数据
                    String reply = new String(data2, 0, packet2.getLength());
                    // 4.关闭资源
                    socket.close();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
