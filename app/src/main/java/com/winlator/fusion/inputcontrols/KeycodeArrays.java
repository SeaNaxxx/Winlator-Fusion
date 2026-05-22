package com.winlator.fusion.inputcontrols;

import android.content.Context;
import android.content.res.TypedArray;
import com.winlator.fusion.R;

public final class KeycodeArrays {
    private KeycodeArrays() {}

    public static int[] loadButtonKeycodes(Context ctx) {
        TypedArray arr = ctx.getResources().obtainTypedArray(R.array.button_keycodes);
        try {
            int[] out = new int[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                int resId = arr.getResourceId(i, 0);
                out[i] = resId != 0 ? ctx.getResources().getInteger(resId) : 0;
            }
            return out;
        } finally {
            arr.recycle();
        }
    }
}
