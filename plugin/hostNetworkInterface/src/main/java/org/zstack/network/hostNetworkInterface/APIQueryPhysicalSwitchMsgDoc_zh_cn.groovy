package org.zstack.network.hostNetworkInterface

import org.zstack.network.hostNetworkInterface.APIQueryPhysicalSwitchReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryPhysicalSwitch"

    category "二层网络"

    desc """查询物理机交换机信息"""

    rest {
        request {
			url "GET /v1/topo/physical-switches"
			url "GET /v1/topo/physical-switches/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryPhysicalSwitchMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryPhysicalSwitchReply.class
        }
    }
}