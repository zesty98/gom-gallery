package com.gomdev.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLES20;
import android.util.Log;
import android.util.SparseArray;

import com.gomdev.gallery.GalleryTexture.TextureState;
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
 * Created by gomdev on 15. 1. 12..
 */

class GalleryObjects implements ImageLoadingListener, GridInfoChangeListener {
    static final String CLASS = "GalleryObjects";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private static GLESTexture sDummyTexture = null;

    private final Context mContext;
    private final GridInfo mGridInfo;
    private final BucketInfo mBucketInfo;

    private final ConcurrentLinkedQueue<Bitmap> mReusableBitmaps = new ConcurrentLinkedQueue<>();

    private GallerySurfaceView mSurfaceView = null;
    private AlbumViewManager mAlbumViewManager = null;

    private List<DateLabelObject> mDateLabelObjects = new ArrayList<>();

    private GLESGLState mDateLabelGLState = null;
    private GLESGLState mImageGLState = null;
    private GLESShader mShader = null;
    private GLESNode mParentNode = null;

    private int mNumOfDateInfos = 0;
    private int mDateLabelHeight = 0;
    private int mSpacing = 0;
    private int mColumnWidth = 0;
    private int mPrevColumnWidth = 0;
    private int mNumOfColumns = 0;
    private int mPrevNumOfColumns = 0;
    private int mSystemBarHeight = 0;

    private Bitmap mLoadingBitmap = null;

    private int mWidth = 0;
    private int mHeight = 0;
    private float mHalfWidth = 0f;
    private float mHalfHeight = 0f;

    private List<TextureMappingInfo> mTextureMappingInfos = new ArrayList<>();
    private Queue<GalleryTexture> mWaitingTextures = new ConcurrentLinkedQueue<>();
    private SparseArray<DateLabelObject> mInvisibleObjects = new SparseArray<>();
    private ArrayList<DateLabelObject> mAnimationObjects = new ArrayList<>();

    private GLESAnimator mAnimator = null;
    private float mAlpha = 1.0f;

    GalleryObjects(Context context, GridInfo gridInfo, BucketInfo bucketInfo) {
        if (DEBUG) {
            Log.d(TAG, "GalleryObjects()");
        }

        mContext = context;
        mGridInfo = gridInfo;
        mBucketInfo = bucketInfo;

        setGridInfo(gridInfo);
    }

    private void setGridInfo(GridInfo gridInfo) {
        if (DEBUG) {
            Log.d(TAG, "setGridInfo()");
        }

        mNumOfDateInfos = gridInfo.getNumOfDateInfos();
        mDateLabelHeight = gridInfo.getDateLabelHeight();
        mSpacing = gridInfo.getSpacing();
        mColumnWidth = gridInfo.getColumnWidth();
        mNumOfColumns = gridInfo.getNumOfColumns();
        mSystemBarHeight = gridInfo.getSystemBarHeight();

        mGridInfo.addListener(this);
    }

    void clear() {
        if (DEBUG) {
            Log.d(TAG, "clear()");
        }

        mWaitingTextures.clear();
        mAnimationObjects.clear();
    }

    // Rendering

    void update() {
        checkVisibility();

        if (mAnimator != null && mAnimator.doAnimation() == true) {
            mSurfaceView.requestRender();
        }

        int size = mAnimationObjects.size();
        for (int i = 0; i < size; i++) {
            DateLabelObject object = mAnimationObjects.get(i);
            ImageObjects imageObjects = object.getImageObjects();
            imageObjects.update();
        }
    }

    // this function should be called on GLThread
    void updateTexture(long currentTime) {
        GalleryTexture texture = mWaitingTextures.poll();

        if (texture != null) {
            TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(texture.getIndex());
            final GalleryObject object = textureMappingInfo.getObject();

            final Bitmap bitmap = texture.getBitmapDrawable().getBitmap();
            texture.load(bitmap);

            mReusableBitmaps.add(bitmap);

            object.setTexture(texture);
        }

        if (mWaitingTextures.size() > 0) {
            mSurfaceView.requestRender();
        }

        int size = mDateLabelObjects.size();
        for (int i = 0; i < size; i++) {
            DateLabelObject dateLabelObject = mDateLabelObjects.get(i);

            ImageObjects imageObjects = dateLabelObject.getImageObjects();
            imageObjects.updateTexture(currentTime);
        }
    }

