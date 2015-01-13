package com.gomdev.gallery;

import android.content.Context;
import android.graphics.Bitmap;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by gomdev on 15. 1. 13..
 */
public class ImageObjects implements ImageLoadingListener {
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

    private ArrayList<TextureMappingInfo> mTextureMappingInfos = new ArrayList<>();
    private Queue<GalleryTexture> mWaitingTextures = new ConcurrentLinkedQueue<>();

    public ImageObjects(Context context) {
        mContext = context;

        init();
    }

    private void init() {
        mImageManager = ImageManager.getInstance();

        mVisibilityPadding = GLESUtils.getPixelFromDpi(mContext, VISIBILITY_PADDING_DP);
    }


    // rendering

    public void updateTexture() {
        GalleryTexture texture = mWaitingTextures.poll();

        if (texture != null) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(texture.getPosition());
            final GalleryObject object = (GalleryObject) textureMappingInfo.getObject();

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

    public void onGridInfoChanged(GridInfo gridInfo) {
        mColumnWidth = gridInfo.getColumnWidth();
        mNumOfColumns = gridInfo.getNumOfColumns();

        changeImageObjectPosition();
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

    // onSurfaceCreated

    public void createObjects(GLESNode parentNode) {
        cancelLoading();

        mWaitingTextures.clear();
        mTextureMappingInfos.clear();

        mImageObjects = new GalleryObject[mNumOfImages];

        for (int i = 0; i < mNumOfImages; i++) {
            mImageObjects[i] = new GalleryObject("image" + i);
            parentNode.addChild(mImageObjects[i]);
            mImageObjects[i].setGLState(mGLState);
            mImageObjects[i].setShader(mTextureShader);

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
}
