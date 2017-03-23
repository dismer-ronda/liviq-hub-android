package hospital.linde.uk.apphubandroid;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import hospital.linde.uk.apphubandroid.utils.ConfigurationData;
import hospital.linde.uk.apphubandroid.utils.Constants;
import hospital.linde.uk.apphubandroid.utils.Pegasus;
import hospital.linde.uk.apphubandroid.utils.Utils;

public class ConfigurationActivity extends MyBaseActivity {
    private final static String TAG = ConfigurationActivity.class.getSimpleName();

    private View mContainerView;
    private View mProgressView;

    private TextView topLabel;

    private Button transmitButton;
    private Button advancedButton;

    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic characteristic;

    private List<byte[]> writeCommands;
    private int currentCommand;

    private String hospitalUrl;
    private int timeout;
    private String pegasusToken = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        hospitalUrl = sharedPref.getString(SettingsFragment.SETTINGS_HOSPITAL_URL, "https://iqhospital.io");
        timeout = Integer.parseInt(sharedPref.getString(SettingsFragment.SETTINGS_HOSPITAL_TIMEOUT, "10000"));

        mContainerView = findViewById(R.id.contents);
        mProgressView = findViewById(R.id.search_progress);

        topLabel = (TextView) findViewById(R.id.top_label);

        TextView titleLabel = (TextView) findViewById(R.id.title_label);
        titleLabel.setText(LoginActivity.getHospital().getName());

