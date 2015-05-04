package com.gomdev.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.gomdev.gallery.GalleryConfig.AlbumViewMode;
import com.gomdev.gallery.Scrollbar.ScrollbarMode;
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
    private Handler mHandler = null;

    private GalleryObjects mGalleryObjects = null;
    private Scrollbar mScrollbar = null;
    private GalleryContext mGalleryContext = null;
    private GLESNode mAlbumViewNode = null;

    private GLESShader mTextureShader = null;
    private GLESShader mTextureAlphaShader = null;
    private GLESShader mScrollbarShader = null;

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

    private int mSystemBarHeight = 0;
    private boolean mIsScrollbarSelected = false;

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
        glState.setBlendState(true);
        glState.setBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
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
    public void update(long currentTime) {
        udpateGestureDetector();

        boolean isOnScrolling = mAlbumViewGestureDetector.isOnScrolling();
        if (mIsOnAnimation == true) {
            mAlbumViewGestureDetector.resetOnScrolling();
        }

        mGalleryObjects.updateTexture(currentTime);

        mGalleryObjects.checkVisibility();
        mGalleryObjects.update();
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
    public void updateAnimation(long currentTime) {
        if (mScaleAnimator.doAnimation() == true) {
            mSurfaceView.requestRender();
        }
    }

    // onSurfaceChanged

    void onSurfaceChanged(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged() width=" + width + " height=" + height);
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
        mScrollbar.setShader(mScrollbarShader);

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.check_black_55);
        GLESTexture checkTexture = new GLESTexture.Builder(GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight())
                .load(bitmap);

        mGalleryObjects.onSurfaceCreated();

        mGalleryObjects.setCheckTexture(checkTexture);

        mSystemBarHeight = GalleryUtils.getActionBarHeight(mContext) + GalleryUtils.getStatusBarHeight(mContext);
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

    void setTextureCustomShader(GLESShader shader) {
        mTextureShader = shader;
    }

    void setScrollbarShader(GLESShader shader) {
        mScrollbarShader = shader;
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

    void setHandler(Handler handler) {
        mHandler = handler;
        mAlbumViewGestureDetector.setHandler(handler);
    }

    // resume / pause

    void onResume() {
        if (DEBUG) {
            Log.d(TAG, "onResume()");
        }
    }

    void onPause() {
        if (DEBUG) {
            Log.d(TAG, "onPause()");
        }

        mGalleryObjects.onPause();
    }

    // touch


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mIsOnAnimation == true) {
            return true;
        }

        boolean retVal = false;

        AlbumViewMode albumViewMode = mGalleryContext.getAlbumViewMode();

        if (albumViewMode == AlbumViewMode.NORMAL_MODE) {
            retVal = mAlbumViewScaleGestureDetector.onTouchEvent(event);
        }

        if (mIsOnScale == false) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mIsScrollbarSelected = isScrollbarSelected(event);
                if (mIsScrollbarSelected == true) {
                    mScrollbar.setScrollbarMode(ScrollbarMode.SCROLLBAR_DRAGGING);
                }
            }

            if (mIsScrollbarSelected == true && event.getAction() == MotionEvent.ACTION_UP) {
                mScrollbar.setScrollbarMode(ScrollbarMode.NORMAL);
            }

            mAlbumViewGestureDetector.setScrollbarMode(mScrollbar.getScrollbarMode());
            mAlbumViewGestureDetector.setScrollbarHeight(mScrollbar.getScrollbarHeight());
            retVal = mAlbumViewGestureDetector.onTouchEvent(event) || retVal;
        }

        return retVal;
    }

    private boolean isScrollbarSelected(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        if (pointerCount != 1) {
            return false;
        }

        float x = event.getX();
        float y = event.getY() - mSystemBarHeight;

        x = x - mWidth * 0.5f;

        float padding = mScrollbar.getScrollbarWidth(ScrollbarMode.SCROLLBAR_DRAGGING);

        float scrollbarLeft = mWidth * 0.5f - padding * 2f;
        float scrollbarRight = mWidth * 0.5f;

        float scrollbarHeight = mScrollbar.getScrollbarHeight();
        float scrollbarPosY = -mScrollbar.getScrollbarPosY();
        float scrollbarTop = scrollbarPosY - padding;
        float scrollbarBottom = scrollbarHeight + scrollbarPosY + padding;

        if (x >= scrollbarLeft && x <= scrollbarRight &&
                y >= scrollbarTop && y <= scrollbarBottom) {
            return true;
        }

        return false;
    }


    // listener / callback

    void resize(float focusX, float focusY) {
        if (DEBUG) {
            Log.d(TAG, "resize()");
        }

        mFocusY = focusY;
        ImageIndexingInfo imageIndexingInfo = getNearestImageIndex(focusX, focusY);
        if (DEBUG) {
            Log.d(TAG, "resize() mCenterObject indexing info " + imageIndexingInfo);
        }
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
    public void onImageDeleted() {
        if (DEBUG) {
            Log.d(TAG, "onImageDeleted()");
        }
    }

    @Override
    public void onDateLabelDeleted() {
        if (DEBUG) {
            Log.d(TAG, "onDateLabelDeleted()");
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

    boolean finish() {
        AlbumViewMode albumViewMode = mGalleryContext.getAlbumViewMode();
        if (albumViewMode == AlbumViewMode.NORMAL_MODE) {
            return true;
        }

        mGalleryContext.setAlbumViewMode(AlbumViewMode.NORMAL_MODE);
        mHandler.sendEmptyMessage(ImageListActivity.INVALIDATE_OPTION_MENU);

        mSurfaceView.requestRender();

        return false;
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
    }

    void deleteImage(ImageIndexingInfo indexingInfo) {
        mGalleryObjects.deleteImage(indexingInfo);
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
