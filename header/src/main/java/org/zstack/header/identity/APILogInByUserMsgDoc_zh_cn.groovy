package org.zstack.header.identity

org.zstack.header.identity.APILogInReply

doc {
    title "LogInByUser"

    category "identity"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/accounts/users/login"


            

            clz APILogInByUserMsg.class

            desc ""
            
			params {

				column {
					name "accountUuid"
					enclosedIn "logInByUser"
					desc "账户UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "accountName"
					enclosedIn "logInByUser"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "userName"
					enclosedIn "logInByUser"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "password"
					enclosedIn "logInByUser"
					desc ""
					location "body"
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
            clz APILogInReply.class
        }
    }
}