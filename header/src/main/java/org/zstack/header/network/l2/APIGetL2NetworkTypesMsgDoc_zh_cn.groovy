package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIGetL2NetworkTypesReply

doc {
    title "GetL2NetworkTypes"

    category "network.l2"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/l2-networks/types"


            header (OAuth: 'the-session-uuid')

            clz APIGetL2NetworkTypesMsg.class

            desc ""
            
			params {

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
            clz APIGetL2NetworkTypesReply.class
        }
    }
}