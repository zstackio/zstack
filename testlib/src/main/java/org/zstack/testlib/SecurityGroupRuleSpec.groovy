package org.zstack.testlib
/**
 * Created by xing5 on 2017/2/20.
 */
class SecurityGroupRuleSpec implements Spec {
    String type
    String protocol
    Integer startPort
    Integer endPort
    String allowedCidr

    SpecID create(String uuid, String sessionId) {
        addSecurityGroupRule {
            delegate.sessionId = sessionId
            delegate.rules = [[
                    "type": type,
                    "protocol": protocol,
                    "startPort": startPort,
                    "endPort": endPort,
                    "allowedCidr": allowedCidr
            ]]
            delegate.securityGroupUuid = (parent as SecurityGroupSpec).inventory.uuid
        }

        return null
    }
}
