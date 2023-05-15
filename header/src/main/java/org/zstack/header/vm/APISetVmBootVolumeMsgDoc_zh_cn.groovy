package org.zstack.header.vm

import org.zstack.header.vm.APISetVmBootVolumeEvent

doc {
    title "SetVmBootVolume"

    category "vmInstance"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APISetVmBootVolumeMsg.class

            desc """"""
            
			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "setVmBootVolume"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "volumeUuid"
					enclosedIn "setVmBootVolume"
					desc "云盘UUID"
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
            clz APISetVmBootVolumeEvent.class
        }
    }
}