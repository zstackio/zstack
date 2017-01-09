package org.zstack.header.network.l3

org.zstack.header.network.l3.APIAddDnsToL3NetworkEvent

doc {
    title "AddDnsToL3Network"

    category "network.l3"

    desc "在这里填写API描述"

    rest {
        request {
			url "POST /v1/l3-networks/{l3NetworkUuid}/dns"


            header (OAuth: 'the-session-uuid')

            clz APIAddDnsToL3NetworkMsg.class

            desc ""
            
			params {

				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "dns"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
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
            clz APIAddDnsToL3NetworkEvent.class
        }
    }
}