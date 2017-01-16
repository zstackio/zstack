package org.zstack.header.volume

import org.zstack.header.volume.APIGetVolumeCapabilitiesReply

doc {
    title "GetVolumeCapabilities"

    category "volume"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/volumes/{uuid}/capabilities"


            header (OAuth: 'the-session-uuid')

            clz APIGetVolumeCapabilitiesMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
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
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIGetVolumeCapabilitiesReply.class
        }
    }
}