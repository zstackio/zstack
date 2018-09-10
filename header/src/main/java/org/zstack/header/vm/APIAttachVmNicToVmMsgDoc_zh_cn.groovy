package org.zstack.header.vm

import org.zstack.header.vm.APIAttachVmNicToVmEvent

doc {
    title "AttachVmNicToVm"

    category "vmInstance"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/vm-instances/{vmInstanceUuid}/nices/{vmNicUuid}"


            header(Authorization: 'OAuth the-session-uuid')

            clz APIAttachVmNicToVmMsg.class

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
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "云主机UUID"
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
            clz APIAttachVmNicToVmEvent.class
        }
    }
}