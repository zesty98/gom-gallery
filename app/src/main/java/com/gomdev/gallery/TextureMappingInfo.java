package com.gomdev.gallery;

/**
 * Created by gomdev on 14. 12. 23..
 */
public class TextureMappingInfo {
    static final String CLASS = "ImageManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private GalleryObject mObject;
    private GalleryTexture mTexture;
    private GalleryInfo mGalleryInfo;


    public TextureMappingInfo(GalleryObject object, GalleryInfo galleryInfo) {
        mObject = object;
        mGalleryInfo = galleryInfo;
    }

    public GalleryObject getObject() {
        return mObject;
    }

    public GalleryTexture getTexture() {
        return mTexture;
    }

    public GalleryInfo getGalleryInfo() {
        return mGalleryInfo;
    }

    public void set(GalleryTexture texture) {
        mTexture = texture;
    }
}
