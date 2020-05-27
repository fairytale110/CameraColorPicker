package tech.nicesky.library

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.Executors

/**
 * Created by fairytale110@foxmail.com at 2020/4/2 11:36
 *
 * Description：
 *
 */
@Suppress(
    "DEPRECATED_IDENTITY_EQUALS",
    "LongLogTag"
)
class Camera2Device {
    private var context: Context? = null
    private var manager: CameraManager? = null
    var cameraId: String = ""
    private var device: CameraDevice? = null
    private var photoSurface: Surface? = null
    private var imageReader: ImageReader? = null
    private var textureView: AutoFitTextureView? = null
    private var previewSurface: Surface? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var previewRequest: CaptureRequest? = null
    private var captureSession: CameraCaptureSession? = null

    private var captureReady = false
    private var surfaceTextureAvailable = false
    private val executorService = Executors.newSingleThreadExecutor()

    private var mHandlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private var sessionCallback: CameraCaptureSession.StateCallback =
        object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                // 如果摄像头为null，直接结束方法
                if (null == device) {
                    Log.e(
                        "Camera2Entity $cameraId",
                        "sessionCallback onConfigured()  device is null"
                    )
                    captureListener?.onFinish(false, cameraId, "")
                    return
                }
                Log.e("Camera2Entity $cameraId", "sessionCallback onConfigured() ")
                // 当摄像头已经准备好时，开始显示预览
                captureSession = cameraCaptureSession
                try {
                    // 设置自动对焦模式
                    previewRequestBuilder?.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                    // 设置自动曝光模式
                    previewRequestBuilder?.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                    )
                    // previewRequestBuilder!!.set(CaptureRequest.JPEG_ORIENTATION, 90)
                    // 开始显示相机预览
                    previewRequest = previewRequestBuilder?.build()
                    // 设置预览时连续捕获图像数据
                    previewRequest?.let {
                        captureSession?.let {
                            it.setRepeatingRequest(previewRequest!!, null, handler)
                            captureReady = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    captureListener?.onFinish(false, cameraId, "")
                }
            }

