package com.gomdev.gallery;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.gomdev.gallery.ImageManager.BitmapWorkerTask;

import java.lang.ref.WeakReference;

public class AsyncDrawable extends BitmapDrawable {

    private final WeakReference<BitmapWorkerTask> mBitmapWorkerTaskReference;

    public AsyncDrawable(Resources res, Bitmap bitmap,
                         BitmapWorkerTask bitmapWorkerTask) {
        super(res, bitmap);
        mBitmapWorkerTaskReference =
                new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
    }

    public BitmapWorkerTask getBitmapWorkerTask() {
        return mBitmapWorkerTaskReference.get();
    }
}
