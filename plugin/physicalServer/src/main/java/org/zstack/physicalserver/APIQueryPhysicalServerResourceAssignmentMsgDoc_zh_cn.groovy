package org.zstack.physicalserver

import org.zstack.physicalserver.APIQueryPhysicalServerResourceAssignmentReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "查询物理服务器资源分配(QueryPhysicalServerResourceAssignment)"

    category "物理服务器"

    desc """查询物理服务器上COMPUTE和MANAGEMENT Role的资源分配。返回CPUSet、总体内存上限及Synced/Unsynced同步状态；ZBS第一期只做实时观测，不创建资源分配。"""

    rest {
        request {
			url "GET /v1/physical-servers/resource-assignments"
			url "GET /v1/physical-servers/resource-assignments/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryPhysicalServerResourceAssignmentMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryPhysicalServerResourceAssignmentReply.class
        }
    }
}