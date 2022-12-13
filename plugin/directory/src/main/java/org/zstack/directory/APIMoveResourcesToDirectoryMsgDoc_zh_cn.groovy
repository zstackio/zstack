package org.zstack.directory

import org.zstack.directory.APIMoveResourcesToDirectoryEvent

doc {
    title "MoveResourcesToDirectory"

    category "directory"

    desc """移动资源到指定目录"""

    rest {
        request {
			url "PUT /v1/move/resources/directory"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIMoveResourcesToDirectoryMsg.class

            desc """"""
            
			params {

				column {
					name "resourceUuids"
					enclosedIn ""
					desc "批量资源UUID"
					location "body"
					type "List"
					optional false
					since "4.6.0"
					
				}
				column {
					name "directoryUuid"
					enclosedIn ""
					desc "目录UUID"
					location "body"
					type "String"
					optional false
					since "4.6.0"
					
				}
			}
        }

        response {
            clz APIMoveResourcesToDirectoryEvent.class
        }
    }
}