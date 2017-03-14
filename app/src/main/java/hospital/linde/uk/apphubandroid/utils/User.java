package hospital.linde.uk.apphubandroid.utils;

import lombok.Data;

/**
 * Created by dismer on 7/03/17.
 */
@Data
public class User
{
    private Integer id;
    private Integer roleId;
    private Integer hospitalId;
    private String lastLogin;
    private String acceptedTCsOn;
    private String deleted;
    private String username;
    private String email;
    private String password;
}
