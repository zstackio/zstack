package org.zstack.header.storage.primary

doc {
    title "获取主存储分配策略清单(GetPrimaryStorageAllocatorStrategies)"

    category "storage.primary"

    desc "获取主存储分配策略清单"

    rest {
        request {
			url "GET /v1/primary-storage/allocators/strategies"


            header (Authorization: 'OAuth the-session-uuid')

            clz APIGetPrimaryStorageAllocatorStrategiesMsg.class

            desc ""
            
			params {

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
            clz APIGetPrimaryStorageAllocatorStrategiesReply.class
        }
    }
}