package com.masterresponde.app.engines;

import android.content.Context;
import android.content.Intent;

public class AutomationCloudEngine {
    public static void start(Context context){
        try {
            Class<?> cls = Class.forName("com.automationcloud.app.MainActivity");
            context.startActivity(new Intent(context, cls));
        } catch(Exception ignored){}
    }
}
