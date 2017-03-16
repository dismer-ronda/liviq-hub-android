package hospital.linde.uk.apphubandroid;

import android.Manifest;
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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
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

public class HubActivity extends MyBaseActivity {
    private final static String TAG = HubActivity.class.getSimpleName();

    private Integer bleTimeout;

    private View mContainerView;
    private View mProgressView;
    private TextView mHubLabel;
    private ListView listView;
    private Button nextButton;
    private Button infoButton;

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

                if ( pegasusMac.startsWith( "B8:27:EB:") )
                {
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
                    showProgress(false, mContainerView, mProgressView);

                    Toast.makeText(HubActivity.this, getString(R.string.ble_scan_report).replaceAll("%1", Integer.toString(listItems.size())), Toast.LENGTH_SHORT).show();
                }
            }, bleTimeout);

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
        mContainerView = findViewById(R.id.hub_list);
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

        infoButton = (Button) findViewById(R.id.info);
        infoButton.setVisibility(View.GONE);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onInfoClick();
            }
        });

        nextButton = (Button) findViewById(R.id.setup);
        nextButton.setVisibility(View.GONE);
        nextButton.setOnClickListener(new View.OnClickListener() {
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
        listView.setSelector(R.color.colorPrimary);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                        selectedMac = ((TextView) arg1).getText().toString();
                        selectedHub = mBluetoothAdapter.getRemoteDevice( bleMap.get( selectedMac ) );
                        nextButton.setVisibility(View.VISIBLE);
                        infoButton.setVisibility(View.VISIBLE);
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

    private void onCancelClick() {
        scanLeDevice(false);
        onBackPressed();
    }

    private void onInfoClick() {
        Intent intent = new Intent( this, InformationActivity.class);
        startActivity(intent);

        /*if ( task == null ) {
            mHubLabel.setText( R.string.ble_device_retrieving );
            showProgress(true, mContainerView, mProgressView);
            task = new InformationTask();
            task.execute((Void) null);
        }*/
    }

    private void onScanClick() {
        selectedMac = null;
        selectedHub = null;

        infoButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);

        listItems.clear();
        adapter.notifyDataSetChanged();

        listView.setAdapter(adapter);

        mHubLabel.setText( R.string.ble_device_scanning );

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences( this );
        bleTimeout = Integer.parseInt( sharedPref.getString( SettingsFragment.SETTINGS_BlE_SCAN_TIMEOUT, "5000") );

        showProgress(true, mContainerView, mProgressView);
        scanLeDevice(true);
    }

    private void onTransmitClick() {
        if (selectedHub != null) {
            Intent intent = new Intent( this, ConfigurationActivity.class);
            startActivity(intent);
        }
    }
}
