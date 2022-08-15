package org.zstack.sdnController.header;

import org.zstack.header.configuration.PythonClass;

/**
 * Created by shixin on 09/17/2019.
 */
@PythonClass
public class SdnControllerConstant {
    @PythonClass
    public static final String HARDWARE_VXLAN_NETWORK_POOL_TYPE = "HardwareVxlanNetworkPool";
    @PythonClass
    public static final String HARDWARE_VXLAN_NETWORK_TYPE = "HardwareVxlanNetwork";

    public static final String ACTION_CATEGORY = "SdnController";

    public static final String SERVICE_ID = "SdnController";

    public static final String H3C_VCFC_CONTROLLER = "H3C VCFC";
    public static final String H3C_VCFC_DEFAULT_TENANT_NAME = "default";
    public static final String H3C_VCFC_DEFAULT_TENANT_TYPE = "default";

    public enum Params {
        HARDWARE_VXLAN_POOLS,
        VXLAN_NETWORK,
    }
}
