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

    private ImageManager mImageManager = null;
    private GalleryTexture mTextureReference = null;

    private int mPosition = -1;

    public GalleryObject() {
        super();
        init();
    }

    public GalleryObject(String name) {
        super(name);
        init();
    }

    private void init() {
        mImageManager = ImageManager.getInstance();
    }

    @Override
    public void setTexture(GLESTexture texture) {
        super.setTexture(texture);
    }

    public void setPosition(int position) {
        mPosition = position;
    }

    public int getPosition() {
        return mPosition;
    }

    public void setTextureReference(GalleryTexture reference) {
        mTextureReference = reference;
    }
}
