package com.gomdev.gallery;

import android.content.Context;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.gomdev.gles.GLESAnimator;
import com.gomdev.gles.GLESAnimatorCallback;
import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESObjectListener;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVector3;
import com.gomdev.gles.GLESVertexInfo;

import java.nio.FloatBuffer;

/**
 * Created by gomdev on 15. 2. 17..
 */
public class DetailViewManager implements GridInfoChangeListener {
    static final String CLASS = "DetailViewManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;
    private final GridInfo mGridInfo;

    private GallerySurfaceView mSurfaceView = null;

    private GalleryContext mGalleryContext = null;

    private ImageObject mDetailObject = null;
    private ImageObject mSelectedImageObject = null;
    private ImageObject mBGObject = null;
    private ImageInfo mSelectedImageInfo = null;
    private GLESAnimator mSelectionAnimator = null;

    private GLESShader mTextureShader = null;
    private GLESShader mColorShader = null;

    private int mWidth = 0;
    private int mHeight = 0;

    private int mColumnWidth = 0;

    private float mTranslateX = 0f;
    private float mTranslateY = 0f;
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

    void update() {
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
    }

    // onSurfaceCreated

    void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        createShader();
    }

    private boolean createShader() {
        mTextureShader = createTextureShader(R.raw.texture_20_vs, R.raw.texture_20_fs);
        if (mTextureShader == null) {
            return false;
        }

        mDetailObject.setShader(mTextureShader);

        mColorShader = createColorShader(R.raw.color_20_vs, R.raw.color_alpha_20_fs);
        if (mColorShader == null) {
            return false;
        }

        mBGObject.setShader(mColorShader);

        return true;
    }

    private GLESShader createTextureShader(int vsResID, int fsResID) {
        GLESShader textureShader = new GLESShader(mContext);

        String vsSource = GLESUtils.getStringFromReosurce(mContext, vsResID);
        String fsSource = GLESUtils.getStringFromReosurce(mContext, fsResID);

        textureShader.setShaderSource(vsSource, fsSource);
        if (textureShader.load() == false) {
            return null;
        }

        String attribName = GLESShaderConstant.ATTRIB_POSITION;
        textureShader.setPositionAttribIndex(attribName);

        attribName = GLESShaderConstant.ATTRIB_TEXCOORD;
        textureShader.setTexCoordAttribIndex(attribName);

        return textureShader;
    }

    private GLESShader createColorShader(int vsResID, int fsResID) {
        GLESShader colorShader = new GLESShader(mContext);

        String vsSource = GLESUtils.getStringFromReosurce(mContext, vsResID);
        String fsSource = GLESUtils.getStringFromReosurce(mContext, fsResID);

        colorShader.setShaderSource(vsSource, fsSource);
        if (colorShader.load() == false) {
            return null;
        }

        String attribName = GLESShaderConstant.ATTRIB_POSITION;
        colorShader.setPositionAttribIndex(attribName);

        attribName = GLESShaderConstant.ATTRIB_COLOR;
        colorShader.setColorAttribIndex(attribName);

        return colorShader;
    }

    void setupObject(GLESCamera camera) {
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

    // touch

    // listener / callback

    @Override
    public void onColumnWidthChanged() {

    }

    @Override
    public void onNumOfImageInfosChanged() {

    }

    @Override
    public void onNumOfDateLabelInfosChanged() {

    }

    void onImageSelected(ImageObject selectedImageObject) {
        mSelectedImageObject = selectedImageObject;

        RectF viewport = mGalleryContext.getCurrentViewport();
        ImageIndexingInfo imageIndexingInfo = mGalleryContext.getImageIndexingInfo();

        mDstX = viewport.centerX();
        mDstY = viewport.centerY();

        mSelectedImageInfo = ImageManager.getInstance().getImageInfo(imageIndexingInfo);

        mTranslateX = mSelectedImageObject.getTranslateX();
        mTranslateY = mSelectedImageObject.getTranslateY();

        mSelectedImageObject.hide();

        mDetailObject.setTexture(mSelectedImageObject.getTexture());

        mSelectionAnimator.setValues(0f, 1f);
        mSelectionAnimator.cancel();
        mSelectionAnimator.start();

        mSurfaceView.requestRender();
    }

    // set / get

    void setSurfaceView(GallerySurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

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

            updateDetailViewObject(normalizedValue);

            updateBGObject(normalizedValue);
        }

        private void updateDetailViewObject(float normalizedValue) {
            mDetailObject.show();

            float x = mTranslateX + (mDstX - mTranslateX) * normalizedValue;
            float y = mTranslateY + (mDstY - mTranslateY) * normalizedValue;

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
//            mDetailObject.hide();
        }
    }
}
