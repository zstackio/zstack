package org.zstack.storage.fusionstor.primary

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/primary-storage/fusionstor"

			url "GET /v1/primary-storage/fusionstor/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryFusionstorPrimaryStorageMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryPrimaryStorageReply.class
        }
    }
}