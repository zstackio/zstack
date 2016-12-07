package org.zstack.header.allocator



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/hosts/allocators/strategies"


            header (OAuth: 'the-session-uuid')

            clz APIGetHostAllocatorStrategiesMsg.class

            desc ""
            
			params {

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
            clz APIGetHostAllocatorStrategiesReply.class
        }
    }
}