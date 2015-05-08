package com.gomdev.gallery;

import com.gomdev.gles.GLESObject;

/**
 * Created by gomdev on 15. 4. 30..
 */
public class LargeImageObject extends GLESObject {
    private static final String CLASS = "LargeImageObject";
    private static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    private static final boolean DEBUG = GalleryConfig.DEBUG;

    private int mX = 0;
    private int mY = 0;

    private int mImageX = 0;
    private int mImageY = 0;

    private int mWidth = 0;
    private int mHeight = 0;

    private int mTextureID = -1;

    private boolean mIsTextureMapped = false;

    public LargeImageObject() {
        super();
    }

    public LargeImageObject(String name) {
        super(name);
    }

    int getX() {
        return mX;
    }

    int getY() {
        return mY;
    }

    void setPosition(int x, int y) {
        mX = x;
        mY = y;
    }

    int getImageX() {
        return mImageX;
    }

    int getmImageY() {
        return mImageY;
    }

    void setImagePosition(int x, int y) {
        mImageX = x;
        mImageY = y;
    }

    void setWidth(int width) {
        mWidth = width;
    }

    int getWidth() {
        return mWidth;
    }

    void setHeight(int height) {
        mHeight = height;
    }

    int getHeight() {
        return mHeight;
    }

    void setTextureMapped(boolean isTextureMapped) {
        mIsTextureMapped = isTextureMapped;
    }

    boolean isTextureMapped() {
        return mIsTextureMapped;
    }
}
