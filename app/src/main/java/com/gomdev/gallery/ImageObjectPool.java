package com.gomdev.gallery;

import java.util.LinkedList;

/**
 * Created by gomdev on 15. 2. 10..
 */
public class ImageObjectPool {
    static final String CLASS = "ImageObjectPool";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private static LinkedList<ImageObject> sImageObjects = new LinkedList<>();

    static void push(ImageObject object) {
        sImageObjects.push(object);
    }

    static ImageObject pop() {
        ImageObject object;
        if (sImageObjects.isEmpty() == true) {
            object = new ImageObject("imageObject");
        } else {
            object = sImageObjects.pop();
        }
        return object;
    }
}
