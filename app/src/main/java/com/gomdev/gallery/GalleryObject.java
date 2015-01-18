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

    private int mPosition = -1;
    private float mLeft = 0;
    private float mTop = 0;

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
}
