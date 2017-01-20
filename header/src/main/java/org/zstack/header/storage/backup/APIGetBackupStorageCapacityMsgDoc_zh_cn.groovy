package org.zstack.header.storage.backup

doc {
    title "获取镜像服务器存储容量(GetBackupStorageCapacity)"

    category "storage.backup"

    desc "获取镜像服务器存储容量"

    rest {
        request {
			url "GET /v1/backup-storage/capacities"


            header (OAuth: 'the-session-uuid')

            clz APIGetBackupStorageCapacityMsg.class

            desc ""
            
			params {

				column {
					name "zoneUuids"
					enclosedIn "params"
					desc "区域UUID列表"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "backupStorageUuids"
					enclosedIn "params"
					desc "镜像服务器UUID列表"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "all"
					enclosedIn "params"
					desc "当镜像服务器UUID列表为空时，该项为真表示查询系统中所有的镜像服务器。"
					location "query"
					type "boolean"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn "params"
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn "params"
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIGetBackupStorageCapacityReply.class
        }
    }
}