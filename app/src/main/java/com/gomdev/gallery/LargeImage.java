package com.gomdev.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.util.Log;
import android.view.MotionEvent;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESNodeListener;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESVertexInfo;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by gomdev on 15. 4. 30..
 */
public class LargeImage {
    private static final String CLASS = "LargeImage";
    private static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = GalleryConfig.DEBUG;

    private static final int GRID_SIZE = 256;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    class BlockInfo {
        final int mIndex;
        final Bitmap mBitmap;

        BlockInfo(int index, Bitmap bitmap) {
            mIndex = index;
            mBitmap = bitmap;
        }
    }

    private final Context mContext;

    private ImageInfo mImageInfo;

    private LargeImageObject[] mObjects = null;
    private LinkedList<LargeImageObject> mObjectPool = new LinkedList<>();

    private GLESShader mShader = null;
    private GLESCamera mCamera = null;
    private GLESNode mParentNode = null;

    // tiled rendering
    private BitmapRegionDecoder mDecoder = null;

    private ThreadPoolExecutor mExecutor = null;
    private CompletionService<BlockInfo> mCompletionService = null;

    private int mWidth = 0;
    private int mHeight = 0;

    private int mAdjustedWidth = 0;
    private int mAdjustedHeight = 0;

    private float mBitmapRatio = 0f;
    private int mBitmapWidth = 0;
    private int mBitmapHeight = 0;

    private int mNumOfObjects = 0;
    private int mNumOfObjectInWidth = 0;
    private int mNumOfObjectInHeight = 0;

    private int mOrientation = 0;

    // touch & transform
    private boolean mIsDown = false;

    private float mPrevX = 0f;
    private float mPrevY = 0f;

    private float mCurrentTranslateX = 0f;
    private float mCurrentTranslateY = 0f;

    LargeImage(Context context) {
        mContext = context;

        mExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(CPU_COUNT);
    }

    void clearObjects() {
        for (int i = 0; i < mNumOfObjects; i++) {
            mObjectPool.add(mObjects[i]);
            GLESTexture texture = mObjects[i].getTexture();
            if (texture != null) {
                texture.destroy();
            }
            mParentNode.removeChild(mObjects[i]);
        }
    }

    // rendering

    void updateTexture() {
        int numOfUpdatedTexture = 0;

        mCompletionService = new ExecutorCompletionService<>(mExecutor);

        float translateX = 0f;
        float translateY = 0f;

        if (mOrientation == 90) {
            translateX = -mCurrentTranslateY;
            translateY = mCurrentTranslateX;
        } else if (mOrientation == 270) {
            translateX = mCurrentTranslateY;
            translateY = -mCurrentTranslateX;
        } else if (mOrientation == 180) {
            translateX = -mCurrentTranslateX;
            translateY = -mCurrentTranslateY;
        } else {
            translateX = mCurrentTranslateX;
            translateY = mCurrentTranslateY;
        }

        for (int i = 0; i < mNumOfObjects; i++) {
            LargeImageObject object = mObjects[i];

            int width = object.getWidth();
            int height = object.getHeight();

            float left = object.getX() + translateX;
            float right = left + width;
            float top = object.getY() + translateY;
            float bottom = top - height;

            boolean isInScreen = isInScreen(left, right, bottom, top);

            if (isInScreen == true && object.isTextureMapped() == false) {
                int imageX = object.getImageX();
                int imageY = object.getmImageY();
                numOfUpdatedTexture++;

                final Rect rect = new Rect(imageX, imageY, imageX + width, imageY + height);
                final int index = i;

                mCompletionService.submit(new Callable<BlockInfo>() {
                    @Override
                    public BlockInfo call() throws Exception {
                        return decodeRegion(index, rect);
                    }
                });
            } else if (isInScreen == false && object.isTextureMapped() == true) {
                GLESTexture texture = object.getTexture();
                if (texture != null) {
                    texture.destroy();
                }

                object.setTexture(null);
                object.setTextureMapped(false);
            }
        }

        for (int i = 0; i < numOfUpdatedTexture; i++) {
            Future<BlockInfo> f = null;
            try {
                f = mCompletionService.take();
                BlockInfo blockInfo = f.get();

                Bitmap bitmap = blockInfo.mBitmap;
                int index = blockInfo.mIndex;

                GLESTexture.Builder builder = new GLESTexture.Builder(
                        GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight())
                        .setWrapMode(GLES20.GL_CLAMP_TO_EDGE)
                        .setFilter(GLES20.GL_NEAREST, GLES20.GL_NEAREST);
                GLESTexture texture = builder.load(bitmap);
                bitmap.recycle();
                bitmap = null;

                mObjects[index].setTexture(texture);
                mObjects[index].setTextureMapped(true);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
//                throw launderThrowable(e.getCause());
                e.printStackTrace();
            }
        }
    }

