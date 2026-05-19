package org.zstack.header.volume

import org.zstack.header.volume.APIReInitDataVolumeEvent

doc {
    title "重新初始化数据云盘"

    category "volume"

    desc """重新初始化一个处于可用状态的数据云盘。该操作会清空数据云盘中的现有数据，若数据云盘已挂载到云主机，云主机必须处于已停止状态。"""

    rest {
        request {
			url "PUT /v1/volumes/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIReInitDataVolumeMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "reInitDataVolume"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "5.4.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.4.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.4.2"
				}
			}
        }

        response {
            clz APIReInitDataVolumeEvent.class
        }
    }
}