package com.gomdev.gallery;

/**
 * Created by gomdev on 14. 12. 23..
 */
public class TextureMappingInfo {
    static final String CLASS = "ImageManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private ImageObject mObject;
    private GalleryTexture mTexture;
    private ImageInfo mImageInfo;


    public TextureMappingInfo(ImageObject object, ImageInfo imageInfo) {
        mObject = object;
        mImageInfo = imageInfo;
    }

    public ImageObject getObject() {
        return mObject;
    }

    public GalleryTexture getTexture() {
        return mTexture;
    }

    public ImageInfo getImageInfo() {
        return mImageInfo;
    }

    public void set(GalleryTexture texture) {
        mTexture = texture;
    }
}
