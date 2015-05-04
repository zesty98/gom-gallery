package com.gomdev.gallery;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.Scroller;
import android.widget.Toast;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

/**
 * Created by gomdev on 15. 2. 24..
 */
public class DetailViewPager implements GridInfoChangeListener, ImageLoadingListener {
    static final String CLASS = "DetailViewPager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;
    static final boolean DEBUG_TOUCH = GalleryConfig.DEBUG;

    private static final int MAX_SETTLE_DURATION = 600;
    private static final int EDGE_SCROLLING_DURATION = 500;
    private static final long SCALE_ANIMATION_DURATION = 300L;

    private static final int NUM_OF_DETAIL_OBJECTS_AT_ONE_SIDE = 3;
    private static final int NUM_OF_DETAIL_OBJECTS = NUM_OF_DETAIL_OBJECTS_AT_ONE_SIDE * 2 + 1;

    private static final int PREV3_INDEX = 0;
    private static final int PREV2_INDEX = 1;
    private static final int PREV_INDEX = 2;
    private static final int CURRENT_INDEX = 3;
    private static final int NEXT_INDEX = 4;
    private static final int NEXT2_INDEX = 5;
    private static final int NEXT3_INDEX = 6;

    private static final int CURRENT_DETAIL_INDEX = 7;

    private static final int MIN_FLING_VELOCITY = 400;  // dips
    private static final int MIN_DISTANCE_FOR_FLING = 25; // dips

    private static final int MAX_DISTANCE_AT_EDGE = 50; //dips

    private static final float MIN_SCALE = 0.7f;
    private static final float MAX_SCALE = 1.0f;

    private enum FocusDirection {
        LEFT,
        RIGHT,
        NONE
    }

    private enum ScrollState {
        ON_SCROLL,
        ON_FLING,
        STABLE,
        ON_SCALE_ANIMATION,
    }

    private enum DetailViewState {
        FIT_SCREEN,
        ORIGINAL_SIZE_SMALL,
        ORIGINAL_SIZE_LARGE
    }

    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    private final Context mContext;
    private final GridInfo mGridInfo;

    private Handler mHandler = null;

    private GalleryContext mGalleryContext = null;
    private ImageManager mImageManager = null;
    private GallerySurfaceView mSurfaceView = null;
    private ImageListRenderer mRenderer = null;

    private GLESShader mTextureShader = null;
    private GLESTexture mDummyTexture = null;
    private GLESNode mViewPagerNode = null;
    private GLESNode mLargeImageNode = null;

    private DateLabelInfo mSelectedDateLabelInfo = null;
    private ImageIndexingInfo mCurrentImageIndexingInfo = null;

    private List<TextureMappingInfo> mTextureMappingInfos = new LinkedList<>();
    private Queue<GalleryTexture> mWaitingTextures = new ConcurrentLinkedQueue<>();

    private FocusDirection mFocusDirection = FocusDirection.NONE;
    private ScrollState mScrollState = ScrollState.STABLE;
    private DetailViewState mDetailViewState = DetailViewState.FIT_SCREEN;

    private boolean mIsFirstImage = false;
    private boolean mIsLastImage = false;

    private int mWidth = 0;
    private int mHeight = 0;
    private float mScreenRatio = 0f;

    private int mRequestWidth = 0;
    private int mRequestHeight = 0;

    private TextureMappingInfo mDetailTextureMappingInfo = null;
    private ImageObject mDetailObject = null;

    private LargeImage mLargeImage = null;
    private boolean mNeedToLoadLargeImage = false;

    // touch or gesture
    private boolean mIsDown = false;
    private float mDownX = 0f;
    private int mDragDistance = 0;

    private Scroller mScroller = null;
    private VelocityTracker mVelocityTracker = null;
    private GestureDetector mGestureDetector = null;

    private boolean mIsOnSwipeAnimation = false;
    private boolean mIsOnScroll = false;
    private boolean mIsFinished = false;
    private boolean mNeedToPopulate = false;

    private int mMinFlingVelocity = 0;
    private int mMinDistanceForFling = 0;
    private int mMaxFlingVelocity = 0;

    private boolean mIsAtEdge = false;
    private int mMaxDragDistanceAtEdge = 0;

    private boolean mIsActionBarShown = false;

    // scale animation
    private ImageObject mCurrentObject = null;
    private ImageInfo mCurrentImageInfo = null;
    private GLESAnimator mScaleAnimator = null;

    private volatile boolean mIsOnScaleAnimation = false;
    private boolean mIsDetailObjectShown = false;
    private boolean mVisibility = false;

    private float mPrevScale = 1f;
    private float mNextScale = 1f;

    private float mPrevTranslateX = 0f;
    private float mPrevTranslateY = 0f;

    // panning
    private float mPrevX = 0f;
    private float mPrevY = 0f;

    DetailViewPager(Context context, GridInfo gridInfo) {
        mContext = context;
        mGridInfo = gridInfo;

        init();
    }

    private void init() {
        reset();

        mWaitingTextures.clear();

        setGridInfo(mGridInfo);

        mGalleryContext = GalleryContext.getInstance();
        mImageManager = ImageManager.getInstance();

        mScroller = new Scroller(mContext, sInterpolator);
        mGestureDetector = new GestureDetector(mContext, mGestureListener);
        final ViewConfiguration configuration = ViewConfiguration.get(mContext);
        final float density = mContext.getResources().getDisplayMetrics().density;

        mMinFlingVelocity = (int) (MIN_FLING_VELOCITY * density);
        mMaxFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        mMinDistanceForFling = (int) (MIN_DISTANCE_FOR_FLING * density);
        mMaxDragDistanceAtEdge = (int) (MAX_DISTANCE_AT_EDGE * density);

        mLargeImage = new LargeImage(mContext);
    }

