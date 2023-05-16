package org.zstack.header.identity

import org.zstack.header.identity.APICheckResourcePermissionReply

doc {
    title "CheckResourcePermission"

    category "rbac"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/accounts/resource/api-permissions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICheckResourcePermissionMsg.class

            desc """"""
            
			params {

				column {
					name "resourceType"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APICheckResourcePermissionReply.class
        }
    }
}