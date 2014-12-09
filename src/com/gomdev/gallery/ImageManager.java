package com.gomdev.gallery;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.gomdev.gles.GLESUtils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

public class ImageManager {
    static final String CLASS = "ImageManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private static final int DISK_CACHE_INDEX = 0;

    private static ImageManager sImageManager = null;
    private static Bitmap sPlaceHolderThumbnail = null;

    public static void newInstance(Context context) {
        sImageManager = new ImageManager(context);
    }

    public static ImageManager getInstance() {
        return sImageManager;
    }

    static {
        sPlaceHolderThumbnail = GLESUtils.makeBitmap(512, 384,
                Config.ARGB_8888,
                Color.BLACK);
    }

    private final Context mContext;
    private int mNumOfImages = 0;
    private int mNumOfBuckets = 0;
    private ArrayList<BucketInfo> mBuckets = new ArrayList<BucketInfo>();

    private LruCache<String, Bitmap> mMemoryCache;

    private DiskLruCache mDiskLruCache;
    private final Object mDiskCacheLock = new Object();
    private boolean mDiskCacheStarting = true;
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
    private static final String DISK_CACHE_SUBDIR = "thumbnails";

    private ImageManager(Context context) {
        mContext = context;

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 4;

        Log.d(TAG, "ImageManager() maxMemory=" + (maxMemory / 1024)
                + " cacheSize="
                + (cacheSize / 1024));

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };

        File cacheDir = getDiskCacheDir(mContext, DISK_CACHE_SUBDIR);
        new InitDiskCacheTask().execute(cacheDir);

        loadFolderInfo();

        for (BucketInfo bucketInfo : mBuckets) {
            loadImageInfoFromBucketInfo(bucketInfo);
        }

