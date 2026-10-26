package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIQueryHostMemoryStateReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryHostMemoryState"

    category "memoryOptimization"

    desc """查询物理机内存优化状态（标准查询，也支持ZQL query hostmemorystate；仅管理员）。status、featureState、reason等可作为查询条件；不含节省量，节省量请用GetMemorySummary按主机明细。状态在内部状态变化后数秒内更新，并每30秒按观测新鲜度刷新。"""

    rest {
        request {
			url "GET /v1/memory-optimization/host-states"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryHostMemoryStateMsg.class

            desc """查询物理机内存优化状态"""

			params APIQueryMessage.class
        }

        response {
            clz APIQueryHostMemoryStateReply.class
        }
    }
}
