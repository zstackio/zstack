package org.zstack.storage.fusionstor.primary

import org.zstack.header.storage.primary.APIQueryPrimaryStorageReply
import org.zstack.header.query.APIQueryMessage
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryFusionstorPrimaryStorage"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/primary-storage/fusionstor"
			url "GET /v1/primary-storage/fusionstor/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIQueryFusionstorPrimaryStorageMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryPrimaryStorageReply.class
        }
    }
}