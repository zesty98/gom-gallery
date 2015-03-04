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
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

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
import com.gomdev.gles.GLESVertexInfo;

import java.nio.FloatBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by gomdev on 15. 2. 24..
 */
public class DetailViewPager implements GridInfoChangeListener, ImageLoadingListener {
    static final String CLASS = "DetailViewPager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private static final int SWIPE_FLING_DURATION = 100;
    private static final int SWIPE_SCROLLING_DURATION = 300;

    private static final int NUM_OF_DETAIL_OBJECTS = 5;

    private static final int MIN_FLING_VELOCITY = 400;  // dips
    private static final int MIN_DISTANCE_FOR_FLING = 25; // dips

    private static final float MIN_SCALE = 0.7f;
    private static final float MAX_SCALE = 1.0f;

    private enum FOCUS_DIRECTION {
        LEFT,
        RIGHT,
        NONE
    }

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

    private DateLabelInfo mSelectedDateLabelInfo = null;
    private ImageIndexingInfo mCurrentImageIndexingInfo = null;

    private TextureMappingInfo[] mTextureMappingInfos = new TextureMappingInfo[NUM_OF_DETAIL_OBJECTS];
    private Queue<GalleryTexture> mWaitingTextures = new ConcurrentLinkedQueue<>();

    private FOCUS_DIRECTION mFocusDirection = FOCUS_DIRECTION.NONE;

    private boolean mIsFirstImage = false;
    private boolean mIsLastImage = false;

    private int mPrevIndex = 0;
    private int mCurrentIndex = 1;
    private int mNextIndex = 2;
    private int mReservedIndex = 3;
    private int mReservedIndex2 = 4;

    private int mWidth = 0;
    private int mHeight = 0;

    private int mRequestWidth = 0;
    private int mRequestHeight = 0;


    // touch or gesture
    private boolean mIsDown = false;
    private float mDownX = 0f;
    private float mDragDistance = 0f;

    private Scroller mScroller = null;
    private VelocityTracker mVelocityTracker = null;
    private GestureDetector mGestureDetector = null;

    private boolean mIsOnSwipeAnimation = false;
    private boolean mIsOnScroll = false;

    private int mMinFlingVelocity = 0;
    private int mMinDistanceForFling = 0;
    private int mMaxFlingVelocity = 0;

    private boolean mIsAtEdge = false;
    private int mMaxDragDistanceAtEdge = 0;

    DetailViewPager(Context context, GridInfo gridInfo) {
        mContext = context;
        mGridInfo = gridInfo;

        init();
    }

