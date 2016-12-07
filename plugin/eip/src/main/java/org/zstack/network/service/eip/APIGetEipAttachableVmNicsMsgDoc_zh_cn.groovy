package org.zstack.network.service.eip



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "GET /v1/eips/vm-instances/candidate-nics"


            header (OAuth: 'the-session-uuid')

            clz APIGetEipAttachableVmNicsMsg.class

            desc ""
            
			params {

				column {
					name "eipUuid"
					enclosedIn "params"
					desc "弹性IP UUID"
					location "query"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "vipUuid"
					enclosedIn "params"
					desc "VIP UUID"
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
            clz APIGetEipAttachableVmNicsReply.class
        }
    }
}