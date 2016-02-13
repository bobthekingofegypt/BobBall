package org.bobstuff.bobball;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

/*
* simple load and save using shared preferences
* you have to use setContext before using load or save in an activity
* you should use setContext in onCreate()
 */

public class Preferences extends Application {

    private static SharedPreferences sharedPreferences;
    private static Context appContext;

    public static void setContext (Context context)
    {
        appContext = context;
        sharedPreferences = context.getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE);
    }

    public static Context getContext () {
        return appContext;
    }

    public static String loadValue (String filename, String defaultValue) {
        return sharedPreferences.getString (filename, defaultValue);
    }

    public static void saveValue (String filename, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(filename, value);
        editor.commit();
    }
}
