package com.gomdev.gallery;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.gomdev.gallery.GalleryConfig.SortBy;

/**
 * Created by gomdev on 15. 3. 5..
 */
public class AlbumViewSortOptionDialog extends DialogFragment {
    static final String CLASS = "AlbumViewOptionDialog";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int OPTION_DESC = 0;
    private final int OPTION_ASC = 1;

    private int mSelectedIndex = -1;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] list = new String[]{
                "Descending time",
                "Ascending time"
        };

        final SharedPreferences pref = getActivity().getSharedPreferences(GalleryConfig.PREF_NAME, 0);
        final int sortBy = pref.getInt(GalleryConfig.PREF_SORT_BY, SortBy.DESCENDING.getIndex());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.albumview_sort_option_dialog_title)
                .setSingleChoiceItems(list, sortBy, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSelectedIndex = which;
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mSelectedIndex == OPTION_DESC) {
                            if (SortBy.DESCENDING.getIndex() != sortBy) {
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putInt(GalleryConfig.PREF_SORT_BY, SortBy.DESCENDING.getIndex());
                                editor.commit();

                                int bucketIndex = getChangedBucketIndex();

                                GalleryContext.newInstance(getActivity());

                                Intent intent = new Intent(getActivity(), ImageListActivity.class);
                                intent.putExtra(GalleryConfig.BUCKET_INDEX, bucketIndex);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                        Intent.FLAG_ACTIVITY_NEW_TASK);
                                getActivity().startActivity(intent);
                            }
                        } else if (mSelectedIndex == OPTION_ASC) {
                            if (SortBy.ASCENDING.getIndex() != sortBy) {
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putInt(GalleryConfig.PREF_SORT_BY, SortBy.ASCENDING.getIndex());
                                editor.commit();

                                int bucketIndex = getChangedBucketIndex();

                                GalleryContext.newInstance(getActivity());

                                Intent intent = new Intent(getActivity(), ImageListActivity.class);
                                intent.putExtra(GalleryConfig.BUCKET_INDEX, bucketIndex);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                getActivity().startActivity(intent);
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                            }
                        });

        return builder.create();
    }

    private int getChangedBucketIndex() {
        ImageManager imageManager = ImageManager.getInstance();
        BucketInfo bucketInfo = imageManager.getCurrentBucketInfo();
        int bucketIndex = bucketInfo.getIndex();
        int numOfBuckets = imageManager.getNumOfBucketInfos();
        bucketIndex = numOfBuckets - bucketIndex - 1;
        return bucketIndex;
    }
}