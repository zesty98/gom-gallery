package com.gomdev.gallery;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESTransform;

import java.util.ArrayList;

/**
 * Created by gomdev on 14. 12. 18..
 */
public class GallerySurfaceView extends GLSurfaceView implements RendererListener {
    static final String CLASS = "GallerySurfaceView";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private Context mContext = null;
    private ImageListRenderer mRenderer = null;

    private GalleryGestureDetector mGalleryGestureDetector = null;
    private GalleryScaleGestureDetector mGalleryScaleGestureDetector = null;

    private GridInfo mGridInfo = null;

    private Object mLockObject;

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
        mLockObject = new Object();

        mListeners.clear();

        mRenderer = new ImageListRenderer(context, mLockObject);
        mRenderer.setSurfaceView(this);

        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mGalleryGestureDetector = new GalleryGestureDetector(context, this);
        mGalleryScaleGestureDetector = new GalleryScaleGestureDetector(context, this);
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
        mGalleryGestureDetector.surfaceChanged(w, h);

        int size = mListeners.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                mListeners.get(i).onGridInfoChanged();
            }
        }
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
        boolean retVal = mGalleryScaleGestureDetector.onTouchEvent(event);
        retVal = mGalleryGestureDetector.onTouchEvent(event) || retVal;
        return retVal || super.onTouchEvent(event);

    }

    public void setGridInfoChangeListener(GridInfoChangeListener listener) {
        mListeners.add(listener);
    }

    public void resize(int centerImageIndex) {
        synchronized (mLockObject) {
            mGalleryGestureDetector.setCenterImageIndex(centerImageIndex);
            int size = mListeners.size();
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    mListeners.get(i).onGridInfoChanged();
                }
            }
        }
    }

    @Override
    public void update(final GLESNode node) {
        mGalleryGestureDetector.update();

        GLESTransform transform = node.getTransform();
        transform.setIdentity();

        float angle = mGalleryGestureDetector.getAngle();
        transform.rotate(angle, 1f, 0f, 0f);

        float translateZ = mGalleryGestureDetector.getTranslate();
        transform.translate(0f, 0f, translateZ);

        float scrollDistance = mGalleryGestureDetector.getScrollDistance();
        transform.preTranslate(0f, scrollDistance, 0f);
    }

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mGalleryGestureDetector.setGridInfo(mGridInfo);
        mGalleryScaleGestureDetector.setGridInfo(mGridInfo);
        mRenderer.setGridInfo(mGridInfo);
    }

    public int getSelectedIndex(float x, float y) {
        int index = mRenderer.getSelectedIndex(x, y);

        return index;
    }

    public int getNearestIndex(float x, float y) {
        int index = mRenderer.getNearestIndex(x, y);

        return index;
    }
}
