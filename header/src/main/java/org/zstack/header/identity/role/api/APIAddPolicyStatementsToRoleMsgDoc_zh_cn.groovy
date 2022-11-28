package org.zstack.header.identity.role.api

import org.zstack.header.identity.role.api.APIAddPolicyStatementsToRoleEvent

doc {
    title "AddPolicyStatementsToRole"

    category "rbac"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/identities/roles/{uuid}/policy-statements"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIAddPolicyStatementsToRoleMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "statements"
					enclosedIn "params"
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
            clz APIAddPolicyStatementsToRoleEvent.class
        }
    }
}