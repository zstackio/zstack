package org.zstack.header.vm

import org.zstack.header.vm.APIDeleteVmStaticIpEvent

doc {
    title "DeleteVmStaticIp"

    category "vmInstance"

    desc """在这里填写API描述"""

    rest {
        request {
			url "DELETE /v1/vm-instances/{vmInstanceUuid}/static-ips"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDeleteVmStaticIpMsg.class

            desc """"""
            
			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "staticIp"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIDeleteVmStaticIpEvent.class
        }
    }
}