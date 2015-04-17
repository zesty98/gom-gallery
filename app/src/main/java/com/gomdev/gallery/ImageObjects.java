package com.gomdev.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLES20;
import android.util.Log;
import android.util.SparseArray;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESObjectListener;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by gomdev on 15. 1. 13..
 */
class ImageObjects implements ImageLoadingListener, GridInfoChangeListener {
    static final String CLASS = "ImageObjects";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int ALPAH_ANIMATION_DURATION = 300;
    private final float VISIBILITY_PADDING_DP = 60f;    // dp

    private final float DUMMY_TEXTURE_R = 0.8f;
    private final float DUMMY_TEXTURE_G = 0.8f;
    private final float DUMMY_TEXTURE_B = 0.8f;
    private final float DUMMY_TEXTURE_A = 1.0f;
    private final int DUMMY_TEXTURE_COLOR = (int) (DUMMY_TEXTURE_A * 0xFF) << 24 |
            (int) (DUMMY_TEXTURE_R * 0xFF) << 16 |
            (int) (DUMMY_TEXTURE_G * 0xFF) << 8 |
            (int) (DUMMY_TEXTURE_B * 0xFF);

    private final Context mContext;
    private final GridInfo mGridInfo;
    private final DateLabelInfo mDateLabelInfo;

    private GallerySurfaceView mSurfaceView = null;
    private AlbumViewManager mAlbumViewManager = null;
    private ImageLoader mImageLoader = null;
    private GalleryNode mParentNode = null;
    private GLESCamera mCamera = null;

    private List<ImageObject> mObjects = new ArrayList<>();

    private GLESShader mShader = null;
    private GLESGLState mGLState = null;
    private GLESTexture mDummyTexture = null;

    private int mWidth = 0;
    private int mHeight = 0;

    private int mSpacing = 0;
    private int mNumOfColumns = 0;
    private int mNumOfImages = 0;
    private int mColumnWidth = 0;
    private int mPrevColumnWidth = 0;
    private int mDefaultColumnWidth = 0;

    private float mStartOffsetY = 0f;
    private float mPrevStartOffsetY = 0f;
    private float mNextStartOffsetY = 0f;
    private float mEndOffsetY = 0f;

    private float mVisibilityPadding = 0f;

    private List<TextureMappingInfo> mTextureMappingInfos = new ArrayList<>();
    private Queue<GalleryTexture> mWaitingTextures = new ConcurrentLinkedQueue<>();
    private SparseArray<ImageObject> mInvisibleObjects = new SparseArray<>();
    private ArrayList<ImageObject> mAnimationObjects = new ArrayList<>();

    private float mScale = 1f;

    private boolean mNeedToSetTranslate = false;

    private long mCurrentTime = 0L;

    ImageObjects(Context context, GridInfo gridInfo, DateLabelInfo dateLabelInfo) {
        mContext = context;
        mGridInfo = gridInfo;
        mDateLabelInfo = dateLabelInfo;

        setGridInfo(mGridInfo);

        mNumOfImages = mDateLabelInfo.getNumOfImages();

        init();
    }

    private void init() {
        mImageLoader = ImageLoader.getInstance();

        mVisibilityPadding = GLESUtils.getPixelFromDpi(mContext, VISIBILITY_PADDING_DP);
    }

    private void setGridInfo(GridInfo gridInfo) {
        mSpacing = gridInfo.getSpacing();
        mNumOfColumns = gridInfo.getNumOfColumns();
        mColumnWidth = gridInfo.getColumnWidth();
        mPrevColumnWidth = mColumnWidth;
        mDefaultColumnWidth = gridInfo.getDefaultColumnWidth();

        mScale = (float) mColumnWidth / mDefaultColumnWidth;

        mGridInfo.addListener(this);
    }

    private void clear() {
        if (DEBUG) {
            Log.d(TAG, "clear()");
        }
        mInvisibleObjects.clear();
        mWaitingTextures.clear();
    }

    // rendering

    void update() {
        if (mNeedToSetTranslate == true) {
            setTranslate();
        }
    }

    private void setTranslate() {
        mNeedToSetTranslate = false;

        int size = mObjects.size();
        for (int i = 0; i < size; i++) {
            ImageObject object = mObjects.get(i);
            setTranslate(object);
        }
    }

