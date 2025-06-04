package org.zstack.header.image

import org.zstack.header.image.APIExpungeImageGroupEvent

doc {
    title "ExpungeImageGroup"

    category "image"

    desc """删除镜像组"""

    rest {
        request {
			url "PUT /v1/imagegroups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIExpungeImageGroupMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "expungeImageGroup"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional true
					since "5.3.36"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.3.36"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.3.36"
				}
			}
        }

        response {
            clz APIExpungeImageGroupEvent.class
        }
    }
}