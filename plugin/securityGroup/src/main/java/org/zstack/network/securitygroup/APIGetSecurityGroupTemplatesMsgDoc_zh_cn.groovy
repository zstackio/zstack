package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIGetSecurityGroupTemplatesReply

doc {
    title "查询内置安全组模板"

    category "securityGroup"

    desc """查询内置安全组模板"""

    rest {
        request {
			url "GET /v1/security-groups/templates"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetSecurityGroupTemplatesMsg.class

            desc """"""
            
			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "5.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "5.7.0"
				}
			}
        }

        response {
            clz APIGetSecurityGroupTemplatesReply.class
        }
    }
}