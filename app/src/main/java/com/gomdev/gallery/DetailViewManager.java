package com.gomdev.gallery;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import com.gomdev.gles.GLESAnimator;
import com.gomdev.gles.GLESAnimatorCallback;
import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESNodeListener;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESObjectListener;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVector3;
import com.gomdev.gles.GLESVertexInfo;

import java.nio.FloatBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by gomdev on 15. 2. 17..
 */
public class DetailViewManager implements GridInfoChangeListener, ViewManager, ImageLoadingListener {
    static final String CLASS = "DetailViewManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private static final int SWIPE_FLING_DURATION = 100;
    private static final int SWIPE_SCROLLING_DURATION = 300;

    private static final int NUM_OF_DETAIL_OBJECTS = 3;
    private static final int NUM_OF_RESERVED_TEXTURE_MAPPING_INFO = 2;
    private static final int MIN_FLING_VELOCITY = 400;  // dips
    private static final int MIN_DISTANCE_FOR_FLING = 25; // dips

    private enum FOCUS_DIRECTION {
        LEFT,
        RIGHT,
        NONE
    }

    private final Context mContext;
    private final GridInfo mGridInfo;

    private Handler mHandler = null;

    private GallerySurfaceView mSurfaceView = null;
    private ImageListRenderer mRenderer = null;

    private GalleryContext mGalleryContext = null;
    private ImageManager mImageManager = null;

    private GLESNode mViewPagerNode = null;
    private ImageObject mBGObject = null;
    private GLESTexture mDummyTexture = null;

    private GLESAnimator mSelectionAnimator = null;
    private ImageObject mSelectedImageObject = null;
    private DateLabelInfo mSelectedDateLabelInfo = null;

    private TextureMappingInfo[] mTextureMappingInfos = new TextureMappingInfo[NUM_OF_DETAIL_OBJECTS];

    private boolean mIsFirstImage = false;
    private boolean mIsLastImage = false;

    private ImageIndexingInfo mCurrentImageIndexingInfo = null;

    private Queue<GalleryTexture> mWaitingTextures = new ConcurrentLinkedQueue<>();

    private boolean mIsFinishing = false;

    private float mNormalizedValue = 0f;

    private GLESShader mTextureShader = null;
    private GLESShader mColorShader = null;

    private int mWidth = 0;
    private int mHeight = 0;

    private int mRequestWidth = 0;
    private int mRequestHeight = 0;

    private int mColumnWidth = 0;

    private float mSrcX = 0f;
    private float mSrcY = 0f;

    private float mMinS = 0f;
    private float mMinT = 0f;
    private float mMaxS = 1f;
    private float mMaxT = 1f;

    private int mPrevIndex = 0;
    private int mCurrentIndex = 1;
    private int mNextIndex = 2;

    // touch
    private boolean mIsDown = false;
    private float mDownX = 0f;
    private float mDragDistance = 0f;

    private boolean mIsOnSelectionAnimation = false;
    private boolean mIsOnSwipeAnimation = false;

    private Scroller mScroller = null;
    private VelocityTracker mVelocityTracker = null;
    private GestureDetector mGestureDetector = null;

    private int mMinFlingVelocity = 0;
    private int mMinDistanceForFling = 0;
    private int mMaxFlingVelocity = 0;

    private FOCUS_DIRECTION mFocusDirection = FOCUS_DIRECTION.NONE;

    private TextureMappingInfo[] mReservedTextureMappingInfo = new TextureMappingInfo[2];
    private int mReservedIndex = 3;

    DetailViewManager(Context context, GridInfo gridInfo) {
        if (DEBUG) {
            Log.d(TAG, "DetailViewManager()");
        }

        mContext = context;
        mGridInfo = gridInfo;

        init();
    }

