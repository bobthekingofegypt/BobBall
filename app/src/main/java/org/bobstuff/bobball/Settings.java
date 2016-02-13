package org.bobstuff.bobball;

import android.content.Context;

public class Settings {

    public static void setDefaultName (String defaultName){
        Preferences.saveValue("defaultName",defaultName);
    }

    public static String getDefaultName (){
        Context context = Preferences.getContext();
        return Preferences.loadValue("defaultName",context.getString(R.string.defaultName));
    }

    public static void setNumPlayers (int numPlayers) {
        Preferences.saveValue("numPlayers", "" + numPlayers);
    }

    public static int getNumPlayers (){
        return Integer.parseInt(Preferences.loadValue("numPlayers","1"));
    }


    public static void setLevelSelectionType (int type) {
        Preferences.saveValue("levelSelectionType", "" + type);
    }

    public static int getLevelSelectionType (){
        return Integer.parseInt(Preferences.loadValue("levelSelectionType", "0"));
    }


    public static void setSelectedLevel (int level) {
        Preferences.saveValue("selectedLevel","" + level);
    }

    public static int getSelectLevel (){
        return Integer.parseInt(Preferences.loadValue("selectedLevel", "1"));
    }


    public static void setLastLevelFailed (int lastLevel){
        Preferences.saveValue("lastLevelFailed","" + lastLevel);
    }

    public static int getLastLevelFailed (){
        return Integer.parseInt(Preferences.loadValue("lastLevelFailed","0"));
    }


    public static void setRetryAction (int retryAction){
        Preferences.saveValue("retryAction","" + retryAction);
    }

    public static int getRetryAction (){
        return Integer.parseInt(Preferences.loadValue("retryAction","0"));
    }
}
