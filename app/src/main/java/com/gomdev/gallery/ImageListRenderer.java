package com.gomdev.gallery;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.gomdev.gles.GLESAnimator;
import com.gomdev.gles.GLESAnimatorCallback;
import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRenderer;
import com.gomdev.gles.GLESSceneManager;
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
    static final boolean DEBUG = true;//GalleryConfig.DEBUG;

    private final Context mContext;
    private final GridInfo mGridInfo;

    private GallerySurfaceView mSurfaceView = null;

    private GLESRenderer mGLESRenderer = null;
    private GLESSceneManager mSM = null;
    private GLESNode mRoot = null;

    private AlbumViewManager mAlbumViewManager = null;

    private GalleryGestureDetector mGalleryGestureDetector = null;
    private GalleryScaleGestureDetector mGalleryScaleGestureDetector = null;

    private int mSpacing = 0;
    private int mDefaultColumnWidth = 0;
    private int mColumnWidth = 0;
    private int mPrevColumnWidth = 0;
    private int mNumOfColumns = 0;
    private int mPrevNumOfColumns = 0;

    private ImageObject mCenterObject = null;
    private float mFocusY = 0f;
    private boolean mIsOnAnimation = false;
    private boolean mIsOnScale = false;

    private GLESAnimator mScaleAnimator = null;
    private float mBottom = 0f;
    private float mPrevBottom = 0f;
    private float mNextBottom = 0f;

    private boolean mIsSurfaceChanged = false;

    ImageListRenderer(Context context, GridInfo gridInfo) {
        mContext = context;
        mGridInfo = gridInfo;


        GLESContext.getInstance().setContext(context);
        ImageManager imageManager = ImageManager.getInstance();

        mGLESRenderer = GLESRenderer.createRenderer();
        mSM = GLESSceneManager.createSceneManager();

        mAlbumViewManager = new AlbumViewManager(context, gridInfo);
        imageManager.setObjectManager(mAlbumViewManager);

        mRoot = mSM.createRootNode("root");

        GLESNode albumViewNode = new GLESNode("albumViewNode");
        mRoot.addChild(albumViewNode);

        mAlbumViewManager.createScene(albumViewNode);

        setGridInfo(gridInfo);

        mGalleryGestureDetector = new GalleryGestureDetector(mContext, gridInfo);
        mGalleryScaleGestureDetector = new GalleryScaleGestureDetector(mContext, gridInfo);

        mScaleAnimator = new GLESAnimator(new ScaleAnimatorCallback());
        mScaleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mScaleAnimator.setDuration(GalleryConfig.IMAGE_ANIMATION_START_OFFSET, GalleryConfig.IMAGE_ANIMATION_END_OFFSET);
    }

    private void setGridInfo(GridInfo gridInfo) {
        mSpacing = gridInfo.getSpacing();
        mDefaultColumnWidth = gridInfo.getDefaultColumnWidth();
        mColumnWidth = mGridInfo.getColumnWidth();
        mNumOfColumns = mGridInfo.getNumOfColumns();

        mGridInfo.addListener(this);
    }

    // rendering

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mIsSurfaceChanged == false) {
            Log.d(TAG, "onDrawFrame() frame is skipped");
            return;
        }

