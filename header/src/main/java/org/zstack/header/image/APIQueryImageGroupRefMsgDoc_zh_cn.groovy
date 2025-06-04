package org.zstack.header.image

import org.zstack.header.image.APIQueryImageGroupRefReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryImageGroupRef"

    category "image"

    desc """查询镜像组关联关系"""

    rest {
        request {
			url "GET /v1/imagegrouprefs"
			url "GET /v1/imagegrouprefs/{imageGroupUuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryImageGroupRefMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryImageGroupRefReply.class
        }
    }
}