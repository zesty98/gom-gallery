package com.gomdev.gallery;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class ImageViewFragment extends Fragment {
    public ImageViewFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_image_view, container,
                false);

        ImageView imageView = (ImageView) rootView.findViewById(R.id.image);
        ImageInfo imageInfo = GalleryContext.getInstance().getCurrentImageInfo();
        
        DisplayMetrics matric = getActivity().getResources().getDisplayMetrics();
        int width = matric.widthPixels;
        int height = matric.heightPixels;
        ImageManager.getInstance().loadBitmap(imageInfo, imageView, width, height);


        return rootView;
    }

    public class ImageGridAdapter extends BaseAdapter {
        static final String CLASS = "ImageAdapter";
        static final String TAG = GalleryConfig.TAG + "_" + CLASS;
        static final boolean DEBUG = GalleryConfig.DEBUG;

        private final LayoutInflater mInflater;
        private final ImageManager mImageManager;
        private final BucketInfo mBucketInfo;
        private int mNumOfImages = 0;

        public ImageGridAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
            
            mImageManager = ImageManager.getInstance();

            mBucketInfo = GalleryContext.getInstance().getCurrentBucketInfo();
            mNumOfImages = mBucketInfo.getNumOfImageInfos();
        }

        public int getCount() {
            return mNumOfImages;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return position;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            FrameLayout layout;

            if (convertView == null) {
                layout = (FrameLayout) mInflater.inflate(
                        R.layout.grid_item_image,
                        parent, false);
            } else {
                layout = (FrameLayout) convertView;

            }
            ImageView imageView = (SquareImageView) layout
                    .findViewById(R.id.image);
            ImageInfo imageInfo = mBucketInfo.get(position);
            mImageManager.loadThumbnail(imageInfo, imageView);

            return layout;
        }
    }
}
