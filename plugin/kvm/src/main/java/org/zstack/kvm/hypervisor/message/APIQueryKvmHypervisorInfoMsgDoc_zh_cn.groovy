package org.zstack.kvm.hypervisor.message

import org.zstack.kvm.hypervisor.message.APIQueryHostOsCategoryReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryKvmHypervisorInfo"

    category "host"

    desc """查询当前物理机 / VM 使用的监控软件的信息"""

    rest {
        request {
			url "GET /v1/hosts/kvm/hypervisor/info"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryKvmHypervisorInfoMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryHostOsCategoryReply.class
        }
    }
}