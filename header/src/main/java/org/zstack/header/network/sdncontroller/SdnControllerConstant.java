package org.zstack.header.network.sdncontroller;

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

    public static final String DEFAULT_VENDOR_VERSION = "V1";

    public static final String ACTION_CATEGORY = "SdnController";

    public static final String SERVICE_ID = "SdnController";

    public static final String H3C_VCFC_CONTROLLER = "H3C VCFC";

    public static final String H3C_VCFC_VENDOR_VERSION_V1 = DEFAULT_VENDOR_VERSION;
    public static final String H3C_VCFC_VENDOR_VERSION_V2 = "V2";

    public static final String H3C_VCFC_DEFAULT_TENANT_NAME = "default";
    public static final String H3C_VCFC_DEFAULT_TENANT_TYPE = "default";

    // H3C SDN Controller Tenant Status
    public static final String H3C_SDN_CONTROLLER_TENANT_STATE_ENABLE = "Enabled";
    public static final String H3C_SDN_CONTROLLER_TENANT_STATE_DISABLE = "Disabled";

    // H3C SDN Controller Default Tenant ID (cannot be deleted)
    public static final String H3C_SDN_CONTROLLER_DEFAULT_TENANT_ID = "ffffffff-0000-0000-0000-000000000001";

    public static final String TF_CONTROLLER = "TF";

    public static final String SDN_CONTROLLER_VROUTER_PREFIX = "VR_";

    public static final String DEFAULT_SDN_CONTROLLER_VERSION = "V1";

    public enum Processes{
        Pre,
        Post
    }

    public enum Operations {
        Init,
        Create,
        AttachToCluster,
        DetachFromCluster,
        Delete
    }

    public enum ResourceTypes{
        SdnController,
        VxlanNetworkPool,
        VxlanNetwork
    }

    public enum Params {
        HARDWARE_VXLAN_POOLS,
        VXLAN_NETWORK,
        SDN_CONTROLLER,
    }
}
