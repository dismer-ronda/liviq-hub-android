package hospital.linde.uk.apphubandroid;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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

import hospital.linde.uk.apphubandroid.utils.Utils;

public class ConfigurationActivity extends AppCompatActivity {
    private final static String TAG = ConfigurationActivity.class.getSimpleName();

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED                = "hospital.linde.uk.apphubandroid.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED             = "hospital.linde.uk.apphubandroid.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED      = "hospital.linde.uk.apphubandroid.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE                = "hospital.linde.uk.apphubandroid.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA                           = "hospital.linde.uk.apphubandroid.EXTRA_DATA";
    public final static String ACTION_WRITE_PACKET                  = "hospital.linde.uk.apphubandroid.ACTION_WRITE_PACKET";
    public final static String ACTION_DESCRIPTOR_WRITE              = "hospital.linde.uk.apphubandroid.ACTION_DESCRIPTOR_WRITE";
    public final static String ACTION_PIPELINE_BROKEN               = "hospital.linde.uk.apphubandroid.ACTION_PIPELINE_BORKEN";

    public final static String IQHubMainCharacteristicsUuid         = "0734594a-a8e7-4b1a-a6b1-cd5243059a57";
    public final static String IQHubMainServivceUuid                = "14839ac4-7d7e-415c-9a42-167340cf2339";
    public final static String IQHubCharacteristicsDescriptorUuid   = "00002902-0000-1000-8000-00805f9b34fb";

    private View mHubFormView;
    private View mProgressView;

    private View wifiView;
    private View proxyView;

    private TextView mHubLabel;
    private CheckBox checkWifi;
    private CheckBox checkProxy;

    private EditText wifiId;
    private EditText wifiPassword;

    private EditText proxyServerView;
    private EditText proxyPortView;
    private EditText proxyUserView;
    private EditText proxyPasswordView;

    private int mConnectionState = STATE_DISCONNECTED;

    private BluetoothGatt mBluetoothGatt;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;

    private List<byte[]> writeCommands;
    private int currentCommand;

    private boolean connected = false;

    private boolean enableWifi = false;
    private boolean enableProxy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        enableWifi = LoginActivity.getHospital().getConfigParameters().getWifiEnabled() == null ? false : LoginActivity.getHospital().getConfigParameters().getWifiEnabled();
        enableProxy = LoginActivity.getHospital().getConfigParameters().getProxyEnabled() == null ? false : LoginActivity.getHospital().getConfigParameters().getProxyEnabled();

        mHubFormView = findViewById(R.id.scroll_configuration);
        mProgressView = findViewById(R.id.search_progress);

        wifiView = findViewById(R.id.wifi_configuration);
        proxyView = findViewById(R.id.proxy_configuration);

        mHubLabel = (TextView)findViewById(R.id.hub_label);
        mHubLabel.setText( R.string.ble_device_configure );

        checkWifi = (CheckBox) findViewById(R.id.checkbox_wifi);
        checkWifi.setChecked( enableWifi );

        wifiView.setVisibility( enableWifi ? View.VISIBLE : View.GONE );

        wifiId = (EditText) findViewById(R.id.wifi_ssid);
        wifiId.setText( LoginActivity.getWifiSSID() );

        wifiPassword = (EditText) findViewById(R.id.wifi_password);
        wifiPassword.setText( LoginActivity.getWifiSecurityKey() );

        checkProxy = (CheckBox) findViewById(R.id.checkbox_proxy);
        checkProxy.setChecked( enableProxy );

        proxyView.setVisibility( enableProxy ? View.VISIBLE : View.GONE );

        proxyServerView = (EditText) findViewById(R.id.proxy_server);
        proxyServerView.setText( LoginActivity.getProxyServer() );

        proxyPortView = (EditText) findViewById(R.id.proxy_port);
        proxyPortView.setText( LoginActivity.getProxyPort() );

        proxyUserView = (EditText) findViewById(R.id.proxy_user);
        proxyUserView.setText( LoginActivity.getProxyUser() );

        proxyPasswordView = (EditText) findViewById(R.id.proxy_password);
        proxyPasswordView.setText( LoginActivity.getProxyPassword() );

        IntentFilter filter = new IntentFilter();
        filter.addAction( ACTION_GATT_CONNECTED );
        filter.addAction( ACTION_GATT_DISCONNECTED );
        filter.addAction( ACTION_GATT_SERVICES_DISCOVERED );
        filter.addAction( ACTION_WRITE_PACKET );
        filter.addAction( ACTION_DATA_AVAILABLE );
        filter.addAction( ACTION_DESCRIPTOR_WRITE );
        filter.addAction( ACTION_PIPELINE_BROKEN );