            override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                Log.e("Camera2Entity $cameraId", "sessionCallback onConfigureFailed() !!")
                // Toast.makeText(this@Photo2Activity, "配置失败！", Toast.LENGTH_SHORT).show()
                captureListener?.onFinish(false, cameraId, "")
            }
        }

    constructor(context: Context, cameraId: String, previewTextureView: AutoFitTextureView) {
        this.context = context
        this.cameraId = cameraId
        this.textureView = previewTextureView
        manager()
        mHandlerThread = HandlerThread("Camera2Device_$cameraId")
        mHandlerThread?.start();
        handler = Handler(mHandlerThread!!.getLooper()) {
            //Log.e("Camera2Entity $cameraId","handler message what is ${it.what}")
            if (it.what == 0) {
                if (device == null){
                    openCamera()
                }
                waitCaptureReady()
            } else if (it.what == 1) {
                takePicture()
            } else if (it.what == 2) {
                val succeed = it.arg1 == 1
                val path = it.obj as String
                releaseeCamera()
                handler?.postDelayed(Runnable {
                    mHandlerThread?.quitSafely()
                    mHandlerThread = null
                    handler = null
                    cameraOpened = false
                    captureListener?.onFinish(succeed, cameraId, path)
                }, 2)
            }
            false
        }
    }

    fun openCamera() {
        Log.e(
            "Camera2Entity $cameraId",
            "openCamera() id: $cameraId thread:${Thread.currentThread().name}"
        )
        setUpCameraOutputs()
        // 打开摄像头
        if (ActivityCompat.checkSelfPermission(context!!,Manifest.permission.CAMERA) !== PackageManager.PERMISSION_GRANTED) {
            Log.e("Camera2Entity $cameraId", " ==== NO CAMERA Permission ====")
            captureListener?.onFinish(false, cameraId, "")
            return
        }
        if (manager == null) {
            Log.e("Camera2Entity $cameraId", "openCamera --> manager is NULL !!!")
            captureListener?.onFinish(false, cameraId, "")
        }
        manager?.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Log.e("Camera2Entity $cameraId", " onOpened ${camera.id}")
                device = camera
                createCameraPreviewSession()
            }

            override fun onDisconnected(camera: CameraDevice) {
                Log.e("Camera2Entity $cameraId", " onDisconnected ${camera.id}")
                releaseeCamera()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.e("Camera2Entity $cameraId", " onError ${camera.id} code = $error")
                camera.close()
                releaseeCamera()
            }
        }, handler)
    }

    public fun shoot(captureListener: CaptureListener?) {
        Log.e("Camera2Entity $cameraId", "shoot==>")
        this.captureListener = captureListener
        // 打开摄像头
        handler?.sendEmptyMessage(0)
    }

    private fun waitCaptureReady() {
        executorService.submit(Runnable {
            var waitCount = 500
            while (waitCount > 0) {
                if (captureReady) {
                    break
                }
               // Log.e("Camera2Entity $cameraId", "wait captureReady 。。。")
                waitCount--
                sleep(10)
            }
            if (!captureReady) {
                Log.e("Camera2Entity $cameraId", "wait captureReady is NOT true")
                captureListener?.onFinish(false, cameraId, "")
                return@Runnable
            }
            sleep(100)
            // 拍一张
            handler?.sendEmptyMessage(1)
        })
    }

    private fun takePicture() {
        Log.e("Camera2Entity $cameraId", " takePicture() ==>")
        try {
            if (device == null) {
                Log.e("Camera2Entity $cameraId", " camera device is null !!!")
                captureListener?.onFinish(false, cameraId, "")
                return
            }

            // 创建作为拍照的CaptureRequest.Builder
            val captureRequestBuilder: CaptureRequest.Builder = device!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            // 将imageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(photoSurface!!)
            captureRequestBuilder.addTarget(previewSurface!!)
            // 设置自动对焦模式
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            // 设置自动曝光模式
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
            // 获取设备方向
//             val rotation: Int = (context as Activity).getWindowManager().getDefaultDisplay().getRotation()
            // 根据设备方向计算设置照片的方向
            //  captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90)
            // 停止连续取景
            captureSession?.stopRepeating()
            // 捕获静态图像
            SHOOT = true
            //captureSession.capture(captureRequestBuilder.build(), captureCallback, null);
            captureSession?.capture(captureRequestBuilder.build(), null, handler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            captureListener?.onFinish(false, cameraId, "")
        }
    }

    private fun manager() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager = context?.getSystemService(CameraManager::class.java)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            manager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager?
        } else {
            // TODO 使用Camera 旧版API
        }
        if (manager == null) {
            Log.e("Camera2Entity $cameraId", "manager is NULL ！！！")
            captureListener?.onFinish(false, cameraId, "")
        }
    }

    private fun createCameraPreviewSession() {
        Log.e(
            "Camera2Entity $cameraId",
            " createCameraPreviewSession() ==> thread is ${Thread.currentThread().name}"
        )
        previewSurface = Surface(textureView?.surfaceTexture)
        photoSurface = imageReader?.surface
        // 创建作为预览的CaptureRequest.Builder
        // 创建作为预览的CaptureRequest.Builder
        previewRequestBuilder = device?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        // 将textureView的surface作为CaptureRequest.Builder的目标
        // 将textureView的surface作为CaptureRequest.Builder的目标
        previewRequestBuilder?.addTarget(previewSurface!!)
        previewRequestBuilder?.addTarget(photoSurface!!)

        device?.createCaptureSession(
            mutableListOf(photoSurface, previewSurface),
            sessionCallback,
            handler
        )
    }

    private fun setUpCameraOutputs() {
        Log.e("Camera2Entity $cameraId", " setUpCameraOutputs() ==>")
        val characteristics = manager?.getCameraCharacteristics(cameraId);
        val streamConfigurationMap = characteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

//        val spSize = streamConfigurationMap!!.getOutputSizes(ImageFormat.JPEG).toList()
//        println("支持的size ==============================")
//        for (idx in 0 until spSize.size){
//            println("支持的size：w=${spSize[idx].width} h=${spSize[idx].height}")
//        }
        val largest = Collections.max(streamConfigurationMap!!.getOutputSizes(ImageFormat.JPEG).toList(), CompareSizesByArea());

        imageReader = ImageReader.newInstance(largest.width, largest.height, ImageFormat.JPEG, 6)
        imageReader?.setOnImageAvailableListener(photoReaderImgListener, null)
    }

    private var SHOOT = false
    private var frameIndex = 0

    var cameraOpened = false

    @SuppressLint("LongLogTag")
    private val photoReaderImgListener = ImageReader.OnImageAvailableListener { reader: ImageReader? ->
//            Log.e("Camera2Entity $cameraId", "onImageAvailable() --->")
            reader?.let {
                cameraOpened = true
//                Log.e("Camera2Entity $cameraId", " onImageAvailable() ==> != null ")
                // 保存图片，释放资源，继续下一个摄像头
                if (frameIndex < 1){
                    frameIndex++
                    it.acquireLatestImage()?.close()
                    return@let
                }
                   // println("frameIndex = $frameIndex")
                if (frameIndex > 1){
                    it.acquireLatestImage()?.close()
                    return@let
                }
                // 获取捕获的照片数据 ,如果模糊，image close，拿下一帧，自行调试
                   // println("SHOOT = $SHOOT")
                if (!SHOOT){
                    it.acquireLatestImage()?.close()
                    return@let
                }
               // executorService.submit {
                    println("executorService save")
                    SHOOT = false
                    var image = it.acquireLatestImage()
                    if (image == null) {
                        Log.e(
                            "Camera2Entity $cameraId",
                            " onImageAvailable() ==> acquireNextImage == null "
                        )
                        captureListener?.onFinish(false, cameraId, "")
                        return@let
                    }
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer[bytes]

                    val path: String = Util.getSaveBitmapPath(context!!)
                    // 使用IO流将照片写入指定文件
                    //File file = new File(getExternalFilesDir(null), "pic.jpg");
                    // 使用IO流将照片写入指定文件
                    //File file = new File(getExternalFilesDir(null), "pic.jpg");
                    val file = File(path)
                    var output: FileOutputStream? = null
                    var success = false
                    try {
                        output = FileOutputStream(file)
                        output.write(bytes)
                        output.flush()
                        Log.e("Camera2Entity $cameraId ", " onImageAvailable() 保存: 成功")
                        success = true
                        //captureListener?.onFinish(true, cameraId,  path)
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            e.printStackTrace()
                        }
                        //captureListener?.onFinish(false,  cameraId, "")
                    } finally {
                        frameIndex = 0
                        try {
                            output?.close()
                            output = null
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        image.close()
                        image = null

                        handler?.let {
                            val message = it.obtainMessage()
                            message.what = 2
                            message.obj = if (success) path else ""
                            message.arg1 = if (success) 1 else 0
                            it.sendMessage(message)
                        }

                    }
              //  }
            }?:run {
                captureListener?.onFinish(false, cameraId, "")
            }
        }

    fun releaseeCamera() {
        Log.e("Camera2Entity $cameraId", " releaseCamera() ==>")
        try {

            Util.releaseCameraSession(captureSession)
            Util.releaseCameraDevice(device)
            Util.releaseImageReader(imageReader)
//        textureView?.surfaceTextureListener = null
            previewRequestBuilder = null
            previewRequest = null
//        if (surfaceTexture != null) {
//            surfaceTexture!!.release()
//            surfaceTexture = null
//        }
            if (photoSurface != null) {
                photoSurface!!.release()
                photoSurface = null
            }
            imageReader = null
//            mHandlerThread?.quitSafely()
//            mHandlerThread = null
//            handler = null
            executorService.shutdownNow()
//        if (textureSurface != null) {
//            textureSurface!!.release()
//            textureSurface = null
//        }
//        val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        try {
//            windowManager.removeView(textureView)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
        }
    }

    private fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: Exception) {
        }
    }

    var captureListener: CaptureListener? = null

}