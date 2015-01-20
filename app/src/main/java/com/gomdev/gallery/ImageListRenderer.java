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
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by gomdev on 14. 12. 16..
 */
public class ImageListRenderer implements GLSurfaceView.Renderer {
    static final String CLASS = "ImageListRenderer";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private ObjectManager mObjectManager = null;

    private boolean mIsSurfaceChanged = false;

    public ImageListRenderer(Context context) {
        GLESContext.getInstance().setContext(context);

        mObjectManager = new ObjectManager(context);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mIsSurfaceChanged == false) {
            Log.d(TAG, "onDrawFrame() frame is skipped");
            return;
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        synchronized (GalleryContext.sLockObject) {
            mObjectManager.update();
            mObjectManager.drawFrame();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged()");
        }

        mIsSurfaceChanged = false;

        mObjectManager.onSurfaceChanged(width, height);

        GLESCamera camera = setupCamera(width, height);
        mObjectManager.setupObjects(camera);

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

        mObjectManager.onSurfaceCreated();

        GLESTexture dummyTexture = createDummyTexture(Color.LTGRAY);
        mObjectManager.setDummyImageTexture(dummyTexture);

        dummyTexture = createDummyTexture(Color.WHITE);
        mObjectManager.setDummyDateLabelTexture(dummyTexture);

        mObjectManager.createScene();
    }

    public void setSurfaceView(GallerySurfaceView surfaceView) {
        mObjectManager.setSurfaceView(surfaceView);
    }

    public void setRendererListener(RendererListener listener) {
        mObjectManager.setRendererListener(listener);
    }

    public void setGridInfo(GridInfo gridInfo) {
        mObjectManager.setGridInfo(gridInfo);
    }

    public int getSelectedIndex(float x, float y) {
        return mObjectManager.getSelectedIndex(x, y);
    }

    public int getNearestIndex(float x, float y) {
        return mObjectManager.getNearestIndex(x, y);
    }

    public GalleryObject getImageObject(int index) {
        return mObjectManager.getImageObject(index);
    }
}
