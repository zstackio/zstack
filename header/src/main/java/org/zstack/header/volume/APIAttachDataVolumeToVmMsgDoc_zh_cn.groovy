package org.zstack.header.volume

org.zstack.header.volume.APIAttachDataVolumeToVmEvent

doc {
    title "AttachDataVolumeToVm"

    category "volume"

    desc "在这里填写API描述"

    rest {
        request {
			url "POST /v1/volumes/{volumeUuid}/vm-instances/{vmInstanceUuid}"


            header (OAuth: 'the-session-uuid')

            clz APIAttachDataVolumeToVmMsg.class

            desc ""
            
			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "volumeUuid"
					enclosedIn ""
					desc "云盘UUID"
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
            clz APIAttachDataVolumeToVmEvent.class
        }
    }
}