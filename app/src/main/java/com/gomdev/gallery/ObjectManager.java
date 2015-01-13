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
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by gomdev on 14. 12. 31..
 */
public class ObjectManager implements GridInfoChangeListener, ImageLoadingListener {

    static final String CLASS = "ImageObjects";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final float VISIBILITY_PADDING_DP = 60f;    // dp

    private final Context mContext;
    private GallerySurfaceView mSurfaceView;

    private ImageObject[] mImageObjects;

    private GLESShader mTextureShader;
    private GLESGLState mImageGLState;
    private GLESTexture mDummyImageTexture;
    private GLESTexture mDummyDateTexture;

    private ImageManager mImageManager = null;
    private GridInfo mGridInfo = null;
    private DateLabelManager mDateLabelManager = null;

    private int mSpacing;
    private int mNumOfColumns;
    private int mNumOfImages;
    private int mNumOfDateInfos;
    private int mColumnWidth;
    private BucketInfo mBucketInfo;
    private int mActionBarHeight;
    private int mDateLabelHeight;

    private int mWidth;
    private int mHeight;

    private float mVisibilityPadding = 0f;

    private boolean mIsSurfaceChanged = false;

    private ArrayList<TextureMappingInfo> mTextureMappingInfos = new ArrayList<>();
    private Queue<GalleryTexture> mWaitingTextures = new ConcurrentLinkedQueue<>();

    public ObjectManager(Context context) {
        mContext = context;

        init();
    }

    private void init() {
        mImageManager = ImageManager.getInstance();
        mDateLabelManager = new DateLabelManager(mContext);

        mImageGLState = new GLESGLState();
        mImageGLState.setCullFaceState(true);
        mImageGLState.setCullFace(GLES20.GL_BACK);
        mImageGLState.setDepthState(false);

        mDateLabelManager.setGLState(mImageGLState);

        mVisibilityPadding = GLESUtils.getPixelFromDpi(mContext, VISIBILITY_PADDING_DP);

        mIsSurfaceChanged = false;
    }

    public void setSurfaceView(GallerySurfaceView surfaceView) {
        mSurfaceView = surfaceView;

        mDateLabelManager.setSurfaceView(surfaceView);
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

        mDateLabelManager.setGridInfo(mGridInfo);
    }

    public void update() {
    }

    public void updateTexture() {
        mDateLabelManager.updateTexture();

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

    public void checkVisibility(float translateY) {
        mDateLabelManager.checkVisibility(translateY);

        float viewportTop = mHeight * 0.5f - translateY;
        float viewportBottom = viewportTop - mHeight;

        for (int i = 0; i < mNumOfImages; i++) {
            ImageObject object = mImageObjects[i];

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

    private void unmapTexture(int position, ImageObject object) {
        object.setTexture(mDummyImageTexture);

        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(position);
        GalleryTexture texture = textureMappingInfo.getTexture();
        BitmapWorker.cancelWork(texture);
        mWaitingTextures.remove(texture);

        textureMappingInfo.set(null);
    }

    public void onSurfaceCreated() {
        mIsSurfaceChanged = false;
    }

    public void onSurfaceChanged(int width, int height) {
        mWidth = width;
        mHeight = height;

        mDateLabelManager.onSurfaceChanged(width, height);

        mIsSurfaceChanged = true;
    }

    public void createObjects(GLESNode parentNode) {
        cancelLoading();

        mWaitingTextures.clear();
        mTextureMappingInfos.clear();

        mImageObjects = new ImageObject[mNumOfImages];

        for (int i = 0; i < mNumOfImages; i++) {
            mImageObjects[i] = new ImageObject("image" + i);
            parentNode.addChild(mImageObjects[i]);
            mImageObjects[i].setGLState(mImageGLState);
            mImageObjects[i].setShader(mTextureShader);

            mImageObjects[i].setTexture(mDummyImageTexture);

            GLESVertexInfo vertexInfo = new GLESVertexInfo();
            vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
            vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);
            mImageObjects[i].setVertexInfo(vertexInfo, false, false);

            ImageInfo imageInfo = mBucketInfo.get(i);
            TextureMappingInfo textureMappingInfo = new TextureMappingInfo(mImageObjects[i], imageInfo);
            mTextureMappingInfos.add(textureMappingInfo);
        }

        mDateLabelManager.createObjects(parentNode);
    }

    public int getSelectedIndex(float x, float y, float translateY) {
        float yPos = mHeight * 0.5f - (y + translateY);

        int selectedDateLabelIndex = getDateLabelIndexFromYPos(yPos);
        int index = getImageIndexFromYPos(x, yPos, selectedDateLabelIndex);

        DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(selectedDateLabelIndex);
        int lastImageIndex = dateLabelInfo.getLastImagePosition();

        if (index > lastImageIndex) {
            return -1;
        }

        return index;
    }

    public int getNearestIndex(float x, float y, float translateY) {
        float yPos = mHeight * 0.5f - (y + translateY);

        int selectedDateLabelIndex = getDateLabelIndexFromYPos(yPos);
        int index = getImageIndexFromYPos(x, yPos, selectedDateLabelIndex);

        DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(selectedDateLabelIndex);
        int lastImageIndex = dateLabelInfo.getLastImagePosition();

        if (index > lastImageIndex) {
            return (index - 1);
        }

        return index;
    }

    private int getImageIndexFromYPos(float x, float yPos, int selectedDateLabelIndex) {
        DateLabelObject dateLabelObject = mDateLabelManager.getObject(selectedDateLabelIndex);
        float imageStartOffset = dateLabelObject.getTop() - mDateLabelHeight - mSpacing;
        float yDistFromDateLabel = imageStartOffset - yPos;

        int row = (int) (yDistFromDateLabel / (mColumnWidth + mSpacing));
        int column = (int) (x / (mColumnWidth + mSpacing));

        DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(selectedDateLabelIndex);
        int firstImageIndex = dateLabelInfo.getFirstImagePosition();

        return mNumOfColumns * row + column + firstImageIndex;
    }

    private int getDateLabelIndexFromYPos(float yPos) {
        int selectedDateLabelIndex = 0;
        for (int i = 0; i < mNumOfDateInfos; i++) {
            DateLabelObject dateLabelObject = mDateLabelManager.getObject(i);
            if (yPos > dateLabelObject.getTop()) {
                selectedDateLabelIndex = i - 1;
                break;
            }
            selectedDateLabelIndex = i; // for last DateLabel
        }

        return selectedDateLabelIndex;
    }

    public void setupObjects(GLESCamera camera) {
        setupImageObjects(camera);
        mDateLabelManager.setupDateLabelObjects(camera);
    }

    private void setupImageObjects(GLESCamera camera) {
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

    public void setDummyImageTexture(GLESTexture dummyTexture) {
        mDummyImageTexture = dummyTexture;
    }

    public void setDummyDateLabelTexture(GLESTexture dummyTexture) {
        mDummyDateTexture = dummyTexture;
        mDateLabelManager.setDummyTexture(mDummyDateTexture);
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

        mDateLabelManager.setShader(mTextureShader);

        return true;
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

    @Override
    public void onGridInfoChanged() {
        mColumnWidth = mGridInfo.getColumnWidth();
        mNumOfColumns = mGridInfo.getNumOfColumns();

        if (mIsSurfaceChanged == false) {
            return;
        }

        mDateLabelManager.onGridInfoChanged(mGridInfo);

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

    @Override
    public void onImageLoaded(int position, GalleryTexture texture) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(position);
        final ImageObject object = (ImageObject) textureMappingInfo.getObject();
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
