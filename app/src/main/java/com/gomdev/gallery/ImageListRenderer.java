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
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESNodeListener;
import com.gomdev.gles.GLESRect;
import com.gomdev.gles.GLESRenderer;
import com.gomdev.gles.GLESSceneManager;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESTexture2D;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by gomdev on 14. 12. 16..
 */
public class ImageListRenderer implements GLSurfaceView.Renderer, ImageLoadingListener {
    static final String CLASS = "ImageListRenderer";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;
    private final ImageManager mImageManager;

    private GLESRenderer mRenderer;
    private GLESSceneManager mSM;
    private GLESNode mRoot;
    private GLESGLState mGLState;
    private GalleryObject[] mObjects;
    private GLESShader mShader;
    private GallerySurfaceView mSurfaceView = null;
    private GLESTexture mDummyTexture = null;

    private RendererListener mListener = null;

    private float mScreenRatio = 1f;
    private int mWidth = 0;
    private int mHeight = 0;

    private GridInfo mGridInfo = null;

    private int mSpacing;
    private int mNumOfColumns;
    private int mNumOfImages;
    private int mColumnWidth;
    private int mNumOfRows;
    private int mNumOfRowsInScreen;
    private BucketInfo mBucketInfo;
    private int mActionBarHeight;

    private ArrayList<TextureMappingInfo> mTextureMappingInfos = new ArrayList<>();
    private Queue<GalleryTexture> mWaitingTextures = new LinkedList<>();

    private boolean mIsSurfaceChanged = false;

    public ImageListRenderer(Context context) {
        mContext = context;
        GLESContext.getInstance().setContext(context);

        mImageManager = ImageManager.getInstance();

        setupSceneComponent();
    }

    private void setupSceneComponent() {
        mRenderer = GLESRenderer.createRenderer();

        mSM = GLESSceneManager.createSceneManager();
        mRoot = mSM.createRootNode("root");
        mRoot.setListener(mNodeListener);

        mGLState = new GLESGLState();
        mGLState.setCullFaceState(true);
        mGLState.setCullFace(GLES20.GL_BACK);
        mGLState.setDepthState(true);
        mGLState.setDepthFunc(GLES20.GL_LEQUAL);
    }


    @Override
    public synchronized void onDrawFrame(GL10 gl) {
        if (mIsSurfaceChanged == false) {
            Log.d(TAG, "onDrawFrame() frame is skipped");
            return;
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        update();

        mRenderer.updateScene(mSM);
        updateTexture();
        checkVisibility();
        mRenderer.drawScene(mSM);
    }

    private void update() {
        float xOffset = -(mWidth * 0.5f) + mColumnWidth * 0.5f + mSpacing;
        float yOffset = (mHeight * 0.5f) - mColumnWidth * 0.5f - mSpacing - mActionBarHeight;
        for (int i = 0; i < mNumOfImages; i++) {
            GLESTransform transform = mObjects[i].getTransform();
            transform.setIdentity();

            int rowIndex = i / mNumOfColumns;
            int columnIndex = i % mNumOfColumns;
            float x = xOffset + (mColumnWidth + mSpacing) * columnIndex;
            float y = yOffset - (mColumnWidth + mSpacing) * rowIndex;

            transform.translate(x, y, 0f);
        }
    }

    private void updateTexture() {
        GalleryTexture texture = mWaitingTextures.poll();
        if (texture != null) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(texture.getPosition());
            final GalleryObject object = textureMappingInfo.getObject();

            final Bitmap bitmap = texture.getBitmapDrawable().getBitmap();
            texture.load(bitmap);
            object.setTexture(texture);
            mSurfaceView.requestRender();
        }
    }

    private void checkVisibility() {
        GLESTransform transform = mRoot.getWorldTransform();

        float[] matrix = transform.getMatrix();
        float y = matrix[13];

        int visibleFirstRow = (int) Math.floor((double) (y - mActionBarHeight) / (mColumnWidth + mSpacing));
        if (visibleFirstRow < 0) {
            visibleFirstRow = 0;
        }

        int visibleLastRow = visibleFirstRow + mNumOfRowsInScreen;
        if (visibleLastRow > mNumOfRows) {
            visibleLastRow = mNumOfRows;
        }

        int visibleFirstPosition = visibleFirstRow * mNumOfColumns;
        int visibleLastPosition = visibleLastRow * mNumOfColumns + (mNumOfColumns - 1);
        int lastIndex = mNumOfImages - 1;

        if (visibleLastPosition > lastIndex) {
            visibleLastPosition = lastIndex;
        }

        for (int i = 0; i <= lastIndex; i++) {
            GalleryObject object = mObjects[i];
            if (i >= visibleFirstPosition && i <= visibleLastPosition) {
                object.show();
                mapTexture(i);
            } else {
                object.hide();
                unmapTexture(i, object);
            }
        }
    }

