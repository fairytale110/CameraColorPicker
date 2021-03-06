package tech.nicesky.library

import android.graphics.Bitmap
import android.graphics.Color
import java.util.*


object Detector {

    fun color(bitmap: Bitmap?, x: Float, y: Float): kotlin.String {
        println("Detector color x=$x y=$y")
        bitmap?.let {
            val pixel: Int = bitmap.getPixel(x.toInt(), y.toInt())
            val redValue1: Int = Color.red(pixel)
            val blueValue1: Int = Color.blue(pixel)
            val greenValue1: Int = Color.green(pixel)
            // val colorInt: Int = Color.rgb(redValue1, greenValue1, blueValue1)
            val hex = String.format("#%02x%02x%02x",redValue1, greenValue1, blueValue1).toUpperCase(Locale.CHINESE)
            println("Detector color = $hex")
            return hex
        }?: kotlin.run {
            println("Detector color = NULL !")
            return ""
        }
    }

//    palleteView.setOnTouchListener(new View.OnTouchListener() {
//
//        @Override
//        public boolean onTouch(View v, MotionEvent ev) {
//            // TODO Auto-generated method stub
//            ImageView img = (ImageView) v;
//
//            final int evX = (int) ev.getX();
//            final int evY = (int) ev.getY();
//
//            img.setDrawingCacheEnabled(true);
//            Bitmap imgbmp = Bitmap.createBitmap(img.getDrawingCache());
//            img.setDrawingCacheEnabled(false);
//
//            try {
//                int pxl = imgbmp.getPixel(evX, evY);
//
//                pickedColorView.setBackgroundColor(pxl);
//
//            }catch (Exception ignore){
//            }
//            imgbmp.recycle();
//
//            return true;
//        }
//    });
}