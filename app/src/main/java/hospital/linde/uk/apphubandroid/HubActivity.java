package hospital.linde.uk.apphubandroid;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import hospital.linde.uk.apphubandroid.utils.Utils;
import lombok.Getter;

public class HubActivity extends AppCompatActivity {
    private final static String TAG = HubActivity.class.getSimpleName();

    private static final int SCAN_TIMEOUT = 5000;

    private View mHubFormView;
    private View mProgressView;
    private TextView mHubLabel;
    private ListView listView;

    private ArrayList<String> listItems = new ArrayList<String>();
    private ArrayAdapter<String> adapter;

    private HashMap<String, String> bleMap = new HashMap<String, String>();
    private boolean scanning = false;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;

    @Getter private static BluetoothDevice selectedHub = null;
    @Getter private static String selectedMac = null;

    private ScanCallback leScanCallback = new ScanCallback() {
        private void registerBluetoothDevice(BluetoothDevice device, int rssi, ScanRecord scanRecord) {
            byte[] bytes = scanRecord.getBytes();

            if ( bytes.length >= 20 ) {
                String pegasusMac = Utils.convertToHex( bytes[14] ) + ":"
                        + Utils.convertToHex( bytes[15] ) + ":"
                        + Utils.convertToHex( bytes[16] ) + ":"
                        + Utils.convertToHex( bytes[17] ) + ":"
                        + Utils.convertToHex( bytes[18] )+ ":"
                        + Utils.convertToHex( bytes[19] );
                Log.i(TAG, "pegasusMac " + pegasusMac);

                if ( pegasusMac.startsWith( "B8:27:EB:") ) {
                    for (String item : listItems)
                        if (item.equals(pegasusMac))
                            return;

                    listItems.add(pegasusMac);
                    bleMap.put(pegasusMac, device.getAddress());

                    Log.i(TAG, "BLE " + device.getAddress() + " - " + pegasusMac);
                }
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            registerBluetoothDevice(result.getDevice(), result.getRssi(), result.getScanRecord());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            for (ScanResult result : results)
                registerBluetoothDevice(result.getDevice(), result.getRssi(), result.getScanRecord());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

            System.out.println("BLE failed " + errorCode);
        }
    };

    private void scanLeDevice(final boolean enable) {
        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (enable) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);

                    adapter.notifyDataSetChanged();

                    mHubLabel.setText( listItems.size() > 0 ? R.string.ble_device_select : R.string.ble_device_not_found );
                    showProgress(false);

                    Toast.makeText(HubActivity.this, getString(R.string.ble_scan_report).replaceAll("%1", Integer.toString(listItems.size())), Toast.LENGTH_SHORT).show();
                }
            }, SCAN_TIMEOUT);

            scanning = true;

            ScanSettings.Builder builder = new ScanSettings.Builder();
            builder.setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY );
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.i( TAG, "set BLE aggressive scan");
                builder.setMatchMode( ScanSettings.MATCH_MODE_AGGRESSIVE );

                Log.i( TAG, "set BLE max number of advertisement");
                builder.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT );
            }

            ScanSettings settings = builder.build();
            List<ScanFilter> filters = new ArrayList<ScanFilter>();
            bluetoothLeScanner.startScan(filters, settings, leScanCallback);
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);

        mProgressView = findViewById(R.id.search_progress);
        mHubFormView = findViewById(R.id.hub_list);
        mHubLabel = (TextView)findViewById(R.id.hub_label);

        setTitle(LoginActivity.getHospital().getName() + " " + getString( R.string.at ) + " " + LocationActivity.getSelectedLocation().getName());

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        Button button = (Button) findViewById(R.id.back);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelClick();
            }
        });

        button = (Button) findViewById(R.id.scan);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onScanClick();
            }
        });

        button = (Button) findViewById(R.id.info);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onInfoClick();
            }
        });

        button = (Button) findViewById(R.id.setup);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onTransmitClick();
            }
        });

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, listItems);

        listView = (ListView) findViewById(R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setSelector(R.color.list_selected_background);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                        selectedMac = ((TextView) arg1).getText().toString();
                        selectedHub = mBluetoothAdapter.getRemoteDevice( bleMap.get( selectedMac ) );
                    }
                }
        );

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        } else {
            onScanClick();
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mHubFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mHubFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mHubFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mHubFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void onCancelClick() {
        scanLeDevice(false);
        onBackPressed();
    }

    private void onInfoClick() {
    }

    private void onScanClick() {
        selectedMac = null;
        selectedHub = null;

        listItems.clear();
        adapter.notifyDataSetChanged();

        listView.setAdapter(adapter);

        mHubLabel.setText( R.string.ble_device_scanning );

        showProgress(true);
        scanLeDevice(true);
    }

    private void onTransmitClick() {
        if (selectedHub != null) {
            Intent intent = new Intent( this, ConfigurationActivity.class);
            startActivity(intent);
        }
    }
}
