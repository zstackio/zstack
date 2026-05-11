package org.zstack.header.server

import org.zstack.header.server.APIScanPhysicalServersEvent

doc {
    title "ScanPhysicalServers"

    category "physicalServer"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/physical-servers/scan"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIScanPhysicalServersMsg.class

            desc """"""
            
			params {

				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "poolUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "ipRange"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "5.5.16"
				}
				column {
					name "oobPort"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "5.5.16"
				}
				column {
					name "credentials"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional false
					since "5.5.16"
				}
				column {
					name "concurrency"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "5.5.16"
				}
				column {
					name "timeoutPerHost"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "5.5.16"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.5.16"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.5.16"
				}
			}
        }

        response {
            clz APIScanPhysicalServersEvent.class
        }
    }
}