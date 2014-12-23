package com.gomdev.gallery;

import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import com.gomdev.gles.GLESTexture2D;

/**
 * Created by gomdev on 14. 12. 17..
 */
public class GalleryTexture extends GLESTexture2D implements CacheContainer {
    static final String CLASS = "GalleryTexture";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private BitmapDrawable mDrawable = null;

    private ImageLoadingListener mImageLoadingListener;

    boolean mIsTextureLoaded = false;
    private int mPosition = 0;

    public GalleryTexture(int width, int height) {
        super(width, height);
    }

    @Override
    public void setBitmapDrawable(BitmapDrawable drawable) {
        mDrawable = drawable;

        if (drawable instanceof AsyncDrawable) {
            return;
        }

        Log.d(TAG, "setBitmapDrawable() position=" + mPosition);
        mImageLoadingListener.onImageLoaded(mPosition, this);
        mIsTextureLoaded = true;
    }

    @Override
    public BitmapDrawable getBitmapDrawable() {
        return mDrawable;
    }

    public void setPosition(int position) {
        mPosition = position;
    }

    public void setImageLoadingListener(ImageLoadingListener listener) {
        mImageLoadingListener = listener;
    }

    public boolean isTextureLoaded() {
        return mIsTextureLoaded;
    }
}
