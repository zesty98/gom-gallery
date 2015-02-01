package com.gomdev.gallery;

import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESTexture;

/**
 * Created by gomdev on 14. 12. 18..
 */
public class GalleryObject extends GLESObject {
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

    public GalleryObject(String name) {
        super(name);
        init();
    }

    private void init() {
    }

    public void setTexture(GalleryTexture texture) {
        GLESTexture prevTexture = getTexture();
        prevTexture.destroy();

        this.setTexture(texture.getTexture());
    }

    public void setIndex(int position) {
        mIndex = position;
    }

    public int getIndex() {
        return mIndex;
    }

    public void setLeftTop(float x, float y) {
        mLeft = x;
        mTop = y;
    }

    public float getLeft() {
        return mLeft;
    }

    public float getTop() {
        return mTop;
    }

    public void setNextLeftTop(float x, float y) {
        mNextLeft = x;
        mNextTop = y;
    }

    public float getNextLeft() {
        return mNextLeft;
    }

    public float getNextTop() {
        return mNextTop;
    }

    public void setPrevLeftTop(float x, float y) {
        mPrevLeft = x;
        mPrevTop = y;
    }

    public float getPrevLeft() {
        return mPrevLeft;
    }

    public float getPrevTop() {
        return mPrevTop;
    }

    public void setTranslate(float x, float y) {
        mTranslateX = x;
        mTranslateY = y;
    }

    public float getTranslateX() {
        return mTranslateX;
    }

    public float getTranslateY() {
        return mTranslateY;
    }

    public void setTextureMapping(boolean isTextureMapped) {
        mIsTextureMapped = isTextureMapped;
    }

    public boolean isTexturMapped() {
        return mIsTextureMapped;
    }
}
