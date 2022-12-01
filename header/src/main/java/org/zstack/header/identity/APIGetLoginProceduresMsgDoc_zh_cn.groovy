package org.zstack.header.identity

doc {
    title "GetLoginProcedures"

    category "identity"

    desc """获取指定用户的附加登录方式"""

    rest {
        request {

			url "GET /v1/accounts/login-procedures"


            clz APIGetLoginProceduresMsg.class

            desc """"""
            
			params {

				column {
					name "name"
					enclosedIn ""
					desc "用户名"
					location "query"
					type "String"
					optional false
					since "4.5.1"
					
				}
				column {
					name "type"
					enclosedIn ""
					desc "用户登录方式，支持 \"account\" / \"IAM2\""
					location "query"
					type "String"
					optional false
					since "4.5.1"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.5.1"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.5.1"
					
				}
			}
        }

        response {
            clz APIGetLoginProceduresReply.class
        }
    }
}