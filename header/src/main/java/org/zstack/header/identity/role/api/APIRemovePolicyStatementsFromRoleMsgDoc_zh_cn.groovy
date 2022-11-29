package org.zstack.header.identity.role.api

import org.zstack.header.identity.role.api.APIRemovePolicyStatementsFromRoleEvent

doc {
    title "RemovePolicyStatementsFromRole"

    category "rbac"

    desc """从角色移除权限描述"""

    rest {
        request {
			url "DELETE /v1/identities/roles/{uuid}/policy-statements"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIRemovePolicyStatementsFromRoleMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "policyStatementUuids"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional false
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
            clz APIRemovePolicyStatementsFromRoleEvent.class
        }
    }
}