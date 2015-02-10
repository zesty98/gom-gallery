package com.gomdev.gallery;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;

import com.gomdev.gles.GLESAnimator;
import com.gomdev.gles.GLESAnimatorCallback;
import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVector3;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by gomdev on 14. 12. 16..
 */
class ImageListRenderer implements GLSurfaceView.Renderer, GridInfoChangeListener {
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
    private int mColumnWidth = 0;
    private int mPrevColumnWidth = 0;
    private int mNumOfColumns = 0;
    private int mPrevNumOfColumns = 0;

    private ImageObject mCenterObject = null;
    //    private ImageObject mLastObject = null;
    private float mFocusY = 0f;
    private boolean mIsOnAnimation = false;
    private boolean mIsOnScale = false;

    private GLESAnimator mAnimator = null;
    private float mBottom = 0f;
    private float mPrevBottom = 0f;
    private float mNextBottom = 0f;

    private boolean mIsSurfaceChanged = false;

    ImageListRenderer(Context context) {
        mContext = context;

        GLESContext.getInstance().setContext(context);
        ImageManager imageManager = ImageManager.getInstance();

        mObjectManager = new ObjectManager(context, this);
        imageManager.setObjectManager(mObjectManager);

        mGalleryGestureDetector = new GalleryGestureDetector(mContext, this);
        mGalleryScaleGestureDetector = new GalleryScaleGestureDetector(mContext, this);

        mAnimator = new GLESAnimator(new AnimatorCallback());
        mAnimator.setDuration(GalleryConfig.IMAGE_ANIMATION_START_OFFSET, GalleryConfig.IMAGE_ANIMATION_END_OFFSET);
    }

    // rendering

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mIsSurfaceChanged == false) {
            Log.d(TAG, "onDrawFrame() frame is skipped");
            return;
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        synchronized (GalleryContext.sLockObject) {
            udpateGestureDetector();

            boolean isOnScrolling = mGalleryGestureDetector.isOnScrolling();

            mObjectManager.update(isOnScrolling);
            mObjectManager.drawFrame();
            mAnimator.doAnimation();
        }
    }

    private void udpateGestureDetector() {
        if (mCenterObject != null && mIsOnAnimation == true) {
            float columnWidth = mDefaultColumnWidth * mCenterObject.getScale();
            float top = mCenterObject.getTop() + mFocusY - columnWidth * 0.5f + mCenterObject.getStartOffsetY();
            mGalleryGestureDetector.adjustViewport(top, mBottom);
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

    private GLESTexture createDummyTexture(int color) {
        Bitmap bitmap = GLESUtils.makeBitmap(16, 16, Bitmap.Config.ARGB_8888, color);

        GLESTexture dummyTexture = new GLESTexture.Builder(GLES20.GL_TEXTURE_2D, 16, 16)
                .load(bitmap);

        return dummyTexture;
    }

    // touch

    boolean onTouchEvent(MotionEvent event) {
        boolean retVal = mGalleryScaleGestureDetector.onTouchEvent(event);
        if (mIsOnScale == false && mIsOnAnimation == false) {
            retVal = mGalleryGestureDetector.onTouchEvent(event) || retVal;
        }

        return retVal;
    }

    // Listener

    void resize(float focusX, float focusY) {
        mFocusY = focusY;
        ImageIndexingInfo imageIndexingInfo = mObjectManager.getNearestImageIndex(focusX, focusY);
        mCenterObject = mObjectManager.getImageObject(imageIndexingInfo);
        Log.d(TAG, "resize() mCenterObject indexing info " + imageIndexingInfo);

        mGridInfo.setImageIndexingInfo(imageIndexingInfo);

        onAnimationStarted();
    }

    @Override
    public void onColumnWidthChanged() {
        if (DEBUG) {
            Log.d(TAG, "onColumnWidthChanged()");
        }

        mPrevColumnWidth = mColumnWidth;
        mColumnWidth = mGridInfo.getColumnWidth();

        mPrevNumOfColumns = mNumOfColumns;
        mNumOfColumns = mGridInfo.getNumOfColumns();

        BucketInfo bucketInfo = mGridInfo.getBucketInfo();
        DateLabelInfo lastDateLabelInfo = bucketInfo.getLast();
        int lastDateLabelIndex = lastDateLabelInfo.getIndex();
        DateLabelObject object = mObjectManager.getDateLabelObject(lastDateLabelIndex);

        int numOfImages = lastDateLabelInfo.getNumOfImages();
        int numOfRows = (int) Math.ceil((float) (numOfImages) / mPrevNumOfColumns);
        mPrevBottom = object.getPrevTop() - mGridInfo.getDateLabelHeight() - mSpacing - (mPrevColumnWidth + mSpacing) * numOfRows;
        mBottom = mPrevBottom;
        numOfRows = (int) Math.ceil((float) (numOfImages) / mNumOfColumns);
        mNextBottom = object.getNextTop() - mGridInfo.getDateLabelHeight() - mSpacing - (mColumnWidth + mSpacing) * numOfRows;

        mAnimator.setValues(0f, 1f);
        mAnimator.start();
    }

    @Override
    public void onNumOfImageInfosChanged() {
    }

    @Override
    public void onNumOfDateLabelInfosChanged() {
    }

    void onAnimationStarted() {
        mIsOnAnimation = true;
    }

    void onAnimationFinished() {
        mIsOnAnimation = false;
        mObjectManager.onAnimationFinished();
    }

    void onScaleBegin() {
        mIsOnScale = true;
    }

    void onScaleEnd() {
        mIsOnScale = false;
    }

    // resume

    void onResume() {
    }

    // pause

    void onPause() {
    }

    // set / get
    void setSurfaceView(GallerySurfaceView surfaceView) {
        mObjectManager.setSurfaceView(surfaceView);
        mGalleryGestureDetector.setSurfaceView(surfaceView);
        mGalleryScaleGestureDetector.setSurfaceView(surfaceView);
    }

    void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mSpacing = gridInfo.getSpacing();
        mDefaultColumnWidth = gridInfo.getDefaultColumnWidth();
        mColumnWidth = mGridInfo.getColumnWidth();
        mNumOfColumns = mGridInfo.getNumOfColumns();

        mGalleryGestureDetector.setGridInfo(gridInfo);
        mGalleryScaleGestureDetector.setGridInfo(gridInfo);

        mObjectManager.setGridInfo(gridInfo);

        mGridInfo.addListener(this);
    }

    ImageIndexingInfo getSelectedImageIndex(float x, float y) {
        return mObjectManager.getSelectedImageIndex(x, y);
    }

    float getNextTranslateY() {
        float top = mCenterObject.getNextTop() - (mColumnWidth * 0.5f) + mFocusY + mCenterObject.getNextStartOffsetY();
        float translateY = mGalleryGestureDetector.getTranslateY(top, mNextBottom);
        return translateY;
    }

    void cancelLoading() {
        mObjectManager.cancelLoading();
    }


    class AnimatorCallback implements GLESAnimatorCallback {
        AnimatorCallback() {
        }

        @Override
        public void onAnimation(GLESVector3 current) {
            mBottom = mPrevBottom + (mNextBottom - mPrevBottom) * current.getX();
            mObjectManager.onAnimation(current.getX());
        }

        @Override
        public void onCancel() {

        }

        @Override
        public void onFinished() {
            onAnimationFinished();
        }
    }
}
