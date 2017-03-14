package hospital.linde.uk.apphubandroid;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by dismer on 7/03/17.
 */

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }
}
