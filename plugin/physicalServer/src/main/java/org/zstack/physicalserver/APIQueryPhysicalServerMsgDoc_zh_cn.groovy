package org.zstack.physicalserver

import org.zstack.physicalserver.APIQueryPhysicalServerReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "查询物理服务器(QueryPhysicalServer)"

    category "物理服务器"

    desc """查询由计算节点或管理节点自动识别并关联的物理服务器。"""

    rest {
        request {
			url "GET /v1/physical-servers"
			url "GET /v1/physical-servers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryPhysicalServerMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryPhysicalServerReply.class
        }
    }
}