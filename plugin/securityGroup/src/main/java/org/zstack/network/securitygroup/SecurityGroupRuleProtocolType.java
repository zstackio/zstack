package org.zstack.network.securitygroup;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public enum SecurityGroupRuleProtocolType {
    TCP,
    UDP,
    ICMP,
    ALL
}
