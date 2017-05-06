package org.zstack.header.vm

import org.zstack.header.vm.APIMigrateVmEvent

doc {
    title "热迁移云主机(MigrateVm)"

    category "vmInstance"

    desc """将云主机热迁移至另一个物理机"""

    rest {
        request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIMigrateVmMsg.class

            desc """"""
            
			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "migrateVm"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "hostUuid"
					enclosedIn "migrateVm"
					desc "物理机UUID"
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
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIMigrateVmEvent.class
        }
    }
}