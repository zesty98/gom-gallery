package com.gomdev.gallery;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by gomdev on 15. 1. 12..
 */
public class BitmapWorker {
    static final String CLASS = "BitmapWorker";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    public static <T extends BitmapContainer> boolean cancelPotentialWork(GalleryInfo galleryInfo,
                                                                          T container) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(container);

        if (bitmapWorkerTask != null) {
            final GalleryInfo data = bitmapWorkerTask.getData();
            // If bitmapData is not yet set or it differs from the new data
            if (data == null || data != galleryInfo) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was
        // cancelled
        return true;
    }

    public static <T extends BitmapContainer> void cancelWork(T container) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(container);
        if (bitmapWorkerTask != null) {
            bitmapWorkerTask.cancel(true);
            if (DEBUG) {
                final Object bitmapData = bitmapWorkerTask.getData();
                Log.d(TAG, "cancelWork - cancelled work for " + bitmapData);
            }
        }
    }

    private static <T extends BitmapContainer> BitmapWorkerTask getBitmapWorkerTask(T container) {
        if (container != null) {
            final Drawable drawable = container.getBitmapDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }

        return null;
    }

    public static class BitmapWorkerTask<T extends BitmapContainer> extends AsyncTask<GalleryInfo, Void, BitmapDrawable> {
        static final String CLASS = "BitmapWorkerTask";
        static final String TAG = GalleryConfig.TAG + "_" + CLASS;
        static final boolean DEBUG = GalleryConfig.DEBUG;

        protected final WeakReference<T> mReference;
        protected GalleryInfo mGalleryInfo = null;

        BitmapWorkerTask(T container) {
            mReference = new WeakReference<>(container);
        }

        @Override
        protected BitmapDrawable doInBackground(GalleryInfo... params) {
            mGalleryInfo = params[0];

            return null;
        }

        @Override
        protected void onPostExecute(BitmapDrawable value) {
            if (isCancelled()) {
                value = null;
            }

            if (mReference != null && value != null) {
                final T container = mReference.get();
                final BitmapWorkerTask dateLabelTask =
                        getBitmapWorkerTask(container);
                if (this == dateLabelTask && value != null) {
                    container.setBitmapDrawable(value);
                }
            }

        }

        protected GalleryInfo getData() {
            return mGalleryInfo;
        }
    }
}
