package org.zstack.header.vm

import org.zstack.header.vm.APISetVmSoundTypeEvent

doc {
    title "指定云主机虚拟声卡类型(SetVmSoundType)"

    category "vmInstance"

    desc """指定一个云主机的虚拟声卡类型"""

    rest {
        request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APISetVmSoundTypeMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "setVmSoundType"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "soundType"
					enclosedIn "setVmSoundType"
					desc "虚拟声卡类型，默认为ich6"
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("ac97","ich6")
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
            clz APISetVmSoundTypeEvent.class
        }
    }
}