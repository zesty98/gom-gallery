package com.gomdev.gallery;

import android.view.animation.Interpolator;

import com.gomdev.gles.GLESAnimator;
import com.gomdev.gles.GLESObject;
import com.gomdev.gles.GLESTexture;
import com.gomdev.gles.GLESVector3;

/**
 * Created by gomdev on 14. 12. 18..
 */
public class GalleryObject extends GLESObject {
    static final String CLASS = "GalleryObject";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    protected int mPosition = -1;

    protected float mWidth = 0;
    protected float mHeight = 0;

    protected float mLeft = 0f;
    protected float mTop = 0f;

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

    public void setPosition(int position) {
        mPosition = position;
    }

    public int getPosition() {
        return mPosition;
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

    public void setSize(float width, float height) {
        mWidth = width;
        mHeight = height;
    }

    public float getWidth() {
        return mWidth;
    }

    public float getHeight() {
        return mHeight;
    }
}