        registerReceiver( mGattUpdateReceiver, filter );

        setTitle( HubActivity.getSelectedMac() + " at " + LocationActivity.getSelectedLocation().getName());

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        Button button = (Button) findViewById(R.id.back);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelClick();
            }
        });

        button = (Button) findViewById(R.id.transmit);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onTransmitClick();
            }
        });
    }

    public void onCheckboxWifiClicked(View view) {
        View focus = this.getCurrentFocus();
        if (focus == wifiId || focus == wifiPassword) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        enableWifi = ((CheckBox) view).isChecked();
        wifiView.setVisibility( enableWifi ? View.VISIBLE : View.GONE );
    }

    public void onCheckboxProxyClicked(View view) {
        View focus = this.getCurrentFocus();
        if (focus == proxyServerView || focus == proxyPortView || focus == proxyUserView || focus == proxyPasswordView) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        enableProxy = ((CheckBox) view).isChecked();
        proxyView.setVisibility( enableProxy ? View.VISIBLE : View.GONE );
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
        onBackPressed();
    }

    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    Log.i(TAG, "onConnectionStateChange status " + status + " state " + newState );
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        mConnectionState = STATE_CONNECTED;
                        Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
                        sendBroadcast( new Intent( ACTION_GATT_CONNECTED) );
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        mConnectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        sendBroadcast( new Intent( ACTION_GATT_DISCONNECTED) );
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        sendBroadcast( new Intent( ACTION_GATT_SERVICES_DISCOVERED ) );
                    }
                    else
                        sendBroadcast( new Intent( ACTION_PIPELINE_BROKEN ) );
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    }
                    else
                        sendBroadcast( new Intent( ACTION_PIPELINE_BROKEN ) );
                }

                @Override
                // Result of a characteristic changed operation
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }

                @Override
                // Result of a characteristic changed operation
                public void onReliableWriteCompleted(BluetoothGatt gatt, int status)
                {
                    Log.i(TAG, "onReliableWriteCompleted status " + status);
                    /*if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    }*/
                }

                @Override
                // Result of a characteristic read operation
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    sendBroadcast( new Intent( status == BluetoothGatt.GATT_SUCCESS ? ACTION_DESCRIPTOR_WRITE : ACTION_PIPELINE_BROKEN ) );
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    sendBroadcast( new Intent( status == BluetoothGatt.GATT_SUCCESS ? ACTION_WRITE_PACKET : ACTION_PIPELINE_BROKEN ) );
                }

                private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
                    Intent intent = new Intent(action);
                    intent.putExtra( EXTRA_DATA, characteristic.getValue() );
                    sendBroadcast(intent);
                }
            };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            Log.i( TAG, "onReceive " + action );

            if (ConfigurationActivity.ACTION_GATT_CONNECTED.equals(action)) {
                onBleConnected();
            } else if (ConfigurationActivity.ACTION_GATT_DISCONNECTED.equals(action)) {
                onBleDisconnected();
            } else if (ConfigurationActivity.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                onServiceDiscovered();
            } else if (ConfigurationActivity.ACTION_DATA_AVAILABLE.equals(action)) {
                onReceivedResponse( intent );
            } else if (ConfigurationActivity.ACTION_DESCRIPTOR_WRITE.equals(action)) {
                onDescriptorWrite();
            } else if (ConfigurationActivity.ACTION_WRITE_PACKET.equals(action)) {
                onCharacteristicWrite();
            } else if (ConfigurationActivity.ACTION_PIPELINE_BROKEN.equals(action)) {
                onPipelineBroken();
            }
        }
    };

    private void onBleConnected()
    {
        connected = true;
    }

    private void onBleDisconnected()
    {
        terminateFailure();;
    }

    private void onServiceDiscovered()
    {
        service = mBluetoothGatt.getService(UUID.fromString(IQHubMainServivceUuid));

        if (service == null) {
            Log.i(TAG, "service not found");
            terminateFailure();
        }
        else {
            characteristic = service.getCharacteristic(UUID.fromString(IQHubMainCharacteristicsUuid));
            if (characteristic == null) {
                Log.i(TAG, "service not found");
                terminateFailure();
            }
            else {
                mBluetoothGatt.setCharacteristicNotification(characteristic, true);

                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(IQHubCharacteristicsDescriptorUuid));

                if ( descriptor != null ) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                } else {
                    terminateFailure();
                }
            }
        }
    }

    private void onDescriptorWrite()
    {
        currentCommand = 0;
        writeCommand( writeCommands.get( currentCommand ) );
    }

    private void onReceivedResponse( Intent intent )
    {
        byte data[] = intent.getByteArrayExtra( ConfigurationActivity.EXTRA_DATA );

        if ( data != null && data.length > 0 && data[2] == 0x6f && data[3] == 0 ) {
            terminateSuccess();
            return;
        }

        terminateFailure();
    }

    private void onCharacteristicWrite()
    {
        if ( writeCommands != null ) {
            currentCommand++;
            if (currentCommand < writeCommands.size())
                writeCommand( writeCommands.get(currentCommand ) );
        }
    }

    private void onPipelineBroken()
    {
        terminateFailure();
    }

    private void bluetoothDisconnect()
    {
        writeCommands = null;

        mBluetoothGatt.close();
        mBluetoothGatt.disconnect();

        mHubLabel.setText( R.string.ble_device_configure );
        showProgress(false);
    }

    private void terminateSuccess()
    {
        bluetoothDisconnect();
        unregisterReceiver(mGattUpdateReceiver);

        Toast.makeText(this, getString( R.string.configuration_success ), Toast.LENGTH_SHORT).show();

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

    private void terminateFailure()
    {
        bluetoothDisconnect();

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString( R.string.failure));
        alertDialog.setMessage(getString(R.string.configuration_failure ));
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString( R.string.ok ),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private void writeCommand( byte[] command )
    {
        characteristic.setValue( command );
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    private byte[] getConfigurationPartBuffer( String part, int index )
    {
        byte [] buffer = new byte[part.length() + 3];

        buffer[0] = 0x6F;
        buffer[1] = (byte)index;
        buffer[2] = (byte)part.length();
        for ( int j = 0; j < part.length(); j++ )
            buffer[3 + j]= (byte)part.charAt( j );

        return buffer;
    }

    private byte[] getConfigurationCrcBuffer( short crc )
    {
        byte buffer[] = new byte[5];

        buffer[0] = 0x6F;
        buffer[1] = (byte)0xFF;
        buffer[2] = 0x02;
        buffer[3] = Utils.lobyte( crc );
        buffer[4] = Utils.hibyte( crc );

        return buffer;
    }

    public String getProxySettings()
    {
        String proxyServer = proxyServerView.getText().toString();
        String proxyPort = proxyPortView.getText().toString();
        String proxyUser = proxyUserView.getText().toString();
        String proxyPassword = proxyPasswordView.getText().toString();

        return enableProxy ? (!proxyServer.isEmpty() ? (proxyServer + (!proxyPort.isEmpty() ? (":" + proxyPort) : "") + (!proxyUser.isEmpty() ? (" -U " + proxyUser + ":" + proxyPassword) : "")) : " ") : " ";
    }

    private void prepareConfiguration()
    {
        String wifiAction = enableWifi ?
                (
                    "<ACTION id=\"A071\">1</ACTION>\n" +
                    "<ACTION id=\"A069\">" + wifiId.getText() +"</ACTION>\n" +
                    "<ACTION id=\"A070\">" + wifiPassword.getText() +"</ACTION>\n"
                ) : "<ACTION id=\"A071\">0</ACTION>\n";

        String setupCommand =
                "<ANSWER>\n" +
                        "<STATUS>0</STATUS>\n" +

                        "<ACTIONS>\n"  +

                            "<ACTION id=\"A064\">" + LocationActivity.getSelectedLocation().getName() +"</ACTION>\n" +
                            "<ACTION id=\"A065\">" + LocationActivity.getSelectedLocation().getId() +"</ACTION>\n" +

                            wifiAction +

                            "<ACTION id=\"A074\">" + getProxySettings() +"</ACTION>\n" +

                        "</ACTIONS>\n"+
                 "</ANSWER>\n";
        Log.i( TAG, setupCommand );

        int crc = Utils.getCRC16( setupCommand );

        List<String> parts = Utils.splitStringInParts( setupCommand, 15 );

        writeCommands = new ArrayList<byte[]>();

        for ( int i = 0; i < parts.size(); i++ )
            writeCommands.add( getConfigurationPartBuffer( parts.get(i), i ) );

        writeCommands.add( getConfigurationCrcBuffer( (short)crc ) );
    }

    private void onTransmitClick() {
        mHubLabel.setText( R.string.ble_device_configuring );
        showProgress(true);
        prepareConfiguration();

        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        mBluetoothGatt = HubActivity.getSelectedHub().connectGatt(this, false, mGattCallback);
    }

    @Override
    public void onBackPressed()
    {
        unregisterReceiver(mGattUpdateReceiver);

        // code here to show dialog
        super.onBackPressed();
    }
}
