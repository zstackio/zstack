package org.zstack.header.identity

import org.zstack.header.identity.APILogOutReply

doc {
    title "LogOut"

    category "identity"

    desc """退出当前登录状态"""

    rest {
        request {
			url "DELETE /v1/accounts/sessions/{sessionUuid}"




            clz APILogOutMsg.class

            desc """退出当前登录状态"""
            
			params {

				column {
					name "sessionUuid"
					enclosedIn ""
					desc "会话UUID"
					location "url"
					type "String"
					optional true
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
            clz APILogOutReply.class
        }
    }
}