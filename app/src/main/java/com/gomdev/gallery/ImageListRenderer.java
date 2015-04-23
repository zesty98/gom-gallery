package com.gomdev.gallery;

import android.annotation.TargetApi;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;

import com.gomdev.gallery.GalleryConfig.ImageViewMode;
import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRenderer;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by gomdev on 14. 12. 16..
 */
class ImageListRenderer implements GLSurfaceView.Renderer, GridInfoChangeListener {
    static final String CLASS = "ImageListRenderer";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;
    private final GridInfo mGridInfo;

    private Handler mHandler = null;

    private GallerySurfaceView mSurfaceView = null;
    private GalleryContext mGalleryContext = null;

    private GLESRenderer mGLESRenderer = null;
    private GLESSceneManager mSM = null;
    private GLESNode mRoot = null;

    private ViewManager mViewManager = null;
    private AlbumViewManager mAlbumViewManager = null;
    private DetailViewManager mDetailViewManager = null;

    private GLESShader mTextureCustomShader = null;
    private GLESShader mTextureAlphaShader = null;
    private GLESShader mColorShader = null;

    private boolean mIsSurfaceChanged = false;
    private boolean mNeedToClearDetailView = false;

    private long mCurrentTime = 0L;

    ImageListRenderer(Context context, GridInfo gridInfo) {
        if (DEBUG) {
            Log.d(TAG, "ImageListRenderer()");
        }

        mContext = context;
        mGridInfo = gridInfo;

        init();
    }

    private void init() {
        GLESContext.getInstance().setContext(mContext);
        ImageManager imageManager = ImageManager.getInstance();
        mGalleryContext = GalleryContext.getInstance();

        mGLESRenderer = GLESRenderer.createRenderer();
        mSM = GLESSceneManager.createSceneManager();
        mRoot = mSM.createRootNode("root");

        GLESNode albumViewNode = new GLESNode("albumViewNode");
        mRoot.addChild(albumViewNode);

        mAlbumViewManager = new AlbumViewManager(mContext, mGridInfo);
        mAlbumViewManager.createScene(albumViewNode);

        GLESNode detailViewNode = new GLESNode("detailViewNode");
        mRoot.addChild(detailViewNode);

        mDetailViewManager = new DetailViewManager(mContext, mGridInfo);
        mDetailViewManager.createScene(detailViewNode);

        imageManager.setObjectManager(mAlbumViewManager);

        mViewManager = mAlbumViewManager;

        setGridInfo(mGridInfo);
    }

    private void setGridInfo(GridInfo gridInfo) {
        if (DEBUG) {
            Log.d(TAG, "setGridInfo()");
        }

        gridInfo.addListener(this);
    }

    // rendering

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mIsSurfaceChanged == false) {
            Log.d(TAG, "onDrawFrame() frame is skipped");
            return;
        }

        mCurrentTime = System.currentTimeMillis();