    void checkVisibility() {
        float translateY = mGridInfo.getTranslateY();
        float viewportTop = mHalfHeight - translateY;
        float viewportBottom = viewportTop - mHeight;

        int size = mDateLabelObjects.size();
        for (int i = 0; i < size; i++) {
            DateLabelObject object = mDateLabelObjects.get(i);
            GalleryNode parentNode = object.getParentNode();

            if (mInvisibleObjects.get(i) != null) {
                parentNode.setVisibility(false);

                if (parentNode.isVisibilityChanged() == true) {
                    unmapTexture(i, object);
                }
                continue;
            }

            ImageObjects imageObjects = object.getImageObjects();

            float top = object.getTop();
            float bottom = 0f;
            if (i + 1 < size) {
                bottom = mDateLabelObjects.get(i + 1).getTop() - mSpacing;
            } else {
                bottom = mHalfHeight - mGridInfo.getScrollableHeight();
            }

            if (bottom < viewportTop && top > viewportBottom) {
                parentNode.setVisibility(true);

                mapTexture(i);

                imageObjects.checkVisibility(true);
            } else {
                parentNode.setVisibility(false);

                if (parentNode.isVisibilityChanged() == true) {
                    unmapTexture(i, object);
                }

                imageObjects.checkVisibility(false);
            }
        }
    }

    void mapTexture(int index) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(index);
        DateLabelInfo dateLabelInfo = (DateLabelInfo) textureMappingInfo.getGalleryInfo();

        GalleryTexture texture = textureMappingInfo.getTexture();

        if (texture == null) {
            texture = new GalleryTexture(mWidth, mHeight);
            texture.setIndex(index);
            texture.setImageLoadingListener(this);
            texture.setState(TextureState.NONE);
        } else {
            TextureState textureState = texture.getState();
            if (textureState != TextureState.NONE && textureState != TextureState.CANCELED) {
                return;
            }
        }

