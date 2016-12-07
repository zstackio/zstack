package org.zstack.header.storage.backup



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "DELETE /v1/zones/{zoneUuid}/backup-storage/{backupStorageUuid}"


            header (OAuth: 'the-session-uuid')

            clz APIDetachBackupStorageFromZoneMsg.class

            desc ""
            
			params {

				column {
					name "backupStorageUuid"
					enclosedIn ""
					desc "镜像存储UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "zoneUuid"
					enclosedIn ""
					desc "区域UUID"
					location "url"
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
            clz APIDetachBackupStorageFromZoneEvent.class
        }
    }
}