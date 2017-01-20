package org.zstack.header.storage.backup

doc {
    title "从区域中卸载已经挂载的镜像服务器(DetachBackupStorageFromZone)"

    category "storage.backup"

    desc "从区域中卸载已经挂载的镜像服务器"

    rest {
        request {
			url "DELETE /v1/zones/{zoneUuid}/backup-storage/{backupStorageUuid}"


            header (OAuth: 'the-session-uuid')

            clz APIDetachBackupStorageFromZoneMsg.class

            desc ""
            
			params {

				column {
					name "backupStorageUuid"
					enclosedIn "params"
					desc "镜像存储UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn "params"
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn "params"
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIDetachBackupStorageFromZoneEvent.class
        }
    }
}