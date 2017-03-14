package hospital.linde.uk.apphubandroid.utils;

import lombok.Data;

/**
 * Created by dismer on 7/03/17.
 */

@Data
public class Token
{
    private String id;
    private Integer ttl;
    private String created;
    private Integer userId;
    private User user;
}
