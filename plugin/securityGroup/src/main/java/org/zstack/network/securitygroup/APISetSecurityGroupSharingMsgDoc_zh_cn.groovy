package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APISetSecurityGroupSharingEvent

doc {
    title "设置安全组共享范围"

    category "securityGroup"

    desc """设置安全组共享范围"""

    rest {
        request {
			url "PUT /v1/security-groups/{securityGroupUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APISetSecurityGroupSharingMsg.class

            desc """"""
            
			params {

				column {
					name "securityGroupUuid"
					enclosedIn "setSecurityGroupSharing"
					desc "安全组UUID"
					location "url"
					type "String"
					optional false
					since "5.7.0"
				}
				column {
					name "mode"
					enclosedIn "setSecurityGroupSharing"
					desc "共享模式：None 不共享，Specified 指定共享，Public 全局共享"
					location "body"
					type "String"
					optional false
					since "5.7.0"
					values ("None","Specified","Public")
				}
				column {
					name "accountUuids"
					enclosedIn "setSecurityGroupSharing"
					desc "指定共享的最终账户名单；项目使用 linkedAccountUuid，仅 Specified 模式可传"
					location "body"
					type "List"
					optional true
					since "5.7.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.7.0"
				}
			}
        }

        response {
            clz APISetSecurityGroupSharingEvent.class
        }
    }
}