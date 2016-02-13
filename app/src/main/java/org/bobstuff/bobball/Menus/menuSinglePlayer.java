package org.bobstuff.bobball.Menus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Spinner;

import org.bobstuff.bobball.BobBallActivity;
import org.bobstuff.bobball.R;
import org.bobstuff.bobball.Settings;
import org.bobstuff.bobball.Statistics;
import org.bobstuff.bobball.Utilities;

public class menuSinglePlayer extends Activity {

    private Spinner numPlayersSelector;
    private Spinner levelSelector;

    protected int numPlayers = 1;
    protected int level = 1;
    protected int topLevel = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.menu_singleplayer);

        numPlayersSelector = (Spinner) findViewById(R.id.num_players);
        levelSelector = (Spinner) findViewById(R.id.level_select);
    }

    public void onStart()
    {
        super.onStart();

        topLevel = Statistics.getHighestLevel(levelSelector.getSelectedItemPosition()+1);

        levelSelector.setAdapter(Utilities.createDropdown(this, topLevel));
        numPlayersSelector.setAdapter(Utilities.createDropdown(this, 3));

        numPlayersSelector.setSelection(Settings.getNumPlayers() - 1);
        levelSelectPreSelect();

        final Context context = this;

        numPlayersSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                int numPlayers = position + 1;

                Settings.setNumPlayers(numPlayers - 1);

                int highestLevel = Statistics.getHighestLevel(numPlayers);

                levelSelector.setAdapter(Utilities.createDropdown(context, highestLevel));
                levelSelectPreSelect();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // nop
            }

        });

        levelSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Settings.setSelectedLevel(levelSelector.getSelectedItemPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // nop
            }
        });

    }


    public void levelSelectPreSelect (){
        int levelSelectionType = Settings.getLevelSelectionType();

        if (levelSelectionType == 0){   // level 1
            levelSelector.setSelection(0);
        }
        if (levelSelectionType == 1){   // last selected level
            levelSelector.setSelection(Settings.getSelectLevel());
        }
        if (levelSelectionType == 2){   //highest available level
            levelSelector.setSelection(levelSelector.getCount() - 1);
        }
        if (levelSelectionType == 3) {   // last level lost
            levelSelector.setSelection(Settings.getLastLevelFailed() - 1);
        }
    }

    public void startBobBall (View view)
    {
        numPlayers = numPlayersSelector.getSelectedItemPosition() + 1;
        level = levelSelector.getSelectedItemPosition() + 1;

        Intent intent = new Intent(this, BobBallActivity.class);
        intent.putExtra("numPlayers", numPlayers);
        intent.putExtra("level", level);
        startActivity(intent);
    }

    public void exitApp (View view)
    {
        finish();
    }
}
