package com.zhangchi.cameraphotograph;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.biometrics.BiometricManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity{

    //预览
    TextureView textureView;
    TextureView.SurfaceTextureListener surfaceTextureListener;
    CameraManager cameraManager;
    CameraDevice.StateCallback cam_stateCallback;
    CameraDevice mCameraDevice;
    Surface texture_surface;
    CameraCaptureSession.StateCallback cam_session_stateCallback;
    CameraCaptureSession.CaptureCallback still_capture_callback;
    CameraCaptureSession cameraCaptureSession;
    CaptureRequest.Builder requestBuilder;
    CaptureRequest request;
    Point screenSize;
    //拍照
    ImageView takephoto_imageView;  //显示拍照结果
    ImageButton takephoto_btn,change;   //触发拍照
    Surface imageReaderSurface;
    CaptureRequest.Builder requestBuilder_image_reader;
    CaptureRequest takephoto_request;
    ImageReader imageReader;    //ImageReader类允许应用程序直接访问呈现表面的图像数据
    Bitmap bitmap;
    //转换摄像头
    String cameraId = String.valueOf(CameraCharacteristics.LENS_FACING_FRONT);;//0代表前置摄像头，1代表后置摄像头

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 先准备一个监听器
        textureView=findViewById(R.id.texture_view_camera2);
        takephoto_btn=(ImageButton)findViewById(R.id.btn_camera2_takephoto);
        takephoto_imageView= findViewById(R.id.image_view_preview_image);
        change = findViewById(R.id.change);
//        change.setOnClickListener(this);
//        takephoto_btn.setOnClickListener(this);
        //TextureView监听器
        surfaceTextureListener=new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                texture_surface=new Surface(textureView.getSurfaceTexture());
                //设置预览画面比例
                //Android获取屏幕宽度和高度的方法
                screenSize=new Point();
//                Display display = getWindowManager().getDefaultDisplay();
//                display.getSize(screenSize);
                getWindowManager().getDefaultDisplay().getSize(screenSize);
                Log.d("MainActivity",  "识别宽高 "+screenSize.x +", " + screenSize.y);
                surface.setDefaultBufferSize(screenSize.y, screenSize.x);
                openCamera();
            }
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        };
        //绑定监听器
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        //B1. 准备工作：初始化ImageReader
        //创建图片读取器,参数为分辨率宽度和高度/图片格式/需要缓存几张图片,这里写的2意思是获取2张照片
        imageReader = ImageReader.newInstance(1920  ,1080, ImageFormat.JPEG,2);
        //B2. 准备工作：设置ImageReader收到图片后的回调函数
        //创建ImagerReader.OnImageAvailableListener方法的实例，并重写OnImageAvailable方法，
        // 该方法在ImageReader中的图像信息可用的时候执行
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                //image.acquireLatestImage();//从ImageReader的队列中获取最新的image,删除旧的
                //image.acquireNextImage();//从ImageReader的队列中获取下一个图像,如果返回null没有新图像可用

                //B2.1 接收图片：acquireLatestImage()从ImageReader中读取最近的一张，转成Bitmap
                Image image= reader.acquireLatestImage();

                try {

                    /**
                     * 文件存储1
                     */
//                    File path = new File(MainActivity.this.getExternalCacheDir().getPath() + "/AAA");
                    //获取路径方便
                    //app删除，app对应的图片不删除，保存路径是sd卡根路径
                    File path = new File(Environment.getExternalStorageDirectory() + "/DCIM/data");
                    //app删除对应的图片相应删除，保护隐私
                    //getExternalFilesDir(null)则为：/storage/emulated/0/Android/data/com.wintec.huashang/files
//                    File path = new File(getExternalFilesDir(null) + "/DCIM/data");
//                    File path = new File(getFilesDir()+ "/DCIM/data");
//                    Log.d("MainActivity", "getFilesDir路径：" + path);
                    if (!path.exists()) {
                        Log.d("MainActivity", "onImageAvailable: 路径不存在");
                        path.mkdirs();
                    } else {
                        Log.d("MainActivity", "onImageAvailable: 路径存在");
                    }
//                    SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
                    SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                    //下面两句一样
//                    Date date = new Date(System.currentTimeMillis());
                    Date date = new Date();
                    String format = formatter.format(date);
//                    System.out.println(formatter.format(date));

                    File file = new File(path, format+".jpg");
                    //先把图片读入到内存--然后写到 文件
                    FileOutputStream fileOutputStream = new FileOutputStream(file);

                    //这里的image.getPlanes()[0]其实是图层的意思,因为我的图片格式是JPEG只有一层所以是getPlanes()[0]
                    //如果你是其他格式(例如png)的图片会有多个图层,就可以获取指定图层的图像数据　
                    //ByteBuffer是一个缓冲区对象,同时包含了这些像素数据的配置信息。
                    ByteBuffer buffer= image.getPlanes()[0].getBuffer();
//                    Log.d("buffer", "image.getPlanes()[0]" + image.getPlanes()[0]); //android.media.ImageReader$SurfaceImage$SurfacePlane@54f3b06
//                    Log.d("buffer", "buffer" + buffer); //java.nio.DirectByteBuffer[pos=0 lim=476769 cap=476769]
    //                int length= buffer.remaining();
    //                byte[] bytes= new byte[length];
                    //返回剩余的可用长度，此长度为实际读取的数据长度，最大自然是底层数组的长度。
                    //定义一个字节数组,相当于缓存
                    byte[] bytes= new byte[buffer.remaining()];
//                    Log.d("buffer", "buffer.remaining()" + buffer.remaining()); //476769
//                    Log.d("buffer", "bytes" + bytes);   //[B@35c25c7
                    //将字节从该缓冲区ByteBuffer中传输到给定的目标阵列byte[].
                    buffer.get(bytes);
//                    Log.d("buffer", "buffer.remaining()" + buffer.remaining()); //0
//                    Log.d("buffer", "buffer.get(bytes)" + buffer.get(bytes)); //报错
//                    Log.d("buffer", "bytes" + bytes);   //[B@35c25c7

    //                bitmap = BitmapFactory.decodeByteArray(bytes,0,length);
                    //从指定的字节数组解码不可变位图，参数：
                    //data-压缩图像数据的字节数组
                    //偏移-偏移到imageData中， 解码器应开始解析的位置。
                    //长度-要分析的字节数，从偏移量开始返回：
                    //解码后的位图， 如果图像无法解码， 则为null，
                    bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);

                    //设置镜像
                    if ("1".equals(cameraId)){
                        Matrix matrix = new Matrix();
                        matrix.postScale(-1, 1);//利用matrix 对矩阵进行转换，y轴镜像     postScale是缩放
                        /*
                        source  –  产生子位图的源位图
                        x       - 源中第一个像素的 x 坐标
                        y       - 源中第一个像素的 y 坐标宽度
                        width   - 每行中的像素数高度
                        height  - 行数m
                        m       - 应用于像素的可选矩阵
                        filter  – 如果源图要被过滤滤源，则为 true。仅当矩阵包含的不仅仅是平移时才适用。
                         */
                        //返回一个不可变的源位图的位图的子集,改变了可选的矩阵。新的位图可能与源相同的对象,或可能是一个副本。
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);//matrix转换传入bitmap
                    }
                    //B2.2 显示图片
                    takephoto_imageView.setImageBitmap(bitmap);
                    image.close();

                    //ByteArrayOutputStream是对byte类型数据进行写入的类 相当于一个中间缓冲层，将类写入到文件等其他outputStream。它是对字节进行操作，属于内存操作流
                    ByteArrayOutputStream byteArray = new ByteArrayOutputStream();  //byte输出流   //下面再次把bitmap转换为byte数组
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArray);    //将位图（Bitmap）的压缩版本写入指定的输出流。
                    byte[] bytes1 = byteArray.toByteArray();//byteArray.toByteArray() 创建一个新分配的 byte 数组。其大小是此输出流的当前大小，并且缓冲区的有效内容已复制到该数组中。

                    /**
                     * 文件存储2
                     */
                    fileOutputStream.write(bytes1);
                    fileOutputStream.flush();
                    fileOutputStream.close();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        },null);
        //B3 配置：获取ImageReader的Surface
        imageReaderSurface = imageReader.getSurface();

        //B4. 相机点击事件
        takephoto_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //B4.1 配置request的参数 拍照模式(这行代码要调用已启动的相机 mCameraDevice，所以不能放在外面
                try {
                    requestBuilder_image_reader = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

                //后置旋转90，前置旋转270
                if ("0".equals(cameraId)){
                    //因为默认水平，所以要旋转90度
                    requestBuilder_image_reader.set(CaptureRequest.JPEG_ORIENTATION,90);
                }else if ("1".equals(cameraId)){
                    requestBuilder_image_reader.set(CaptureRequest.JPEG_ORIENTATION,270);
                }

                //开启自动对焦
                requestBuilder_image_reader.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                //开启闪光灯
                requestBuilder_image_reader.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                //B4.2 配置request的参数 的目标对象
                requestBuilder_image_reader.addTarget(imageReaderSurface );
                takephoto_request =  requestBuilder_image_reader.build();
                try {
                    //B4.3 触发拍照
                    //capture提交一个获取单张图片的捕捉请求，常用于拍照场景。
                    cameraCaptureSession.capture(takephoto_request,null,null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });

        //切换摄像头点击事件
        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

    }

    private void openCamera() {
        // 1 创建相机管理器，调用系统相机
        cameraManager= (CameraManager) getSystemService(Context.CAMERA_SERVICE);  // 初始化
        // 2 准备 相机状态回调对象为后面用
        cam_stateCallback=new CameraDevice.StateCallback() {
            /**
             * 相机打开时调用
             * @param camera
             */
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                // 2.1 保存已开启的相机对象
                mCameraDevice=camera;
                try {
                    // 2.2 构建请求对象（设置预览参数，和输出对象）
                    requestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);// 设置参数：预览
                    requestBuilder.addTarget(texture_surface);// 设置参数：目标容器(绑定Surface)
                    request = requestBuilder.build();
                    //2.3 创建会话的回调函数，后面用
                    cam_session_stateCallback=new CameraCaptureSession.StateCallback() {
                        //2.3.1  会话准备好了，在里面创建 预览或拍照请求
                        //摄像头完成配置，可以处理Capture请求了。
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            cameraCaptureSession=session;
                            try {
                                // 2.3.2 预览请求
                                //setRepeatingRequest不断的重复请求捕捉画面，常用于预览或者连拍场景。
                                cameraCaptureSession.setRepeatingRequest(request,null,null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        //摄像头配置失败
                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        }
                    };
                    // 2.3 创建会话
                    ////创建相机捕获会话，第一个参数是捕获数据的输出Surface列表，
                    // 第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，
                    // 第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行

                    //在预览的 Surface捕获图像的同时， 我们也需要 ImageReader来同时捕获图像数据
                    mCameraDevice.createCaptureSession( Arrays.asList(texture_surface,imageReaderSurface), cam_session_stateCallback,null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
            }
            /**
             * 发生异常时调用
             * 释放资源，关闭界面
             * @param camera
             * @param error
             */
            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
            }
        };
        // 4 检查相机权限
        checkPermission();
        // 5 开启相机（传入：要开启的相机ID，和状态回调对象）
        try {
//            for (String cameraId : cameraManager.getCameraIdList()){
//                //获取到每个相机的参数对象，包含前后摄像头，分辨率等
//                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
//                //摄像头的方向
//                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
//                if(facing==null){
//                    continue;
//                }
//                //匹配方向,指定打开后摄像头
//                if(facing!=CameraCharacteristics.LENS_FACING_BACK){
//                    continue;
//                }
//
//                Log.d("cameraId", cameraId);
//                Log.d("cameraId", "facing:" + String.valueOf(facing));
            Log.d("cameraId1", "openCamera_cameraId:" + cameraId);
                cameraManager.openCamera(cameraId,cam_stateCallback,null);
//                cameraManager.openCamera(cameraManager.getCameraIdList()[0],cam_stateCallback,null);
//            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     *  检查是否申请了权限
     */
    private void checkPermission() {

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CAMERA)){
                Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show();
            }else{
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        // 如果 textureView可用，就直接打开相机
        if(textureView.isAvailable()){
            openCamera();
        }else{
            // 否则，就开启它的可用时监听。
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }
//    @Override
//    protected void onPause() {
//        // 先把相机的session关掉
//        if(cameraCaptureSession!=null){
//            cameraCaptureSession.close();
//        }
//        // 再关闭相机
//        if(null!=mCameraDevice){
//            mCameraDevice.close();
//        }
//        // 最后关闭ImageReader
//        if(null!=imageReader){
//            imageReader.close();
//        }
//        // 最后交给父View去处理
//        super.onPause();
//    }

    public void switchCamera(){
        Log.d("switch", "test");

        //获取摄像头的管理者
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : cameraManager.getCameraIdList()) { //获取当前设备的相机设备列表 根据这个列表可以查询当前存在几个相机
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id); //查询出了相关设备的特征信息，特征信息都被封装在了 CameraCharacteristics 中
//                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//                previewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), textureView.getWidth(), textureView.getHeight());
                //匹配方向,指定打开后摄像头
                if (cameraId.equals(String.valueOf(CameraCharacteristics.LENS_FACING_BACK)) && characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    Log.d("change", "cameraId:" + cameraId);//1
                    Log.d("change", "String.valueOf(CameraCharacteristics.LENS_FACING_BACK)"+String.valueOf(CameraCharacteristics.LENS_FACING_BACK));//1
                    Log.d("change", "characteristics.get(CameraCharacteristics.LENS_FACING)"+characteristics.get(CameraCharacteristics.LENS_FACING));//0
                    cameraId = String.valueOf(CameraCharacteristics.LENS_FACING_FRONT);
                    mCameraDevice.close();
//                    backOrientation();
                    openCamera();
                    break;
                } else if (cameraId.equals(String.valueOf(CameraCharacteristics.LENS_FACING_FRONT)) && characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = String.valueOf(CameraCharacteristics.LENS_FACING_BACK);
                    mCameraDevice.close();
//                    frontOrientation();
                    openCamera();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}