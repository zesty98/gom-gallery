package com.gomdev.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.SparseArray;

import com.gomdev.gles.GLESAnimator;
import com.gomdev.gles.GLESAnimatorCallback;
import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESObjectListener;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVector3;
import com.gomdev.gles.GLESVertexInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by gomdev on 15. 1. 13..
 */
public class ImageObjects implements ImageLoadingListener, GridInfoChangeListener {
    static final String CLASS = "ImageObjects";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final float VISIBILITY_PADDING_DP = 60f;    // dp

    private final Context mContext;
    private final ImageListRenderer mRenderer;

    private GallerySurfaceView mSurfaceView = null;
    private ImageManager mImageManager = null;
    private ImageLoader mImageLoader = null;

    private List<ImageObject> mObjects = new LinkedList<>();

    private GLESShader mTextureShader = null;
    private GLESGLState mGLState = null;
    private GLESTexture mDummyTexture = null;

    private int mWidth = 0;
    private int mHeight = 0;

    private DateLabelInfo mDateLabelInfo = null;

    private GridInfo mGridInfo = null;

    private int mSpacing = 0;
    private int mNumOfColumns = 0;
    private int mNumOfImages = 0;
    private int mColumnWidth = 0;
    private int mPrevColumnWidth = 0;
    private int mDefaultColumnWidth = 0;

    private float mStartOffsetY = 0f;
    private float mNextStartOffsetY = 0f;
    private float mEndOffsetY = 0f;

    private float mVisibilityPadding = 0f;

    private List<TextureMappingInfo> mTextureMappingInfos = new LinkedList<>();
    private Queue<GalleryTexture> mWaitingTextures = new ConcurrentLinkedQueue<>();
    private SparseArray<ImageObject> mInvisibleObjects = new SparseArray<>();

    private List<GLESAnimator> mAnimators = new ArrayList<>();
    private int mAnimationFinishCount = 0;
    private int mAnimationCancelCount = 0;
    private float mScale = 1f;
    private float mAnimationVisibilityPadding = 0f;

    private boolean mNeedToSetTranslate = false;

    public ImageObjects(Context context, ImageListRenderer renderer) {
        mContext = context;
        mRenderer = renderer;

        init();
    }

    private void init() {
        mImageManager = ImageManager.getInstance();
        mImageLoader = ImageLoader.getInstance();

        mVisibilityPadding = GLESUtils.getPixelFromDpi(mContext, VISIBILITY_PADDING_DP);

        clear();
    }

    private void clear() {
        mObjects.clear();
        mInvisibleObjects.clear();
        mWaitingTextures.clear();
        mTextureMappingInfos.clear();
        mAnimators.clear();
    }

    // rendering

    public void update() {
        int requestRenderCount = 0;

        int size = mAnimators.size();
        for (int i = 0; i < size; i++) {
            if (mAnimators.get(i).doAnimation() == true) {
                requestRenderCount++;
            }
        }

        if (requestRenderCount > 0) {
            mSurfaceView.requestRender();
        }

        if (mNeedToSetTranslate == true) {
            setTranslate();
        }
    }

    private void setTranslate() {
        mNeedToSetTranslate = false;

        Iterator<ImageObject> iter = mObjects.iterator();
        while (iter.hasNext()) {
            ImageObject object = iter.next();
            setTranslate(object);
        }
    }

    private void setTranslate(ImageObject object) {
        float translateX = object.getLeft() - (-mDefaultColumnWidth * mScale * 0.5f);
        float translateY = mStartOffsetY + object.getTop() - (mDefaultColumnWidth * mScale * 0.5f);
        object.setTranslate(translateX, translateY);
    }

    public void updateTexture() {
        GalleryTexture texture = mWaitingTextures.poll();

        if (texture != null) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(texture.getPosition());
            final ImageObject object = (ImageObject) textureMappingInfo.getObject();
            final Bitmap bitmap = texture.getBitmapDrawable().getBitmap();
            texture.load(bitmap);

            object.setTexture(texture.getTexture());
        }