    private void unmapTexture(int position, GalleryObject object) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(position);

        object.setTexture(mDummyTexture);
        mImageManager.cancelWork(textureMappingInfo.getTexture());
        textureMappingInfo.set(null);
    }

    private void mapTexture(int position) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(position);

        ImageInfo imageInfo = textureMappingInfo.getImageInfo();
        GalleryTexture texture = textureMappingInfo.getTexture();
        if (texture == null) {
            texture = new GalleryTexture(imageInfo.getWidth(), imageInfo.getHeight());
            texture.setPosition(position);
            texture.setImageLoadingListener(this);
        }

        if ((texture != null && texture.isTextureLoadingNeeded() == true)) {
            mImageManager.loadThumbnail(imageInfo, texture);
            textureMappingInfo.set(texture);
            mSurfaceView.requestRender();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged()");
        }

        mIsSurfaceChanged = false;

        mRenderer.reset();
        cancelLoading();
        mWaitingTextures.clear();
        mTextureMappingInfos.clear();

        mWidth = width;
        mHeight = height;

        mGridInfo.setScreenSize(width, height);
        mNumOfRowsInScreen = mGridInfo.getNumOfRowsInScreen();
        mNumOfImages = mGridInfo.getNumOfImages();

        mDummyTexture = createDummyTexture();

        createObjects();

        mScreenRatio = (float) width / height;

        GLESCamera camera = setupCamera(width, height);

        for (int i = 0; i < mNumOfImages; i++) {
            ImageInfo imageInfo = mBucketInfo.get(i);

            mObjects[i].setCamera(camera);

            GLESVertexInfo vertexInfo = createPlaneVertexInfo(mColumnWidth, mColumnWidth);
            mObjects[i].setVertexInfo(vertexInfo, false, false);

            mObjects[i].setTexture(mDummyTexture);

            TextureMappingInfo textureMappingInfo = new TextureMappingInfo(mObjects[i], imageInfo);
            mTextureMappingInfos.add(textureMappingInfo);
        }
        mIsSurfaceChanged = true;
    }

    private GLESTexture createDummyTexture() {
        Bitmap bitmap = GLESUtils.makeBitmap(512, 512, Bitmap.Config.ARGB_8888, Color.BLACK);
        GLESTexture dummyTexture = new GLESTexture2D(512, 512, bitmap);
        dummyTexture.load();

        return dummyTexture;
    }

    private GLESVertexInfo createPlaneVertexInfo(float width, float height) {
        float right = width * 0.5f;
        float left = -right;
        float top = height * 0.5f;
        float bottom = -top;
        float z = 0.0f;

        float[] vertex = {
                left, bottom, z,
                right, bottom, z,
                left, top, z,
                right, top, z
        };

        GLESVertexInfo vertexInfo = new GLESVertexInfo();

        vertexInfo.setBuffer(mShader.getPositionAttribIndex(), vertex, 3);

        float[] texCoord = {
                0f, 1f,
                1f, 1f,
                0f, 0f,
                1f, 0f
        };

        vertexInfo.setBuffer(mShader.getTexCoordAttribIndex(), texCoord, 2);

        vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
        vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);

        return vertexInfo;
    }

    private GLESCamera setupCamera(int width, int height) {
        GLESCamera camera = new GLESCamera();

        float fovy = 30f;
        float eyeZ = (height / 2f) / (float) Math.tan(Math.toRadians(fovy * 0.5));

        camera.setLookAt(0f, 0f, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);

        camera.setFrustum(fovy, mScreenRatio, eyeZ * 0.1f, eyeZ * 2f);

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
    }

    private boolean createShader() {
        if (DEBUG) {
            Log.d(TAG, "createShader()");
        }

        mShader = new GLESShader(mContext);

        String vsSource = GLESUtils.getStringFromReosurce(mContext, R.raw.texture_20_vs);
        String fsSource = GLESUtils.getStringFromReosurce(mContext, R.raw.texture_20_fs);

        mShader.setShaderSource(vsSource, fsSource);
        if (mShader.load() == false) {
            return false;
        }

        String attribName = GLESShaderConstant.ATTRIB_POSITION;
        mShader.setPositionAttribIndex(attribName);

        attribName = GLESShaderConstant.ATTRIB_TEXCOORD;
        mShader.setTexCoordAttribIndex(attribName);

        return true;
    }

    private void createObjects() {
        mObjects = new GalleryObject[mNumOfImages];

        for (int i = 0; i < mNumOfImages; i++) {
            mObjects[i] = new GalleryObject("image" + i);
            mRoot.addChild(mObjects[i]);
            mObjects[i].setGLState(mGLState);
            mObjects[i].setShader(mShader);
        }
    }


    public void setSurfaceView(GallerySurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    public void setRendererListener(RendererListener listener) {
        mListener = listener;
    }

    private GLESNodeListener mNodeListener = new GLESNodeListener() {
        @Override
        public void update(GLESNode node) {
            if (mListener != null) {
                mListener.update(node);
            }
        }
    };

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mSpacing = gridInfo.getSpacing();
        mNumOfColumns = gridInfo.getNumOfColumns();
        mNumOfRows = gridInfo.getNumOfRows();
        mColumnWidth = gridInfo.getColumnWidth();
        mActionBarHeight = gridInfo.getActionBarHeight();
        mBucketInfo = gridInfo.getBucketInfo();
    }


    @Override
    public void onImageLoaded(final int position, final GalleryTexture texture) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(position);
        final GalleryObject object = textureMappingInfo.getObject();
        final ImageInfo imageInfo = textureMappingInfo.getImageInfo();

        final Bitmap bitmap = texture.getBitmapDrawable().getBitmap();
        imageInfo.setThumbnailWidth(bitmap.getWidth());
        imageInfo.setThumbnailHeight(bitmap.getHeight());

        calcTexCoord(imageInfo);

        GLESVertexInfo vertexInfo = object.getVertexInfo();
        GLESShader shader = object.getShader();
        vertexInfo.setBuffer(shader.getTexCoordAttribIndex(), imageInfo.getTexCoord(), 2);

        mWaitingTextures.add(texture);
        mSurfaceView.requestRender();
    }

    private void calcTexCoord(ImageInfo imageInfo) {
        int width = imageInfo.getThumbnailWidth();
        int height = imageInfo.getThumbnailHeight();

        float minS = 0f;
        float minT = 0f;
        float maxS = 0f;
        float maxT = 0f;
        if (width > height) {
            minS = (float) ((width - height) / 2f) / width;
            maxS = 1f - minS;

            minT = 0f;
            maxT = 1f;
        } else {
            minT = (float) ((height - width) / 2f) / height;
            maxT = 1f - minT;

            minS = 0f;
            maxS = 1f;
        }

        float[] texCoord = new float[]{
                minS, maxT,
                maxS, maxT,
                minS, minT,
                maxS, minT
        };
        imageInfo.setTexCoord(texCoord);
    }

    public synchronized void resize() {
        int columnWidth = mGridInfo.getColumnWidth();
        if (mColumnWidth == columnWidth) {
            return;
        }

        mColumnWidth = columnWidth;
        mNumOfColumns = mGridInfo.getNumOfColumns();
        mNumOfRows = mGridInfo.getNumOfRows();
        mNumOfRowsInScreen = mGridInfo.getNumOfRowsInScreen();

        float halfScaledWidth = mColumnWidth * 0.5f;
        for (int i = 0; i < mNumOfImages; i++) {
            GLESVertexInfo vertexInfo = mObjects[i].getVertexInfo();
            FloatBuffer buffer = (FloatBuffer) vertexInfo.getBuffer(mShader.getPositionAttribIndex());

            buffer.put(0, -halfScaledWidth);
            buffer.put(1, -halfScaledWidth);
            buffer.put(2, 0f);

            buffer.put(3, halfScaledWidth);
            buffer.put(4, -halfScaledWidth);
            buffer.put(5, 0f);

            buffer.put(6, -halfScaledWidth);
            buffer.put(7, halfScaledWidth);
            buffer.put(8, 0f);

            buffer.put(9, halfScaledWidth);
            buffer.put(10, halfScaledWidth);
            buffer.put(11, 0f);
        }

        mSurfaceView.requestRender();
    }


    public void onSurfaceDestroyed() {
        cancelLoading();
    }

    private void cancelLoading() {
        int size = mTextureMappingInfos.size();

        for (int i = 0; i < size; i++) {
            GalleryTexture texture = mTextureMappingInfos.get(i).getTexture();
            if (texture != null) {
                mImageManager.cancelWork(mTextureMappingInfos.get(i).getTexture());
            }
        }
    }
}
