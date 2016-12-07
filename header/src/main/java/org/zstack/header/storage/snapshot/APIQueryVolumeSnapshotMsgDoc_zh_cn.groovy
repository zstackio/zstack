package org.zstack.header.storage.snapshot

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/volume-snapshots"

			url "GET /v1/volume-snapshots/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryVolumeSnapshotMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryVolumeSnapshotReply.class
        }
    }
}