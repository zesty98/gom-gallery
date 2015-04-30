package com.gomdev.gallery;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

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
 * Created by gomdev on 14. 12. 29..
 */
class Scrollbar implements GridInfoChangeListener {
    static final String CLASS = "Scrollbar";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    enum ScrollbarMode {
        NORMAL(0),
        SCROLLBAR_DRAGGING(1),
        NUM_OF_MODE(2);

        private final int mIndex;

        ScrollbarMode(int index) {
            mIndex = index;
        }

        int getIndex() {
            return mIndex;
        }
    }

    private final Context mContext;
    private final GridInfo mGridInfo;

    private GallerySurfaceView mSurfaceView = null;
    private GLESObject mObject = null;
    private GLESShader mShader = null;
    private GLESGLState mGLState = null;

    private GLESAnimator mAnimator = null;

    private int mWidth = 0;
    private int mHeight = 0;

    private float[] mRed = new float[ScrollbarMode.NUM_OF_MODE.getIndex()];
    private float[] mGreen = new float[ScrollbarMode.NUM_OF_MODE.getIndex()];
    private float[] mBlue = new float[ScrollbarMode.NUM_OF_MODE.getIndex()];
    private float[] mAlpha = new float[ScrollbarMode.NUM_OF_MODE.getIndex()];

    private float mBlendingAlpha = 1f;

    private int mSystemBarHeight;
    private int mSpacing;
    private int mNumOfColumns;
    private int mColumnWidth;

    private float mScrollbarRegionTop = 0f;
    private float mScrollbarRegionLeft = 0f;
    private float[] mScrollbarRegionWidth = new float[ScrollbarMode.NUM_OF_MODE.getIndex()];
    private float mScrollbarRegionHeight = 0f;

    private float mScrollbarHeight = 0;
    private float mScrollableDistance = 0f;
    private float mScrollbarMinHeight = 0f;

    private float mScrollDistance = 0f;
    private ScrollbarMode mScrollbarMode = ScrollbarMode.NORMAL;

    private FloatBuffer mNormalBuffer = null;
    private FloatBuffer mDraggingBuffer = null;

    private boolean mIsPrevScrolling = false;

    private boolean mIsVisible = true;

    Scrollbar(Context context, GridInfo gridInfo) {
        mContext = context;
        mGridInfo = gridInfo;

        setGridInfo(gridInfo);

        init();
    }

    private void setGridInfo(GridInfo gridInfo) {
        mSpacing = gridInfo.getSpacing();
        mNumOfColumns = gridInfo.getNumOfColumns();
        mColumnWidth = gridInfo.getColumnWidth();
        mSystemBarHeight = gridInfo.getSystemBarHeight();

        gridInfo.addListener(this);
    }

    private void init() {
        mGLState = new GLESGLState();
        mGLState.setCullFaceState(true);
        mGLState.setCullFace(GLES20.GL_BACK);
        mGLState.setDepthState(false);
        mGLState.setBlendState(true);
        mGLState.setBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        mScrollbarMinHeight = GLESUtils.getPixelFromDpi(mContext, GalleryConfig.SCROLLBAR_MIN_HEIGHT_IN_DP);

        mAnimator = new GLESAnimator(1f, 0f, new ScrollbarAnimatorCallback());
        mAnimator.setDuration(0L, GalleryConfig.SCROLLBAR_ANIMATION_DURATION);

        int index = ScrollbarMode.SCROLLBAR_DRAGGING.getIndex();
        int color = mContext.getResources().getColor(R.color.actionBarTitleText);
        mAlpha[index] = 0.8f;
        mRed[index] = (color & 0x00FF0000) >> 16;
        mRed[index] /= 255f;
        mGreen[index] = (color & 0x0000FF00) >> 8;
        mGreen[index] /= 255f;
        mBlue[index] = (color & 0x000000FF);
        mBlue[index] /= 255f;
    }

    void update(boolean isOnScrolling) {
        if (isOnScrolling == true) {
            show();

            mAnimator.cancel();

            mBlendingAlpha = 1.0f;
        }

        if (mAnimator.doAnimation() == true) {
            mSurfaceView.requestRender();
        }

        if (isOnScrolling == false && mIsPrevScrolling == true) {
            mAnimator.cancel();
            mAnimator.setValues(1f, 0f);
            mAnimator.start();
        }

        if (mNeedToUpdateColor == true) {
            mNeedToUpdateColor = false;

            changeMode();
        }

        mIsPrevScrolling = isOnScrolling;
    }

