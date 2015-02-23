package com.gomdev.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.GLES20;
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
public class DetailViewManager implements GridInfoChangeListener, ViewManager, ImageLoadingListener {
    static final String CLASS = "DetailViewManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = true;//GalleryConfig.DEBUG;

    private final Context mContext;
    private final GridInfo mGridInfo;

    private GallerySurfaceView mSurfaceView = null;
    private ImageListRenderer mRenderer = null;

    private GalleryContext mGalleryContext = null;

    private ImageObject mDetailObject = null;
    private ImageObject mSelectedImageObject = null;
    private ImageObject mBGObject = null;
    private ImageInfo mSelectedImageInfo = null;
    private GLESAnimator mSelectionAnimator = null;

    private GalleryTexture mTexture = null;
    private boolean mIsImageLoaded = false;

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
    private float mDstX = 0f;
    private float mDstY = 0f;

    DetailViewManager(Context context, GridInfo gridInfo) {
        mContext = context;
        mGridInfo = gridInfo;

        setGridInfo(gridInfo);

        mGalleryContext = GalleryContext.getInstance();

        mSelectionAnimator = new GLESAnimator(new SelectionAnimatorCallback());
        mSelectionAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mSelectionAnimator.setDuration(GalleryConfig.SELECTION_ANIMATION_START_OFFSET, GalleryConfig.SELECTION_ANIMATION_END_OFFSET);
    }

    private void setGridInfo(GridInfo gridInfo) {
        mColumnWidth = mGridInfo.getColumnWidth();

        gridInfo.addListener(this);
    }


    // rendering

    @Override
    public void update() {
        if (mIsImageLoaded == true) {
            mIsImageLoaded = false;

            if (mTexture != null) {
                final Bitmap bitmap = mTexture.getBitmapDrawable().getBitmap();
                mTexture.load(bitmap);

                Log.d(TAG, "update() >>> setTexture()");

                mDetailObject.setTexture(mTexture.getTexture());
            }
        }
    }

    @Override
    public void updateAnimation() {
        if (mSelectionAnimator.doAnimation() == true) {
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
    }

    // onSurfaceCreated

    void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        mDetailObject.setShader(mTextureShader);
        mBGObject.setShader(mColorShader);
    }

    void setupObjects(GLESCamera camera) {
        mDetailObject.setCamera(camera);

        GLESVertexInfo vertexInfo = new GLESVertexInfo();

        float[] vertex = GLESUtils.makePositionCoord(-mWidth * 0.5f, mWidth * 0.5f, mWidth, mWidth);
        vertexInfo.setBuffer(mTextureShader.getPositionAttribIndex(), vertex, 3);

        float[] texCoord = GLESUtils.makeTexCoord(0f, 0f, 1f, 1f);
        vertexInfo.setBuffer(mTextureShader.getTexCoordAttribIndex(), texCoord, 2);

        vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
        vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);

        mDetailObject.setVertexInfo(vertexInfo, false, false);

        mBGObject.setCamera(camera);
        mBGObject.setShader(mColorShader);

