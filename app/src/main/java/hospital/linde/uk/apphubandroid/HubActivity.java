package hospital.linde.uk.apphubandroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
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

import hospital.linde.uk.apphubandroid.utils.Constants;
import hospital.linde.uk.apphubandroid.utils.Pegasus;
import hospital.linde.uk.apphubandroid.utils.Utils;
import lombok.Getter;

public class HubActivity extends MyBaseActivity {
    private final static String TAG = HubActivity.class.getSimpleName();

    private Integer bleTimeout;

    private View mContainerView;
    private View mProgressView;
    private TextView mHubLabel;
    private TextView topLabel;
    private ListView listView;
    private Button nextButton;
    private Button infoButton;
    private Button scanButton;

    private ArrayList<String> listItems = new ArrayList<>();
    private HashMap<String, Pegasus> mapPegasus = new HashMap<>();

    private ArrayAdapter<String> adapter;

    private HashMap<String, String> bleMap = new HashMap<>();
    private boolean scanning = false;

    private BluetoothAdapter mBluetoothAdapter;

    @Getter
    private static BluetoothDevice selectedHub = null;
    @Getter
    private static String selectedMac = null;
    @Getter
    private static Pegasus selectedPegasus = null;

    private ScanCallback leScanCallback = new ScanCallback() {
        private void registerBluetoothDevice(BluetoothDevice device, int rssi, ScanRecord scanRecord) {
            byte[] bytes = scanRecord.getBytes();

            if (bytes.length >= 20) {
                String pegasusMac = Utils.convertToHex(bytes[14]) + ":"
                        + Utils.convertToHex(bytes[15]) + ":"
                        + Utils.convertToHex(bytes[16]) + ":"
                        + Utils.convertToHex(bytes[17]) + ":"
                        + Utils.convertToHex(bytes[18]) + ":"
                        + Utils.convertToHex(bytes[19]);
                Log.i(TAG, "pegasusMac " + pegasusMac);

                if (pegasusMac.startsWith("B8:27:EB:")) {
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

            Log.i(TAG, "BLE failed " + errorCode);
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
                    mHubLabel.setText(listItems.size() > 0 ? R.string.ble_device_select : R.string.ble_device_not_found);
                    showProgress(false, mContainerView, mProgressView);
                    scanButton.setEnabled(true);

                    Toast.makeText(HubActivity.this, getString(R.string.ble_scan_report).replaceAll("%1", Integer.toString(listItems.size())), Toast.LENGTH_SHORT).show();

                    new RetrieveTask().execute((Void) null);
                }
            }, bleTimeout);

            scanning = true;

            ScanSettings.Builder builder = new ScanSettings.Builder();
            builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.i(TAG, "set BLE aggressive scan");
                builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);

