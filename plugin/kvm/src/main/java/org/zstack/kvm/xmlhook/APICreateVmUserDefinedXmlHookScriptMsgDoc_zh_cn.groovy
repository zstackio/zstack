package org.zstack.kvm.xmlhook

import org.zstack.kvm.xmlhook.APICreateVmUserDefinedXmlHookScriptEvent

doc {
    title "APCreateVmUserDefinedXmlHookScript"

    category "未知类别"

    desc """创建用户自定义xml hook"""

    rest {
        request {
			url "POST /v1/vm-instances/xml-hook-script"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateVmUserDefinedXmlHookScriptMsg.class

            desc """"""
            
			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "4.7.21"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.7.21"
				}
				column {
					name "hookScript"
					enclosedIn "params"
					desc "xml hook脚本内容"
					location "body"
					type "String"
					optional false
					since "4.7.21"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "4.7.21"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "4.7.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.7.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.7.21"
				}
			}
        }

        response {
            clz APICreateVmUserDefinedXmlHookScriptEvent.class
        }
    }
}