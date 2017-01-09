package org.zstack.header.vm

org.zstack.header.vm.APIQueryVmInstanceReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryVmInstance"

    category "vmInstance"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/vm-instances"

			url "GET /v1/vm-instances/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryVmInstanceMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryVmInstanceReply.class
        }
    }
}