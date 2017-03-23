package hospital.linde.uk.apphubandroid;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import hospital.linde.uk.apphubandroid.utils.Pegasus;

public class InformationActivity extends MyBaseActivity {
    private final static String TAG = InformationActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);

        TextView titleLabel = (TextView) findViewById(R.id.title_label);
        titleLabel.setText(LoginActivity.getHospital().getName());

        TextView macaddressView = (TextView) findViewById(R.id.status_macaddress);
        TextView nameView = (TextView) findViewById(R.id.status_name);
        TextView descriptionView = (TextView) findViewById(R.id.status_descritpion);
        TextView locationView = (TextView) findViewById(R.id.status_location);
        TextView versionView = (TextView) findViewById(R.id.status_version);
        TextView httpView = (TextView) findViewById(R.id.status_http);
        TextView httpsView = (TextView) findViewById(R.id.status_https);
        TextView insightsView = (TextView) findViewById(R.id.status_insights);
        TextView internetView = (TextView) findViewById(R.id.status_internet);
        TextView seenView = (TextView) findViewById(R.id.status_seen);
        TextView lostView = (TextView) findViewById(R.id.status_lost);

        Pegasus pegasus = HubActivity.getSelectedPegasus();

        String http = (pegasus.getStatusHttp() != null && pegasus.getStatusHttp()) ? getString(R.string.ok) : getString(R.string.not_ready);
        String https = (pegasus.getStatusHttps() != null && pegasus.getStatusHttps()) ? getString(R.string.ok) : getString(R.string.not_ready);
        String insights = (pegasus.getStatusInsights() != null && pegasus.getStatusInsights()) ? getString(R.string.ok) : getString(R.string.not_ready);
        String internet = (pegasus.getStatusInternet() != null && pegasus.getStatusInternet()) ? getString(R.string.ok) : getString(R.string.not_ready);
        String lost = (pegasus.getLost() != null && pegasus.getLost()) ? getString(R.string.yes) : getString(R.string.no);

        macaddressView.setText(pegasus.getMacAddress());
        nameView.setText(pegasus.getName());
        descriptionView.setText(pegasus.getFriendlyName());
        locationView.setText(pegasus.getCurrentLocation());
        versionView.setText(pegasus.getVersion());
        httpView.setText(http);
        httpsView.setText(https);
        insightsView.setText(insights);
        internetView.setText(internet);
        seenView.setText(pegasus.getLastSeen());
        lostView.setText(lost);

        Button button = (Button) findViewById(R.id.back);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelClick();
            }
        });
    }

    private void onCancelClick() {
        onBackPressed();
    }
}
