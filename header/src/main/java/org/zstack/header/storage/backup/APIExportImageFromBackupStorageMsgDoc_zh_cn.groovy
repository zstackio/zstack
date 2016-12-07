package org.zstack.header.storage.backup



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/backup-storage/{backupStorageUuid}/actions"


            header (OAuth: 'the-session-uuid')

            clz APIExportImageFromBackupStorageMsg.class

            desc ""
            
			params {

				column {
					name "backupStorageUuid"
					enclosedIn "exportImageFromBackupStorage"
					desc "镜像存储UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "imageUuid"
					enclosedIn "exportImageFromBackupStorage"
					desc "镜像UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIExportImageFromBackupStorageEvent.class
        }
    }
}