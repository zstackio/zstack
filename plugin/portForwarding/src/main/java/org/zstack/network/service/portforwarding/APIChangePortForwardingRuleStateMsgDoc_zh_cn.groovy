package org.zstack.network.service.portforwarding



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/port-forwarding/{uuid}/actions"


            header (OAuth: 'the-session-uuid')

            clz APIChangePortForwardingRuleStateMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "changePortForwardingRuleState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "stateEvent"
					enclosedIn "changePortForwardingRuleState"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("enable","disable")
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
            clz APIChangePortForwardingRuleStateEvent.class
        }
    }
}