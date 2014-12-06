package com.gomdev.gallery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.gomdev.gles.GLESUtils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

public class ImageManager {
    static final String CLASS = "ImageManager";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

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

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
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
}
