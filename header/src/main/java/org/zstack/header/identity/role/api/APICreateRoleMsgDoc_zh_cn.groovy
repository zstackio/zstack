package org.zstack.header.identity.role.api

import org.zstack.header.identity.role.api.APICreateRoleEvent

doc {
    title "CreateRole"

    category "rbac"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/identities/roles"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateRoleMsg.class

            desc """"""
            
			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "statements"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "policyUuids"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
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
				column {
					name "identity"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
        }

        response {
            clz APICreateRoleEvent.class
        }
    }
}