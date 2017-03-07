package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIQueryL2VlanNetworkReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryL2VlanNetwork"

    category "network.l2.vlan"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/l2-networks/vlan"

			url "GET /v1/l2-networks/vlan/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryL2VlanNetworkMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryL2VlanNetworkReply.class
        }
    }
}