        topLabel.setText(getString(R.string.setup_label_format).replace("$hub", HubActivity.getSelectedPegasus().getName()).replace("$location", LocationActivity.getSelectedLocation().getName()));

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_GATT_CONNECTED);
        filter.addAction(Constants.ACTION_GATT_DISCONNECTED);
        filter.addAction(Constants.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(Constants.ACTION_WRITE_PACKET);
        filter.addAction(Constants.ACTION_DATA_AVAILABLE);
        filter.addAction(Constants.ACTION_DESCRIPTOR_WRITE);
        filter.addAction(Constants.ACTION_PIPELINE_BROKEN);
        filter.addAction(Constants.ACTION_TRANSMIT);

        registerReceiver(mGattUpdateReceiver, filter);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        mBluetoothGatt = HubActivity.getSelectedHub().connectGatt(this, false, mGattCallback);

        Button button = (Button) findViewById(R.id.back);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        advancedButton = (Button) findViewById(R.id.advanced);
        advancedButton.setVisibility(View.GONE);
        advancedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAdvancedPressed();
            }
        });

        transmitButton = (Button) findViewById(R.id.transmit);
        transmitButton.setVisibility(View.GONE);
        transmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onTransmitClick();
            }
        });
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mGattUpdateReceiver);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        mBluetoothGatt.close();
        mBluetoothGatt.disconnect();

        super.onBackPressed();
    }

    public void onAdvancedPressed() {
        Intent intent = new Intent(this, AdvancedActivity.class);
        startActivity(intent);
    }

    private void transmitDefaultConfiguration() {
        showProgress(true, mContainerView, mProgressView);

        ConfigurationData data = new ConfigurationData();

        data.setEnableWifi(LoginActivity.getHospital().getConfigParameters().getWifiEnabled() == null ? false : LoginActivity.getHospital().getConfigParameters().getWifiEnabled());
        data.setEnableProxy(LoginActivity.getHospital().getConfigParameters().getProxyEnabled() == null ? false : LoginActivity.getHospital().getConfigParameters().getProxyEnabled());
        data.setEnableStatic(false);

        data.setHubName(Utils.getEmptyIfNull(HubActivity.getSelectedPegasus().getName()));
        data.setHubDescription(Utils.getEmptyIfNull(HubActivity.getSelectedPegasus().getFriendlyName()));

        data.setWifiId(Utils.getEmptyIfNull(LoginActivity.getWifiSSID()));
        data.setWifiPassword(Utils.getEmptyIfNull(LoginActivity.getWifiSecurityKey()));

        data.setProxyServer(Utils.getEmptyIfNull(LoginActivity.getProxyServer()));
        data.setProxyPort(Utils.getEmptyIfNull(LoginActivity.getProxyPort()));
        data.setProxyUser(Utils.getEmptyIfNull(LoginActivity.getProxyUser()));
        data.setProxyPassword(Utils.getEmptyIfNull(LoginActivity.getProxyPassword()));

        data.setAddress("");
        data.setNetmask("");
        data.setGateway("");

        new ConfigurationTask(data).execute((Void) null);
    }

    private void transmitAdvancedConfiguration(ConfigurationData data) {
        showProgress(true, mContainerView, mProgressView);
        new ConfigurationTask(data).execute((Void) null);
    }

    private void onTransmitClick() {
        transmitDefaultConfiguration();

        /*showProgress(true, mContainerView, mProgressView);

        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        hubName = hubNameView.getText().toString();
        hubDescription = hubDescriptionView.getText().toString();

        new ConfigurationTask().execute((Void) null);*/
    }

    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    Log.i(TAG, "onConnectionStateChange status " + status + " state " + newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
                        sendBroadcast(new Intent(Constants.ACTION_GATT_CONNECTED));
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.i(TAG, "Disconnected from GATT server.");
                        sendBroadcast(new Intent(Constants.ACTION_GATT_DISCONNECTED));
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        sendBroadcast(new Intent(Constants.ACTION_GATT_SERVICES_DISCOVERED));
                    } else
                        sendBroadcast(new Intent(Constants.ACTION_PIPELINE_BROKEN));
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(Constants.ACTION_DATA_AVAILABLE, characteristic);
                    } else
                        sendBroadcast(new Intent(Constants.ACTION_PIPELINE_BROKEN));
                }

                @Override
                // Result of a characteristic changed operation
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    broadcastUpdate(Constants.ACTION_DATA_AVAILABLE, characteristic);
                }

                @Override
                // Result of a characteristic changed operation
                public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                    Log.i(TAG, "onReliableWriteCompleted status " + status);
                    /*if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    }*/
                }

                @Override
                // Result of a characteristic read operation
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    sendBroadcast(new Intent(status == BluetoothGatt.GATT_SUCCESS ? Constants.ACTION_DESCRIPTOR_WRITE : Constants.ACTION_PIPELINE_BROKEN));
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    sendBroadcast(new Intent(status == BluetoothGatt.GATT_SUCCESS ? Constants.ACTION_WRITE_PACKET : Constants.ACTION_PIPELINE_BROKEN));
                }

                private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
                    Intent intent = new Intent(action);
                    intent.putExtra(Constants.EXTRA_DATA, characteristic.getValue());
                    sendBroadcast(intent);
                }
            };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            Log.i(TAG, "onReceive " + action);

            if (Constants.ACTION_GATT_CONNECTED.equals(action)) {
                onBleConnected();
            } else if (Constants.ACTION_GATT_DISCONNECTED.equals(action)) {
                onBleDisconnected();
            } else if (Constants.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                onServiceDiscovered();
            } else if (Constants.ACTION_DATA_AVAILABLE.equals(action)) {
                onReceivedResponse(intent);
            } else if (Constants.ACTION_DESCRIPTOR_WRITE.equals(action)) {
                onDescriptorWrite();
            } else if (Constants.ACTION_WRITE_PACKET.equals(action)) {
                onCharacteristicWrite();
            } else if (Constants.ACTION_PIPELINE_BROKEN.equals(action)) {
                onPipelineBroken();
            } else if (Constants.ACTION_TRANSMIT.equals(action)) {
                transmitAdvancedConfiguration((ConfigurationData) intent.getParcelableExtra(Constants.TRANSMIT_DATA));
            }
        }
    };

    private void onBleConnected() {
    }

    private void onBleDisconnected() {
        terminateFailure();
    }

    private void onServiceDiscovered() {
        BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(Constants.IQHubMainServivceUuid));

        if (service == null) {
            Log.i(TAG, "service not found");
            terminateFailure();
        } else {
            characteristic = service.getCharacteristic(UUID.fromString(Constants.IQHubMainCharacteristicsUuid));
            if (characteristic == null) {
                Log.i(TAG, "service not found");
                terminateFailure();
            } else {
                mBluetoothGatt.setCharacteristicNotification(characteristic, true);

                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(Constants.IQHubCharacteristicsDescriptorUuid));

                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                } else {
                    terminateFailure();
                }
            }
        }
    }

    private void onDescriptorWrite() {
        advancedButton.setVisibility(View.VISIBLE);
        transmitButton.setVisibility(View.VISIBLE);
    }

    private void onReceivedResponse(Intent intent) {
        byte data[] = intent.getByteArrayExtra(Constants.EXTRA_DATA);

        if (data != null && data.length > 0 && data[2] == 0x6f && data[3] == 0) {
            terminateSuccess();
            return;
        }

        terminateFailure();
    }

    private void onCharacteristicWrite() {
        if (writeCommands != null) {
            currentCommand++;
            if (currentCommand < writeCommands.size())
                writeCommand(writeCommands.get(currentCommand));
        }
    }

    private void onPipelineBroken() {
        terminateFailure();
    }

    private void bluetoothDisconnect() {
        writeCommands = null;

        mBluetoothGatt.close();
        mBluetoothGatt.disconnect();

        showProgress(false, mContainerView, mProgressView);
    }

    private void terminateSuccess() {
        bluetoothDisconnect();

        //Toast.makeText(this, getString(R.string.configuration_success), Toast.LENGTH_SHORT).show();

        sendBroadcast(new Intent(Constants.ACTION_UPDATE_HUB));
        ConfigurationActivity.this.finish();

        Intent intent = new Intent(this, SuccessActivity.class);
        startActivity(intent);
    }

    private void terminateFailure() {
        bluetoothDisconnect();

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.failure));
        alertDialog.setMessage(getString(R.string.configuration_failure));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ConfigurationActivity.this.finish();
                    }
                });
        alertDialog.show();
    }

    private void writeCommand(byte[] command) {
        characteristic.setValue(command);
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    private byte[] getConfigurationPartBuffer(String part, int index) {
        byte[] buffer = new byte[part.length() + 3];

        buffer[0] = 0x6F;
        buffer[1] = (byte) index;
        buffer[2] = (byte) part.length();
        for (int j = 0; j < part.length(); j++)
            buffer[3 + j] = (byte) part.charAt(j);

        return buffer;
    }

    private byte[] getConfigurationCrcBuffer(short crc) {
        byte buffer[] = new byte[5];

        buffer[0] = 0x6F;
        buffer[1] = (byte) 0xFF;
        buffer[2] = 0x02;
        buffer[3] = Utils.lobyte(crc);
        buffer[4] = Utils.hibyte(crc);

        return buffer;
    }

    public String getProxySettings(ConfigurationData data) {
        String proxyServer = data.getProxyServer();
        String proxyPort = data.getProxyPort();
        String proxyUser = data.getProxyUser();
        String proxyPassword = data.getProxyPassword();

        boolean serverSet = !proxyServer.isEmpty();

        return (data.isEnableProxy() && serverSet) ? "<ACTION id=\"A074\">" + (proxyServer + (!proxyPort.isEmpty() ? (":" + proxyPort) : "") + (!proxyUser.isEmpty() ? (" -U " + proxyUser + ":" + proxyPassword) : "")) + "</ACTION>\n" : "";
    }

    private void prepareConfiguration(String hospitalUrl, ConfigurationData data) {
        Log.i(TAG, data.toString());
        String wifiAction = data.isEnableWifi() ?
                (
                        "<ACTION id=\"A071\">1</ACTION>\n" +
                                "<ACTION id=\"A069\">" + data.getWifiId() + "</ACTION>\n" +
                                "<ACTION id=\"A070\">" + data.getWifiPassword() + "</ACTION>\n"
                ) : "<ACTION id=\"A071\">0</ACTION>\n";

        String staticAction = data.isEnableStatic() ?
                (
                        "<ACTION id=\"A083\">1</ACTION>\n" +
                                "<ACTION id=\"A084\">" + data.getAddress() + "</ACTION>\n" +
                                "<ACTION id=\"A085\">" + data.getNetmask() + "</ACTION>\n" +
                                "<ACTION id=\"A086\">" + data.getGateway() + "</ACTION>\n"
                ) : "<ACTION id=\"A083\">0</ACTION>\n";

        String setupCommand =
                "<ANSWER>\n" +
                        "<STATUS>0</STATUS>\n" +

                        "<ACTIONS>\n" +
                        "<ACTION id=\"A062\">" + pegasusToken + "</ACTION>\n" +

                        "<ACTION id=\"A081\">" + hospitalUrl + "/feed/cylinders</ACTION>\n" +

                        "<ACTION id=\"A064\">" + LocationActivity.getSelectedLocation().getName() + "</ACTION>\n" +
                        "<ACTION id=\"A065\">" + LocationActivity.getSelectedLocation().getId() + "</ACTION>\n" +

                        // Disable GPRS
                        "<ACTION id=\"A068\">0</ACTION>\n" +

                        wifiAction +
                        staticAction +

                        getProxySettings(data) +

                        "</ACTIONS>\n" +
                        "</ANSWER>\n";
        Log.i(TAG, setupCommand);

        int crc = Utils.getCRC16(setupCommand);

        List<String> parts = Utils.splitStringInParts(setupCommand, 15);

        writeCommands = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++)
            writeCommands.add(getConfigurationPartBuffer(parts.get(i), i));

        writeCommands.add(getConfigurationCrcBuffer((short) crc));
    }

    public class ConfigurationTask extends AsyncTask<Void, Void, Boolean> {
        private ConfigurationData data;

        public ConfigurationTask(ConfigurationData data) {
            this.data = data;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Pegasus pegasus = Utils.getPegasus(hospitalUrl, HubActivity.getSelectedPegasus().getMacAddress(), LoginActivity.getHospital().getId(), LoginActivity.getToken().getId(), timeout);

            if (pegasus != null) {
                pegasus.setLocationId(LocationActivity.getSelectedLocation().getId());
                pegasus.setHospitalId(LoginActivity.getHospital().getId());
                if (!data.getHubName().isEmpty())
                    pegasus.setName(data.getHubName());
                if (!data.getHubDescription().isEmpty())
                    pegasus.setFriendlyName(data.getHubDescription());
                pegasus.setDeleted("0");

                Utils.updatePegasus(hospitalUrl, pegasus, LoginActivity.getToken().getId(), timeout);
            } else {
                pegasus = new Pegasus();

                pegasus.setLocationId(LocationActivity.getSelectedLocation().getId());
                pegasus.setHospitalId(LoginActivity.getHospital().getId());
                pegasus.setName(!data.getHubName().isEmpty() ? data.getHubName() : data.getAddress());
                pegasus.setFriendlyName(data.getHubDescription());
                pegasus.setMacAddress(HubActivity.getSelectedMac());
                pegasus.setDeleted("0");

                Utils.addPegasus(hospitalUrl, pegasus, LoginActivity.getToken().getId(), timeout);
            }

            pegasusToken = Utils.getPegasusToken(hospitalUrl, pegasus, LoginActivity.getToken().getId(), timeout);

            return pegasusToken != null;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                prepareConfiguration(hospitalUrl, data);

                currentCommand = 0;
                writeCommand(writeCommands.get(currentCommand));
            } else {
                showProgress(false, mContainerView, mProgressView);
            }
        }
    }
}