                Log.i(TAG, "set BLE max number of advertisement");
                builder.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT);
            }

            ScanSettings settings = builder.build();
            List<ScanFilter> filters = new ArrayList<ScanFilter>();
            bluetoothLeScanner.startScan(filters, settings, leScanCallback);
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            Log.i(TAG, "onReceive " + action);

            if (Constants.ACTION_UPDATE_HUB.equals(action)) {
                updateConfiguredHub();
            } else if (Constants.ACTION_FINISH.equals(action)) {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_UPDATE_HUB);
        filter.addAction(Constants.ACTION_FINISH);
        registerReceiver(broadcastReceiver, filter);

        mProgressView = findViewById(R.id.search_progress);
        mContainerView = findViewById(R.id.hub_list);
        mHubLabel = (TextView) findViewById(R.id.hub_label);
        topLabel = (TextView) findViewById(R.id.top_label);

        TextView titleLabel = (TextView) findViewById(R.id.title_label);
        titleLabel.setText(LoginActivity.getHospital().getName());

        topLabel.setText(getString(R.string.location) + " " + LocationActivity.getSelectedLocation().getName());

        Button button = (Button) findViewById(R.id.back);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        infoButton = (Button) findViewById(R.id.info);
        infoButton.setVisibility(View.GONE); //setEnabled( false );
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onInfoClick();
            }
        });

        nextButton = (Button) findViewById(R.id.setup);
        nextButton.setVisibility(View.GONE); //setEnabled( false );
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onTransmitClick();
            }
        });

        scanButton = (Button) findViewById(R.id.scan);
        scanButton.setEnabled(true);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onScanClick();
            }
        });

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, listItems);

        listView = (ListView) findViewById(R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setSelector(R.color.selection);
        listView.setAdapter(adapter);
        listView.setEnabled(false);
        listView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                        selectedMac = ((TextView) arg1).getText().toString();
                        selectedHub = mBluetoothAdapter.getRemoteDevice(bleMap.get(selectedMac));
                        selectedPegasus = mapPegasus.get(selectedMac);

                        nextButton.setVisibility(View.VISIBLE);
                        infoButton.setVisibility(selectedPegasus != null ? View.VISIBLE : View.GONE);
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

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Throwable e) {
        }

        super.onDestroy();
    }

    private void onInfoClick() {
        if (selectedHub != null && selectedPegasus != null) {
            Intent intent = new Intent(this, InformationActivity.class);
            startActivity(intent);
        }
    }

    private void onScanClick() {
        selectedMac = null;
        selectedHub = null;
        selectedPegasus = null;

        infoButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);

        listItems.clear();
        adapter.notifyDataSetChanged();

        listView.setAdapter(adapter);
        listView.setEnabled(false);

        mHubLabel.setText(R.string.ble_device_scanning);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        bleTimeout = Integer.parseInt(sharedPref.getString(SettingsFragment.SETTINGS_BlE_SCAN_TIMEOUT, "5000"));

        showProgress(true, mContainerView, mProgressView);
        scanButton.setEnabled(false);
        scanLeDevice(true);
    }

    private void onTransmitClick() {
        if (selectedHub != null && selectedPegasus != null) {
            Intent intent = new Intent(this, ConfigurationActivity.class);
            startActivity(intent);
        }
    }

    public void updateConfiguredHub() {
        int count = listView.getCount();
        for (int i = 0; i < count; i++) {
            TextView view = (TextView) listView.getChildAt(i);

            if (view != null) {
                String macAddress = view.getText().toString();

                if (macAddress.equals(selectedMac))
                    view.setBackgroundResource(R.drawable.hub_configured);
            }
        }
    }

    public class RetrieveTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(HubActivity.this);
            String hospitalUrl = sharedPref.getString(SettingsFragment.SETTINGS_HOSPITAL_URL, "https://iqhospital.io");
            Integer timeout = Integer.parseInt(sharedPref.getString(SettingsFragment.SETTINGS_HOSPITAL_TIMEOUT, "10000"));

            for (String item : listItems) {
                Pegasus pegasus = Utils.getPegasus(hospitalUrl, item, LoginActivity.getHospital().getId(), LoginActivity.getToken().getId(), timeout);

                if (pegasus == null) {
                    pegasus = new Pegasus();

                    pegasus.setMacAddress(HubActivity.getSelectedMac());
                    pegasus.setCurrentLocation(getString(R.string.location_none));
                    pegasus.setStatusHttp(false);
                    pegasus.setStatusHttps(false);
                    pegasus.setStatusInsights(false);
                    pegasus.setStatusInternet(false);
                }

                mapPegasus.put(item, pegasus);
            }

            return true;
        }

        private boolean isConfigured(Pegasus pegasus) {
            return pegasus.getLocationId() != null && pegasus.getHospitalId() != null && "0".equals(pegasus.getDeleted());
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                int count = listView.getCount();
                for (int i = 0; i < count; i++) {
                    TextView view = (TextView) listView.getChildAt(i);

                    if (view != null) {
                        String macAddress = view.getText().toString();

                        Pegasus pegasus = mapPegasus.get(macAddress);

                        if (isConfigured(pegasus))
                            view.setBackgroundResource(R.drawable.hub_configured);
                    }
                    listView.setEnabled(true);
                }
            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(HubActivity.this).create();
                alertDialog.setTitle(getString(R.string.failure));
                alertDialog.setMessage(getString(R.string.information_failure));
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok),
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
