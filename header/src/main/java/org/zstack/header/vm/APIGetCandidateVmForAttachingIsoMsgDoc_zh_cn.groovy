package org.zstack.header.vm



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/images/iso/{isoUuid}/vm-candidates"


            header (OAuth: 'the-session-uuid')

            clz APIGetCandidateVmForAttachingIsoMsg.class

            desc ""
            
			params {

				column {
					name "isoUuid"
					enclosedIn ""
					desc ""
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
            clz APIGetCandidateVmForAttachingIsoReply.class
        }
    }
}