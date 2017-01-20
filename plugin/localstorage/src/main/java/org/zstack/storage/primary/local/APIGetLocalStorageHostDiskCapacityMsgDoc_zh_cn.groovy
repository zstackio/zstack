package org.zstack.storage.primary.local

doc {
    title "获取主机本地存储容量(GetLocalStorageHostDiskCapacity)"

    category "storage.primary"

    desc "获取主机的本地存储的磁盘容量"

    rest {
        request {
			url "GET /v1/primary-storage/local-storage/capacities"


            header (OAuth: 'the-session-uuid')

            clz APIGetLocalStorageHostDiskCapacityMsg.class

            desc ""
            
			params {

				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "query"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "primaryStorageUuid"
					enclosedIn "params"
					desc "主存储UUID"
					location "query"
					type "String"
					optional false
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
            clz APIGetLocalStorageHostDiskCapacityReply.class
        }
    }
}