package hospital.linde.uk.apphubandroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import hospital.linde.uk.apphubandroid.utils.Location;
import lombok.Getter;
import lombok.Setter;

public class LocationActivity extends MyBaseActivity
{
    private ListView listView;
    private Button nextButton;

    @Getter @Setter private static Location selectedLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        TextView titleLabel = (TextView) findViewById(R.id.title_label);
        titleLabel.setText(LoginActivity.getHospital().getName());

        ArrayList<String> values = new ArrayList<String>();
        for ( Location location : LoginActivity.getLocations() )
            values.add( location.getName() );
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( this, android.R.layout.simple_list_item_1, android.R.id.text1, values );

        listView = (ListView) findViewById(R.id.list);
        listView.setChoiceMode( ListView.CHOICE_MODE_SINGLE );
        listView.setSelector(R.color.selection);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                String selected = (String) listView.getItemAtPosition(position);

                for ( Location location : LoginActivity.getLocations() )
                    if ( selected.equals( location.getName() ) ) {
                        selectedLocation = location;
                        nextButton.setVisibility(View.VISIBLE);
                    }

            }
        });
        listView.setAdapter(adapter);

        Button button = (Button) findViewById( R.id.cancel );
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                onCancelClick();
            }
        });

        nextButton = (Button) findViewById( R.id.next );
        nextButton.setVisibility( View.GONE );
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                onNextClick();
            }
        });
    }

    private void onCancelClick()
    {
        selectedLocation = null;
        onBackPressed();
    }

    private void onNextClick()
    {
        if ( selectedLocation != null ) {
            Intent intent = new Intent(this, HubActivity.class);
            startActivity(intent);
        }
    }
}
