package org.zstack.header.volume

org.zstack.header.volume.APIRecoverDataVolumeEvent

doc {
    title "RecoverDataVolume"

    category "volume"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/volumes/{uuid}/actions"


            header (OAuth: 'the-session-uuid')

            clz APIRecoverDataVolumeMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "recoverDataVolume"
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
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIRecoverDataVolumeEvent.class
        }
    }
}