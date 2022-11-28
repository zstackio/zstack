package org.zstack.header.identity.role.api

import org.zstack.header.identity.role.api.APIDetachRoleFromAccountEvent

doc {
    title "DetachRoleFromAccount"

    category "rbac"

    desc """在这里填写API描述"""

    rest {
        request {
			url "DELETE /v1/identities/accounts/{accountUuid}/roles/{roleUuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDetachRoleFromAccountMsg.class

            desc """"""
            
			params {

				column {
					name "roleUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "accountUuid"
					enclosedIn ""
					desc "账户UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
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
            clz APIDetachRoleFromAccountEvent.class
        }
    }
}