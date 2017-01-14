package org.zstack.header.volume

import org.zstack.header.volume.APIGetVolumeFormatReply

doc {
    title "GetVolumeFormat"

    category "volume"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/volumes/formats"


            header (OAuth: 'the-session-uuid')

            clz APIGetVolumeFormatMsg.class

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
            clz APIGetVolumeFormatReply.class
        }
    }
}