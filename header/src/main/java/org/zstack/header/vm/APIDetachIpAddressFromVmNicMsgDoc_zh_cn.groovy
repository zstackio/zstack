package org.zstack.header.vm

doc {
    title "DetachIpAddressFromVmNic"

    category "vmInstance"

    desc """在这里填写API描述"""

    rest {
        request {
			url "DELETE /v1/nics/{vmNicUuid}/usedIps/{usedIpUuid}"


            header(Authorization: 'OAuth the-session-uuid')

            clz APIDetachIpAddressFromVmNicMsg.class

            desc """"""
            
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
					name "usedIpUuid"
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
            clz APIDetachIpAddressFromVmNicEvent.class
        }
    }
}