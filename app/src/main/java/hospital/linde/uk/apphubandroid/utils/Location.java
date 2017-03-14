package hospital.linde.uk.apphubandroid.utils;

import lombok.Data;

/**
 * Created by dismer on 7/03/17.
 */
@Data
public class Location
{
    private String id;
    private String desc;
    private String name;
    private Integer hospitalId;
    private Integer locationTypeId;
    private String idFor3DModelLocationName;
    private String deleted;
    private Integer configParametersId;
}
