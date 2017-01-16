package org.zstack.header.host

import org.zstack.header.host.APIChangeHostStateEvent

doc {
    title "ChangeHostState"

    category "host"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/hosts/{uuid}/actions"


            header (OAuth: 'the-session-uuid')

            clz APIChangeHostStateMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "changeHostState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "stateEvent"
					enclosedIn "changeHostState"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("enable","disable","maintain")
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
            clz APIChangeHostStateEvent.class
        }
    }
}