package org.zstack.header.vm

import org.zstack.header.vm.APICreateVmNicEvent

doc {
    title "CreateVmNic"

    category "vmInstance"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/nics"


            header(Authorization: 'OAuth the-session-uuid')

            clz APICreateVmNicMsg.class

            desc """"""
            
			params {

				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "ip"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
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
            clz APICreateVmNicEvent.class
        }
    }
}