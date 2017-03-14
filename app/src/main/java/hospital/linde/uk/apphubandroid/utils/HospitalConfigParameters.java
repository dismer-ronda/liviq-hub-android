package hospital.linde.uk.apphubandroid.utils;

import lombok.Data;

/**
 * Created by dismer on 7/03/17.
 */
@Data
public class HospitalConfigParameters
{
    private Integer id;

    private Boolean proxyEnabled;
    private String proxyServer;
    private Integer proxyPort;
    private String proxyUser;
    private String proxyPassword;

    private Boolean wifiEnabled;
    private String wifiSSID;
    private String wifiSecurityKey;

    private Boolean mobileEnabled;
    private String mobileUser;
    private String mobilePassword;

    private Integer emptyLevelForFillPressure;
    private Integer emptyPressureCB;
    private Integer cylinderDwellTimeForOrder;
    private Integer lostCylinderDetectionTimeWindow;
    private Integer cylinderDetectionTimeWindow;
    private Integer fromStoreTimeWindow;
    private Integer hospitalId;
    private String deleted;
}
