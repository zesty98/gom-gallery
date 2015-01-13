package com.gomdev.gallery;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESContext;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESNodeListener;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRenderer;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVector3;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by gomdev on 14. 12. 16..
 */
public class ImageListRenderer implements GLSurfaceView.Renderer {
    static final String CLASS = "ImageListRenderer";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;

    private final Object mLockObject;

    private GLESRenderer mRenderer;
    private GLESSceneManager mSM;
    private GLESNode mRoot;
    private GLESNode mImageNode;

    private ObjectManager mObjectManager = null;
    private Scrollbar mScrollbar = null;

    private GallerySurfaceView mSurfaceView = null;

    private RendererListener mListener = null;

    private boolean mIsSurfaceChanged = false;

    public ImageListRenderer(Context context, Object lockObject) {
        mContext = context;
        mLockObject = lockObject;

        GLESContext.getInstance().setContext(context);

        mRenderer = GLESRenderer.createRenderer();

        mObjectManager = new ObjectManager(context);

        mScrollbar = new Scrollbar(context);
        mScrollbar.setColor(0.3f, 0.3f, 0.3f, 0.7f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mIsSurfaceChanged == false) {
            Log.d(TAG, "onDrawFrame() frame is skipped");
            return;
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        synchronized (mLockObject) {
            mRenderer.updateScene(mSM);

            float translateY = getTranslateY();
            mScrollbar.setTranslateY(translateY);

            mObjectManager.updateTexture();
            mObjectManager.checkVisibility(translateY);

            mRenderer.drawScene(mSM);
        }

        mScrollbar.hide();
    }

    private float getTranslateY() {
        GLESTransform transform = mImageNode.getTransform();
        GLESVector3 translate = transform.getPreTranslate();
        return translate.mY;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged()");
        }

        mIsSurfaceChanged = false;

        mRenderer.reset();

        mObjectManager.onSurfaceChanged(width, height);
        mScrollbar.onSurfaceChanged(width, height);

        GLESCamera camera = setupCamera(width, height);
        setupScene(camera);

        mIsSurfaceChanged = true;
    }

    private GLESTexture createDummyTexture(int color) {
        Bitmap bitmap = GLESUtils.makeBitmap(16, 16, Bitmap.Config.ARGB_8888, color);

        GLESTexture dummyTexture = new GLESTexture.Builder(GLES20.GL_TEXTURE_2D, 16, 16)
                .load(bitmap);

        return dummyTexture;
    }


    private GLESCamera setupCamera(int width, int height) {
        GLESCamera camera = new GLESCamera();

        float screenRatio = (float) width / height;

        float fovy = 30f;
        float eyeZ = (height / 2f) / (float) Math.tan(Math.toRadians(fovy * 0.5));

        camera.setLookAt(0f, 0f, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);

        camera.setFrustum(fovy, screenRatio, eyeZ * 0.01f, eyeZ * 10f);

        camera.setViewport(new GLESRect(0, 0, width, height));

        return camera;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        GLES20.glClearColor(1f, 1f, 1f, 1f);

        createShader();

        mObjectManager.onSurfaceCreated();

        GLESTexture dummyTexture = createDummyTexture(Color.LTGRAY);
        mObjectManager.setDummyImageTexture(dummyTexture);

        dummyTexture = createDummyTexture(Color.WHITE);
        mObjectManager.setDummyDateLabelTexture(dummyTexture);

        createScene();
    }

    private boolean createShader() {
        if (DEBUG) {
            Log.d(TAG, "createShader()");
        }

        boolean res = mObjectManager.createShader(R.raw.texture_20_vs, R.raw.texture_20_fs);
        if (res == false) {
            return false;
        }

        res = mScrollbar.createShader(R.raw.color_20_vs, R.raw.color_20_fs);
        if (res == false) {
            return false;
        }

        return true;
    }

    private void createScene() {
        mSM = GLESSceneManager.createSceneManager();
        mRoot = mSM.createRootNode("root");

        mImageNode = mSM.createNode("imageNode");
        mImageNode.setListener(mImageNodeListener);
        mRoot.addChild(mImageNode);

        mObjectManager.createObjects(mImageNode);
        mScrollbar.createObject(mRoot);
    }

    private void setupScene(GLESCamera camera) {
        mObjectManager.setupObjects(camera);
        mScrollbar.setupObject(camera);
    }

    public void setSurfaceView(GallerySurfaceView surfaceView) {
        mSurfaceView = surfaceView;

        mObjectManager.setSurfaceView(surfaceView);

        mSurfaceView.setGridInfoChangeListener(mScrollbar);
        mSurfaceView.setGridInfoChangeListener(mObjectManager);
    }

    public void setRendererListener(RendererListener listener) {
        mListener = listener;
    }

    public void setGridInfo(GridInfo gridInfo) {
        mObjectManager.setGridInfo(gridInfo);
        mScrollbar.setGridInfo(gridInfo);
    }

    public int getSelectedIndex(float x, float y) {
        float translateY = getTranslateY();
        return mObjectManager.getSelectedIndex(x, y, translateY);
    }

    public int getNearestIndex(float x, float y) {
        float translateY = getTranslateY();
        return mObjectManager.getNearestIndex(x, y, translateY);
    }

    private GLESNodeListener mImageNodeListener = new GLESNodeListener() {
        @Override
        public void update(GLESNode node) {
            GLESTransform transform = node.getTransform();
            float[] matrix = transform.getMatrix();
            float prevY = matrix[13];
            if (mListener != null) {
                mListener.update(node);
            }

            transform = node.getTransform();
            matrix = transform.getMatrix();
            float currentY = matrix[13];

            if (prevY != currentY) {
                mScrollbar.show();
            }
        }
    };
}
