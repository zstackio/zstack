package org.zstack.header.identity

import org.zstack.header.identity.APICheckApiPermissionReply

doc {
    title "CheckApiPermission"

    category "identity"

    desc """检查API权限"""

    rest {
        request {
			url "PUT /v1/accounts/permissions/actions"

			header (Authorization: 'OAuth the-session-uuid')


            clz APICheckApiPermissionMsg.class

            desc """检查API权限"""
            
			params {

				column {
					name "userUuid"
					enclosedIn "checkApiPermission"
					desc "用户UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "apiNames"
					enclosedIn "checkApiPermission"
					desc "API名称列表"
					location "body"
					type "List"
					optional false
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APICheckApiPermissionReply.class
        }
    }
}