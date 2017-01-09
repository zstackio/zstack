package org.zstack.header.identity

org.zstack.header.identity.APIGetAccountQuotaUsageReply

doc {
    title "GetAccountQuotaUsage"

    category "identity"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/accounts/quota/{uuid}/usages"


            header (OAuth: 'the-session-uuid')

            clz APIGetAccountQuotaUsageMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIGetAccountQuotaUsageReply.class
        }
    }
}