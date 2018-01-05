package org.zstack.storage.surfs.backup

import org.zstack.header.storage.backup.APIQueryBackupStorageReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QuerySurfsBackupStorage"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/backup-storage/surfs"

			url "GET /v1/backup-storage/surfs/{uuid}"


            header(Authorization: 'OAuth the-session-uuid')

            clz APIQuerySurfsBackupStorageMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryBackupStorageReply.class
        }
    }
}