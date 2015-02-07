package com.gomdev.gallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLES20;

import com.gomdev.gles.GLESTexture;

/**
 * Created by gomdev on 14. 12. 17..
 */
class GalleryTexture implements BitmapContainer {
    static final String CLASS = "GalleryTexture";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private GLESTexture mTexture = null;
    private GLESTexture.Builder mBuilder = null;
    private BitmapDrawable mDrawable = null;

    private ImageLoadingListener mImageLoadingListener;

    boolean mIsTextureLoadingFinished = false;
    boolean mIsTextureLoadingStarted = false;
    private int mPosition = 0;

    GalleryTexture(int width, int height) {
        mBuilder = new GLESTexture.Builder(GLES20.GL_TEXTURE_2D, width, height);
    }

    GLESTexture getTexture() {
        return mTexture;
    }

    @Override
    public void setBitmapDrawable(BitmapDrawable drawable) {
        mDrawable = drawable;

        if (drawable instanceof AsyncDrawable) {
            mIsTextureLoadingStarted = true;
            return;
        }

        if (mDrawable instanceof RecyclingBitmapDrawable) {
            ((RecyclingBitmapDrawable) mDrawable).setIsDisplayed(true);
        }

        if (mIsTextureLoadingFinished == false) {
            mImageLoadingListener.onImageLoaded(mPosition, this);
        }
        mIsTextureLoadingFinished = true;
    }

    @Override
    public BitmapDrawable getBitmapDrawable() {
        return mDrawable;
    }

    void load(Bitmap bitmap) {
        mTexture = mBuilder.load(bitmap);
    }

    void setPosition(int position) {
        mPosition = position;
    }

    int getPosition() {
        return mPosition;
    }

    void setImageLoadingListener(ImageLoadingListener listener) {
        mImageLoadingListener = listener;
    }

    boolean isTextureLoadingNeeded() {
        return (mIsTextureLoadingFinished == false) && (mIsTextureLoadingStarted == false);
    }
}
