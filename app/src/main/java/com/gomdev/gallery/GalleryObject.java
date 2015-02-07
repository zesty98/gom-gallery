package com.gomdev.gallery;

import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESTexture;

/**
 * Created by gomdev on 14. 12. 18..
 */
class GalleryObject extends GLESObject {
    static final String CLASS = "GalleryObject";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private int mIndex = -1;

    private float mLeft = 200f;
    private float mTop = 600f;

    private float mPrevLeft = 200f;
    private float mPrevTop = 600f;

    private float mNextLeft = 200f;
    private float mNextTop = 600f;

    private float mTranslateX = 200f;
    private float mTranslateY = 600f;

    private boolean mIsTextureMapped = false;

    GalleryObject(String name) {
        super(name);
        init();
    }

    private void init() {
    }

    void setTexture(GalleryTexture texture) {
        GLESTexture prevTexture = getTexture();
        prevTexture.destroy();

        this.setTexture(texture.getTexture());
    }

    void setIndex(int position) {
        mIndex = position;
    }

    int getIndex() {
        return mIndex;
    }

    void setLeftTop(float x, float y) {
        mLeft = x;
        mTop = y;
    }

    float getLeft() {
        return mLeft;
    }

    float getTop() {
        return mTop;
    }

    void setNextLeftTop(float x, float y) {
        mNextLeft = x;
        mNextTop = y;
    }

    float getNextLeft() {
        return mNextLeft;
    }

    float getNextTop() {
        return mNextTop;
    }

    void setPrevLeftTop(float x, float y) {
        mPrevLeft = x;
        mPrevTop = y;
    }

    float getPrevLeft() {
        return mPrevLeft;
    }

    float getPrevTop() {
        return mPrevTop;
    }

    void setTranslate(float x, float y) {
        mTranslateX = x;
        mTranslateY = y;
    }

    float getTranslateX() {
        return mTranslateX;
    }

    float getTranslateY() {
        return mTranslateY;
    }

    void setTextureMapping(boolean isTextureMapped) {
        mIsTextureMapped = isTextureMapped;
    }

    boolean isTexturMapped() {
        return mIsTextureMapped;
    }
}
