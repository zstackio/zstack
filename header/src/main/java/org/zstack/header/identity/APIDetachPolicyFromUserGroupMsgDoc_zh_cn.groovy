package org.zstack.header.identity



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "DELETE /v1/accounts/groups/{groupUuid}/policies/{policyUuid}"


            header (OAuth: 'the-session-uuid')

            clz APIDetachPolicyFromUserGroupMsg.class

            desc ""
            
			params {

				column {
					name "policyUuid"
					enclosedIn ""
					desc "权限策略UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "groupUuid"
					enclosedIn ""
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
            clz APIDetachPolicyFromUserGroupEvent.class
        }
    }
}