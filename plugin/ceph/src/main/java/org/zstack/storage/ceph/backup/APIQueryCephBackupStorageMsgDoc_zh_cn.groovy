package org.zstack.storage.ceph.backup

org.zstack.header.storage.backup.APIQueryBackupStorageReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryCephBackupStorage"

    category "未知类别"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/backup-storage/ceph"

			url "GET /v1/backup-storage/ceph/{uuid}"


            header (OAuth: 'the-session-uuid')

            clz APIQueryCephBackupStorageMsg.class

            desc ""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryBackupStorageReply.class
        }
    }
}