    private void init() {
        setGridInfo(mGridInfo);
        clear();
        reset();

        mGalleryContext = GalleryContext.getInstance();
        mImageManager = ImageManager.getInstance();

        mSelectionAnimator = new GLESAnimator(mSelectionAnimatorCB);
        mSelectionAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mSelectionAnimator.setDuration(GalleryConfig.SELECTION_ANIMATION_START_OFFSET, GalleryConfig.SELECTION_ANIMATION_END_OFFSET);

        mScroller = new Scroller(mContext, new DecelerateInterpolator());
        mGestureDetector = new GestureDetector(mContext, mGestureListener);

        final ViewConfiguration configuration = ViewConfiguration.get(mContext);
        final float density = mContext.getResources().getDisplayMetrics().density;
        mMinFlingVelocity = (int) (MIN_FLING_VELOCITY * density);
        mMinDistanceForFling = (int) (MIN_DISTANCE_FOR_FLING * density);
        mMaxFlingVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    private void setGridInfo(GridInfo gridInfo) {
        mColumnWidth = mGridInfo.getColumnWidth();

        gridInfo.addListener(this);
    }

    private void clear() {
        mWaitingTextures.clear();
    }

    private void reset() {
        mIsFirstImage = false;
        mIsLastImage = false;

        mPrevIndex = 0;
        mCurrentIndex = 1;
        mNextIndex = 2;
    }

    // rendering

    @Override
    public void update() {
        GalleryTexture texture = mWaitingTextures.poll();

        if (texture != null) {
            int index = texture.getIndex();
            final Bitmap bitmap = texture.getBitmapDrawable().getBitmap();
            texture.load(bitmap);
            bitmap.recycle();

            TextureMappingInfo textureMappingInfo = null;
            if (index == mReservedIndex) {
                textureMappingInfo = mReservedTextureMappingInfo[0];
            } else {
                textureMappingInfo = mTextureMappingInfos[index];
            }
            ImageObject object = (ImageObject) textureMappingInfo.getObject();

            object.setTexture(texture.getTexture());
        }

        if (mWaitingTextures.size() > 0) {
            mSurfaceView.requestRender();
        }
    }

    @Override
    public void updateAnimation() {
        if (mSelectionAnimator.doAnimation() == true) {
            mSurfaceView.requestRender();
        }

        if (mScroller.isFinished() == false) {
            mSurfaceView.requestRender();
        } else {
            if (mIsOnSwipeAnimation == true) {
                changePosition();
                mDragDistance = 0f;

                Message msg = mHandler.obtainMessage(ImageListActivity.UPDATE_ACTION_BAR_TITLE);
                BucketInfo bucketInfo = mImageManager.getBucketInfo(mCurrentImageIndexingInfo.mBucketIndex);
                mSelectedDateLabelInfo = bucketInfo.get(mCurrentImageIndexingInfo.mDateLabelIndex);

                msg.obj = mSelectedDateLabelInfo.getDate();
                mHandler.sendMessage(msg);
            }
            mIsOnSwipeAnimation = false;
        }
    }

    private void changePosition() {
        if (mFocusDirection == FOCUS_DIRECTION.LEFT) {
            mReservedTextureMappingInfo[1] = mTextureMappingInfos[mNextIndex];
            if (mReservedTextureMappingInfo[1].getTexture() != null) {
                mReservedTextureMappingInfo[1].getTexture().setIndex(mReservedIndex);
            }

            mTextureMappingInfos[mNextIndex] = mTextureMappingInfos[mCurrentIndex];
            mTextureMappingInfos[mNextIndex].getTexture().setIndex(mNextIndex);

            mTextureMappingInfos[mCurrentIndex] = mTextureMappingInfos[mPrevIndex];
            mTextureMappingInfos[mCurrentIndex].getTexture().setIndex(mCurrentIndex);

            mTextureMappingInfos[mPrevIndex] = mReservedTextureMappingInfo[0];
            mTextureMappingInfos[mPrevIndex].getTexture().setIndex(mPrevIndex);

            mReservedTextureMappingInfo[0] = mReservedTextureMappingInfo[1];
            if (mReservedTextureMappingInfo[0].getTexture() != null) {
                mReservedTextureMappingInfo[0].getTexture().setIndex(mReservedIndex);
            }
        } else if (mFocusDirection == FOCUS_DIRECTION.RIGHT) {
            mReservedTextureMappingInfo[1] = mTextureMappingInfos[mPrevIndex];
            if (mReservedTextureMappingInfo[1].getTexture() != null) {
                mReservedTextureMappingInfo[1].getTexture().setIndex(mReservedIndex);
            }

            mTextureMappingInfos[mPrevIndex] = mTextureMappingInfos[mCurrentIndex];
            mTextureMappingInfos[mPrevIndex].getTexture().setIndex(mPrevIndex);

            mTextureMappingInfos[mCurrentIndex] = mTextureMappingInfos[mNextIndex];
            mTextureMappingInfos[mCurrentIndex].getTexture().setIndex(mCurrentIndex);

            mTextureMappingInfos[mNextIndex] = mReservedTextureMappingInfo[0];
            mTextureMappingInfos[mNextIndex].getTexture().setIndex(mNextIndex);

            mReservedTextureMappingInfo[0] = mReservedTextureMappingInfo[1];
            if (mReservedTextureMappingInfo[0].getTexture() != null) {
                mReservedTextureMappingInfo[0].getTexture().setIndex(mReservedIndex);
            }
        }

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos[i];
            ImageObject object = (ImageObject) textureMappingInfo.getObject();

            if (i == mPrevIndex) {
                object.setTranslate(-mWidth, 0f);
            } else if (i == mCurrentIndex) {
                object.setTranslate(0f, 0f);
            } else if (i == mNextIndex) {
                object.setTranslate(mWidth, 0f);
            } else {
                object.setTranslate(0f, mHeight);
            }
            object.setScale(1.0f);
        }

        mReservedTextureMappingInfo[0].getObject().setTranslate(0f, mHeight);
        mReservedTextureMappingInfo[1].getObject().setTranslate(0f, mHeight);

        if (DEBUG) {
            Log.d(TAG, "changePosition()");
            Log.d(TAG, "\t mPrevIndex=" + mPrevIndex);
            Log.d(TAG, "\t mCurrentIndex=" + mCurrentIndex);
            Log.d(TAG, "\t mNextIndex=" + mNextIndex);
        }
    }

