package tech.nicesky.library

import android.content.Context
import android.graphics.*
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.view.View.GONE
import android.view.View.VISIBLE
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_picker.*
import java.io.File

class PickerActivity : AppCompatActivity(),CaptureListener {

    private var cameraManager: CameraManager? = null
    private var camera2Device: Camera2DeviceWithYUVOnlyPreView? = null
    private var cId = "0"
    private var cameraOpened = false
    private var shooted = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picker)
        btn_exit.setOnClickListener { finish() }
        btn_shoot.setOnClickListener {
            btn_shoot.visibility = GONE
            if (!shooted){
                shooted = false
                camera2Device?.shoot(null)
            }else{
                closeCamera()
                reOpenCamera()
            }
        }

        img_switch_camera.setOnClickListener {
            img_switch_camera.visibility = GONE
            closeCamera()
            cId = if (cId.toInt() == 0) "1" else "0"
            reOpenCamera()
        }

        texture_preview.surfaceTextureListener = object : TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {

            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
                if (!cameraOpened){
                    cameraOpened = true
                }

                if (img_switch_camera.visibility == GONE){
                    img_switch_camera.visibility = VISIBLE
                }
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean = true

            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
                openCamera()
            }
        }
//        transformImage()
        init()
    }

    private fun updateColorvalue(color: String) {
        runOnUiThread {
            if (!color.isNullOrEmpty()){
                txt_color.text = "$color"
                img_color.setBackgroundColor(Color.parseColor(color))
            }
        }
    }

    private fun init(){
        showCamView()
        if (cameraManager == null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager = this.getSystemService(CameraManager::class.java)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager?
            }
        }
        cameraManager?.let {manager ->
            if (!manager.cameraIdList.isNullOrEmpty()){
                camera2Device = Camera2DeviceWithYUVOnlyPreView(this, cId, texture_preview)
                camera2Device?.captureListener = this@PickerActivity
            }
        }
    }

    private fun showCamView() {
        texture_preview.visibility = VISIBLE
        img_preview.visibility = GONE

    }

    private fun showImage(){
        texture_preview.visibility = GONE
        img_preview.visibility = VISIBLE
    }

    private fun reOpenCamera(){
        shooted = false
        init()
        openCamera()
    }

    private fun openCamera(){
        camera2Device?.let {device ->
            device.openCamera()
        }
    }

    private fun closeCamera(){
        camera2Device?.releaseeCamera()
        camera2Device = null
        // cameraOpened = false
    }

    override fun onStop() {
        cameraOpened = camera2Device?.cameraOpened ?: false
        if (cameraOpened){
            closeCamera()
        }
        super.onStop()
    }

    override fun onRestart() {
        super.onRestart()
        if (cameraOpened){
            reOpenCamera()
        }
    }

    override fun onDestroy() {
        closeCamera()
        cameraManager = null
        super.onDestroy()
    }

    override fun onFinish(success: Boolean, cameraId: String, imgPath: String) {
        println("camera $cameraId shoot finish $success, $imgPath")
        runOnUiThread {
            btn_shoot.visibility = VISIBLE
            shooted = true
            showImage()
            Glide.with(this).load(File(imgPath)).into(img_preview)
        }
    }

    override fun onFinish(success: Boolean, cameraId: String, temp: Bitmap) {
        println("camera $cameraId shoot finish $success")
        runOnUiThread {
            btn_shoot.visibility = VISIBLE
            shooted = true
            showImage()
            Glide.with(this).load(temp).into(img_preview)
        }
    }

    override fun onDetected(colorHexString: String, x: Int, y: Int) {
        updateColorvalue(colorHexString)
    }

    private fun transformImage() {
        if (texture_preview == null) {
            return
        } else try {
            val size = Point()
            getWindowManager().getDefaultDisplay().getSize(size)
            val width = img_preview.width
            val height = img_preview.height
            run {
                val matrix = Matrix()
                val rotation: Int = getWindowManager().getDefaultDisplay().getRotation()
                val textureRectF = RectF(0F, 0F, width.toFloat(), height.toFloat())
                val previewRectF =
                    RectF(
                        0F, 0F,
                        texture_preview.height.toFloat(),
                        texture_preview.width.toFloat()
                    )
                val centerX = textureRectF.centerX()
                val centerY = textureRectF.centerY()
                if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                    previewRectF.offset(
                        centerX - previewRectF.centerX(),
                        centerY - previewRectF.centerY()
                    )
                    matrix.setRectToRect(
                        textureRectF,
                        previewRectF,
                        Matrix.ScaleToFit.FILL
                    )
                    val scale = Math.max(
                        width.toFloat() / width,
                        height.toFloat() / width
                    )
                    matrix.postScale(scale, scale, centerX, centerY)
                    matrix.postRotate(90 * (rotation - 2).toFloat(), centerX, centerY)
                }
                texture_preview.setTransform(matrix)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}
