package org.zstack.header.vm

import org.zstack.header.vm.APIDeleteVmStaticIpEvent

doc {
    title "删除云主机指定IP(DeleteVmStaticIp)"

    category "vmInstance"

    desc """删除云主机三层网络上指定的IP"""

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