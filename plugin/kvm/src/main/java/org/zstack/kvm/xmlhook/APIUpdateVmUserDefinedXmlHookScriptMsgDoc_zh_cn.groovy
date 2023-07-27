package org.zstack.kvm.xmlhook

import org.zstack.kvm.xmlhook.APIUpdateVmUserDefinedXmlHookScriptEvent

doc {
    title "UpdateVmUserDefinedXmlHookScript"

    category "host"

    desc """更新用户自定义xml hook"""

    rest {
        request {
			url "PUT /v1/vm-instances/xml-hook-script"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateVmUserDefinedXmlHookScriptMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateVmUserDefinedXmlHookScript"
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "4.7.21"
				}
				column {
					name "name"
					enclosedIn "updateVmUserDefinedXmlHookScript"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "4.7.21"
				}
				column {
					name "description"
					enclosedIn "updateVmUserDefinedXmlHookScript"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.7.21"
				}
				column {
					name "hookScript"
					enclosedIn "updateVmUserDefinedXmlHookScript"
					desc "xml hook 脚本"
					location "body"
					type "String"
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
            clz APIUpdateVmUserDefinedXmlHookScriptEvent.class
        }
    }
}