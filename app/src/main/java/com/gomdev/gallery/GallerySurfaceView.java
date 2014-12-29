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
    private int mActionBarHeight = 0;

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
        mListeners.clear();

        mRenderer = new ImageListRenderer(context);
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
        boolean retVal = mGalleryScaleGestureDetector.onTouchEvent(event);
        retVal = mGalleryGestureDetector.onTouchEvent(event) || retVal;
        return retVal || super.onTouchEvent(event);

    }

    public void setGridInfoChangeListener(GridInfoChangeListener listener) {
        mListeners.add(listener);
    }

    public void resize(int centerImageIndex) {
        mGalleryGestureDetector.setCenterImageIndex(centerImageIndex);
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

        mActionBarHeight = mGridInfo.getActionBarHeight();

        mGalleryGestureDetector.setGridInfo(mGridInfo);
        mGalleryScaleGestureDetector.setGridInfo(mGridInfo);
        mRenderer.setGridInfo(mGridInfo);
    }

    public int getImageIndex(float x, float y) {
        float scrollDistance = mGalleryGestureDetector.getScrollDistance();
        int columnWidth = mGridInfo.getColumnWidth();
        int spacing = mGridInfo.getSpacing();
        int row = (int) (((scrollDistance + y) - mActionBarHeight) / (columnWidth + spacing));
        int column = (int) (x / (columnWidth + spacing));

        int numOfColumns = mGridInfo.getNumOfColumns();
        int imageIndex = numOfColumns * row + column;

        return imageIndex;
    }
}
