package hospital.linde.uk.apphubandroid;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LoggedActivity extends AppCompatActivity {

    private TextView userName;
    private TextView hospitalName;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_logged);

        userName = (TextView) findViewById(R.id.user_name);
        hospitalName = (TextView) findViewById(R.id.hospital_name);

        userName.setText( LoginActivity.getToken().getUser().getUsername() );
        hospitalName.setText( LoginActivity.getHospital().getName() );

        Button button = (Button) findViewById( R.id.start_configuration );
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConfiguration();
            }
        });

        button = (Button) findViewById( R.id.cancel );
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelClick();
            }
        });
    }

    private void onCancelClick()
    {
        onBackPressed();
    }

    private void startConfiguration()
    {
        Intent intent = new Intent( this, LocationActivity.class);
        startActivity(intent);
    }
}
