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

    enum TextureState {
        NONE,
        DECODING,
        QUEUING,
        LOADED
    }

    private GLESTexture mTexture = null;
    private GLESTexture.Builder mBuilder = null;
    private BitmapDrawable mDrawable = null;

    private TextureState mTextureState = TextureState.NONE;

    private ImageLoadingListener mImageLoadingListener;

    volatile boolean mIsTextureLoadingFinished = false;
    volatile boolean mIsTextureLoadingStarted = false;

    private boolean mIsThumbnail = false;

    private int mIndex = 0;

    GalleryTexture(int width, int height) {
        synchronized (this) {
            setState(TextureState.NONE);
            mBuilder = new GLESTexture.Builder(GLES20.GL_TEXTURE_2D, width, height);
        }
    }

    GLESTexture getTexture() {
        return mTexture;
    }

    void destroy() {
        if (mDrawable != null && mDrawable instanceof RecyclingBitmapDrawable) {
            ((RecyclingBitmapDrawable) mDrawable).setIsDisplayed(false);
        }

        if (mTexture != null) {
            mTexture.destroy();
        }
        mTexture = null;
    }

    @Override
    public void setBitmapDrawable(BitmapDrawable drawable) {
        mDrawable = drawable;

        if (drawable instanceof AsyncDrawable) {
            synchronized (this) {
                setState(TextureState.DECODING);
                mIsTextureLoadingStarted = true;
                mIsTextureLoadingFinished = false;
            }
            return;
        }

        if (mDrawable instanceof RecyclingBitmapDrawable) {
            ((RecyclingBitmapDrawable) mDrawable).setIsDisplayed(true);
        }

        if (mIsTextureLoadingFinished == false) {
            synchronized (this) {
                setState(TextureState.QUEUING);
                mImageLoadingListener.onImageLoaded(mIndex, this);
            }
        }

        mIsTextureLoadingFinished = true;
    }

    @Override
    public BitmapDrawable getBitmapDrawable() {
        return mDrawable;
    }

    synchronized void load(Bitmap bitmap) {
        setState(TextureState.LOADED);
        mTexture = mBuilder.load(bitmap);
    }

    void setIndex(int index) {
        mIndex = index;
    }

    int getIndex() {
        return mIndex;
    }

    void setImageLoadingListener(ImageLoadingListener listener) {
        mImageLoadingListener = listener;
    }

    boolean isTextureLoadingNeeded() {
        return (mIsTextureLoadingFinished == false) && (mIsTextureLoadingStarted == false);
    }

    private void setState(TextureState textureState) {
        mTextureState = textureState;
    }

    synchronized TextureState getState() {
        return mTextureState;
    }

    void setThumbnail(boolean isThumbnail) {
        mIsThumbnail = isThumbnail;
    }

    boolean isThumbnail() {
        return mIsThumbnail;
    }
}
