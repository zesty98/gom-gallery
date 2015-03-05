package com.gomdev.gallery;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * Created by gomdev on 15. 3. 5..
 */
public class OptionDialog extends DialogFragment {
    static final String CLASS = "ShaderListDialog";
    static final String TAG = GalleryConfig.TAG + "_" + CLASS;
    static final boolean DEBUG = GalleryConfig.DEBUG;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] list = new String[]{
                "Delete"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.option_dialog_title)
                .setItems(list, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GalleryContext galleryContext = GalleryContext.getInstance();
                        ImageIndexingInfo imageIndexingInfo = galleryContext.getImageIndexingInfo();

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
                    }
                });
        return builder.create();
    }
}