package org.zstack.header.vm

import org.zstack.header.vm.APIConvertVmInstanceToVmTemplateEvent

doc {
    title "虚拟机转换为虚拟机模板(ConvertVmInstanceToVmTemplate)"

    category "vmInstance"

    desc """虚拟机转换为虚拟机模板"""

    rest {
        request {
			url "POST /v1/vm-instances/{vmInstanceUuid}/convert-to-vmTemplate"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIConvertVmInstanceToVmTemplateMsg.class

            desc """"""
            
			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
			}
        }

        response {
            clz APIConvertVmInstanceToVmTemplateEvent.class
        }
    }
}