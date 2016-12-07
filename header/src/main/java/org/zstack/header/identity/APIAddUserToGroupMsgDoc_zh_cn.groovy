package org.zstack.header.identity



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "POST /v1/accounts/groups/{groupUuid}/users"


            header (OAuth: 'the-session-uuid')

            clz APIAddUserToGroupMsg.class

            desc ""
            
			params {

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
					name "groupUuid"
					enclosedIn "params"
					desc ""
					location "url"
					type "String"
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
            clz APIAddUserToGroupEvent.class
        }
    }
}