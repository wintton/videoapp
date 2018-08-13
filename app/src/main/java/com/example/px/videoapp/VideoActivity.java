package com.example.px.videoapp;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 需要注意的是我实验了.mp4,3gp和avi3种格式的视频，在5.1的真机和模拟器上avi格式都是只有声音没有影像，其他两种格式
 * 播放正常。
 */

public class VideoActivity extends AppCompatActivity{

        private SurfaceView sfv;//能够播放图像的控件
        private SeekBar sb;//进度条
        private String path ;//本地文件路径
        private SurfaceHolder holder;
        private MediaPlayer player;//媒体播放器
        private Button Play;//播放按钮
        private Timer timer;//定时器
        private TimerTask task;//定时器任务
        private int position = 0;
        private EditText et;
        private VideoView videoview;

    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };
    //检测是否有写的权限


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_play);
            //android6.0以后必须动态获取权限
            int permission = ActivityCompat.checkSelfPermission(VideoActivity.this,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(VideoActivity.this, PERMISSIONS_STORAGE,2);
            }
            initView();
        }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == 1){
                if (data != null) {
                    // 得到视屏的的全路径
                 Uri uri = data.getData();
                      et.setText(uri+"");
                 }
            }
        super.onActivityResult(requestCode, resultCode, data);
    }
    /**
     *
     * 从媒体库中查询 获取视频缩略图  新视频增加后需要SDCard重新扫描才能给新增加的文件添加缩略图，灵活性差，而且不是很稳定，适合简单应用
     * @createAuthor luzhenbang
     * @updateAuthor
     * @updateInfo (此处输入修改内容,若无修改可不写.)
     * @param context
     * @param contentResolver
     * @return 视频集合 key : 视频路径  value ：视频压缩后的位图
     */
    @SuppressLint("NewApi")
    public static HashMap<String , Bitmap> getVideoMapThumbnail(Context context, ContentResolver contentResolver) {
        ContentResolver testcr = context.getContentResolver();
        String[] projection = { MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID, };
//         String whereClause = MediaStore.Video.Media.DATA + " = '" + Videopath + "'";、
        //查询多媒体数据库
        Cursor cursor = testcr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null,null, MediaStore.Video.Media.DEFAULT_SORT_ORDER);
        int _id = 0;
        String videoPath = "";
        HashMap<String, Bitmap>  bitmaps = new HashMap<String, Bitmap>();
        if (cursor == null || cursor.getCount() == 0) {
            return null;
        }
        if (cursor.moveToFirst()) {
            int _idColumn = cursor.getColumnIndex(MediaStore.Video.Media._ID);
            int _dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            do {
//视频id
                _id = cursor.getInt(_idColumn);
                //视频路径
                videoPath = cursor.getString(_dataColumn);
                Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(contentResolver, _id, MediaStore.Images.Thumbnails.MINI_KIND,options);
                bitmaps.put(videoPath, bitmap);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return bitmaps;
    }


    //初始化控件，并且为进度条和图像控件添加监听
        private void initView() {
            sfv = (SurfaceView) findViewById(R.id.sfv);
            sb = (SeekBar) findViewById(R.id.sb);
            Play = (Button) findViewById(R.id.play);
            et = (EditText) findViewById(R.id.et);
            videoview = (VideoView) findViewById(R.id.videoview);
            Play.setEnabled(false);

            holder = sfv.getHolder();
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    //当进度条停止拖动的时候，把媒体播放器的进度跳转到进度条对应的进度
                    if (player != null) {
                        player.seekTo(seekBar.getProgress());
                    }
                }
            });

            holder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    //为了避免图像控件还没有创建成功，用户就开始播放视频，造成程序异常，所以在创建成功后才使播放按钮可点击
                    Log.d("zhangdi","surfaceCreated");
                    Play.setEnabled(true);
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    Log.d("zhangdi","surfaceChanged");
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    //当程序没有退出，但不在前台运行时，因为surfaceview很耗费空间，所以会自动销毁，
                    // 这样就会出现当你再次点击进程序的时候点击播放按钮，声音继续播放，却没有图像
                    //为了避免这种不友好的问题，简单的解决方式就是只要surfaceview销毁，我就把媒体播放器等
                    //都销毁掉，这样每次进来都会重新播放，当然更好的做法是在这里再记录一下当前的播放位置，
                    //每次点击进来的时候把位置赋给媒体播放器，很简单加个全局变量就行了。
                    Log.d("zhangdi","surfaceDestroyed");
                    if (player != null) {
                        position = player.getCurrentPosition();
                        stop();
                    }
                }
            });
        }


        private void play() {

            Play.setEnabled(false);//在播放时不允许再点击播放按钮

            if (isPause) {//如果是暂停状态下播放，直接start
                isPause = false;
                player.start();
                return;
            }

            path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/";//path +
            path = et.getText().toString();//sdcard的路径加上文件名称是文件全路径

//            File file = new File(path);
//            if (!file.exists()) {//判断需要播放的文件路径是否存在，不存在退出播放流程
//                Toast.makeText(this,"文件路径不存在",Toast.LENGTH_LONG).show();
//                Play.setEnabled(true);//在播放失败恢复可播放
//                return;
//            }

            try {
                player = new MediaPlayer();
                Uri uri =  null;
                if (path.contains("file")){
                    path = path.substring("file://".length());
                    uri = Uri.parse(path);
                }else{
                    uri = Uri.parse(path);
                }
                player.setDataSource(VideoActivity.this,uri);
                player.setDisplay(holder);//将影像播放控件与媒体播放控件关联起来

                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {//视频播放完成后，释放资源
                        Play.setEnabled(true);
                        position = 0;               //播放完成后位置回到最初
                        stop();
                    }
                });

                player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        //媒体播放器就绪后，设置进度条总长度，开启计时器不断更新进度条，播放视频
                        Log.d("zhangdi","onPrepared");
                        sb.setMax(player.getDuration());
                        timer = new Timer();
                        task = new TimerTask() {
                            @Override
                            public void run() {
                                if (player != null) {
                                    int time = player.getCurrentPosition();
                                    sb.setProgress(time);
                                }
                            }
                        };
                        timer.schedule(task,0,500);
                        sb.setProgress(position);
                        player.seekTo(position);
                        player.start();
                    }
                });

                player.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show();
            }
        }

    private void playVideo(){
        String path = et.getText().toString();//获取视频路径
        Uri uri =  null;
        if (path.contains("file")){
            path = path.substring("file://".length());
            uri = Uri.parse(path);
        }else{
            uri = Uri.parse(path);
        }
        videoview.setVideoURI(uri);//为视频播放器设置视频路径
        videoview.setMediaController(new MediaController(VideoActivity.this));//显示控制栏
        videoview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoview.start();//开始播放视频
            }
        });
    }

    public void play(View v) {
            play();
        playVideo();
            Log.d("zhangdi",path);
        }

        private boolean isPause;
        private void pause() {
            if (player != null && player.isPlaying()) {
                player.pause();
                isPause = true;
                Play.setEnabled(true);
            }
        }

        public void pause(View v) {
            pause();
        }

        private void replay() {
            isPause = false;
            if (player != null) {
                stop();
                play();
            }
        }
        public void selflie(View view){
            openVideo();
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


        public void replay(View v) {
                replay();
         }

        private void stop(){
            isPause = false;
            if (player != null) {
                sb.setProgress(0);
                player.stop();
                player.release();
                player = null;
                if (timer != null) {
                    timer.cancel();
                }
                Play.setEnabled(true);
            }
        }

        public void stop(View v) {
            stop();
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            stop();
        }
}
