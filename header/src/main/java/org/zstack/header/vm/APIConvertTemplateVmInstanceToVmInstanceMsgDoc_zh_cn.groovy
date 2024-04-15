package org.zstack.header.vm

import org.zstack.header.vm.APIConvertTemplateVmInstanceToVmInstanceEvent

doc {
    title "虚拟机模板转换为虚拟机(ConvertTemplateVmInstanceToVmInstance)"

    category "vmInstance"

    desc """虚拟机模板转换为虚拟机"""

    rest {
        request {
			url "POST /v1/vm-instances/{templateVmInstanceUuid}/convert-to-vmInstance"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIConvertTemplateVmInstanceToVmInstanceMsg.class

            desc """"""
            
			params {

				column {
					name "TemplateVmInstanceUuid"
					enclosedIn "params"
					desc "虚拟机模板UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "虚拟机名称"
					location "body"
					type "String"
					optional false
					since "zsv 4.2.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.6"
				}
			}
        }

        response {
            clz APIConvertTemplateVmInstanceToVmInstanceEvent.class
        }
    }
}