package hospital.linde.uk.apphubandroid.utils;

import lombok.Data;

/**
 * Created by dismer on 7/03/17.
 */
@Data
public class Pegasus
{
    private Integer id;
    private String macAddress;
    private String name;
    private String friendlyName;
    private String type;
    private String version;
    private String imei;
    private String ccid;
    private String csq;
    private String lastSeen;
    private String lastLookup;
    private Integer workingMode;
    private Boolean statusHttp;
    private Boolean statusHttps;
    private Boolean statusInternet;
    private Boolean statusInsights;
    private Integer hospitalId;
    private String deleted;
    private Boolean lost;
    private Integer locationId;
    private String currentLocation;
}
