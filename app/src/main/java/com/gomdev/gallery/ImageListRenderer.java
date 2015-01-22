package com.gomdev.gallery;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by gomdev on 14. 12. 16..
 */
public class ImageListRenderer implements GLSurfaceView.Renderer {
    static final String CLASS = "ImageListRenderer";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;

    private ObjectManager mObjectManager = null;

    private GalleryGestureDetector mGalleryGestureDetector = null;
    private GalleryScaleGestureDetector mGalleryScaleGestureDetector = null;

    private GridInfo mGridInfo = null;
    private int mSpacing = 0;
    private int mDefaultColumnWidth = 0;

    private GalleryObject mCenterObject = null;
    private GalleryObject mLastObject = null;
    private float mFocusY = 0f;
    private boolean mIsOnAnimation = false;
    private boolean mIsOnScale = false;

    private boolean mIsSurfaceChanged = false;

    public ImageListRenderer(Context context) {
        mContext = context;

        GLESContext.getInstance().setContext(context);

        mObjectManager = new ObjectManager(context, this);

        mGalleryGestureDetector = new GalleryGestureDetector(mContext, this);
        mGalleryScaleGestureDetector = new GalleryScaleGestureDetector(mContext, this);
    }

    // rendering

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mIsSurfaceChanged == false) {
            Log.d(TAG, "onDrawFrame() frame is skipped");
            return;
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        udpateGestureDetector();

        synchronized (GalleryContext.sLockObject) {
            mObjectManager.update();
            mObjectManager.drawFrame();
        }
    }

    private void udpateGestureDetector() {
        if (mCenterObject != null && mIsOnAnimation == true) {
            float columnWidth = mDefaultColumnWidth * mGridInfo.getScale();
            float bottom = mLastObject.getTop() - columnWidth + mSpacing;
            float pos = mCenterObject.getTop() + mFocusY - columnWidth * 0.5f;
            mGalleryGestureDetector.adjustViewport(pos, bottom);
        }

        mGalleryGestureDetector.update();
    }

    // onSurfaceChanged

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged()");
        }

        mIsSurfaceChanged = false;

        mGridInfo.setScreenSize(width, height);
        mGalleryGestureDetector.surfaceChanged(width, height);

        mObjectManager.onSurfaceChanged(width, height);

        GLESCamera camera = setupCamera(width, height);
        mObjectManager.setupObjects(camera);

        mIsSurfaceChanged = true;
    }

    private GLESTexture createDummyTexture(int color) {
        Bitmap bitmap = GLESUtils.makeBitmap(16, 16, Bitmap.Config.ARGB_8888, color);

        GLESTexture dummyTexture = new GLESTexture.Builder(GLES20.GL_TEXTURE_2D, 16, 16)
                .load(bitmap);

        return dummyTexture;
    }


    private GLESCamera setupCamera(int width, int height) {
        GLESCamera camera = new GLESCamera();

        float screenRatio = (float) width / height;

        float fovy = 30f;
        float eyeZ = (height / 2f) / (float) Math.tan(Math.toRadians(fovy * 0.5));

        camera.setLookAt(0f, 0f, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);

        camera.setFrustum(fovy, screenRatio, eyeZ * 0.01f, eyeZ * 10f);

        camera.setViewport(new GLESRect(0, 0, width, height));

        return camera;
    }

    // onSurfaceCreated

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        GLES20.glClearColor(1f, 1f, 1f, 1f);

        mObjectManager.onSurfaceCreated();

        GLESTexture dummyTexture = createDummyTexture(Color.LTGRAY);
        mObjectManager.setDummyImageTexture(dummyTexture);

        dummyTexture = createDummyTexture(Color.WHITE);
        mObjectManager.setDummyDateLabelTexture(dummyTexture);

        mObjectManager.createScene();
    }

    // touch

    public boolean onTouchEvent(MotionEvent event) {
        boolean retVal = mGalleryScaleGestureDetector.onTouchEvent(event);
        if (mIsOnScale == false && mIsOnAnimation == false) {
            retVal = mGalleryGestureDetector.onTouchEvent(event) || retVal;
        }

        return retVal;
    }

    // Listener

    public void resize(float focusX, float focusY) {
        mFocusY = focusY;
        int imageIndex = mObjectManager.getNearestImageIndex(focusX, focusY);
        mCenterObject = mObjectManager.getImageObject(imageIndex);
        int lastIndex = mGridInfo.getBucketInfo().getNumOfImages() - 1;
        mLastObject = mObjectManager.getImageObject(lastIndex);
        mGalleryGestureDetector.setCenterImageIndex(imageIndex);
    }

    public void onAnimationStarted() {
        mIsOnAnimation = true;
    }

    public void onAnimationFinished() {
        mIsOnAnimation = false;
    }

    public void onAnimationCanceled() {
        mIsOnAnimation = false;
    }

    public void onScaleBegin() {
        mIsOnScale = true;
    }

    public void onScaleEnd() {
        mIsOnScale = false;
    }

    // resume

    public void onResume() {
        SharedPreferences pref = mContext.getSharedPreferences(GalleryConfig.PREF_NAME, 0);
        int currentImageIndex = pref.getInt(GalleryConfig.PREF_IMAGE_INDEX, 0);
        mGalleryGestureDetector.setCenterImageIndex(currentImageIndex);
    }

    // pause

    public void onPause() {

    }

    // set / get
    public void setSurfaceView(GallerySurfaceView surfaceView) {
        mObjectManager.setSurfaceView(surfaceView);
        mGalleryGestureDetector.setSurfaceView(surfaceView);
        mGalleryScaleGestureDetector.setSurfaceView(surfaceView);
    }

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mSpacing = gridInfo.getSpacing();
        mDefaultColumnWidth = gridInfo.getDefaultColumnWidth();

        mGalleryGestureDetector.setGridInfo(gridInfo);
        mGalleryScaleGestureDetector.setGridInfo(gridInfo);

        mObjectManager.setGridInfo(gridInfo);
    }

    public int getSelectedImageIndex(float x, float y) {
        return mObjectManager.getSelectedImageIndex(x, y);
    }

    public int getDateLabelIndex(float y) {
        return mObjectManager.getDateLabelIndex(y);
    }
}
