package com.gomdev.gallery;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.OverScroller;

import com.gomdev.gallery.GalleryConfig.AlbumViewMode;
import com.gomdev.gles.GLESUtils;

/**
 * Created by gomdev on 14. 12. 29..
 */
class AlbumViewGestureDetector implements GridInfoChangeListener {
    static final String CLASS = "AlbumViewGestureDetector";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final static float MAX_ROTATION_ANGLE = 15f;
    private final static float MAX_TRANSLATE_Z_DP = -70f; // dp

    private final static float SCROLL_SCALE_UP_1X = 10f;
    private final static float SCROLL_SCALE_UP_3X = SCROLL_SCALE_UP_1X * 3f;

    private final Context mContext;
    private final GridInfo mGridInfo;

    private Handler mHandler = null;

    private GallerySurfaceView mSurfaceView = null;
    private GalleryContext mGalleryContext = null;
    private ImageListRenderer mRenderer = null;
    private AlbumViewManager mAlbumViewManager = null;

    private GestureDetectorCompat mGestureDetector;

    private OverScroller mScroller;
    private boolean mIsOnScrolling = false;
    private boolean mIsOnFling = false;

    private boolean m2ndPointerDown = false;
    private boolean m3rdPointerDown = false;

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

    private float mSurfaceBufferTop = 0f;
    private float mSurfaceBufferBottom = 0f;
    private float mSurfaceBufferLeft = 0f;

    private float mTranslateY = 0f;

    private float mMaxDistance = 0f;
    private float mRotationAngle = 0f;
    private float mMaxTranslateZ = 0f;
    private float mTranslateZ = 0f;

    private int mHeight = 0;

    AlbumViewGestureDetector(Context context, GridInfo gridInfo) {
        if (DEBUG) {
            Log.d(TAG, "AlbumViewGestureDetector()");
        }

        mContext = context;
        mGridInfo = gridInfo;

        setGridInfo(gridInfo);

        mGalleryContext = GalleryContext.getInstance();
        mGestureDetector = new GestureDetectorCompat(context, mGestureListener);

        mScroller = new OverScroller(context);

        mMaxTranslateZ = GLESUtils.getPixelFromDpi(mContext, MAX_TRANSLATE_Z_DP);
    }

    private void setGridInfo(GridInfo gridInfo) {
        mColumnWidth = mGridInfo.getColumnWidth();
        mNumOfColumns = mGridInfo.getNumOfColumns();
        mSpacing = mGridInfo.getSpacing();

        mActionBarHeight = gridInfo.getSystemBarHeight();
        mDateLabelHeight = gridInfo.getDateLabelHeight();
        int columnWidth = GalleryContext.getInstance().getDefaultColumnWidth();
        mMaxDistance = (columnWidth + mGridInfo.getSpacing()) * 10f;

        mNumOfColumns = gridInfo.getNumOfColumns();

        gridInfo.addListener(this);
    }

    void setSurfaceView(GallerySurfaceView surfaceView) {
        if (DEBUG) {
            Log.d(TAG, "setSurfaceView()");
        }

        mSurfaceView = surfaceView;
        mRenderer = surfaceView.getRenderer();
    }

    void setAlbumViewManager(AlbumViewManager manager) {
        mAlbumViewManager = manager;
    }

    void setHandler(Handler handler) {
        mHandler = handler;
    }

