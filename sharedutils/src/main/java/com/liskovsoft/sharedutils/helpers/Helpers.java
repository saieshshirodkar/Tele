package com.liskovsoft.sharedutils.helpers;

import android.view.View;

public class Helpers {
    public static void describedBy(View v, int id) {
        v.setContentDescription(v.getContext().getString(id));
    }
}