        if (DEBUG) {
            Log.d(TAG, "ImageManager() BucketInfos");
            for (BucketInfo bucketInfo : mBuckets) {
                Log.d(TAG,
                        "\t name=" + bucketInfo.getName() + " num="
                                + bucketInfo.getNumOfImageInfos());
            }
        }
    }

    class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... params) {
            synchronized (mDiskCacheLock) {
                int versionCode = GalleryContext.getInstance().getVersionCode();
                File cacheDir = params[0];
                try {
                    mDiskLruCache = DiskLruCache.open(cacheDir, versionCode, 1,
                            DISK_CACHE_SIZE);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mDiskCacheStarting = false; // Finished initialization
                mDiskCacheLock.notifyAll(); // Wake any waiting threads
            }
            return null;
        }
    }

    public void addBitmapToCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
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
                            bitmap.compress(
                                    CompressFormat.JPEG, 70, out);
                            editor.commit();
                            out.close();
                        }
                    } else {
                        snapshot.getInputStream(DISK_CACHE_INDEX).close();
                    }
                } catch (final IOException e) {
                    Log.e(TAG, "addBitmapToCache - " + e);
                } catch (Exception e) {
                    Log.e(TAG, "addBitmapToCache - " + e);
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    public Bitmap getBitmapFromDiskCache(String key) {
        Bitmap bitmap = null;

        synchronized (mDiskCacheLock) {
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait();
                } catch (InterruptedException e) {
                }
            }
            if (mDiskLruCache != null) {
                InputStream inputStream = null;
                try {
                    final DiskLruCache.Snapshot snapshot = mDiskLruCache
                            .get(key);
                    if (snapshot != null) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Disk cache hit");
                        }
                        inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
                        if (inputStream != null) {
                            FileDescriptor fd = ((FileInputStream) inputStream)
                                    .getFD();

                            // Decode bitmap, but we don't want to sample so
                            // give
                            // MAX_VALUE as the target dimensions
                            bitmap = decodeSampledBitmapFromDescriptor(
                                    fd, Integer.MAX_VALUE,
                                    Integer.MAX_VALUE);
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
                    }
                }
            }
            return bitmap;
        }
    }

    private void loadFolderInfo() {
        String[] projection = {
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        };

        long tick = System.nanoTime();
        // CursorLoader cursorLoader = new CursorLoader(mContext,
        // MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
        // null, null, null);
        // Cursor cursor = cursorLoader.loadInBackground();

        Cursor cursor = mContext.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                null, null);

        Log.d(TAG, "loadFolderInfo() duration=" + (System.nanoTime() - tick));

        mNumOfImages = cursor.getCount();

        Set<Integer> buckets = new HashSet<Integer>();

        if (cursor.moveToFirst() == true) {
            do {
                int columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
                int bucketID = cursor.getInt(columnIndex);

                columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                String bucketName = cursor.getString(columnIndex);

                if (buckets.add(bucketID) == true) {
                    BucketInfo bucketInfo = new BucketInfo(bucketID);
                    bucketInfo.setName(bucketName);
                    mBuckets.add(bucketInfo);
                }

            } while (cursor.moveToNext());

            mNumOfBuckets = mBuckets.size();
        }

        cursor.close();
    }

    private void loadImageInfoFromBucketInfo(BucketInfo bucketInfo) {
        int bucketID = bucketInfo.getID();
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.ORIENTATION,
        };

        Cursor cursor = mContext.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Images.Media.BUCKET_ID + " = ? ",
                new String[] { String.valueOf(bucketID)
                },
                null);

        String bucketName = null;

        int index = 0;
        if (cursor.moveToFirst() == true) {
            do {
                int columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                long imageID = cursor.getLong(columnIndex);

                columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                String imagePath = cursor.getString(columnIndex);

                columnIndex = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION);
                int orientation = cursor.getInt(columnIndex);

                ImageInfo imageInfo = new ImageInfo(index, orientation);
                imageInfo.setImagePath(imagePath);
                imageInfo.setImageID(imageID);
                bucketInfo.add(imageInfo);

                if (DEBUG) {
                    Log.d(TAG, index + " : imageID=" + imageID + " imagePath="
                            + imagePath + " orientation=" + orientation
                            + " bucket=" + bucketName);
                }

                index++;
            } while (cursor.moveToNext());
        }

        cursor.close();
    }

    public int getNumOfImages() {
        return mNumOfImages;
    }

    public int getNumOfBucksets() {
        return mNumOfBuckets;
    }

    public BucketInfo getBucketInfo(int index) {
        return mBuckets.get(index);
    }

    // public ImageInfo getImageInfo(int index) {
    // return mAllImages[index];
    // }

    public void loadThumbnail(ImageInfo imageInfo, ImageView imageView) {
        final String imageKey = String.valueOf(imageInfo.getImageID());
        final Bitmap bitmap = getBitmapFromMemCache(imageKey);

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            if (DEBUG) {
                Log.d(TAG, "memory cache hit " + imageInfo.getImagePath());
            }
        } else {

            if (cancelPotentialWork(imageInfo, imageView)) {
                final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
                final AsyncDrawable asyncDrawable =
                        new AsyncDrawable(mContext.getResources(),
                                sPlaceHolderThumbnail, task);
                imageView.setImageDrawable(asyncDrawable);
                task.execute(imageInfo);
            }
        }
    }

    public void loadBitmap(ImageInfo imageInfo, ImageView imageView,
            int requestWidth, int requestHeight) {
        if (cancelPotentialWork(imageInfo, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView,
                    requestWidth, requestHeight);

            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(mContext.getResources(),
                            sPlaceHolderThumbnail, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(imageInfo);
        }
    }

    public Bitmap getThumbnail(ImageInfo imageInfo,
            boolean forcePortrait) {
        long imageID = imageInfo.getImageID();

        Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                mContext.getContentResolver(), imageID,
                MediaStore.Images.Thumbnails.MINI_KIND,
                (BitmapFactory.Options) null);

        if (bitmap == null) {
            bitmap = decodeSampledBitmap(imageInfo, 512, 512);
        }

        if (forcePortrait == true) {
            bitmap = rotate(bitmap, imageInfo.getOrientation());
        }

        return bitmap;
    }

    public Bitmap getBitmap(ImageInfo imageInfo, int requestWidth,
            int requestHeight, boolean forcePortrait) {
        int orientation = imageInfo.getOrientation();
        if (orientation == 90 || orientation == 270) {
            int temp = requestWidth;
            requestWidth = requestHeight;
            requestHeight = temp;
        }

        Bitmap bitmap = decodeSampledBitmap(imageInfo, requestWidth,
                requestHeight);

        if (forcePortrait == true && orientation != 0) {
            bitmap = rotate(bitmap, orientation);
        }

        return bitmap;
    }

    public static Bitmap rotate(Bitmap b, int degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees,
                    (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(
                        b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError ex) {
                // We have no memory to rotate. Return the original bitmap.
            }
        }
        return b;
    }

    public static boolean cancelPotentialWork(ImageInfo imageInfo,
            ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final ImageInfo bitmapData = bitmapWorkerTask.getData();
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == null || bitmapData != imageInfo) {
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

    public static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public static Bitmap decodeSampledBitmap(ImageInfo imageInfo,
            int reqWidth, int reqHeight) {
        String path = imageInfo.getImagePath();

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    public static Bitmap decodeSampledBitmapFromDescriptor(
            FileDescriptor fileDescriptor, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory
                .decodeFileDescriptor(fileDescriptor, null, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    // Creates a unique subdirectory of the designated app cache directory.
    // Tries to use external
    // but if not mounted, falls back on internal storage.
    public static File getDiskCacheDir(Context context, String uniqueName) {
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

    class BitmapWorkerTask extends AsyncTask<ImageInfo, Void, Bitmap> {

        private final WeakReference<ImageView> mImageViewReference;
        private ImageInfo mImageInfo = null;
        private boolean mNeedThumbnail = true;

        private int mRequestWidth = 0;
        private int mRequestHeight = 0;

        public BitmapWorkerTask(ImageView imageView) {
            mNeedThumbnail = true;
            mImageViewReference = new WeakReference<ImageView>(imageView);
        }

        public BitmapWorkerTask(ImageView imageView, int requestWidth,
                int requestHeight) {
            mNeedThumbnail = false;
            mRequestWidth = requestWidth;
            mRequestHeight = requestHeight;
            mImageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(ImageInfo... params) {
            mImageInfo = params[0];

            ImageManager mImageManager = ImageManager.getInstance();
            Bitmap bitmap = null;
            if (mNeedThumbnail == true) {
                String imageKey = String.valueOf(mImageInfo.getImageID());

                bitmap = getBitmapFromDiskCache(imageKey);

                if (bitmap == null) {
                    bitmap = mImageManager.getThumbnail(mImageInfo, true);
                } else {
                    if (DEBUG) {
                        Log.d(TAG,
                                "disk cache hit " + mImageInfo.getImagePath());
                    }
                }

                mImageManager.addBitmapToCache(imageKey, bitmap);
            } else {
                bitmap = mImageManager.getBitmap(mImageInfo, mRequestWidth,
                        mRequestHeight, true);
            }

            return bitmap;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (mImageViewReference != null && bitmap != null) {
                final ImageView imageView = mImageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask =
                        ImageManager.getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }

        public ImageInfo getData() {
            return mImageInfo;
        }
    }
}
