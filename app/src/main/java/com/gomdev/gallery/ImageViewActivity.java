package com.gomdev.gallery;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

class ImageViewActivity extends FragmentActivity implements View.OnClickListener {
    static final String CLASS = "ImageViewActivity";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private ImagePagerAdapter mAdapter;
    private ViewPager mPager;

    private ImageManager mImageManager = null;

    private BucketInfo mBucketInfo = null;
    private DateLabelInfo mDateLabelInfo = null;

    private ImageIndexingInfo mCurrentImageIndexingInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_detail_pager);

        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int bucketIndex = getIntent().getIntExtra(GalleryConfig.BUCKET_INDEX, 0);
        int dateLabelIndex = getIntent().getIntExtra(GalleryConfig.DATE_LABEL_INDEX, 0);
        int imageIndex = getIntent().getIntExtra(GalleryConfig.IMAGE_INDEX, 0);

        mCurrentImageIndexingInfo = new ImageIndexingInfo(bucketIndex, dateLabelIndex, imageIndex);


        GalleryContext galleryContext = GalleryContext.getInstance();
        if (galleryContext == null) {
            GalleryContext.newInstance(this);
        }

        mImageManager = ImageManager.getInstance();
        mBucketInfo = mImageManager.getBucketInfo(bucketIndex);

        mDateLabelInfo = mBucketInfo.get(dateLabelIndex);

        mAdapter = new ImagePagerAdapter(getSupportFragmentManager(), mBucketInfo.getNumOfImages());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setOffscreenPageLimit(2);

        mPager.setOnPageChangeListener(mOnPageChangeListener);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            final ActionBar actionBar = getActionBar();

            // Hide title text and set home as up
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(mDateLabelInfo.getDate());

            // Hide and show the ActionBar as the visibility changes
            mPager.setOnSystemUiVisibilityChangeListener(
                    new View.OnSystemUiVisibilityChangeListener() {
                        @Override
                        public void onSystemUiVisibilityChange(int vis) {
                            if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
                                actionBar.hide();
                            } else {
                                actionBar.show();
                            }
                        }
                    });

            mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            actionBar.hide();

            int indexInBucket = mImageManager.getImageIndex(mCurrentImageIndexingInfo);

            mPager.setCurrentItem(indexInBucket);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onClick(View v) {
        final int vis = mPager.getSystemUiVisibility();
        if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
            mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } else {
            mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            boolean isBucketDeleted = ImageManager.getInstance().deleteImage(mCurrentImageIndexingInfo);
            if (isBucketDeleted == false) {
                finish();
            } else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ImagePagerAdapter extends FragmentStatePagerAdapter {
        private final int mSize;

        ImagePagerAdapter(FragmentManager fm, int size) {
            super(fm);
            mSize = size;
        }

        @Override
        public int getCount() {
            return mSize;
        }

        @Override
        public Fragment getItem(int position) {
            ImageIndexingInfo indexingInfo = mImageManager.getImageIndexingInfo(position);
            ImageInfo imageInfo = mImageManager.getImageInfo(indexingInfo);
            return ImageViewFragment.newInstance(imageInfo);
        }
    }

    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i2) {

        }

        @Override
        public void onPageSelected(int i) {
            mCurrentImageIndexingInfo = mImageManager.getImageIndexingInfo(i);

            DateLabelInfo dateLabelInfo = mBucketInfo.get(mCurrentImageIndexingInfo.mDateLabelIndex);
            getActionBar().setTitle(dateLabelInfo.getDate());

            SharedPreferences pref = ImageViewActivity.this.getSharedPreferences(GalleryConfig.PREF_NAME, 0);
            SharedPreferences.Editor editor = pref.edit();
            editor.putInt(GalleryConfig.PREF_IMAGE_INDEX, mCurrentImageIndexingInfo.mImageIndex);
            editor.putInt(GalleryConfig.PREF_DATE_LABEL_INDEX, mCurrentImageIndexingInfo.mDateLabelIndex);
            editor.putInt(GalleryConfig.PREF_BUCKET_INDEX, mCurrentImageIndexingInfo.mBucketIndex);
            editor.commit();
        }

        @Override
        public void onPageScrollStateChanged(int i) {

        }
    };
}