//        GLESUtils.checkFPS();

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        synchronized (GalleryContext.sLockObject) {
            mViewManager.updateAnimation(mCurrentTime);
            mViewManager.update(mCurrentTime);
        }

        mGLESRenderer.updateScene(mSM);
        mGLESRenderer.drawScene(mSM);

        if (mNeedToClearDetailView == true) {
            mNeedToClearDetailView = false;
            mDetailViewManager.destroyTextures();
        }
    }


    // onSurfaceChanged

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged() width=" + width + " height=" + height);
        }

        mGLESRenderer.reset();

        mIsSurfaceChanged = false;

        mGridInfo.setScreenSize(width, height);

        mAlbumViewManager.onSurfaceChanged(width, height);

        GLESCamera camera = setupCamera(width, height);
        mAlbumViewManager.setupObjects(camera);

        mDetailViewManager.onSurfaceChanged(width, height);

        camera = setupCamera(width, height);
        mDetailViewManager.setupObjects(camera);

        mIsSurfaceChanged = true;
    }

    private GLESCamera setupCamera(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "setupCamera()");
        }

        GLESCamera camera = new GLESCamera();

        float screenRatio = (float) width / height;

        float fovy = 30f;
        float eyeZ = (height / 2f) / (float) Math.tan(Math.toRadians(fovy * 0.5));

        camera.setLookAt(0f, 0f, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);

        camera.setFrustum(fovy, screenRatio, eyeZ * 0.001f, eyeZ * 1.5f);

        camera.setViewport(new GLESRect(0, 0, width, height));

        return camera;
    }

    // onSurfaceCreated

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        GLES20.glClearColor(1f, 1f, 1f, 1f);

        createShader();

        mAlbumViewManager.onSurfaceCreated();
        mDetailViewManager.onSurfaceCreated();
    }

    private boolean createShader() {
        mTextureCustomShader = createTextureCustomShader(R.raw.texture_custom_20_vs, R.raw.texture_custom_20_fs);
        if (mTextureCustomShader == null) {
            return false;
        }

        mAlbumViewManager.setTextureCustomShader(mTextureCustomShader);

        mTextureAlphaShader = createTextureShader(R.raw.texture_20_vs, R.raw.texture_alpha_20_fs);
        if (mTextureAlphaShader == null) {
            return false;
        }

        mAlbumViewManager.setTextureAlphaShader(mTextureAlphaShader);
        mDetailViewManager.setTextureAlphaShader(mTextureAlphaShader);

        mColorShader = createColorShader(R.raw.color_20_vs, R.raw.color_alpha_20_fs);
        if (mColorShader == null) {
            return false;
        }

        mAlbumViewManager.setColorShader(mColorShader);
        mDetailViewManager.setColorShader(mColorShader);

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

    private GLESShader createTextureCustomShader(int vsResID, int fsResID) {
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

    // touch

    boolean onTouchEvent(MotionEvent event) {
        boolean retVal = mViewManager.onTouchEvent(event);
        return retVal;
    }

    // Listener

    void onImageSelected(ImageIndexingInfo imageIndexingInfo) {
        ImageObject selectedObject = mAlbumViewManager.getImageObject(imageIndexingInfo);

        GLESVertexInfo vertexInfo = selectedObject.getVertexInfo();
        vertexInfo.getBuffer(mTextureCustomShader.getPositionAttribIndex());

        mDetailViewManager.onImageSelected(selectedObject);

        mViewManager = mDetailViewManager;
        mGalleryContext.setImageViewMode(GalleryConfig.ImageViewMode.DETAIL_VIEW_MODE);

        mHandler.sendEmptyMessage(ImageListActivity.SET_SYSTEM_UI_FLAG_LOW_PROFILE);
    }

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
    }

    @Override
    public void onDateLabelDeleted() {
        if (DEBUG) {
            Log.d(TAG, "onDateLabelDeleted()");
        }
    }

    // resume

    void onResume() {
        if (DEBUG) {
            Log.d(TAG, "onResume()");
        }

        mAlbumViewManager.onResume();
        mDetailViewManager.onResume();
    }

    // pause

    void onPause() {
        if (DEBUG) {
            Log.d(TAG, "onPause()");
        }

        mAlbumViewManager.onPause();
        mDetailViewManager.onPause();
    }

    // set / get
    void setSurfaceView(GallerySurfaceView surfaceView) {
        if (DEBUG) {
            Log.d(TAG, "setSurfaceView()");
        }

        mSurfaceView = surfaceView;
        mAlbumViewManager.setSurfaceView(surfaceView);
        mDetailViewManager.setSurfaceView(surfaceView);
    }

    ImageObject getObjectFromAlbumView(ImageIndexingInfo imageIndexingInfo) {
        return mAlbumViewManager.getImageObject(imageIndexingInfo);
    }

    void adjustAlbumView(float translateY) {
        mAlbumViewManager.adjustViewport(translateY);
    }

    void finish() {
        if (DEBUG) {
            Log.d(TAG, "finish()");
        }

        ImageViewMode mode = mGalleryContext.getImageViewMode();
        if (mode == GalleryConfig.ImageViewMode.ALBUME_VIEW_MODE) {
            boolean isAlbumViewFinished = mAlbumViewManager.finish();
            if (isAlbumViewFinished == true) {
                ((ImageListActivity) mContext).onFinished();
            }
            return;
        }

        mAlbumViewManager.show();
        mDetailViewManager.finish();
        mSurfaceView.requestRender();
    }

    void onFinished(boolean isDetailViewFinished) {
        if (DEBUG) {
            Log.d(TAG, "onFinished()");
        }

        if (isDetailViewFinished == true) {
            mNeedToClearDetailView = true;

            mDetailViewManager.hide();

            mViewManager = mAlbumViewManager;
            mGalleryContext.setImageViewMode(GalleryConfig.ImageViewMode.ALBUME_VIEW_MODE);
            mSurfaceView.requestRender();

            mHandler.sendEmptyMessage(ImageListActivity.SET_SYSTEM_UI_FLAG_VISIBLE);
        } else {
            mAlbumViewManager.hide();
        }

    }

    void setHandler(Handler handler) {
        mHandler = handler;

        mAlbumViewManager.setHandler(handler);
        mDetailViewManager.setHandler(handler);
    }
}