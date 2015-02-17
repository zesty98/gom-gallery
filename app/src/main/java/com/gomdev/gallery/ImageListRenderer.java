package com.gomdev.gallery;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRenderer;
import com.gomdev.gles.GLESSceneManager;

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

    private GallerySurfaceView mSurfaceView = null;
    private GalleryContext mGalleryContext = null;

    private GLESRenderer mGLESRenderer = null;
    private GLESSceneManager mSM = null;
    private GLESNode mRoot = null;

    private AlbumViewManager mAlbumViewManager = null;
    private DetailViewManager mDetailViewManager = null;

    private boolean mIsSurfaceChanged = false;

    ImageListRenderer(Context context, GridInfo gridInfo) {
        if (DEBUG) {
            Log.d(TAG, "ImageListRenderer()");
        }

        mContext = context;
        mGridInfo = gridInfo;

        GLESContext.getInstance().setContext(context);
        ImageManager imageManager = ImageManager.getInstance();
        mGalleryContext = GalleryContext.getInstance();

        mGLESRenderer = GLESRenderer.createRenderer();
        mSM = GLESSceneManager.createSceneManager();
        mRoot = mSM.createRootNode("root");

        GLESNode albumViewNode = new GLESNode("albumViewNode");
        mRoot.addChild(albumViewNode);

        mAlbumViewManager = new AlbumViewManager(context, gridInfo);
        mAlbumViewManager.createScene(albumViewNode);

        GLESNode detailViewNode = new GLESNode("detailViewNode");
        mRoot.addChild(detailViewNode);

        mDetailViewManager = new DetailViewManager(context, gridInfo);
        mDetailViewManager.createScene(detailViewNode);

        imageManager.setObjectManager(mAlbumViewManager);

        setGridInfo(gridInfo);
    }

    private void setGridInfo(GridInfo gridInfo) {
        if (DEBUG) {
            Log.d(TAG, "setGridInfo()");
        }

        mGridInfo.addListener(this);
    }

    // rendering

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mIsSurfaceChanged == false) {
            Log.d(TAG, "onDrawFrame() frame is skipped");
            return;
        }

//        GLESUtils.checkFPS();

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        synchronized (GalleryContext.sLockObject) {
            mAlbumViewManager.update();

            mGLESRenderer.updateScene(mSM);
            mGLESRenderer.drawScene(mSM);

            mAlbumViewManager.updateAnimation();
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

        camera.setFrustum(fovy, screenRatio, eyeZ * 0.01f, eyeZ * 10f);

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

        mAlbumViewManager.onSurfaceCreated();
    }

    // touch

    boolean onTouchEvent(MotionEvent event) {
        boolean retVal = mAlbumViewManager.onTouchEvent(event);
        return retVal;
    }

    // Listener

    void onImageSelected(ImageIndexingInfo imageIndexingInfo) {
//        ImageObject selectedObject = mAlbumViewManager.getImageObject(imageIndexingInfo);
//        mDetailViewManager.onImageSelected(selectedObject);
    }

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

    // resume

    void onResume() {
        if (DEBUG) {
            Log.d(TAG, "onResume()");
        }

        mAlbumViewManager.onResume();
    }

    // pause

    void onPause() {
        if (DEBUG) {
            Log.d(TAG, "onPause()");
        }

        mAlbumViewManager.onPause();
    }

    // set / get
    void setSurfaceView(GallerySurfaceView surfaceView) {
        if (DEBUG) {
            Log.d(TAG, "setSurfaceView()");
        }

        mSurfaceView = surfaceView;
        mAlbumViewManager.setSurfaceView(surfaceView);

    }
}
