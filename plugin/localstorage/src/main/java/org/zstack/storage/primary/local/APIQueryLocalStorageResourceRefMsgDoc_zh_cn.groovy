package org.zstack.storage.primary.local

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/primary-storage/local-storage/resource-refs"


            header (OAuth: 'the-session-uuid')

            clz APIQueryLocalStorageResourceRefMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryLocalStorageResourceRefReply.class
        }
    }
}