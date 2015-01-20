package com.gomdev.gallery;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
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
public class GallerySurfaceView extends GLSurfaceView {
    static final String CLASS = "GallerySurfaceView";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;

    private ImageListRenderer mRenderer = null;

    private GalleryGestureDetector mGalleryGestureDetector = null;
    private GalleryScaleGestureDetector mGalleryScaleGestureDetector = null;

    private GridInfo mGridInfo = null;
    private int mSpacing = 0;
    private int mDefaultColumnWidth = 0;

    private GalleryObject mCenterObject = null;
    private GalleryObject mLastObject = null;
    private float mFocusY = 0f;
    private boolean mIsOnAnimation = false;
    private boolean mIsOnScale = false;

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
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mGalleryGestureDetector = new GalleryGestureDetector(mContext, this);
        mGalleryScaleGestureDetector = new GalleryScaleGestureDetector(mContext, this);
    }

    @Override
    public void setRenderer(Renderer renderer) {
        super.setRenderer(renderer);

        mRenderer = (ImageListRenderer) renderer;
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
        if (mIsOnScale == false && mIsOnAnimation == false) {
            retVal = mGalleryGestureDetector.onTouchEvent(event) || retVal;
        }
        return retVal || super.onTouchEvent(event);
    }


    public void resize(float focusX, float focusY) {
        mFocusY = focusY;
        int imageIndex = getNearestIndex(focusX, focusY);
        mCenterObject = mRenderer.getImageObject(imageIndex);
        mLastObject = mRenderer.getImageObject(mGridInfo.getBucketInfo().getNumOfImages() - 1);
        mGalleryGestureDetector.setCenterImageIndex(imageIndex);
    }

    public void update() {
        if (mCenterObject != null && mIsOnAnimation == true) {
            float columnWidth = mDefaultColumnWidth * mGridInfo.getScale();
            float bottom = mLastObject.getTop() - columnWidth + mSpacing;
            float pos = mCenterObject.getTop() + mFocusY - columnWidth * 0.5f;
            mGalleryGestureDetector.adjustViewport(pos, bottom);
        }

        mGalleryGestureDetector.update();
    }

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mSpacing = gridInfo.getSpacing();
        mDefaultColumnWidth = gridInfo.getDefaultColumnWidth();

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

    public void onAnimationStarted() {
        mIsOnAnimation = true;
    }

    public void onAnimationFinished() {
        mIsOnAnimation = false;
    }

    public void onAnimationCanceled() {
        mIsOnAnimation = false;
    }

    public void onScaleBegin() {
        mIsOnScale = true;
    }

    public void onScaleEnd() {
        mIsOnScale = false;
    }
}
