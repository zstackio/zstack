package org.zstack.network.hostNetwork.lldp.api

import org.zstack.network.hostNetwork.lldp.api.APIQueryHostNetworkInterfaceLldpReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryHostNetworkInterfaceLldp"

    category "hostNetwork.lldp"

    desc """查询物理网口lldp信息"""

    rest {
        request {
			url "GET /v1/lldp/all"
			url "GET /v1/lldp/{interfaceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryHostNetworkInterfaceLldpMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryHostNetworkInterfaceLldpReply.class
        }
    }
}