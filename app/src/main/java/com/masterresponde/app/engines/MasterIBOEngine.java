package com.masterresponde.app.engines;

import android.content.Context;
import android.content.Intent;

public class MasterIBOEngine {
    public static void start(Context context){
        try {
            Class<?> cls = Class.forName("com.masteribo.app.MainActivity");
            context.startActivity(new Intent(context, cls));
        } catch(Exception ignored){}
    }
}
