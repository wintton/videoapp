package com.example.px.videoapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private boolean    isPreview = false,isRecording = false;
    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder   mMediaRecorder;
    private RelativeLayout preview;
    private ImageView  img_photo;
    private Button      btn_record;           //录像摄像头
    private Uri         imgUri;              //图片URI
    private int number_camera = 0;          //摄像头个数
    private int which_camera = 0;           //打开哪个摄像头


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initdata();
        setContentView(R.layout.activity_main);
        bindViews();
        init();

        number_camera = Camera.getNumberOfCameras();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.fist:{
                Intent intent = new Intent(MainActivity.this,VideoSmipleActivity.class);
                startActivity(intent);
            }
            break;
            default:
        }
        return true;
    }

    private void initanim() {
        final Animation animation = AnimationUtils.loadAnimation(MainActivity.this,R.anim.img_anim);
        img_photo.startAnimation(animation);
    }


    /**
     * 初始化摄像头参数
     */
    private void initCamera() {
        //设备支持摄像头才创建实例
        if (checkCameraHardware(MainActivity.this)){
            mCamera = getCameraInstance();//打开硬件摄像头，这里导包得时候一定要注意是android.hardware.Camera
        }else{
            Toast.makeText(MainActivity.this,"当前设备不支持摄像头",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 判断摄像头是否存在
     * @param context
     * @return
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // 摄像头存在
            return true;
        } else {
            // 摄像头不存在
            return false;
        }

    }

    /**
     * 获取Camera实例
     * @return Camera实例
     */
    public  Camera getCameraInstance(){

        Camera c = null;
        //android 6.0以后必须动态调用权限
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) MainActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    3);
        }else {
            try {
                c = Camera.open(which_camera); // 试图获取Camera实例
            } catch (Exception e){
                Log.e("sda",e.toString());
                // 摄像头不可用（正被占用或不存在）
            }
        }
        return c; // 不可用则返回null
    }

    /**
     * 初始化数据
     */
    private void init() {
        // 创建Camera实例
         initCamera();
        if(mCamera != null){
            // 创建Preview view并将其设为activity中的内容
            mPreview = new CameraPreview(this, mCamera);
            preview.addView(mPreview,0);

            try {
                Toast.makeText(MainActivity.this,preview.getChildCount(),Toast.LENGTH_SHORT).show();
            }catch (Exception e){

            }
        }else{
            Toast.makeText(MainActivity.this,"打开摄像头失败",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        super.onPause();
    }

    private void closeCamera() {
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            if (mPreview != null){
                mPreview.getHolder().removeCallback(mPreview.getmCallback());
                preview.removeView(mPreview);
            }
        }
    }

    @Override
    protected void onResume() {
        if (mCamera == null){
            init();
        }
        super.onResume();
    }

    private void initCameraData() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);//得到窗口管理器
        Display display  = wm.getDefaultDisplay();//得到当前屏幕
        Camera.Parameters parameters = mCamera.getParameters();//得到摄像头的参数
        parameters.setPreviewSize(display.getWidth(), display.getHeight());//设置预览照片的大小
        parameters.setPreviewFrameRate(3);//设置每秒3帧
        parameters.setPictureFormat(PixelFormat.JPEG);//设置照片的格式
        parameters.setJpegQuality(85);//设置照片的质量
        parameters.setPictureSize(display.getHeight(), display.getWidth());//设置照片的大小，默认是和     屏幕一样大
        mCamera.setParameters(parameters);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 3){
            init();                 //获取权限后在去验证一次
        }else if (requestCode == 4){
            startRecord();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {//处理按键事件
        if(mCamera!=null && event.getRepeatCount()==0)//代表只按了一下
        {
            switch(keyCode){
                case KeyEvent.KEYCODE_BACK://如果是搜索键
                    mCamera.autoFocus(null);//自动对焦
                    break;
                case KeyEvent.KEYCODE_DPAD_CENTER://如果是中间键
                    mCamera.takePicture(null, null, new TakePictureCallback());//将拍到的照片给第三个对象中，这里的TakePictureCallback()是自己定义的，在下面的代码中
                    break;
            }
        }
        return true;//阻止事件往下传递，否则按搜索键会变成系统默认的
    }
    private final class TakePictureCallback implements Camera.PictureCallback {
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                img_photo.setVisibility(View.VISIBLE);
                img_photo.setImageBitmap(bitmap);
                initanim();
                File filePar = new File(Environment.getExternalStorageDirectory()+"/videoappimg");
                //如果不存在这个文件夹就去创建
                if (!filePar.exists()){
                    filePar.mkdirs();
                }
                File file = new File(Environment.getExternalStorageDirectory(),"/videoappimg/"+ "videoapp_" + System.currentTimeMillis()+".jpg");
                FileOutputStream outputStream = new FileOutputStream(file);
                imgUri = Uri.fromFile(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.close();
                camera.stopPreview();
                camera.startPreview();//处理完数据之后可以预览
            } catch (Exception e) {
            }
        }
    }
    /**
     * 未绑定页面时的数据初始化操作
     */
    private void initdata() {
        Window window = getWindow();                    //得到窗口
        requestWindowFeature(Window.FEATURE_NO_TITLE);              //请求没有标题
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, //设置全屏
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);    //设置高亮
    }
    /**
     * 绑定视图
     */
    private void bindViews() {
        preview = (RelativeLayout) findViewById(R.id.camera_preview);
        img_photo = (ImageView)findViewById(R.id.img_photo);
        btn_record = (Button)findViewById(R.id.btn_record);
    }
    //打开相机
    public void DoOpenCamera(View view){
        closeCamera();
        init();
    }
    //切换镜头
    public void DoChangeCamera(View view){
        if (number_camera > 0){
            which_camera = which_camera == 0?1:0;
            closeCamera();
            init();
        }else {
            showToast("当前设备没有多余摄像头");
        }
    }
    //拍照
    public void DoTakePhoto(View view){
        if (mCamera != null){
            mCamera.takePicture(null, null, new TakePictureCallback());
        }else{
            showToast("没有打开相机");
        }
    }
    public void DoRecord(View view){
        try{
            if (isRecording){stopRecord();}else{startRecord();}
        }catch (Exception e){
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
        }

    }
    //打开图片
    public  void DoOpenImg(View view){
        new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_HOLO_LIGHT).setTitle("提示")
                .setMessage("是否查看图片")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton("查看", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);    //打开图片得启动ACTION_VIEW意图
                        intent.setDataAndType(imgUri, "image/*");    //设置intent数据和图片格式
                        startActivity(intent);
                    }
                }).create().show();
    }
    //准备录制视屏，设置相关参数
    private boolean prepareRecord(){
        if (mCamera == null){
            showToast("未打开摄像头");
            return  false;
        }
        //如果没有权限
        if (!checkRecordPermiss()){
            showToast("未获得录音权限");
            return  false;
        }
        btn_record.setText("停止录像");

        mMediaRecorder = new MediaRecorder();
        // 第1步：解锁并将摄像头指向MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        // 第2步：指定源 录音权限6.0以上需要动态获取

         mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
          mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // 第3步：指定CamcorderProfile（需要API Level 8以上版本）
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        // 第4步：指定输出文件
        mMediaRecorder.setOutputFile(getVideoFile());
        // 第5步：指定预览输出
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
        mMediaRecorder.setVideoSize(640, 480); //设置录制视频尺寸     mWidth   mHeight
        mMediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024 );
        // 第6步：根据以上配置准备MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            //释放资源
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            //释放资源
            releaseMediaRecorder();
            return false;
        }
        return true;
    }
    private String getVideoFile(){
        File filePar = new File(Environment.getExternalStorageDirectory()+"/videoappVideo");
        //如果不存在这个文件夹就去创建
        if (!filePar.exists()){
            filePar.mkdirs();
        }
        return  Environment.getExternalStorageDirectory() + "/videoappVideo/"+ "videoapp_" + System.currentTimeMillis()+".mp4";
    }
    private void releaseMediaRecorder() {
        if (mMediaRecorder != null){
            mMediaRecorder.release();
        }
    }
    //检测录音权限
    private boolean checkRecordPermiss(){
        //如果没有拿到权限，申请权限
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) MainActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    4);
            return  false;
        }
        return  true;
    }
    private void startRecord(){
            // 初始化视频camera
            if (prepareRecord()) {
                // Camera已可用并解锁，MediaRecorder已就绪,
                // 现在可以开始录像
                mMediaRecorder.start();
                // 通知用户录像已开始
                btn_record.setText("停止录像");
                showToast("开始录像");
                isRecording = true;
            } else {
                // 准备未能完成，释放camera
                releaseMediaRecorder();
                // 通知用户
                showToast("录制失败");
            }
    }
    //停止录制
    private void stopRecord(){
        // 停止录像并释放camera
        isRecording = false;
        mMediaRecorder.stop(); // 停止录像
        releaseMediaRecorder(); // 释放MediaRecorder对象
        mCamera.lock();         // 将控制权从MediaRecorder 交回camera
        // 通知用户录像已停止
        btn_record.setText("开始录像");
        showToast("录像已停止");
    }
    private void showToast(String msg){
        Toast.makeText(MainActivity.this,msg,Toast.LENGTH_SHORT).show();
    }
    class CameraPreview extends SurfaceView implements  SurfaceHolder.Callback{

        private SurfaceHolder mHolder;
        private Camera mCamera;
        private final String TAG = "CameraPreview";
        private SurfaceHolder.Callback mCallback;

        public CameraPreview(Context context,Camera camera) {
            super(context);
            mCamera = camera;
            mHolder = getHolder();
            mCallback = this;
            mHolder.addCallback(mCallback);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            mHolder.setFormat(PixelFormat.TRANSPARENT);
            setZOrderOnTop(true);
            setZOrderMediaOverlay(true);
        }
        public SurfaceHolder.Callback getmCallback(){
            return  this.mCallback;
        }

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
                isPreview = true;       //开始预览
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            // 如果预览无法更改或旋转，注意此处的事件
            // 确保在缩放或重排时停止预览
            if (mHolder.getSurface() == null){
                // 预览surface不存在
                return;
            }
            // 更改时停止预览
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // 忽略：试图停止不存在的预览
            }
            // 在此进行缩放、旋转和重新组织格式
            // 以新的设置启动预览
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e){

                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            if(mCamera != null) {
                if (isPreview) {            //如果正在预览
                    mCamera.stopPreview();   //停止预览
                    mCamera.release();       //释放资源
                    isPreview = false;
                }
            }
        }
    }
}
