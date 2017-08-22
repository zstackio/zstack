package org.zstack.header.network.l3

import org.zstack.header.network.l3.APISetL3NetworkRouterInterfaceIpEvent

doc {
    title "SetL3NetworkRouterInterfaceIp"

    category "network.l3"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/l3-networks/{l3NetworkUuid}/router-interface-ip"


            header(Authorization: 'OAuth the-session-uuid')

            clz APISetL3NetworkRouterInterfaceIpMsg.class

            desc """"""
            
			params {

				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "routerInterfaceIp"
					enclosedIn "params"
					desc ""
					location "body"
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
            clz APISetL3NetworkRouterInterfaceIpEvent.class
        }
    }
}