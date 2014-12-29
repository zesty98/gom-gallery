package com.gomdev.gallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.OverScroller;

/**
 * Created by gomdev on 14. 12. 29..
 */
public class GalleryGestureDetector {
    static final String CLASS = "GalleryScroller";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;

    private GridInfo mGridInfo = null;
    private GallerySurfaceView mSurfaceView = null;

    private GestureDetectorCompat mGestureDetector;

    private OverScroller mScroller;

    private RectF mCurrentViewport = null;   // OpenGL ES coordinate
    private RectF mScrollerStartViewport = new RectF();
    private Rect mContentRect = new Rect(); // screen coordinate
    private Point mSurfaceSizeBuffer = new Point();

    private int mScrollableHeight = 0;
    private int mActionBarHeight = 0;

    private float mSurfaceBufferTop = 0f;
    private float mSurfaceBufferBottom = 0f;
    private float mSurfaceBufferLeft = 0f;

    private EdgeEffectCompat mEdgeEffectTop;
    private EdgeEffectCompat mEdgeEffectBottom;

    private boolean mEdgeEffectTopActive;
    private boolean mEdgeEffectBottomActive;

    private float mTranslateY = 0f;


    public GalleryGestureDetector(Context context, GallerySurfaceView surfaceView) {
        mContext = context;
        mSurfaceView = surfaceView;

        mGestureDetector = new GestureDetectorCompat(context, mGestureListener);

        mScroller = new OverScroller(context);

        mEdgeEffectTop = new EdgeEffectCompat(context);
        mEdgeEffectBottom = new EdgeEffectCompat(context);
    }

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mActionBarHeight = gridInfo.getActionBarHeight();
    }

    public void onSurfaceChanged(int width, int height) {
        mContentRect.set(0, 0, width, height);

        mScrollableHeight = mGridInfo.getScrollableHeight();

        mSurfaceSizeBuffer.x = width;
        mSurfaceSizeBuffer.y = mScrollableHeight;

        float surfaceBufferRight = width * 0.5f;
        mSurfaceBufferLeft = -surfaceBufferRight;
        mSurfaceBufferTop = mScrollableHeight * 0.5f;
        mSurfaceBufferBottom = -mSurfaceBufferTop;

        if (mCurrentViewport == null) {
            float right = width * 0.5f;
            float left = -right;
            float top = mSurfaceBufferTop;
            float bottom = mSurfaceBufferTop - height;

            // OpenGL ES coordiante.
            mCurrentViewport = new RectF(left, bottom, right, top);
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    public void update() {
        if (mScroller.getCurrVelocity() > 0) {
            computeScroll();
        }
    }

    public void resize(int imageIndex) {
        int scrollableHeight = mGridInfo.getScrollableHeight();
        if (mScrollableHeight == scrollableHeight) {
            return;
        }

        mScrollableHeight = scrollableHeight;
        mSurfaceSizeBuffer.y = scrollableHeight;
        mSurfaceBufferTop = mScrollableHeight * 0.5f;
        mSurfaceBufferBottom = -mSurfaceBufferTop;

        int row = (int) (imageIndex / mGridInfo.getNumOfColumns());
        float y = row * (mGridInfo.getColumnWidth() + mGridInfo.getSpacing()) + mActionBarHeight;
        float left = mSurfaceBufferLeft;
        float top = mSurfaceBufferTop - y;

        top = Math.min(top, mSurfaceBufferTop);
        top = Math.max(top, mSurfaceBufferBottom + mCurrentViewport.height());

        setViewportBottomLeft(left, top);
    }

    public float getScrollDistance() {
        return mTranslateY;
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

            int imageIndex = getImageIndex(x, y);

            Intent intent = new Intent(mContext, com.gomdev.gallery.ImageViewActivity.class);

            intent.putExtra(GalleryConfig.BUCKET_POSITION, mGridInfo.getBucketInfo().getPosition());
            intent.putExtra(GalleryConfig.IMAGE_POSITION, imageIndex);

            mContext.startActivity(intent);

            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float viewportOffsetY = -distanceY;

            int scrolledY = (int) (mSurfaceBufferTop - mCurrentViewport.bottom - viewportOffsetY);

            boolean canScrollY = mCurrentViewport.top > mSurfaceBufferBottom
                    || mCurrentViewport.bottom < mSurfaceBufferTop;
            setViewportBottomLeft(mCurrentViewport.left,
                    (mCurrentViewport.bottom + viewportOffsetY));

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

    public int getImageIndex(float x, float y) {
        int columnWidth = mGridInfo.getColumnWidth();
        int spacing = mGridInfo.getSpacing();
        int row = (int) (((mTranslateY + y) - mActionBarHeight) / (columnWidth + spacing));
        int column = (int) (x / (columnWidth + spacing));

        int numOfColumns = mGridInfo.getNumOfColumns();
        int imageIndex = numOfColumns * row + column;

        return imageIndex;
    }

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
        boolean needsInvalidate = false;

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
                needsInvalidate = true;
            } else if (canScrollY
                    && currY > (mSurfaceSizeBuffer.y - mContentRect.height())
                    && mEdgeEffectBottom.isFinished()
                    && !mEdgeEffectBottomActive) {
                mEdgeEffectBottom.onAbsorb((int) mScroller.getCurrVelocity());
                mEdgeEffectBottomActive = true;
                needsInvalidate = true;
            }

            float currYRange = (mSurfaceBufferTop) - currY;
            setViewportBottomLeft(mSurfaceBufferLeft, currYRange);
        }

        if (needsInvalidate) {
            invalidateViewport();
        }
    }

    private void setViewportBottomLeft(float leftX, float topY) {
        float curWidth = mCurrentViewport.width();
        float curHeight = mCurrentViewport.height();

        if (mSurfaceSizeBuffer.y < curHeight) {
            topY = mSurfaceBufferTop;
        } else {
            topY = Math.max(mSurfaceBufferBottom + curHeight, Math.min(topY, mSurfaceBufferTop));
        }

        mCurrentViewport.set(leftX, topY - curHeight, leftX + curWidth, topY);

        mTranslateY = (mSurfaceBufferTop - mCurrentViewport.bottom);

        mSurfaceView.requestRender();
    }

    private void invalidateViewport() {
        computeScroll();
    }
}
