package org.zstack.header.identity



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/accounts/login"


            

            clz APILogInByAccountMsg.class

            desc ""
            
			params {

				column {
					name "accountName"
					enclosedIn "logInByAccount"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "password"
					enclosedIn "logInByAccount"
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