    void surfaceChanged(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "surfaceChanged() width=" + width + " height=" + height);
        }

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
            if (DEBUG) {
                Log.d(TAG, "surfaceChanged() create CurrentViewPort");
            }

            float right = width * 0.5f;
            float left = -right;
            float top = mSurfaceBufferTop;
            float bottom = mSurfaceBufferTop - height;

            // OpenGL ES coordiante.
            mCurrentViewport = new RectF(left, bottom, right, top);
        }
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
        if (DEBUG) {
            Log.d(TAG, "onNumOfImageInfosChanged()");
        }

        mScrollableHeight = mGridInfo.getScrollableHeight();

        mSurfaceSizeBuffer.y = mScrollableHeight;

        mSurfaceBufferTop = mHeight * 0.5f;
        mSurfaceBufferBottom = mSurfaceBufferTop - mScrollableHeight;
    }

    @Override
    public void onNumOfDateLabelInfosChanged() {
        if (DEBUG) {
            Log.d(TAG, "onNumOfDateLabelInfosChanged()");
        }

        mScrollableHeight = mGridInfo.getScrollableHeight();

        mSurfaceSizeBuffer.y = mScrollableHeight;

        mSurfaceBufferTop = mHeight * 0.5f;
        mSurfaceBufferBottom = mSurfaceBufferTop - mScrollableHeight;
    }

    private void adjustViewport() {
        ImageIndexingInfo indexingInfo = mGalleryContext.getImageIndexingInfo();
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

    void adjustViewport(float translateY) {

        float top = mCurrentViewport.bottom - translateY;

        setViewportBottomLeft(mSurfaceBufferLeft, top, true);
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
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mIsOnScrolling = true;

                break;
            case MotionEvent.ACTION_UP:
                mIsOnScrolling = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (m2ndPointerDown == false && event.getPointerCount() == 2) {
                    m2ndPointerDown = true;
                }

                if (m2ndPointerDown == true && event.getPointerCount() == 3) {
                    m3rdPointerDown = true;
                }

                mIsOnScrolling = false;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() == 3) {
                    m3rdPointerDown = false;
                } else if (event.getPointerCount() == 2) {
                    m2ndPointerDown = false;
                }

                mIsOnScrolling = false;
                break;
        }

        boolean result = mGestureDetector.onTouchEvent(event);

        return result;
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

            ImageIndexingInfo imageIndexingInfo = mAlbumViewManager.getSelectedImageIndex(x, y);

            AlbumViewMode albumViewMode = mGalleryContext.getAlbumViewMode();
            if (albumViewMode == AlbumViewMode.MULTI_SELECTION_MODE) {
                ImageObject imageObject = mAlbumViewManager.getImageObject(imageIndexingInfo);
                boolean isChecked = imageObject.isChecked();
                if (isChecked == true) {
                    imageObject.setCheck(false);
                    mGalleryContext.uncheckImageIndexingInfo(imageIndexingInfo);
                } else {
                    imageObject.setCheck(true);
                    mGalleryContext.checkImageIndexingInfo(imageIndexingInfo);
                }
            } else {
                if (imageIndexingInfo.mImageIndex == -1) {
                    return false;
                }

                invalidateViewport();

                mGalleryContext.setImageIndexingInfo(imageIndexingInfo);
                mGalleryContext.setCurrentViewport(mCurrentViewport);
                mRenderer.onImageSelected(imageIndexingInfo);
            }

            return true;
        }


        @Override
        public void onLongPress(MotionEvent e) {
            mGalleryContext.setAlbumViewMode(AlbumViewMode.MULTI_SELECTION_MODE);
            mHandler.sendEmptyMessage(ImageListActivity.INVALIDATE_OPTION_MENU);

            float x = e.getX();
            float y = e.getY();

            ImageIndexingInfo imageIndexingInfo = mAlbumViewManager.getSelectedImageIndex(x, y);
            ImageObject imageObject = mAlbumViewManager.getImageObject(imageIndexingInfo);
            boolean isChecked = imageObject.isChecked();
            if (isChecked == true) {
                imageObject.setCheck(false);
                mGalleryContext.uncheckImageIndexingInfo(imageIndexingInfo);
            } else {
                imageObject.setCheck(true);
                mGalleryContext.checkImageIndexingInfo(imageIndexingInfo);
            }

            mSurfaceView.requestRender();

            mIsOnScrolling = false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mIsOnScrolling = true;

            float viewportOffsetY = -distanceY;
            if (m2ndPointerDown == true && m3rdPointerDown == true) {
                viewportOffsetY *= SCROLL_SCALE_UP_3X;
            } else if (m2ndPointerDown == true) {
                viewportOffsetY *= SCROLL_SCALE_UP_1X;
            }
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
