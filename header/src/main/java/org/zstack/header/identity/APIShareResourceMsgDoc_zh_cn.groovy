package org.zstack.header.identity

org.zstack.header.identity.APIShareResourceEvent

doc {
    title "ShareResource"

    category "identity"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/accounts/resources/actions"


            header (OAuth: 'the-session-uuid')

            clz APIShareResourceMsg.class

            desc ""
            
			params {

				column {
					name "resourceUuids"
					enclosedIn "shareResource"
					desc ""
					location "body"
					type "List"
					optional false
					since "0.6"
					
				}
				column {
					name "accountUuids"
					enclosedIn "shareResource"
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "toPublic"
					enclosedIn "shareResource"
					desc ""
					location "body"
					type "boolean"
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
            clz APIShareResourceEvent.class
        }
    }
}