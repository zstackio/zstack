package org.zstack.header.volume

doc {
    title "同步云盘大小(SyncVolumeSize)"

    category "volume"

    desc "同步云盘大小"

    rest {
        request {
			url "GET /v1/volumes/{uuid}/actions"


            header (OAuth: 'the-session-uuid')

            clz APISyncVolumeSizeMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "syncVolumeSize"
					desc "云盘的UUID，唯一标示该资源"
					location "url"
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
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APISyncVolumeSizeEvent.class
        }
    }
}