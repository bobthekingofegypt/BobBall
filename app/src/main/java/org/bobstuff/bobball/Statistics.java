package org.bobstuff.bobball;

public class Statistics {

    public static void setHighestLevel (int levelReached) {
        int topLevel = getHighestLevel();

        if (topLevel < levelReached) {
            Preferences.saveValue("level", "" + levelReached);
        }
    }

    public static int getHighestLevel() {
        return Integer.parseInt(Preferences.loadValue("level","1"));
    }
}