    // onSurfaceChanged

    void onSurfaceChanged(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged()");
        }

        mWidth = width;
        mHeight = height;

        mRequestWidth = mWidth / 2;
        mRequestHeight = mHeight / 2;
    }

    // onSurfaceCreated

    void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        mDummyTexture = GalleryUtils.createDummyTexture(Color.DKGRAY);

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            mTextureMappingInfos[i].getObject().setShader(mTextureShader);
        }
        mReservedTextureMappingInfo[0].getObject().setShader(mTextureShader);
        mReservedTextureMappingInfo[1].getObject().setShader(mTextureShader);

        mBGObject.setShader(mColorShader);
    }

    void setupObjects(GLESCamera camera) {
        if (DEBUG) {
            Log.d(TAG, "setupObjects()");
        }

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos[i];
            ImageObject object = (ImageObject) textureMappingInfo.getObject();
            object.setCamera(camera);

            GLESVertexInfo vertexInfo = object.getVertexInfo();

            float[] position = GLESUtils.makePositionCoord(-mWidth * 0.5f - mWidth + mWidth * i, mHeight * 0.5f, mWidth, mHeight);
            vertexInfo.setBuffer(mTextureShader.getPositionAttribIndex(), position, 3);

            float[] texCoord = GLESUtils.makeTexCoord(0f, 0f, 1f, 1f);
            vertexInfo.setBuffer(mTextureShader.getTexCoordAttribIndex(), texCoord, 2);
        }

        for (int i = 0; i < NUM_OF_RESERVED_TEXTURE_MAPPING_INFO; i++) {
            ImageObject object = (ImageObject) mReservedTextureMappingInfo[i].getObject();
            object.setCamera(camera);

            GLESVertexInfo vertexInfo = object.getVertexInfo();

            float[] position = GLESUtils.makePositionCoord(-mWidth * 0.5f, mHeight * 0.5f, mWidth, mHeight);
            vertexInfo.setBuffer(mTextureShader.getPositionAttribIndex(), position, 3);

            float[] texCoord = GLESUtils.makeTexCoord(0f, 0f, 1f, 1f);
            vertexInfo.setBuffer(mTextureShader.getTexCoordAttribIndex(), texCoord, 2);

            object.setTranslate(0f, mHeight);
            object.setScale(1.0f);
        }

        {
            mBGObject.setCamera(camera);

            GLESVertexInfo vertexInfo = GalleryUtils.createColorVertexInfo(mColorShader,
                    -mWidth * 0.5f, mHeight * 0.5f,
                    mWidth, mHeight,
                    0f, 0f, 0f, 1f);

            vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
            vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);

            mBGObject.setVertexInfo(vertexInfo, false, false);
        }

        if (mCurrentImageIndexingInfo != null) {
            loadDetailTextures();
        }
    }

    private void loadDetailTextures() {
        loadDetailTexture(mCurrentIndex, mCurrentImageIndexingInfo);

        ImageIndexingInfo next = getNextImageIndexingInfo(mCurrentImageIndexingInfo);
        if (next != null) {
            loadDetailTexture(mNextIndex, next);

            mIsLastImage = false;
        } else {
            mIsLastImage = true;
        }

        ImageIndexingInfo prev = getPrevImageIndexingInfo(mCurrentImageIndexingInfo);

        if (prev != null) {
            loadDetailTexture(mPrevIndex, prev);

            mIsFirstImage = false;
        } else {
            mIsFirstImage = true;
        }
    }

    void onResume() {
        if (DEBUG) {
            Log.d(TAG, "onResume()");
        }
    }

    void onPause() {
        if (DEBUG) {
            Log.d(TAG, "onPause()");
        }
    }

    // touch

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final int action = MotionEventCompat.getActionMasked(event);

        if (action == MotionEvent.ACTION_DOWN) {
            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
        }
        mVelocityTracker.addMovement(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mIsOnSelectionAnimation == false || mIsOnSwipeAnimation == false) {
                    mIsDown = true;
                    mDownX = event.getX();
                    mDragDistance = 0f;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDown == true) {
                    handleAnimation();
                    mIsDown = false;
                }

                if (mVelocityTracker != null) {
                    mVelocityTracker.clear();
                    mVelocityTracker = null;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsDown == true) {

                    mDragDistance = event.getX() - mDownX;
                    if (mIsFirstImage == true && mDragDistance >= 0) {
                        mDragDistance = 0f;
                    }

                    if (mIsLastImage == true && mDragDistance <= 0) {
                        mDragDistance = 0f;
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
        }

        mGestureDetector.onTouchEvent(event);

        mSurfaceView.requestRender();

        return true;
    }

    private void handleAnimation() {
        mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
        int initialVelocity = (int) mVelocityTracker.getXVelocity();

        if (Math.abs(initialVelocity) < mMinFlingVelocity) {
            handleScrollAnimation();
        } else {
            handleFlingAnimation(initialVelocity);
        }

        mIsOnSwipeAnimation = true;

        prePopulate();
    }

    private void handleScrollAnimation() {
        if (Math.abs(mDragDistance) > mWidth * 0.5f) {
            if (mDragDistance > 0) {
                mFocusDirection = FOCUS_DIRECTION.LEFT;
                mScroller.startScroll((int) mDragDistance, 0, (int) (mWidth - mDragDistance), 0, SWIPE_SCROLLING_DURATION);
            } else {
                mFocusDirection = FOCUS_DIRECTION.RIGHT;
                mScroller.startScroll((int) mDragDistance, 0, (int) (-mWidth - mDragDistance), 0, SWIPE_SCROLLING_DURATION);
            }
        } else {
            mFocusDirection = FOCUS_DIRECTION.NONE;
            mScroller.startScroll((int) mDragDistance, 0, (int) -mDragDistance, 0, SWIPE_SCROLLING_DURATION);
        }
    }

    private void handleFlingAnimation(int initialVelocity) {
        if (Math.abs(mDragDistance) > mMinDistanceForFling) {
            if (mDragDistance > 0) {
                mFocusDirection = FOCUS_DIRECTION.LEFT;
//                mScroller.fling((int) mDragDistance, 0, initialVelocity, 0, mWidth, mWidth, 0, 0);
                mScroller.startScroll((int) mDragDistance, 0, (int) (mWidth - mDragDistance), 0, SWIPE_FLING_DURATION);
            } else {
                mFocusDirection = FOCUS_DIRECTION.RIGHT;
//                mScroller.fling((int) mDragDistance, 0, initialVelocity, 0, -mWidth, -mWidth, 0, 0);
                mScroller.startScroll((int) mDragDistance, 0, (int) (-mWidth - mDragDistance), 0, SWIPE_FLING_DURATION);
            }
        } else {
            mFocusDirection = FOCUS_DIRECTION.NONE;
//            mScroller.fling((int) mDragDistance, 0, initialVelocity, 0, 0, 0, 0, 0);
            mScroller.startScroll((int) mDragDistance, 0, (int) -mDragDistance, 0, SWIPE_FLING_DURATION);
        }
    }

    private void prePopulate() {
        if (mFocusDirection == FOCUS_DIRECTION.LEFT) {
            mCurrentImageIndexingInfo = getPrevImageIndexingInfo(mCurrentImageIndexingInfo);
            ImageIndexingInfo prev = getPrevImageIndexingInfo(mCurrentImageIndexingInfo);
            if (prev != null) {
                loadDetailTexture(mReservedIndex, prev);

                mIsFirstImage = false;
            } else {
                mIsFirstImage = true;
            }

            mIsLastImage = false;
        } else if (mFocusDirection == FOCUS_DIRECTION.RIGHT) {
            mCurrentImageIndexingInfo = getNextImageIndexingInfo(mCurrentImageIndexingInfo);
            ImageIndexingInfo next = getNextImageIndexingInfo(mCurrentImageIndexingInfo);
            if (next != null) {
                loadDetailTexture(mReservedIndex, next);

                mIsLastImage = false;
            } else {
                mIsLastImage = true;
            }

            mIsFirstImage = false;
        }
    }

    // show / hide
    void show() {
        if (DEBUG) {
            if (mTextureMappingInfos[mCurrentIndex].getObject().getVisibility() == false) {
                Log.d(TAG, "show()");
            }
        }

        mBGObject.show();

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            mTextureMappingInfos[i].getObject().show();
        }

        for (int i = 0; i < NUM_OF_RESERVED_TEXTURE_MAPPING_INFO; i++) {
            mReservedTextureMappingInfo[i].getObject().show();
        }
    }

    void hide() {
        if (DEBUG) {
            Log.d(TAG, "hide()");
        }

        mBGObject.hide();

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            mTextureMappingInfos[i].getObject().hide();
        }

        for (int i = 0; i < NUM_OF_RESERVED_TEXTURE_MAPPING_INFO; i++) {
            mReservedTextureMappingInfo[i].getObject().hide();
        }
    }


    // initialization

    void createScene(GLESNode node) {
        if (DEBUG) {
            Log.d(TAG, "createScene()");
        }

        GLESGLState glState = new GLESGLState();
        glState.setCullFaceState(true);
        glState.setCullFace(GLES20.GL_BACK);
        glState.setBlendState(true);
        glState.setBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        {
            mBGObject = new ImageObject("BG");
            node.addChild(mBGObject);

            mBGObject.setGLState(glState);
            mBGObject.setListener(mBGObjectListener);
            mBGObject.hide();
        }

        mViewPagerNode = new GLESNode("ViewPagerNode");
        node.addChild(mViewPagerNode);
        mViewPagerNode.setListener(mViewPagerNodeListener);


        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            ImageObject object = new ImageObject("DetailObject");
            mViewPagerNode.addChild(object);

            GLESVertexInfo vertexInfo = new GLESVertexInfo();
            vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
            vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);
            object.setVertexInfo(vertexInfo, false, false);

            object.setGLState(glState);
            object.setListener(mDetailImageObjectListener);
            object.setIndex(i);
            object.hide();

            mTextureMappingInfos[i] = new TextureMappingInfo(object);
        }

        for (int i = 0; i < NUM_OF_RESERVED_TEXTURE_MAPPING_INFO; i++) {
            ImageObject reservedObject = new ImageObject("reservedObject_" + i);
            mViewPagerNode.addChild(reservedObject);

            GLESVertexInfo vertexInfo = new GLESVertexInfo();
            vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
            vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);
            reservedObject.setVertexInfo(vertexInfo, false, false);

            reservedObject.setGLState(glState);
            reservedObject.setListener(mDetailImageObjectListener);
            reservedObject.setIndex(mReservedIndex);
            reservedObject.hide();

            mReservedTextureMappingInfo[i] = new TextureMappingInfo(reservedObject);
        }
    }

    public void setColorShader(GLESShader colorShader) {
        if (DEBUG) {
            Log.d(TAG, "setColorShader()");
        }

        mColorShader = colorShader;
    }

    public void setTextureShader(GLESShader textureShader) {
        if (DEBUG) {
            Log.d(TAG, "setTextureShader()");
        }

        mTextureShader = textureShader;
    }

    void setSurfaceView(GallerySurfaceView surfaceView) {
        if (DEBUG) {
            Log.d(TAG, "setSurfaceView()");
        }

        mSurfaceView = surfaceView;
        mRenderer = mSurfaceView.getRenderer();
    }

    // touch

    // listener / callback

    @Override
    public void onColumnWidthChanged() {
        if (DEBUG) {
            Log.d(TAG, "onColumnWidthChanged()");
        }

        mColumnWidth = mGridInfo.getColumnWidth();
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
    }

    @Override
    public void onImageLoaded(int index, GalleryTexture texture) {
        if (DEBUG) {
            Log.d(TAG, "onImageLoaded() index=" + index);
        }

        mWaitingTextures.add(texture);

        mSurfaceView.requestRender();
    }


    void onImageSelected(ImageObject selectedImageObject) {
        if (DEBUG) {
            Log.d(TAG, "onImageSelected()");
        }

        reset();

        mSelectedImageObject = selectedImageObject;
        mCurrentImageIndexingInfo = mGalleryContext.getImageIndexingInfo();
        BucketInfo bucketInfo = mImageManager.getBucketInfo(mCurrentImageIndexingInfo.mBucketIndex);
        mSelectedDateLabelInfo = bucketInfo.get(mCurrentImageIndexingInfo.mDateLabelIndex);

        setupSelectionStartingAnimationInfo();

        loadDetailTextures();

        TextureMappingInfo textureMappingInfo = mTextureMappingInfos[mCurrentIndex];
        ImageObject object = (ImageObject) textureMappingInfo.getObject();
        object.setTexture(mSelectedImageObject.getTexture());

        mSelectionAnimator.setValues(0f, 1f);
        mSelectionAnimator.cancel();
        mSelectionAnimator.start();

        mIsOnSelectionAnimation = true;

        mSurfaceView.requestRender();
    }

    private void setupSelectionStartingAnimationInfo() {
        mCurrentImageIndexingInfo = mGalleryContext.getImageIndexingInfo();
        ImageInfo imageInfo = mImageManager.getImageInfo(mCurrentImageIndexingInfo);

        RectF viewport = mGalleryContext.getCurrentViewport();

        mSrcX = mSelectedImageObject.getTranslateX();
        mSrcY = mHeight * 0.5f - (viewport.bottom - mSelectedImageObject.getTranslateY());

        TextureMappingInfo textureMappingInfo = mTextureMappingInfos[mCurrentIndex];
        ImageObject object = (ImageObject) textureMappingInfo.getObject();

        GLESVertexInfo vertexInfo = object.getVertexInfo();
        float[] vertex = GLESUtils.makePositionCoord(-mWidth * 0.5f, mWidth * 0.5f, mWidth, mWidth);
        vertexInfo.setBuffer(mTextureShader.getPositionAttribIndex(), vertex, 3);

        calcTexCoordInfo(imageInfo);

        float[] texCoord = GLESUtils.makeTexCoord(mMinS, mMinT, mMaxS, mMaxT);
        vertexInfo.setBuffer(mTextureShader.getTexCoordAttribIndex(), texCoord, 2);

        mTextureMappingInfos[mPrevIndex].getObject().setTranslate(-mWidth, 0);
        mTextureMappingInfos[mNextIndex].getObject().setTranslate(mWidth, 0);
    }

    private ImageIndexingInfo getPrevImageIndexingInfo(ImageIndexingInfo current) {
        ImageIndexingInfo imageIndexingInfo = new ImageIndexingInfo();
        imageIndexingInfo.mBucketIndex = current.mBucketIndex;

        if (current.mImageIndex == 0) {
            if (current.mDateLabelIndex == 0) {
                return null;
            } else {
                imageIndexingInfo.mDateLabelIndex = current.mDateLabelIndex - 1;

                BucketInfo bucketInfo = mImageManager.getBucketInfo(current.mBucketIndex);
                DateLabelInfo dateLabelInfo = bucketInfo.get(imageIndexingInfo.mDateLabelIndex);
                int numOfImages = dateLabelInfo.getNumOfImages();

                imageIndexingInfo.mImageIndex = numOfImages - 1;
            }
        } else {
            imageIndexingInfo.mDateLabelIndex = current.mDateLabelIndex;
            imageIndexingInfo.mImageIndex = current.mImageIndex - 1;
        }

        return imageIndexingInfo;
    }

    private ImageIndexingInfo getNextImageIndexingInfo(ImageIndexingInfo current) {
        ImageIndexingInfo imageIndexingInfo = new ImageIndexingInfo();
        imageIndexingInfo.mBucketIndex = current.mBucketIndex;

        BucketInfo bucketInfo = mImageManager.getBucketInfo(current.mBucketIndex);
        DateLabelInfo dateLabelInfo = bucketInfo.get(current.mDateLabelIndex);
        int lastIndex = dateLabelInfo.getNumOfImages() - 1;

        if (current.mImageIndex == lastIndex) {
            int lastDateLabelIndex = bucketInfo.getNumOfDateInfos() - 1;
            if (current.mDateLabelIndex == lastDateLabelIndex) {
                return null;
            } else {
                imageIndexingInfo.mDateLabelIndex = current.mDateLabelIndex + 1;
                imageIndexingInfo.mImageIndex = 0;
            }
        } else {
            imageIndexingInfo.mDateLabelIndex = current.mDateLabelIndex;
            imageIndexingInfo.mImageIndex = current.mImageIndex + 1;
        }

        return imageIndexingInfo;
    }

    private void loadDetailTexture(int index, ImageIndexingInfo imageIndexingInfo) {
        ImageInfo imageInfo = ImageManager.getInstance().getImageInfo(imageIndexingInfo);

        TextureMappingInfo textureMappingInfo = null;
        if (index == mReservedIndex) {
            textureMappingInfo = mReservedTextureMappingInfo[0];
        } else {
            textureMappingInfo = mTextureMappingInfos[index];
        }
        textureMappingInfo.setGalleryInfo(imageInfo);

        GalleryTexture texture = new GalleryTexture(imageInfo.getWidth(), imageInfo.getHeight());
        texture.setIndex(index);
        texture.setImageLoadingListener(this);
        textureMappingInfo.setTexture(texture);

        setPositionCoord(index, imageInfo);

        ImageObject object = (ImageObject) textureMappingInfo.getObject();
        object.setTexture(mDummyTexture);

        ImageLoader.getInstance().loadBitmap(imageInfo, texture, mRequestWidth, mRequestHeight);
    }

    private void setPositionCoord(int index, ImageInfo imageInfo) {
        int imageWidth = imageInfo.getWidth();
        int imageHeight = imageInfo.getHeight();

        float top = ((float) imageHeight / imageWidth) * mWidth * 0.5f;
        float right = mWidth * 0.5f;

        if (top > mHeight * 0.5f) {
            top = mHeight * 0.5f;
            right = ((float) imageWidth / imageHeight) * mHeight * 0.5f;
        }

        if (DEBUG) {
            Log.d(TAG, "setPositionCoord() index=" + index);
            Log.d(TAG, "\t top=" + top + " right=" + right);
        }

        TextureMappingInfo textureMappingInfo = null;
        if (index == mReservedIndex) {
            textureMappingInfo = mReservedTextureMappingInfo[0];
        } else {
            textureMappingInfo = mTextureMappingInfos[index];
        }

        ImageObject object = (ImageObject) textureMappingInfo.getObject();
        FloatBuffer positionBuffer = (FloatBuffer) object.getVertexInfo().getBuffer(mTextureShader.getPositionAttribIndex());

        positionBuffer.put(0, -right);
        positionBuffer.put(1, -top);

        positionBuffer.put(3, right);
        positionBuffer.put(4, -top);

        positionBuffer.put(6, -right);
        positionBuffer.put(7, top);

        positionBuffer.put(9, right);
        positionBuffer.put(10, top);
    }

    private void calcTexCoordInfo(ImageInfo imageInfo) {
        float imageWidth = imageInfo.getWidth();
        float imageHeight = imageInfo.getHeight();

        if (imageWidth > imageHeight) {
            mMinS = ((imageWidth - imageHeight) * 0.5f) / imageWidth;
            mMaxS = 1f - mMinS;

            mMinT = 0f;
            mMaxT = 1f;
        } else {
            mMinT = ((imageHeight - imageWidth) * 0.5f) / imageHeight;
            mMaxT = 1f - mMinT;

            mMinS = 0f;
            mMaxS = 1f;
        }
    }

    void finish() {
        if (DEBUG) {
            Log.d(TAG, "finish()");
        }

        setupSelectionFinishingAnimationInfo();

        if (mSelectionAnimator.isFinished() == false) {
            mSelectionAnimator.cancel();
            mSelectionAnimator.setValues(mNormalizedValue, 0f);
        } else {
            mSelectionAnimator.setValues(1f, 0f);
        }

        mSelectionAnimator.start();

        mIsOnSelectionAnimation = true;

        mIsFinishing = true;

        mSurfaceView.requestRender();
    }

    private void setupSelectionFinishingAnimationInfo() {
        ImageObject object = mRenderer.getObjectFromAlbumView(mCurrentImageIndexingInfo);
        RectF viewport = mGalleryContext.getCurrentViewport();

        mSrcX = object.getTranslateX();
        mSrcY = mHeight * 0.5f - (viewport.bottom - object.getTranslateY());
        float translateY = 0f;
        float bottomY = (-mHeight * 0.5f + mColumnWidth * 0.5f);
        float topY = (mHeight * 0.5f - mColumnWidth * 0.5f);
        if (mSrcY < bottomY) {
            translateY = bottomY - mSrcY;
            mSrcY = bottomY;
        } else if (mSrcY > topY) {
            translateY = topY - mSrcY;
            mSrcY = topY;
        }

        mRenderer.adjustAlbumView(translateY);
    }

    void setHandler(Handler handler) {
        mHandler = handler;
    }

    // member class

    private GLESObjectListener mDetailImageObjectListener = new GLESObjectListener() {
        @Override
        public void update(GLESObject object) {
            ImageObject imageObject = (ImageObject) object;
            GLESTransform transform = object.getTransform();
            transform.setIdentity();

            float x = imageObject.getTranslateX();
            float y = imageObject.getTranslateY();
            float scale = imageObject.getScale();

            transform.setTranslate(x, y, 0f);
            transform.setScale(scale);
        }

        @Override
        public void apply(GLESObject object) {
        }
    };

    private GLESObjectListener mBGObjectListener = new GLESObjectListener() {
        @Override
        public void update(GLESObject object) {
        }

        @Override
        public void apply(GLESObject object) {
            int location = GLES20.glGetUniformLocation(object.getShader().getProgram(), "uAlpha");
            GLES20.glUniform1f(location, ((ImageObject) object).getAlpha());
        }
    };

    private final GLESAnimatorCallback mSelectionAnimatorCB = new GLESAnimatorCallback() {
        @Override
        public void onAnimation(GLESVector3 current) {
            float normalizedValue = current.getX();
            mNormalizedValue = normalizedValue;

            updateDetailViewObject(normalizedValue);

            updateBGObject(normalizedValue);

            show();
        }

        private void updateDetailViewObject(float normalizedValue) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos[mCurrentIndex];
            ImageObject object = (ImageObject) textureMappingInfo.getObject();
            object.show();

            float x = mSrcX + (0f - mSrcX) * normalizedValue;
            float y = mSrcY + (0f - mSrcY) * normalizedValue;

            float fromScale = (float) mColumnWidth / mWidth;
            float scale = fromScale + (1f - fromScale) * normalizedValue;

            object.setTranslate(x, y);
            object.setScale(scale);

            GLESVertexInfo vertexInfo = object.getVertexInfo();
            FloatBuffer positionBuffer = (FloatBuffer) vertexInfo.getBuffer(mTextureShader.getPositionAttribIndex());

            ImageInfo imageInfo = mImageManager.getImageInfo(mCurrentImageIndexingInfo);

            int imageWidth = imageInfo.getWidth();
            int imageHeight = imageInfo.getHeight();

            float prevTop = mWidth * 0.5f;
            float nextTop = 0f;

            float prevRight = mWidth * 0.5f;
            float nextRight = 0f;

            nextTop = ((float) imageHeight / imageWidth) * mWidth * 0.5f;
            nextRight = prevRight;

            if (nextTop > mHeight * 0.5f) {
                nextTop = mHeight * 0.5f;
                nextRight = ((float) imageWidth / imageHeight) * mHeight * 0.5f;
            }

            float currentTop = prevTop + (nextTop - prevTop) * normalizedValue;
            float currentRight = prevRight + (nextRight - prevRight) * normalizedValue;

            positionBuffer.put(0, -currentRight);
            positionBuffer.put(1, -currentTop);

            positionBuffer.put(3, currentRight);
            positionBuffer.put(4, -currentTop);

            positionBuffer.put(6, -currentRight);
            positionBuffer.put(7, currentTop);

            positionBuffer.put(9, currentRight);
            positionBuffer.put(10, currentTop);

            float currentMinS = mMinS + (0f - mMinS) * normalizedValue;
            float currentMinT = mMinT + (0f - mMinT) * normalizedValue;

            float currentMaxS = mMaxS + (1f - mMaxS) * normalizedValue;
            float currentMaxT = mMaxT + (1f - mMaxT) * normalizedValue;

            FloatBuffer texBuffer = (FloatBuffer) object.getVertexInfo().getBuffer(mTextureShader.getTexCoordAttribIndex());

            texBuffer.put(0, currentMinS);
            texBuffer.put(1, currentMaxT);

            texBuffer.put(2, currentMaxS);
            texBuffer.put(3, currentMaxT);

            texBuffer.put(4, currentMinS);
            texBuffer.put(5, currentMinT);

            texBuffer.put(6, currentMaxS);
            texBuffer.put(7, currentMinT);
        }

        private void updateBGObject(float normalizedValue) {
            mBGObject.show();
            mBGObject.setAlpha(normalizedValue);
        }

        @Override
        public void onCancel() {

        }

        @Override
        public void onFinished() {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos[mCurrentIndex];
            ImageObject object = (ImageObject) textureMappingInfo.getObject();

            object.setScale(1.0f);

            if (mIsFinishing == true) {
                mIsFinishing = false;

                Message msg = mHandler.obtainMessage(ImageListActivity.UPDATE_ACTION_BAR_TITLE);
                BucketInfo bucketInfo = mImageManager.getBucketInfo(mCurrentImageIndexingInfo.mBucketIndex);
                msg.obj = bucketInfo.getName();
                mHandler.sendMessage(msg);

                mRenderer.onFinished();
            }

            mIsOnSelectionAnimation = false;
        }
    };

    private GLESNodeListener mViewPagerNodeListener = new GLESNodeListener() {
        @Override
        public void update(GLESNode node) {
            GLESTransform transform = node.getTransform();
            transform.setIdentity();

            if (mScroller.computeScrollOffset() == true) {
                mDragDistance = mScroller.getCurrX();
            }

            transform.setTranslate(mDragDistance, 0f, 0f);
        }
    };

    private final GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        private boolean mIsShown = false;

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (DEBUG) {
                Log.d(TAG, "onSingleTabUp()");
            }

            if (mIsShown == true) {
                mIsShown = false;
                mHandler.sendEmptyMessage(ImageListActivity.SET_SYSTEM_UI_FLAG_LOW_PROFILE);
            } else {
                mIsShown = true;
                mHandler.sendEmptyMessage(ImageListActivity.SET_SYSTEM_UI_FLAG_VISIBLE);
                ((Activity)mContext).getActionBar().setTitle(mSelectedDateLabelInfo.getDate());
            }
            return true;
        }
    };
}
