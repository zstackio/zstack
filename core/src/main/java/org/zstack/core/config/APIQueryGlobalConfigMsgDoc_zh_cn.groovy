package org.zstack.core.config

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/global-configurations"


            header (OAuth: 'the-session-uuid')

            clz APIQueryGlobalConfigMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryGlobalConfigReply.class
        }
    }
}