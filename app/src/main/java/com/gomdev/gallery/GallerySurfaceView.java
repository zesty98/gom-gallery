package com.gomdev.gallery;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;

import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;

import java.util.ArrayList;

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

    private GalleryGestureDetector mGalleryGestureDetector = null;

    private ScaleGestureDetector mScaleGestureDetector;

    private GridInfo mGridInfo = null;
    private int mNumOfColumns;
    private int mMinNumOfColumns;
    private int mMaxNumOfColumns;

    private ArrayList<GridInfoChangeListener> mListeners = new ArrayList<>();

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

        mListeners.clear();

        mRenderer = new ImageListRenderer(context);
        mRenderer.setSurfaceView(this);

        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


        mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);

        mGalleryGestureDetector = new GalleryGestureDetector(context, this);

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

        mGalleryGestureDetector.onSurfaceChanged(w, h);

        int size = mListeners.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                mListeners.get(i).onSurfaceChanged(w, h);
            }
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
        retVal = mGalleryGestureDetector.onTouchEvent(event) || retVal;
        return retVal || super.onTouchEvent(event);

    }

    public void setGridInfoChangeListener(GridInfoChangeListener listener) {
        mListeners.add(listener);
    }

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

                numOfColumns = Math.max(numOfColumns, mMinNumOfColumns);
                numOfColumns = Math.min(numOfColumns, mMaxNumOfColumns);

                if (numOfColumns != mNumOfColumns) {
                    int imageIndex = mGalleryGestureDetector.getImageIndex(focusX, focusY);
                    mGridInfo.resize(numOfColumns);
                    mGalleryGestureDetector.resize(imageIndex);
                    GallerySurfaceView.this.resize();
                }
            }

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        }
    };

    private void resize() {
        int size = mListeners.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                mListeners.get(i).onGridInfoChanged();
            }
        }
    }

    @Override
    public void update(final GLESNode node) {
        mGalleryGestureDetector.update();

        float scrollDistance = mGalleryGestureDetector.getScrollDistance();

        GLESTransform transform = node.getTransform();
        transform.setIdentity();
        transform.setTranslate(0f, scrollDistance, 0);
    }

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mNumOfColumns = GalleryContext.getInstance().getNumOfColumns();
        mMinNumOfColumns = mNumOfColumns;
        mMaxNumOfColumns = 3 * mNumOfColumns;

        mGalleryGestureDetector.setGridInfo(mGridInfo);
        mRenderer.setGridInfo(mGridInfo);
    }
}
