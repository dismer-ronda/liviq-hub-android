package hospital.linde.uk.apphubandroid;


import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by dismer on 6/03/17.
 */

public class SettingsFragment extends PreferenceFragment {

    final static String SETTINGS_HOSPITAL_URL       = "iq_url";
    final static String SETTINGS_BlE_SCAN_TIMEOUT   = "ble_timeout";
    final static String SETTINGS_HOSPITAL_TIMEOUT = "iq_timeout";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
