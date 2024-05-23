package org.zstack.header.network.service

import org.zstack.header.network.service.APIDeleteNetworkServiceFromL3NetworkEvent

doc {
    title "DeleteNetworkServiceFromL3Network"

    category "network.l3"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/l3-networks/{l3NetworkUuid}/network-services/delete"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDeleteNetworkServiceFromL3NetworkMsg.class

            desc """"""
            
			params {

				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "5.1.0"
				}
				column {
					name "networkServices"
					enclosedIn "params"
					desc ""
					location "body"
					type "Map"
					optional false
					since "5.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.1.0"
				}
			}
        }

        response {
            clz APIDeleteNetworkServiceFromL3NetworkEvent.class
        }
    }
}