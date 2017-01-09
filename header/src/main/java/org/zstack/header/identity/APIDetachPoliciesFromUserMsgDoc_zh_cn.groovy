package org.zstack.header.identity

org.zstack.header.identity.APIDetachPoliciesFromUserEvent

doc {
    title "DetachPoliciesFromUser"

    category "identity"

    desc "在这里填写API描述"

    rest {
        request {
			url "DELETE /v1/accounts/users/{userUuid}/policies"


            header (OAuth: 'the-session-uuid')

            clz APIDetachPoliciesFromUserMsg.class

            desc ""
            
			params {

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
					name "userUuid"
					enclosedIn "params"
					desc "用户UUID"
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
            clz APIDetachPoliciesFromUserEvent.class
        }
    }
}