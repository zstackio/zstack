package org.zstack.header.network.l3



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/l3-networks/{l3NetworkUuid}/ip/free"

			url "GET /v1/l3-networks/ip-ranges/{ipRangeUuid}/ip/free"


            header (OAuth: 'the-session-uuid')

            clz APIGetFreeIpMsg.class

            desc ""
            
			params {

				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "query"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "ipRangeUuid"
					enclosedIn "params"
					desc "IP段UUID"
					location "query"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "start"
					enclosedIn "params"
					desc ""
					location "query"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "limit"
					enclosedIn "params"
					desc ""
					location "query"
					type "int"
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
            clz APIGetFreeIpReply.class
        }
    }
}