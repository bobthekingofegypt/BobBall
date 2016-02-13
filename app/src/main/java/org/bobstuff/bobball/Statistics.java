package org.bobstuff.bobball;

public class Statistics {

    // Highest Level reached
    public static void setHighestLevel (int numPlayers, int levelReached) {
        String valueName = "level";

        if (numPlayers > 1){
            valueName = "levelBotMode";
        }

        Preferences.saveValue(valueName, "" + levelReached);
    }

    public static int getHighestLevel(int numPlayers) {
        String valueName = "level";

        if (numPlayers > 1){
            valueName = "levelBotMode";
        }

        return Integer.parseInt(Preferences.loadValue(valueName,"1"));
    }

    public static int getTopLevel () {
        int topLevel = 1;
        for (int i = 1; i <= 3; i++) {
            int tmp_level = getHighestLevel(i);
            if (topLevel < tmp_level){
                topLevel = tmp_level;
            }
        }

        return topLevel;
    }

    public static void saveHighestLevel (int numPlayers, int levelReached) {
        int topLevel = getHighestLevel(numPlayers);

        if (topLevel < levelReached) {
            setHighestLevel(numPlayers, levelReached);
        }
    }

    // Played Games Counter
    public static void setPlayedGames (int playedGames) {
        Preferences.saveValue("playedGames", "" + playedGames);
    }

    public static int getPlayedGames () {
        return Integer.parseInt(Preferences.loadValue("playedGames","0"));
    }

    public static void increasePlayedGames (){
        setPlayedGames(getPlayedGames() + 1);
    }

    // Highest number of levels played in a row without loosing
    public static void setLongestSeries (int longestSeries) {
        Preferences.saveValue("longestSeries", "" + longestSeries);
    }

    public static int getLongestSeries (){
        return Integer.parseInt(Preferences.loadValue("longestSeries", "0"));
    }

    public static void saveLongestSeries (int longestSeries) {
        int currentLongestSeries = getLongestSeries();

        if (currentLongestSeries < longestSeries)
        {
            setLongestSeries(longestSeries);
        }
    }


    // highest score in a single level
    public static void setHighestLevelScore (int highestLevelScore){
        Preferences.saveValue("highestLevelScore","" + highestLevelScore);
    }

    public static int getHighestLevelScore (){
        return Integer.parseInt(Preferences.loadValue("highestLevelScore","0"));
    }

    public static void saveHighestLevelScore (int highestLevelScore) {
        int currentHighestLevelScore = getHighestLevelScore();

        if (currentHighestLevelScore < highestLevelScore){
            setHighestLevelScore(highestLevelScore);
        }
    }

    public static int getTopScore () {
        int topScore = 0;

        for (int i = 1; i <= 3; i++){
            Scores scores = new Scores (i);
            scores.loadScores();
            int bestScore = scores.getBestScore();

            if (topScore < bestScore){
                topScore = bestScore;
            }
        }

        return topScore;
    }

    // most time left after finishing a level

    public static void setTimeLeftRecord(int timeLeft){
        Preferences.saveValue("timeLeftRecord","" + timeLeft);
    }

    public static int getTimeLeftRecord(){
        return Integer.parseInt(Preferences.loadValue("timeLeftRecord","0"));
    }

    public static void saveTimeLeftRecord (int timeLeft){
        int currentRecord = getTimeLeftRecord();

        if (currentRecord < timeLeft){
            setTimeLeftRecord(timeLeft);
        }
    }

    // highest percentage cleared for one level

    public static void setPercentageClearedRecord(int percentageCleared){
        Preferences.saveValue("percentageCleared","" + percentageCleared);
    }

    public static int getPercentageClearedRecord(){
        return Integer.parseInt(Preferences.loadValue("percentageCleared","0"));
    }

    public static void savePercentageClearedRecord (int percentageCleared){
        int currentRecord = getPercentageClearedRecord();

        if (currentRecord < percentageCleared){
            setPercentageClearedRecord(percentageCleared);
        }
    }

    // most lives left after one level

    public static void setLivesLeftRecord(int livesLeft){
        Preferences.saveValue("livesLeft","" + livesLeft);
    }

    public static int getLivesLeftRecord(){
        return Integer.parseInt(Preferences.loadValue("livesLeft","0"));
    }

    public static void saveLivesLeftRecord(int livesLeft){
        int currentRecord = getLivesLeftRecord();

        if (currentRecord < livesLeft){
            setLivesLeftRecord(livesLeft);
        }
    }

    // least time left after a single level

    public static void setLeastTimeLeft (int timeLeft){
        Preferences.saveValue("leastTimeLeft","" + timeLeft);
    }

    public static int getLeastTimeLeft (){
        return Integer.parseInt(Preferences.loadValue("leastTimeLeft","1000000"));
    }

    public static void saveLeastTimeLeft (int timeLeft){
        int currentRecord = getLeastTimeLeft();

        if (currentRecord > timeLeft){
            setLeastTimeLeft(timeLeft);
        }
    }
}
