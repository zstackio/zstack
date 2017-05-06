package org.zstack.network.service.eip

import org.zstack.network.service.eip.APIGetEipAttachableVmNicsReply

doc {
    title "获取可绑定指定弹性IP的云主机网卡(GetEipAttachableVmNics)"

    category "弹性IP"

    desc """获取可绑定指定弹性IP的云主机网卡"""

    rest {
        request {
			url "GET /v1/eips/{eipUuid}/vm-instances/candidate-nics"

			header (Authorization: 'OAuth the-session-uuid')


            clz APIGetEipAttachableVmNicsMsg.class

            desc """"""
            
			params {

				column {
					name "eipUuid"
					enclosedIn ""
					desc "弹性IP UUID"
					location "url"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "vipUuid"
					enclosedIn ""
					desc "VIP UUID"
					location "query"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIGetEipAttachableVmNicsReply.class
        }
    }
}