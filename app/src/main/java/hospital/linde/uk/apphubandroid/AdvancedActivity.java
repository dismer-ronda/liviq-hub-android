package hospital.linde.uk.apphubandroid;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import hospital.linde.uk.apphubandroid.utils.ConfigurationData;
import hospital.linde.uk.apphubandroid.utils.Constants;
import hospital.linde.uk.apphubandroid.utils.Utils;

public class AdvancedActivity extends MyBaseActivity {
    private final static String TAG = AdvancedActivity.class.getSimpleName();

    private TextView topLabel;

    private View wifiView;
    private View proxyView;
    private View staticView;

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

    private boolean enableWifi;
    private boolean enableProxy;
    private boolean enableStatic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced);

        enableWifi = LoginActivity.getHospital().getConfigParameters().getWifiEnabled() == null ? false : LoginActivity.getHospital().getConfigParameters().getWifiEnabled();
        enableProxy = LoginActivity.getHospital().getConfigParameters().getProxyEnabled() == null ? false : LoginActivity.getHospital().getConfigParameters().getProxyEnabled();
        enableStatic = false; /*LoginActivity.getHospital().getConfigParameters().getProxyEnabled() == null ? false : LoginActivity.getHospital().getConfigParameters().getStaticEnabled();*/

        topLabel = (TextView) findViewById(R.id.top_label);
        topLabel.setText(getString(R.string.setup_label_format).replace("$hub", HubActivity.getSelectedPegasus().getName()).replace("$location", LocationActivity.getSelectedLocation().getName()));

        TextView titleLabel = (TextView) findViewById(R.id.title_label);
        titleLabel.setText(LoginActivity.getHospital().getName());

        wifiView = findViewById(R.id.wifi_configuration);
        proxyView = findViewById(R.id.proxy_configuration);
        staticView = findViewById(R.id.static_configuration);

        hubNameView = (EditText) findViewById(R.id.hub_name);
        hubNameView.setText(HubActivity.getSelectedPegasus().getName());

        hubDescriptionView = (EditText) findViewById(R.id.hub_description);
        hubDescriptionView.setText(HubActivity.getSelectedPegasus().getFriendlyName());

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

        Button button = (Button) findViewById(R.id.back);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
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

    private void onTransmitClick() {

        ConfigurationData data = new ConfigurationData();

        data.setEnableWifi(enableWifi);
        data.setEnableProxy(enableProxy);
        data.setEnableStatic(enableStatic);

        data.setHubName(Utils.getEmptyIfNull(hubNameView.getText().toString()));
        data.setHubDescription(Utils.getEmptyIfNull(hubDescriptionView.getText().toString()));

        data.setWifiId(Utils.getEmptyIfNull(wifiId.getText().toString()));
        data.setWifiPassword(Utils.getEmptyIfNull(wifiPassword.getText().toString()));

        data.setProxyServer(Utils.getEmptyIfNull(proxyServerView.getText().toString()));
        data.setProxyPort(Utils.getEmptyIfNull(proxyPortView.getText().toString()));
        data.setProxyUser(Utils.getEmptyIfNull(proxyUserView.getText().toString()));
        data.setProxyPassword(Utils.getEmptyIfNull(proxyPasswordView.getText().toString()));

        data.setAddress(Utils.getEmptyIfNull(addressView.getText().toString()));
        data.setNetmask(Utils.getEmptyIfNull(netmaskView.getText().toString()));
        data.setGateway(Utils.getEmptyIfNull(gatewayView.getText().toString()));

        Intent intent = new Intent(Constants.ACTION_TRANSMIT);
        intent.putExtra(Constants.TRANSMIT_DATA, data);
        sendBroadcast(intent);

        finish();
    }
}
