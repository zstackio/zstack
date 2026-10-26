package org.zstack.network.securitygroup;

import org.zstack.header.configuration.PythonClassInventory;

@PythonClassInventory
public class SecurityGroupTemplateRule extends APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO {
    private Integer priority;

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
