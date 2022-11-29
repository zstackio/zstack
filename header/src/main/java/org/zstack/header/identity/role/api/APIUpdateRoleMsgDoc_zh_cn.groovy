package org.zstack.header.identity.role.api

import org.zstack.header.identity.role.api.APIUpdateRoleEvent

doc {
    title "UpdateRole"

    category "rbac"

    desc """更新角色"""

    rest {
        request {
			url "PUT /v1/identities/roles/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateRoleMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateRole"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateRole"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateRole"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "statements"
					enclosedIn "updateRole"
					desc "角色权限描述"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "policyUuids"
					enclosedIn "updateRole"
					desc "角色权限uuid"
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
            clz APIUpdateRoleEvent.class
        }
    }
}