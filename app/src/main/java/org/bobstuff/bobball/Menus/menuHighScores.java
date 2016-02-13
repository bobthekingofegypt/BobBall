package org.bobstuff.bobball.Menus;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.bobstuff.bobball.BobBallActivity;
import org.bobstuff.bobball.R;
import org.bobstuff.bobball.Scores;
import org.bobstuff.bobball.Settings;
import org.bobstuff.bobball.Utilities;

public class menuHighScores extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.menu_highscores);

        Spinner numPlayersSelector = (Spinner) findViewById(R.id.num_players);
        numPlayersSelector.setAdapter(Utilities.createDropdown(this, 3));

        int rank = 0;
        int numPlayers = 0;
        Scores scores = new Scores (1);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            if (extras.containsKey("rank") && extras.containsKey("numPlayers")) {
                rank = extras.getInt("rank");
                numPlayers = extras.getInt("numPlayers");

                Button retryButton = (Button) findViewById(R.id.retryButton);
                retryButton.setVisibility(View.VISIBLE);
                numPlayersSelector.setSelection(numPlayers-1);

                scores = new Scores (numPlayers);
            }
        }
        else {
            Button backToMainButton = (Button) findViewById(R.id.backToMainButton);
            backToMainButton.setVisibility(View.VISIBLE);
        }

        scores.loadScores();
        displayScores(scores, rank);

        final int listenerNumPlayers = numPlayers;
        final int listenerRank = rank;

        numPlayersSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Scores scores = new Scores(position + 1);
                scores.loadScores();

                if (position + 1 == listenerNumPlayers) {
                    displayScores(scores, listenerRank);
                }

                else {
                    displayScores(scores, 0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // nop
            }

        });

    }

    private void displayScores (Scores scores, int rank) {
        final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.highScores);
        final TextView noRecords = (TextView) findViewById(R.id.noRecords);

        linearLayout.removeAllViews();

        if (scores.asCharSequence().length == 0) {
            noRecords.setVisibility(View.VISIBLE);
            linearLayout.setVisibility(View.GONE);
        }

        else {
            linearLayout.setVisibility(View.VISIBLE);
            noRecords.setVisibility(View.GONE);

            final CharSequence[] highScoreArray = scores.asCharSequence();

            for (int i = 1; i <= highScoreArray.length; i++) {

                final TextView highScoreEntry = new TextView(this);
                highScoreEntry.setText(i + ". " + highScoreArray[i - 1]);
                highScoreEntry.setTextSize(20);

                if (i == rank){
                    highScoreEntry.setTextColor(Color.parseColor("#000000"));
                    highScoreEntry.setBackgroundColor(Color.parseColor("#FFFFFF"));
                }

                linearLayout.addView(highScoreEntry);
            }
        }
    }

    public void goBack (View view) {
        finish();
    }

    public void retry (View view){
        int retryAction = Settings.getRetryAction();
        Intent intent = new Intent(this, BobBallActivity.class);
        int numPlayers = Settings.getNumPlayers() + 1;
        finish();   // finish in any case, finish only for retryAction 0 (Go back to level select)

        if (retryAction == 1){  // restart last level lost, same numPlayers
            intent.putExtra("numPlayers", numPlayers);
            intent.putExtra("level", Settings.getLastLevelFailed());
            startActivity(intent);
        }
        if (retryAction == 2){  // restart from last selected level
            intent.putExtra("numPlayers", numPlayers);
            intent.putExtra("level", Settings.getSelectLevel() + 1);
            startActivity(intent);
        }
        if (retryAction == 3){  // restart from level 1
            intent.putExtra("numPlayers", numPlayers);
            intent.putExtra("level", 1);
            startActivity(intent);
        }
    }}
