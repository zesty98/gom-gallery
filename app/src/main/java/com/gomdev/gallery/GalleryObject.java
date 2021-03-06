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

    private GalleryTexture mTexture = null;

    private float mLeft = 0f;
    private float mTop = 0f;

    private float mPrevLeft = 0f;
    private float mPrevTop = 0f;

    private float mNextLeft = 0f;
    private float mNextTop = 0f;

    private float mTranslateX = 0f;
    private float mTranslateY = 0f;
    private float mTranslateZ = 0f;

    GalleryObject(String name) {
        super(name);
        init();
    }

    private void init() {
    }

    void setTexture(GalleryTexture texture) {
        if (mTexture != null) {
            mTexture.destroy();
            mTexture = null;
        }

        mTexture = texture;

        this.setTexture(texture.getTexture());
    }

    void setDummyTexture(GLESTexture texture) {
        super.setTexture(texture);

        if (mTexture != null) {
            mTexture.destroy();
            mTexture = null;
        }
    }

    GalleryTexture getGalleryTexture() {
        return mTexture;
    }

    void setIndex(int index) {
        mIndex = index;
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
        this.setTranslate(x, y, 0f);
    }

    void setTranslate(float x, float y, float z) {
        mTranslateX = x;
        mTranslateY = y;
        mTranslateZ = z;
    }

    float getTranslateX() {
        return mTranslateX;
    }

    float getTranslateY() {
        return mTranslateY;
    }

    float getTranslateZ() {
        return mTranslateZ;
    }
}
