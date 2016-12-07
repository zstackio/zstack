package org.zstack.header.identity



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "POST /v1/accounts/users/{userUuid}/policy-collection"


            header (OAuth: 'the-session-uuid')

            clz APIAttachPoliciesToUserMsg.class

            desc ""
            
			params {

				column {
					name "userUuid"
					enclosedIn "params"
					desc "用户UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "policyUuids"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
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
            clz APIAttachPoliciesToUserEvent.class
        }
    }
}