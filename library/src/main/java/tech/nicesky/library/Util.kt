package tech.nicesky.library

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.media.ImageReader
import android.os.Environment
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by fairytale110@foxmail.com at 2020/5/27 9:00
 *
 * Description：
 *
 */
class Util {
    companion object{
        fun getSaveBitmapPath(context: Context): String {
            return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                var path =
                    Environment.getExternalStorageDirectory().path + "/tech.nicesky.camera2colorpicker/Temp/"
                val dirFile = File(path)
                if (!dirFile.exists()) {
                    dirFile.mkdirs()
                }
                /*if(dirFile.exists()){
                        File files[]=dirFile.listFiles();
                        //超过10个后删除
                        if(files.length>=150){
                            for (int i=0;i<files.length;i++){
                                files[i].delete();
                            }
                        }
                    }*/
                val time =
                    SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINESE).format(Date()) + ".jpg"
                val file = File(path, time)
                if (!file.exists()) {
                    try {
                        file.createNewFile()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                path = path + time
                path
            } else {
                context.filesDir.path
            }
        }


        fun releaseImageReader(reader: ImageReader?) {
            var reader = reader
            if (reader != null) {
                reader.close()
                reader = null
            }
        }

        fun releaseCameraSession(session: CameraCaptureSession?) {
            var session = session
            if (session != null) {
                session.close()
                session = null
            }
        }

        fun releaseCameraDevice(cameraDevice: CameraDevice?) {
            var cameraDevice = cameraDevice
            if (cameraDevice != null) {
                cameraDevice.close()
                cameraDevice = null
            }
        }
    }
}