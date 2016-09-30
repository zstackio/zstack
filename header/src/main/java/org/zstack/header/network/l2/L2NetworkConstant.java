package org.zstack.header.network.l2;

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

    public static final String DETACH_L2NETWORK_CODE = "l2Network.detach";
}
