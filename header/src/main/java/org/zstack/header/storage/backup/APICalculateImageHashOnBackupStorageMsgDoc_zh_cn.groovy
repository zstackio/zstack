package org.zstack.header.storage.backup

import org.zstack.header.storage.backup.APICalculateImageHashOnBackupStorageEvent

doc {
    title "CalculateImageHashOnBackupStorage"

    category "storage.backup"

    desc """计算镜像的哈希值"""

    rest {
        request {
			url "PUT /v1/backup-storage/{backupStorageUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICalculateImageHashOnBackupStorageMsg.class

            desc """"""
            
			params {

				column {
					name "backupStorageUuid"
					enclosedIn "calculateImageHashOnBackupStorage"
					desc "镜像存储UUID"
					location "url"
					type "String"
					optional false
					since "4.8.0"
				}
				column {
					name "imageUuid"
					enclosedIn "calculateImageHashOnBackupStorage"
					desc "镜像UUID"
					location "body"
					type "String"
					optional false
					since "4.8.0"
				}
				column {
					name "algorithm"
					enclosedIn "calculateImageHashOnBackupStorage"
					desc ""
					location "body"
					type "String"
					optional true
					since "4.8.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.8.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.8.0"
				}
			}
        }

        response {
            clz APICalculateImageHashOnBackupStorageEvent.class
        }
    }
}