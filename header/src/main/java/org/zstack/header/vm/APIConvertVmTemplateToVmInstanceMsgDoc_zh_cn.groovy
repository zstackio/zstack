package org.zstack.header.vm

import org.zstack.header.vm.APIConvertVmTemplateToVmInstanceEvent

doc {
    title "虚拟机模板转换为虚拟机(ConvertVmTemplateToVmInstance)"

    category "vmInstance"

    desc """虚拟机模板转换为虚拟机"""

    rest {
        request {
			url "POST /v1/vm-instances/{vmTemplateUuid}/convert-to-vmInstance"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIConvertVmTemplateToVmInstanceMsg.class

            desc """"""
            
			params {

				column {
					name "vmTemplateUuid"
					enclosedIn "params"
					desc "虚拟机模板UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.0"
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
					name "strategy"
					enclosedIn "params"
					desc "模板转换为虚拟机的策略，转换后立即启动或仅转换"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
					values ("InstantStart","JustConvert")
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
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
            clz APIConvertVmTemplateToVmInstanceEvent.class
        }
    }
}