package org.zstack.header.volume

import org.zstack.header.volume.APISyncVolumeSizeEvent

doc {
    title "SyncVolumeSize"

    category "volume"

    desc "在这里填写API描述"

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
					desc "资源的UUID，唯一标示该资源"
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
					desc ""
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