    private BlockInfo decodeRegion(int index, Rect rect) {
        Bitmap bitmap = mDecoder.decodeRegion(rect, null);
        BlockInfo blockInfo = new BlockInfo(index, bitmap);

        return blockInfo;
    }


    // onSurfaceChanged
    void onSurfaceChanged(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    // onSurfaceCreated
    void onSurfaceCreated() {

    }

    // initialize

    void setCamera(GLESCamera camera) {
        mCamera = camera;
    }

    void setShader(GLESShader shader) {
        mShader = shader;
    }

    void setParentNode(GLESNode node) {
        mParentNode = node;

        node.setListener(mNodeListener);
    }

    // listener / callback

    boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        float x = event.getX();
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsDown = true;

                touchDown(x, y);
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDown == true) {
                    touchUp(x, y);
                }

                mIsDown = false;

                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsDown == true) {
                    touchMove(x, y);
                }
                break;
        }

        return true;
    }

    public void touchDown(float x, float y) {
        mPrevX = x;
        mPrevY = y;
    }

    public void touchUp(float x, float y) {

    }

    public void touchMove(float x, float y) {
        float translateX = 0f;
        float translateY = 0f;

        float maxTranslateX = 0f;
        float maxTranslateY = 0f;

        if (mBitmapWidth > mAdjustedWidth) {
            if (mOrientation == 90 || mOrientation == 270) {
                maxTranslateY = (mBitmapWidth - mAdjustedWidth) * 0.5f;
            } else {
                maxTranslateX = (mBitmapWidth - mAdjustedWidth) * 0.5f;
            }
        }

        if (mBitmapHeight > mAdjustedHeight) {
            if (mOrientation == 90 || mOrientation == 270) {
                maxTranslateX = (mBitmapHeight - mAdjustedHeight) * 0.5f;
            } else {
                maxTranslateY = (mBitmapHeight - mAdjustedHeight) * 0.5f;
            }
        }

        translateX = mCurrentTranslateX;
        translateX += (x - mPrevX);

        if (maxTranslateX < Math.abs(translateX)) {
            if (translateX < 0) {
                translateX = -maxTranslateX;
            } else {
                translateX = maxTranslateX;
            }
        }

        translateY = mCurrentTranslateY;
        translateY += (mPrevY - y);

        if (maxTranslateY < Math.abs(translateY)) {
            if (translateY < 0) {
                translateY = -maxTranslateY;
            } else {
                translateY = maxTranslateY;
            }
        }

        mCurrentTranslateX = translateX;
        mCurrentTranslateY = translateY;

        mPrevX = x;
        mPrevY = y;
    }

    public void touchCancel(float x, float y) {
    }

    // set / get
    float getTranslateX() {
        return mCurrentTranslateX;
    }

    float getTranslateY() {
        return mCurrentTranslateY;
    }

    // should be called by GLThread
    // this function should be called before onSurfaceChanged
    void load(ImageInfo imageInfo) {
        reset();

        String path = imageInfo.getImagePath();
        mOrientation = imageInfo.getOrientation();

        if (mOrientation == 90 || mOrientation == 270) {
            mAdjustedWidth = mHeight;
            mAdjustedHeight = mWidth;
        } else {
            mAdjustedWidth = mWidth;
            mAdjustedHeight = mHeight;
        }

        try {
            mDecoder = BitmapRegionDecoder.newInstance(path, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mBitmapWidth = mDecoder.getWidth();
        mBitmapHeight = mDecoder.getHeight();

        mNumOfObjectInWidth = (int) Math.ceil((float) mBitmapWidth / GRID_SIZE);
        mNumOfObjectInHeight = (int) Math.ceil((float) mBitmapHeight / GRID_SIZE);
        mNumOfObjects = mNumOfObjectInWidth * mNumOfObjectInHeight;

        createObjects();
        setupObjects();
    }

    private void reset() {
        mCurrentTranslateX = 0f;
        mCurrentTranslateY = 0f;
    }

    private void createObjects() {
        mObjects = new LargeImageObject[mNumOfObjects];

        GLESGLState state = new GLESGLState();
        state.setCullFaceState(true);
        state.setCullFace(GLES20.GL_BACK);
        state.setDepthState(false);

        for (int i = 0; i < mNumOfObjects; i++) {
            LargeImageObject object = mObjectPool.pollFirst();
            if (object == null) {
                mObjects[i] = new LargeImageObject();
            } else {
                mObjects[i] = object;
            }

            mObjects[i].setGLState(state);
            mObjects[i].setShader(mShader);
            mObjects[i].setCamera(mCamera);
            mParentNode.addChild(mObjects[i]);

            mObjects[i].setTextureMapped(false);
        }
    }

    private void setupObjects() {
        int halfWidth = mBitmapWidth / 2;
        int halfHeight = mBitmapHeight / 2;

        int objX = 0;
        int objY = 0;
        int objWidth = 0;
        int objHeight = 0;

        int imageX = 0;
        int imageY = 0;
        Rect rect = new Rect();
        for (int i = 0; i < mNumOfObjectInHeight; i++) {
            objY = halfHeight - GRID_SIZE * i;

            for (int j = 0; j < mNumOfObjectInWidth; j++) {
                int index = i * mNumOfObjectInWidth + j;

                mObjects[index].setCamera(mCamera);

                objX = -halfWidth + GRID_SIZE * j;

                imageX = halfWidth + objX;
                objWidth = GRID_SIZE;
                if (imageX + objWidth > mBitmapWidth) {
                    objWidth = mBitmapWidth - imageX;
                }

                imageY = halfHeight - objY;
                objHeight = GRID_SIZE;
                if (imageY + objHeight > mBitmapHeight) {
                    objHeight = mBitmapHeight - imageY;
                }

                mObjects[index].setPosition(objX, objY);
                mObjects[index].setImagePosition(imageX, imageY);
                mObjects[index].setWidth(objWidth);
                mObjects[index].setHeight(objHeight);

                GLESVertexInfo vertexInfo = GalleryUtils.createImageVertexInfo(mShader,
                        objX, objY,
                        objWidth, objHeight);
                mObjects[index].setVertexInfo(vertexInfo, true, false);

                if (isInScreen(mObjects[index]) == true) {
                    rect.set(imageX, imageY, imageX + objWidth, imageY + objHeight);
                    Bitmap bitmap = mDecoder.decodeRegion(rect, null);
                    GLESTexture.Builder builder = new GLESTexture.Builder(
                            GLES20.GL_TEXTURE_2D, bitmap.getWidth(), bitmap.getHeight())
                            .setWrapMode(GLES20.GL_CLAMP_TO_EDGE)
                            .setFilter(GLES20.GL_NEAREST, GLES20.GL_NEAREST);
                    GLESTexture texture = builder.load(bitmap);
                    bitmap.recycle();
                    bitmap = null;

                    mObjects[index].setTexture(texture);
                    mObjects[index].setTextureMapped(true);
                }
            }
        }
    }

    private boolean isInScreen(LargeImageObject object) {
        boolean isInScreen = false;

        float left = object.getX();
        float right = left + object.getWidth();
        float top = object.getY();
        float bottom = top - object.getHeight();

        isInScreen(left, right, bottom, top);

        return isInScreen;
    }

    private boolean isInScreen(float left, float right, float bottom, float top) {
        boolean isInScreen = false;

        float screenLeft = -mAdjustedWidth * 0.5f;
        float screenRight = mAdjustedWidth * 0.5f;
        float screenTop = mAdjustedHeight * 0.5f;
        float screenBottom = -mAdjustedHeight * 0.5f;

        if (left < screenRight && right > screenLeft
                && top > screenBottom && bottom < screenTop) {
            isInScreen = true;
        }

        return isInScreen;
    }

    private GLESNodeListener mNodeListener = new GLESNodeListener() {
        @Override
        public void update(GLESNode node) {
            GLESTransform transform = node.getTransform();
            transform.setIdentity();

            if (mOrientation != 0) {
                transform.setRotate(-mOrientation, 0f, 0f, 1f);
            }

            transform.setTranslate(mCurrentTranslateX, mCurrentTranslateY, 0f);

        }
    };
}
