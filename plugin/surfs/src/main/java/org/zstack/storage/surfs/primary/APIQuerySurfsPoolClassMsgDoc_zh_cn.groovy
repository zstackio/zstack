package org.zstack.storage.surfs.primary

import org.zstack.storage.surfs.primary.APIQuerySurfsPoolClassReplay
import org.zstack.header.query.APIQueryMessage

doc {
    title "QuerySurfsPoolClass"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/primary-storage/surfs/poolclss"

			url "GET /v1/primary-storage/surfs/poolclss/{uuid}"


            header(Authorization: 'OAuth the-session-uuid')

            clz APIQuerySurfsPoolClassMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQuerySurfsPoolClassReplay.class
        }
    }
}