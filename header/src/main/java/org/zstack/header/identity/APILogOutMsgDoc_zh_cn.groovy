package org.zstack.header.identity

org.zstack.header.identity.APILogOutReply

doc {
    title "LogOut"

    category "identity"

    desc "在这里填写API描述"

    rest {
        request {
			url "DELETE /v1/accounts/sessions/{sessionUuid}"


            

            clz APILogOutMsg.class

            desc ""
            
			params {

				column {
					name "sessionUuid"
					enclosedIn ""
					desc ""
					location "url"
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
            clz APILogOutReply.class
        }
    }
}