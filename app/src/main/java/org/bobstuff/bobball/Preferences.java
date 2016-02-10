package org.bobstuff.bobball;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

/*
* simple global load and save using shared preferences
* you have to use setContext before using load or save
*/

public class Preferences extends Application{

    private static Context context;

    public static void setContext (Context ctxt)
    {
        context = ctxt;
    }

    public static String loadValue (String filename, String defaultValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(filename, Context.MODE_PRIVATE);
        return sharedPref.getString (filename, defaultValue);
    }

    public static void saveValue (String filename, String value) {
        SharedPreferences sharedPref = context.getSharedPreferences(filename, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(filename, value);
        editor.commit();
    }
}
