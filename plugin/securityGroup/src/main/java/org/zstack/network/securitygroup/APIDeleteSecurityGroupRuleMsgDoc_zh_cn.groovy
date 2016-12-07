package org.zstack.network.securitygroup



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "DELETE /v1/security-groups/rules"


            header (OAuth: 'the-session-uuid')

            clz APIDeleteSecurityGroupRuleMsg.class

            desc ""
            
			params {

				column {
					name "ruleUuids"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional false
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIDeleteSecurityGroupRuleEvent.class
        }
    }
}