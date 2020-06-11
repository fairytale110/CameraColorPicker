package tech.nicesky.library

import android.content.Context
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.TextureView
import android.view.View.GONE
import android.view.View.VISIBLE
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_picker.*
import java.io.File

class PickerActivity : AppCompatActivity(),CaptureListener {

    private var cameraManager: CameraManager? = null
    private var camera2Device: Camera2DeviceWithYUV? = null
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
            shooted = false
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
                camera2Device = Camera2DeviceWithYUV(this, cId, texture_preview)
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

    override fun onDetected(colorHexString: String, x: Int, y: Int) {
        updateColorvalue(colorHexString)
    }

}
