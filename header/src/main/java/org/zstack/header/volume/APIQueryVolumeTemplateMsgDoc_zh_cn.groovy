package org.zstack.header.volume

import org.zstack.header.volume.APIQueryVolumeTemplateReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryVolumeTemplate"

    category "volume"

    desc """查询硬盘模板"""

    rest {
        request {
			url "GET /v1/volumes/volumeTemplate"
			url "GET /v1/volumes/volumeTemplate/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryVolumeTemplateMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryVolumeTemplateReply.class
        }
    }
}