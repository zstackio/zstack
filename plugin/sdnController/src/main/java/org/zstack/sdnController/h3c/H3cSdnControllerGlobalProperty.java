package org.zstack.sdnController.h3c;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

import java.util.List;

@GlobalPropertyDefinition
public class H3cSdnControllerGlobalProperty {
    @GlobalProperty(name="H3c.Scheme", defaultValue = "http")
    public static String H3C_CONTROLLER_SCHEME;

    @GlobalProperty(name="H3c.Port", defaultValue = "80")
    public static int H3C_CONTROLLER_PORT;

    /* default timeout 30 seconds */
    @GlobalProperty(name="H3c.Timeout", defaultValue = "30000000")
    public static Long H3C_CONTROLLER_TIMEOUT;
}
