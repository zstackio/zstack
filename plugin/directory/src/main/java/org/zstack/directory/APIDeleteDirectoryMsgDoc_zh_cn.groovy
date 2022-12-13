package org.zstack.directory

import org.zstack.directory.APIDeleteDirectoryEvent

doc {
    title "DeleteDirectory"

    category "directory"

    desc """删除目录"""

    rest {
        request {
			url "DELETE /v1/delete/directory"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDeleteDirectoryMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "4.6.0"
					
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "body"
					type "String"
					optional true
					since "4.6.0"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.6.0"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.6.0"
					
				}
			}
        }

        response {
            clz APIDeleteDirectoryEvent.class
        }
    }
}