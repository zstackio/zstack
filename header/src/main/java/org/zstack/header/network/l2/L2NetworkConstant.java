package org.zstack.header.network.l2;

import org.springframework.security.access.method.P;
import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface L2NetworkConstant {
    public static final String SERVICE_ID = "network.l2";
    public static final String L2_VLAN_NETWORK_FACTORY_SERVICE_ID = "network.l2.vlan";

    public static final String ACTION_CATEGORY = "l2Network";

    @PythonClass
    public static final String L2_NO_VLAN_NETWORK_TYPE = "L2NoVlanNetwork";
    @PythonClass
    public static final String L2_VLAN_NETWORK_TYPE = "L2VlanNetwork";
    @PythonClass
    public static final String HARDWARE_VXLAN_NETWORK_POOL_TYPE = "HardwareVxlanNetworkPool";
    @PythonClass
    public static final String HARDWARE_VXLAN_NETWORK_TYPE = "HardwareVxlanNetwork";
    @PythonClass
    public static final String VXLAN_NETWORK_TYPE = "VxlanNetwork";
    @PythonClass
    public static final String VXLAN_NETWORK_POOL_TYPE = "VxlanNetworkPool";

    @PythonClass
    public static final String VSWITCH_TYPE_LINUX_BRIDGE = "LinuxBridge";
    @PythonClass
    public static final String VSWITCH_TYPE_OVS_DPDK = "OvsDpdk";
    @PythonClass
    public static final String VSWITCH_TYPE_MACVLAN = "MacVlan";
    @PythonClass
    public static final String VSWITCH_TYPE_OVS_KERNEL = "OvsKernel";

    public static final String DETACH_L2NETWORK_CODE = "l2Network.detach";

    // https://elixir.bootlin.com/linux/v5.6/source/include/uapi/linux/if.h#L33
    public static final int LINUX_IF_NAME_MAX_SIZE = 15;

    public static final int VIRTUAL_NETWORK_ID_DEFAULT_VALUE = 0;
}
