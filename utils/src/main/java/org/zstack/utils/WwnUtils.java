package org.zstack.utils;

import java.util.UUID;

/**
 * Created by miao on 11/1/16.
 */
public class WwnUtils {
    final String oui = "000f";

    public String getRandomWwn() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String wwn = oui + uuid.substring(0, 12);
        return wwn;
    }

    public boolean isValidWwn(final String wwn) {
        return wwn.toLowerCase().matches(oui + "[0-9a-f]{12}");
    }
}
