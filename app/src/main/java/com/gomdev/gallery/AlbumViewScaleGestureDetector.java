package com.gomdev.gallery;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Created by gomdev on 14. 12. 29..
 */
class AlbumViewScaleGestureDetector implements GridInfoChangeListener {
    static final String CLASS = "AlbumViewScaleGestureDetector";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;
    private final GridInfo mGridInfo;

    private GallerySurfaceView mSurfaceView = null;
    private ImageListRenderer mRenderer = null;
    private AlbumViewManager mAlbumViewManager = null;

    private ScaleGestureDetector mScaleGestureDetector = null;

    private int mNumOfColumns;
    private int mMinNumOfColumns;
    private int mMaxNumOfColumns;

    private int mMinDragDistanceToScale = 0;
    private int mThresholdToRecognizingScale = 0;

    AlbumViewScaleGestureDetector(Context context, GridInfo gridInfo) {
        mContext = context;
        mGridInfo = gridInfo;

        setGridInfo(gridInfo);

        mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);

        mMinDragDistanceToScale = mContext.getResources().getDimensionPixelSize(R.dimen.min_drag_distance);
        mThresholdToRecognizingScale = mMinDragDistanceToScale / 4;
    }

    private void setGridInfo(GridInfo gridInfo) {
        mNumOfColumns = gridInfo.getNumOfColumns();
        mMinNumOfColumns = gridInfo.getMinNumOfColumns();
        mMaxNumOfColumns = gridInfo.getMaxNumOfColumns();

        mGridInfo.addListener(this);
    }

    void setSurfaceView(GallerySurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    void setAlbumViewManager(AlbumViewManager manager) {
        mAlbumViewManager = manager;
    }

    boolean onTouchEvent(MotionEvent event) {
        return mScaleGestureDetector.onTouchEvent(event);
    }

    @Override
    public void onColumnWidthChanged() {
        if (DEBUG) {
            Log.d(TAG, "onColumnWidthChanged()");
        }

        mNumOfColumns = mGridInfo.getNumOfColumns();
    }

    @Override
    public void onImageDeleted() {
    }

    @Override
    public void onDateLabelDeleted() {
    }

    private final ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener
            = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private float mLastSpan;


        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            mLastSpan = scaleGestureDetector.getCurrentSpan();

            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {


            float span = scaleGestureDetector.getCurrentSpan();

            float focusX = scaleGestureDetector.getFocusX();
            float focusY = scaleGestureDetector.getFocusY();

            float dragDistance = span - mLastSpan;
            float absDragDistance = Math.abs(dragDistance);

            if (absDragDistance > mThresholdToRecognizingScale) {
                mAlbumViewManager.onScaleBegin();
            }

            if (absDragDistance > mMinDragDistanceToScale) {


                mLastSpan = span;

                int numOfColumns = mNumOfColumns;
                if (dragDistance > 0) {
                    numOfColumns--;
                } else {
                    numOfColumns++;
                }

                numOfColumns = Math.max(numOfColumns, mMinNumOfColumns);
                numOfColumns = Math.min(numOfColumns, mMaxNumOfColumns);

                if (numOfColumns != mNumOfColumns) {
                    mAlbumViewManager.resize(focusX, focusY);
                    mGridInfo.resize(numOfColumns);
                }
            }

            if (absDragDistance > mThresholdToRecognizingScale) {
                mAlbumViewManager.onScaleEnd();
            }

            mSurfaceView.requestRender();

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
//            mAlbumViewManager.onScaleEnd();
        }
    };
}
