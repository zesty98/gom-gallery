package com.gomdev.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.gomdev.gles.GLESAnimator;
import com.gomdev.gles.GLESAnimatorCallback;
import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESNodeListener;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESObjectListener;
import com.gomdev.gles.GLESShader;
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

    private static final int NUM_OF_DETAIL_OBJECTS = 3;

    private final Context mContext;
    private final GridInfo mGridInfo;

    private GallerySurfaceView mSurfaceView = null;
    private ImageListRenderer mRenderer = null;

    private GalleryContext mGalleryContext = null;
    private ImageManager mImageManager = null;

    private GLESNode mViewPagerNode = null;
    private ImageObject mBGObject = null;

    private GLESAnimator mSelectionAnimator = null;
    private ImageObject mSelectedImageObject = null;

    private ImageObject[] mDetailObjects = null;
    private GalleryTexture[] mDetailTextures = null;
    private ImageInfo[] mDetailImageInfos = null;

    private boolean mIsFirstImage = false;
    private boolean mIsLastImage = false;

    private ImageIndexingInfo mDetailImageIndexingInfo = null;

    private Queue<GalleryTexture> mWaitingTextures = new ConcurrentLinkedQueue<>();
    private boolean mIsImageLoaded = false;
//    private Bitmap mBitmap = null;

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

    private float mCurrentOffsetX = 0f;
    private float mPrevOffsetX = 0f;
    private float mNextOffsetX = 0f;

    // touch
    private boolean mIsDown = false;
    private float mDownX = 0f;
    private float mDragDistance = 0f;
    private GLESAnimator mSwipeAnimator = null;

    DetailViewManager(Context context, GridInfo gridInfo) {
        if (DEBUG) {
            Log.d(TAG, "DetailViewManager()");
        }

        mContext = context;
        mGridInfo = gridInfo;

        setGridInfo(gridInfo);

        mGalleryContext = GalleryContext.getInstance();
        mImageManager = ImageManager.getInstance();

        mSelectionAnimator = new GLESAnimator(new SelectionAnimatorCallback());
        mSelectionAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mSelectionAnimator.setDuration(GalleryConfig.SELECTION_ANIMATION_START_OFFSET, GalleryConfig.SELECTION_ANIMATION_END_OFFSET);

        mSwipeAnimator = new GLESAnimator(mSwipeAnimatorCB);
        mSwipeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mSwipeAnimator.setDuration(0L, 1000L);

        mDetailObjects = new ImageObject[NUM_OF_DETAIL_OBJECTS];
        mDetailTextures = new GalleryTexture[NUM_OF_DETAIL_OBJECTS];
        mDetailImageInfos = new ImageInfo[NUM_OF_DETAIL_OBJECTS];

        clear();
    }

    private void setGridInfo(GridInfo gridInfo) {
        mColumnWidth = mGridInfo.getColumnWidth();

        gridInfo.addListener(this);
    }

    private void clear() {
        mWaitingTextures.clear();
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

            mDetailObjects[index].setTexture(texture.getTexture());
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

        if (mSwipeAnimator.doAnimation() == true) {
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

        mRequestWidth = mWidth / 2;
        mRequestHeight = mHeight / 2;

        mCurrentOffsetX = 0f;
        mPrevOffsetX = -mWidth;
        mNextOffsetX = mWidth;
    }

    // onSurfaceCreated

    void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            mDetailObjects[i].setShader(mTextureShader);
        }

        mBGObject.setShader(mColorShader);
    }

    void setupObjects(GLESCamera camera) {
        if (DEBUG) {
            Log.d(TAG, "setupObjects()");
        }

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            mDetailObjects[i].setCamera(camera);

            GLESVertexInfo vertexInfo = mDetailObjects[i].getVertexInfo();
            float[] position = GLESUtils.makePositionCoord(-mWidth * 0.5f - mWidth + mWidth * i, mHeight * 0.5f, mWidth, mHeight);
            vertexInfo.setBuffer(mTextureShader.getPositionAttribIndex(), position, 3);

            float[] texCoord = GLESUtils.makeTexCoord(0f, 0f, 1f, 1f);
            vertexInfo.setBuffer(mTextureShader.getTexCoordAttribIndex(), texCoord, 2);
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
    }

    void onResume() {
        if (DEBUG) {
            Log.d(TAG, "onResume()");
        }

        if (mDetailImageIndexingInfo != null) {
            loadCurrentDetailTexture();
            loadPrevDetailTexture();
            loadNextDetailTexture();
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
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsDown = true;
                mDownX = event.getX();
                mDragDistance = 0f;
                break;
            case MotionEvent.ACTION_UP:
                mIsDown = false;
                handleAnimation();
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

        mSurfaceView.requestRender();

        return true;//mGestureDetector.onTouchEvent(event);
    }

    private void handleAnimation() {
        if (Math.abs(mDragDistance) > mWidth * 0.5f) {
            if (mDragDistance > 0) {
                mSwipeAnimator.setValues(mDragDistance, mWidth);
            } else {
                mSwipeAnimator.setValues(mDragDistance, -mWidth);
            }
        } else {
            mSwipeAnimator.setValues(mDragDistance, 0f);
        }

        mSwipeAnimator.setDuration(0L, 300L);
        mSwipeAnimator.start();
    }

    // show / hide
    void show() {
        if (DEBUG) {
            if (mDetailObjects[1].getVisibility() == false) {
                Log.d(TAG, "show()");
            }
        }

        mBGObject.show();
        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            mDetailObjects[i].show();
        }
    }

    void hide() {
        if (DEBUG) {
            Log.d(TAG, "hide()");
        }

        mBGObject.hide();

        for (int i = 0; i < NUM_OF_DETAIL_OBJECTS; i++) {
            mDetailObjects[i].hide();
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
            mDetailObjects[i] = new ImageObject("DetailObject");
            mViewPagerNode.addChild(mDetailObjects[i]);

            GLESVertexInfo vertexInfo = new GLESVertexInfo();
            vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
            vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);
            mDetailObjects[i].setVertexInfo(vertexInfo, false, false);

            mDetailObjects[i].setGLState(glState);

            mDetailObjects[i].setListener(mDetailImageObjectListener);
            mDetailObjects[i].hide();
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
        mIsImageLoaded = true;

        mSurfaceView.requestRender();
    }

    private void setPositionCoord(int index) {
        int imageWidth = 0;
        int imageHeight = 0;

        if (mDetailImageInfos[index].getOrientation() == 90 || mDetailImageInfos[index].getOrientation() == 270) {
            imageWidth = mDetailImageInfos[index].getHeight();
            imageHeight = mDetailImageInfos[index].getWidth();
        } else {
            imageWidth = mDetailImageInfos[index].getWidth();
            imageHeight = mDetailImageInfos[index].getHeight();
        }

        float top = ((float) imageHeight / imageWidth) * mWidth * 0.5f;
        float right = mWidth * 0.5f;

        if (top > mHeight * 0.5f) {
            top = mHeight * 0.5f;
            right = ((float) imageWidth / imageHeight) * mHeight * 0.5f;
        }

        FloatBuffer positionBuffer = (FloatBuffer) mDetailObjects[index].getVertexInfo().getBuffer(mTextureShader.getPositionAttribIndex());

        float offsetX = 0f;
        if (index == mPrevIndex) {
            offsetX = -mWidth;
        } else if (index == mNextIndex) {
            offsetX = mWidth;
        }

        positionBuffer.put(0, -right + offsetX);
        positionBuffer.put(1, -top);

        positionBuffer.put(3, right + offsetX);
        positionBuffer.put(4, -top);

        positionBuffer.put(6, -right + offsetX);
        positionBuffer.put(7, top);

        positionBuffer.put(9, right + offsetX);
        positionBuffer.put(10, top);
    }

    void onImageSelected(ImageObject selectedImageObject) {
        if (DEBUG) {
            Log.d(TAG, "onImageSelected()");
        }

        mIsImageLoaded = false;
        mIsFirstImage = false;
        mIsLastImage = false;

        mPrevIndex = 0;
        mCurrentIndex = 1;
        mNextIndex = 2;

        mSelectedImageObject = selectedImageObject;

        RectF viewport = mGalleryContext.getCurrentViewport();

        mSrcX = mSelectedImageObject.getTranslateX();
        mSrcY = mHeight * 0.5f - (viewport.bottom - mSelectedImageObject.getTranslateY());

        mDetailImageIndexingInfo = mGalleryContext.getImageIndexingInfo();
        mDetailImageInfos[mCurrentIndex] = mImageManager.getImageInfo(mDetailImageIndexingInfo);

        GLESVertexInfo vertexInfo = mDetailObjects[mCurrentIndex].getVertexInfo();
        float[] vertex = GLESUtils.makePositionCoord(-mWidth * 0.5f, mWidth * 0.5f, mWidth, mWidth);
        vertexInfo.setBuffer(mTextureShader.getPositionAttribIndex(), vertex, 3);

        calcTexCoordInfo(mDetailImageInfos[mCurrentIndex]);

        float[] texCoord = GLESUtils.makeTexCoord(mMinS, mMinT, mMaxS, mMaxT);
        vertexInfo.setBuffer(mTextureShader.getTexCoordAttribIndex(), texCoord, 2);

        {
            mDetailTextures[mCurrentIndex] = new GalleryTexture(mDetailImageInfos[mCurrentIndex].getWidth(), mDetailImageInfos[mCurrentIndex].getHeight());
            mDetailTextures[mCurrentIndex].setIndex(mCurrentIndex);
            mDetailTextures[mCurrentIndex].setImageLoadingListener(this);

            ImageLoader.getInstance().loadBitmap(mDetailImageInfos[mCurrentIndex], mDetailTextures[mCurrentIndex], mRequestWidth, mRequestHeight);
        }

        loadPrevDetailTexture();
        loadNextDetailTexture();

        mDetailObjects[mCurrentIndex].setTexture(mSelectedImageObject.getTexture());

        mSelectionAnimator.setValues(0f, 1f);
        mSelectionAnimator.cancel();
        mSelectionAnimator.start();

        mSurfaceView.requestRender();
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

    private void loadCurrentDetailTexture() {
        mDetailImageInfos[mCurrentIndex] = ImageManager.getInstance().getImageInfo(mDetailImageIndexingInfo);

        mDetailTextures[mCurrentIndex] = new GalleryTexture(mDetailImageInfos[mCurrentIndex].getWidth(), mDetailImageInfos[mCurrentIndex].getHeight());
        mDetailTextures[mCurrentIndex].setIndex(mCurrentIndex);
        mDetailTextures[mCurrentIndex].setImageLoadingListener(this);

        setPositionCoord(mCurrentIndex);

        ImageLoader.getInstance().loadBitmap(mDetailImageInfos[mCurrentIndex], mDetailTextures[mCurrentIndex], mRequestWidth, mRequestHeight);
    }

    private void loadNextDetailTexture() {
        ImageIndexingInfo next = getNextImageIndexingInfo(mDetailImageIndexingInfo);
        if (next != null) {
            mDetailImageInfos[mNextIndex] = ImageManager.getInstance().getImageInfo(next);

            mDetailTextures[mNextIndex] = new GalleryTexture(mDetailImageInfos[mNextIndex].getWidth(), mDetailImageInfos[mNextIndex].getHeight());
            mDetailTextures[mNextIndex].setIndex(mNextIndex);
            mDetailTextures[mNextIndex].setImageLoadingListener(this);

            setPositionCoord(mNextIndex);

            ImageLoader.getInstance().loadBitmap(mDetailImageInfos[mNextIndex], mDetailTextures[mNextIndex], mRequestWidth, mRequestHeight);

            mIsLastImage = false;
            mIsFirstImage = false;
        } else {
            mIsLastImage = true;
        }
    }

    private void loadPrevDetailTexture() {
        ImageIndexingInfo prev = getPrevImageIndexingInfo(mDetailImageIndexingInfo);

        if (prev != null) {
            mDetailImageInfos[mPrevIndex] = ImageManager.getInstance().getImageInfo(prev);

            mDetailTextures[mPrevIndex] = new GalleryTexture(mDetailImageInfos[mPrevIndex].getWidth(), mDetailImageInfos[mPrevIndex].getHeight());
            mDetailTextures[mPrevIndex].setIndex(mPrevIndex);
            mDetailTextures[mPrevIndex].setImageLoadingListener(this);

            setPositionCoord(mPrevIndex);

            ImageLoader.getInstance().loadBitmap(mDetailImageInfos[mPrevIndex], mDetailTextures[mPrevIndex], mRequestWidth, mRequestHeight);

            mIsFirstImage = false;
            mIsLastImage = false;
        } else {
            mIsFirstImage = true;
        }
    }

    private void calcTexCoordInfo(ImageInfo imageInfo) {
        float imageWidth = 0f;
        float imageHeight = 0f;

        if (imageInfo.getOrientation() == 90 || imageInfo.getOrientation() == 270) {
            imageWidth = imageInfo.getHeight();
            imageHeight = imageInfo.getWidth();
        } else {
            imageWidth = imageInfo.getWidth();
            imageHeight = imageInfo.getHeight();
        }

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

        if (mSelectionAnimator.isFinished() == false) {
            mSelectionAnimator.cancel();
            mSelectionAnimator.setValues(mNormalizedValue, 0f);
        } else {
            mSelectionAnimator.setValues(1f, 0f);
        }

        mSelectionAnimator.start();

        mIsFinishing = true;

        mSurfaceView.requestRender();
    }

    // set / get

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
            ImageObject imageObject = (ImageObject) object;
            GLESTransform transform = object.getTransform();
            transform.setIdentity();

            transform.setTranslate(0f, 0f, 0f);
            transform.setScale(1f);
        }

        @Override
        public void apply(GLESObject object) {
            int location = GLES20.glGetUniformLocation(object.getShader().getProgram(), "uAlpha");
            GLES20.glUniform1f(location, ((ImageObject) object).getAlpha());
        }
    };


    class SelectionAnimatorCallback implements GLESAnimatorCallback {
        SelectionAnimatorCallback() {
        }

        @Override
        public void onAnimation(GLESVector3 current) {
            float normalizedValue = current.getX();
            mNormalizedValue = normalizedValue;

            updateDetailViewObject(normalizedValue);

            updateBGObject(normalizedValue);

            show();
        }

        private void updateDetailViewObject(float normalizedValue) {
            mDetailObjects[mCurrentIndex].show();

            float x = mSrcX + (0f - mSrcX) * normalizedValue;
            float y = mSrcY + (0f - mSrcY) * normalizedValue;

            float fromScale = (float) mColumnWidth / mWidth;
            float scale = fromScale + (1f - fromScale) * normalizedValue;

            mDetailObjects[mCurrentIndex].setTranslate(x, y);
            mDetailObjects[mCurrentIndex].setScale(scale);

            GLESVertexInfo vertexInfo = mDetailObjects[mCurrentIndex].getVertexInfo();
            FloatBuffer positionBuffer = (FloatBuffer) vertexInfo.getBuffer(mTextureShader.getPositionAttribIndex());

            int imageWidth = 0;
            int imageHeight = 0;

            if (mDetailImageInfos[mCurrentIndex].getOrientation() == 90 || mDetailImageInfos[mCurrentIndex].getOrientation() == 270) {
                imageWidth = mDetailImageInfos[mCurrentIndex].getHeight();
                imageHeight = mDetailImageInfos[mCurrentIndex].getWidth();
            } else {
                imageWidth = mDetailImageInfos[mCurrentIndex].getWidth();
                imageHeight = mDetailImageInfos[mCurrentIndex].getHeight();
            }

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

            FloatBuffer texBuffer = (FloatBuffer) mDetailObjects[mCurrentIndex].getVertexInfo().getBuffer(mTextureShader.getTexCoordAttribIndex());

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
            if (mIsFinishing == true) {
                mIsFinishing = false;
                mRenderer.onFinished();
            }
        }
    }

    private GLESNodeListener mViewPagerNodeListener = new GLESNodeListener() {
        @Override
        public void update(GLESNode node) {
            GLESTransform transform = node.getTransform();
            transform.setIdentity();
            transform.setTranslate(mDragDistance, 0f, 0f);
        }
    };

    private final GLESAnimatorCallback mSwipeAnimatorCB = new GLESAnimatorCallback() {
        @Override
        public void onAnimation(GLESVector3 current) {
            mDragDistance = current.getX();
        }

        @Override
        public void onCancel() {

        }

        @Override
        public void onFinished() {
            if (mDragDistance > 0f) {
                mCurrentIndex--;
                mPrevIndex--;
                mNextIndex--;

                if (mCurrentIndex < 0) {
                    mCurrentIndex += NUM_OF_DETAIL_OBJECTS;
                }

                if (mPrevIndex < 0) {
                    mPrevIndex += NUM_OF_DETAIL_OBJECTS;
                }

                if (mNextIndex < 0) {
                    mNextIndex += NUM_OF_DETAIL_OBJECTS;
                }

                setPositionCoord(mCurrentIndex);
                setPositionCoord(mNextIndex);

                ImageIndexingInfo current = getPrevImageIndexingInfo(mDetailImageIndexingInfo);
                mDetailImageIndexingInfo = current;

                setPositionCoord(mPrevIndex);

                loadPrevDetailTexture();
            } else if (mDragDistance < 0f) {
                mCurrentIndex++;
                mPrevIndex++;
                mNextIndex++;

                if (mCurrentIndex >= NUM_OF_DETAIL_OBJECTS) {
                    mCurrentIndex -= NUM_OF_DETAIL_OBJECTS;
                }

                if (mPrevIndex >= NUM_OF_DETAIL_OBJECTS) {
                    mPrevIndex -= NUM_OF_DETAIL_OBJECTS;
                }

                if (mNextIndex >= NUM_OF_DETAIL_OBJECTS) {
                    mNextIndex -= NUM_OF_DETAIL_OBJECTS;
                }

                setPositionCoord(mCurrentIndex);
                setPositionCoord(mPrevIndex);

                ImageIndexingInfo current = getNextImageIndexingInfo(mDetailImageIndexingInfo);
                mDetailImageIndexingInfo = current;

                setPositionCoord(mNextIndex);

                loadNextDetailTexture();
            }

            mDragDistance = 0f;
            mSurfaceView.requestRender();
        }
    };
}