    private void setTranslate(ImageObject object) {
        float translateX = object.getLeft() - (-mDefaultColumnWidth * mScale * 0.5f);
        float translateY = mStartOffsetY + object.getTop() - (mDefaultColumnWidth * mScale * 0.5f);
        object.setTranslate(translateX, translateY);
    }

    void updateTexture(long currentTime) {
        mCurrentTime = currentTime;

        GalleryTexture texture = mWaitingTextures.poll();

        if (texture != null) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(texture.getIndex());
            final ImageObject object = (ImageObject) textureMappingInfo.getObject();
            Bitmap bitmap = texture.getBitmapDrawable().getBitmap();

            if (bitmap == null) {
                int width = mWidth / 10;
                int height = mHeight / 10;

                bitmap = GLESUtils.makeBitmap(width, height, Bitmap.Config.ARGB_8888, Color.DKGRAY);
            }
            texture.load(bitmap);

            object.setTexture(texture.getTexture());

            object.setAnimationStartTime(currentTime);
            object.setIsOnAlphaAnimation(true);
        }

        if (mWaitingTextures.size() > 0) {
            mSurfaceView.requestRender();
        }
    }

    void checkVisibility(boolean parentVisibility) {
        if (parentVisibility == true) {
            handleVisibleObjects();
        } else {
            handleInvisibleObjects();
        }
    }

    private void handleVisibleObjects() {
        float translateY = mGridInfo.getTranslateY();
        float viewportTop = mHeight * 0.5f - translateY;
        float viewportBottom = viewportTop - mHeight;

        int size = mObjects.size();
        for (int i = 0; i < size; i++) {
            ImageObject object = mObjects.get(i);

            if (mInvisibleObjects.get(i) != null) {
                if (object.isVisibilityChanged() == true) {
                    if (object.isTexturMapped() == true) {
                        unmapTexture(i, object);
                        object.setTextureMapping(false);
                    }
                }
                continue;
            }

            float top = object.getTop() + mStartOffsetY;

            if ((top - mColumnWidth) < (viewportTop + mVisibilityPadding) &&
                    (top > (viewportBottom - mVisibilityPadding))) {

                object.setVisibility(true);

                if (object.isTexturMapped() == false) {
                    mapTexture(i);
                    object.setTextureMapping(true);
                }
            } else {
                object.setVisibility(false);

                if (object.isVisibilityChanged() == true) {
                    if (object.isTexturMapped() == true) {
                        unmapTexture(i, object);
                        object.setTextureMapping(false);
                    }
                }
            }
        }
    }

    private void handleInvisibleObjects() {
        int size = mObjects.size();
        for (int i = 0; i < size; i++) {
            ImageObject object = mObjects.get(i);

            object.setVisibility(false);

            unmapTexture(i, object);
            object.setTextureMapping(false);
        }
    }

    private void mapTexture(int index) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(index);

        ImageInfo imageInfo = (ImageInfo) textureMappingInfo.getGalleryInfo();
        GalleryTexture texture = textureMappingInfo.getTexture();
        if (texture == null) {
            texture = new GalleryTexture(imageInfo.getWidth(), imageInfo.getHeight());
            texture.setIndex(index);
            texture.setImageLoadingListener(this);
        }

        if ((texture != null && texture.isTextureLoadingNeeded() == true)) {
            mImageLoader.loadThumbnail(imageInfo, texture);
            textureMappingInfo.setTexture(texture);
            mSurfaceView.requestRender();
        }
    }

    private void unmapTexture(int index, ImageObject object) {
        object.setTexture(mDummyTexture);

        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(index);
        GalleryTexture texture = textureMappingInfo.getTexture();

        if (texture == null) {
            return;
        }

        GalleryTexture.State state = texture.getState();

        switch (state) {
            case DECODING:
                BitmapWorker.cancelWork(texture);
                break;
            case QUEUING:
                mWaitingTextures.remove(texture);
                break;
        }
        textureMappingInfo.setTexture(null);
    }

    // onSurfaceChanged

    void onSurfaceChanged(int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceChanged() width=" + width + " height=" + height);
        }

        mWidth = width;
        mHeight = height;
    }

    void setupObjects(GLESCamera camera) {
        if (DEBUG) {
            Log.d(TAG, "setupObjects(camera)");
        }

        mCamera = camera;

        int size = mObjects.size();
        for (int i = 0; i < size; i++) {
            ImageObject object = mObjects.get(i);
            object.setCamera(mCamera);

            float left = mSpacing + (i % mNumOfColumns) * (mColumnWidth + mSpacing) - mWidth * 0.5f;
            float top = -((i / mNumOfColumns) * (mColumnWidth + mSpacing));

            object.setLeftTop(left, top);

            object.setTranslate(left - (-mColumnWidth * 0.5f), mStartOffsetY + top - (mColumnWidth * 0.5f));
            object.setScale(mScale);
            object.setAlpha(1.0f);

            GLESVertexInfo vertexInfo = object.getVertexInfo();
            float[] vertex = GLESUtils.makePositionCoord(-mDefaultColumnWidth * 0.5f, mDefaultColumnWidth * 0.5f, mDefaultColumnWidth, mDefaultColumnWidth);
            vertexInfo.setBuffer(mShader.getPositionAttribIndex(), vertex, 3);

            mEndOffsetY = top - mColumnWidth;
        }
    }

    // onSurfaceCreated

    void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        cancelLoading();
        clear();

        mDummyTexture = GalleryUtils.createDummyTexture(DUMMY_TEXTURE_COLOR);

        int size = mObjects.size();
        for (int i = 0; i < size; i++) {
            ImageObject object = mObjects.get(i);
            object.setTextureMapping(false);
            object.setVisibility(false);
            object.setShader(mShader);

            object.setTexture(mDummyTexture);
        }

        size = mTextureMappingInfos.size();
        for (int i = 0; i < size; i++) {
            mTextureMappingInfos.get(i).setTexture(null);
        }
    }

    @Override
    public void onColumnWidthChanged() {
        if (DEBUG) {
            Log.d(TAG, "onColumnWidthChanged()");
        }

        mNeedToSetTranslate = false;

        mPrevColumnWidth = mColumnWidth;
        mColumnWidth = mGridInfo.getColumnWidth();

        mNumOfColumns = mGridInfo.getNumOfColumns();

        mScale = (float) mColumnWidth / mDefaultColumnWidth;

        changeImageObjectPosition();

        setupAnimations();
    }

    private void changeImageObjectPosition() {
        int size = mObjects.size();
        for (int i = 0; i < size; i++) {
            ImageObject object = mObjects.get(i);

            object.setPrevLeftTop(object.getLeft(), object.getTop());

            float prevScale = (float) mPrevColumnWidth / mDefaultColumnWidth;
            object.setPrevScale(prevScale);

            float nextLeft = mSpacing + (i % mNumOfColumns) * (mColumnWidth + mSpacing) - mWidth * 0.5f;
            float nextTop = -((i / mNumOfColumns) * (mColumnWidth + mSpacing));
            object.setNextLeftTop(nextLeft, nextTop);

            float nextScale = (float) mColumnWidth / mDefaultColumnWidth;
            object.setNextScale(nextScale);

            mEndOffsetY = nextTop - mDefaultColumnWidth * nextScale;
        }
    }

    private void setupAnimations() {
        mAnimationObjects.clear();

        if (mParentNode.getVisibility() == false) {
            int size = mObjects.size();
            for (int i = 0; i < size; i++) {
                ImageObject object = mObjects.get(i);

                object.setVisibility(false);
                mInvisibleObjects.put(i, object);
            }

            return;
        }

        float viewportTop = mHeight * 0.5f - mGridInfo.getTranslateY();
        float viewportBottom = viewportTop - mHeight;

        float nextTranslateY = mAlbumViewManager.getNextTranslateY();
        float nextViewportTop = mHeight * 0.5f - nextTranslateY;
        float nextViewportBottom = nextViewportTop - mHeight;

        int size = mObjects.size();
        for (int i = 0; i < size; i++) {
            ImageObject object = mObjects.get(i);

            float prevTop = object.getPrevTop();
            float prevBottom = prevTop - mPrevColumnWidth;

            float nextTop = object.getNextTop();
            float nextBottom = nextTop - mColumnWidth;

            if (((prevTop + mPrevStartOffsetY) >= viewportBottom && (prevBottom + mPrevStartOffsetY) <= viewportTop) ||
                    ((nextTop + mNextStartOffsetY) >= nextViewportBottom && (nextBottom + mNextStartOffsetY) <= nextViewportTop)) {
                object.setVisibility(true);

                mAnimationObjects.add(object);
            } else {
                object.setVisibility(false);

                mInvisibleObjects.put(i, object);
            }
        }
    }

    @Override
    public void onNumOfImageInfosChanged() {
        if (DEBUG) {
            Log.d(TAG, "onNumOfImageInfosChanged()");
        }

        mNumOfImages = mDateLabelInfo.getNumOfImages();

        int size = mObjects.size();
        for (int i = 0; i < size; i++) {
            ImageObject object = mObjects.get(i);

            float left = mSpacing + (i % mNumOfColumns) * (mColumnWidth + mSpacing) - mWidth * 0.5f;
            float top = -((i / mNumOfColumns) * (mColumnWidth + mSpacing));
            object.setLeftTop(left, top);

            float scale = (float) mColumnWidth / mDefaultColumnWidth;
            object.setScale(scale);

            mEndOffsetY = top - mDefaultColumnWidth * scale;
        }
    }

    @Override
    public void onNumOfDateLabelInfosChanged() {
    }

    // initialization

    void createObjects(GalleryNode parentNode) {
        if (DEBUG) {
            Log.d(TAG, "createObjects(parentNode)");
        }

        mParentNode = parentNode;

        for (int i = 0; i < mNumOfImages; i++) {
            ImageObject object = new ImageObject("ImageObject_" + mDateLabelInfo.getIndex() + "_" + i);

            mObjects.add(object);
            mParentNode.addChild(object);
            object.setGLState(mGLState);
            object.setListener(mObjectListener);
            object.setIndex(i);
            object.setVisibility(false);
            object.setTextureMapping(false);

            GLESVertexInfo vertexInfo = new GLESVertexInfo();
            vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
            vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);
            object.setVertexInfo(vertexInfo, false, false);

            ImageInfo imageInfo = mDateLabelInfo.get(i);
            TextureMappingInfo textureMappingInfo = new TextureMappingInfo(object, imageInfo);
            mTextureMappingInfos.add(textureMappingInfo);
        }
    }

    // set / get

    void setSurfaceView(GallerySurfaceView surfaceView) {
        if (DEBUG) {
            Log.d(TAG, "setSurfaceView()");
        }

        mSurfaceView = surfaceView;
    }

    void setAlbumViewManager(AlbumViewManager manager) {
        mAlbumViewManager = manager;
    }


    void setStartOffsetY(float startOffsetY) {
        mStartOffsetY = startOffsetY;

        mNeedToSetTranslate = true;

        int size = mObjects.size();
        for (int i = 0; i < size; i++) {
            ImageObject object = mObjects.get(i);
            object.setStartOffsetY(startOffsetY);
        }
    }

    float getStartOffsetY() {
        return mStartOffsetY;
    }

    void setPrevStartOffsetY(float prevStartOffsetY) {
        mPrevStartOffsetY = prevStartOffsetY;
    }

    float getPrevStartOffsetY() {
        return mPrevStartOffsetY;
    }

    void setNextStartOffsetY(float nextStartOffsetY) {
        mNextStartOffsetY = nextStartOffsetY;

        int size = mObjects.size();
        for (int i = 0; i < size; i++) {
            ImageObject object = mObjects.get(i);
            object.setNextStartOffsetY(nextStartOffsetY);
        }
    }

    float getNextStartOffsetY() {
        return mNextStartOffsetY;
    }

    float getBottom() {
        return mEndOffsetY + mStartOffsetY;
    }

    int getNumOfImages() {
        return mNumOfImages;
    }

    void setShader(GLESShader shader) {
        if (DEBUG) {
            Log.d(TAG, "setShader()");
        }

        mShader = shader;

        int location = mShader.getUniformLocation("uAlpha");
        GLES20.glUniform1f(location, 1f);

        location = mShader.getUniformLocation("uDefaultColor");
        GLES20.glUniform4f(location, DUMMY_TEXTURE_R, DUMMY_TEXTURE_G, DUMMY_TEXTURE_B, DUMMY_TEXTURE_A);
    }

    void setGLState(GLESGLState state) {
        mGLState = state;
    }

    ImageObject getObject(int index) {
        return mObjects.get(index);
    }

    void delete(int index) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(index);

        GalleryTexture texture = textureMappingInfo.getTexture();
        texture = null;

        mTextureMappingInfos.remove(index);

        mObjects.remove(index);
    }

    @Override
    public void onImageLoaded(int index, GalleryTexture texture) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(index);
        final ImageObject object = (ImageObject) textureMappingInfo.getObject();

        final Bitmap bitmap = texture.getBitmapDrawable().getBitmap();

        int imageWidth = 0;
        int imageHeight = 0;

        if (bitmap == null) {
            ImageInfo imageInfo = (ImageInfo) textureMappingInfo.getGalleryInfo();
            imageWidth = imageInfo.getWidth();
            imageHeight = imageInfo.getHeight();
        } else {
            imageWidth = bitmap.getWidth();
            imageHeight = bitmap.getHeight();
        }

        float[] texCoord = GalleryUtils.calcTexCoord(imageWidth, imageHeight);

        GLESVertexInfo vertexInfo = object.getVertexInfo();
        vertexInfo.setBuffer(mShader.getTexCoordAttribIndex(), texCoord, 2);

        mWaitingTextures.add(texture);
        mSurfaceView.requestRender();
    }

    void cancelLoading() {
        int size = mTextureMappingInfos.size();
        for (int i = 0; i < size; i++) {
            TextureMappingInfo info = mTextureMappingInfos.get(i);

            GalleryTexture texture = info.getTexture();
            if (texture == null) {
                continue;
            }

            GalleryTexture.State state = texture.getState();

            switch (state) {
                case DECODING:
                    BitmapWorker.cancelWork(texture);
                    break;
            }
        }
    }

    void invalidateObjects() {
        int size = mInvisibleObjects.size();
        for (int i = 0; i < size; i++) {
            ImageObject object = mInvisibleObjects.valueAt(i);
            invalidateObject(object);
        }

        mInvisibleObjects.clear();
    }

    private void invalidateObject(ImageObject object) {
        object.setLeftTop(object.getNextLeft(), object.getNextTop());
        object.setScale(object.getNextScale());

        float translateX = object.getLeft() + mDefaultColumnWidth * object.getScale() * 0.5f;
        float translateY = mStartOffsetY + object.getTop() - mDefaultColumnWidth * object.getScale() * 0.5f;

        object.setTranslate(translateX, translateY);
    }

    void onAnimation(float x) {
        mNeedToSetTranslate = false;

        int size = mAnimationObjects.size();
        for (int i = 0; i < size; i++) {
            ImageObject object = mAnimationObjects.get(i);

            float prevLeft = object.getPrevLeft();
            float nextLeft = object.getNextLeft();
            float currentLeft = prevLeft + (nextLeft - prevLeft) * x;

            float prevTop = object.getPrevTop();
            float nextTop = object.getNextTop();
            float currentTop = prevTop + (nextTop - prevTop) * x;

            object.setLeftTop(currentLeft, currentTop);

            float prevScale = object.getPrevScale();
            float nextScale = object.getNextScale();
            float currentScale = prevScale + (nextScale - prevScale) * x;

            mScale = currentScale;
            object.setScale(currentScale);

            float translateX = currentLeft - (-mDefaultColumnWidth * currentScale * 0.5f);
            float translateY = mStartOffsetY + currentTop - (mDefaultColumnWidth * currentScale * 0.5f);
            object.setTranslate(translateX, translateY);
        }
    }

    private GLESObjectListener mObjectListener = new GLESObjectListener() {
        @Override
        public void update(GLESObject object) {
            ImageObject imageObject = (ImageObject) object;
            GLESTransform transform = object.getTransform();
            transform.setIdentity();
            transform.setTranslate(
                    imageObject.getTranslateX(),
                    imageObject.getTranslateY(),
                    0f);

            transform.scale(imageObject.getScale());

            long startTime = imageObject.getAnimationStartTime();
            int elapsedTime = (int) (mCurrentTime - startTime);
            if (imageObject.isOnAlphaAnimation() == true && elapsedTime <= ALPAH_ANIMATION_DURATION) {
                float alpha = ((float) elapsedTime / ALPAH_ANIMATION_DURATION);
                imageObject.setAlpha(alpha);

                mSurfaceView.requestRender();
            } else {
                if (imageObject.isOnAlphaAnimation() == true) {
                    imageObject.setIsOnAlphaAnimation(false);
                    mSurfaceView.requestRender();
                }

                imageObject.setAlpha(1f);
            }
        }

        @Override
        public void apply(GLESObject object) {
            ImageObject imageObject = (ImageObject) object;
            float alpha = imageObject.getAlpha();

            int location = mShader.getUniformLocation("uAlpha");
            GLES20.glUniform1f(location, alpha);

            location = mShader.getUniformLocation("uDefaultColor");
            GLES20.glUniform4f(location, DUMMY_TEXTURE_R, DUMMY_TEXTURE_G, DUMMY_TEXTURE_B, DUMMY_TEXTURE_A);
        }
    };
}
