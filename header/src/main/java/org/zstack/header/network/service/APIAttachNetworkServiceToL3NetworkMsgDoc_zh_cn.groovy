package org.zstack.header.network.service

import org.zstack.header.network.service.APIAttachNetworkServiceToL3NetworkEvent

doc {
    title "AttachNetworkServiceToL3Network"

    category "network.l3"

    desc "在这里填写API描述"

    rest {
        request {
			url "POST /v1/l3-networks/{l3NetworkUuid}/network-services"


            header (OAuth: 'the-session-uuid')

            clz APIAttachNetworkServiceToL3NetworkMsg.class

            desc ""
            
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
					name "networkServices"
					enclosedIn "params"
					desc ""
					location "body"
					type "Map"
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
            clz APIAttachNetworkServiceToL3NetworkEvent.class
        }
    }
}