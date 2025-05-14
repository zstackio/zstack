package org.zstack.network.service.portforwarding

import org.zstack.network.service.portforwarding.APIChangePortForwardingRuleEvent

doc {
    title "ChangePortForwardingRule"

    category "portForwarding"

    desc """修改端口转发规则"""

    rest {
        request {
			url "PUT /v1/port-forwarding/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIChangePortForwardingRuleMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "changePortForwardingRule"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.4.0"
				}
				column {
					name "allowedCidr"
					enclosedIn "changePortForwardingRule"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.4.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.4.0"
				}
			}
        }

        response {
            clz APIChangePortForwardingRuleEvent.class
        }
    }
}