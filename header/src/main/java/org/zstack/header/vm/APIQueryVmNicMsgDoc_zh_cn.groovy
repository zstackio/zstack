package org.zstack.header.vm

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/vm-instances/nics"

			url "GET /v1/vm-instances/nics/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryVmNicMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryVmNicReply.class
        }
    }
}