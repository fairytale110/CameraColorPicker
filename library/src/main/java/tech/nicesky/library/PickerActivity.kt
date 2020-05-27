package tech.nicesky.library

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.TextureView
import android.view.View.GONE
import android.view.View.VISIBLE
import kotlinx.android.synthetic.main.activity_picker.*

class PickerActivity : AppCompatActivity() {

    private var cameraManager: CameraManager? = null
    private var camera2Device: Camera2Device? = null
    private var cId = "0"
    private var cameraOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picker)
        btn_exit.setOnClickListener { finish() }
        btn_shoot.setOnClickListener {
            camera2Device?.shoot(object : CaptureListener{
                override fun onFinish(success: Boolean, cameraId: String, imgPath: String) {
                    println("camera $cameraId shoot finish $success, $imgPath")
                }
            })
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
        init()
    }

    private fun init(){
        if (cameraManager == null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager = this.getSystemService(CameraManager::class.java)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager?
            }
        }
        cameraManager?.let {manager ->
            if (!manager.cameraIdList.isNullOrEmpty()){
                camera2Device = Camera2Device(this, cId, texture_preview)
            }
        }
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

}