        if (mWaitingTextures.size() > 0) {
            mSurfaceView.requestRender();
        }
    }

    public void checkVisibility(boolean parentVisibility, boolean needToMapTexture, float translateY) {
        float viewportTop = mHeight * 0.5f - translateY;
        float viewportBottom = viewportTop - mHeight;

        int index = 0;
        Iterator<ImageObject> iter = mObjects.iterator();
        while (iter.hasNext()) {
            ImageObject object = iter.next();

            if (mInvisibleObjects.get(index) != null) {
                index++;
                continue;
            }

            if (parentVisibility == false) {
                object.hide();
                unmapTexture(index, object);
            } else {
                float top = object.getTop() + mStartOffsetY;

                if ((top - mColumnWidth) < (viewportTop + mVisibilityPadding) &&
                        (top > (viewportBottom - mVisibilityPadding))) {

                    object.show();

                    if (needToMapTexture == true) {
                        mapTexture(index);
                    }
                } else {
                    object.hide();

                    if (needToMapTexture == true) {
                        unmapTexture(index, object);
                    }
                }
            }

            index++;
        }
    }

    private void mapTexture(int index) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(index);

        ImageInfo imageInfo = (ImageInfo) textureMappingInfo.getGalleryInfo();
        GalleryTexture texture = textureMappingInfo.getTexture();
        if (texture == null) {
            texture = new GalleryTexture(imageInfo.getWidth(), imageInfo.getHeight());
            texture.setPosition(index);
            texture.setImageLoadingListener(this);
        }

        if ((texture != null && texture.isTextureLoadingNeeded() == true)) {
            mImageLoader.loadThumbnail(imageInfo, texture);
            textureMappingInfo.set(texture);
            mSurfaceView.requestRender();
        }
    }

    private void unmapTexture(int index, ImageObject object) {
        object.setTexture(mDummyTexture);

        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(index);
        GalleryTexture texture = textureMappingInfo.getTexture();
        BitmapWorker.cancelWork(texture);
        mWaitingTextures.remove(texture);

        textureMappingInfo.set(null);
    }


    // onSurfaceChanged

    public void onSurfaceChanged(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void setupObjects(GLESCamera camera) {
        int index = 0;
        Iterator<ImageObject> iter = mObjects.iterator();
        while (iter.hasNext()) {
            ImageObject object = iter.next();
            object.setCamera(camera);

            float left = mSpacing + (index % mNumOfColumns) * (mColumnWidth + mSpacing) - mWidth * 0.5f;
            float top = -((index / mNumOfColumns) * (mColumnWidth + mSpacing));

            object.setTranslate(left - (-mColumnWidth * 0.5f), mStartOffsetY + top - (mColumnWidth * 0.5f));
            object.setScale(mScale);

            float[] vertex = GLESUtils.makePositionCoord(-mDefaultColumnWidth * 0.5f, mDefaultColumnWidth * 0.5f, mDefaultColumnWidth, mDefaultColumnWidth);

            GLESVertexInfo vertexInfo = object.getVertexInfo();
            vertexInfo.setBuffer(mTextureShader.getPositionAttribIndex(), vertex, 3);

            object.setLeftTop(left, top);

            mEndOffsetY = top - mColumnWidth;

            index++;
        }
    }

    @Override
    public void onColumnWidthChanged() {
        mPrevColumnWidth = mColumnWidth;
        mColumnWidth = mGridInfo.getColumnWidth();

        mNumOfColumns = mGridInfo.getNumOfColumns();

        mScale = (float) mColumnWidth / mDefaultColumnWidth;

        changeImageObjectPosition();

        int size = mAnimators.size();
        for (int i = 0; i < size; i++) {
            mAnimators.get(i).cancel();
        }

        mAnimationCancelCount = 0;
        mAnimators.clear();

        setupAnimations();

        mAnimationFinishCount = 0;
        size = mAnimators.size();
        for (int i = 0; i < size; i++) {
            mAnimators.get(i).start();
        }
    }

    private void changeImageObjectPosition() {
        int index = 0;
        Iterator<ImageObject> iter = mObjects.iterator();
        while (iter.hasNext()) {
            ImageObject object = iter.next();

            float left = object.getLeft();
            float top = object.getTop();
            object.setLeftTop(left, top);

            float scale = (float) mPrevColumnWidth / mDefaultColumnWidth;
            object.setScale(scale);

            float nextLeft = mSpacing + (index % mNumOfColumns) * (mColumnWidth + mSpacing) - mWidth * 0.5f;
            float nextTop = -((index / mNumOfColumns) * (mColumnWidth + mSpacing));
            object.setNextLeftTop(nextLeft, nextTop);

            float nextScale = (float) mColumnWidth / mDefaultColumnWidth;
            object.setNextScale(nextScale);

            mEndOffsetY = nextTop - mDefaultColumnWidth * nextScale;

            index++;
        }
    }

    private void setupAnimations() {
        float viewportTop = mHeight * 0.5f - mGridInfo.getTranslateY() - mAnimationVisibilityPadding;
        float viewportBottom = viewportTop - mHeight + mAnimationVisibilityPadding;

        float nextTranslateY = mRenderer.getNextTranslateY();
        float nextViewportTop = mHeight * 0.5f - nextTranslateY - mAnimationVisibilityPadding;
        float nextViewportBottom = nextViewportTop - mHeight + mAnimationVisibilityPadding;

        int index = 0;
        Iterator<ImageObject> iter = mObjects.iterator();
        while (iter.hasNext()) {
            ImageObject object = iter.next();

            float left = object.getLeft();
            float top = object.getTop();
            float bottom = top - mPrevColumnWidth;
            float scale = object.getScale();

            float nextLeft = object.getNextLeft();
            float nextTop = object.getNextTop();
            float nextBottom = nextTop - mColumnWidth;
            float nextScale = object.getNextScale();

            if ((top + mStartOffsetY >= viewportBottom && bottom + mStartOffsetY <= viewportTop) ||
                    (nextTop + mNextStartOffsetY >= nextViewportBottom && nextBottom + mNextStartOffsetY <= nextViewportTop)) {
                GLESVector3 from = new GLESVector3(left, top, scale);
                GLESVector3 to = new GLESVector3(nextLeft, nextTop, nextScale);


                GLESAnimator animator = new GLESAnimator(new GalleryAnimatorCallback(object));
                animator.setValues(from, to);
                animator.setDuration(GalleryConfig.IMAGE_ANIMATION_START_OFFSET, GalleryConfig.IMAGE_ANIMATION_END_OFFSET);

                mAnimators.add(animator);
            } else {
                object.hide();
                mInvisibleObjects.put(index, object);
            }

            index++;
        }
    }

    @Override
    public void onNumOfImageInfosChanged() {
        mNumOfImages = mDateLabelInfo.getNumOfImages();
    }

    @Override
    public void onNumOfDateLabelInfosChanged() {
    }

    // onSurfaceCreated

    public void createObjects(GLESNode parentNode) {
        cancelLoading();

        clear();

        for (int i = 0; i < mNumOfImages; i++) {
            ImageObject object = new ImageObject("image" + i);
            mObjects.add(object);
            parentNode.addChild(object);
            object.setGLState(mGLState);
            object.setShader(mTextureShader);
            object.setTexture(mDummyTexture);
            object.setListener(mObjectListener);
            object.setIndex(i);

            GLESVertexInfo vertexInfo = new GLESVertexInfo();
            vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
            vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);
            object.setVertexInfo(vertexInfo, false, false);

            ImageInfo imageInfo = mDateLabelInfo.get(i);
            TextureMappingInfo textureMappingInfo = new TextureMappingInfo(object, imageInfo);
            mTextureMappingInfos.add(textureMappingInfo);
        }
    }

    private void cancelLoading() {
        int size = mTextureMappingInfos.size();

        for (int i = 0; i < size; i++) {
            GalleryTexture texture = mTextureMappingInfos.get(i).getTexture();
            if (texture != null) {
                BitmapWorker.cancelWork(mTextureMappingInfos.get(i).getTexture());
            }
        }
    }


    // initialization

    public void setSurfaceView(GallerySurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mSpacing = gridInfo.getSpacing();
        mNumOfColumns = gridInfo.getNumOfColumns();
        mColumnWidth = gridInfo.getColumnWidth();
        mPrevColumnWidth = mColumnWidth;
        mDefaultColumnWidth = gridInfo.getDefaultColumnWidth();
        mAnimationVisibilityPadding = gridInfo.getActionBarHeight();

        mScale = (float) mColumnWidth / mDefaultColumnWidth;

        mGridInfo.addListener(this);
    }

    public void setDateLabelInfo(DateLabelInfo dateLabelInfo) {
        mDateLabelInfo = dateLabelInfo;

        mNumOfImages = mDateLabelInfo.getNumOfImages();
    }

    public void setStartOffsetY(float startOffsetY) {
        mStartOffsetY = startOffsetY;

        mNeedToSetTranslate = true;

        Iterator<ImageObject> iter = mObjects.iterator();
        while (iter.hasNext()) {
            ImageObject object = iter.next();
            object.setStartOffsetY(startOffsetY);
        }
    }

    public float getStartOffsetY() {
        return mStartOffsetY;
    }

    public void setNextStartOffsetY(float nextStartOffsetY) {
        mNextStartOffsetY = nextStartOffsetY;

        Iterator<ImageObject> iter = mObjects.iterator();
        while (iter.hasNext()) {
            ImageObject object = iter.next();
            object.setNextStartOffsetY(nextStartOffsetY);
        }
    }

    public float getNextStartOffsetY() {
        return mNextStartOffsetY;
    }

    public float getBottom() {
        return mEndOffsetY + mStartOffsetY;
    }

    public int getNumOfImages() {
        return mNumOfImages;
    }

    public void setShader(GLESShader shader) {
        mTextureShader = shader;
    }

    public void setGLState(GLESGLState state) {
        mGLState = state;
    }

    public void setDummyTexture(GLESTexture texture) {
        mDummyTexture = texture;
    }

    public ImageObject getObject(int index) {
        return mObjects.get(index);
    }

    public void delete(int index) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(index);

        GalleryTexture texture = textureMappingInfo.getTexture();
        texture = null;

        mTextureMappingInfos.remove(index);

        mObjects.remove(index);
    }

    @Override
    public void onImageLoaded(int position, GalleryTexture texture) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(position);
        final ImageObject object = (ImageObject) textureMappingInfo.getObject();

        final Bitmap bitmap = texture.getBitmapDrawable().getBitmap();

        float[] texCoord = GalleryUtils.calcTexCoord(bitmap.getWidth(), bitmap.getHeight());

        GLESVertexInfo vertexInfo = object.getVertexInfo();
        GLESShader shader = object.getShader();
        vertexInfo.setBuffer(shader.getTexCoordAttribIndex(), texCoord, 2);

        mWaitingTextures.add(texture);
        mSurfaceView.requestRender();
    }


    private void onAnimationFinished() {
        mAnimationFinishCount++;

        int size = mAnimators.size();
        if (mAnimationFinishCount >= size) {
            mAnimationFinishCount = 0;
        }
    }

    public void invalidateObjects() {
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

    private void onAnimationCanceled() {
        mAnimationCancelCount++;

        int size = mAnimators.size();
        if (mAnimationCancelCount >= size) {
            mAnimationCancelCount = 0;
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
        }

        @Override
        public void apply(GLESObject object) {

        }
    };

    class GalleryAnimatorCallback implements GLESAnimatorCallback {
        private final ImageObject mObject;

        GalleryAnimatorCallback(ImageObject object) {
            mObject = object;
        }

        @Override
        public void onAnimation(GLESVector3 current) {
            mNeedToSetTranslate = false;

            mObject.setLeftTop(current.mX, current.mY);

            mScale = current.mZ;

            mGridInfo.setScale(mScale);

            float translateX = current.mX - (-mDefaultColumnWidth * mScale * 0.5f);
            float translateY = mStartOffsetY + current.mY - (mDefaultColumnWidth * mScale * 0.5f);
            mObject.setTranslate(translateX, translateY);
            mObject.setScale(current.mZ);
        }

        @Override
        public void onCancel() {
            onAnimationCanceled();
        }

        @Override
        public void onFinished() {
            onAnimationFinished();
        }
    }
}
