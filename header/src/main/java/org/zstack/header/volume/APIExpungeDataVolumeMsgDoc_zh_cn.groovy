package org.zstack.header.volume

import org.zstack.header.volume.APIExpungeDataVolumeEvent

doc {
    title "ExpungeDataVolume"

    category "volume"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/volumes/{uuid}/actions"


            header (OAuth: 'the-session-uuid')

            clz APIExpungeDataVolumeMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "expungeDataVolume"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIExpungeDataVolumeEvent.class
        }
    }
}