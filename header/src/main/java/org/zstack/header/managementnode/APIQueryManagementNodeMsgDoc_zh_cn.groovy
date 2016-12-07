package org.zstack.header.managementnode

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/management-nodes"

			url "GET /v1/management-nodes/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryManagementNodeMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryManagementNodeReply.class
        }
    }
}