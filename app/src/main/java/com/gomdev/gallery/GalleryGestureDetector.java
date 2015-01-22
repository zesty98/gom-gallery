package com.gomdev.gallery;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.OverScroller;

import com.gomdev.gles.GLESUtils;

/**
 * Created by gomdev on 14. 12. 29..
 */
public class GalleryGestureDetector implements GridInfoChangeListener {
    static final String CLASS = "GalleryGestureDetector";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final static float MAX_ROTATION_ANGLE = 15f;
    private final static float MAX_TRANSLATE_Z_DP = -70f; // dp

    private final Context mContext;
    private final ImageListRenderer mRenderer;

    private GallerySurfaceView mSurfaceView = null;

    private ImageManager mImageManager = null;
    private GridInfo mGridInfo = null;

    private GestureDetectorCompat mGestureDetector;

    private OverScroller mScroller;

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

    private EdgeEffectCompat mEdgeEffectTop;
    private EdgeEffectCompat mEdgeEffectBottom;

    private boolean mEdgeEffectTopActive;
    private boolean mEdgeEffectBottomActive;

    private float mTranslateY = 0f;
    private int mCenterImageIndex = 0;

    private float mMaxDistance = 0f;
    private float mRotationAngle = 0f;
    private float mMaxTranslateZ = 0f;
    private float mTranslateZ = 0f;

    private int mWidth = 0;
    private int mHeight = 0;

    public GalleryGestureDetector(Context context, ImageListRenderer renderer) {
        mContext = context;
        mRenderer = renderer;

        mImageManager = ImageManager.getInstance();

        mGestureDetector = new GestureDetectorCompat(context, mGestureListener);

        mScroller = new OverScroller(context);

        mEdgeEffectTop = new EdgeEffectCompat(context);
        mEdgeEffectBottom = new EdgeEffectCompat(context);

        mMaxTranslateZ = GLESUtils.getPixelFromDpi(mContext, MAX_TRANSLATE_Z_DP);
    }

