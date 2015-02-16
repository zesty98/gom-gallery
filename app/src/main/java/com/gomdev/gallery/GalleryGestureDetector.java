package com.gomdev.gallery;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.OverScroller;

import com.gomdev.gles.GLESUtils;

/**
 * Created by gomdev on 14. 12. 29..
 */
class GalleryGestureDetector implements GridInfoChangeListener {
    static final String CLASS = "GalleryGestureDetector";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final static float MAX_ROTATION_ANGLE = 15f;
    private final static float MAX_TRANSLATE_Z_DP = -70f; // dp

    private final Context mContext;
    private final GridInfo mGridInfo;

    private GallerySurfaceView mSurfaceView = null;
    private ImageListRenderer mRenderer = null;


    private GestureDetectorCompat mGestureDetector;

    private OverScroller mScroller;
    private boolean mIsOnScrolling = false;
    private boolean mIsOnFling = false;

    private RectF mCurrentViewport = null;   // OpenGL ES coordinate
    private RectF mScrollerStartViewport = new RectF();
    private Rect mContentRect = new Rect(); // screen coordinate
    private Point mSurfaceSizeBuffer = new Point();

    private int mScrollableHeight = 0;
    private int mActionBarHeight = 0;
    private int mDateLabelHeight = 0;
    private int mColumnWidth = 0;
    private int mSpacing = 0;
    private int mNumOfColumns = 0;
    private int mMinNumOfColumns;
    private int mMaxNumOfColumns;

    private float mSurfaceBufferTop = 0f;
    private float mSurfaceBufferBottom = 0f;
    private float mSurfaceBufferLeft = 0f;

    private float mTranslateY = 0f;

    private float mMaxDistance = 0f;
    private float mRotationAngle = 0f;
    private float mMaxTranslateZ = 0f;
    private float mTranslateZ = 0f;

    private int mHeight = 0;

    GalleryGestureDetector(Context context, GridInfo gridInfo) {
        mContext = context;
        mGridInfo = gridInfo;

        setGridInfo(gridInfo);

        mGestureDetector = new GestureDetectorCompat(context, mGestureListener);

        mScroller = new OverScroller(context);

        mMaxTranslateZ = GLESUtils.getPixelFromDpi(mContext, MAX_TRANSLATE_Z_DP);
    }

    private void setGridInfo(GridInfo gridInfo) {
        mColumnWidth = mGridInfo.getColumnWidth();
        mNumOfColumns = mGridInfo.getNumOfColumns();
        mSpacing = mGridInfo.getSpacing();

        mActionBarHeight = gridInfo.getActionBarHeight();
        mDateLabelHeight = gridInfo.getDateLabelHeight();
        int columnWidth = GalleryContext.getInstance().getColumnWidth();
        mMaxDistance = (columnWidth + mGridInfo.getSpacing()) * 10f;

        mNumOfColumns = gridInfo.getNumOfColumns();
        mMinNumOfColumns = gridInfo.getMinNumOfColumns();
        mMaxNumOfColumns = gridInfo.getMaxNumOfColumns();

        gridInfo.addListener(this);
    }

    void setSurfaceView(GallerySurfaceView surfaceView) {
        mSurfaceView = surfaceView;
        mRenderer = surfaceView.getRenderer();
    }

    void surfaceChanged(int width, int height) {
        mHeight = height;

        mContentRect.set(0, 0, width, height);

        mScrollableHeight = mGridInfo.getScrollableHeight();

        mSurfaceSizeBuffer.x = width;
        mSurfaceSizeBuffer.y = mScrollableHeight;

        float surfaceBufferRight = width * 0.5f;
        mSurfaceBufferLeft = -surfaceBufferRight;
        mSurfaceBufferTop = height * 0.5f;
        mSurfaceBufferBottom = mSurfaceBufferTop - mScrollableHeight;

        if (mCurrentViewport == null) {
            float right = width * 0.5f;
            float left = -right;
            float top = mSurfaceBufferTop;
            float bottom = mSurfaceBufferTop - height;

            // OpenGL ES coordiante.
            mCurrentViewport = new RectF(left, bottom, right, top);
        }

        adjustViewport();
    }

    RectF getCurrentViewport() {
        return mCurrentViewport;
    }

    @Override
    public void onColumnWidthChanged() {
        if (DEBUG) {
            Log.d(TAG, "onColumnWidthChanged()");
        }

        mColumnWidth = mGridInfo.getColumnWidth();
        mNumOfColumns = mGridInfo.getNumOfColumns();
    }

