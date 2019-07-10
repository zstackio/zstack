package org.zstack.storage.surfs.primary

import org.zstack.header.storage.primary.APIQueryPrimaryStorageReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QuerySurfsPrimaryStorage"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/primary-storage/surfs"

			url "GET /v1/primary-storage/surfs/{uuid}"


            header(Authorization: 'OAuth the-session-uuid')

            clz APIQuerySurfsPrimaryStorageMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryPrimaryStorageReply.class
        }
    }
}