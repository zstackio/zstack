package org.zstack.header.zone

doc {
    title "在这里填写API标题"

    desc "在这里填写API描述"

    rest {
        request {
            url ("PUT /zones/{uuid}/actions", "创建一个新的zone")
            url ("POST /zones", "例子")

            header (Authorization: 'OAuth the-session-uuid', "这里session-uuid是通过Login API得到的，例如bfa67f956afb430890aa49db14b85153")
			header ('OAuth the-session-uuid')

            clz APIChangeZoneStateMsg.class

            desc ""

            params "APIQueryMessageDoc_.groovy"

            /*
			params {
				column {
					name "uuid"
					desc "zone UUID"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "stateEvent"
					desc "状态事件"
					type "String"
					optional false
					since "0.6"
					values ("enable","disable")
				}
				column {
					name "systemTags"
					desc ""
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					desc ""
					type "List"
					optional true
					since "0.6"
					
				}
			}
			*/
        }

        response {
            clz APIChangeZoneStateEvent.class
        }
    }
}