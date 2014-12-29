package com.gomdev.gallery;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Created by gomdev on 14. 12. 29..
 */
public class GalleryScaleGestureDetector implements GridInfoChangeListener {
    static final String CLASS = "GalleryScaleGestureDetector";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;
    private final GallerySurfaceView mSurfaceView;

    private GridInfo mGridInfo = null;
    private ScaleGestureDetector mScaleGestureDetector = null;

    private int mNumOfColumns;
    private int mMinNumOfColumns;
    private int mMaxNumOfColumns;

    public GalleryScaleGestureDetector(Context context, GallerySurfaceView surfaceView) {
        mContext = context;
        mSurfaceView = surfaceView;

        mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);
        mSurfaceView.setGridInfoChangeListener(this);
    }

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mNumOfColumns = GalleryContext.getInstance().getNumOfColumns();
        mMinNumOfColumns = mNumOfColumns;
        mMaxNumOfColumns = 3 * mNumOfColumns;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return mScaleGestureDetector.onTouchEvent(event);
    }

    private final ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener
            = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private float mLastSpan;
        private int mDragDistance = 0;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            mLastSpan = scaleGestureDetector.getCurrentSpan();
            mDragDistance = mContext.getResources().getDimensionPixelSize(R.dimen.drag_distance);
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            float span = scaleGestureDetector.getCurrentSpan();

            float focusX = scaleGestureDetector.getFocusX();
            float focusY = scaleGestureDetector.getFocusY();

            float dragDistance = span - mLastSpan;
            float absDragDistance = Math.abs(dragDistance);
            if (absDragDistance > mDragDistance) {
                mLastSpan = span;

                int numOfColumns = mGridInfo.getNumOfColumns();
                if (dragDistance > 0) {
                    numOfColumns--;
                } else {
                    numOfColumns++;
                }

                numOfColumns = Math.max(numOfColumns, mMinNumOfColumns);
                numOfColumns = Math.min(numOfColumns, mMaxNumOfColumns);

                if (numOfColumns != mNumOfColumns) {
                    int imageIndex = mSurfaceView.getImageIndex(focusX, focusY);
                    mGridInfo.resize(numOfColumns);
                    mSurfaceView.resize(imageIndex);
                }
            }

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        }
    };

    @Override
    public void onSurfaceChanged(int width, int height) {

    }

    @Override
    public void onGridInfoChanged() {
        mNumOfColumns = GalleryContext.getInstance().getNumOfColumns();
    }
}