    private void reset() {
        mIsFirstImage = false;
        mIsLastImage = false;
        mIsActionBarShown = false;
        mDetailViewState = DetailViewState.FIT_SCREEN;

        mFocusDirection = FocusDirection.NONE;

        mWaitingTextures.clear();
    }

    private void setGridInfo(GridInfo gridInfo) {
        gridInfo.addListener(this);
    }

    // rendering

    void update() {
        if (mIsFinished == true) {
            return;
        }

        if (mNeedToLoadLargeImage == true && mDetailViewState == DetailViewState.ORIGINAL_SIZE_LARGE) {
            mLargeImage.updateTexture();

            mLargeImageNode.show();
            mViewPagerNode.hide();
        }

        GalleryTexture texture = mWaitingTextures.poll();

        if (texture != null) {
            int index = texture.getIndex();
            Bitmap bitmap = texture.getBitmapDrawable().getBitmap();

            if (bitmap == null) {
                int width = mWidth / 10;
                int height = mHeight / 10;

                bitmap = GLESUtils.makeBitmap(width, height, Bitmap.Config.ARGB_8888, Color.DKGRAY);
            }

            if (bitmap.isRecycled() == true) {
                return;
            }

            texture.load(bitmap);
            if (texture.isThumbnail() == false) {
                bitmap.recycle();
            }

            TextureMappingInfo textureMappingInfo = null;
            if (index == CURRENT_DETAIL_INDEX) {
                textureMappingInfo = mDetailTextureMappingInfo;
                if (mIsOnScroll == false) {
                    showDetailObject();
                }
            } else {
                textureMappingInfo = mTextureMappingInfos.get(index);
            }
            ImageObject object = (ImageObject) textureMappingInfo.getObject();

            object.setTexture(texture);
        }

        if (mWaitingTextures.size() > 0) {
            mSurfaceView.requestRender();
        }

        if (mNeedToPopulate == true) {
            mNeedToPopulate = false;
            prePopulate();
        }
    }

    void updateAnimation() {
        if (mIsOnScaleAnimation == true) {
            if (mScaleAnimator.doAnimation() == true) {
                mSurfaceView.requestRender();
            }
            return;
        }

        if (mScroller.isFinished() == false) {
            mSurfaceView.requestRender();

            if (mScroller.computeScrollOffset() == true) {
                mDragDistance = mScroller.getCurrX();
            }
        } else {
            if (mIsOnSwipeAnimation == true) {
                onSwipingFinished();
            }
            mIsOnSwipeAnimation = false;
            mScrollState = ScrollState.STABLE;
        }

        if (mIsOnScroll == true) {
            changeScaleAndAlpha();
        }
    }

    private void changeScaleAndAlpha() {
        float normalizedValue = Math.abs((float) mDragDistance / mWidth);

        float scale = 1f;
        float alpha = 1f;

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            switch (i) {
                case CURRENT_INDEX:
                    scale = MAX_SCALE + (MIN_SCALE - MAX_SCALE) * normalizedValue;
                    alpha = 1f - normalizedValue;
                    break;
                case PREV_INDEX:
                case NEXT_INDEX:
                    scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * normalizedValue;
                    alpha = normalizedValue;
                    break;
                default:
                    scale = MIN_SCALE;
                    alpha = 0f;
            }

            ImageObject object = (ImageObject) mTextureMappingInfos.get(i).getObject();

            object.setScale(scale);
            object.setAlpha(alpha);

            if (mIsAtEdge == true && i != CURRENT_INDEX) {
                object.setAlpha(0f);
            }
        }

