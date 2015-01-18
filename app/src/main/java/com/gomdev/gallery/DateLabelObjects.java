package com.gomdev.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;

import com.gomdev.gles.GLESCamera;
import com.gomdev.gles.GLESGLState;
import com.gomdev.gles.GLESNode;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESUtils;
import com.gomdev.gles.GLESVertexInfo;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by gomdev on 15. 1. 12..
 */

public class DateLabelObjects implements ImageLoadingListener, GridInfoChangeListener {
    static final String CLASS = "DateLabelObjects";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final Context mContext;
    private final ReusableBitmaps mReusableBitmaps;

    private GallerySurfaceView mSurfaceView = null;

    private GalleryObject[] mObjects;

    private GLESGLState mGLState = null;
    private GLESShader mTextureShader = null;

    private GridInfo mGridInfo = null;
    private BucketInfo mBucketInfo = null;
    private int mNumOfDateInfos = 0;
    private int mDateLabelHeight = 0;
    private int mSpacing = 0;
    private int mColumnWidth = 0;
    private int mActionBarHeight = 0;

    private Bitmap mLoadingBitmap = null;
    private GLESTexture mDummyTexture = null;

    private int mWidth = 0;
    private int mHeight = 0;

    private List<TextureMappingInfo> mTextureMappingInfos = new ArrayList<>();
    private Queue<GalleryTexture> mWaitingTextures = new ConcurrentLinkedQueue<>();


    public DateLabelObjects(Context context) {
        mContext = context;
        mReusableBitmaps = ReusableBitmaps.getInstance();

        mTextureMappingInfos.clear();
    }

    public void clear() {
        mTextureMappingInfos.clear();
        mWaitingTextures.clear();
    }

    // Rendering

    // this function should be called on GLThread
    public void updateTexture() {
        GalleryTexture texture = mWaitingTextures.poll();

        if (texture != null) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(texture.getPosition());
            final GalleryObject object = (GalleryObject) textureMappingInfo.getObject();

            final Bitmap bitmap = texture.getBitmapDrawable().getBitmap();
            texture.load(bitmap);

            mReusableBitmaps.addBitmapToResuableSet(bitmap);

            object.setTexture(texture.getTexture());
        }

