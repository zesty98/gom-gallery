package com.gomdev.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESTransform;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by gomdev on 14. 12. 31..
 */
public class ImageObjects implements GridInfoChangeListener, ImageLoadingListener {

    static final String CLASS = "ImageObjects";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;
    private GallerySurfaceView mSurfaceView;

    private GalleryObject[] mObjects;
    private GLESShader mTextureShader;
    private GLESGLState mImageGLState;
    private GLESTexture mDummyTexture;

    private ImageManager mImageManager = null;
    private GridInfo mGridInfo = null;

    private int mSpacing;
    private int mNumOfColumns;
    private int mNumOfImages;
    private int mColumnWidth;
    private int mNumOfRows;
    private int mNumOfRowsInScreen;
    private BucketInfo mBucketInfo;
    private int mActionBarHeight;

    private int mWidth;
    private int mHeight;

    private ArrayList<TextureMappingInfo> mTextureMappingInfos = new ArrayList<>();
    private Queue<GalleryTexture> mWaitingTextures = new ConcurrentLinkedQueue<>();

    public ImageObjects(Context context) {
        mContext = context;

        init();
    }

    private void init() {
        mImageManager = ImageManager.getInstance();

        mImageGLState = new GLESGLState();
        mImageGLState.setCullFaceState(true);
        mImageGLState.setCullFace(GLES20.GL_BACK);
        mImageGLState.setDepthState(false);
    }

    public void setSurfaceView(GallerySurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mSpacing = gridInfo.getSpacing();
        mNumOfColumns = gridInfo.getNumOfColumns();
        mNumOfRows = gridInfo.getNumOfRows();
        mColumnWidth = gridInfo.getColumnWidth();
        mActionBarHeight = gridInfo.getActionBarHeight();
        mBucketInfo = gridInfo.getBucketInfo();
    }

    public void update() {
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
        int visibleFirstRow = (int) Math.floor((double) (translateY - mActionBarHeight) / (mColumnWidth + mSpacing)) - 1;
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

    private void unmapTexture(int position, GalleryObject object) {
        object.setTexture(mDummyTexture);

        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(position);
        GalleryTexture texture = textureMappingInfo.getTexture();
        mImageManager.cancelWork(texture);
        mWaitingTextures.remove(texture);

        textureMappingInfo.set(null);
    }

    public void onSurfaceChanged(int width, int height) {
        mWidth = width;
        mHeight = height;

        cancelLoading();

        mWaitingTextures.clear();
        mTextureMappingInfos.clear();

        mNumOfImages = mGridInfo.getNumOfImages();
    }

    public void createObjects(GLESCamera camera, GLESNode parentNode) {
        mObjects = new GalleryObject[mNumOfImages];

        for (int i = 0; i < mNumOfImages; i++) {
            mObjects[i] = new GalleryObject("image" + i);
            parentNode.addChild(mObjects[i]);
            mObjects[i].setGLState(mImageGLState);
            mObjects[i].setShader(mTextureShader);

            mObjects[i].setCamera(camera);

            GLESVertexInfo vertexInfo = GalleryUtils.createImageVertexInfo(mTextureShader, mColumnWidth, mColumnWidth);
            mObjects[i].setVertexInfo(vertexInfo, false, false);

            mObjects[i].setTexture(mDummyTexture);

            ImageInfo imageInfo = mBucketInfo.get(i);
            TextureMappingInfo textureMappingInfo = new TextureMappingInfo(mObjects[i], imageInfo);
            mTextureMappingInfos.add(textureMappingInfo);
        }
    }

    public void setDummyTexture(GLESTexture dummyTexture) {
        mDummyTexture = dummyTexture;
    }

    public boolean createShader(int vsResID, int fsResID) {
        mTextureShader = new GLESShader(mContext);

        String vsSource = GLESUtils.getStringFromReosurce(mContext, vsResID);
        String fsSource = GLESUtils.getStringFromReosurce(mContext, fsResID);

        mTextureShader.setShaderSource(vsSource, fsSource);
        if (mTextureShader.load() == false) {
            return false;
        }

        String attribName = GLESShaderConstant.ATTRIB_POSITION;
        mTextureShader.setPositionAttribIndex(attribName);

        attribName = GLESShaderConstant.ATTRIB_TEXCOORD;
        mTextureShader.setTexCoordAttribIndex(attribName);

        return true;
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

    @Override
    public void onGridInfoChanged() {
        mColumnWidth = mGridInfo.getColumnWidth();
        mNumOfColumns = mGridInfo.getNumOfColumns();
        mNumOfRows = mGridInfo.getNumOfRows();
        mNumOfRowsInScreen = mGridInfo.getNumOfRowsInScreen();

        float halfScaledWidth = mColumnWidth * 0.5f;
        for (int i = 0; i < mNumOfImages; i++) {
            GLESVertexInfo vertexInfo = mObjects[i].getVertexInfo();
            FloatBuffer buffer = (FloatBuffer) vertexInfo.getBuffer(mTextureShader.getPositionAttribIndex());

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
    }

    @Override
    public void onImageLoaded(int position, GalleryTexture texture) {
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
}