    private void init() {
        reset();
        clear();
        setGridInfo(mGridInfo);

        mGalleryContext = GalleryContext.getInstance();
        mImageManager = ImageManager.getInstance();

        mScroller = new Scroller(mContext, new DecelerateInterpolator());
        mGestureDetector = new GestureDetector(mContext, mGestureListener);
        final ViewConfiguration configuration = ViewConfiguration.get(mContext);
        final float density = mContext.getResources().getDisplayMetrics().density;

        mMinFlingVelocity = (int) (MIN_FLING_VELOCITY * density);
        mMinDistanceForFling = (int) (MIN_DISTANCE_FOR_FLING * density);
        mMaxFlingVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    private void reset() {
        mIsFirstImage = false;
        mIsLastImage = false;

        mFocusDirection = FOCUS_DIRECTION.NONE;
    }

    private void clear() {
        mWaitingTextures.clear();
    }

    private void setGridInfo(GridInfo gridInfo) {
        gridInfo.addListener(this);
    }

    // rendering

    void update() {
        GalleryTexture texture = mWaitingTextures.poll();

        if (texture != null) {
            int index = texture.getIndex();
            final Bitmap bitmap = texture.getBitmapDrawable().getBitmap();
            texture.load(bitmap);
            bitmap.recycle();

            TextureMappingInfo textureMappingInfo = mTextureMappingInfos[index];

            ImageObject object = (ImageObject) textureMappingInfo.getObject();

            object.setTexture(texture.getTexture());
        }

        if (mWaitingTextures.size() > 0) {
            mSurfaceView.requestRender();
        }
    }

    void updateAnimation() {
        if (mScroller.isFinished() == false) {
            mSurfaceView.requestRender();

            if (mScroller.computeScrollOffset() == true) {
                mDragDistance = mScroller.getCurrX();
            }
        } else {
            if (mIsOnSwipeAnimation == true) {
                mIsOnScroll = false;
                mIsAtEdge = false;
                mDragDistance = 0f;
                changePosition();

                Message msg = mHandler.obtainMessage(ImageListActivity.UPDATE_ACTION_BAR_TITLE);
                BucketInfo bucketInfo = mImageManager.getBucketInfo(mCurrentImageIndexingInfo.mBucketIndex);
                mSelectedDateLabelInfo = bucketInfo.get(mCurrentImageIndexingInfo.mDateLabelIndex);

                msg.obj = mSelectedDateLabelInfo.getDate();
                mHandler.sendMessage(msg);
            }
            mIsOnSwipeAnimation = false;
        }

        if (mIsOnScroll == true) {
            float normalizedValue = Math.abs(mDragDistance / mWidth);

            for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
                float scale = 1f;

                if (i == mPrevIndex) {
                    scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * normalizedValue;
                } else if (i == mCurrentIndex) {
                    scale = MAX_SCALE + (MIN_SCALE - MAX_SCALE) * normalizedValue;
                } else if (i == mNextIndex) {
                    scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * normalizedValue;
                } else {
                    scale = 1f;
                }

                float alpha = (float) Math.pow(scale, 3f);

                ImageObject object = (ImageObject) mTextureMappingInfos[i].getObject();
                object.setScale(scale);
                object.setAlpha(alpha);

                if (mIsAtEdge == true && i != mCurrentIndex) {
                    object.setAlpha(0f);
                }
            }
        }
    }

    private void changePosition() {
        if (DEBUG) {
            Log.d(TAG, "changePosition() before");
            Log.d(TAG, "\t mPrev =" + mTextureMappingInfos[mPrevIndex].getObject().getName());
            Log.d(TAG, "\t mCurrent =" + mTextureMappingInfos[mCurrentIndex].getObject().getName());
            Log.d(TAG, "\t mNext =" + mTextureMappingInfos[mNextIndex].getObject().getName());
        }

        if (mFocusDirection == FOCUS_DIRECTION.LEFT) {
            mTextureMappingInfos[mReservedIndex2] = mTextureMappingInfos[mNextIndex];
            if (mTextureMappingInfos[mReservedIndex2].getTexture() != null) {
                mTextureMappingInfos[mReservedIndex2].getTexture().setIndex(mReservedIndex2);
            }

            mTextureMappingInfos[mNextIndex] = mTextureMappingInfos[mCurrentIndex];
            mTextureMappingInfos[mNextIndex].getTexture().setIndex(mNextIndex);

            mTextureMappingInfos[mCurrentIndex] = mTextureMappingInfos[mPrevIndex];
            mTextureMappingInfos[mCurrentIndex].getTexture().setIndex(mCurrentIndex);

            mTextureMappingInfos[mPrevIndex] = mTextureMappingInfos[mReservedIndex];
            mTextureMappingInfos[mPrevIndex].getTexture().setIndex(mPrevIndex);

            mTextureMappingInfos[mReservedIndex] = mTextureMappingInfos[mReservedIndex2];
            if (mTextureMappingInfos[mReservedIndex].getTexture() != null) {
                mTextureMappingInfos[mReservedIndex].getTexture().setIndex(mReservedIndex);
            }
        } else if (mFocusDirection == FOCUS_DIRECTION.RIGHT) {
            mTextureMappingInfos[mReservedIndex2] = mTextureMappingInfos[mPrevIndex];
            if (mTextureMappingInfos[mReservedIndex2].getTexture() != null) {
                mTextureMappingInfos[mReservedIndex2].getTexture().setIndex(mReservedIndex2);
            }

            mTextureMappingInfos[mPrevIndex] = mTextureMappingInfos[mCurrentIndex];
            mTextureMappingInfos[mPrevIndex].getTexture().setIndex(mPrevIndex);

            mTextureMappingInfos[mCurrentIndex] = mTextureMappingInfos[mNextIndex];
            mTextureMappingInfos[mCurrentIndex].getTexture().setIndex(mCurrentIndex);

            mTextureMappingInfos[mNextIndex] = mTextureMappingInfos[mReservedIndex];
            mTextureMappingInfos[mNextIndex].getTexture().setIndex(mNextIndex);

            mTextureMappingInfos[mReservedIndex] = mTextureMappingInfos[mReservedIndex2];
            if (mTextureMappingInfos[mReservedIndex].getTexture() != null) {
                mTextureMappingInfos[mReservedIndex].getTexture().setIndex(mReservedIndex);
            }
        }

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos[i];
            ImageObject object = (ImageObject) textureMappingInfo.getObject();

            if (i == mPrevIndex) {
                object.setTranslate(-mWidth, 0f);
                object.setScale(MIN_SCALE);
            } else if (i == mCurrentIndex) {
                object.setTranslate(0f, 0f);
                object.setScale(MAX_SCALE);
            } else if (i == mNextIndex) {
                object.setTranslate(mWidth, 0f);
                object.setScale(MIN_SCALE);
            } else {
                object.setTranslate(0f, mHeight);
                object.setScale(MIN_SCALE);
            }
            object.setScale(1.0f);
        }

