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

    private float mLeft = 0;
    private float mTop = 0;

    private float mNextLeft = 0;
    private float mNextTop = 0;

    private float mTranslateX = 0f;
    private float mTranslateY = 0f;

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
}
