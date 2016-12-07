package org.zstack.header.volume

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/volumes"

			url "GET /v1/volumes/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryVolumeMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryVolumeReply.class
        }
    }
}