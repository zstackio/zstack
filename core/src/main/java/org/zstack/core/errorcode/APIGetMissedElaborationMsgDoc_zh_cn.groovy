package org.zstack.core.errorcode

import org.zstack.core.errorcode.APIGetMissedElaborationReply

doc {
    title "GetMissedElaboration"

    category "errorcode"

    desc """查看没有对应的错误码的错误列表"""

    rest {
        request {
			url "GET /v1/errorcode/elaborations/missed"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetMissedElaborationMsg.class

            desc """查看没有对应的错误码的错误列表"""
            
			params {

				column {
					name "repeats"
					enclosedIn ""
					desc "该错误在系统中最少出现的次数"
					location "query"
					type "Long"
					optional true
					since "3.3.0"
					
				}
				column {
					name "startTime"
					enclosedIn ""
					desc "查找起始时间"
					location "query"
					type "String"
					optional true
					since "3.3.0"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.3.0"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.3.0"
					
				}
			}
        }

        response {
            clz APIGetMissedElaborationReply.class
        }
    }
}