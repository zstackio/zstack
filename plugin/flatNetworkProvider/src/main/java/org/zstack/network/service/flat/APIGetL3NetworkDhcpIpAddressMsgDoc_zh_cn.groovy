package org.zstack.network.service.flat

import org.zstack.network.service.flat.APIGetL3NetworkDhcpIpAddressReply

doc {
    title "GetL3NetworkDhcpIpAddress"

    category "flat.dhcp"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/l3-networks/{l3NetworkUuid/dhcp-ip"


            header (OAuth: 'the-session-uuid')

            clz APIGetL3NetworkDhcpIpAddressMsg.class

            desc ""
            
			params {

				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "query"
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
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIGetL3NetworkDhcpIpAddressReply.class
        }
    }
}