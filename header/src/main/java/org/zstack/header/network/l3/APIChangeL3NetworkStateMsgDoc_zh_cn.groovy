package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIChangeL3NetworkStateEvent

doc {
    title "ChangeL3NetworkState"

    category "network.l3"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/l3-networks/{uuid}/actions"


            header (OAuth: 'the-session-uuid')

            clz APIChangeL3NetworkStateMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "changeL3NetworkState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "stateEvent"
					enclosedIn "changeL3NetworkState"
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
            clz APIChangeL3NetworkStateEvent.class
        }
    }
}