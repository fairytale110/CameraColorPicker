package tech.nicesky.library

/**
 * Created by fairytale110@foxmail.com at 2020/5/11 16:06
 *
 * Description：
 *
 */
interface CaptureListener {
    fun onFinish(success: Boolean, cameraId: String, imgPath: String)
    fun onDetected(colorHexString: String, x: Int, y: Int)
}