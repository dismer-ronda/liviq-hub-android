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
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import hospital.linde.uk.apphubandroid.utils.Constants;
import hospital.linde.uk.apphubandroid.utils.Pegasus;
import hospital.linde.uk.apphubandroid.utils.Utils;

public class ConfigurationActivity extends MyBaseActivity {
    private final static String TAG = ConfigurationActivity.class.getSimpleName();

    private View mContainerView;
    private View mProgressView;

    private TextView mHubLabel;

    private View setupView;
    private View wifiView;
    private View proxyView;
    private View staticView;

    private Button transmitButton;

    private EditText hubNameView;
    private EditText hubDescriptionView;

    private EditText wifiId;
    private EditText wifiPassword;

    private EditText proxyServerView;
    private EditText proxyPortView;
    private EditText proxyUserView;
    private EditText proxyPasswordView;

    private EditText addressView;
    private EditText netmaskView;
    private EditText gatewayView;

    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic characteristic;

    private List<byte[]> writeCommands;
    private int currentCommand;

    private boolean enableWifi = false;
    private boolean enableProxy = false;
    private boolean enableStatic = false;

    private String hospitalUrl;
    private int timeout;
    private String pegasusToken = null;
    private String hubName;
    private String hubDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        hospitalUrl = sharedPref.getString(SettingsFragment.SETTINGS_HOSPITAL_URL, "https://iqhospital.io");
        timeout = Integer.parseInt(sharedPref.getString(SettingsFragment.SETTINGS_HOSPITAL_TIMEOUT, "10000"));

        enableWifi = LoginActivity.getHospital().getConfigParameters().getWifiEnabled() == null ? false : LoginActivity.getHospital().getConfigParameters().getWifiEnabled();
        enableProxy = LoginActivity.getHospital().getConfigParameters().getProxyEnabled() == null ? false : LoginActivity.getHospital().getConfigParameters().getProxyEnabled();
        enableStatic = false; /*LoginActivity.getHospital().getConfigParameters().getProxyEnabled() == null ? false : LoginActivity.getHospital().getConfigParameters().getStaticEnabled();*/

        mContainerView = findViewById(R.id.contents);
        mProgressView = findViewById(R.id.search_progress);

        mHubLabel = (TextView) findViewById(R.id.top_label);

        setupView = findViewById(R.id.hub_configuration);
        setupView.setVisibility(View.GONE);

        wifiView = findViewById(R.id.wifi_configuration);
        proxyView = findViewById(R.id.proxy_configuration);
        staticView = findViewById(R.id.static_configuration);

        hubNameView = (EditText) findViewById(R.id.hub_name);
        hubDescriptionView = (EditText) findViewById(R.id.hub_description);

        CheckBox checkWifi = (CheckBox) findViewById(R.id.checkbox_wifi);
        checkWifi.setChecked(enableWifi);

        wifiView.setVisibility(enableWifi ? View.VISIBLE : View.GONE);

        wifiId = (EditText) findViewById(R.id.wifi_ssid);
        wifiId.setText(LoginActivity.getWifiSSID());

        wifiPassword = (EditText) findViewById(R.id.wifi_password);
        wifiPassword.setText(LoginActivity.getWifiSecurityKey());

        CheckBox checkProxy = (CheckBox) findViewById(R.id.checkbox_proxy);
        checkProxy.setChecked(enableProxy);

        proxyView.setVisibility(enableProxy ? View.VISIBLE : View.GONE);

        proxyServerView = (EditText) findViewById(R.id.proxy_server);
        proxyServerView.setText(LoginActivity.getProxyServer());

        proxyPortView = (EditText) findViewById(R.id.proxy_port);
        proxyPortView.setText(LoginActivity.getProxyPort());

        proxyUserView = (EditText) findViewById(R.id.proxy_user);
        proxyUserView.setText(LoginActivity.getProxyUser());

        proxyPasswordView = (EditText) findViewById(R.id.proxy_password);
        proxyPasswordView.setText(LoginActivity.getProxyPassword());

        CheckBox checkStatic = (CheckBox) findViewById(R.id.checkbox_static);
        checkStatic.setChecked(enableStatic);

        staticView.setVisibility(enableStatic ? View.VISIBLE : View.GONE);

