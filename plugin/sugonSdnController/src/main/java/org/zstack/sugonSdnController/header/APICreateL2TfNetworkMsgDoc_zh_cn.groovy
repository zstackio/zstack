package org.zstack.sugonSdnController.header

import org.zstack.header.network.l2.APICreateL2NetworkEvent

doc {
    title "CreateL2TfNetworkMsg"

    category "network.l2"

    desc """创建Tf二层网络"""

    rest {
        request {
			url "POST /v1/l2-networks/tf"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateL2TfNetworkMsg.class

            desc """"""
            
			params {
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "4.3"
					
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.3"
					
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "4.3"
					
				}
				column {
					name "ipPrefix"
					enclosedIn "params"
					desc "ip prefix"
					location "body"
					type "String"
					optional false
					since "4.3"

				}
				column {
					name "ipPrefixLength"
					enclosedIn "params"
					desc "ip prefix length"
					location "body"
					type "Integer"
					optional false
					since "4.3"

				}
			}
        }

        response {
            clz APICreateL2NetworkEvent.class
        }
    }
}