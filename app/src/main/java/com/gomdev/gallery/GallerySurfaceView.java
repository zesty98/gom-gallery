package com.gomdev.gallery;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by gomdev on 14. 12. 18..
 */
class GallerySurfaceView extends GLSurfaceView {
    static final String CLASS = "GallerySurfaceView";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;

    private ImageListRenderer mRenderer = null;

    GallerySurfaceView(Context context) {
        super(context);

        mContext = context;

        init();
    }

    GallerySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        init();
    }

    private void init() {
        mRenderer = new ImageListRenderer(mContext);
        mRenderer.setSurfaceView(this);

        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void setRenderer(Renderer renderer) {
        super.setRenderer(renderer);

        mRenderer = (ImageListRenderer) renderer;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mRenderer != null) {
            mRenderer.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mRenderer != null) {
            mRenderer.onPause();
        }

        super.onPause();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean retVal = false;

        if (mRenderer != null) {
            retVal = mRenderer.onTouchEvent(event);
        }

        return retVal || super.onTouchEvent(event);
    }

    void setGridInfo(GridInfo gridInfo) {
        mRenderer.setGridInfo(gridInfo);
    }
}
