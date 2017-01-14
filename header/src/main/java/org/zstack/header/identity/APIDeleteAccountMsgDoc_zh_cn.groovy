package org.zstack.header.identity

import org.zstack.header.identity.APIDeleteAccountEvent

doc {
    title "DeleteAccount"

    category "identity"

    desc "在这里填写API描述"

    rest {
        request {
			url "DELETE /v1/accounts/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIDeleteAccountMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "deleteMode"
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
            clz APIDeleteAccountEvent.class
        }
    }
}