    private synchronized void changeMode() {
        mShader.useProgram();

        int index = mScrollbarMode.getIndex();
        int location = mShader.getUniformLocation("uColor");
        GLES20.glUniform4f(location, mRed[index], mGreen[index], mBlue[index], mAlpha[index]);

        GLESVertexInfo vertexInfo = mObject.getVertexInfo();

        switch (mScrollbarMode) {
            case NORMAL:
                vertexInfo.setBuffer(mShader.getPositionAttribIndex(), mNormalBuffer);

                mScrollbarRegionLeft = mWidth * 0.5f - mScrollbarRegionWidth[index] - mSpacing * 2;

                mAnimator.cancel();
                mAnimator.setValues(1f, 0f);
                mAnimator.start();

                break;
            case SCROLLBAR_DRAGGING:
                vertexInfo.setBuffer(mShader.getPositionAttribIndex(), mDraggingBuffer);

                mScrollbarRegionLeft = mWidth * 0.5f - mScrollbarRegionWidth[index] - mSpacing * 2;

                mAnimator.cancel();
                mBlendingAlpha = 1.0f;
                mObject.show();

                break;
        }

        mSurfaceView.requestRender();
    }

    private void calcScrollbarHeight() {
        float scrollableHeight = mGridInfo.getScrollableHeight();
        if (scrollableHeight > mHeight) {
            mScrollbarHeight = (mScrollbarRegionHeight / scrollableHeight) * mScrollbarRegionHeight;
        } else {
            mScrollbarHeight = mScrollbarRegionHeight;
        }

        if (mScrollbarHeight < mScrollbarMinHeight) {
            mScrollbarHeight = mScrollbarMinHeight;
        }
    }

    private void calcScrollableDistance() {
        float bottom = mScrollbarRegionTop - mScrollbarHeight;
        mScrollableDistance = bottom + (mHeight * 0.5f - mSpacing);
    }

    void onSurfaceChanged(int width, int height) {
        mWidth = width;
        mHeight = height;

        float scrollableHeight = mGridInfo.getScrollableHeight();
        if (scrollableHeight < height) {
            mIsVisible = false;
        } else {
            mIsVisible = true;
        }

        int index = ScrollbarMode.NORMAL.getIndex();
        mScrollbarRegionWidth[index] = mContext.getResources().getDimensionPixelSize(R.dimen.gridview_scrollbar_width);
        mScrollbarRegionHeight = height - mSystemBarHeight - mSpacing * 2;

        mScrollbarRegionTop = height * 0.5f - mSystemBarHeight - mSpacing;
        mScrollbarRegionLeft = mWidth * 0.5f - mScrollbarRegionWidth[index] - mSpacing * 2;

        index = ScrollbarMode.SCROLLBAR_DRAGGING.getIndex();
        mScrollbarRegionWidth[index] = mScrollbarRegionWidth[ScrollbarMode.NORMAL.getIndex()] * 2.5f;

        calcScrollbarHeight();
        calcScrollableDistance();
    }

    @Override
    public void onColumnWidthChanged() {
        if (DEBUG) {
            Log.d(TAG, "onColumnWidthChanged()");
        }

        float scrollableHeight = mGridInfo.getScrollableHeight();
        if (scrollableHeight < mHeight) {
            mIsVisible = false;
        } else {
            mIsVisible = true;
        }

        mColumnWidth = mGridInfo.getColumnWidth();
        mNumOfColumns = mGridInfo.getNumOfColumns();

        if (mObject == null) {
            return;
        }

        int index = ScrollbarMode.NORMAL.getIndex();
        mScrollbarRegionLeft = mWidth * 0.5f - mScrollbarRegionWidth[index] - mSpacing * 2;;

        GLESVertexInfo vertexInfo = mObject.getVertexInfo();
        FloatBuffer position = (FloatBuffer) vertexInfo.getBuffer(mShader.getPositionAttribIndex());

        calcScrollbarHeight();

        float top = position.get(7);
        float bottom = top - mScrollbarHeight;

        position.put(1, bottom);
        position.put(4, bottom);

        mScrollableDistance = bottom + (mHeight * 0.5f - mSpacing);
    }

