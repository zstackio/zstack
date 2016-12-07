package org.zstack.header.vm



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/images-l3networks/dependencies"


            header (OAuth: 'the-session-uuid')

            clz APIGetInterdependentL3NetworksImagesMsg.class

            desc ""
            
			params {

				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "query"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "l3NetworkUuids"
					enclosedIn "params"
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "imageUuid"
					enclosedIn "params"
					desc "镜像UUID"
					location "query"
					type "String"
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
            clz APIGetInterdependentL3NetworkImageReply.class
        }
    }
}