package org.zstack.kvm.xmlhook

import org.zstack.kvm.xmlhook.APIExpungeVmUserDefinedXmlHookScriptEvent

doc {
    title "ExpungeVmUserDefinedXmlHookScript"

    category "host"

    desc """彻底删除用户自定义xml hook脚本"""

    rest {
        request {
			url "DELETE /v1/vm-instances/xml-hook-script"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIExpungeVmUserDefinedXmlHookScriptMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
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
            clz APIExpungeVmUserDefinedXmlHookScriptEvent.class
        }
    }
}