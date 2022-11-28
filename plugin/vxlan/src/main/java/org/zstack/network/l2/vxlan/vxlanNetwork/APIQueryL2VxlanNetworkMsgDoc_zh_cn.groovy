package org.zstack.network.l2.vxlan.vxlanNetwork

import org.zstack.network.l2.vxlan.vxlanNetwork.APIQueryL2VxlanNetworkReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryL2VxlanNetwork"

    category "network.l2"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/l2-networks/vxlan"
			url "GET /v1/l2-networks/vxlan/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryL2VxlanNetworkMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryL2VxlanNetworkReply.class
        }
    }
}