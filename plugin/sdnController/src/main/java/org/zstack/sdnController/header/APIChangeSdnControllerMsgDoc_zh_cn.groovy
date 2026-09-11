package org.zstack.sdnController.header

import org.zstack.sdnController.header.APIChangeSdnControllerEvent

doc {
    title "ChangeSdnController"

    category "SdnController"

    desc """Changes SDN controller"""

    rest {
        request {
			url "PUT /v1/sdn-controllers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIChangeSdnControllerMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "changeSdnController"
					desc "UUID of the SDN controller resource"
					location "url"
					type "String"
					optional false
					since "5.3.28"
				}
				column {
					name "userName"
					enclosedIn "changeSdnController"
					desc "New user name for the SDN controller"
					location "body"
					type "String"
					optional true
					since "5.3.28"
				}
				column {
					name "password"
					enclosedIn "changeSdnController"
					desc "New password for the SDN controller"
					location "body"
					type "String"
					optional true
					since "5.3.28"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "System tags"
					location "body"
					type "List"
					optional true
					since "5.3.28"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "User tags"
					location "body"
					type "List"
					optional true
					since "5.3.28"
				}
				column {
					name "vlanRanges"
					enclosedIn "changeSdnController"
					desc ""
					location "body"
					type "List"
					optional true
					since "5.3.28"
				}
				column {
					name "ip"
					enclosedIn "changeSdnController"
					desc ""
					location "body"
					type "String"
					optional true
					since "5.5.28"
				}
			}
        }

        response {
            clz APIChangeSdnControllerEvent.class
        }
    }
}