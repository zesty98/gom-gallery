package com.gomdev.gallery;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.opengl.GLES20;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.gomdev.gles.GLESAnimator;
import com.gomdev.gles.GLESAnimatorCallback;
import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESNodeListener;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESVector3;

/**
 * Created by gomdev on 14. 12. 31..
 */
class AlbumViewManager implements GridInfoChangeListener, ViewManager {
    static final String CLASS = "AlbumViewManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;
    private final GridInfo mGridInfo;

    private GallerySurfaceView mSurfaceView = null;

    private GalleryObjects mGalleryObjects = null;
    private Scrollbar mScrollbar = null;
    private GalleryContext mGalleryContext = null;
    private GLESNode mAlbumViewNode = null;

    private GLESShader mTextureShader = null;
    private GLESShader mTextureAlphaShader = null;
    private GLESShader mColorShader = null;

    private GLESAnimator mScaleAnimator = null;
    private boolean mIsOnAnimation = false;
    private boolean mIsOnScale = false;

    private AlbumViewGestureDetector mAlbumViewGestureDetector = null;
    private AlbumViewScaleGestureDetector mAlbumViewScaleGestureDetector = null;

    private BucketInfo mBucketInfo = null;
    private int mNumOfDateInfos = 0;
    private int mSpacing = 0;
    private int mDefaultColumnWidth = 0;
    private int mColumnWidth = 0;
    private int mPrevColumnWidth = 0;
    private int mNumOfColumns = 0;
    private int mPrevNumOfColumns = 0;

    private ImageObject mCenterObject = null;
    private float mFocusY = 0f;

    private float mBottom = 0f;
    private float mPrevBottom = 0f;
    private float mNextBottom = 0f;

    private int mDateLabelHeight;

    private int mWidth = 0;
    private int mHeight = 0;

    private boolean mIsSurfaceChanged = false;

    AlbumViewManager(Context context, GridInfo gridInfo) {
        if (DEBUG) {
            Log.d(TAG, "ObjectManager()");
        }

        mContext = context;
        mGridInfo = gridInfo;

        init();

        setGridInfo(gridInfo);
    }

    private void init() {
        mGalleryContext = GalleryContext.getInstance();

        mBucketInfo = mGridInfo.getBucketInfo();
        mGalleryObjects = new GalleryObjects(mContext, mGridInfo, mBucketInfo);
        mGalleryObjects.setAlbumViewManager(this);

        GLESGLState glState = new GLESGLState();
        glState.setCullFaceState(true);
        glState.setCullFace(GLES20.GL_BACK);
        glState.setDepthState(false);

        mGalleryObjects.setImageGLState(glState);

        glState = new GLESGLState();
        glState.setCullFaceState(true);
        glState.setCullFace(GLES20.GL_BACK);
        glState.setBlendState(true);
        glState.setBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        mGalleryObjects.setDateLabelGLState(glState);

        mScrollbar = new Scrollbar(mContext, mGridInfo);
        mScrollbar.setColor(0.3f, 0.3f, 0.3f, 0.7f);

        mScaleAnimator = new GLESAnimator(new ScaleAnimatorCallback());
        mScaleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mScaleAnimator.setDuration(GalleryConfig.IMAGE_ANIMATION_START_OFFSET, GalleryConfig.IMAGE_ANIMATION_END_OFFSET);

        mAlbumViewScaleGestureDetector = new AlbumViewScaleGestureDetector(mContext, mGridInfo);
        mAlbumViewScaleGestureDetector.setAlbumViewManager(this);
        mAlbumViewGestureDetector = new AlbumViewGestureDetector(mContext, mGridInfo);
        mAlbumViewGestureDetector.setAlbumViewManager(this);

        mIsSurfaceChanged = false;
    }

    private void setGridInfo(GridInfo gridInfo) {
        mDateLabelHeight = gridInfo.getDateLabelHeight();
        mSpacing = gridInfo.getSpacing();
        mNumOfDateInfos = gridInfo.getNumOfDateInfos();
        mColumnWidth = mGridInfo.getColumnWidth();
        mNumOfColumns = mGridInfo.getNumOfColumns();

        gridInfo.addListener(this);
    }


    // rendering

