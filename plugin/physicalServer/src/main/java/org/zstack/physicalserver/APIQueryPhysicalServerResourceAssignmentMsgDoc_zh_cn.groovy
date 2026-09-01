package org.zstack.physicalserver

import org.zstack.physicalserver.APIQueryPhysicalServerResourceAssignmentReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "查询物理服务器资源分配(QueryPhysicalServerResourceAssignment)"

    category "物理服务器"

    desc """查询物理服务器各Role的资源分配。返回CPUSet、总体内存上限及Synced/Unsynced同步状态；可写Role由Cloud应用约束，只读Role记录第三方已生效的约束。"""

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
