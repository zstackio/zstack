package org.zstack.network.service.portforwarding



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/port-forwarding/{ruleUuid}/vm-instances/candidate-nics"


            header (OAuth: 'the-session-uuid')

            clz APIGetPortForwardingAttachableVmNicsMsg.class

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
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIGetPortForwardingAttachableVmNicsReply.class
        }
    }
}