    @Override
    public void update() {
        udpateGestureDetector();

        boolean isOnScrolling = mAlbumViewGestureDetector.isOnScrolling();
        if (mIsOnAnimation == true) {
            mAlbumViewGestureDetector.resetOnScrolling();
        }

        if (isOnScrolling == false) {
            mGalleryObjects.updateTexture();
        }

        mGalleryObjects.checkVisibility(isOnScrolling);
        mGalleryObjects.update(isOnScrolling);
        mScrollbar.update(isOnScrolling);
    }

    private void udpateGestureDetector() {
        if (mCenterObject != null && mIsOnAnimation == true) {
            float columnWidth = mDefaultColumnWidth * mCenterObject.getScale();
            float top = mCenterObject.getTop() + mFocusY - columnWidth * 0.5f + mCenterObject.getStartOffsetY();
            mAlbumViewGestureDetector.adjustViewport(top, mBottom);
        }

        mAlbumViewGestureDetector.update();
    }

    @Override
    public void updateAnimation() {
        if (mScaleAnimator.doAnimation() == true) {
            mSurfaceView.requestRender();
        }
    }

    // onSurfaceChanged

    void onSurfaceChanged(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged()");
        }

        mWidth = width;
        mHeight = height;

        mGalleryObjects.onSurfaceChanged(width, height);
        mScrollbar.onSurfaceChanged(width, height);
        mAlbumViewGestureDetector.surfaceChanged(width, height);

