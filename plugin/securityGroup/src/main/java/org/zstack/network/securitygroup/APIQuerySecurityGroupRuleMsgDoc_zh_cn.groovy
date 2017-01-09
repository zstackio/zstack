package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIQuerySecurityGroupRuleReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QuerySecurityGroupRule"

    category "securityGroup"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/security-groups/rules"

			url "GET /v1/security-groups/rules/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQuerySecurityGroupRuleMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQuerySecurityGroupRuleReply.class
        }
    }
}