package org.zstack.network.service.portforwarding

import org.zstack.network.service.portforwarding.APIAttachPortForwardingRuleEvent

doc {
    title "AttachPortForwardingRule"

    category "portForwarding"

    desc "在这里填写API描述"

    rest {
        request {
			url "POST /v1/port-forwarding/{ruleUuid}/vm-instances/nics/{vmNicUuid}"


            header (OAuth: 'the-session-uuid')

            clz APIAttachPortForwardingRuleMsg.class

            desc ""
            
			params {

				column {
					name "ruleUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "vmNicUuid"
					enclosedIn ""
					desc "云主机网卡UUID"
					location "url"
					type "String"
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
            clz APIAttachPortForwardingRuleEvent.class
        }
    }
}