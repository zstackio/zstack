package org.zstack.header.storage.backup

org.zstack.header.storage.backup.APIQueryBackupStorageReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryBackupStorage"

    category "storage.backup"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/backup-storage"


            header (OAuth: 'the-session-uuid')

            clz APIQueryBackupStorageMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryBackupStorageReply.class
        }
    }
}