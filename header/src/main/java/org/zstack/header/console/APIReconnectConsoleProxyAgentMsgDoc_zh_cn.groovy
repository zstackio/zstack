package org.zstack.header.console



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/consoles/agents"


            header (OAuth: 'the-session-uuid')

            clz APIReconnectConsoleProxyAgentMsg.class

            desc ""
            
			params {

				column {
					name "agentUuids"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional true
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
            clz APIReconnectConsoleProxyAgentEvent.class
        }
    }
}