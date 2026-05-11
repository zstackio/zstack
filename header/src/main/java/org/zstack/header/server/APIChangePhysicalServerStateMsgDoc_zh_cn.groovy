package org.zstack.header.server

import org.zstack.header.server.APIChangePhysicalServerStateEvent

doc {
    title "ChangePhysicalServerState"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/physical-servers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIChangePhysicalServerStateMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "changePhysicalServerState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "stateEvent"
					enclosedIn "changePhysicalServerState"
					desc ""
					location "body"
					type "String"
					optional false
					since "5.5.16"
					values ("enable","disable","maintain")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.16"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.16"
				}
			}
        }

        response {
            clz APIChangePhysicalServerStateEvent.class
        }
    }
}