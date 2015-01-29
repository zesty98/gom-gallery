package com.gomdev.gallery;

import android.content.Context;
import android.opengl.GLES20;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESObjectListener;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESVertexInfo;

import java.nio.FloatBuffer;

/**
 * Created by gomdev on 14. 12. 29..
 */
public class Scrollbar implements GridInfoChangeListener {
    static final String CLASS = "Scrollbar";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;

    private GLESObject mScrollbarObject;
    private GLESShader mColorShader;
    private GLESGLState mGLState;

    private GridInfo mGridInfo;

    private int mWidth = 0;
    private int mHeight = 0;

    private float mRed = 0f;
    private float mGreen = 0f;
    private float mBlue = 0f;
    private float mAlpha = 1f;

    private int mActionBarHeight;
    private int mSpacing;
    private int mNumOfColumns;
    private int mColumnWidth;

    private float mScrollbarRegionTop = 0f;
    private float mScrollbarRegionLeft = 0f;
    private int mScrollbarRegionWidth = 0;
    private int mScrollbarRegionHeight = 0;
    private int mScrollbarHeight = 0;
    private float mScrollableDistance = 0f;

    private boolean mIsVisible = true;

    public Scrollbar(Context context) {
        mContext = context;

        init();
    }

    private void init() {
        mGLState = new GLESGLState();
        mGLState.setCullFaceState(true);
        mGLState.setCullFace(GLES20.GL_BACK);
        mGLState.setDepthState(false);
        mGLState.setBlendState(true);
        mGLState.setBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void calcScrollbarHeight() {
        int scrollableHeight = mGridInfo.getScrollableHeight();
        if (scrollableHeight > mHeight) {
            mScrollbarHeight = (int) (((float) mScrollbarRegionHeight / scrollableHeight) * mScrollbarRegionHeight);
        } else {
            mScrollbarHeight = mScrollbarRegionHeight;
        }
    }

    private void calcScrollableDistance() {
        float bottom = mScrollbarRegionTop - mScrollbarHeight;
        mScrollableDistance = bottom + (mHeight * 0.5f - mSpacing);
    }

    public void onSurfaceChanged(int width, int height) {
        mWidth = width;
        mHeight = height;

        int scrollableHeight = mGridInfo.getScrollableHeight();
        if (scrollableHeight < height) {
            mIsVisible = false;
        } else {
            mIsVisible = true;
        }

        mScrollbarRegionWidth = mContext.getResources().getDimensionPixelSize(R.dimen.gridview_scrollbar_width);
        mScrollbarRegionHeight = height - mActionBarHeight - mSpacing * 2;
        mScrollbarRegionTop = height * 0.5f - mActionBarHeight - mSpacing;
        mScrollbarRegionLeft = -width * 0.5f + (mSpacing + mColumnWidth) * mNumOfColumns - mScrollbarRegionWidth;

        calcScrollbarHeight();
        calcScrollableDistance();
    }

    @Override
    public void onColumnWidthChanged() {
        int scrollableHeight = mGridInfo.getScrollableHeight();
        if (scrollableHeight < mHeight) {
            mIsVisible = false;
        } else {
            mIsVisible = true;
        }

        mColumnWidth = mGridInfo.getColumnWidth();
        mNumOfColumns = mGridInfo.getNumOfColumns();

        if (mScrollbarObject == null) {
            return;
        }

        mScrollbarRegionLeft = -mWidth * 0.5f + (mSpacing + mColumnWidth) * mNumOfColumns - mScrollbarRegionWidth;

        GLESVertexInfo vertexInfo = mScrollbarObject.getVertexInfo();
        FloatBuffer position = (FloatBuffer) vertexInfo.getBuffer(mColorShader.getPositionAttribIndex());

        int scrollbarHeight = 0;
        if (scrollableHeight > mHeight) {
            scrollbarHeight = (int) (((float) mScrollbarRegionHeight / scrollableHeight) * mScrollbarRegionHeight);
        } else {
            scrollbarHeight = mScrollbarRegionHeight;
        }

        float top = position.get(7);
        float bottom = top - scrollbarHeight;
        float left = mScrollbarRegionLeft;
        float right = mScrollbarRegionLeft + mScrollbarRegionWidth;

        position.put(0, left);
        position.put(1, bottom);

        position.put(3, right);
        position.put(4, bottom);

        position.put(6, left);

        position.put(9, right);

        mScrollableDistance = bottom + (mHeight * 0.5f - mSpacing);
    }

    @Override
    public void onNumOfImageInfosChanged() {
        mScrollbarHeight = mGridInfo.getScrollableHeight();
    }

    @Override
    public void onNumOfDateLabelInfosChanged() {
        mScrollbarHeight = mGridInfo.getScrollableHeight();
    }

    public void setColor(float r, float g, float b, float a) {
        mRed = r;
        mGreen = g;
        mBlue = b;
        mAlpha = a;
    }

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mSpacing = gridInfo.getSpacing();
        mNumOfColumns = gridInfo.getNumOfColumns();
        mColumnWidth = gridInfo.getColumnWidth();
        mActionBarHeight = gridInfo.getActionBarHeight();

        gridInfo.addListener(this);
    }

    public GLESObject createObject(GLESNode parent) {
        mScrollbarObject = new GLESObject("scrollbar");
        parent.addChild(mScrollbarObject);

        mScrollbarObject.setListener(mScrollbarListener);
        mScrollbarObject.setGLState(mGLState);
        mScrollbarObject.setShader(mColorShader);

        return mScrollbarObject;
    }

    public void setupObject(GLESCamera camera) {
        mScrollbarObject.setCamera(camera);

        GLESVertexInfo vertexInfo = GalleryUtils.createColorVertexInfo(mColorShader,
                mScrollbarRegionLeft, mScrollbarRegionTop,
                mScrollbarRegionWidth, mScrollbarHeight,
                mRed, mGreen, mBlue, mAlpha);
        mScrollbarObject.setVertexInfo(vertexInfo, false, false);

        mScrollbarObject.hide();
    }

    public void setShader(GLESShader shader) {
        mColorShader = shader;
    }

    public void show() {
        if (mScrollbarObject != null && mIsVisible == true) {
            mScrollbarObject.show();
        }
    }

    public void hide() {
        if (mScrollbarObject != null) {
            mScrollbarObject.hide();
        }
    }

    private GLESObjectListener mScrollbarListener = new GLESObjectListener() {

        @Override
        public void update(GLESObject object) {
            if (mIsVisible == false) {
                return;
            }

            GLESTransform transform = object.getTransform();

            transform.setIdentity();

            int scrollableHeight = mGridInfo.getScrollableHeight();
            float translateY = mGridInfo.getTranslateY();

            if (scrollableHeight > mHeight) {
                float scrollDistance = (translateY / (scrollableHeight - mHeight)) * mScrollableDistance;
                transform.setTranslate(0f, -scrollDistance, 0f);
            }
        }

        @Override
        public void apply(GLESObject object) {

        }
    };
}
