package org.zstack.header.identity

import org.zstack.header.identity.APICreateAccessKeyEvent

doc {
    title "CreateAccessKey"

    category "identity"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/accesskey"


            header(Authorization: 'OAuth the-session-uuid')

            clz APICreateAccessKeyMsg.class

            desc """"""
            
			params {

				column {
					name "accountUuid"
					enclosedIn "params"
					desc "账户UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "userUuid"
					enclosedIn "params"
					desc "用户UUID"
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
			}
        }

        response {
            clz APICreateAccessKeyEvent.class
        }
    }
}