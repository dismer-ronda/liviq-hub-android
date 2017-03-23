package hospital.linde.uk.apphubandroid.utils;

import android.os.Parcel;
import android.os.Parcelable;

import lombok.Data;

/**
 * Created by dismer on 23/03/17.
 */
@Data
public class ConfigurationData implements Parcelable {
    private boolean enableWifi;
    private boolean enableProxy;
    private boolean enableStatic;

    private String hubName;
    private String hubDescription;

    private String wifiId;
    private String wifiPassword;

    private String proxyServer;
    private String proxyPort;
    private String proxyUser;
    private String proxyPassword;

    private String address;
    private String netmask;
    private String gateway;

    public ConfigurationData() {
    }

    protected ConfigurationData(Parcel in) {
        enableWifi = in.readByte() != 0;
        enableProxy = in.readByte() != 0;
        enableStatic = in.readByte() != 0;
        hubName = in.readString();
        hubDescription = in.readString();
        wifiId = in.readString();
        wifiPassword = in.readString();
        proxyServer = in.readString();
        proxyPort = in.readString();
        proxyUser = in.readString();
        proxyPassword = in.readString();
        address = in.readString();
        netmask = in.readString();
        gateway = in.readString();
    }

    public static final Creator<ConfigurationData> CREATOR = new Creator<ConfigurationData>() {
        @Override
        public ConfigurationData createFromParcel(Parcel in) {
            return new ConfigurationData(in);
        }

        @Override
        public ConfigurationData[] newArray(int size) {
            return new ConfigurationData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeByte((byte) (enableWifi ? 1 : 0));
        out.writeByte((byte) (enableProxy ? 1 : 0));
        out.writeByte((byte) (enableStatic ? 1 : 0));

        out.writeString(hubName);
        out.writeString(hubDescription);

        out.writeString(wifiId);
        out.writeString(wifiPassword);

        out.writeString(proxyServer);
        out.writeString(proxyPort);
        out.writeString(proxyUser);
        out.writeString(proxyPassword);

        out.writeString(address);
        out.writeString(netmask);
        out.writeString(gateway);
    }
}
