package org.zstack.header.identity.role.api

import org.zstack.header.identity.role.api.APIChangeRoleStateEvent

doc {
    title "ChangeRoleState"

    category "rbac"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/identities/roles/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIChangeRoleStateMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "changeRoleState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changeRoleState"
					desc ""
					location "body"
					type "RoleStateEvent"
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
            clz APIChangeRoleStateEvent.class
        }
    }
}