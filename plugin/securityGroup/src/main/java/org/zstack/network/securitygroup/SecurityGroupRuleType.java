package org.zstack.network.securitygroup;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public enum SecurityGroupRuleType {
    Ingress,
    Egress,
}
