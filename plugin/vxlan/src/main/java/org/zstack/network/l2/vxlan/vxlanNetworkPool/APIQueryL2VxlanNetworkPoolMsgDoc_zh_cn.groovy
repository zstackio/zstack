package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryL2VxlanNetworkPoolReply
import org.zstack.header.query.APIQueryMessage
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryL2VxlanNetworkPool"

    category "network.l2"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/l2-networks/vxlan-pool"
			url "GET /v1/l2-networks/vxlan-pool/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIQueryL2VxlanNetworkPoolMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryL2VxlanNetworkPoolReply.class
        }
    }
}