        mTextureMappingInfos[mReservedIndex].getObject().setTranslate(0f, mHeight);
        mTextureMappingInfos[mReservedIndex2].getObject().setTranslate(0f, mHeight);

        if (DEBUG) {
            Log.d(TAG, "changePosition() after");
            Log.d(TAG, "\t mPrev =" + mTextureMappingInfos[mPrevIndex].getObject().getName());
            Log.d(TAG, "\t mCurrent =" + mTextureMappingInfos[mCurrentIndex].getObject().getName());
            Log.d(TAG, "\t mNext =" + mTextureMappingInfos[mNextIndex].getObject().getName());
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

        mMaxDragDistanceAtEdge = mWidth / 4;
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
        mTextureMappingInfos[mReservedIndex].getObject().setShader(mTextureShader);
        mTextureMappingInfos[mReservedIndex2].getObject().setShader(mTextureShader);
    }

    // initialization

    void setupObjects(GLESCamera camera) {
        if (DEBUG) {
            Log.d(TAG, "setupObjects()");
        }

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos[i];
            ImageObject object = (ImageObject) textureMappingInfo.getObject();
            object.setCamera(camera);

            GLESVertexInfo vertexInfo = object.getVertexInfo();

            float x = -mWidth * 0.5f;
            float y = mHeight * 0.5f;

            float[] position = GLESUtils.makePositionCoord(x, y, mWidth, mHeight);
            vertexInfo.setBuffer(mTextureShader.getPositionAttribIndex(), position, 3);

            float[] texCoord = GLESUtils.makeTexCoord(0f, 0f, 1f, 1f);
            vertexInfo.setBuffer(mTextureShader.getTexCoordAttribIndex(), texCoord, 2);

            if (i == mPrevIndex) {
                object.setTranslate(-mWidth, 0f);
                object.setScale(MIN_SCALE);
            } else if (i == mCurrentIndex) {
                object.setTranslate(0f, 0f);
                object.setScale(MAX_SCALE);
            } else if (i == mNextIndex) {
                object.setTranslate(mWidth, 0f);
                object.setScale(MIN_SCALE);
            } else {
                object.setTranslate(0f, mHeight);
                object.setScale(MAX_SCALE);
            }
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

        TextureMappingInfo textureMappingInfo = mTextureMappingInfos[index];
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

        TextureMappingInfo textureMappingInfo = mTextureMappingInfos[index];

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

            mTextureMappingInfos[i] = new TextureMappingInfo(object);
        }
    }

    // touch

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
                if (mIsOnSwipeAnimation == false) {
                    mIsOnScroll = true;
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
                        if (mDragDistance > mMaxDragDistanceAtEdge) {
                            mDragDistance = mMaxDragDistanceAtEdge;
                        }

                        mIsAtEdge = true;
                    }

                    if (mIsLastImage == true && mDragDistance <= 0) {
                        if (mDragDistance < -mMaxDragDistanceAtEdge) {
                            mDragDistance = -mMaxDragDistanceAtEdge;
                        }

                        mIsAtEdge = true;
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
        if (mIsAtEdge == true) {
            handleEdgeAnimation();
        } else {
            mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
            int initialVelocity = (int) mVelocityTracker.getXVelocity();

            if (Math.abs(initialVelocity) < mMinFlingVelocity) {
                handleScrollAnimation();
            } else {
                handleFlingAnimation(initialVelocity);
            }
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

    private void handleEdgeAnimation() {
        mFocusDirection = FOCUS_DIRECTION.NONE;
        mScroller.startScroll((int) mDragDistance, 0, (int) -mDragDistance, 0, SWIPE_SCROLLING_DURATION);
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

    // callback / listener

    @Override
    public void onColumnWidthChanged() {
        if (DEBUG) {
            Log.d(TAG, "onColumnWidthChanged()");
        }
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

    // set / get

    void onImageSelected(ImageObject selectedImageObject) {
        if (DEBUG) {
            Log.d(TAG, "onImageSelected()");
        }

        reset();

        mCurrentImageIndexingInfo = mGalleryContext.getImageIndexingInfo();
        BucketInfo bucketInfo = mImageManager.getBucketInfo(mCurrentImageIndexingInfo.mBucketIndex);
        mSelectedDateLabelInfo = bucketInfo.get(mCurrentImageIndexingInfo.mDateLabelIndex);

        loadDetailTextures();

        TextureMappingInfo textureMappingInfo = mTextureMappingInfos[mCurrentIndex];
        ImageObject object = (ImageObject) textureMappingInfo.getObject();
        object.setTexture(selectedImageObject.getTexture());

        mTextureMappingInfos[mPrevIndex].getObject().setTranslate(-mWidth, 0);
        mTextureMappingInfos[mNextIndex].getObject().setTranslate(mWidth, 0);

        mSurfaceView.requestRender();
    }

    ImageObject getCurrentDetailObject() {
        return (ImageObject) mTextureMappingInfos[mCurrentIndex].getObject();
    }

    ImageIndexingInfo getCurrentImageIndexingInfo() {
        return mCurrentImageIndexingInfo;
    }

    void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void setTextureShader(GLESShader textureShader) {
        if (DEBUG) {
            Log.d(TAG, "setTextureShader()");
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
            if (mTextureMappingInfos[mCurrentIndex].getObject().getVisibility() == false) {
                Log.d(TAG, "show()");
            }
        }

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            mTextureMappingInfos[i].getObject().show();
        }
    }

    void hide() {
        if (DEBUG) {
            Log.d(TAG, "hide()");
        }

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            mTextureMappingInfos[i].getObject().hide();
        }
    }

    // memeber class

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
                ((Activity) mContext).getActionBar().setTitle(mSelectedDateLabelInfo.getDate());
            }
            return true;
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
            ImageObject imageObject = (ImageObject) object;

            int location = mTextureShader.getUniformLocation("uAlpha");
            GLES20.glUniform1f(location, imageObject.getAlpha());
        }
    };
}
