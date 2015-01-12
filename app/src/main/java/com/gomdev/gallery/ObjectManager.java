package com.gomdev.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.util.Log;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESShaderConstant;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;

import java.lang.ref.WeakReference;
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
    private final float DATE_LABEL_TEXT_SIZE = 18f;
    private final float DATE_LABEL_TEXT_SHADOW_RADIUS = 1f;
    private final float DATE_LABEL_TEXT_SHADOW_DX = 0.5f;
    private final float DATE_LABEL_TEXT_SHADOW_DY = 0.5f;
    private final int DATE_LABEL_TEXT_SHOW_COLOR = 0x88444444;
    private final int DATE_LABEL_TEXT_COLOR = 0xFF222222;

    private final Context mContext;
    private GallerySurfaceView mSurfaceView;

    private ImageObject[] mImageObjects;
    private DateLabelObject[] mDateLabelObjects;
    private GLESShader mTextureShader;
    private GLESGLState mImageGLState;
    private GLESTexture mDummyTexture;

    private ImageManager mImageManager = null;
    private GridInfo mGridInfo = null;

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

        mImageGLState = new GLESGLState();
        mImageGLState.setCullFaceState(true);
        mImageGLState.setCullFace(GLES20.GL_BACK);
        mImageGLState.setDepthState(false);

        mVisibilityPadding = GLESUtils.getPixelFromDpi(mContext, VISIBILITY_PADDING_DP);
        Log.d(TAG, "init() mVisibilityPadding=" + mVisibilityPadding);

        mIsSurfaceChanged = false;
    }

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

    public void update() {
    }

    public void updateTexture() {
        GalleryTexture texture = mWaitingTextures.poll();

        if (texture != null) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(texture.getPosition());
            final ImageObject object = textureMappingInfo.getObject();

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

        for (int i = 0; i < mNumOfDateInfos; i++) {
            DateLabelObject object = mDateLabelObjects[i];

            float top = object.getTop();
            if ((top - mDateLabelHeight) < viewportTop && (top) > viewportBottom) {
                object.show();
            } else {
                object.hide();
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

    private void unmapTexture(int position, ImageObject object) {
        object.setTexture(mDummyTexture);

        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(position);
        GalleryTexture texture = textureMappingInfo.getTexture();
        mImageManager.cancelWork(texture);
        mWaitingTextures.remove(texture);

        textureMappingInfo.set(null);
    }

    public void onSurfaceCreated() {
        mIsSurfaceChanged = false;
    }

    public void onSurfaceChanged(int width, int height) {
        mWidth = width;
        mHeight = height;

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

            mImageObjects[i].setTexture(mDummyTexture);

            GLESVertexInfo vertexInfo = new GLESVertexInfo();
            vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
            vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);
            mImageObjects[i].setVertexInfo(vertexInfo, false, false);

            ImageInfo imageInfo = mBucketInfo.get(i);
            imageInfo.setImageObjectRef(new WeakReference<>(mImageObjects[i]));

            TextureMappingInfo textureMappingInfo = new TextureMappingInfo(mImageObjects[i], imageInfo);
            mTextureMappingInfos.add(textureMappingInfo);
        }

        mDateLabelObjects = new DateLabelObject[mNumOfDateInfos];
        for (int i = 0; i < mNumOfDateInfos; i++) {
            mDateLabelObjects[i] = new DateLabelObject("dataIndex" + i);
            parentNode.addChild(mDateLabelObjects[i]);
            mDateLabelObjects[i].setGLState(mImageGLState);
            mDateLabelObjects[i].setShader(mTextureShader);

            GLESVertexInfo vertexInfo = new GLESVertexInfo();
            vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
            vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);
            mDateLabelObjects[i].setVertexInfo(vertexInfo, false, false);


        }
    }

    public int getSelectedIndex(float x, float y, float translateY) {
        float yPos = mHeight * 0.5f - (y + translateY);

        int selectedDateLabelIndex = getDateLabelIndexFromYPos(yPos);
        int index = getImageIndexFromYPos(x, yPos, selectedDateLabelIndex);

        DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(selectedDateLabelIndex);
        int lastImageIndex = dateLabelInfo.getLastImagePosition();

        Log.d(TAG, "getSelectedIndex() DateLabelIndex=" + selectedDateLabelIndex + " lastIndex=" + lastImageIndex + " index=" + index);

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
        float imageStartOffset = mDateLabelObjects[selectedDateLabelIndex].getTop() - mDateLabelHeight - mSpacing;
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
            if (yPos > mDateLabelObjects[i].getTop()) {
                selectedDateLabelIndex = i - 1;
                break;
            }
            selectedDateLabelIndex = i; // for last DateLabel
        }

        return selectedDateLabelIndex;
    }

    public void setupObjects(GLESCamera camera) {
        setupImageObjects(camera);
        setupDateLabelObjects(camera);
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

    private void setupDateLabelObjects(GLESCamera camera) {
        float yOffset = mHeight * 0.5f - mActionBarHeight;
        for (int i = 0; i < mNumOfDateInfos; i++) {
            mDateLabelObjects[i].setCamera(camera);

            float left = mSpacing - mWidth * 0.5f;
            float top = yOffset;
            float width = (mColumnWidth + mSpacing) * mNumOfColumns - mSpacing;
            float height = mDateLabelHeight;

            mDateLabelObjects[i].setLeftTop(left, top);

            float[] vertex = GLESUtils.makePositionCoord(left, top, width, height);

            GLESVertexInfo vertexInfo = mDateLabelObjects[i].getVertexInfo();
            vertexInfo.setBuffer(mTextureShader.getPositionAttribIndex(), vertex, 3);

            float[] texCoord = GLESUtils.makeTexCoord(0f, 0f, 1f, 1f);
            vertexInfo.setBuffer(mTextureShader.getTexCoordAttribIndex(), texCoord, 2);

            setDataLabelTexture(i);

            yOffset -= (mDateLabelHeight + mSpacing);

            DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(i);
            yOffset -= (dateLabelInfo.getNumOfRows() * (mColumnWidth + mSpacing));
        }
    }

    private void setDataLabelTexture(int index) {
        DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(index);

        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setShadowLayer(
                GLESUtils.getPixelFromDpi(mContext, DATE_LABEL_TEXT_SHADOW_RADIUS),
                GLESUtils.getPixelFromDpi(mContext, DATE_LABEL_TEXT_SHADOW_DX),
                GLESUtils.getPixelFromDpi(mContext, DATE_LABEL_TEXT_SHADOW_DY),
                DATE_LABEL_TEXT_SHOW_COLOR);
        textPaint.setTextSize(GLESUtils.getPixelFromDpi(mContext, DATE_LABEL_TEXT_SIZE));
        textPaint.setARGB(0xFF, 0x00, 0x00, 0x00);
        textPaint.setColor(DATE_LABEL_TEXT_COLOR);

        int ascent = (int) Math.ceil(-textPaint.ascent());
        int descent = (int) Math.ceil(textPaint.descent());

        int textHeight = ascent + descent;

        int width = mWidth - mSpacing * 2;
        int height = mDateLabelHeight;

        int x = mContext.getResources().getDimensionPixelSize(R.dimen.dateindex_padding);
        int y = (height - textHeight) / 2 + ascent;

        Bitmap bitmap = GLESUtils.drawTextToBitmap(mContext,
                x, y,
                width, height,
                dateLabelInfo.getDate(), textPaint);

        GLESTexture texture = new GLESTexture.Builder(GLES20.GL_TEXTURE_2D, width, height)
                .load(bitmap);

        mDateLabelObjects[index].setTexture(texture);
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

        if (mIsSurfaceChanged == false) {
            return;
        }

        changeImageObjectPosition();
        changeDateLabelObjectPosition();
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

    private void changeDateLabelObjectPosition() {
        float yOffset = mHeight * 0.5f - mActionBarHeight;
        for (int i = 0; i < mNumOfDateInfos; i++) {

            float left = mSpacing - mWidth * 0.5f;
            float top = yOffset;
            float bottom = yOffset - mDateLabelHeight;

            mDateLabelObjects[i].setLeftTop(left, top);

            GLESVertexInfo vertexInfo = mDateLabelObjects[i].getVertexInfo();
            FloatBuffer buffer = (FloatBuffer) vertexInfo.getBuffer(mTextureShader.getPositionAttribIndex());

            buffer.put(1, bottom);
            buffer.put(4, bottom);
            buffer.put(7, top);
            buffer.put(10, top);

            yOffset -= (mDateLabelHeight + mSpacing);

            DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(i);
            yOffset -= (dateLabelInfo.getNumOfRows() * (mColumnWidth + mSpacing));
        }
    }

    @Override
    public void onImageLoaded(int position, GalleryTexture texture) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(position);
        final ImageObject object = textureMappingInfo.getObject();
        final ImageInfo imageInfo = textureMappingInfo.getImageInfo();

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