//        GLESUtils.checkFPS();

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        synchronized (GalleryContext.sLockObject) {
            udpateGestureDetector();

            boolean isOnScrolling = mGalleryGestureDetector.isOnScrolling();
            if (mIsOnAnimation == true) {
                mGalleryGestureDetector.resetOnScrolling();
            }

            mAlbumViewManager.update(isOnScrolling);

            mGLESRenderer.updateScene(mSM);
            mGLESRenderer.drawScene(mSM);

            if (mScaleAnimator.doAnimation() == true) {
                mSurfaceView.requestRender();
            }
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

        mGLESRenderer.reset();

        mIsSurfaceChanged = false;

        mGridInfo.setScreenSize(width, height);
        mGalleryGestureDetector.surfaceChanged(width, height);

        mAlbumViewManager.onSurfaceChanged(width, height);

        GLESCamera camera = setupCamera(width, height);
        mAlbumViewManager.setupObjects(camera);

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

        mAlbumViewManager.onSurfaceCreated();

        GLESTexture dummyImageTexture = createDummyTexture(Color.LTGRAY);
        GLESTexture dummyDateLabelTexture = createDummyTexture(Color.WHITE);
        mAlbumViewManager.setDummyTexture(dummyDateLabelTexture, dummyImageTexture);
    }

    private GLESTexture createDummyTexture(int color) {
        Bitmap bitmap = GLESUtils.makeBitmap(16, 16, Bitmap.Config.ARGB_8888, color);

        GLESTexture dummyTexture = new GLESTexture.Builder(GLES20.GL_TEXTURE_2D, 16, 16)
                .load(bitmap);

        return dummyTexture;
    }

    // touch

    boolean onTouchEvent(MotionEvent event) {
        if (mIsOnAnimation == true) {
            return true;
        }

        boolean retVal = mGalleryScaleGestureDetector.onTouchEvent(event);
        if (mIsOnScale == false) {
            retVal = mGalleryGestureDetector.onTouchEvent(event) || retVal;
        }

        return retVal;
    }

    // Listener

    void resize(float focusX, float focusY) {
        mFocusY = focusY;
        ImageIndexingInfo imageIndexingInfo = mAlbumViewManager.getNearestImageIndex(focusX, focusY);
        Log.d(TAG, "resize() mCenterObject indexing info " + imageIndexingInfo);
        mCenterObject = mAlbumViewManager.getImageObject(imageIndexingInfo);

        mGridInfo.setImageIndexingInfo(imageIndexingInfo);

        onAnimationStarted();
    }

    void onImageSelected(ImageIndexingInfo imageIndexingInfo) {
        RectF viewport = mGalleryGestureDetector.getCurrentViewport();

        mAlbumViewManager.onImageSelected(imageIndexingInfo, viewport);
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
        DateLabelObject object = mAlbumViewManager.getDateLabelObject(lastDateLabelIndex);

        int numOfImages = lastDateLabelInfo.getNumOfImages();
        int prevNumOfRows = (int) Math.ceil((float) (numOfImages) / mPrevNumOfColumns);
        mPrevBottom = object.getPrevTop() - mGridInfo.getDateLabelHeight() - mSpacing - (mPrevColumnWidth + mSpacing) * prevNumOfRows;
        mBottom = mPrevBottom;
        int numOfRows = (int) Math.ceil((float) (numOfImages) / mNumOfColumns);
        mNextBottom = object.getNextTop() - mGridInfo.getDateLabelHeight() - mSpacing - (mColumnWidth + mSpacing) * numOfRows;

        mScaleAnimator.setValues(0f, 1f);
        mScaleAnimator.start();
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
        mAlbumViewManager.onAnimationFinished();
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
        mSurfaceView = surfaceView;
        mAlbumViewManager.setSurfaceView(surfaceView);
        mGalleryGestureDetector.setSurfaceView(surfaceView);
        mGalleryScaleGestureDetector.setSurfaceView(surfaceView);
    }


    ImageIndexingInfo getSelectedImageIndex(float x, float y) {
        return mAlbumViewManager.getSelectedImageIndex(x, y);
    }

    float getNextTranslateY() {
        float top = mCenterObject.getNextTop() - (mColumnWidth * 0.5f) + mFocusY + mCenterObject.getNextStartOffsetY();
        float translateY = mGalleryGestureDetector.getTranslateY(top, mNextBottom);
        return translateY;
    }

    void cancelLoading() {
        mAlbumViewManager.cancelLoading();
    }

    class ScaleAnimatorCallback implements GLESAnimatorCallback {
        ScaleAnimatorCallback() {
        }

        @Override
        public void onAnimation(GLESVector3 current) {
            mBottom = mPrevBottom + (mNextBottom - mPrevBottom) * current.getX();
            mAlbumViewManager.onAnimation(current.getX());
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
