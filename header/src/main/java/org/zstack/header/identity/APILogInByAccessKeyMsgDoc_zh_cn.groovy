package org.zstack.header.identity

import org.zstack.header.identity.APILogInReply

doc {
    title "LogInByAccessKey"

    category "identity"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/accesskey/login"


            

            clz APILogInByAccessKeyMsg.class

            desc """"""
            
			params {

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