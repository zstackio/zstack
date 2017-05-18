package org.zstack.network.service;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by weiwang on 13/05/2017.
 */
@GlobalConfigDefinition
public class NetworkServiceGlobalConfig {
    public static final String CATEGORY = "networkService";

    @GlobalConfigValidation
    public static GlobalConfig DHCP_MTU_NO_VLAN = new GlobalConfig(CATEGORY, "defaultDhcpMtu.l2NoVlanNetwork");
    @GlobalConfigValidation
    public static GlobalConfig DHCP_MTU_VLAN = new GlobalConfig(CATEGORY, "defaultDhcpMtu.l2VlanNetwork");
    @GlobalConfigValidation
    public static GlobalConfig DHCP_MTU_VXLAN = new GlobalConfig(CATEGORY, "defaultDhcpMtu.l2VxlanNetwork");
    @GlobalConfigValidation
    public static GlobalConfig DHCP_MTU_DUMMY = new GlobalConfig(CATEGORY, "defaultDhcpMtu.dummyNetwork");
}
