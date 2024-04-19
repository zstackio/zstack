package org.zstack.header.vm

import org.zstack.header.vm.APIQueryTemplateVmInstanceReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryTemplateVmInstance"

    category "vmInstance"

    desc """查询虚拟机模板"""

    rest {
        request {
			url "GET /v1/vm-instances/templateVmInstance"
			url "GET /v1/vm-instances/templateVmInstance/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryTemplateVmInstanceMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryTemplateVmInstanceReply.class
        }
    }
}