package com.gomdev.gallery;

import android.content.Context;
import android.opengl.GLES20;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESNodeListener;
import com.gomdev.gles.GLESRenderer;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;

/**
 * Created by gomdev on 14. 12. 31..
 */
public class ObjectManager implements GridInfoChangeListener {
    static final String CLASS = "ObjectManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;
    private final ImageListRenderer mRenderer;

    private GLESRenderer mGLESRenderer;
    private GLESSceneManager mSM;
    private GLESNode mRoot;
    private GLESNode mImageNode;

    private DateLabelObjects mDateLabelObjects = null;
    private ImageObjects mImageObjects = null;
    private Scrollbar mScrollbar = null;

    private GridInfo mGridInfo = null;
    private BucketInfo mBucketInfo = null;
    private int mSpacing;
    private int mNumOfColumns;
    private int mNumOfDateInfos;
    private int mColumnWidth;

    private int mDateLabelHeight;

    private int mWidth;
    private int mHeight;

    private boolean mIsSurfaceChanged = false;

    public ObjectManager(Context context, ImageListRenderer renderer) {
        mContext = context;
        mRenderer = renderer;

        init();
    }

    private void init() {
        mGLESRenderer = GLESRenderer.createRenderer();

        mDateLabelObjects = new DateLabelObjects(mContext, mRenderer);
        mImageObjects = new ImageObjects(mContext, mRenderer);

        GLESGLState glState = new GLESGLState();
        glState.setCullFaceState(true);
        glState.setCullFace(GLES20.GL_BACK);
        glState.setDepthState(false);

        mImageObjects.setGLState(glState);

        glState = new GLESGLState();
        glState.setCullFaceState(true);
        glState.setCullFace(GLES20.GL_BACK);
        glState.setBlendState(true);
        glState.setBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        mDateLabelObjects.setGLState(glState);


        mScrollbar = new Scrollbar(mContext);
        mScrollbar.setColor(0.3f, 0.3f, 0.3f, 0.7f);

        mIsSurfaceChanged = false;
    }

    // rendering

    public void update() {

        mGLESRenderer.updateScene(mSM);

        float translateY = mGridInfo.getTranslateY();

        mScrollbar.setTranslateY(translateY);

        updateTexture();

        checkVisibility(translateY);

        mImageObjects.update();
        mDateLabelObjects.update();
    }

    private void updateTexture() {
        mDateLabelObjects.updateTexture();
        mImageObjects.updateTexture();
    }

    private void checkVisibility(float translateY) {
        mDateLabelObjects.checkVisibility(translateY);
        mImageObjects.checkVisibility(translateY);
    }

    public void drawFrame() {
        mGLESRenderer.drawScene(mSM);

        mScrollbar.hide();
    }

    // onSurfaceChanged

    public void onSurfaceChanged(int width, int height) {
        mWidth = width;
        mHeight = height;

        mGLESRenderer.reset();

        mDateLabelObjects.onSurfaceChanged(width, height);
        mImageObjects.onSurfaceChanged(width, height);
        mScrollbar.onSurfaceChanged(width, height);

        mIsSurfaceChanged = true;
    }

    public void setupObjects(GLESCamera camera) {
        mImageObjects.setupImageObjects(camera);
        mDateLabelObjects.setupDateLabelObjects(camera);
        mScrollbar.setupObject(camera);
    }

    // onSurfaceCreated

    public void onSurfaceCreated() {
        mIsSurfaceChanged = false;

        createShader();
    }

    private boolean createShader() {
        GLESShader textureShader = createTextureShader(R.raw.texture_20_vs, R.raw.texture_20_fs);
        if (textureShader == null) {
            return false;
        }

        mImageObjects.setShader(textureShader);

        GLESShader textureAlphaShader = createTextureShader(R.raw.texture_20_vs, R.raw.texture_alpha_20_fs);
        if (textureAlphaShader == null) {
            return false;
        }
        mDateLabelObjects.setShader(textureAlphaShader);

        GLESShader colorShader = createColorShader(R.raw.color_20_vs, R.raw.color_20_fs);
        if (colorShader == null) {
            return false;
        }

        mScrollbar.setShader(colorShader);

        return true;
    }

