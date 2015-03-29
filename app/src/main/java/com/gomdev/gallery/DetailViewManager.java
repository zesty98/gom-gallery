package com.gomdev.gallery;

import android.content.Context;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.gomdev.gles.GLESAnimator;
import com.gomdev.gles.GLESAnimatorCallback;
import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESObjectListener;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVector3;
import com.gomdev.gles.GLESVertexInfo;

import java.nio.FloatBuffer;

/**
 * Created by gomdev on 15. 2. 17..
 */
public class DetailViewManager implements GridInfoChangeListener, ViewManager {
    static final String CLASS = "DetailViewManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;
    private final GridInfo mGridInfo;

    private Handler mHandler = null;

    private GallerySurfaceView mSurfaceView = null;
    private ImageListRenderer mRenderer = null;

    private GalleryContext mGalleryContext = null;
    private ImageManager mImageManager = null;

    private DetailViewPager mPager = null;

    private ImageObject mBGObject = null;
    private ImageObject mCurrentDetailObject = null;
    private GLESObjectListener mBackupListener = null;

    private GLESAnimator mSelectionAnimator = null;
    private ImageObject mSelectedImageObject = null;

    private ImageIndexingInfo mCurrentImageIndexingInfo = null;

    private boolean mIsFinishing = false;

    private float mNormalizedValue = 0f;

    private GLESShader mColorShader = null;
    private GLESShader mTextureAlphaShader = null;

    private int mWidth = 0;
    private int mHeight = 0;
    private float mScreenRatio = 0f;

    private int mColumnWidth = 0;

    private float mSrcX = 0f;
    private float mSrcY = 0f;

    private float mMinS = 0f;
    private float mMinT = 0f;
    private float mMaxS = 1f;
    private float mMaxT = 1f;

    // touch
    private boolean mIsOnSelectionAnimation = false;

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

        mGalleryContext = GalleryContext.getInstance();
        mImageManager = ImageManager.getInstance();

        mPager = new DetailViewPager(mContext, mGridInfo);

