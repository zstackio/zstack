package org.zstack.header.storage.snapshot



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/volume-snapshots/{uuid}/actions"


            header (OAuth: 'the-session-uuid')

            clz APIUpdateVolumeSnapshotMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateVolumeSnapshot"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn "updateVolumeSnapshot"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn "updateVolumeSnapshot"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
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
            clz APIUpdateVolumeSnapshotEvent.class
        }
    }
}