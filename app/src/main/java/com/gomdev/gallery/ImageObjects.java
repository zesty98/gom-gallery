package com.gomdev.gallery;

import android.content.Context;
import android.graphics.Bitmap;

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
    private ImageLoader mImageLoader = null;

    private GalleryObject[] mObjects;

    private GLESShader mTextureShader = null;
    private GLESGLState mGLState = null;
    private GLESTexture mDummyTexture = null;

    private int mWidth = 0;
    private int mHeight = 0;

    private GridInfo mGridInfo = null;
    private BucketInfo mBucketInfo = null;

    private int mSpacing = 0;
    private int mNumOfColumns = 0;
    private int mNumOfImages = 0;
    private int mNumOfDateInfos = 0;
    private int mColumnWidth = 0;
    private int mPrevColumnWidth = 0;
    private int mDefaultColumnWidth = 0;
    private int mActionBarHeight = 0;
    private int mDateLabelHeight = 0;

    private float mVisibilityPadding = 0f;

    private ArrayList<TextureMappingInfo> mTextureMappingInfos = new ArrayList<>();
    private Queue<GalleryTexture> mWaitingTextures = new ConcurrentLinkedQueue<>();

    private List<GLESAnimator> mAnimators = new ArrayList<GLESAnimator>();
    private int mAnimationFinishCount = 0;
    private int mAnimationCancelCount = 0;
    private float mScale = 1f;

    public ImageObjects(Context context, ImageListRenderer renderer) {
        mContext = context;
        mRenderer = renderer;

        init();
    }

    private void init() {
        mImageLoader = ImageLoader.getInstance();

        mVisibilityPadding = GLESUtils.getPixelFromDpi(mContext, VISIBILITY_PADDING_DP);
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
    }

    public void updateTexture() {
        GalleryTexture texture = mWaitingTextures.poll();

        if (texture != null) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(texture.getPosition());
            final GalleryObject object = textureMappingInfo.getObject();
            final Bitmap bitmap = texture.getBitmapDrawable().getBitmap();
            texture.load(bitmap);

            object.setTexture(texture.getTexture());
        }

        if (mWaitingTextures.size() > 0) {
            mSurfaceView.requestRender();
        }
    }

    public void checkVisibility(float translateY) {
        float viewportTop = mHeight * 0.5f - translateY;
        float viewportBottom = viewportTop - mHeight;

        for (int i = 0; i < mNumOfImages; i++) {
            GalleryObject object = mObjects[i];

            float top = object.getTop();
            if ((top - mColumnWidth) < (viewportTop + mVisibilityPadding) &&
                    (top > (viewportBottom - mVisibilityPadding))) {
                object.show();
                mapTexture(i);
            } else {
                object.hide();
                unmapTexture(i, object);
            }
        }
    }

    private void mapTexture(int position) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(position);

        ImageInfo imageInfo = (ImageInfo) textureMappingInfo.getGalleryInfo();
        GalleryTexture texture = textureMappingInfo.getTexture();
        if (texture == null) {
            texture = new GalleryTexture(imageInfo.getWidth(), imageInfo.getHeight());
            texture.setPosition(position);
            texture.setImageLoadingListener(this);
        }

        if ((texture != null && texture.isTextureLoadingNeeded() == true)) {
            mImageLoader.loadThumbnail(imageInfo, texture);
            textureMappingInfo.set(texture);
            mSurfaceView.requestRender();
        }
    }

    private void unmapTexture(int position, GalleryObject object) {
        object.setTexture(mDummyTexture);

        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(position);
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

    public void setupImageObjects(GLESCamera camera) {
        int imageIndex = 0;

        float yOffset = mHeight * 0.5f - mActionBarHeight;

        for (int i = 0; i < mNumOfDateInfos; i++) {
            yOffset -= (mDateLabelHeight + mSpacing);

            DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(i);
            int numOfImages = dateLabelInfo.getNumOfImages();
            for (int j = 0; j < numOfImages; j++) {
                mObjects[imageIndex].setCamera(camera);

                float left = mSpacing + (j % mNumOfColumns) * (mColumnWidth + mSpacing) - mWidth * 0.5f;
                float top = yOffset - ((j / mNumOfColumns) * (mColumnWidth + mSpacing));

                mObjects[imageIndex].setTranslate(left - (-mColumnWidth * 0.5f), top - (mColumnWidth * 0.5f));
                mObjects[imageIndex].setScale(mScale);

                float[] vertex = GLESUtils.makePositionCoord(-mDefaultColumnWidth * 0.5f, mDefaultColumnWidth * 0.5f, mDefaultColumnWidth, mDefaultColumnWidth);

                GLESVertexInfo vertexInfo = mObjects[imageIndex].getVertexInfo();
                vertexInfo.setBuffer(mTextureShader.getPositionAttribIndex(), vertex, 3);

                mObjects[imageIndex].setLeftTop(left, top);

                imageIndex++;
            }

            yOffset -= (dateLabelInfo.getNumOfRows() * (mColumnWidth + mSpacing));
        }
    }

    @Override
    public void onGridInfoChanged() {
        mPrevColumnWidth = mColumnWidth;

        mColumnWidth = mGridInfo.getColumnWidth();
        mNumOfColumns = mGridInfo.getNumOfColumns();

        mScale = (float) mColumnWidth / mDefaultColumnWidth;

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

        mRenderer.onAnimationStarted();
    }

    private void setupAnimations() {
        int index = 0;
        float yOffset = mHeight * 0.5f - mActionBarHeight;
        for (int i = 0; i < mNumOfDateInfos; i++) {
            yOffset -= (mDateLabelHeight + mSpacing);
            DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(i);
            int numOfImages = dateLabelInfo.getNumOfImages();
            for (int j = 0; j < numOfImages; j++) {
                float left = mSpacing + (j % mNumOfColumns) * (mColumnWidth + mSpacing) - mWidth * 0.5f;
                float top = yOffset - ((j / mNumOfColumns) * (mColumnWidth + mSpacing));
                float scale = (float) mColumnWidth / mDefaultColumnWidth;

                float prevLeft = mObjects[index].getLeft();
                float prevTop = mObjects[index].getTop();
                float prevScale = (float) mPrevColumnWidth / mDefaultColumnWidth;

                GLESVector3 from = new GLESVector3(prevLeft, prevTop, prevScale);
                GLESVector3 to = new GLESVector3(left, top, scale);

                GLESAnimator animator = new GLESAnimator(new GalleryAnimatorCallback(mObjects[index]));
                animator.setValues(from, to);
                animator.setDuration(GalleryConfig.IMAGE_ANIMATION_START_OFFSET, GalleryConfig.IMAGE_ANIMATION_END_OFFSET);

                mAnimators.add(animator);

                index++;
            }

            yOffset -= (dateLabelInfo.getNumOfRows() * (mColumnWidth + mSpacing));
        }
    }

    // onSurfaceCreated

    public void createObjects(GLESNode parentNode) {
        cancelLoading();

        mWaitingTextures.clear();
        mTextureMappingInfos.clear();
        mAnimators.clear();

        mObjects = new GalleryObject[mNumOfImages];

        for (int i = 0; i < mNumOfImages; i++) {
            mObjects[i] = new GalleryObject("image" + i);
            parentNode.addChild(mObjects[i]);
            mObjects[i].setGLState(mGLState);
            mObjects[i].setShader(mTextureShader);
            mObjects[i].setTexture(mDummyTexture);
            mObjects[i].setListener(mObjectListener);
            mObjects[i].setPosition(i);

            GLESVertexInfo vertexInfo = new GLESVertexInfo();
            vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
            vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);
            mObjects[i].setVertexInfo(vertexInfo, false, false);

            ImageInfo imageInfo = mBucketInfo.getImageInfo(i);
            TextureMappingInfo textureMappingInfo = new TextureMappingInfo(mObjects[i], imageInfo);
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
        mActionBarHeight = gridInfo.getActionBarHeight();
        mBucketInfo = gridInfo.getBucketInfo();
        mNumOfImages = mGridInfo.getNumOfImages();
        mNumOfDateInfos = mGridInfo.getNumOfDateInfos();
        mDateLabelHeight = mGridInfo.getDateLabelHeight();

        mScale = (float) mColumnWidth / mDefaultColumnWidth;

        mGridInfo.addListener(this);
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

    public GalleryObject getObject(int index) {
        return mObjects[index];
    }

    @Override
    public void onImageLoaded(int position, GalleryTexture texture) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(position);
        final GalleryObject object = textureMappingInfo.getObject();
        final ImageInfo imageInfo = (ImageInfo) textureMappingInfo.getGalleryInfo();

        final Bitmap bitmap = texture.getBitmapDrawable().getBitmap();
        imageInfo.setThumbnailWidth(bitmap.getWidth());
        imageInfo.setThumbnailHeight(bitmap.getHeight());

        float[] texCoord = GalleryUtils.calcTexCoord(imageInfo);

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
            mRenderer.onAnimationFinished();
            mAnimationFinishCount = 0;
        }
    }

    private void onAnimationCanceled() {
        mAnimationCancelCount++;

        int size = mAnimators.size();
        if (mAnimationCancelCount >= size) {
            mRenderer.onAnimationCanceled();
            mAnimationCancelCount = 0;
        }
    }

    private GLESObjectListener mObjectListener = new GLESObjectListener() {
        @Override
        public void update(GLESObject object) {
            GalleryObject galleryObject = (GalleryObject) object;
            GLESTransform transform = object.getTransform();
            transform.setIdentity();
            transform.setTranslate(
                    galleryObject.getTranslateX(),
                    galleryObject.getTranslateY(),
                    0f);

            transform.scale(galleryObject.getScale());
        }

        @Override
        public void apply(GLESObject object) {

        }
    };

    class GalleryAnimatorCallback implements GLESAnimatorCallback {
        private final GalleryObject mObject;

        GalleryAnimatorCallback(GalleryObject object) {
            mObject = object;
        }

        @Override
        public void onAnimation(GLESVector3 current) {
            mObject.setLeftTop(current.mX, current.mY);

            mScale = current.mZ;

            mGridInfo.setScale(mScale);

            float translateX = current.mX - (-mDefaultColumnWidth * mScale * 0.5f);
            float translateY = current.mY - (mDefaultColumnWidth * mScale * 0.5f);
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
