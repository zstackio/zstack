package org.zstack.header.host

import org.zstack.header.host.APICreateHostNetworkServiceTypeEvent

doc {
    title "CreateHostNetworkServiceType"

    category "host"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/hosts/service-types"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateHostNetworkServiceTypeMsg.class

            desc """"""
            
			params {

				column {
					name "serviceType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "5.2.1"
				}
				column {
					name "system"
					enclosedIn "params"
					desc ""
					location "body"
					type "boolean"
					optional true
					since "5.2.1"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "5.2.1"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
            clz APICreateHostNetworkServiceTypeEvent.class
        }
    }
}