        if (mIsDetailObjectShown == true) {
            scale = MAX_SCALE + (MIN_SCALE - MAX_SCALE) * normalizedValue;
            alpha = 1f - normalizedValue;

            mDetailObject.setAlpha(alpha);
            mDetailObject.setScale(scale);
        }
    }

    private void onSwipingFinished() {
        if (DEBUG) {
            Log.d(TAG, "onSwipingFinished()");
        }

        mIsOnScroll = false;
        mIsAtEdge = false;
        mDragDistance = 0;

        changePosition();

        mGalleryContext.setImageIndexingInfo(mCurrentImageIndexingInfo);
        updateActionBarTitle();
    }

    private void updateActionBarTitle() {
        Message msg = mHandler.obtainMessage(ImageListActivity.UPDATE_ACTION_BAR_TITLE);
        BucketInfo bucketInfo = mImageManager.getBucketInfo(mCurrentImageIndexingInfo.mBucketIndex);
        mSelectedDateLabelInfo = bucketInfo.get(mCurrentImageIndexingInfo.mDateLabelIndex);

        msg.obj = mSelectedDateLabelInfo.getDate();
        mHandler.sendMessage(msg);
    }

    private void changePosition() {
        if (DEBUG) {
            Log.d(TAG, "changePosition()");
        }

        if (mFocusDirection == FocusDirection.LEFT) {
            int size = mTextureMappingInfos.size();
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos.remove(NUM_OF_DETAIL_OBJECTS - 1);

            textureMappingInfo.setTexture(null);
            textureMappingInfo.setGalleryInfo(null);

            mTextureMappingInfos.add(0, textureMappingInfo);
        } else if (mFocusDirection == FocusDirection.RIGHT) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos.remove(0);

            textureMappingInfo.setTexture(null);
            textureMappingInfo.setGalleryInfo(null);

            mTextureMappingInfos.add(textureMappingInfo);
        }

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(i);
            GalleryTexture texture = textureMappingInfo.getTexture();
            if (texture != null) {
                texture.setIndex(i);
            }
        }

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(i);
            ImageObject object = (ImageObject) textureMappingInfo.getObject();

            switch (i) {
                case PREV_INDEX:
                    object.setTranslate(-mWidth, 0f);
                    object.setScale(MIN_SCALE);
                    break;
                case CURRENT_INDEX:
                    object.setTranslate(0f, 0f);
                    object.setScale(MAX_SCALE);
                    break;
                case NEXT_INDEX:
                    object.setTranslate(mWidth, 0f);
                    object.setScale(MIN_SCALE);
                    break;
                default:
                    object.setTranslate((i - CURRENT_INDEX) * mWidth, 0f);
                    object.setScale(MIN_SCALE);
            }
        }

        mDetailObject.setTranslate(0f, 0f);
        mDetailObject.setScale(MAX_SCALE);
        mDetailObject.setAlpha(1f);
        GalleryTexture texture = mDetailTextureMappingInfo.getTexture();
        if (texture != null && texture.getState() == GalleryTexture.State.LOADED) {
            showDetailObject();
        }
    }

    // onSurfaceChanged

    void onSurfaceChanged(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged() width=" + width + " height=" + height);
        }

        mWidth = width;
        mHeight = height;
        mScreenRatio = (float) width / height;

        mRequestWidth = mWidth / 2;
        mRequestHeight = mHeight / 2;

        mLargeImage.onSurfaceChanged(width, height);
    }

    // onSurfaceCreated
    void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        mDummyTexture = GalleryUtils.createDummyTexture(Color.LTGRAY);

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            mTextureMappingInfos.get(i).getObject().setShader(mTextureShader);
        }

        mDetailObject.setShader(mTextureShader);

        mLargeImage.setShader(mTextureShader);
    }

    // initialization

    void setupObjects(GLESCamera camera) {
        if (DEBUG) {
            Log.d(TAG, "setupObjects()");
        }

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(i);
            ImageObject object = (ImageObject) textureMappingInfo.getObject();

            object.setCamera(camera);

            GLESVertexInfo vertexInfo = object.getVertexInfo();

            float x = -mWidth * 0.5f;
            float y = mHeight * 0.5f;

            float[] position = GLESUtils.makePositionCoord(x, y, mWidth, mHeight);
            vertexInfo.setBuffer(mTextureShader.getPositionAttribIndex(), position, 3);

            float[] texCoord = GLESUtils.makeTexCoord(0f, 0f, 1f, 1f);
            vertexInfo.setBuffer(mTextureShader.getTexCoordAttribIndex(), texCoord, 2);

            switch (i) {
                case CURRENT_INDEX:
                    object.setTranslate(0f, 0f);
                    object.setScale(MAX_SCALE);
                    break;
                default:
                    object.setTranslate((i - CURRENT_INDEX) * mWidth, 0f);
                    object.setScale(MIN_SCALE);
            }
        }

        {
            mDetailObject.setCamera(camera);

            GLESVertexInfo vertexInfo = mDetailObject.getVertexInfo();

            float x = -mWidth * 0.5f;
            float y = mHeight * 0.5f;

            float[] position = GLESUtils.makePositionCoord(x, y, mWidth, mHeight);
            vertexInfo.setBuffer(mTextureShader.getPositionAttribIndex(), position, 3);

            float[] texCoord = GLESUtils.makeTexCoord(0f, 0f, 1f, 1f);
            vertexInfo.setBuffer(mTextureShader.getTexCoordAttribIndex(), texCoord, 2);

            mDetailObject.setTranslate(0f, 0f);
            mDetailObject.setScale(MAX_SCALE);
            mDetailObject.setAlpha(1f);
            hideDetailObject();
        }

        mLargeImage.setCamera(camera);

        if (mCurrentImageIndexingInfo != null) {
            loadTextures();
            loadDetailTexture();
        }
    }

    private void loadTextures() {
        loadTexture(CURRENT_INDEX, mCurrentImageIndexingInfo, true);

        ImageIndexingInfo next = getNextImageIndexingInfo(mCurrentImageIndexingInfo);
        if (next != null) {
            loadTexture(NEXT_INDEX, next, true);

            ImageIndexingInfo next2 = getNextImageIndexingInfo(next);
            if (next2 != null) {
                loadTexture(NEXT2_INDEX, next2, true);

                ImageIndexingInfo next3 = getNextImageIndexingInfo(next2);
                if (next3 != null) {
                    loadTexture(NEXT3_INDEX, next3, true);
                }
            }

            mIsLastImage = false;
        } else {
            mIsLastImage = true;
        }

        ImageIndexingInfo prev = getPrevImageIndexingInfo(mCurrentImageIndexingInfo);

        if (prev != null) {
            loadTexture(PREV_INDEX, prev, true);

            ImageIndexingInfo prev2 = getNextImageIndexingInfo(prev);
            if (prev2 != null) {
                loadTexture(PREV2_INDEX, prev2, true);

                ImageIndexingInfo prev3 = getNextImageIndexingInfo(prev2);
                if (prev3 != null) {
                    loadTexture(PREV3_INDEX, prev3, true);
                }
            }

            mIsFirstImage = false;
        } else {
            mIsFirstImage = true;
        }
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

    private void loadTexture(int index, ImageIndexingInfo imageIndexingInfo, boolean isThumbnail) {
        ImageInfo imageInfo = ImageManager.getInstance().getImageInfo(imageIndexingInfo);

        TextureMappingInfo textureMappingInfo = null;
        if (index == CURRENT_DETAIL_INDEX) {
            textureMappingInfo = mDetailTextureMappingInfo;
        } else {
            textureMappingInfo = mTextureMappingInfos.get(index);
        }

        GalleryTexture texture = textureMappingInfo.getTexture();
        ImageInfo prevImageInfo = (ImageInfo) textureMappingInfo.getGalleryInfo();

        if (texture == null || prevImageInfo != imageInfo || index == CURRENT_DETAIL_INDEX) {
            texture = new GalleryTexture(imageInfo.getWidth(), imageInfo.getHeight());
            texture.setIndex(index);
            texture.setImageLoadingListener(this);

            textureMappingInfo.setTexture(texture);
        }

        textureMappingInfo.setGalleryInfo(imageInfo);

        setPositionCoord(index, imageInfo);

        ImageObject object = (ImageObject) textureMappingInfo.getObject();

        texture.setThumbnail(isThumbnail);
        if (isThumbnail == true) {
            if (texture.isTextureLoadingNeeded() == true) {
                object.setDummyTexture(mDummyTexture);
                ImageLoader.getInstance().loadThumbnail(imageInfo, texture);
            }
        } else {
            if (texture.isTextureLoadingNeeded() == true) {
                ImageLoader.getInstance().loadBitmap(imageInfo, texture, mRequestWidth, mRequestHeight);
            }
        }
    }

    private void loadOriginalSizeBitmap(ImageInfo imageInfo, GLESTexture texture) {

    }

    private void setPositionCoord(int index, ImageInfo imageInfo) {
        int imageWidth = imageInfo.getWidth();
        int imageHeight = imageInfo.getHeight();

        float top = 0f;
        float right = 0f;

        TextureMappingInfo textureMappingInfo = null;
        if (index == CURRENT_DETAIL_INDEX) {
            textureMappingInfo = mDetailTextureMappingInfo;
        } else {
            textureMappingInfo = mTextureMappingInfos.get(index);
        }
        ImageObject object = (ImageObject) textureMappingInfo.getObject();

        float imageRatio = (float) imageWidth / imageHeight;

        if ((mDetailViewState != DetailViewState.FIT_SCREEN) &&
                (index == CURRENT_INDEX || index == CURRENT_DETAIL_INDEX)) {
            top = mCurrentImageInfo.getHeight() * 0.5f;
            right = mCurrentImageInfo.getWidth() * 0.5f;
        } else {
            if (imageRatio >= mScreenRatio) {
                top = ((float) imageHeight / imageWidth) * mWidth * 0.5f;
                right = mWidth * 0.5f;
            } else {
                top = mHeight * 0.5f;
                right = ((float) imageWidth / imageHeight) * mHeight * 0.5f;
            }
        }

        if (DEBUG) {
            Log.d(TAG, "setPositionCoord() index=" + convert(index));
            Log.d(TAG, "\t imageWidth=" + imageWidth + " imageHeight=" + imageHeight);
            Log.d(TAG, "\t right=" + right + " top=" + top);
        }

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

    void createScene(GLESNode node) {
        if (DEBUG) {
            Log.d(TAG, "createScene()");
        }

        GLESGLState glState = new GLESGLState();
        glState.setCullFaceState(true);
        glState.setCullFace(GLES20.GL_BACK);
        glState.setBlendState(true);
        glState.setBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        glState.setDepthState(false);

        mViewPagerNode = new GLESNode("ViewPagerNode");
        node.addChild(mViewPagerNode);
        mViewPagerNode.setListener(mViewPagerNodeListener);

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            ImageObject object = new ImageObject("DetailObject_" + i);
            mViewPagerNode.addChild(object);

            GLESVertexInfo vertexInfo = new GLESVertexInfo();
            vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
            vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);
            object.setVertexInfo(vertexInfo, false, false);

            object.setGLState(glState);
            object.setListener(mDetailImageObjectListener);
            object.setIndex(i);
            object.hide();

            mTextureMappingInfos.add(new TextureMappingInfo(object));
        }

        {
            mDetailObject = new ImageObject("DetailObject");
            mViewPagerNode.addChild(mDetailObject);

            GLESVertexInfo vertexInfo = new GLESVertexInfo();
            vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
            vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);
            mDetailObject.setVertexInfo(vertexInfo, false, false);

            mDetailObject.setGLState(glState);
            mDetailObject.setListener(mDetailImageObjectListener);
            mDetailObject.setIndex(CURRENT_DETAIL_INDEX);
            hideDetailObject();

            mDetailTextureMappingInfo = new TextureMappingInfo(mDetailObject);
        }

        mLargeImageNode = new GLESNode("ViewPagerNode");
        node.addChild(mLargeImageNode);
        mLargeImageNode.hide();
        mNeedToLoadLargeImage = false;

        mLargeImage.setParentNode(mLargeImageNode);
    }

    // touch

    public boolean onTouchEvent(MotionEvent event) {

        if (mIsOnScaleAnimation == true) {
            return true;
        }

        boolean retVal = mGestureDetector.onTouchEvent(event);

        switch (mDetailViewState) {
            case ORIGINAL_SIZE_SMALL:
                return true;
            case ORIGINAL_SIZE_LARGE:
//                handlePanning(event);
                mLargeImage.onTouchEvent(event);
                mSurfaceView.requestRender();
                return true;
        }

        if (retVal == true) {
            return true;
        }

        final int action = MotionEventCompat.getActionMasked(event);

        if (action == ACTION_DOWN) {
            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }

            mScroller.abortAnimation();
            mIsDown = false;
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(event);
        }

        switch (action) {
            case ACTION_DOWN:
                mIsOnScroll = true;
                mScrollState = ScrollState.ON_SCROLL;
                mIsDown = true;
                mDownX = event.getX();
                mDragDistance = 0;
                break;
            case ACTION_UP:
                if (mIsDown == true) {
                    if (mIsAtEdge == false) {
                        mDragDistance = (int) (event.getX() - mDownX);
                    }

                    handleAnimation();
                    mIsDown = false;
                }

                if (mVelocityTracker != null) {
                    mVelocityTracker.clear();
                    mVelocityTracker = null;
                }

                break;
            case ACTION_MOVE:
                if (mIsDown == true) {
                    mIsOnScroll = true;
                    mScrollState = ScrollState.ON_SCROLL;
                    mDragDistance = (int) (event.getX() - mDownX);
                    if (mIsFirstImage == true && mDragDistance > 0) {
                        if (mDragDistance > mMaxDragDistanceAtEdge) {
                            mDragDistance = mMaxDragDistanceAtEdge;
                        }

                        mIsAtEdge = true;
                    }

                    if (mIsLastImage == true && mDragDistance < 0) {
                        if (mDragDistance < -mMaxDragDistanceAtEdge) {
                            mDragDistance = -mMaxDragDistanceAtEdge;
                        }

                        mIsAtEdge = true;
                    }
                }
                break;
            default:
        }

        mSurfaceView.requestRender();

        return true;
    }

    private void handleAnimation() {
        if (DEBUG) {
            Log.d(TAG, "handleAnimation()");
        }

        if (mIsAtEdge == true) {
            handleEdgeAnimation();
        } else {
            mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
            int initialVelocity = (int) mVelocityTracker.getXVelocity();

            if (Math.abs(initialVelocity) < mMinFlingVelocity) {
                handleScrollAnimation(0);
            } else {
                handleScrollAnimation(initialVelocity);
            }
        }

        mIsOnSwipeAnimation = true;
        mNeedToPopulate = true;
        mScrollState = ScrollState.ON_FLING;
    }

    private void handleEdgeAnimation() {
        if (DEBUG) {
            Log.d(TAG, "handleEdgeAnimation() mDragDistance=" + mDragDistance);
        }

        mFocusDirection = FocusDirection.NONE;
        mScroller.startScroll(mDragDistance, 0, -mDragDistance, 0, EDGE_SCROLLING_DURATION);
    }

    private void handleScrollAnimation(int initialVelocity) {
        if (DEBUG) {
            Log.d(TAG, "handleScrollAnimation() mDragDistance=" + mDragDistance);
        }

        int distance = 0;

        if (Math.abs(mDragDistance) > mMinDistanceForFling) {
            if (mDragDistance > 0) {
                mFocusDirection = FocusDirection.LEFT;
                distance = mWidth - mDragDistance;
            } else {
                mFocusDirection = FocusDirection.RIGHT;
                distance = -mWidth - mDragDistance;
            }
        } else {
            mFocusDirection = FocusDirection.NONE;
            distance = -mDragDistance;
        }

        int velocity = Math.abs(initialVelocity);
        int duration = 0;
        if (velocity > 0) {
            duration = 4 * Math.round(1000 * Math.abs((float) distance / velocity));
        } else {
            duration = (int) ((((float) Math.abs(distance) / mWidth) + 4) * 100);
        }

        duration = Math.min(duration, MAX_SETTLE_DURATION);
        mScroller.startScroll(mDragDistance, 0, distance, 0, duration);
    }


    private void handlePanning(MotionEvent event) {
        int width = mCurrentImageInfo.getWidth();
        int height = mCurrentImageInfo.getHeight();

        if (width < mWidth && height < mHeight) {
            return;
        }

        float maxTranslateX = 0f;
        if (width > mWidth) {
            maxTranslateX = (width - mWidth) * 0.5f;
        }

        float maxTranslateY = 0f;
        if (height > mHeight) {
            maxTranslateY = (height - mHeight) * 0.5f;
        }

        float x = 0f;
        float y = 0f;

        float translateX = 0f;
        float translateY = 0f;

        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case ACTION_DOWN:
                mPrevX = event.getX();
                mPrevY = event.getY();
                break;
            case ACTION_MOVE:
                x = event.getX();
                y = event.getY();

                translateX = mCurrentObject.getTranslateX();
                translateX += (x - mPrevX);

                if (maxTranslateX < Math.abs(translateX)) {
                    if (translateX < 0) {
                        translateX = -maxTranslateX;
                    } else {
                        translateX = maxTranslateX;
                    }
                }

                translateY = mCurrentObject.getTranslateY();
                translateY += (mPrevY - y);
                if (maxTranslateY < Math.abs(translateY)) {
                    if (translateY < 0) {
                        translateY = -maxTranslateY;
                    } else {
                        translateY = maxTranslateY;
                    }
                }

                mCurrentObject.setTranslate(translateX, translateY);

                mPrevX = x;
                mPrevY = y;
                break;
            case ACTION_UP:
                break;
            default:
        }
    }

    private void prePopulate() {
        if (DEBUG) {
            Log.d(TAG, "prePopulate()");
        }

        if (mFocusDirection == FocusDirection.LEFT) {
            mCurrentImageIndexingInfo = getPrevImageIndexingInfo(mCurrentImageIndexingInfo);
            loadTexture(PREV_INDEX, mCurrentImageIndexingInfo, true);

            ImageIndexingInfo prev = getPrevImageIndexingInfo(mCurrentImageIndexingInfo);
            if (prev != null) {
                loadTexture(PREV2_INDEX, prev, true);

                ImageIndexingInfo prev2 = getPrevImageIndexingInfo(prev);
                if (prev2 != null) {
                    loadTexture(PREV3_INDEX, prev2, true);
                }

                mIsFirstImage = false;
            } else {
                mIsFirstImage = true;
            }

            loadDetailTexture();

            mIsLastImage = false;
        } else if (mFocusDirection == FocusDirection.RIGHT) {
            mCurrentImageIndexingInfo = getNextImageIndexingInfo(mCurrentImageIndexingInfo);
            loadTexture(NEXT_INDEX, mCurrentImageIndexingInfo, true);

            ImageIndexingInfo next = getNextImageIndexingInfo(mCurrentImageIndexingInfo);
            if (next != null) {
                loadTexture(NEXT2_INDEX, next, true);

                ImageIndexingInfo next2 = getNextImageIndexingInfo(next);
                if (next2 != null) {
                    loadTexture(NEXT3_INDEX, next2, true);
                }

                mIsLastImage = false;
            } else {
                mIsLastImage = true;
            }

            loadDetailTexture();

            mIsFirstImage = false;
        }
    }

    private void loadDetailTexture() {
        GalleryTexture texture = mDetailTextureMappingInfo.getTexture();
        if (texture != null) {
            GalleryTexture.State state = texture.getState();
            switch (state) {
                case DECODING:
                    BitmapWorker.cancelWork(texture);
                    break;
                case QUEUING:
                    mWaitingTextures.remove(texture);
                    break;
            }
            mDetailTextureMappingInfo.setTexture(null);
        }

        hideDetailObject();
        loadTexture(CURRENT_DETAIL_INDEX, mCurrentImageIndexingInfo, false);
        ImageInfo imageInfo = ImageManager.getInstance().getImageInfo(mCurrentImageIndexingInfo);
        mDetailObject.setTranslate(0f, 0f);
        mDetailObject.setScale(MAX_SCALE);
        mDetailObject.setAlpha(1f);
    }

    // callback / listener

    @Override
    public void onColumnWidthChanged() {
        if (DEBUG) {
            Log.d(TAG, "onColumnWidthChanged()");
        }
    }

    @Override
    public void onImageDeleted() {
        if (DEBUG) {
            Log.d(TAG, "onImageDeleted()");
        }

        mCurrentImageIndexingInfo = null;
    }

    @Override
    public void onDateLabelDeleted() {
        if (DEBUG) {
            Log.d(TAG, "onDateLabelDeleted()");
        }
    }

    @Override
    public void onImageLoaded(int index, GalleryTexture texture) {
        if (DEBUG) {
            Log.d(TAG, "onImageLoaded() index=" + convert(index));
        }

        mWaitingTextures.add(texture);

        mSurfaceView.requestRender();
    }

    // set / get

    void onImageSelected(ImageObject selectedImageObject) {
        if (DEBUG) {
            Log.d(TAG, "onImageSelected()");
        }

        reset();

        mCurrentImageIndexingInfo = mGalleryContext.getImageIndexingInfo();
        BucketInfo bucketInfo = mImageManager.getBucketInfo(mCurrentImageIndexingInfo.mBucketIndex);
        mSelectedDateLabelInfo = bucketInfo.get(mCurrentImageIndexingInfo.mDateLabelIndex);

        loadTextures();

        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(CURRENT_INDEX);
        ImageObject object = (ImageObject) textureMappingInfo.getObject();
        object.setTexture(selectedImageObject.getTexture());

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            switch (i) {
                case CURRENT_INDEX:
                    mTextureMappingInfos.get(i).getObject().setTranslate(0f, 0f);
                    break;
                default:
                    mTextureMappingInfos.get(i).getObject().setTranslate((i - CURRENT_INDEX) * mWidth, 0f);
            }
        }

        mSurfaceView.requestRender();
    }

    ImageObject getCurrentDetailObject() {
        return (ImageObject) mTextureMappingInfos.get(CURRENT_INDEX).getObject();
    }


    ImageIndexingInfo getCurrentImageIndexingInfo() {
        return mCurrentImageIndexingInfo;
    }

    void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void setTextureAlphaShader(GLESShader textureShader) {
        if (DEBUG) {
            Log.d(TAG, "setTextureAlphaShader()");
        }

        mTextureShader = textureShader;

        int location = textureShader.getUniformLocation("uAlpha");
        GLES20.glUniform1f(location, 1f);
    }

    void setSurfaceView(GallerySurfaceView surfaceView) {
        if (DEBUG) {
            Log.d(TAG, "setSurfaceView()");
        }

        mSurfaceView = surfaceView;
        mRenderer = mSurfaceView.getRenderer();
    }

    void show() {
        if (DEBUG) {
            Log.d(TAG, "show()");
        }

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            mTextureMappingInfos.get(i).getObject().show();
        }

        mVisibility = true;

        mIsFinished = false;

        loadTexture(CURRENT_DETAIL_INDEX, mCurrentImageIndexingInfo, false);
        mDetailObject.setTranslate(0f, 0f);
        mDetailObject.setScale(MAX_SCALE);
        mDetailObject.setAlpha(1f);
    }

    void hide() {
        if (DEBUG) {
            Log.d(TAG, "hide()");
        }

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            mTextureMappingInfos.get(i).getObject().hide();
        }

        mVisibility = false;

        mDetailObject.hide();
    }

    private void showDetailObject() {
        mTextureMappingInfos.get(CURRENT_INDEX).getObject().hide();
        mDetailObject.show();
        mIsDetailObjectShown = true;
    }

    private void hideDetailObject() {
        if (mVisibility == true) {
            mTextureMappingInfos.get(CURRENT_INDEX).getObject().show();
        }

        mDetailObject.hide();
        mIsDetailObjectShown = false;
    }

    void onFinish() {
        if (DEBUG) {
            Log.d(TAG, "onFinish()");
        }

        mIsFinished = true;

        mWaitingTextures.clear();

        mScroller.abortAnimation();

        if (mScaleAnimator != null) {
            mScaleAnimator.cancel();
        }

        if (mIsOnSwipeAnimation == true) {
            onSwipingFinished();
        }

        mIsOnSwipeAnimation = false;
        mScrollState = ScrollState.STABLE;

        hideDetailObject();

        mViewPagerNode.show();
        mLargeImageNode.hide();
        mNeedToLoadLargeImage = false;
    }

    void destroyTextures() {
        if (DEBUG) {
            Log.d(TAG, "destroyTextures()");
        }

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(i);
            ImageObject object = (ImageObject) textureMappingInfo.getObject();
            GalleryTexture texture = object.getGalleryTexture();
            if (texture != null) {
                texture.destroy();
                textureMappingInfo.setTexture(null);
            }
        }

        ImageObject object = (ImageObject) mDetailTextureMappingInfo.getObject();
        GalleryTexture texture = object.getGalleryTexture();
        if (texture != null) {
            texture.destroy();
            mDetailTextureMappingInfo.setTexture(null);
        }
    }

    static String convert(int index) {
        String str;
        switch (index) {
            case 0:
                str = "PREV3";
                break;
            case 1:
                str = "PREV2";
                break;
            case 2:
                str = "PREV1";
                break;
            case 3:
                str = "CURRENT";
                break;
            case 4:
                str = "NEXT1";
                break;
            case 5:
                str = "NEXT2";
                break;
            case 6:
                str = "NEXT3";
                break;
            case 7:
                str = "CURRENT_DETAIL";
                break;
            default:
                str = "NONE";
        }

        return str;
    }

    // memeber class

    private final GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (DEBUG_TOUCH) {
                Log.d(TAG, "onSingleTapConfirmed()");
            }

            if (mIsActionBarShown == true) {
                mIsActionBarShown = false;
                GalleryUtils.setSystemUiVisibility((Activity) mContext, GalleryConfig.VisibleMode.FULLSCREEN_MODE);
            } else {
                mIsActionBarShown = true;
                ((Activity) mContext).invalidateOptionsMenu();
                GalleryUtils.setSystemUiVisibility((Activity) mContext, GalleryConfig.VisibleMode.VISIBLE_TRANSPARENT_MODE);
                ((Activity) mContext).getActionBar().setTitle(mSelectedDateLabelInfo.getDate());
            }

            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (DEBUG_TOUCH) {
                Log.d(TAG, "onDoubleTap()");
            }

            return super.onDoubleTap(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            if (DEBUG_TOUCH) {
                Log.d(TAG, "onDoubleTapEvent()");
            }

            int action = e.getAction();
            if (action == ACTION_UP) {
                startScaleAnimation();
            }
            return true;
        }

        private void startScaleAnimation() {
            mScrollState = ScrollState.ON_SCALE_ANIMATION;

            GalleryTexture texture = mDetailTextureMappingInfo.getTexture();
            if (texture != null && texture.getState() == GalleryTexture.State.LOADED) {
                mIsDetailObjectShown = true;

                mCurrentObject = mDetailObject;
                mCurrentImageInfo = (ImageInfo) mDetailTextureMappingInfo.getGalleryInfo();
                showDetailObject();

            } else {
                mIsDetailObjectShown = false;

                mCurrentObject = (ImageObject) mTextureMappingInfos.get(CURRENT_INDEX).getObject();
                mCurrentImageInfo = (ImageInfo) mTextureMappingInfos.get(CURRENT_INDEX).getGalleryInfo();

                hideDetailObject();
            }

            int width = mCurrentImageInfo.getWidth();
            int height = mCurrentImageInfo.getHeight();
            float imageRatio = (float) width / height;

            if (mDetailViewState == DetailViewState.FIT_SCREEN) {

                mPrevScale = 1f;
                if (imageRatio >= mScreenRatio) {
                    mNextScale = (float) width / mWidth;
                } else {
                    mNextScale = (float) height / mHeight;
                }

                if (mNextScale > 1f) {
                    mDetailViewState = DetailViewState.ORIGINAL_SIZE_LARGE;
                } else {
                    mDetailViewState = DetailViewState.ORIGINAL_SIZE_SMALL;
                }

                mPrevTranslateX = 0f;
                mPrevTranslateY = 0f;
            } else {
                mDetailViewState = DetailViewState.FIT_SCREEN;

                mPrevScale = 1f;
                if (imageRatio >= mScreenRatio) {
                    mNextScale = (float) mWidth / width;
                } else {
                    mNextScale = (float) mHeight / height;
                }

                mPrevTranslateX = mLargeImage.getTranslateX();
                mPrevTranslateY = mLargeImage.getTranslateY();
            }

            if (mDetailViewState == DetailViewState.FIT_SCREEN) {
                Toast.makeText(mContext, "Fit Screen",
                        Toast.LENGTH_SHORT)
                        .show();
            } else {
                Toast.makeText(mContext, "Original size",
                        Toast.LENGTH_SHORT)
                        .show();
            }

            mScaleAnimator = new GLESAnimator(mScaleAnimationCB);
            mScaleAnimator.setValues(0f, 1f);
            mScaleAnimator.setDuration(0L, SCALE_ANIMATION_DURATION);
            mScaleAnimator.start();

            mLargeImageNode.hide();
            mNeedToLoadLargeImage = false;
            mViewPagerNode.show();

            if (DEBUG) {
                Log.d(TAG, "startScaleAnimation() mDetailViewState=" + mDetailViewState);
                Log.d(TAG, "\t image width=" + width + " height=" + height);
                Log.d(TAG, "\t screen mWidth=" + mWidth + " mHeight=" + mHeight);
                Log.d(TAG, "\t mPrevScale=" + mPrevScale + " mNextScale=" + mNextScale);
            }

            mIsOnScaleAnimation = true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (DEBUG_TOUCH) {
                Log.d(TAG, "onScroll()");
            }

            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (DEBUG_TOUCH) {
                Log.d(TAG, "onFling()");
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (DEBUG_TOUCH) {
                Log.d(TAG, "onLongPress()");
            }
        }
    };

    private GLESNodeListener mViewPagerNodeListener = new GLESNodeListener() {
        @Override
        public void update(GLESNode node) {
            GLESTransform transform = node.getTransform();
            transform.setIdentity();

            transform.setTranslate(mDragDistance, 0f, 0f);
        }
    };

    private GLESObjectListener mDetailImageObjectListener = new GLESObjectListener() {
        @Override
        public void update(GLESObject object) {
            ImageObject imageObject = (ImageObject) object;
            GLESTransform transform = object.getTransform();
            transform.setIdentity();

            float x = imageObject.getTranslateX();
            float y = imageObject.getTranslateY();
            float z = imageObject.getTranslateZ();
            float scale = imageObject.getScale();

            transform.setTranslate(x, y, z);
            transform.setScale(scale);
        }

        @Override
        public void apply(GLESObject object) {
        }
    };

    private GLESAnimatorCallback mScaleAnimationCB = new GLESAnimatorCallback() {
        @Override
        public void onAnimation(GLESVector3 current) {
            float normalizedValue = current.getX();

            float scale = mPrevScale + (mNextScale - mPrevScale) * normalizedValue;
            mCurrentObject.setScale(scale);

            float translateX = mPrevTranslateX - mPrevTranslateX * normalizedValue;
            float translateY = mPrevTranslateY - mPrevTranslateY * normalizedValue;

            mCurrentObject.setTranslate(translateX, translateY);
        }

        @Override
        public void onCancel() {
            mCurrentObject.setScale(1f);
            mIsOnScaleAnimation = false;

            if (mIsDetailObjectShown == true) {
                setPositionCoord(CURRENT_DETAIL_INDEX, mCurrentImageInfo);
            } else {
                setPositionCoord(CURRENT_INDEX, mCurrentImageInfo);
            }

            mScrollState = ScrollState.STABLE;
        }

        @Override
        public void onFinished() {
            mCurrentObject.setScale(1f);
            mIsOnScaleAnimation = false;

            if (mIsDetailObjectShown == true) {
                setPositionCoord(CURRENT_DETAIL_INDEX, mCurrentImageInfo);
            } else {
                GalleryTexture texture = mDetailTextureMappingInfo.getTexture();
                if (texture != null && texture.getState() == GalleryTexture.State.LOADED) {
                    mCurrentObject = mDetailObject;
                    mCurrentImageInfo = (ImageInfo) mDetailTextureMappingInfo.getGalleryInfo();
                    showDetailObject();

                    setPositionCoord(CURRENT_DETAIL_INDEX, mCurrentImageInfo);
                } else {
                    setPositionCoord(CURRENT_INDEX, mCurrentImageInfo);
                }
            }

            if (mDetailViewState == DetailViewState.ORIGINAL_SIZE_LARGE) {
                mLargeImage.load(mCurrentImageInfo);
                mNeedToLoadLargeImage = true;
            }

            mScrollState = ScrollState.STABLE;
        }
    };
}
