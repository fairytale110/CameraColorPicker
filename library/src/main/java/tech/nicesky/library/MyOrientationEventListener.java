package tech.nicesky.library;

import android.content.Context;
import android.view.OrientationEventListener;

public class MyOrientationEventListener extends OrientationEventListener {

    private int mDeviceOrientation;

    public MyOrientationEventListener(Context context) {
        super(context);
    }

    @Override
    public void onOrientationChanged(int orientation) {
        // We keep the last known orientation. So if the user first orient
        // the camera then point the camera to floor or sky, we still have
        // the correct orientation.
        if (orientation != ORIENTATION_UNKNOWN) {
            mDeviceOrientation = normalize(orientation);
        }
    }

    public int getmDeviceOrientation() {
        return mDeviceOrientation;
    }

    private int normalize(int orientation) {
        if ((orientation > 315) || (orientation <= 45)) {
            return 0;
        }

        if (orientation > 45 && orientation <= 135) {
            return 90;
        }

        if (orientation <= 225) {
            return 180;
        }

        if (orientation > 225 && orientation <= 315) {
            return 270;
        }

        return  0;
    }
}
