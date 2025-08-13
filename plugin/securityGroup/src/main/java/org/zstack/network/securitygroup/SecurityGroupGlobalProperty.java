package org.zstack.network.securitygroup;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class SecurityGroupGlobalProperty {
    @GlobalProperty(name="upgradeSecurityGroup", defaultValue = "false")
    public static boolean UPGRADE_SECURITY_GROUP;
    @GlobalProperty(name="SecurityGroupRuleIpLimit", defaultValue = "50")
    public static int IP_GROUP_NUMBER_LIMIT;
    @GlobalProperty(name="SecurityGroupRulePortLimit", defaultValue = "50")
    public static int PORT_GROUP_NUMBER_LIMIT;
}
