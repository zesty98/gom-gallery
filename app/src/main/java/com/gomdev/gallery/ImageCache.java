package com.gomdev.gallery;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.LruCache;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ImageCache {
    static final String CLASS = "ImageCache";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    // Default memory cache size in kilobytes
    private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 5; // 5MB

    // Default disk cache size in bytes
    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB

    // Compression settings when writing images to disk cache
    private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;
    private static final int DEFAULT_COMPRESS_QUALITY = 70;
    private static final int DISK_CACHE_INDEX = 0;

    // Constants to easily toggle various caches
    private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;
    private static final boolean DEFAULT_DISK_CACHE_ENABLED = true;
    private static final boolean DEFAULT_INIT_DISK_CACHE_ON_CREATE = false;
    private final Object mDiskCacheLock = new Object();
    private ImageCacheParams mCacheParams = null;
    private LruCache<String, BitmapDrawable> mMemoryCache;
    private Set<SoftReference<Bitmap>> mReusableBitmaps;
    private DiskLruCache mDiskLruCache;
    private boolean mDiskCacheStarting = true;

    private ImageCache(ImageCacheParams params) {
        mCacheParams = params;

        if (params.mMemoryCacheEnabled == true) {
            initMemoryCache();
        }

        initDiskCache();
    }

    public static ImageCache getInstance(FragmentManager fragmentManager, ImageCacheParams params) {

        // Search for, or create an instance of the non-UI RetainFragment
        final RetainFragment mRetainFragment = findOrCreateRetainFragment(fragmentManager);

        // See if we already have an ImageCache stored in RetainFragment
        ImageCache imageCache = (ImageCache) mRetainFragment.getObject();

        // No existing ImageCache, create one and store it in RetainFragment
        if (imageCache == null) {
            imageCache = new ImageCache(params);
            mRetainFragment.setObject(imageCache);
        }

        return imageCache;
    }

    private static File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use
        // external cache dir
        // otherwise use internal cache dir
        final String cachePath =
                Environment.MEDIA_MOUNTED.equals(Environment
                        .getExternalStorageState()) ||
                        !Environment.isExternalStorageRemovable() ? context
                        .getExternalCacheDir().getPath() :
                        context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    private static long getUsableSpace(File path) {
        return path.getUsableSpace();
    }

    /**
     * Locate an existing instance of this Fragment or if not found, create and
     * add it using FragmentManager.
     *
     * @param fm The FragmentManager manager to use.
     * @return The existing instance of the Fragment or the new instance if just
     * created.
     */
    private static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
        //BEGIN_INCLUDE(find_create_retain_fragment)
        // Check to see if we have retained the worker fragment.
        RetainFragment mRetainFragment = (RetainFragment) fm.findFragmentByTag(TAG);

        // If not retained (or first time running), we need to create and add it.
        if (mRetainFragment == null) {
            mRetainFragment = new RetainFragment();
            fm.beginTransaction().add(mRetainFragment, TAG).commitAllowingStateLoss();
        }

        return mRetainFragment;
        //END_INCLUDE(find_create_retain_fragment)
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static int getBitmapSize(BitmapDrawable value) {
        Bitmap bitmap = value.getBitmap();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return bitmap.getAllocationByteCount();
        }

        return bitmap.getByteCount();
    }

    /**
     * A helper function to return the byte usage per pixel of a bitmap based on its configuration.
     */
    private static int getBytesPerPixel(Bitmap.Config config) {
        if (config == Bitmap.Config.ARGB_8888) {
            return 4;
        } else if (config == Bitmap.Config.RGB_565) {
            return 2;
        } else if (config == Bitmap.Config.ARGB_4444) {
            return 2;
        } else if (config == Bitmap.Config.ALPHA_8) {
            return 1;
        }
        return 1;
    }

    private static boolean canUseForInBitmap(
            Bitmap candidate, BitmapFactory.Options targetOptions) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // From Android 4.4 (KitKat) onward we can re-use if the byte size of
            // the new bitmap is smaller than the reusable bitmap candidate
            // allocation byte count.
            int width = targetOptions.outWidth / targetOptions.inSampleSize;
            int height = targetOptions.outHeight / targetOptions.inSampleSize;
            int byteCount = width * height * getBytesPerPixel(candidate.getConfig());

            return byteCount <= candidate.getAllocationByteCount();
        }

        // On earlier versions, the dimensions must match exactly and the inSampleSize must be 1
        return candidate.getWidth() == targetOptions.outWidth
                && candidate.getHeight() == targetOptions.outHeight
                && targetOptions.inSampleSize == 1;
    }

    private void initMemoryCache() {
        int cacheSize = mCacheParams.mMemCacheSize;

        mReusableBitmaps =
                Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());

        mMemoryCache = new LruCache<String, BitmapDrawable>(cacheSize) {
            @Override
            protected int sizeOf(String key, BitmapDrawable value) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return getBitmapSize(value) / 1024;
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, BitmapDrawable oldValue, BitmapDrawable newValue) {

                if (RecyclingBitmapDrawable.class.isInstance(oldValue)) {
                    ((RecyclingBitmapDrawable) oldValue).setIsCached(false);
                }

//                mReusableBitmaps.add
//                        (new SoftReference<Bitmap>(oldValue.getBitmap()));
            }
        };
    }

    private void initDiskCache() {
        File cacheDir = mCacheParams.mDiskCacheDir;
        new InitDiskCacheTask().execute(cacheDir);
    }

    public void addBitmapToCache(String key, BitmapDrawable value) {
        if (key == null || value == null) {
            return;
        }

        if (mMemoryCache != null) {
            if (RecyclingBitmapDrawable.class.isInstance(value)) {
                // The removed entry is a recycling drawable, so notify it
                // that it has been added into the memory cache
                ((RecyclingBitmapDrawable) value).setIsCached(true);
            }

            mMemoryCache.put(key, value);
        }

        // Also add to disk cache
        synchronized (mDiskCacheLock) {
            // Add to disk cache
            if (mDiskLruCache != null) {
                OutputStream out = null;
                try {
                    DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot == null) {
                        final DiskLruCache.Editor editor = mDiskLruCache
                                .edit(key);
                        if (editor != null) {
                            out = editor.newOutputStream(DISK_CACHE_INDEX);
                            value.getBitmap().compress(
                                    mCacheParams.mCompressFormat, mCacheParams.mCompressQuality, out);
                            editor.commit();
                            out.close();
                        }
                    } else {
                        snapshot.getInputStream(DISK_CACHE_INDEX).close();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "addBitmapToCache - " + e);
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public BitmapDrawable getBitmapFromMemCache(String key) {
        BitmapDrawable value = null;

        if (mMemoryCache != null) {
            value = mMemoryCache.get(key);
        }

        return value;
    }

    public Bitmap getBitmapFromDiskCache(String key) {
        Bitmap bitmap = null;

        synchronized (mDiskCacheLock) {
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (mDiskLruCache != null) {
                InputStream inputStream = null;
                try {
                    final DiskLruCache.Snapshot snapshot = mDiskLruCache
                            .get(key);
                    if (snapshot != null) {
                        inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
                        if (inputStream != null) {
                            FileDescriptor fd = ((FileInputStream) inputStream)
                                    .getFD();

                            // Decode bitmap, but we don't want to sample so
                            // give
                            // MAX_VALUE as the target dimensions
                            bitmap = ImageManager.decodeSampledBitmapFromDescriptor(
                                    fd, Integer.MAX_VALUE,
                                    Integer.MAX_VALUE, this);
//                            bitmap = ImageManager.decodeSampledBitmapFromDescriptor(
//                                    fd, Integer.MAX_VALUE,
//                                    Integer.MAX_VALUE);
                        }
                    }
                } catch (final IOException e) {
                    Log.e(TAG, "getBitmapFromDiskCache - " + e);
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return bitmap;
        }
    }

    public void addBitmapToResuableSet(Bitmap bitmap) {
        synchronized (mReusableBitmaps) {
            mReusableBitmaps.add(new SoftReference<>(bitmap));
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "addBitmapToResuableSet() add Bitmap width=" + bitmap.getWidth() + " height=" + bitmap.getHeight() + " size=" + mReusableBitmaps.size());
        }
    }

    // This method iterates through the reusable bitmaps, looking for one
    // to use for inBitmap:
    public Bitmap getBitmapFromReusableSet(BitmapFactory.Options options) {
        Bitmap bitmap = null;

        if (mReusableBitmaps != null && !mReusableBitmaps.isEmpty()) {
            synchronized (mReusableBitmaps) {
                final Iterator<SoftReference<Bitmap>> iterator
                        = mReusableBitmaps.iterator();
                Bitmap item;

                while (iterator.hasNext()) {
                    item = iterator.next().get();

                    if (null != item && item.isMutable()) {
                        // Check to see it the item can be used for inBitmap.
                        if (canUseForInBitmap(item, options)) {
                            bitmap = item;

                            // Remove from reusable set so it can't be used again.
                            iterator.remove();
                            break;
                        }
                    } else {
                        // Remove from the set if the reference has been cleared.
                        iterator.remove();
                    }
                }
            }
        }

        return bitmap;
    }

    /**
     * A holder class that contains cache parameters.
     */
    public static class ImageCacheParams {
        public File mDiskCacheDir;

        public ImageCacheParams(Context context, String diskCacheDirectoryName) {
            mDiskCacheDir = getDiskCacheDir(context, diskCacheDirectoryName);
        }

        public void setMemCacheSizePercent(float percent) {
            if (percent < 0.01f || percent > 0.8f) {
                throw new IllegalArgumentException("setMemCacheSizePercent - percent must be "
                        + "between 0.01 and 0.8 (inclusive)");
            }
            mMemCacheSize = Math.round(percent * Runtime.getRuntime().maxMemory() / 1024);
        }

        public int mMemCacheSize = DEFAULT_MEM_CACHE_SIZE;


        public int mDiskCacheSize = DEFAULT_DISK_CACHE_SIZE;


        public CompressFormat mCompressFormat = DEFAULT_COMPRESS_FORMAT;
        public int mCompressQuality = DEFAULT_COMPRESS_QUALITY;
        public boolean mMemoryCacheEnabled = DEFAULT_MEM_CACHE_ENABLED;
        public boolean mDiskCacheEnabled = DEFAULT_DISK_CACHE_ENABLED;
        public boolean mInitDiskCacheOnCreate = DEFAULT_INIT_DISK_CACHE_ON_CREATE;


    }

    /**
     * A simple non-UI Fragment that stores a single Object and is retained over configuration
     * changes. It will be used to retain the ImageCache object.
     */
    public static class RetainFragment extends Fragment {
        private Object mObject;

        /**
         * Empty constructor as per the Fragment documentation
         */
        public RetainFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Make sure this Fragment is retained over a configuration change
            setRetainInstance(true);
        }

        /**
         * Get the stored object.
         *
         * @return The stored object
         */
        public Object getObject() {
            return mObject;
        }

        /**
         * Store a single object in this Fragment.
         *
         * @param object The object to store
         */
        public void setObject(Object object) {
            mObject = object;
        }
    }

    class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... params) {
            synchronized (mDiskCacheLock) {
                if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
                    File diskCacheDir = params[0];
                    if (mCacheParams.mDiskCacheEnabled && diskCacheDir != null) {
                        if (!diskCacheDir.exists()) {
                            diskCacheDir.mkdirs();
                        }
                        if (getUsableSpace(diskCacheDir) > mCacheParams.mDiskCacheSize) {
                            try {
                                int versionCode = GalleryContext.getInstance().getVersionCode();
                                mDiskLruCache = DiskLruCache.open(
                                        diskCacheDir, versionCode, 1, mCacheParams.mDiskCacheSize);
                                if (BuildConfig.DEBUG) {
                                    Log.d(TAG, "Disk cache initialized");
                                }
                            } catch (final IOException e) {
                                mCacheParams.mDiskCacheDir = null;
                                Log.e(TAG, "initDiskCache - " + e);
                            }
                        }
                    }
                }
                mDiskCacheStarting = false;
                mDiskCacheLock.notifyAll();
            }
            return null;
        }
    }
}
