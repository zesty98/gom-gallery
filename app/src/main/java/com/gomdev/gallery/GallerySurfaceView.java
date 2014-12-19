package com.gomdev.gallery;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

/**
 * Created by gomdev on 14. 12. 18..
 */
public class GallerySurfaceView extends GLSurfaceView {
    public GallerySurfaceView(Context context) {
        super(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}
