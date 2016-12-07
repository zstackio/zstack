package org.zstack.header.storage.primary



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/primary-storage/capacities"


            header (OAuth: 'the-session-uuid')

            clz APIGetPrimaryStorageCapacityMsg.class

            desc ""
            
			params {

				column {
					name "zoneUuids"
					enclosedIn "params"
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "clusterUuids"
					enclosedIn "params"
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "primaryStorageUuids"
					enclosedIn "params"
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "all"
					enclosedIn "params"
					desc ""
					location "query"
					type "boolean"
					optional true
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
            clz APIGetPrimaryStorageCapacityReply.class
        }
    }
}