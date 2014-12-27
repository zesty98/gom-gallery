package com.gomdev.gallery;

import android.content.Context;
import android.util.TypedValue;

/**
 * Created by gomdev on 14. 12. 27..
 */
public class GalleryUtils {
    public static int getActionBarHeight(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(
                android.R.attr.actionBarSize, tv, true)) {
            int actionBarHeight = TypedValue.complexToDimensionPixelSize(
                    tv.data, context.getResources().getDisplayMetrics());

            return actionBarHeight;

        }

        return 0;
    }
}
