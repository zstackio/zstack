package org.zstack.storage.backup.sftp

import org.zstack.header.query.APIQueryMessage

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/backup-storage/sftp"

			url "GET /v1/backup-storage/sftp/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQuerySftpBackupStorageMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQuerySftpBackupStorageReply.class
        }
    }
}