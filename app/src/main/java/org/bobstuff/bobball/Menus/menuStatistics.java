package org.bobstuff.bobball.Menus;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.bobstuff.bobball.R;
import org.bobstuff.bobball.Statistics;

public class menuStatistics extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.menu_statistics);

        String gamesPlayedText = getString(R.string.gamesPlayedText, Statistics.getPlayedGames());
        String highestLevelText = getString(R.string.highestLevelText, Statistics.getTopLevel());
        String levelSeriesText = getString(R.string.levelSeriesText, Statistics.getLongestSeries());
        String highestLevelScoreText = getString(R.string.highestLevelScoreText, Statistics.getHighestLevelScore());
        String topScoreText = getString(R.string.topScoreText, Statistics.getTopScore());
        String timeLeftRecordText = getString(R.string.timeLeftRecordText, Statistics.getTimeLeftRecord());
        String percentageClearedRecordText = getString(R.string.percentageClearedText, Statistics.getPercentageClearedRecord());
        String livesLeftRecordText = getString(R.string.livesLeftRecordText, Statistics.getLivesLeftRecord());
        String leastTimeLeftText = getString(R.string.leastTimeLeftText, Statistics.getLeastTimeLeft());

        setTextViewText(R.id.gamesPlayed, gamesPlayedText);
        setTextViewText(R.id.highestLevel, highestLevelText);
        setTextViewText(R.id.levelSeries, levelSeriesText);
        setTextViewText(R.id.highestLevelScore, highestLevelScoreText);
        setTextViewText(R.id.topScore, topScoreText);
        setTextViewText(R.id.timeLeftRecord, timeLeftRecordText);
        setTextViewText(R.id.percentageClearedRecord, percentageClearedRecordText);
        setTextViewText(R.id.livesLeftRecord,livesLeftRecordText);
        setTextViewText(R.id.leastTimeLeft,leastTimeLeftText);
    }

    protected void setTextViewText (int textViewId, String text){
        TextView textView = (TextView) findViewById(textViewId);
        textView.setText(text);
    }

    public void goBack (View view) {
        finish();
    }
}
