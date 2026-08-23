package com.masterresponde.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

public final class IconAssets {
    private IconAssets() {}

    private static Bitmap decode(String data) {
        byte[] bytes = Base64.decode(data, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public static Bitmap robot() { return decode("PLACEHOLDER_ROBOT"); }
    public static Bitmap shield() { return decode("PLACEHOLDER_SHIELD"); }
    public static Bitmap bell() { return decode("PLACEHOLDER_BELL"); }
    public static Bitmap accessibility() { return decode("PLACEHOLDER_ACCESSIBILITY"); }
    public static Bitmap power() { return decode("PLACEHOLDER_POWER"); }
    public static Bitmap whatsapp() { return decode("PLACEHOLDER_WHATSAPP"); }
}
