package org.zstack.storage.backup.sftp

org.zstack.storage.backup.sftp.APIQuerySftpBackupStorageReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QuerySftpBackupStorage"

    category "storage.backup.sftp"

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