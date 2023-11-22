package org.zstack.network.hostNetwork.lldp.api

import org.zstack.network.hostNetwork.lldp.api.APIQueryHostNetworkInterfaceLldpRefReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryHostNetworkInterfaceLldpRef"

    category "hostNetwork.lldp"

    desc """查询物理网口lldp信息"""

    rest {
        request {
			url "GET /v1/lldp/info"
			url "GET /v1/lldp/info/{interfaceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryHostNetworkInterfaceLldpRefMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryHostNetworkInterfaceLldpRefReply.class
        }
    }
}