    public void setSurfaceView(GallerySurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

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

    public void surfaceChanged(int width, int height) {
        mWidth = width;
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

    @Override
    public void onColumnWidthChanged() {
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
        float distFromTop = getDistanceOfSelectedImage(mCenterImageIndex) - mHeight * 0.5f;

        float left = mSurfaceBufferLeft;
        float top = mSurfaceBufferTop - distFromTop;

        setViewportBottomLeft(left, top, true);
    }

    public void adjustViewport(float top, float bottom) {
        float left = mSurfaceBufferLeft;

        mSurfaceBufferBottom = bottom;
        mScrollableHeight = (int) (mHeight * 0.5f - bottom);
        mSurfaceSizeBuffer.y = mScrollableHeight;

        setViewportBottomLeft(left, top, true);
    }

    private float getDistanceOfSelectedImage(int index) {
        float dist = mActionBarHeight;
        BucketInfo bucketInfo = mGridInfo.getBucketInfo();
        int numOfDateLabels = mGridInfo.getNumOfDateInfos();
        for (int i = 0; i < numOfDateLabels; i++) {
            DateLabelInfo dateLabelInfo = bucketInfo.getDateInfo(i);

            ImageInfo firstImageInfo = dateLabelInfo.get(0);
            int firstImagePosition = bucketInfo.getIndex(firstImageInfo);

            int numOfImages = dateLabelInfo.getNumOfImages();
            ImageInfo lastImageInfo = dateLabelInfo.get(numOfImages - 1);
            int lastImagePosition = bucketInfo.getIndex(lastImageInfo);
            if (firstImagePosition <= index && index <= lastImagePosition) {
                dist += mDateLabelHeight;

                int row = (index - firstImagePosition) / mNumOfColumns;
                dist += ((mColumnWidth + mSpacing) * row);

                return dist;
            } else {
                dist += mDateLabelHeight;

                int numOfRowsInDateLabel = dateLabelInfo.getNumOfRows();
                dist += ((mColumnWidth + mSpacing) * numOfRowsInDateLabel);
            }
        }

        return 0;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    public void update() {
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

        if (mTranslateY == finalY) {
            mRotationAngle = 0f;
            mTranslateZ = 0f;
        }
    }

    public void setCenterImageIndex(int index) {
        mCenterImageIndex = index;
    }

    private final GestureDetector.SimpleOnGestureListener mGestureListener
            = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            releaseEdgeEffects();
            mScrollerStartViewport.set(mCurrentViewport);
            mScroller.forceFinished(true);
            invalidateViewport();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();

            int dateLabelIndex = mRenderer.getDateLabelIndex(y);
            int imageIndex = mRenderer.getSelectedImageIndex(x, y);
            if (imageIndex == -1) {
                return false;
            }

            Intent intent = new Intent(mContext, com.gomdev.gallery.ImageViewActivity.class);

            BucketInfo bucketInfo = mGridInfo.getBucketInfo();
            int bucketIndex = mImageManager.getIndex(bucketInfo);
            intent.putExtra(GalleryConfig.BUCKET_POSITION, bucketIndex);
            intent.putExtra(GalleryConfig.IMAGE_POSITION, imageIndex);
            intent.putExtra(GalleryConfig.DATE_LABEL_POSITION, dateLabelIndex);

            SharedPreferences pref = mContext.getSharedPreferences(GalleryConfig.PREF_NAME, 0);
            SharedPreferences.Editor editor = pref.edit();

            editor.putInt(GalleryConfig.PREF_BUCKET_INDEX, bucketIndex);
            editor.putInt(GalleryConfig.PREF_DATE_LABEL_INDEX, dateLabelIndex);
            editor.putInt(GalleryConfig.PREF_IMAGE_INDEX, imageIndex);

            editor.commit();

            mCenterImageIndex = imageIndex;

            mContext.startActivity(intent);

            return true;
        }

        private boolean mToScaleDown = true;

        @Override
        public boolean onDoubleTap(MotionEvent e) {
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
                mRenderer.resize(x, y);
                mGridInfo.resize(numOfColumns);
            }

            mSurfaceView.requestRender();

            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float viewportOffsetY = -distanceY;

            int scrolledY = (int) (mSurfaceBufferTop - mCurrentViewport.bottom - viewportOffsetY);

            boolean canScrollY = mCurrentViewport.top > mSurfaceBufferBottom
                    || mCurrentViewport.bottom < mSurfaceBufferTop;
            setViewportBottomLeft(mCurrentViewport.left,
                    (mCurrentViewport.bottom + viewportOffsetY), true);

            if (canScrollY && scrolledY < 0) {
                mEdgeEffectTop.onPull(scrolledY / (float) mContentRect.height());
                mEdgeEffectTopActive = true;
            }

            if (canScrollY && scrolledY > mSurfaceSizeBuffer.y - mContentRect.height()) {
                mEdgeEffectBottom.onPull((scrolledY - mSurfaceSizeBuffer.y + mContentRect.height())
                        / (float) mContentRect.height());
                mEdgeEffectBottomActive = true;
            }

            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            fling((int) -velocityX, (int) -velocityY);

            return true;
        }
    };

    private void releaseEdgeEffects() {
        mEdgeEffectTopActive
                = mEdgeEffectBottomActive
                = false;
        mEdgeEffectTop.onRelease();
        mEdgeEffectBottom.onRelease();
    }

    private void fling(int velocityX, int velocityY) {
        releaseEdgeEffects();
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

    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int currY = mScroller.getCurrY();

            boolean canScrollY = (mCurrentViewport.top > mSurfaceBufferBottom
                    || mCurrentViewport.bottom < (mSurfaceBufferTop));

            if (canScrollY
                    && currY < 0
                    && mEdgeEffectTop.isFinished()
                    && !mEdgeEffectTopActive) {
                mEdgeEffectTop.onAbsorb((int) mScroller.getCurrVelocity());
                mEdgeEffectTopActive = true;
            } else if (canScrollY
                    && currY > (mSurfaceSizeBuffer.y - mContentRect.height())
                    && mEdgeEffectBottom.isFinished()
                    && !mEdgeEffectBottomActive) {
                mEdgeEffectBottom.onAbsorb((int) mScroller.getCurrVelocity());
                mEdgeEffectBottomActive = true;
            }

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
