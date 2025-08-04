package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.zstack.header.configuration.PythonClass;

/**
 * Created by weiwang on 02/03/2017.
 */
@PythonClass
public class VxlanNetworkConstant {
    @PythonClass
    public static final String VXLAN_NETWORK_TYPE = "VxlanNetwork";

    //vlxan id range
    public static final int MIN_VNI = 1;
    public static final int MAX_VNI = 16777215;

    //vlan id range
    public static final int MIN_VLAN = 1;
    public static final int MAX_VLAN = 4095;
}
