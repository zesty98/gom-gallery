package com.gomdev.gallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.widget.OverScroller;

import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;

/**
 * Created by gomdev on 14. 12. 18..
 */
public class GallerySurfaceView extends GLSurfaceView implements RendererListener {
    static final String CLASS = "GallerySurfaceView";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int DRAG_DISTANCE_DPI = 50;   // dpi

    private Context mContext = null;
    private ImageListRenderer mRenderer = null;

    private RectF mCurrentViewport = null;   // OpenGL ES coordinate

    private GestureDetectorCompat mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private OverScroller mScroller;
    private RectF mScrollerStartViewport = new RectF(); // Used only for zooms and flings.
    private Rect mContentRect = new Rect(); // screen coordinate
    private Point mSurfaceSizeBuffer = new Point();

    // Edge effect / overscroll tracking objects.
    private EdgeEffectCompat mEdgeEffectTop;
    private EdgeEffectCompat mEdgeEffectBottom;

    private boolean mEdgeEffectTopActive;
    private boolean mEdgeEffectBottomActive;

    private float mSurfaceBufferTop = 0f;
    private float mSurfaceBufferBottom = 0f;
    private float mSurfaceBufferLeft = 0f;

    private GridInfo mGridInfo = null;
    private int mScrollableHeight = 0;
    private int mActionBarHeight = 0;

    private float mTranslateY = 0f;

    public GallerySurfaceView(Context context) {
        super(context);

        init(context);
    }

    public GallerySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    private void init(Context context) {
        mContext = context;

        mRenderer = new ImageListRenderer(context);
        mRenderer.setSurfaceView(this);

        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mGestureDetector = new GestureDetectorCompat(context, mGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);

        mScroller = new OverScroller(context);

        // Sets up edge effects
        mEdgeEffectTop = new EdgeEffectCompat(context);
        mEdgeEffectBottom = new EdgeEffectCompat(context);


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

        mContentRect.set(0, 0, w, h);

        mScrollableHeight = mGridInfo.getScrollableHeight();

        mSurfaceSizeBuffer.x = w;
        mSurfaceSizeBuffer.y = mScrollableHeight;

        float surfaceBufferRight = w * 0.5f;
        mSurfaceBufferLeft = -surfaceBufferRight;
        mSurfaceBufferTop = mScrollableHeight * 0.5f;
        mSurfaceBufferBottom = -mSurfaceBufferTop;

        if (mCurrentViewport == null) {
            float right = w * 0.5f;
            float left = -right;
            float top = mSurfaceBufferTop;
            float bottom = mSurfaceBufferTop - h;

            // OpenGL ES coordiante.
            mCurrentViewport = new RectF(left, bottom, right, top);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mRenderer.onSurfaceDestroyed();
        super.surfaceDestroyed(holder);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean retVal = mScaleGestureDetector.onTouchEvent(event);
        retVal = mGestureDetector.onTouchEvent(event) || retVal;
        return retVal || super.onTouchEvent(event);

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

    private final ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener
            = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private float mLastSpan;
        private int mDragDistance = 0;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            mLastSpan = scaleGestureDetector.getCurrentSpan();
            mDragDistance = GLESUtils.getPixelFromDpi(mContext, DRAG_DISTANCE_DPI);
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

                int imageIndex = getImageIndex(focusX, focusY);
                mGridInfo.resize(numOfColumns);
                GallerySurfaceView.this.resize(imageIndex);
                mRenderer.resize();
            }

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        }
    };

    private void resize(int imageIndex) {
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

        Log.d(TAG, "\nresize() mSurfaceBufferTop=" + mSurfaceBufferTop + " top=" + top);
        top = Math.min(top, mSurfaceBufferTop);

        Log.d(TAG, "resize() top=" + top);
        Log.d(TAG, "resize() mSurfaceBufferBottom=" + mSurfaceBufferBottom + " mCurrentViewport.height()=" + mCurrentViewport.height());
        top = Math.max(top, mSurfaceBufferBottom + mCurrentViewport.height());

        Log.d(TAG, "resize() top=" + top);

        setViewportBottomLeft(left, top);
    }

    public int getImageIndex(float x, float y) {
        int columnWidth = mGridInfo.getColumnWidth();
        int spacing = mGridInfo.getSpacing();
        int row = (int) (((mTranslateY + y) - mActionBarHeight) / (columnWidth + spacing));
        int column = (int) (x / (columnWidth + spacing));

        int numOfColumns = mGridInfo.getNumOfColumns();
        int imageIndex = numOfColumns * row + column;

        return imageIndex;
    }

    public int getImageColumn(float x) {
        int columnWidth = mGridInfo.getColumnWidth();
        int spacing = mGridInfo.getSpacing();
        int column = (int) (x / (columnWidth + spacing));

        return column;
    }

    public int getImageRow(float y) {
        int columnWidth = mGridInfo.getColumnWidth();
        int spacing = mGridInfo.getSpacing();
        int row = (int) (((mTranslateY + y) - mActionBarHeight) / (columnWidth + spacing));

        return row;
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

    @Override
    public void computeScroll() {
        super.computeScroll();

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

        Log.d(TAG, "setViewportBottomLeft() topY=" + topY);
        Log.d(TAG, "setViewportBottomLeft() mSurfaceSizeBuffer.y=" + mSurfaceSizeBuffer.y + " curHeight=" + curHeight);

        if (mSurfaceSizeBuffer.y < curHeight) {
            topY = mSurfaceBufferTop;
        } else {
            topY = Math.max(mSurfaceBufferBottom + curHeight, Math.min(topY, mSurfaceBufferTop));
        }

        Log.d(TAG, "setViewportBottomLeft() topY=" + topY);

        mCurrentViewport.set(leftX, topY - curHeight, leftX + curWidth, topY);

        mTranslateY = (mSurfaceBufferTop - mCurrentViewport.bottom);
        requestRender();
    }

    private void invalidateViewport() {
        computeScroll();
    }

    @Override
    public void update(final GLESNode node) {
        if (mScroller.getCurrVelocity() > 0) {
            computeScroll();
        }

        queueEvent(new Runnable() {
            @Override
            public void run() {
                GLESTransform transform = node.getTransform();
                transform.setIdentity();
                transform.setTranslate(0f, mTranslateY, 0);
            }
        });
    }

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mActionBarHeight = mGridInfo.getActionBarHeight();

        mRenderer.setGridInfo(mGridInfo);
    }
}
