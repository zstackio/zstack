package org.zstack.header.storage.snapshot

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/volume-snapshots/trees"

			url "GET /v1/volume-snapshots/trees/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryVolumeSnapshotTreeMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryVolumeSnapshotTreeReply.class
        }
    }
}