        vertexInfo = GalleryUtils.createColorVertexInfo(mColorShader,
                -mWidth * 0.5f, mHeight * 0.5f,
                mWidth, mHeight,
                0f, 0f, 0f, 1f);
        mBGObject.setVertexInfo(vertexInfo, false, false);

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
        return true;
    }

    // show / hide
    void show() {
        mBGObject.show();
        mDetailObject.show();
    }

    void hide() {
        mBGObject.hide();
        mDetailObject.hide();
    }


    // initialization

    void createScene(GLESNode node) {
        GLESGLState glState = new GLESGLState();
        glState.setCullFaceState(true);
        glState.setCullFace(GLES20.GL_BACK);
        glState.setBlendState(true);
        glState.setBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        mBGObject = new ImageObject("BG");
        node.addChild(mBGObject);

        mBGObject.setGLState(glState);
        mBGObject.setListener(mBGObjectListener);
        mBGObject.hide();

        mDetailObject = new ImageObject("selectedObject");
        node.addChild(mDetailObject);

        mDetailObject.setGLState(glState);

        mDetailObject.setListener(mSelectedImageObjectListener);
        mDetailObject.hide();
    }

    public void setColorShader(GLESShader colorShader) {
        mColorShader = colorShader;
    }

    public void setTextureShader(GLESShader textureShader) {
        mTextureShader = textureShader;
    }

    void setSurfaceView(GallerySurfaceView surfaceView) {
        mSurfaceView = surfaceView;
        mRenderer = mSurfaceView.getRenderer();
    }

    // touch

    // listener / callback

    @Override
    public void onColumnWidthChanged() {
        mColumnWidth = mGridInfo.getColumnWidth();
    }

    @Override
    public void onNumOfImageInfosChanged() {

    }

    @Override
    public void onNumOfDateLabelInfosChanged() {

    }

    @Override
    public void onImageLoaded(int position, GalleryTexture texture) {
        if (DEBUG) {
            Log.d(TAG, "onImageLoaded()");
        }

        mIsImageLoaded = true;
        mSurfaceView.requestRender();
    }

    void onImageSelected(ImageObject selectedImageObject) {
        mSelectedImageObject = selectedImageObject;

        ImageIndexingInfo imageIndexingInfo = mGalleryContext.getImageIndexingInfo();
        mSelectedImageInfo = ImageManager.getInstance().getImageInfo(imageIndexingInfo);

        GLESVertexInfo vertexInfo = selectedImageObject.getVertexInfo();
        FloatBuffer texCoordBuffer = (FloatBuffer) vertexInfo.getBuffer(mTextureShader.getTexCoordAttribIndex());
        if (texCoordBuffer == null) {
            float[] texCoord = GalleryUtils.calcTexCoord(mSelectedImageInfo.getWidth(), mSelectedImageInfo.getHeight());
            vertexInfo.setBuffer(mTextureShader.getTexCoordAttribIndex(), texCoord, 2);
        }

        RectF viewport = mGalleryContext.getCurrentViewport();

        mDstX = 0f;
        mDstY = 0f;

        mSrcX = mSelectedImageObject.getTranslateX();
        mSrcY = mHeight * 0.5f - (viewport.bottom - mSelectedImageObject.getTranslateY());

        mTexture = new GalleryTexture(mSelectedImageInfo.getWidth(), mSelectedImageInfo.getHeight());
        mTexture.setImageLoadingListener(this);

        ImageLoader.getInstance().loadBitmap(mSelectedImageInfo, mTexture, mRequestWidth, mRequestHeight);

        mDetailObject.setTexture(mSelectedImageObject.getTexture());

        mSelectionAnimator.setValues(0f, 1f);
        mSelectionAnimator.cancel();
        mSelectionAnimator.start();

        mSurfaceView.requestRender();
    }

    void finish() {
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

    private GLESObjectListener mSelectedImageObjectListener = new GLESObjectListener() {
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

            transform.setTranslate(mDstX, mDstY, 0f);
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
            mDetailObject.show();

            float x = mSrcX + (mDstX - mSrcX) * normalizedValue;
            float y = mSrcY + (mDstY - mSrcY) * normalizedValue;

            float fromScale = (float) mColumnWidth / mWidth;
            float scale = fromScale + (1f - fromScale) * normalizedValue;

            mDetailObject.setTranslate(x, y);
            mDetailObject.setScale(scale);

            GLESVertexInfo vertexInfo = mDetailObject.getVertexInfo();
            FloatBuffer positionBuffer = (FloatBuffer) vertexInfo.getBuffer(mTextureShader.getPositionAttribIndex());

            int imageWidth = 0;
            int imageHeight = 0;

            if (mSelectedImageInfo.getOrientation() == 90 || mSelectedImageInfo.getOrientation() == 270) {
                imageWidth = mSelectedImageInfo.getHeight();
                imageHeight = mSelectedImageInfo.getWidth();
            } else {
                imageWidth = mSelectedImageInfo.getWidth();
                imageHeight = mSelectedImageInfo.getHeight();
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

            FloatBuffer srcTexBuffer = (FloatBuffer) mSelectedImageObject.getVertexInfo().getBuffer(mTextureShader.getTexCoordAttribIndex());

            float minS = srcTexBuffer.get(0);
            float minT = srcTexBuffer.get(5);

            float maxS = srcTexBuffer.get(2);
            float maxT = srcTexBuffer.get(1);

            float currentMinS = minS + (0f - minS) * normalizedValue;
            float currentMinT = minT + (0f - minT) * normalizedValue;

            float currentMaxS = maxS + (1f - maxS) * normalizedValue;
            float currentMaxT = maxT + (1f - maxT) * normalizedValue;

            FloatBuffer texBuffer = (FloatBuffer) mDetailObject.getVertexInfo().getBuffer(mTextureShader.getTexCoordAttribIndex());

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
}