        if (mWaitingTextures.size() > 0) {
            mSurfaceView.requestRender();
        }
    }

    public void checkVisibility(float translateY) {
        float viewportTop = mHeight * 0.5f - translateY;
        float viewportBottom = viewportTop - mHeight;

        for (int i = 0; i < mNumOfDateInfos; i++) {
            GalleryObject object = mObjects[i];

            float top = object.getTop();
            if ((top - mDateLabelHeight) < viewportTop && (top) > viewportBottom) {
                object.show();
                mapTexture(i);
            } else {
                object.hide();
                unmapTexture(i, mObjects[i]);
            }
        }
    }

    public void mapTexture(int position) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(position);

        DateLabelInfo dateLabelInfo = (DateLabelInfo) textureMappingInfo.getGalleryInfo();

        GalleryTexture texture = textureMappingInfo.getTexture();
        if (texture == null) {
            texture = new GalleryTexture(mWidth, mHeight);
            texture.setPosition(position);
            texture.setImageLoadingListener(this);
        }

        if ((texture != null && texture.isTextureLoadingNeeded() == true)) {
            makeDateLabel(dateLabelInfo, texture);
            textureMappingInfo.set(texture);
            mSurfaceView.requestRender();
        }
    }

    public void unmapTexture(int position, GalleryObject object) {
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

        mLoadingBitmap = GLESUtils.makeBitmap(16, 16, Bitmap.Config.ARGB_8888, Color.WHITE);
    }

    public void setupDateLabelObjects(GLESCamera camera) {
        float yOffset = mHeight * 0.5f - mActionBarHeight;
        for (int i = 0; i < mNumOfDateInfos; i++) {
            mObjects[i].setCamera(camera);

            float left = mSpacing - mWidth * 0.5f;
            float top = yOffset;
            float width = mWidth - mSpacing * 2f;
            float height = mDateLabelHeight;

            mObjects[i].setLeftTop(left, top);

            float[] vertex = GLESUtils.makePositionCoord(left, top, width, height);

            GLESVertexInfo vertexInfo = mObjects[i].getVertexInfo();
            vertexInfo.setBuffer(mTextureShader.getPositionAttribIndex(), vertex, 3);

            float[] texCoord = GLESUtils.makeTexCoord(0f, 0f, 1f, 1f);
            vertexInfo.setBuffer(mTextureShader.getTexCoordAttribIndex(), texCoord, 2);

            yOffset -= (mDateLabelHeight + mSpacing);

            DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(i);
            yOffset -= (dateLabelInfo.getNumOfRows() * (mColumnWidth + mSpacing));
        }
    }


    @Override
    public void onGridInfoChanged() {
        mColumnWidth = mGridInfo.getColumnWidth();

        changeDateLabelObjectPosition();
    }

    private void changeDateLabelObjectPosition() {
        float yOffset = mHeight * 0.5f - mActionBarHeight;
        for (int i = 0; i < mNumOfDateInfos; i++) {

            float left = mSpacing - mWidth * 0.5f;
            float right = -left;
            float top = yOffset;
            float bottom = yOffset - mDateLabelHeight;

            mObjects[i].setLeftTop(left, top);

            GLESVertexInfo vertexInfo = mObjects[i].getVertexInfo();
            FloatBuffer buffer = (FloatBuffer) vertexInfo.getBuffer(mTextureShader.getPositionAttribIndex());

            buffer.put(0, left);
            buffer.put(1, bottom);

            buffer.put(3, right);
            buffer.put(4, bottom);

            buffer.put(6, left);
            buffer.put(7, top);

            buffer.put(9, right);
            buffer.put(10, top);

            yOffset -= (mDateLabelHeight + mSpacing);

            DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(i);
            yOffset -= (dateLabelInfo.getNumOfRows() * (mColumnWidth + mSpacing));
        }
    }

    // onSurfaceCreated

    public void createObjects(GLESNode parentNode) {
        clear();
        mObjects = new GalleryObject[mNumOfDateInfos];
        for (int i = 0; i < mNumOfDateInfos; i++) {
            mObjects[i] = new GalleryObject("dataIndex" + i);
            parentNode.addChild(mObjects[i]);
            mObjects[i].setGLState(mGLState);
            mObjects[i].setShader(mTextureShader);
            mObjects[i].setTexture(mDummyTexture);

            GLESVertexInfo vertexInfo = new GLESVertexInfo();
            vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
            vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);
            mObjects[i].setVertexInfo(vertexInfo, false, false);

            DateLabelInfo dateLabelInfo = mBucketInfo.getDateInfo(i);
            TextureMappingInfo textureMappingInfo = new TextureMappingInfo(mObjects[i], dateLabelInfo);
            addTextureMapingInfo(textureMappingInfo);
        }
    }

    public void addTextureMapingInfo(TextureMappingInfo info) {
        mTextureMappingInfos.add(info);
    }

    public GalleryObject getObject(int i) {
        return mObjects[i];
    }

    // initialization

    public void setSurfaceView(GallerySurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    public void setGLState(GLESGLState state) {
        mGLState = state;
    }

    public void setShader(GLESShader shader) {
        mTextureShader = shader;
    }

    public void setGridInfo(GridInfo gridInfo) {
        mGridInfo = gridInfo;

        mBucketInfo = gridInfo.getBucketInfo();
        mNumOfDateInfos = gridInfo.getNumOfDateInfos();
        mDateLabelHeight = gridInfo.getDateLabelHeight();
        mSpacing = gridInfo.getSpacing();
        mColumnWidth = gridInfo.getColumnWidth();
        mActionBarHeight = gridInfo.getActionBarHeight();

        mGridInfo.addListener(this);
    }

    public void setDummyTexture(GLESTexture texture) {
        mDummyTexture = texture;
    }

    @Override
    public void onImageLoaded(int position, GalleryTexture texture) {
        mWaitingTextures.add(texture);

        mSurfaceView.requestRender();
    }

    public void makeDateLabel(DateLabelInfo dateLableInfo, GalleryTexture texture) {
        if (BitmapWorker.cancelPotentialWork(dateLableInfo, texture) && texture != null) {
            final DateLabelTask task = new DateLabelTask(mContext, mGridInfo, texture);
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(mContext.getResources(),
                            mLoadingBitmap, task);
            GalleryObject object = mTextureMappingInfos.get(dateLableInfo.getPosition()).getObject();
            object.setTexture(mDummyTexture);
            texture.setBitmapDrawable(asyncDrawable);
            task.execute(dateLableInfo);
        }
    }

    class DateLabelTask extends BitmapWorker.BitmapWorkerTask<GalleryTexture> {
        static final String CLASS = "DateLabelTask";
        static final String TAG = GalleryConfig.TAG + "_" + CLASS;
        static final boolean DEBUG = GalleryConfig.DEBUG;

        private final float DATE_LABEL_TEXT_SIZE = 18f;
        private final float DATE_LABEL_TEXT_SHADOW_RADIUS = 1f;
        private final float DATE_LABEL_TEXT_SHADOW_DX = 0.5f;
        private final float DATE_LABEL_TEXT_SHADOW_DY = 0.5f;
        private final int DATE_LABEL_TEXT_SHADOW_COLOR = 0x88444444;
        private final int DATE_LABEL_TEXT_COLOR = 0xFF222222;

        private final Context mContext;

        private int mSpacing = 0;
        private int mDateLabelHeight = 0;

        private DateLabelInfo mDateLabelInfo = null;

        DateLabelTask(Context context, GridInfo gridInfo, GalleryTexture texture) {
            super(texture);
            mContext = context;

            mSpacing = gridInfo.getSpacing();
            mDateLabelHeight = gridInfo.getDateLabelHeight();
        }

        @Override
        protected BitmapDrawable doInBackground(GalleryInfo... params) {
            mDateLabelInfo = (DateLabelInfo) params[0];

            Paint textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setShadowLayer(
                    GLESUtils.getPixelFromDpi(mContext, DATE_LABEL_TEXT_SHADOW_RADIUS),
                    GLESUtils.getPixelFromDpi(mContext, DATE_LABEL_TEXT_SHADOW_DX),
                    GLESUtils.getPixelFromDpi(mContext, DATE_LABEL_TEXT_SHADOW_DY),
                    DATE_LABEL_TEXT_SHADOW_COLOR);
            textPaint.setTextSize(GLESUtils.getPixelFromDpi(mContext, DATE_LABEL_TEXT_SIZE));
            textPaint.setARGB(0xFF, 0x00, 0x00, 0x00);
            textPaint.setColor(DATE_LABEL_TEXT_COLOR);

            int ascent = (int) Math.ceil(-textPaint.ascent());
            int descent = (int) Math.ceil(textPaint.descent());

            int textHeight = ascent + descent;

            int width = (int) (GLESUtils.getWidthPixels(mContext) - mSpacing * 2);
            int height = mDateLabelHeight;

            int x = mContext.getResources().getDimensionPixelSize(R.dimen.dateindex_padding);
            int y = (height - textHeight) / 2 + ascent;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            options.inSampleSize = 1;
            options.outHeight = height;
            options.outWidth = width;
            Bitmap bitmap = mReusableBitmaps.getBitmapFromReusableSet(options);
            if (bitmap == null) {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }

            bitmap = GLESUtils.drawTextToBitmap(x, y,
                    mDateLabelInfo.getDate(), textPaint, bitmap);

            BitmapDrawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);

            return drawable;
        }
    }
}
