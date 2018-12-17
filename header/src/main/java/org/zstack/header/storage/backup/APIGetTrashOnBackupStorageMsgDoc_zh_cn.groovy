package org.zstack.header.storage.backup

import org.zstack.header.storage.backup.APIGetTrashOnBackupStorageReply

doc {
    title "GetTrashOnBackupStorage"

    category "storage.backup"

    desc """获取备份存储上的回收数据列表"""

    rest {
        request {
			url "GET /v1/backup-storage/trash"


            header(Authorization: 'OAuth the-session-uuid')

            clz APIGetTrashOnBackupStorageMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "备份存储UUID"
					location "query"
					type "String"
					optional false
					since "3.2.0"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.2.0"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.2.0"
					
				}
			}
        }

        response {
            clz APIGetTrashOnBackupStorageReply.class
        }
    }
}