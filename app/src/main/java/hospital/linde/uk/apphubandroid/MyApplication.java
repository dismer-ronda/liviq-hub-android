package hospital.linde.uk.apphubandroid;

import android.app.Application;
import android.util.Log;

import hospital.linde.uk.apphubandroid.utils.TypefaceUtil;

/**
 * Created by dismer on 22/03/17.
 */

public class MyApplication extends Application {
    private final static String TAG = MyApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "overriding SERIF font with arial.ttf");
        TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/arial.ttf"); // font from assets: "assets/fonts/arial.ttf
    }
}
