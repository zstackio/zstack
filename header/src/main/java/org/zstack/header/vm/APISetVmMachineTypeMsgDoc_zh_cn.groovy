package org.zstack.header.vm

import org.zstack.header.vm.APISetVmMachineTypeEvent

doc {
    title "SetVmMachineType"

    category "vmInstance"

    desc """修改云主机主板类型"""

    rest {
        request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APISetVmMachineTypeMsg.class

            desc """修改云主机主板类型"""
            
			params {

				column {
					name "uuid"
					enclosedIn "setVmMachineType"
					desc "云主机 UUID"
					location "url"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "machineType"
					enclosedIn "setVmMachineType"
					desc "目标主板类型，仅支持 q35"
					location "body"
					type "String"
					optional false
					since "5.5.38"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.38"
				}
			}
        }

        response {
            clz APISetVmMachineTypeEvent.class
        }
    }
}