    @Override
    public void onNumOfImageInfosChanged() {
        mScrollableHeight = mGridInfo.getScrollableHeight();

        mSurfaceSizeBuffer.y = mScrollableHeight;

        mSurfaceBufferTop = mHeight * 0.5f;
        mSurfaceBufferBottom = mSurfaceBufferTop - mScrollableHeight;
    }

    @Override
    public void onNumOfDateLabelInfosChanged() {
        mScrollableHeight = mGridInfo.getScrollableHeight();

        mSurfaceSizeBuffer.y = mScrollableHeight;

        mSurfaceBufferTop = mHeight * 0.5f;
        mSurfaceBufferBottom = mSurfaceBufferTop - mScrollableHeight;
    }

    private void adjustViewport() {
        ImageIndexingInfo indexingInfo = mGridInfo.getImageIndexingInfo();
        float distFromTop = getDistanceOfSelectedImage(indexingInfo) - mHeight * 0.5f;

        float left = mSurfaceBufferLeft;
        float top = mSurfaceBufferTop - distFromTop;

        setViewportBottomLeft(left, top, true);
    }

    void adjustViewport(float top, float bottom) {
        float left = mSurfaceBufferLeft;

        mSurfaceBufferBottom = bottom;
        mScrollableHeight = (int) (mHeight * 0.5f - bottom);
        mSurfaceSizeBuffer.y = mScrollableHeight;

        setViewportBottomLeft(left, top, true);
    }

    float getTranslateY(float top, float bottom) {
        int nextScrollableHeight = (int) (mHeight * 0.5f - bottom);

        float curHeight = mCurrentViewport.height();

        if (nextScrollableHeight < curHeight) {
            top = mSurfaceBufferTop;
        } else {
            top = Math.max(bottom + curHeight, Math.min(top, mSurfaceBufferTop));
        }

        float translateY = (mSurfaceBufferTop - top);

        return translateY;
    }

    private float getDistanceOfSelectedImage(ImageIndexingInfo imageIndexingInfo) {
        float dist = mActionBarHeight;
        BucketInfo bucketInfo = mGridInfo.getBucketInfo();
        int numOfDateLabels = mGridInfo.getNumOfDateInfos();
        for (int i = 0; i < imageIndexingInfo.mDateLabelIndex; i++) {
            DateLabelInfo dateLabelInfo = bucketInfo.get(i);

            dist += mDateLabelHeight;

            int numOfRowsInDateLabel = dateLabelInfo.getNumOfRows();
            dist += ((mColumnWidth + mSpacing) * numOfRowsInDateLabel);
        }

        dist += mDateLabelHeight;

        int row = imageIndexingInfo.mImageIndex / mNumOfColumns;
        dist += ((mColumnWidth + mSpacing) * row);

        return dist;
    }

    boolean onTouchEvent(MotionEvent event) {
        mSurfaceView.requestRender();

        final int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsOnScrolling = true;
                break;
            case MotionEvent.ACTION_UP:
                mIsOnScrolling = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mIsOnScrolling = false;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mIsOnScrolling = false;
                break;
        }

