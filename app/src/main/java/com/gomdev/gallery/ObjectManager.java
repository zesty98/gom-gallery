package com.gomdev.gallery;

import android.content.Context;
import android.opengl.GLES20;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESUtils;

/**
 * Created by gomdev on 14. 12. 31..
 */
public class ObjectManager implements GridInfoChangeListener {
    static final String CLASS = "ObjectManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;

    private DateLabelObjects mDateLabelObjects = null;
    private ImageObjects mImageObjects = null;

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

    public ObjectManager(Context context) {
        mContext = context;

        init();
    }

    private void init() {
        mDateLabelObjects = new DateLabelObjects(mContext);
        mImageObjects = new ImageObjects(mContext);

        GLESGLState glState = new GLESGLState();
        glState.setCullFaceState(true);
        glState.setCullFace(GLES20.GL_BACK);
        glState.setDepthState(false);

        mDateLabelObjects.setGLState(glState);
        mImageObjects.setGLState(glState);

        mIsSurfaceChanged = false;
    }

    // rendering

    public void updateTexture() {
        mDateLabelObjects.updateTexture();
        mImageObjects.updateTexture();
    }

    public void checkVisibility(float translateY) {
        mDateLabelObjects.checkVisibility(translateY);
        mImageObjects.checkVisibility(translateY);
    }

    // onSurfaceChanged

    public void onSurfaceChanged(int width, int height) {
        mWidth = width;
        mHeight = height;

        mDateLabelObjects.onSurfaceChanged(width, height);
        mImageObjects.onSurfaceChanged(width, height);

        mIsSurfaceChanged = true;
    }

    public void setupObjects(GLESCamera camera) {
        mImageObjects.setupImageObjects(camera);
        mDateLabelObjects.setupDateLabelObjects(camera);
    }

    // onSurfaceCreated

    public void onSurfaceCreated() {
        mIsSurfaceChanged = false;
    }

    public void createObjects(GLESNode parentNode) {
        mDateLabelObjects.createObjects(parentNode);
        mImageObjects.createObjects(parentNode);
    }

    public boolean createShader(int vsResID, int fsResID) {
        GLESShader textureShader = new GLESShader(mContext);

        String vsSource = GLESUtils.getStringFromReosurce(mContext, vsResID);
        String fsSource = GLESUtils.getStringFromReosurce(mContext, fsResID);

        textureShader.setShaderSource(vsSource, fsSource);
        if (textureShader.load() == false) {
            return false;
        }

        String attribName = GLESShaderConstant.ATTRIB_POSITION;
        textureShader.setPositionAttribIndex(attribName);

        attribName = GLESShaderConstant.ATTRIB_TEXCOORD;
        textureShader.setTexCoordAttribIndex(attribName);

        mImageObjects.setShader(textureShader);
        mDateLabelObjects.setShader(textureShader);

        return true;
    }

    // initialization

    public void setSurfaceView(GallerySurfaceView surfaceView) {
        mDateLabelObjects.setSurfaceView(surfaceView);
        mImageObjects.setSurfaceView(surfaceView);
    }

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mBucketInfo = gridInfo.getBucketInfo();
        mDateLabelHeight = mGridInfo.getDateLabelHeight();
        mSpacing = mGridInfo.getSpacing();
        mNumOfDateInfos = mGridInfo.getNumOfDateInfos();

        mDateLabelObjects.setGridInfo(mGridInfo);
        mImageObjects.setGridInfo(mGridInfo);
    }

    public void setDummyImageTexture(GLESTexture dummyTexture) {
        mImageObjects.setDummyTexture(dummyTexture);
    }

    public void setDummyDateLabelTexture(GLESTexture dummyTexture) {
        mDateLabelObjects.setDummyTexture(dummyTexture);
    }

    public int getSelectedIndex(float x, float y, float translateY) {
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

    public int getNearestIndex(float x, float y, float translateY) {
        float yPos = mHeight * 0.5f - (y + translateY);

        int selectedDateLabelIndex = getDateLabelIndexFromYPos(yPos);
        int index = getImageIndexFromYPos(x, yPos, selectedDateLabelIndex);

        DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(selectedDateLabelIndex);
        int lastImageIndex = dateLabelInfo.getLastImagePosition();

        if (index > lastImageIndex) {
            return (index - 1);
        }

        return index;
    }

    private int getImageIndexFromYPos(float x, float yPos, int selectedDateLabelIndex) {
        GalleryObject dateLabelObject = mDateLabelObjects.getObject(selectedDateLabelIndex);
        float imageStartOffset = dateLabelObject.getTop() - mDateLabelHeight - mSpacing;
        float yDistFromDateLabel = imageStartOffset - yPos;

        int row = (int) (yDistFromDateLabel / (mColumnWidth + mSpacing));
        int column = (int) (x / (mColumnWidth + mSpacing));

        DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(selectedDateLabelIndex);
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

    @Override
    public void onGridInfoChanged() {
        mColumnWidth = mGridInfo.getColumnWidth();
        mNumOfColumns = mGridInfo.getNumOfColumns();

        if (mIsSurfaceChanged == false) {
            return;
        }

        mDateLabelObjects.onGridInfoChanged(mGridInfo);
        mImageObjects.onGridInfoChanged(mGridInfo);
    }
}
