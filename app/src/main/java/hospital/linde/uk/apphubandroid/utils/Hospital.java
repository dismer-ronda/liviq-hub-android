package hospital.linde.uk.apphubandroid.utils;

import lombok.Data;

/**
 * Created by dismer on 7/03/17.
 */
@Data
public class Hospital
{
    private Integer id;
    private String name;
    private String customerContactDetails;
    private String lindeContactDetails;
    private Integer defaultFillPressureCB;
    private Integer hubLostConnectivityThresholdMinutes;
    private Integer countryId;
    private String deleted;
    private Integer primaryAdminId;
    private Integer serviceLevelId;
    private Integer configParametersId;
    private HospitalConfigParameters configParameters;
}
