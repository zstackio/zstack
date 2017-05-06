package org.zstack.header.vm

import org.zstack.header.vm.APIDestroyVmInstanceEvent

doc {
    title "删除云主机(DestroyVmInstance)"

    category "vmInstance"

    desc """删除一个云主机"""

    rest {
        request {
			url "DELETE /v1/vm-instances/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIDestroyVmInstanceMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
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
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIDestroyVmInstanceEvent.class
        }
    }
}