package org.zstack.header.vm

import org.zstack.header.vm.APIQueryVmTemplateReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryVmTemplate"

    category "vmInstance"

    desc """查询虚拟机模板"""

    rest {
        request {
			url "GET /v1/vm-instances/vmTemplate"
			url "GET /v1/vm-instances/vmTemplate/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryVmTemplateMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryVmTemplateReply.class
        }
    }
}