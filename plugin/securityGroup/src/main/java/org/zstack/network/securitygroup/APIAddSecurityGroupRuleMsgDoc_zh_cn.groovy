package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIAddSecurityGroupRuleEvent

doc {
    title "AddSecurityGroupRule"

    category "securityGroup"

    desc """用户可以使用AddSecurityGroupRule添加规则到安全组"""

    rest {
        request {
			url "POST /v1/security-groups/{securityGroupUuid}/rules"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIAddSecurityGroupRuleMsg.class

            desc """用户可以使用AddSecurityGroupRule添加规则到安全组"""
            
			params {

				column {
					name "securityGroupUuid"
					enclosedIn "params"
					desc "安全组UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "rules"
					enclosedIn "params"
					desc "安全组中的规则"
					location "body"
					type "List"
					optional false
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIAddSecurityGroupRuleEvent.class
        }
    }
}