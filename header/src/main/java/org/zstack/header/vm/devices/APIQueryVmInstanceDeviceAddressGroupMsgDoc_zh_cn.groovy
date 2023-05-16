package org.zstack.header.vm.devices

import org.zstack.header.vm.devices.APIQueryVmInstanceDeviceAddressGroupReply
import org.zstack.header.query.APIQueryMessage
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryVmInstanceDeviceAddressGroup"

    category "snapshot.volume"

    desc """在这里填写API描述"""

    rest {
        request {
			url "GET /v1/vmInstance/device/address/group"
			url "GET /v1/vmInstance/device/address/group/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryVmInstanceDeviceAddressGroupMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryVmInstanceDeviceAddressGroupReply.class
        }
    }
}