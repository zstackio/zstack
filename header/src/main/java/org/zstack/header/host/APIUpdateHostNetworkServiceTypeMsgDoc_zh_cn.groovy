package org.zstack.header.host

import org.zstack.header.host.APIUpdateHostNetworkServiceTypeEvent

doc {
    title "UpdateHostNetworkServiceType"

    category "host"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/hosts/service-types/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateHostNetworkServiceTypeMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "updateHostNetworkServiceType"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.2.1"
				}
				column {
					name "serviceType"
					enclosedIn "updateHostNetworkServiceType"
					desc ""
					location "body"
					type "String"
					optional false
					since "5.2.1"
				}
				column {
					name "system"
					enclosedIn "updateHostNetworkServiceType"
					desc ""
					location "body"
					type "boolean"
					optional true
					since "5.2.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.2.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.2.1"
				}
			}
        }

        response {
            clz APIUpdateHostNetworkServiceTypeEvent.class
        }
    }
}