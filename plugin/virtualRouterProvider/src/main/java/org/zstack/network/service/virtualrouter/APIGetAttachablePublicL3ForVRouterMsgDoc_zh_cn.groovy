package org.zstack.network.service.virtualrouter

import org.zstack.network.service.virtualrouter.APIGetAttachablePublicL3ForVRouterReply

doc {
    title "GetAttachablePublicL3ForVRouter"

    category "virtualRouter"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/vm-instances/appliances/virtual-routers/{vmInstanceUuid}/attachable-public-l3s"


            header(Authorization: 'OAuth the-session-uuid')

            clz APIGetAttachablePublicL3ForVRouterMsg.class

            desc """"""
            
			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
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
            clz APIGetAttachablePublicL3ForVRouterReply.class
        }
    }
}