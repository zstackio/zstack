package org.zstack.header.vm



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "DELETE /v1/vm-instances/nics/{vmNicUuid}"


            header (OAuth: 'the-session-uuid')

            clz APIDetachL3NetworkFromVmMsg.class

            desc ""
            
			params {

				column {
					name "vmNicUuid"
					enclosedIn ""
					desc "云主机网卡UUID"
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
            clz APIDetachL3NetworkFromVmEvent.class
        }
    }
}