package org.zstack.header.vm



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/actions"


            header (OAuth: 'the-session-uuid')

            clz APISetVmStaticIpMsg.class

            desc ""
            
			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "setVmStaticIp"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "l3NetworkUuid"
					enclosedIn "setVmStaticIp"
					desc "三层网络UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "ip"
					enclosedIn "setVmStaticIp"
					desc ""
					location "body"
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
            clz APISetVmStaticIpEvent.class
        }
    }
}