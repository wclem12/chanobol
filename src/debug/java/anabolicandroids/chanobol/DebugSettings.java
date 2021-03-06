package anabolicandroids.chanobol;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import anabolicandroids.chanobol.ui.boards.FavoritesActivity;
import anabolicandroids.chanobol.util.BaseSettings;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

// The following resources have been helpful:
// http://stackoverflow.com/a/3026922/283607
public class DebugSettings extends BaseSettings {

    public static final String MOCK = "pref_mock";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.debugprefs);
        toolbar.setTitle("Debug Settings");
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(MOCK)) {
            // From U2020 DebugAppContainer.setEndpointAndRelaunch
            Intent newApp = new Intent(this, FavoritesActivity.class);
            newApp.setFlags(FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK);
            getApplication().startActivity(newApp);
            App.get(getApplicationContext()).buildAppGraphAndInject();
        }
    }
}
