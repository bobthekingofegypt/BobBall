package org.bobstuff.bobball.Menus;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Spinner;

import org.bobstuff.bobball.R;
import org.bobstuff.bobball.Settings;
import org.bobstuff.bobball.Utilities;

public class menuOptions extends Activity {

    private EditText defaultNameInput;
    private Spinner levelSelectSettings;
    private Spinner retryActionSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        setContentView(R.layout.menu_options);

        defaultNameInput = (EditText) findViewById(R.id.defaultName);
        defaultNameInput.setHint(Settings.getDefaultName());

        levelSelectSettings = (Spinner) findViewById(R.id.spinnerLevelSelect);
        retryActionSettings = (Spinner) findViewById(R.id.spinnerRetryAction);

        levelSelectSettings.setAdapter(Utilities.createDropdownFromStrings(this,R.array.levelSelectSettings));
        retryActionSettings.setAdapter(Utilities.createDropdownFromStrings(this,R.array.retryActionSettings));

        levelSelectSettings.setSelection(Settings.getLevelSelectionType());
        retryActionSettings.setSelection(Settings.getRetryAction());
    }

    public void cancel (View view) {
        finish();
    }

    public void confirm (View view){
        Editable value = defaultNameInput.getText();
        String defaultNameNew = value.toString().trim();

        if (! defaultNameNew.isEmpty()) {
            Settings.setDefaultName(defaultNameNew);
        }

        Settings.setLevelSelectionType(levelSelectSettings.getSelectedItemPosition());
        Settings.setRetryAction(retryActionSettings.getSelectedItemPosition());

        finish();

    }
}