    @Override
    public void onImageDeleted() {
        calcScrollbarHeight();
    }

    @Override
    public void onDateLabelDeleted() {
        calcScrollbarHeight();
    }

    // initialize

    void setSurfaceView(GallerySurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    void setColor(float r, float g, float b, float a) {
        int index = ScrollbarMode.NORMAL.getIndex();
        mRed[index] = r;
        mGreen[index] = g;
        mBlue[index] = b;
        mAlpha[index] = a;
    }

    GLESObject createObject(GLESNode parent) {
        mObject = new GLESObject("scrollbar");
        parent.addChild(mObject);

        mObject.setListener(mScrollbarListener);
        mObject.setGLState(mGLState);


        return mObject;
    }

    void setupObject(GLESCamera camera) {
        mObject.setCamera(camera);
        mObject.setShader(mShader);

        int index = ScrollbarMode.SCROLLBAR_DRAGGING.getIndex();
        GLESVertexInfo vertexInfo = GalleryUtils.createScrollbarVertexInfo(mShader,
                0f, 0f,
                mScrollbarRegionWidth[index], mScrollbarHeight);
        mDraggingBuffer = (FloatBuffer) vertexInfo.getBuffer(mShader.getPositionAttribIndex());

        index = ScrollbarMode.NORMAL.getIndex();
        vertexInfo = GalleryUtils.createScrollbarVertexInfo(mShader,
                0f, 0f,
                mScrollbarRegionWidth[index], mScrollbarHeight);
        mNormalBuffer = (FloatBuffer) vertexInfo.getBuffer(mShader.getPositionAttribIndex());

        mObject.setVertexInfo(vertexInfo, false, false);

        mObject.hide();
    }

    void setShader(GLESShader shader) {
        mShader = shader;

        shader.useProgram();

        int location = shader.getUniformLocation("uAlpha");
        GLES20.glUniform1f(location, 1.0f);

        int index = ScrollbarMode.NORMAL.getIndex();
        location = shader.getUniformLocation("uColor");
        GLES20.glUniform4f(location, mRed[index], mGreen[index], mBlue[index], mAlpha[index]);
    }

    float getScrollbarHeight() {
        return mScrollbarHeight;
    }

    float getScrollbarPosY() {
        return mScrollDistance;
    }

    float getScrollbarWidth(ScrollbarMode mode) {
        int index = mode.getIndex();
        return mScrollbarRegionWidth[index];
    }

    private boolean mNeedToUpdateColor = false;

    synchronized void setScrollbarMode(ScrollbarMode mode) {
        int scrollableHeight = mGridInfo.getScrollableHeight();
        if (scrollableHeight < mHeight) {
            mode = ScrollbarMode.NORMAL;
        }

        if (mScrollbarMode != mode) {
            mNeedToUpdateColor = true;
            mSurfaceView.requestRender();
        }

        mScrollbarMode = mode;
    }

    synchronized ScrollbarMode getScrollbarMode() {
        return mScrollbarMode;
    }

    void show() {
        if (mObject != null && mIsVisible == true) {
            mObject.show();
        }
    }

    void hide() {
        if (mObject != null) {
            mObject.hide();
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

            float scrollableHeight = mGridInfo.getScrollableHeight();
            float translateY = mGridInfo.getTranslateY();

            if (scrollableHeight > mHeight) {
                float scrollDistance = (translateY / (scrollableHeight - mHeight)) * mScrollableDistance;
                transform.setTranslate(mScrollbarRegionLeft, -scrollDistance + mScrollbarRegionTop, 0f);
                mScrollDistance = -scrollDistance;
            } else {
                mScrollDistance = 0f;
            }
        }

        @Override
        public void apply(GLESObject object) {
            int location = mShader.getUniformLocation("uAlpha");
            GLES20.glUniform1f(location, mBlendingAlpha);
        }
    };

    class ScrollbarAnimatorCallback implements GLESAnimatorCallback {

        @Override
        public void onAnimation(GLESVector3 current) {
            mBlendingAlpha = current.getX();
        }

        @Override
        public void onCancel() {

        }

        @Override
        public void onFinished() {
            mObject.hide();
        }
    }
}
