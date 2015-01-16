package com.gomdev.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.gomdev.gles.GLESAnimator;
import com.gomdev.gles.GLESAnimatorCallback;
import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVector3;
import com.gomdev.gles.GLESVertexInfo;

import java.nio.FloatBuffer;
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

    private GallerySurfaceView mSurfaceView = null;
    private ImageManager mImageManager = null;

    private GalleryObject[] mImageObjects;

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
    private int mActionBarHeight = 0;
    private int mDateLabelHeight = 0;

    private float mVisibilityPadding = 0f;
    private float mTranslateY = 0f;

    private ArrayList<TextureMappingInfo> mTextureMappingInfos = new ArrayList<>();
    private Queue<GalleryTexture> mWaitingTextures = new ConcurrentLinkedQueue<>();

    private Interpolator mInterpolator = new LinearInterpolator();

    private List<GLESAnimator> mAnimators = new ArrayList<>();

    public ImageObjects(Context context) {
        mContext = context;

        init();
    }

    private void init() {
        mImageManager = ImageManager.getInstance();

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
        mTranslateY = translateY;

        float viewportTop = mHeight * 0.5f - translateY;
        float viewportBottom = viewportTop - mHeight;

        for (int i = 0; i < mNumOfImages; i++) {
            GalleryObject object = mImageObjects[i];

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
            mImageManager.loadThumbnail(imageInfo, texture);
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
                mImageObjects[imageIndex].setCamera(camera);

                float left = mSpacing + (j % mNumOfColumns) * (mColumnWidth + mSpacing) - mWidth * 0.5f;
                float top = yOffset - ((j / mNumOfColumns) * (mColumnWidth + mSpacing));

                float[] vertex = GLESUtils.makePositionCoord(left, top, mColumnWidth, mColumnWidth);

                GLESVertexInfo vertexInfo = mImageObjects[imageIndex].getVertexInfo();
                vertexInfo.setBuffer(mTextureShader.getPositionAttribIndex(), vertex, 3);

                mImageObjects[imageIndex].setLeftTop(left, top);

                imageIndex++;
            }

            yOffset -= (dateLabelInfo.getNumOfRows() * (mColumnWidth + mSpacing));
        }
    }

    @Override
    public void onGridInfoChanged() {
        float prevColumnWidth = mColumnWidth;
        mColumnWidth = mGridInfo.getColumnWidth();
        mNumOfColumns = mGridInfo.getNumOfColumns();


//        changeImageObjectPosition();

        int size = mAnimators.size();

        for (int i = 0; i < size; i++) {
            mAnimators.get(i).cancel();
        }

        mAnimators.clear();

        setAnimationInfo(prevColumnWidth);

        size = mAnimators.size();

        for (int i = 0; i < size; i++) {
            mAnimators.get(i).start();
        }
    }

    private void changeImageObjectPosition() {
        int index = 0;
        float yOffset = mHeight * 0.5f - mActionBarHeight;
        for (int i = 0; i < mNumOfDateInfos; i++) {
            yOffset -= (mDateLabelHeight + mSpacing);
            DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(i);
            int numOfImages = dateLabelInfo.getNumOfImages();
            for (int j = 0; j < numOfImages; j++) {
                float left = mSpacing + (j % mNumOfColumns) * (mColumnWidth + mSpacing) - mWidth * 0.5f;
                float right = left + mColumnWidth;
                float top = yOffset - ((j / mNumOfColumns) * (mColumnWidth + mSpacing));
                float bottom = top - mColumnWidth;

                mImageObjects[index].setLeftTop(left, top);

                GLESVertexInfo vertexInfo = mImageObjects[index].getVertexInfo();
                FloatBuffer buffer = (FloatBuffer) vertexInfo.getBuffer(mTextureShader.getPositionAttribIndex());

                buffer.put(0, left);
                buffer.put(1, bottom);

                buffer.put(3, right);
                buffer.put(4, bottom);

                buffer.put(6, left);
                buffer.put(7, top);

                buffer.put(9, right);
                buffer.put(10, top);

                index++;
            }

            yOffset -= (dateLabelInfo.getNumOfRows() * (mColumnWidth + mSpacing));
        }
    }

    private void setAnimationInfo(float prevColumnWidth) {
        float viewportTop = mHeight * 0.5f - mTranslateY;
        float viewportBottom = viewportTop - mHeight;

        int index = 0;
        float yOffset = mHeight * 0.5f - mActionBarHeight;
        for (int i = 0; i < mNumOfDateInfos; i++) {
            yOffset -= (mDateLabelHeight + mSpacing);
            DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(i);
            int numOfImages = dateLabelInfo.getNumOfImages();
            for (int j = 0; j < numOfImages; j++) {
                float left = mSpacing + (j % mNumOfColumns) * (mColumnWidth + mSpacing) - mWidth * 0.5f;
                float right = left + mColumnWidth;
                float top = yOffset - ((j / mNumOfColumns) * (mColumnWidth + mSpacing));
                float bottom = top - mColumnWidth;

                GalleryObject object = mImageObjects[index];

                float prevLeft = object.getLeft();
                float prevTop = object.getTop();

//                if ((bottom <= viewportTop && top >= viewportBottom) ||
//                        ((prevTop - prevColumnWidth) <= viewportTop && prevTop >= viewportBottom)) {

                ImageAnimatorCallback cb = new ImageAnimatorCallback(object);
                GLESAnimator animator = new GLESAnimator(cb);

                animator.setDuration(0L, GalleryConfig.ANIMATION_DURATION);

                GLESVector3 from = new GLESVector3(prevLeft, prevTop, prevColumnWidth);
                GLESVector3 to = new GLESVector3(left, top, mColumnWidth);
                animator.setValues(from, to);

                animator.setInterpolator(mInterpolator);

                mAnimators.add(animator);
//                } else {
//                    mImageObjects[index].setLeftTop(left, top);
//
//                    GLESVertexInfo vertexInfo = mImageObjects[index].getVertexInfo();
//                    FloatBuffer buffer = (FloatBuffer) vertexInfo.getBuffer(mTextureShader.getPositionAttribIndex());
//
//                    buffer.put(0, left);
//                    buffer.put(1, bottom);
//
//                    buffer.put(3, right);
//                    buffer.put(4, bottom);
//
//                    buffer.put(6, left);
//                    buffer.put(7, top);
//
//                    buffer.put(9, right);
//                    buffer.put(10, top);
//                }

                index++;
            }

            yOffset -= (dateLabelInfo.getNumOfRows() * (mColumnWidth + mSpacing));
        }
    }

    private int mFinishCount = 0;

    private void finishAnimation() {
        int size = mAnimators.size();

        mFinishCount++;
        if (mFinishCount >= size) {
            mSurfaceView.finishAnimation();
            mFinishCount = 0;
        }
    }

    private int mCancelCount = 0;

    private void cancelAnimation() {
        int size = mAnimators.size();

        mCancelCount++;
        if (mCancelCount >= size) {
            mCancelCount = 0;
        }
    }

    // onSurfaceCreated

    public void createObjects(GLESNode parentNode) {
        cancelLoading();

        mAnimators.clear();
        mWaitingTextures.clear();
        mTextureMappingInfos.clear();

        mImageObjects = new GalleryObject[mNumOfImages];

        for (int i = 0; i < mNumOfImages; i++) {
            mImageObjects[i] = new GalleryObject("image" + i);
            parentNode.addChild(mImageObjects[i]);
            mImageObjects[i].setGLState(mGLState);
            mImageObjects[i].setShader(mTextureShader);
            mImageObjects[i].setPosition(i);
            mImageObjects[i].setTexture(mDummyTexture);

            GLESVertexInfo vertexInfo = new GLESVertexInfo();
            vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
            vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);
            mImageObjects[i].setVertexInfo(vertexInfo, false, false);

            ImageInfo imageInfo = mBucketInfo.get(i);
            TextureMappingInfo textureMappingInfo = new TextureMappingInfo(mImageObjects[i], imageInfo);
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
        mActionBarHeight = gridInfo.getActionBarHeight();
        mBucketInfo = gridInfo.getBucketInfo();
        mNumOfImages = mGridInfo.getNumOfImages();
        mNumOfDateInfos = mGridInfo.getNumOfDateInfos();
        mDateLabelHeight = mGridInfo.getDateLabelHeight();

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
        return mImageObjects[index];
    }

    @Override
    public void onImageLoaded(int position, GalleryTexture texture) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(position);
        final GalleryObject object = textureMappingInfo.getObject();
        final ImageInfo imageInfo = (ImageInfo) textureMappingInfo.getGalleryInfo();

        final Bitmap bitmap = texture.getBitmapDrawable().getBitmap();
        imageInfo.setThumbnailWidth(bitmap.getWidth());
        imageInfo.setThumbnailHeight(bitmap.getHeight());

        GalleryUtils.calcTexCoord(imageInfo);

        GLESVertexInfo vertexInfo = object.getVertexInfo();
        GLESShader shader = object.getShader();
        vertexInfo.setBuffer(shader.getTexCoordAttribIndex(), imageInfo.getTexCoord(), 2);

        mWaitingTextures.add(texture);
        mSurfaceView.requestRender();
    }

    class ImageAnimatorCallback implements GLESAnimatorCallback {

        private final GalleryObject mObject;
        private int mFinishCount = 0;

        public ImageAnimatorCallback(GalleryObject object) {
            mObject = object;
        }

        @Override
        public void onAnimation(GLESVector3 current) {
            GLESVertexInfo vertexInfo = mObject.getVertexInfo();
            GLESShader shader = mObject.getShader();

            FloatBuffer position = (FloatBuffer) vertexInfo.getBuffer(shader.getPositionAttribIndex());

            float left = current.mX;
            float right = left + current.mZ;
            float top = current.mY;
            float bottom = top - current.mZ;

            position.put(0, left);
            position.put(1, bottom);

            position.put(3, right);
            position.put(4, bottom);

            position.put(6, left);
            position.put(7, top);

            position.put(9, right);
            position.put(10, top);

            mObject.setLeftTop(left, top);

            mGridInfo.setColumnWidthOnAnimation(current.mZ);
        }

        @Override
        public void onCancel() {
            cancelAnimation();
        }

        @Override
        public void onFinished() {
            finishAnimation();
        }
    }
}
