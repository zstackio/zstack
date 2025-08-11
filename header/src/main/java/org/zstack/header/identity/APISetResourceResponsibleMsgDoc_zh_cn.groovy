package org.zstack.header.identity

import org.zstack.header.identity.APISetResourceResponsibleEvent

doc {
    title "SetResourceResponsible"

    category "identity"

    desc """配置资源责任关系"""

    rest {
        request {
			url "PUT /v1/resources/responsible/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APISetResourceResponsibleMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "setResourceResponsible"
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "4.4.52"
					
				}
				column {
					name "responsibleUuids"
					enclosedIn "setResourceResponsible"
					desc ""
					location "body"
					type "List"
					optional false
					since "4.4.52"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.4.52"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.4.52"
					
				}
			}
        }

        response {
            clz APISetResourceResponsibleEvent.class
        }
    }
}