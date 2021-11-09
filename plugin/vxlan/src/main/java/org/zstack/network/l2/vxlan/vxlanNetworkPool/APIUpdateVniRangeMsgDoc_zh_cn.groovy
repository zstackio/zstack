package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIUpdateVniRangeEvent

doc {
    title "UpdateVniRange"

    category "network.l2"

    desc """修改 Vni Range"""

    rest {
        request {
			url "PUT /v1/l2-networks/vxlan-pool/vni-ranges/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateVniRangeMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateVniRange"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.3.0"
					
				}
				column {
					name "name"
					enclosedIn "updateVniRange"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.3.0"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.3.0"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.3.0"
					
				}
			}
        }

        response {
            clz APIUpdateVniRangeEvent.class
        }
    }
}