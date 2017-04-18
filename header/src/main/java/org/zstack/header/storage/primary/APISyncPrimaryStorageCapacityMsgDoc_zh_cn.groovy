package org.zstack.header.storage.primary

doc {
    title "刷新主存储容量(SyncPrimaryStorageCapacity)"

    category "storage.primary"

    desc "刷新主存储容量"

    rest {
        request {
			url "PUT /v1/primary-storage/{primaryStorageUuid}/actions"


            header (Authorization: 'OAuth the-session-uuid')

            clz APISyncPrimaryStorageCapacityMsg.class

            desc ""
            
			params {

				column {
					name "primaryStorageUuid"
					enclosedIn "syncPrimaryStorageCapacity"
					desc "主存储UUID"
					location "body"
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
            clz APISyncPrimaryStorageCapacityEvent.class
        }
    }
}