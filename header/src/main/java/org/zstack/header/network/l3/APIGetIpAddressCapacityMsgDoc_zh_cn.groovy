package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIGetIpAddressCapacityReply

doc {
    title "获取IP网络地址容量(GetIpAddressCapacity)"

    category "三层网络"

    desc "获取IP网络地址容量"

    rest {
        request {
			url "GET /v1/ip-capacity"


            header (OAuth: 'the-session-uuid')

            clz APIGetIpAddressCapacityMsg.class

            desc ""
            
			params {

				column {
					name "zoneUuids"
					enclosedIn "params"
					desc "区域UUID"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "l3NetworkUuids"
					enclosedIn "params"
					desc "三层网络UUID"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "ipRangeUuids"
					enclosedIn "params"
					desc "IP地址范围UUID"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "all"
					enclosedIn "params"
					desc "系统全局"
					location "query"
					type "boolean"
					optional true
					since "0.6"
					
				}
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
            clz APIGetIpAddressCapacityReply.class
        }
    }
}