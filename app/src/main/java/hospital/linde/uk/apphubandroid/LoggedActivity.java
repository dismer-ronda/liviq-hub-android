package hospital.linde.uk.apphubandroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LoggedActivity extends MyBaseActivity {

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
