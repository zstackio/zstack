package org.zstack.header.vm

import org.zstack.header.vm.APIAttachL3NetworkToVmNicEvent

doc {
    title "AttachL3NetworkToVmNic"

    category "vmInstance"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/vm-instances/{vmNicUuid}/l3-networks/{l3NetworkUuid}"


            header(Authorization: 'OAuth the-session-uuid')

            clz APIAttachL3NetworkToVmNicMsg.class

            desc """"""
            
			params {

				column {
					name "vmNicUuid"
					enclosedIn "params"
					desc "云主机网卡UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "staticIp"
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
            clz APIAttachL3NetworkToVmNicEvent.class
        }
    }
}