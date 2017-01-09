package org.zstack.storage.primary.local

org.zstack.storage.primary.local.APIGetLocalStorageHostDiskCapacityReply

doc {
    title "GetLocalStorageHostDiskCapacity"

    category "storage.primary"

    desc "在这里填写API描述"

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
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
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