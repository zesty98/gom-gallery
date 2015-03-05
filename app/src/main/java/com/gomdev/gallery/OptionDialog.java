package com.gomdev.gallery;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

/**
 * Created by gomdev on 15. 3. 5..
 */
public class OptionDialog extends DialogFragment {
    static final String CLASS = "ShaderListDialog";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    private final int OPTION_DELETE = 0;
    private final int OPTION_SHARE = 1;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] list = new String[]{
                "Delete",
                "Share"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.option_dialog_title)
                .setItems(list, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GalleryContext galleryContext = GalleryContext.getInstance();
                        ImageIndexingInfo imageIndexingInfo = galleryContext.getImageIndexingInfo();
                        if (which == OPTION_DELETE) {
                            boolean isBucketDeleted = ImageManager.getInstance().deleteImage(imageIndexingInfo);

                            if (isBucketDeleted == true) {
                                Intent intent = new Intent(getActivity(), MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                            } else {
                                galleryContext.setImageViewMode(GalleryContext.ImageViewMode.ALBUME_VIEW_MODE);
                                Intent intent = new Intent(getActivity(), ImageListActivity.class);
                                intent.putExtra(GalleryConfig.BUCKET_INDEX, imageIndexingInfo.mBucketIndex);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                            }
                        } else if (which == OPTION_SHARE) {
                            ImageInfo imageInfo = ImageManager.getInstance().getImageInfo(imageIndexingInfo);
                            Intent shareIntent = new Intent();
                            shareIntent.setAction(Intent.ACTION_SEND);
                            Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageInfo.getImageID());
                            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);

                            shareIntent.setType("image/*");
                            startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));
                        }
                    }
                });
        return builder.create();
    }
}