package com.gomdev.gallery;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ImageViewFragment extends Fragment {
    static final String CLASS = "ImageViewFragment";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private ImageView mImageView = null;

    private ImageManager mImageManager = null;
    private ImageInfo mImageInfo = null;

    private int mWidth = 0;
    private int mHeight = 0;

    public ImageViewFragment() {
    }

    public static ImageViewFragment newInstance(ImageInfo imageInfo) {
        final ImageViewFragment fragment = new ImageViewFragment();

        final Bundle args = new Bundle();
        args.putSerializable(GalleryConfig.IMAGE_VIEW_DATA, imageInfo);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mImageInfo = (ImageInfo) getArguments().getSerializable(GalleryConfig.IMAGE_VIEW_DATA);
        }

        DisplayMetrics matric = getActivity().getResources().getDisplayMetrics();
        mWidth = matric.widthPixels / 2;
        mHeight = matric.heightPixels / 2;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG) {
            Log.d(TAG, "onCreateView()");
        }

        mImageManager = ImageManager.getInstance();
        mImageManager.setLoadingBitmap(null);

        final View v = inflater.inflate(R.layout.image_detail_fragment, container, false);
        mImageView = (ImageView) v.findViewById(R.id.imageView);

        return v;
    }

    @Override
    public void onResume() {
        if (DEBUG) {
            Log.d(TAG, "onResume()");
        }
        super.onResume();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Use the parent activity to load the image asynchronously into the ImageView (so a single
        // cache can be used over all pages in the ViewPager
        if (ImageViewActivity.class.isInstance(getActivity())) {
            mImageManager = ImageManager.getInstance();
            mImageManager.loadBitmap(mImageInfo, mImageView, mWidth, mHeight);
        }

        // Pass clicks on the ImageView to the parent activity to handle
        if (View.OnClickListener.class.isInstance(getActivity()) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mImageView.setOnClickListener((View.OnClickListener) getActivity());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mImageView != null) {
            ImageManager.cancelWork(mImageView);
            mImageView.setImageDrawable(null);
        }
    }
}
