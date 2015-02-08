package com.gomdev.gallery;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import java.lang.ref.WeakReference;

class AsyncDrawable extends BitmapDrawable {

    private final WeakReference<BitmapWorker.BitmapWorkerTask> mBitmapWorkerTaskReference;

    AsyncDrawable(Resources res, Bitmap bitmap,
                  BitmapWorker.BitmapWorkerTask bitmapLoaderTask) {
        super(res, bitmap);
        mBitmapWorkerTaskReference =
                new WeakReference<>(bitmapLoaderTask);
    }

    BitmapWorker.BitmapWorkerTask getBitmapWorkerTask() {
        return mBitmapWorkerTaskReference.get();
    }
}