        mIsSurfaceChanged = true;
    }

    void setupObjects(GLESCamera camera) {
        if (DEBUG) {
            Log.d(TAG, "setupObjects()");
        }

        mGalleryObjects.setupObjects(camera);
        mScrollbar.setupObject(camera);
    }

    // onSurfaceCreated

    void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        mIsSurfaceChanged = false;

        mGalleryObjects.setDateLabelShader(mTextureAlphaShader);
        mGalleryObjects.setImageShader(mTextureShader);
        mScrollbar.setShader(mColorShader);

        mGalleryObjects.onSurfaceCreated();

        GLESTexture dummyImageTexture = GalleryUtils.createDummyTexture(Color.LTGRAY);
        GLESTexture dummyDateLabelTexture = GalleryUtils.createDummyTexture(Color.WHITE);

        mGalleryObjects.setDummyTexture(dummyDateLabelTexture, dummyImageTexture);
    }

    // initialization
    void createScene(GLESNode node) {
        if (DEBUG) {
            Log.d(TAG, "createScene()");
        }

        mAlbumViewNode = node;

        GLESNode imageNode = new GLESNode("imageNode");
        imageNode.setListener(mImageNodeListener);
        node.addChild(imageNode);

        mGalleryObjects.createObjects(imageNode);

        mScrollbar.createObject(node);

        mAlbumViewNode.show();
    }

    void setTextureAlphaShader(GLESShader shader) {
        mTextureAlphaShader = shader;
    }

    void setTextureShader(GLESShader shader) {
        mTextureShader = shader;
    }

    void setColorShader(GLESShader shader) {
        mColorShader = shader;
    }

    void setSurfaceView(GallerySurfaceView surfaceView) {
        if (DEBUG) {
            Log.d(TAG, "setSurfaceView()");
        }

        mSurfaceView = surfaceView;

        mGalleryObjects.setSurfaceView(surfaceView);
        mScrollbar.setSurfaceView(surfaceView);

        mAlbumViewScaleGestureDetector.setSurfaceView(surfaceView);
        mAlbumViewGestureDetector.setSurfaceView(surfaceView);
    }

    // resume / pause

    void onResume() {
        if (DEBUG) {
            Log.d(TAG, "onResume()");
        }

        SharedPreferences pref = mContext.getSharedPreferences(GalleryConfig.PREF_NAME, 0);
        int bucketIndex = pref.getInt(GalleryConfig.PREF_BUCKET_INDEX, 0);
        int dateLabelIndex = pref.getInt(GalleryConfig.PREF_DATE_LABEL_INDEX, 0);
        int imageIndex = pref.getInt(GalleryConfig.PREF_IMAGE_INDEX, 0);

        ImageIndexingInfo imageIndexingInfo = new ImageIndexingInfo(bucketIndex, dateLabelIndex, imageIndex);
        mCenterObject = getImageObject(imageIndexingInfo);
        mGalleryContext.setImageIndexingInfo(imageIndexingInfo);
    }

    void onPause() {
        if (DEBUG) {
            Log.d(TAG, "onPause()");
        }
    }

    // touch

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mIsOnAnimation == true) {
            return true;
        }

        boolean retVal = mAlbumViewScaleGestureDetector.onTouchEvent(event);

        if (mIsOnScale == false) {
            retVal = mAlbumViewGestureDetector.onTouchEvent(event) || retVal;
        }

        return retVal;
    }


    // listener / callback

    void resize(float focusX, float focusY) {
        if (DEBUG) {
            Log.d(TAG, "resize()");
        }

        mFocusY = focusY;
        ImageIndexingInfo imageIndexingInfo = getNearestImageIndex(focusX, focusY);
        Log.d(TAG, "resize() mCenterObject indexing info " + imageIndexingInfo);
        mCenterObject = getImageObject(imageIndexingInfo);

        mGalleryContext.setImageIndexingInfo(imageIndexingInfo);
    }

    void onScaleBegin() {
        mIsOnScale = true;
    }

    void onScaleEnd() {
        mIsOnScale = false;
    }

    void onAnimationStarted() {
        mIsOnAnimation = true;
    }

    void onAnimationFinished() {
        mIsOnAnimation = false;
        mGalleryObjects.onAnimationFinished();
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
        DateLabelObject object = getDateLabelObject(lastDateLabelIndex);

        int numOfImages = lastDateLabelInfo.getNumOfImages();
        int prevNumOfRows = (int) Math.ceil((float) (numOfImages) / mPrevNumOfColumns);
        mPrevBottom = object.getPrevTop() - mGridInfo.getDateLabelHeight() - mSpacing - (mPrevColumnWidth + mSpacing) * prevNumOfRows;
        mBottom = mPrevBottom;
        int numOfRows = (int) Math.ceil((float) (numOfImages) / mNumOfColumns);
        mNextBottom = object.getNextTop() - mGridInfo.getDateLabelHeight() - mSpacing - (mColumnWidth + mSpacing) * numOfRows;

        if (mIsSurfaceChanged == false) {
            return;
        }

        mGalleryObjects.hide();

        mScaleAnimator.setValues(0f, 1f);
        mScaleAnimator.start();

        onAnimationStarted();
    }

    @Override
    public void onNumOfImageInfosChanged() {
        if (DEBUG) {
            Log.d(TAG, "onNumOfImageInfosChanged()");
        }
    }

    @Override
    public void onNumOfDateLabelInfosChanged() {
        if (DEBUG) {
            Log.d(TAG, "onNumOfDateLabelInfosChanged()");
        }

        mNumOfDateInfos = mGridInfo.getNumOfDateInfos();
    }

    // set / get

    void show() {
        mAlbumViewNode.show();
    }

    void hide() {
        mAlbumViewNode.hide();
    }


    int getDateLabelIndex(float y) {
        float translateY = mGridInfo.getTranslateY();

        float yPos = mHeight * 0.5f - (y + translateY);

        int selectedDateLabelIndex = getDateLabelIndexFromYPos(yPos);

        return selectedDateLabelIndex;
    }

    ImageIndexingInfo getSelectedImageIndex(float x, float y) {
        BucketInfo bucketInfo = mGridInfo.getBucketInfo();
        int bucketIndex = bucketInfo.getIndex();

        float translateY = mGridInfo.getTranslateY();
        float yPos = mHeight * 0.5f - (y + translateY);

        int dateLabelIndex = getDateLabelIndexFromYPos(yPos);
        int index = getImageIndexFromYPos(x, yPos, dateLabelIndex);

        DateLabelInfo dateLabelInfo = mBucketInfo.get(dateLabelIndex);
        ImageInfo lastImageInfo = dateLabelInfo.getLast();
        int lastImageIndex = lastImageInfo.getIndex();

        if (index > lastImageIndex) {
            index = -1;
        }

        ImageIndexingInfo imageIndexingInfo = new ImageIndexingInfo(bucketIndex, dateLabelIndex, index);

        return imageIndexingInfo;
    }

    ImageIndexingInfo getNearestImageIndex(float x, float y) {
        BucketInfo bucketInfo = mGridInfo.getBucketInfo();
        int bucketIndex = bucketInfo.getIndex();

        float translateY = mGridInfo.getTranslateY();
        float yPos = mHeight * 0.5f - (y + translateY);

        int dateLabelIndex = getDateLabelIndexFromYPos(yPos);
        int index = getImageIndexFromYPos(x, yPos, dateLabelIndex);

        DateLabelInfo dateLabelInfo = mBucketInfo.get(dateLabelIndex);
        ImageInfo lastImageInfo = dateLabelInfo.getLast();
        int lastImageIndex = lastImageInfo.getIndex();

        if (index > lastImageIndex) {
            index = lastImageIndex;
        }

        ImageIndexingInfo imageIndexingInfo = new ImageIndexingInfo(bucketIndex, dateLabelIndex, index);

        return imageIndexingInfo;
    }

    private int getDateLabelIndexFromYPos(float yPos) {
        int selectedDateLabelIndex = 0;
        for (int i = 0; i < mNumOfDateInfos; i++) {
            GalleryObject dateLabelObject = mGalleryObjects.getObject(i);
            if (yPos > dateLabelObject.getTop()) {
                selectedDateLabelIndex = i - 1;
                break;
            }
            selectedDateLabelIndex = i; // for last DateLabel
        }

        return selectedDateLabelIndex;
    }

    private int getImageIndexFromYPos(float x, float yPos, int selectedDateLabelIndex) {
        GalleryObject dateLabelObject = mGalleryObjects.getObject(selectedDateLabelIndex);
        float imageStartOffset = dateLabelObject.getTop() - mDateLabelHeight - mSpacing;
        float yDistFromDateLabel = imageStartOffset - yPos;
        if (yDistFromDateLabel < 0) {
            yDistFromDateLabel = 0f;
        }

        int row = (int) (yDistFromDateLabel / (mColumnWidth + mSpacing));
        int column = (int) (x / (mColumnWidth + mSpacing));

        return mNumOfColumns * row + column;
    }

    float getNextTranslateY() {
        float top = mCenterObject.getNextTop() - (mColumnWidth * 0.5f) + mFocusY + mCenterObject.getNextStartOffsetY();
        float translateY = mAlbumViewGestureDetector.getTranslateY(top, mNextBottom);
        return translateY;
    }

    ImageObject getImageObject(ImageIndexingInfo imageIndexingInfo) {
        return mGalleryObjects.getImageObject(imageIndexingInfo);
    }

    void adjustViewport(float translateY) {
        mAlbumViewGestureDetector.adjustViewport(translateY);
    }

    DateLabelObject getDateLabelObject(int index) {
        return mGalleryObjects.getObject(index);
    }

    void cancelLoading() {
        mGalleryObjects.cancelLoading();
    }

    void deleteDateLabel(int index) {
        mGalleryObjects.deleteDateLabel(index);
        mGridInfo.deleteDateLabelInfo();
    }

    void deleteImage(ImageIndexingInfo indexingInfo) {
        mGalleryObjects.deleteImage(indexingInfo);
        mGridInfo.deleteImageInfo();
    }

    private GLESNodeListener mImageNodeListener = new GLESNodeListener() {
        @Override
        public void update(GLESNode node) {
            GLESTransform transform = node.getTransform();

            transform.setIdentity();

            float angle = mGridInfo.getRotateX();
            transform.rotate(angle, 1f, 0f, 0f);

            float translateZ = mGridInfo.getTranslateZ();
            transform.translate(0f, 0f, translateZ);

            float scrollDistance = mGridInfo.getTranslateY();
            transform.preTranslate(0f, scrollDistance, 0f);
        }
    };

    class ScaleAnimatorCallback implements GLESAnimatorCallback {
        ScaleAnimatorCallback() {
        }

        @Override
        public void onAnimation(GLESVector3 current) {
            mBottom = mPrevBottom + (mNextBottom - mPrevBottom) * current.getX();
            mGalleryObjects.onAnimation(current.getX());
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