        mSelectionAnimator = new GLESAnimator(mSelectionAnimatorCB);
        mSelectionAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mSelectionAnimator.setDuration(GalleryConfig.SELECTION_ANIMATION_START_OFFSET, GalleryConfig.SELECTION_ANIMATION_END_OFFSET);
    }

    private void setGridInfo(GridInfo gridInfo) {
        mColumnWidth = mGridInfo.getColumnWidth();

        gridInfo.addListener(this);
    }

    // rendering

    @Override
    public void update(long currentTime) {
        mPager.update();
    }

    @Override
    public void updateAnimation(long currentTime) {
        if (mSelectionAnimator.doAnimation() == true) {
            mSurfaceView.requestRender();
        }

        mPager.updateAnimation();
    }


    // onSurfaceChanged

    void onSurfaceChanged(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged()");
        }

        mWidth = width;
        mHeight = height;
        mScreenRatio = (float) mWidth / mHeight;

        mPager.onSurfaceChanged(width, height);
    }

    // onSurfaceCreated

    void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        mBGObject.setShader(mColorShader);

        mPager.onSurfaceCreated();
    }

    void setupObjects(GLESCamera camera) {
        if (DEBUG) {
            Log.d(TAG, "setupObjects()");
        }
        mBGObject.setCamera(camera);

        GLESVertexInfo vertexInfo = GalleryUtils.createColorVertexInfo(mColorShader,
                -mWidth * 0.5f, mHeight * 0.5f,
                mWidth, mHeight,
                1f, 1f, 1f, 1f);

        vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
        vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);

        mBGObject.setVertexInfo(vertexInfo, false, false);

        mPager.setupObjects(camera);
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

    public boolean onTouchEvent(MotionEvent event) {
        if (mIsOnSelectionAnimation == false) {
            mPager.onTouchEvent(event);
        }

        mSurfaceView.requestRender();

        return true;
    }

    // show / hide
    void show() {
        mBGObject.show();

        mPager.show();
    }

    void hide() {
        mBGObject.hide();

        mPager.hide();
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
        glState.setDepthState(false);

        mBGObject = new ImageObject("BG");
        node.addChild(mBGObject);

        mBGObject.setGLState(glState);
        mBGObject.setListener(mBGObjectListener);
        mBGObject.hide();

        mPager.createScene(node);

        mCurrentDetailObject = mPager.getCurrentDetailObject();
    }

    public void setColorShader(GLESShader colorShader) {
        if (DEBUG) {
            Log.d(TAG, "setColorShader()");
        }

        mColorShader = colorShader;
    }

    public void setTextureAlphaShader(GLESShader textureShader) {
        if (DEBUG) {
            Log.d(TAG, "setTextureAlphaShader()");
        }

        mTextureAlphaShader = textureShader;
        mPager.setTextureAlphaShader(textureShader);
    }

    void setSurfaceView(GallerySurfaceView surfaceView) {
        if (DEBUG) {
            Log.d(TAG, "setSurfaceView()");
        }

        mSurfaceView = surfaceView;
        mRenderer = mSurfaceView.getRenderer();

        mPager.setSurfaceView(surfaceView);
    }

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

    void onImageSelected(ImageObject selectedImageObject) {
        if (DEBUG) {
            Log.d(TAG, "onImageSelected()");
        }

        mSelectedImageObject = selectedImageObject;
        mCurrentImageIndexingInfo = mGalleryContext.getImageIndexingInfo();

        mPager.onImageSelected(selectedImageObject);

        setupSelectionStartingAnimationInfo();

        mBackupListener = mCurrentDetailObject.getListener();
        mCurrentDetailObject.setListener(mDetailImageObjectListener);

        mPager.hide();

        mSelectionAnimator.setValues(0f, 1f);
        mSelectionAnimator.cancel();
        mSelectionAnimator.start();

        mIsOnSelectionAnimation = true;

        mSurfaceView.requestRender();
    }

    private void setupSelectionStartingAnimationInfo() {
        ImageInfo imageInfo = mImageManager.getImageInfo(mCurrentImageIndexingInfo);

        RectF viewport = mGalleryContext.getCurrentViewport();

        mSrcX = mSelectedImageObject.getTranslateX();
        mSrcY = mHeight * 0.5f - (viewport.bottom - mSelectedImageObject.getTranslateY());

        GLESVertexInfo vertexInfo = mCurrentDetailObject.getVertexInfo();
        float[] vertex = GLESUtils.makePositionCoord(-mWidth * 0.5f, mWidth * 0.5f, mWidth, mWidth);
        vertexInfo.setBuffer(mTextureAlphaShader.getPositionAttribIndex(), vertex, 3);

        calcTexCoordInfo(imageInfo);

        float[] texCoord = GLESUtils.makeTexCoord(mMinS, mMinT, mMaxS, mMaxT);
        vertexInfo.setBuffer(mTextureAlphaShader.getTexCoordAttribIndex(), texCoord, 2);
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

        mCurrentDetailObject = mPager.getCurrentDetailObject();
        mCurrentImageIndexingInfo = mPager.getCurrentImageIndexingInfo();

        setupSelectionFinishingAnimationInfo();

        mBackupListener = mCurrentDetailObject.getListener();
        mCurrentDetailObject.setListener(mDetailImageObjectListener);

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

        mPager.setHandler(handler);
    }

    // member class


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
            int location = GLES20.glGetUniformLocation(object.getShader().getProgram(), "uAlpha");
            GLES20.glUniform1f(location, 1f);
        }
    };

    private final GLESAnimatorCallback mSelectionAnimatorCB = new GLESAnimatorCallback() {
        @Override
        public void onAnimation(GLESVector3 current) {
            float normalizedValue = current.getX();
            mNormalizedValue = normalizedValue;

            updateDetailViewObject(normalizedValue);

            updateBGObject(normalizedValue);

            mCurrentDetailObject.show();
        }

        private void updateDetailViewObject(float normalizedValue) {
            mCurrentDetailObject.show();

            float x = mSrcX + (0f - mSrcX) * normalizedValue;
            float y = mSrcY + (0f - mSrcY) * normalizedValue;

            float fromScale = (float) mColumnWidth / mWidth;
            float scale = fromScale + (1f - fromScale) * normalizedValue;

            mCurrentDetailObject.setTranslate(x, y);
            mCurrentDetailObject.setScale(scale);

            GLESVertexInfo vertexInfo = mCurrentDetailObject.getVertexInfo();
            FloatBuffer positionBuffer = (FloatBuffer) vertexInfo.getBuffer(mTextureAlphaShader.getPositionAttribIndex());

            ImageInfo imageInfo = mImageManager.getImageInfo(mCurrentImageIndexingInfo);

            int imageWidth = imageInfo.getWidth();
            int imageHeight = imageInfo.getHeight();

            float prevTop = mWidth * 0.5f;
            float nextTop = 0f;

            float prevRight = mWidth * 0.5f;
            float nextRight = 0f;

            float imageRatio = (float) imageWidth / imageHeight;

            if (imageRatio >= mScreenRatio) {
                nextTop = ((float) imageHeight / imageWidth) * mWidth * 0.5f;
                nextRight = mWidth * 0.5f;
            } else {
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

            FloatBuffer texBuffer = (FloatBuffer) mCurrentDetailObject.getVertexInfo().getBuffer(mTextureAlphaShader.getTexCoordAttribIndex());

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
            mCurrentDetailObject.setScale(1.0f);

            if (mIsFinishing == true) {
                mIsFinishing = false;

                Message msg = mHandler.obtainMessage(ImageListActivity.UPDATE_ACTION_BAR_TITLE);
                BucketInfo bucketInfo = mImageManager.getBucketInfo(mCurrentImageIndexingInfo.mBucketIndex);
                msg.obj = bucketInfo.getName();
                mHandler.sendMessage(msg);

                mRenderer.onFinished(true);
            } else {
//                mHandler.sendEmptyMessage(ImageListActivity.INVALIDATE_OPTION_MENU);

                mPager.show();
                mRenderer.onFinished(false);
            }

            mCurrentDetailObject.setListener(mBackupListener);

            mIsOnSelectionAnimation = false;
        }
    };
}
