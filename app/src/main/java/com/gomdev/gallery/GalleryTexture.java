package com.gomdev.gallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLSurfaceView;

import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESTexture2D;
import com.gomdev.gles.GLESVertexInfo;

/**
 * Created by gomdev on 14. 12. 17..
 */
public class GalleryTexture extends GLESTexture2D implements CacheContainer {
    static final String CLASS = "GalleryTexture";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private BitmapDrawable mDrawable = null;

    private GLSurfaceView mSurfaceView = null;
    private GLESObject mObject = null;

    private final ImageManager mImageManager;
    private BucketInfo mBucketInfo = null;
    private ImageInfo mImageInfo = null;
    private int mPosition = -1;

    public GalleryTexture(int width, int height) {
        super(width, height);

        mImageManager = ImageManager.getInstance();
    }

    @Override
    public void setBitmapDrawable(BitmapDrawable drawable) {
        mDrawable = drawable;

        if (drawable instanceof AsyncDrawable) {
            return;
        }

        GLESVertexInfo vertexInfo = mObject.getVertexInfo();
        GLESShader shader = mObject.getShader();

        final Bitmap bitmap = drawable.getBitmap();
        mImageInfo.setThumbnailWidth(bitmap.getWidth());
        mImageInfo.setThumbnailHeight(bitmap.getHeight());

        calcTexCoord(mImageInfo);

        vertexInfo.setBuffer(shader.getTexCoordAttribIndex(), mImageInfo.getTexCoord(), 2);


        if (mSurfaceView != null) {
            mSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    GalleryTexture.this.load(bitmap);
                    mObject.setTexture(GalleryTexture.this);
                    mSurfaceView.requestRender();
                }
            });
        }
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

    @Override
    public BitmapDrawable getBitmapDrawable() {
        return mDrawable;
    }

    public void setSurfaceView(GLSurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    public void setObject(GLESObject object) {
        mObject = object;
    }

    public void setBucketInfo(BucketInfo bucketInfo) {
        mBucketInfo = bucketInfo;
    }

    public void setPosition(int position) {
        mPosition = position;

        mImageInfo = mBucketInfo.get(position);

        mImageManager.loadThumbnail(mImageInfo, this);
    }

    public int getPosition() {
        return mPosition;
    }
}