        addressView = (EditText) findViewById(R.id.address);
        netmaskView = (EditText) findViewById(R.id.netmask);
        gatewayView = (EditText) findViewById(R.id.gateway);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_GATT_CONNECTED);
        filter.addAction(Constants.ACTION_GATT_DISCONNECTED);
        filter.addAction(Constants.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(Constants.ACTION_WRITE_PACKET);
        filter.addAction(Constants.ACTION_DATA_AVAILABLE);
        filter.addAction(Constants.ACTION_DESCRIPTOR_WRITE);
        filter.addAction(Constants.ACTION_PIPELINE_BROKEN);

        registerReceiver(mGattUpdateReceiver, filter);

        setTitle(HubActivity.getSelectedMac() + " at " + LocationActivity.getSelectedLocation().getName());

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        mBluetoothGatt = HubActivity.getSelectedHub().connectGatt(this, false, mGattCallback);

        Button button = (Button) findViewById(R.id.back);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
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

    public void onCheckboxSetupClicked(View view) {
        boolean enableSetup = ((CheckBox) view).isChecked();
        setupView.setVisibility(enableSetup ? View.VISIBLE : View.GONE);
    }

    public void onCheckboxWifiClicked(View view) {
        View focus = this.getCurrentFocus();
        if (focus == wifiId || focus == wifiPassword) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        enableWifi = ((CheckBox) view).isChecked();
        wifiView.setVisibility(enableWifi ? View.VISIBLE : View.GONE);
    }

    public void onCheckboxProxyClicked(View view) {
        View focus = this.getCurrentFocus();
        if (focus == proxyServerView || focus == proxyPortView || focus == proxyUserView || focus == proxyPasswordView) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        enableProxy = ((CheckBox) view).isChecked();
        proxyView.setVisibility(enableProxy ? View.VISIBLE : View.GONE);
    }

    public void onCheckboxStaticClicked(View view) {
        View focus = this.getCurrentFocus();
        if (focus == addressView || focus == netmaskView || focus == gatewayView) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        enableStatic = ((CheckBox) view).isChecked();
        staticView.setVisibility(enableStatic ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBackPressed() {
        mBluetoothGatt.close();
        mBluetoothGatt.disconnect();

        unregisterReceiver(mGattUpdateReceiver);

        // code here to show dialog
        super.onBackPressed();
    }

    private void onTransmitClick() {
        showProgress(true, mContainerView, mProgressView);
        mHubLabel.setText(R.string.ble_device_configuring);

        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        hubName = hubNameView.getText().toString();
        hubDescription = hubDescriptionView.getText().toString();

        new ConfigurationTask().execute((Void) null);
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
        unregisterReceiver(mGattUpdateReceiver);

        Toast.makeText(this, getString(R.string.configuration_success), Toast.LENGTH_SHORT).show();

        sendBroadcast(new Intent(Constants.ACTION_UPDATE_HUB));
        ConfigurationActivity.this.finish();

        /*AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString( R.string.success));
        alertDialog.setMessage( getString( R.string.configuration_success) );
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString( R.string.ok ),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ConfigurationActivity.this.finish();
                        dialog.dismiss();
                    }
                });
        alertDialog.show(); */
    }

    private void terminateFailure() {
        bluetoothDisconnect();
        unregisterReceiver(mGattUpdateReceiver);

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.failure));
        alertDialog.setMessage(getString(R.string.configuration_failure));
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.ok),
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

    public String getProxySettings() {
        String proxyServer = proxyServerView.getText().toString();
        String proxyPort = proxyPortView.getText().toString();
        String proxyUser = proxyUserView.getText().toString();
        String proxyPassword = proxyPasswordView.getText().toString();

        return enableProxy ? (!proxyServer.isEmpty() ? (proxyServer + (!proxyPort.isEmpty() ? (":" + proxyPort) : "") + (!proxyUser.isEmpty() ? (" -U " + proxyUser + ":" + proxyPassword) : "")) : " ") : " ";
    }

    private void prepareConfiguration(String hospitalUrl) {
        String wifiAction = enableWifi ?
                (
                        "<ACTION id=\"A071\">1</ACTION>\n" +
                                "<ACTION id=\"A069\">" + wifiId.getText() + "</ACTION>\n" +
                                "<ACTION id=\"A070\">" + wifiPassword.getText() + "</ACTION>\n"
                ) : "<ACTION id=\"A071\">0</ACTION>\n";

        String staticAction = enableStatic ?
                (
                        "<ACTION id=\"A083\">1</ACTION>\n" +
                                "<ACTION id=\"A084\">" + addressView.getText() + "</ACTION>\n" +
                                "<ACTION id=\"A085\">" + netmaskView.getText() + "</ACTION>\n" +
                                "<ACTION id=\"A086\">" + gatewayView.getText() + "</ACTION>\n"
                ) : "<ACTION id=\"A083\">0</ACTION>\n";

        String setupCommand =
                "<ANSWER>\n" +
                        "<STATUS>0</STATUS>\n" +

                        "<ACTIONS>\n" +
                        "<ACTION id=\"A062\">" + pegasusToken + "</ACTION>\n" +

                        "<ACTION id=\"A081\">" + hospitalUrl + "/feed/cylinders</ACTION>\n" +

                        "<ACTION id=\"A064\">" + LocationActivity.getSelectedLocation().getName() + "</ACTION>\n" +
                        "<ACTION id=\"A065\">" + LocationActivity.getSelectedLocation().getId() + "</ACTION>\n" +

                        wifiAction +
                        staticAction +

                        "<ACTION id=\"A074\">" + getProxySettings() + "</ACTION>\n" +

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
        @Override
        protected Boolean doInBackground(Void... params) {
            Pegasus pegasus = Utils.getPegasus(hospitalUrl, HubActivity.getSelectedMac(), LoginActivity.getHospital().getId(), LoginActivity.getToken().getId(), timeout);

            if (pegasus != null) {
                pegasus.setLocationId(LocationActivity.getSelectedLocation().getId());
                pegasus.setHospitalId(LoginActivity.getHospital().getId());
                if (!hubName.isEmpty())
                    pegasus.setName(hubName);
                if (!hubDescription.isEmpty())
                    pegasus.setFriendlyName(hubDescription);
                pegasus.setDeleted("0");

                Utils.updatePegasus(hospitalUrl, pegasus, LoginActivity.getToken().getId(), timeout);
            } else {
                pegasus = new Pegasus();

                pegasus.setLocationId(LocationActivity.getSelectedLocation().getId());
                pegasus.setHospitalId(LoginActivity.getHospital().getId());
                pegasus.setName(hubName);
                pegasus.setFriendlyName(hubDescription);
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
                prepareConfiguration(hospitalUrl);

                currentCommand = 0;
                writeCommand(writeCommands.get(currentCommand));
            } else {
                showProgress(false, mContainerView, mProgressView);
            }
        }
    }
}
