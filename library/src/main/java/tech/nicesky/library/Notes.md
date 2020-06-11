
> 参考：
> https://stackoverflow.com/questions/30513147/detect-color-code-of-live-android-camera-preview
>
```
private void check(){
    int frameHeight1 = camera.getParameters().getPreviewSize().height;
    int frameWidth1 = camera.getParameters().getPreviewSize().width;
    int rgb1[] = new int[frameWidth * frameHeight];
    decodeYUV420SP(rgb1, data, frameWidth, frameHeight);
    Bitmap bmp1 = Bitmap.createBitmap(rgb, frameWidth1, frameHeight1, Config.ARGB_8888);
    int pixel = bmp1.getPixel( x,y );
    int redValue1 = Color.red(pixel);
    int blueValue1 = Color.blue(pixel);
    int greenValue1 = Color.green(pixel);
    int thiscolor1 = Color.rgb(redValue1, greenValue1, blueValue1);
}
```
