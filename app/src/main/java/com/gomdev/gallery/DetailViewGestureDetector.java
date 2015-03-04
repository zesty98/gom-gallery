package com.gomdev.gallery;

import android.content.Context;
import android.graphics.RectF;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.gomdev.gles.GLESAnimator;

/**
 * Created by gomdev on 15. 2. 23..
 */
public class DetailViewGestureDetector {
    static final String CLASS = "DetailViewGestureDetector";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = true;//GalleryConfig.DEBUG;

    private final Context mContext;
    private final GridInfo mGridInfo;

    private GalleryContext mGalleryContext = null;
    private GestureDetectorCompat mGestureDetector = null;

    private DetailViewManager mDetailViewManager = null;
    private GallerySurfaceView mSurfaceView = null;

    private GLESAnimator mAnimator = null;

    private int mWidth = 0;
    private int mHeight = 0;

    private RectF mCurrentViewport = null;   // OpenGL ES coordinate

    private boolean mIsDown = false;
    private float mDownX = 0;
    private float mDragDistance = 0;


    DetailViewGestureDetector(Context context, GridInfo gridInfo) {
        if (DEBUG) {
            Log.d(TAG, "DetailViewGestureDetector()");
        }

        mContext = context;
        mGridInfo = gridInfo;

        setGridInfo(gridInfo);

        mGalleryContext = GalleryContext.getInstance();
        mGestureDetector = new GestureDetectorCompat(context, mGestureListener);
    }

    private void setGridInfo(GridInfo gridInfo) {
        if (DEBUG) {
            Log.d(TAG, "setGridInfo()");
        }
    }

    // rendering
    void update() {
        if (mAnimator.doAnimation() == true) {
            mSurfaceView.requestRender();
        }
    }


    // onSurfaceChanged
    void onSurfaceChanged(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged() width=" + width + " height=" + height);
        }

        mWidth = width;
        mHeight = height;

        if (mCurrentViewport == null) {
            float right = width * 0.5f;
            float left = -right;
            float top = height * 0.5f;
            float bottom = -top;

            // OpenGL ES coordiante.
            mCurrentViewport = new RectF(left, bottom, right, top);
        }
    }

    // onSurfaceCreated

    void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }
    }

    // callback, listener

    // touch
    boolean onTouchEvent(MotionEvent event) {
        if (DEBUG) {
            Log.d(TAG, "onTouchEvent()");
        }

        mSurfaceView.requestRender();

        final int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsDown = true;
                mDownX = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                mIsDown = false;
                if (Math.abs(mDragDistance) > mWidth * 0.5f) {
                    if (mDragDistance > 0) {
                        mAnimator.setValues(event.getX(), -mWidth);
                    } else {
                        mAnimator.setValues(event.getX(), mWidth);
                    }
                    mAnimator.setDuration(0L, 1000L);
                    mAnimator.start();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsDown == true) {
                    mDragDistance = mDownX - event.getX();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
        }

        return mGestureDetector.onTouchEvent(event);
    }

    // initialization

    // set / get

    void setSurfaceView(GallerySurfaceView surfaceview) {
        mSurfaceView = surfaceview;
    }

    void setDetailViewManager(DetailViewManager manager) {
        mDetailViewManager = manager;
    }

    private final GestureDetector.SimpleOnGestureListener mGestureListener
            = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    };
}
