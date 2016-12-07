package org.zstack.storage.fusionstor.backup

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/backup-storage/fusionstor"

			url "GET /v1/backup-storage/fusionstor/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryFusionstorBackupStorageMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryBackupStorageReply.class
        }
    }
}