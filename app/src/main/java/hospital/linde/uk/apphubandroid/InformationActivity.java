package hospital.linde.uk.apphubandroid;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.URLEncoder;

import hospital.linde.uk.apphubandroid.utils.JsonSerializer;
import hospital.linde.uk.apphubandroid.utils.Location;
import hospital.linde.uk.apphubandroid.utils.Pegasus;
import hospital.linde.uk.apphubandroid.utils.Utils;

public class InformationActivity extends MyBaseActivity {
    private final static String TAG = InformationActivity.class.getSimpleName();

    private View mContainerView;
    private View mProgressView;
    private TextView mHubLabel;

    private TextView macaddressView;
    private TextView nameView;
    private TextView descriptionView;
    private TextView locationView;
    private TextView versionView;
    private TextView httpView;
    private TextView httpsView;
    private TextView insightsView;
    private TextView internetView;
    private TextView seenView;
    private TextView lostView;

    private String hospitalUrl;
    private InformationTask task = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        hospitalUrl = sharedPref.getString( SettingsFragment.SETTINGS_HOSPITAL_URL, "https://iqhospital.io");

        mProgressView = findViewById(R.id.search_progress);
        mContainerView = findViewById(R.id.contents);

        mHubLabel = (TextView)findViewById(R.id.hub_label);

        macaddressView = (TextView)findViewById(R.id.status_macaddress);
        nameView = (TextView)findViewById(R.id.status_name);
        descriptionView = (TextView)findViewById(R.id.status_descritpion);
        locationView = (TextView)findViewById(R.id.status_location);
        versionView = (TextView)findViewById(R.id.status_version);
        httpView = (TextView)findViewById(R.id.status_http);
        httpsView = (TextView)findViewById(R.id.status_https);
        insightsView = (TextView)findViewById(R.id.status_insights);
        internetView = (TextView)findViewById(R.id.status_internet);
        seenView = (TextView)findViewById(R.id.status_seen);
        lostView = (TextView)findViewById(R.id.status_lost);

        Button button = (Button) findViewById(R.id.back);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelClick();
            }
        });

         button = (Button) findViewById(R.id.retrieve);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRetrieveClick();
            }
        });

        onRetrieveClick();
    }

    private void onCancelClick() {
        onBackPressed();
    }

    private void onRetrieveClick() {
        if ( task == null ) {
            mHubLabel.setText( R.string.ble_device_retrieving );
            mHubLabel.setVisibility(View.VISIBLE);
            showProgress(true, mContainerView, mProgressView);
            task = new InformationTask();
            task.execute((Void) null);
        }
    }

    public class InformationTask extends AsyncTask<Void, Void, Boolean> {

        private Pegasus pegasus = null;

        private Location getLocation(String hospitalUrl, Integer locationId, Integer hospitalId, String token) {
            try {
                String[] headers = { "Content-Type:application/json", "Accept:application/json" };
                String response = Utils.platformCall( hospitalUrl + "/api/locations/" + locationId + "?hospitalId=" + hospitalId + "&access_token=" + token, "GET", 10000, null, headers );
                Log.i( TAG, "getLocation " + response );

                return (Location) JsonSerializer.toPojo(response, Location.class);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            return null;
        }

        private Pegasus getPegasus(String hospitalUrl, String macAddress, Integer hospitalId, String token) {
            try {
                String filter = URLEncoder.encode("{\"where\":{\"macAddress\":\"" + macAddress + "\", \"hospitalId\":" + hospitalId + "}}", "UTF8");
                String[] headers = { "Content-Type:application/json", "Accept:application/json" };
                String response = Utils.platformCall( hospitalUrl + "/api/hubs/findOne?filter=" + filter + "&access_token=" + token, "GET", 10000, null, headers );
                Log.i( TAG, "getPegasus " + response );

                return (Pegasus) JsonSerializer.toPojo( response, Pegasus.class);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected Boolean doInBackground(Void... params)
        {
            pegasus = getPegasus( hospitalUrl, HubActivity.getSelectedMac(), LoginActivity.getHospital().getId(), LoginActivity.getToken().getId() );

            if ( pegasus != null )
            {
                Location location = getLocation( hospitalUrl, pegasus.getLocationId(), LoginActivity.getHospital().getId(), LoginActivity.getToken().getId() );

                if ( location != null )
                {
                    pegasus.setCurrentLocation( location.getName() );
                }
            }
            else
            {
                pegasus = new Pegasus();

                pegasus.setMacAddress( HubActivity.getSelectedMac() );
                pegasus.setCurrentLocation( getString(R.string.location_none) );
                pegasus.setStatusHttp( false );
                pegasus.setStatusHttps( false );
                pegasus.setStatusInsights( false );
                pegasus.setStatusInternet( false );
            }

            return true;
        }

        @Override
        protected void onPostExecute( final Boolean success )
        {
            showProgress(false, mContainerView, mProgressView);
            mHubLabel.setVisibility(View.GONE);
            task = null;

            if ( success ) {
                String http = (pegasus.getStatusHttp() != null && pegasus.getStatusHttp().booleanValue()) ? getString(R.string.ok) : getString(R.string.not_ready);
                String https = (pegasus.getStatusHttps() != null && pegasus.getStatusHttps().booleanValue()) ? getString(R.string.ok) : getString(R.string.not_ready);
                String insights = (pegasus.getStatusInsights() != null && pegasus.getStatusInsights().booleanValue()) ? getString(R.string.ok) : getString(R.string.not_ready);
                String internet = (pegasus.getStatusInternet() != null && pegasus.getStatusInternet().booleanValue()) ? getString(R.string.ok) : getString(R.string.not_ready);
                String lost = (pegasus.getLost() != null && pegasus.getLost().booleanValue()) ? getString(R.string.yes) : getString(R.string.no) ;

                macaddressView.setText( pegasus.getMacAddress() );
                nameView.setText( pegasus.getName() );
                descriptionView.setText( pegasus.getFriendlyName() );
                locationView.setText( pegasus.getCurrentLocation() );
                versionView.setText( pegasus.getVersion() );
                httpView.setText( http );
                httpsView.setText( https );
                insightsView.setText( insights );
                internetView.setText( internet );
                seenView.setText( pegasus.getLastSeen() );
                lostView.setText( lost );
            }
            else {
                AlertDialog alertDialog = new AlertDialog.Builder(InformationActivity.this).create();
                alertDialog.setTitle(getString( R.string.failure));
                alertDialog.setMessage(getString(R.string.information_failure ));
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString( R.string.ok ),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        }
    }
}
