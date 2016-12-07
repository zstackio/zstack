package org.zstack.header.cluster

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/clusters"

			url "GET /v1/clusters/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryClusterMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryClusterReply.class
        }
    }
}