    public void createScene() {
        mSM = GLESSceneManager.createSceneManager();
        mRoot = mSM.createRootNode("root");

        mImageNode = mSM.createNode("imageNode");
        mImageNode.setListener(mImageNodeListener);
        mRoot.addChild(mImageNode);

        mDateLabelObjects.createObjects(mImageNode);
        mImageObjects.createObjects(mImageNode);

        mScrollbar.createObject(mRoot);
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

    // initialization

    public void setSurfaceView(GallerySurfaceView surfaceView) {
        mDateLabelObjects.setSurfaceView(surfaceView);
        mImageObjects.setSurfaceView(surfaceView);
    }

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mBucketInfo = gridInfo.getBucketInfo();
        mDateLabelHeight = gridInfo.getDateLabelHeight();
        mSpacing = gridInfo.getSpacing();
        mNumOfDateInfos = gridInfo.getNumOfDateInfos();
        mColumnWidth = mGridInfo.getColumnWidth();
        mNumOfColumns = mGridInfo.getNumOfColumns();

        mDateLabelObjects.setGridInfo(mGridInfo);
        mImageObjects.setGridInfo(mGridInfo);
        mScrollbar.setGridInfo(gridInfo);

        gridInfo.addListener(this);
    }

    public void setDummyImageTexture(GLESTexture dummyTexture) {
        mImageObjects.setDummyTexture(dummyTexture);
    }

    public void setDummyDateLabelTexture(GLESTexture dummyTexture) {
        mDateLabelObjects.setDummyTexture(dummyTexture);
    }

    public int getSelectedIndex(float x, float y) {
        float translateY = mGridInfo.getTranslateY();

        float yPos = mHeight * 0.5f - (y + translateY);

        int selectedDateLabelIndex = getDateLabelIndexFromYPos(yPos);
        int index = getImageIndexFromYPos(x, yPos, selectedDateLabelIndex);

        DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(selectedDateLabelIndex);
        int lastImageIndex = dateLabelInfo.getLastImagePosition();

        if (index > lastImageIndex) {
            return -1;
        }

        return index;
    }

    public int getNearestIndex(float x, float y) {
        float translateY = mGridInfo.getTranslateY();

        float yPos = mHeight * 0.5f - (y + translateY);

        int selectedDateLabelIndex = getDateLabelIndexFromYPos(yPos);
        int index = getImageIndexFromYPos(x, yPos, selectedDateLabelIndex);

        DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(selectedDateLabelIndex);
        int lastImageIndex = dateLabelInfo.getLastImagePosition();

        if (index > lastImageIndex) {
            return lastImageIndex;
        }

        return index;
    }

    private int getImageIndexFromYPos(float x, float yPos, int selectedDateLabelIndex) {
        GalleryObject dateLabelObject = mDateLabelObjects.getObject(selectedDateLabelIndex);
        float imageStartOffset = dateLabelObject.getTop() - mDateLabelHeight - mSpacing;
        float yDistFromDateLabel = imageStartOffset - yPos;

        DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(selectedDateLabelIndex);

        int row = (int) (yDistFromDateLabel / (mColumnWidth + mSpacing));
        int column = (int) (x / (mColumnWidth + mSpacing));

        int firstImageIndex = dateLabelInfo.getFirstImagePosition();

        return mNumOfColumns * row + column + firstImageIndex;
    }

    private int getDateLabelIndexFromYPos(float yPos) {
        int selectedDateLabelIndex = 0;
        for (int i = 0; i < mNumOfDateInfos; i++) {
            GalleryObject dateLabelObject = mDateLabelObjects.getObject(i);
            if (yPos > dateLabelObject.getTop()) {
                selectedDateLabelIndex = i - 1;
                break;
            }
            selectedDateLabelIndex = i; // for last DateLabel
        }

        return selectedDateLabelIndex;
    }

    public GalleryObject getImageObject(int index) {
        return mImageObjects.getObject(index);
    }

    @Override
    public void onGridInfoChanged() {
        mColumnWidth = mGridInfo.getColumnWidth();
        mNumOfColumns = mGridInfo.getNumOfColumns();

        if (mIsSurfaceChanged == false) {
            return;
        }

        mDateLabelObjects.hide();
    }

    private GLESNodeListener mImageNodeListener = new GLESNodeListener() {
        @Override
        public void update(GLESNode node) {
            GLESTransform transform = node.getTransform();
            float[] matrix = transform.getMatrix();
            float prevY = matrix[13];

            transform.setIdentity();

            float angle = mGridInfo.getRotateX();
            transform.rotate(angle, 1f, 0f, 0f);

            float translateZ = mGridInfo.getTranslateZ();
            transform.translate(0f, 0f, translateZ);

            float scrollDistance = mGridInfo.getTranslateY();
            transform.preTranslate(0f, scrollDistance, 0f);

            float currentY = scrollDistance;

            if (prevY != currentY) {
                mScrollbar.show();
            }
        }
    };
}