        if ((texture != null && texture.isTextureLoadingNeeded() == true)) {
            makeDateLabel(dateLabelInfo, texture);
            textureMappingInfo.setTexture(texture);
            mSurfaceView.requestRender();
        }
    }

    void unmapTexture(int index, GalleryObject object) {
        if (DEBUG) {
            if (sDummyTexture == null) {
                Log.d(TAG, "unmapTexture() sDummyTexture is null");
            } else {
                int textureID = sDummyTexture.getTextureID();
                if (GLES20.glIsTexture(textureID) == false) {
                    Log.d(TAG, "unmapTexture() sDummyTexture is invalid");
                }
            }
        }

        object.setDummyTexture(sDummyTexture);

        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(index);
        GalleryTexture texture = textureMappingInfo.getTexture();

        if (texture == null) {
            return;
        }

        TextureState textureState = texture.getState();

        switch (textureState) {
            case NONE:
                break;
            case DECODING:
                BitmapWorker.cancelWork(texture);
                break;
            case QUEUING:
                mWaitingTextures.remove(texture);
                break;
            case CANCELED:
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

        mHalfWidth = width * 0.5f;
        mHalfHeight = height * 0.5f;

        mLoadingBitmap = GLESUtils.makeBitmap(16, 16, Bitmap.Config.ARGB_8888, Color.WHITE);

        int size = mDateLabelObjects.size();
        for (int i = 0; i < size; i++) {
            DateLabelObject object = mDateLabelObjects.get(i);

            ImageObjects imageObjects = object.getImageObjects();
            imageObjects.onSurfaceChanged(width, height);
        }

        if (sDummyTexture == null) {
            sDummyTexture = GalleryUtils.createDummyTexture(Color.WHITE);
        }
    }

    void setupObjects(GLESCamera camera) {
        if (DEBUG) {
            Log.d(TAG, "setupObjects() mSystemBarHeight=" + mSystemBarHeight);
        }

        float yOffset = mHeight * 0.5f - mSystemBarHeight;

        int size = mDateLabelObjects.size();
        for (int i = 0; i < size; i++) {
            DateLabelObject object = mDateLabelObjects.get(i);

            object.setCamera(camera);

            float left = mSpacing - mHalfWidth;
            float top = yOffset;
            float width = mWidth - mSpacing * 2f;
            float height = mDateLabelHeight;

            object.setLeftTop(left, top);
            object.setTranslate(left + width * 0.5f, top - height * 0.5f);

            GLESVertexInfo vertexInfo = object.getVertexInfo();
            float[] vertex = GLESUtils.makePositionCoord(-width * 0.5f, height * 0.5f, width, height);
            vertexInfo.setBuffer(mShader.getPositionAttribIndex(), vertex, 3);
            float[] texCoord = GLESUtils.makeTexCoord(0f, 0f, 1f, 1f);
            vertexInfo.setBuffer(mShader.getTexCoordAttribIndex(), texCoord, 2);

            yOffset -= (mDateLabelHeight + mSpacing);

            DateLabelInfo dateLabelInfo = mBucketInfo.get(i);
            yOffset -= (dateLabelInfo.getNumOfRows() * (mColumnWidth + mSpacing));

            ImageObjects imageObjects = object.getImageObjects();
            imageObjects.setSurfaceView(mSurfaceView);
            imageObjects.setStartOffsetY(top - mGridInfo.getDateLabelHeight() - mGridInfo.getSpacing());
            imageObjects.setupObjects(camera);
        }
    }

    // onSurfaceCreated

    void onSurfaceCreated() {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceCreated()");
        }

        clear();

        if (sDummyTexture == null) {
            sDummyTexture = GalleryUtils.createDummyTexture(Color.WHITE);
        }

        int size = mDateLabelObjects.size();
        for (int i = 0; i < size; i++) {
            DateLabelObject object = mDateLabelObjects.get(i);
            object.setShader(mShader);
            object.setDummyTexture(sDummyTexture);

            ImageObjects imageObjects = object.getImageObjects();
            imageObjects.onSurfaceCreated();
        }

        size = mTextureMappingInfos.size();
        for (int i = 0; i < size; i++) {
            mTextureMappingInfos.get(i).setTexture(null);
        }
    }

    void setCheckTexture(GLESTexture texture) {
        int size = mDateLabelObjects.size();
        for (int i = 0; i < size; i++) {
            DateLabelObject object = mDateLabelObjects.get(i);
            ImageObjects imageObjects = object.getImageObjects();
            imageObjects.setCheckTexture(texture);
        }
    }

    void onResume() {
        int size = mDateLabelObjects.size();
        for (int i = 0; i < size; i++) {
            DateLabelObject object = mDateLabelObjects.get(i);
            ImageObjects imageObjects = object.getImageObjects();
            imageObjects.onResume();
        }

    }

    void onPause() {
        sDummyTexture = null;

        cancelLoading();

        int size = mDateLabelObjects.size();
        for (int i = 0; i < size; i++) {
            DateLabelObject object = mDateLabelObjects.get(i);

            ImageObjects imageObjects = object.getImageObjects();
            imageObjects.onPause();
        }
    }

    @Override
    public void onColumnWidthChanged() {
        if (DEBUG) {
            Log.d(TAG, "onColumnWidthChanged()");
        }

        mPrevColumnWidth = mColumnWidth;
        mColumnWidth = mGridInfo.getColumnWidth();

        mPrevNumOfColumns = mNumOfColumns;
        mNumOfColumns = mGridInfo.getNumOfColumns();

        changeDateLabelObjectPosition();

        if (mAnimator != null) {
            mAnimator.cancel();
        }

        mAnimator = null;

        setupAnimations();

        mAnimator.start();
    }

    private void changeDateLabelObjectPosition() {
        int index = 0;
        float yOffset = mHalfHeight - mSystemBarHeight;

        int size = mDateLabelObjects.size();
        for (int i = 0; i < size; i++) {
            DateLabelObject object = mDateLabelObjects.get(i);
            ImageObjects imageObjects = object.getImageObjects();

            float prevTop = object.getTop();
            object.setPrevLeftTop(object.getLeft(), prevTop);
            imageObjects.setPrevStartOffsetY(prevTop - mDateLabelHeight - mSpacing);

            float nextLeft = mSpacing - mHalfWidth;
            float nextTop = yOffset;

            object.setNextLeftTop(nextLeft, nextTop);
            imageObjects.setNextStartOffsetY(nextTop - mDateLabelHeight - mSpacing);

            yOffset -= (mDateLabelHeight + mSpacing);

            DateLabelInfo dateLabelInfo = mBucketInfo.get(index);
            yOffset -= (dateLabelInfo.getNumOfRows() * (mColumnWidth + mSpacing));

            index++;
        }
    }

    private void setupAnimations() {
        mInvisibleObjects.clear();
        mAnimationObjects.clear();

        float fromAlpha = 0f;
        float toAlpha = 1f;

        mAnimator = new GLESAnimator(new AlphaAnimatorCallback());
        mAnimator.setValues(fromAlpha, toAlpha);
        mAnimator.setDuration(GalleryConfig.DATE_LABEL_ANIMATION_START_OFFSET, GalleryConfig.DATE_LABEL_ANIMATION_END_OFFSET);

        float viewportTop = mHalfHeight - mGridInfo.getTranslateY();
        float viewportBottom = viewportTop - mHeight;

        float nextTranslateY = mAlbumViewManager.getNextTranslateY();
        float nextViewportTop = mHalfHeight - nextTranslateY;
        float nextViewportBottom = nextViewportTop - mHeight;

        int size = mDateLabelObjects.size();
        int lastVisibleIndex = 0;
        for (int i = 0; i < size; i++) {
            DateLabelObject object = mDateLabelObjects.get(i);
            ImageObjects imageObjects = object.getImageObjects();
            DateLabelInfo dateLabelInfo = mBucketInfo.get(i);
            GalleryNode parentNode = object.getParentNode();

            float prevTop = object.getPrevTop();
            int numOfRows = (int) Math.ceil((double) imageObjects.getNumOfImages() / mPrevNumOfColumns);
            float bottom = prevTop - (mDateLabelHeight + mSpacing) - numOfRows * (mPrevColumnWidth + mSpacing);

            float nextTop = object.getNextTop();
            int nextNumOfRows = dateLabelInfo.getNumOfRows();
            float nextBottom = nextTop - (mDateLabelHeight + mSpacing) - nextNumOfRows * (mColumnWidth + mSpacing);

            if ((nextTop >= nextViewportBottom && nextBottom <= nextViewportTop) ||
                    (prevTop >= viewportBottom && bottom <= viewportTop)) {
                parentNode.setVisibility(true);

                mAnimationObjects.add(object);

                lastVisibleIndex = i;
            } else {
                if (lastVisibleIndex + 1 == i) {
                    parentNode.setVisibility(true);
                    mAnimationObjects.add(object);
                } else {
                    parentNode.setVisibility(false);
                    imageObjects.setStartOffsetY(imageObjects.getNextStartOffsetY());
                    mInvisibleObjects.put(i, object);
                }
            }
        }
    }

    @Override
    public void onImageDeleted() {
        if (DEBUG) {
            Log.d(TAG, "onImageDeleted()");
        }

        changeDateLabelPosition();
    }

    private void changeDateLabelPosition() {
        float yOffset = mHalfHeight - mSystemBarHeight;

        float left = mSpacing - mHalfWidth;
        float top = 0f;
        float width = mWidth - mSpacing * 2f;
        float height = mDateLabelHeight;
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;

        int size = mDateLabelObjects.size();
        for (int i = 0; i < size; i++) {
            DateLabelObject object = mDateLabelObjects.get(i);

            top = yOffset;

            object.setLeftTop(left, top);
            object.setTranslate(left + halfWidth, top - halfHeight);

            yOffset -= (mDateLabelHeight + mSpacing);

            DateLabelInfo dateLabelInfo = mBucketInfo.get(i);
            yOffset -= (dateLabelInfo.getNumOfRows() * (mColumnWidth + mSpacing));

            ImageObjects imageObjects = object.getImageObjects();
            imageObjects.setStartOffsetY(top - mDateLabelHeight - mSpacing);
        }
    }

    @Override
    public void onDateLabelDeleted() {
        if (DEBUG) {
            Log.d(TAG, "onDateLabelDeleted()");
        }

        mNumOfDateInfos = mGridInfo.getNumOfDateInfos();

        changeDateLabelPosition();
    }


    // initialization

    void createObjects(GLESNode parentNode) {
        if (DEBUG) {
            Log.d(TAG, "createObjects()");
        }

        mParentNode = parentNode;

        for (int i = 0; i < mNumOfDateInfos; i++) {
            GalleryNode node = new GalleryNode("node" + i);
            parentNode.addChild(node);
            node.setVisibility(false);

            DateLabelObject object = new DateLabelObject("dataIndex" + i);
            mDateLabelObjects.add(object);
            node.addChild(object);
            object.setParentNode(node);

            object.setGLState(mDateLabelGLState);
            object.setListener(mObjectListener);
            object.setIndex(i);

            GLESVertexInfo vertexInfo = new GLESVertexInfo();
            vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
            vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);
            object.setVertexInfo(vertexInfo, false, false);

            DateLabelInfo dateLabelInfo = mBucketInfo.get(i);
            TextureMappingInfo textureMappingInfo = new TextureMappingInfo(object, dateLabelInfo);
            mTextureMappingInfos.add(textureMappingInfo);

            ImageObjects imageObjects = new ImageObjects(mContext, mGridInfo, dateLabelInfo);
            imageObjects.setAlbumViewManager(mAlbumViewManager);
            imageObjects.setGLState(mImageGLState);
            imageObjects.createObjects(node);

            object.setImageObjects(imageObjects);
        }
    }

    void setAlbumViewManager(AlbumViewManager manager) {
        mAlbumViewManager = manager;
    }

    void setSurfaceView(GallerySurfaceView surfaceView) {
        if (DEBUG) {
            Log.d(TAG, "setSurfaceView()");
        }

        mSurfaceView = surfaceView;
    }

    void setDateLabelGLState(GLESGLState state) {
        if (DEBUG) {
            Log.d(TAG, "setDateLabelGLState()");
        }

        mDateLabelGLState = state;
    }

    void setImageGLState(GLESGLState state) {
        if (DEBUG) {
            Log.d(TAG, "setImageGLState()");
        }

        mImageGLState = state;
    }

    void setDateLabelShader(GLESShader shader) {
        if (DEBUG) {
            Log.d(TAG, "setDateLabelShader()");
        }

        mShader = shader;

        int location = mShader.getUniformLocation("uAlpha");
        GLES20.glUniform1f(location, 1f);
    }

    void setImageShader(GLESShader shader) {
        if (DEBUG) {
            Log.d(TAG, "setImageShader()");
        }

        int size = mDateLabelObjects.size();
        for (int i = 0; i < size; i++) {
            DateLabelObject object = mDateLabelObjects.get(i);
            ImageObjects imageObjects = object.getImageObjects();
            imageObjects.setShader(shader);
        }
    }

    DateLabelObject getObject(int i) {
        return mDateLabelObjects.get(i);
    }

    ImageObject getImageObject(ImageIndexingInfo indexingInfo) {
        DateLabelObject object = mDateLabelObjects.get(indexingInfo.mDateLabelIndex);
        ImageObjects imageObjects = object.getImageObjects();
        ImageObject imageObject = imageObjects.getObject(indexingInfo.mImageIndex);

        return imageObject;
    }

    void hide() {
        mAlpha = 0f;
    }

    void makeDateLabel(DateLabelInfo dateLableInfo, GalleryTexture texture) {
        if (BitmapWorker.cancelPotentialWork(dateLableInfo, texture) && texture != null) {
            final DateLabelTask task = new DateLabelTask(mContext, mGridInfo, texture);
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(mContext.getResources(),
                            mLoadingBitmap, task);
            DateLabelObject object = (DateLabelObject) mTextureMappingInfos.get(dateLableInfo.getIndex()).getObject();
            texture.setBitmapDrawable(asyncDrawable);
            task.execute(dateLableInfo);
        }
    }

    void deleteDateLabel(int index) {
        TextureMappingInfo textureMappingInfo = mTextureMappingInfos.get(index);

        GalleryTexture texture = textureMappingInfo.getTexture();
        texture = null;

        mTextureMappingInfos.remove(index);

        DateLabelObject object = mDateLabelObjects.remove(index);
        mParentNode.removeChild(object);

    }

    void deleteImage(ImageIndexingInfo indexingInfo) {
        DateLabelObject object = mDateLabelObjects.get(indexingInfo.mDateLabelIndex);
        ImageObjects imageObjects = object.getImageObjects();
        imageObjects.delete(indexingInfo.mImageIndex);
    }

    @Override
    public void onImageLoaded(int index, GalleryTexture texture) {
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

            TextureState textureState = texture.getState();

            switch (textureState) {
                case DECODING:
                    BitmapWorker.cancelWork(texture);
                    texture.setState(TextureState.CANCELED);
                    break;
                case QUEUING:
                    texture.setState(TextureState.CANCELED);
                    break;
            }
        }

        mWaitingTextures.clear();
    }

    class DateLabelTask extends BitmapWorker.BitmapWorkerTask<GalleryTexture> {
        static final String CLASS = "DateLabelTask";
        static final String TAG = GalleryConfig.TAG + "_" + CLASS;
        static final boolean DEBUG = GalleryConfig.DEBUG;

        private final float DATE_LABEL_TEXT_SIZE = 17f;
        private final float DATE_LABEL_TEXT_SHADOW_RADIUS = 0.7f;
        private final float DATE_LABEL_TEXT_SHADOW_DX = 0.3f;
        private final float DATE_LABEL_TEXT_SHADOW_DY = 0.3f;
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

            int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
            int width = (int) (screenWidth - mSpacing * 2);
            int height = mDateLabelHeight;

            int x = mContext.getResources().getDimensionPixelSize(R.dimen.dateindex_padding);
            int y = (height - textHeight) / 2 + ascent;

            Bitmap bitmap = mReusableBitmaps.poll();
            if (bitmap == null) {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }

            bitmap = GLESUtils.drawTextToBitmap(x, y,
                    mDateLabelInfo.getDate(), textPaint, bitmap);
//            bitmap = GLESUtils.drawTextToBitmap(x, y,
//                    "" + mDateLabelInfo.getIndex(), textPaint, bitmap);

            BitmapDrawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);

            return drawable;
        }
    }

    void onAnimationFinished() {
        invalidateObjects();
    }

    private void invalidateObjects() {
        int size = mInvisibleObjects.size();
        for (int i = 0; i < size; i++) {
            DateLabelObject object = mInvisibleObjects.valueAt(i);
            invalidateObject(object);
        }

        mInvisibleObjects.clear();

        size = mDateLabelObjects.size();
        for (int i = 0; i < size; i++) {
            DateLabelObject object = mDateLabelObjects.get(i);
            ImageObjects imageObjects = object.getImageObjects();
            imageObjects.invalidateObjects();
        }
    }

    private void invalidateObject(DateLabelObject object) {
        object.setLeftTop(object.getNextLeft(), object.getNextTop());

        float translateX = object.getLeft() + mGridInfo.getDateLabelWidth() * 0.5f;
        float translateY = object.getTop() - mGridInfo.getDateLabelHeight() * 0.5f;

        object.setTranslate(translateX, translateY);
    }


    void onAnimation(float x) {
        int size = mAnimationObjects.size();
        for (int i = 0; i < size; i++) {
            DateLabelObject object = mAnimationObjects.get(i);

            float prevLeft = object.getPrevLeft();
            float prevTop = object.getPrevTop();

            float nextLeft = object.getNextLeft();
            float nextTop = object.getNextTop();

            float currentLeft = prevLeft + (nextLeft - prevLeft) * x;
            float currentTop = prevTop + (nextTop - prevTop) * x;

            object.setLeftTop(currentLeft, currentTop);

            ImageObjects imageObjects = object.getImageObjects();
            imageObjects.setStartOffsetY(currentTop - mDateLabelHeight - mSpacing);
            imageObjects.onAnimation(x);

            float translateX = currentLeft + mGridInfo.getDateLabelWidth() * 0.5f;
            float translateY = currentTop - mDateLabelHeight * 0.5f;

            object.setTranslate(translateX, translateY);
        }
    }

    private GLESObjectListener mObjectListener = new GLESObjectListener() {
        @Override
        public void update(GLESObject object) {
            DateLabelObject dateLabelObject = (DateLabelObject) object;
            GLESTransform transform = object.getTransform();
            transform.setIdentity();
            transform.setTranslate(dateLabelObject.getTranslateX(), dateLabelObject.getTranslateY(), 0f);
        }

        @Override
        public void apply(GLESObject object) {
            int location = mShader.getUniformLocation("uAlpha");
            GLES20.glUniform1f(location, mAlpha);
        }
    };


    class AlphaAnimatorCallback implements GLESAnimatorCallback {
        AlphaAnimatorCallback() {
        }

        @Override
        public void onAnimation(GLESVector3 current) {
            mAlpha = current.getX();
        }


        @Override
        public void onCancel() {

        }

        @Override
        public void onFinished() {
            onAnimationFinished();
        }
    }
}
