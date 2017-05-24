package org.zstack.header.identity

import org.zstack.header.identity.APIGetResourceAccountReply

doc {
    title "GetResourceAccount"

    category "identity"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/resources/accounts"


            header(Authorization: 'OAuth the-session-uuid')

            clz APIGetResourceAccountMsg.class

            desc """"""
            
			params {

				column {
					name "resourceUuids"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional false
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
            clz APIGetResourceAccountReply.class
        }
    }
}