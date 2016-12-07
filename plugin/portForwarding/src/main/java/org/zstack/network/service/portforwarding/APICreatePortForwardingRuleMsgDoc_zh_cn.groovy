package org.zstack.network.service.portforwarding



doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
			url "POST /v1/port-forwarding"


            header (OAuth: 'the-session-uuid')

            clz APICreatePortForwardingRuleMsg.class

            desc ""
            
			params {

				column {
					name "vipUuid"
					enclosedIn ""
					desc "VIP UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "vipPortStart"
					enclosedIn ""
					desc ""
					location "body"
					type "Integer"
					optional false
					since "0.6"
					
				}
				column {
					name "vipPortEnd"
					enclosedIn ""
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
					
				}
				column {
					name "privatePortStart"
					enclosedIn ""
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
					
				}
				column {
					name "privatePortEnd"
					enclosedIn ""
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
					
				}
				column {
					name "protocolType"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("TCP","UDP")
				}
				column {
					name "vmNicUuid"
					enclosedIn ""
					desc "云主机网卡UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "allowedCidr"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn ""
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn ""
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "resourceUuid"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
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
            clz APICreatePortForwardingRuleEvent.class
        }
    }
}