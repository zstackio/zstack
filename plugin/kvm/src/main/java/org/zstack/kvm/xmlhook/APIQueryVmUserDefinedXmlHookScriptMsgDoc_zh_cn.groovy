package org.zstack.kvm.xmlhook

import org.zstack.kvm.xmlhook.APIQueryVmUserDefinedXmlHookScriptReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryVmUserDefinedXmlHookScript"

    category "未知类别"

    desc """查询用户自定义xml hook脚本"""

    rest {
        request {
			url "GET /v1/vm-instances/xml-hook-script"
			url "GET /v1/vm-instances/xml-hook-script/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryVmUserDefinedXmlHookScriptMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryVmUserDefinedXmlHookScriptReply.class
        }
    }
}