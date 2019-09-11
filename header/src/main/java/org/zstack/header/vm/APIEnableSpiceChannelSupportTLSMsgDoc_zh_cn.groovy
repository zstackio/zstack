package org.zstack.header.vm

import org.zstack.header.vm.APIEnableSpiceChannelSupportTLSEvent

doc {
    title "指定spice channel开启TLS加密(EnableSpiceChannelSupportTLS)"

    category "vmInstance"

    desc """开启TLS加密后，spice客户端需要下载CA证书才能访问云主机控制台"""

    rest {
        request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIEnableSpiceChannelSupportTLSMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "enableSpiceChannelSupportTLS"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "open"
					enclosedIn "enableSpiceChannelSupportTLS"
					desc "spice是否开启TLS加密"
					location "body"
					type "Boolean"
					optional false
					since "0.6"
					
				}
				column {
					name "channels"
					enclosedIn "enableSpiceChannelSupportTLS"
					desc "支持 [main, display, inputs, cursor, playback, record, smartcard, usbredir]"
					location "body"
					type "List"
					optional true
					since "0.6"
					
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
            clz APIEnableSpiceChannelSupportTLSEvent.class
        }
    }
}