        return mGestureDetector.onTouchEvent(event);
    }

    void update() {
        float currVelocity = mScroller.getCurrVelocity();
        if (currVelocity > 0f) {
            computeScroll();
        }

        calcRotationAngle();

        mGridInfo.setRotateX(mRotationAngle);
        mGridInfo.setTranslateZ(mTranslateZ);
    }

    private void calcRotationAngle() {
        if (mScroller.isFinished() == true) {
            mRotationAngle = 0f;
            mTranslateZ = 0f;

            if (mIsOnFling == true) {
                mIsOnFling = false;
            }
            return;
        }

        float startY = mScroller.getStartY();
        float finalY = mScroller.getFinalY();

        float scrollDistance = finalY - startY;
        scrollDistance = Math.abs(scrollDistance);

        if (scrollDistance < mMaxDistance) {
            mRotationAngle = 0f;
            mTranslateZ = 0f;

            return;
        }

        float distanceToFinalY = finalY - mTranslateY;
        distanceToFinalY = Math.abs(distanceToFinalY);
        if (distanceToFinalY < mMaxDistance) {
            mRotationAngle = distanceToFinalY * MAX_ROTATION_ANGLE / mMaxDistance;
            mTranslateZ = distanceToFinalY * mMaxTranslateZ / mMaxDistance;
        } else {
            mRotationAngle = MAX_ROTATION_ANGLE;
            mTranslateZ = mMaxTranslateZ;
        }

        if (startY > finalY) {
            mRotationAngle = -mRotationAngle;
        }
    }

    void resetOnScrolling() {
        mIsOnScrolling = false;
    }

    boolean isOnScrolling() {
        return (mIsOnScrolling || mIsOnFling);
    }

    private final GestureDetector.SimpleOnGestureListener mGestureListener
            = new GestureDetector.SimpleOnGestureListener() {

        private boolean mToScaleDown = true;

        @Override
        public boolean onDown(MotionEvent e) {
            mScrollerStartViewport.set(mCurrentViewport);
            mScroller.forceFinished(true);
            invalidateViewport();

            return true;
        }


        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();

            ImageIndexingInfo imageIndexingInfo = mRenderer.getSelectedImageIndex(x, y);
            if (imageIndexingInfo.mImageIndex == -1) {
                return false;
            }

            Intent intent = new Intent(mContext, com.gomdev.gallery.ImageViewActivity.class);

            intent.putExtra(GalleryConfig.BUCKET_INDEX, imageIndexingInfo.mBucketIndex);
            intent.putExtra(GalleryConfig.DATE_LABEL_INDEX, imageIndexingInfo.mDateLabelIndex);
            intent.putExtra(GalleryConfig.IMAGE_INDEX, imageIndexingInfo.mImageIndex);

            SharedPreferences pref = mContext.getSharedPreferences(GalleryConfig.PREF_NAME, 0);
            SharedPreferences.Editor editor = pref.edit();

            editor.putInt(GalleryConfig.PREF_BUCKET_INDEX, imageIndexingInfo.mBucketIndex);
            editor.putInt(GalleryConfig.PREF_DATE_LABEL_INDEX, imageIndexingInfo.mDateLabelIndex);
            editor.putInt(GalleryConfig.PREF_IMAGE_INDEX, imageIndexingInfo.mImageIndex);

            editor.commit();

            mGridInfo.setImageIndexingInfo(imageIndexingInfo);

            mRenderer.cancelLoading();

            Log.d(TAG, "onSingleTapUp() imageIndexingInfo " + imageIndexingInfo);
//            mRenderer.onImageSelected(imageIndexingInfo);

            mContext.startActivity(intent);

            return true;
        }


        @Override
        public void onLongPress(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();

            int numOfColumns = mNumOfColumns;
            if (mToScaleDown == true) {
                numOfColumns++;
            } else {
                numOfColumns--;
            }

            if (numOfColumns > mMaxNumOfColumns) {
                numOfColumns = mMaxNumOfColumns - 1;
                mToScaleDown = false;
            }

            if (numOfColumns < mMinNumOfColumns) {
                numOfColumns = mMinNumOfColumns + 1;
                mToScaleDown = true;
            }

            if (numOfColumns != mNumOfColumns) {
                synchronized (GalleryContext.sLockObject) {
                    mRenderer.resize(x, y);
                    mGridInfo.resize(numOfColumns);
                }
            }

            mSurfaceView.requestRender();

            mIsOnScrolling = false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mIsOnScrolling = true;

            float viewportOffsetY = -distanceY;
            setViewportBottomLeft(mCurrentViewport.left,
                    (mCurrentViewport.bottom + viewportOffsetY), true);


            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            mIsOnFling = true;

            fling((int) -velocityX, (int) -velocityY);

            return true;
        }
    };

    private void fling(int velocityX, int velocityY) {
        // Flings use math in pixels (as opposed to math based on the viewport).
        mScrollerStartViewport.set(mCurrentViewport);
        int startX = 0;
        int startY = (int) (mSurfaceBufferTop - mScrollerStartViewport.bottom);
        mScroller.forceFinished(true);
        mScroller.fling(
                startX,
                startY,
                velocityX,
                velocityY,
                0, 0,
                0, mSurfaceSizeBuffer.y - mContentRect.height(),
                0,
                0);
        invalidateViewport();
    }

    void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int currY = mScroller.getCurrY();

            float currYRange = (mSurfaceBufferTop) - currY;
            setViewportBottomLeft(mSurfaceBufferLeft, currYRange, true);
        }
    }

    private void setViewportBottomLeft(float leftX, float topY, boolean checkBound) {
        float curWidth = mCurrentViewport.width();
        float curHeight = mCurrentViewport.height();

        if (checkBound == true) {
            if (mSurfaceSizeBuffer.y < curHeight) {
                topY = mSurfaceBufferTop;
            } else {
                topY = Math.max(mSurfaceBufferBottom + curHeight, Math.min(topY, mSurfaceBufferTop));
            }
        }

        mCurrentViewport.set(leftX, topY - curHeight, leftX + curWidth, topY);

        mTranslateY = (mSurfaceBufferTop - mCurrentViewport.bottom);

        mGridInfo.setTranslateY(mTranslateY);

        mSurfaceView.requestRender();
    }

    private void invalidateViewport() {
        computeScroll();
    }
}
