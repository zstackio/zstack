package org.zstack.storage.fusionstor.backup

import org.zstack.header.storage.backup.APIQueryBackupStorageReply
import org.zstack.header.query.APIQueryMessage
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryFusionstorBackupStorage"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/backup-storage/fusionstor"
			url "GET /v1/backup-storage/fusionstor/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIQueryFusionstorBackupStorageMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryBackupStorageReply.class
        }
    }
}