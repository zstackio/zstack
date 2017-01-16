package org.zstack.header.network.service

import org.zstack.header.network.service.APIGetNetworkServiceTypesReply

doc {
    title "GetNetworkServiceTypes"

    category "network.service"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/network-services/types"


            header (OAuth: 'the-session-uuid')

            clz APIGetNetworkServiceTypesMsg.class

            desc ""
            
			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIGetNetworkServiceTypesReply.class
        }
    }
}