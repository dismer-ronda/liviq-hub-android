package hospital.linde.uk.apphubandroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import hospital.linde.uk.apphubandroid.utils.Constants;

public class SuccessActivity extends MyBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        TextView titleLabel = (TextView) findViewById(R.id.title_label);
        titleLabel.setText(LoginActivity.getHospital().getName());

        Button button = (Button) findViewById(R.id.back_hubs);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackToHubs();
            }
        });

        button = (Button) findViewById(R.id.back_locations);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackToLocations();
            }
        });
    }

    private void onBackToHubs() {
        finish();
    }

    private void onBackToLocations() {
        sendBroadcast(new Intent(Constants.ACTION_FINISH));
        finish();
    }
}
