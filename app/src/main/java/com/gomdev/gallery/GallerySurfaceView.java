package com.gomdev.gallery;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESTransform;

/**
 * Created by gomdev on 14. 12. 18..
 */
public class GallerySurfaceView extends GLSurfaceView implements RendererListener {
    static final String CLASS = "GallerySurfaceView";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;

    private ImageListRenderer mRenderer = null;

    private GalleryGestureDetector mGalleryGestureDetector = null;
    private GalleryScaleGestureDetector mGalleryScaleGestureDetector = null;

    private GridInfo mGridInfo = null;

    public GallerySurfaceView(Context context) {
        super(context);

        mContext = context;

        init();
    }

    public GallerySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        init();
    }

    private void init() {
        mRenderer = new ImageListRenderer(mContext);
        mRenderer.setSurfaceView(this);

        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mGalleryGestureDetector = new GalleryGestureDetector(mContext, this);
        mGalleryScaleGestureDetector = new GalleryScaleGestureDetector(mContext, this);
    }

    @Override
    public void setRenderer(Renderer renderer) {
        super.setRenderer(renderer);

        mRenderer = (ImageListRenderer) renderer;
        mRenderer.setRendererListener(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (DEBUG) {
            Log.d(TAG, "surfaceChanged()");
        }

        super.surfaceChanged(holder, format, w, h);
        mGridInfo.setScreenSize(w, h);
        mGalleryGestureDetector.surfaceChanged(w, h);
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences pref = mContext.getSharedPreferences(GalleryConfig.PREF_NAME, 0);
        int currentImageIndex = pref.getInt(GalleryConfig.PREF_IMAGE_INDEX, 0);
        mGalleryGestureDetector.setCenterImageIndex(currentImageIndex);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean retVal = mGalleryScaleGestureDetector.onTouchEvent(event);
        retVal = mGalleryGestureDetector.onTouchEvent(event) || retVal;
        return retVal || super.onTouchEvent(event);
    }

    public void resize(float focusX, float focusY) {
        int imageIndex = getNearestIndex(focusX, focusY);
        mGalleryGestureDetector.setCenterImageIndex(imageIndex);
    }

    @Override
    public void update(final GLESNode node) {
        mGalleryGestureDetector.update();

        GLESTransform transform = node.getTransform();
        transform.setIdentity();

        float angle = mGalleryGestureDetector.getAngle();
        transform.rotate(angle, 1f, 0f, 0f);

        float translateZ = mGalleryGestureDetector.getTranslate();
        transform.translate(0f, 0f, translateZ);

        float scrollDistance = mGalleryGestureDetector.getScrollDistance();
        transform.preTranslate(0f, scrollDistance, 0f);
    }

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mGalleryGestureDetector.setGridInfo(gridInfo);
        mGalleryScaleGestureDetector.setGridInfo(gridInfo);
        mRenderer.setGridInfo(gridInfo);
    }

    public int getSelectedIndex(float x, float y) {
        int index = mRenderer.getSelectedIndex(x, y);

        return index;
    }

    public int getNearestIndex(float x, float y) {
        int index = mRenderer.getNearestIndex(x, y);

        return index;
    }
}
