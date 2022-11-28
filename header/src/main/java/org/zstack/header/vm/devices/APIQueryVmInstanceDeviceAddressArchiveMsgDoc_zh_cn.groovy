package org.zstack.header.vm.devices

import org.zstack.header.vm.devices.APIQueryVmInstanceDeviceAddressArchiveReply
import org.zstack.header.query.APIQueryMessage

doc {
    title "QueryVmInstanceDeviceAddressArchive"

    category "snapshot.volume"

    desc """查询云主机设备地址归档"""

    rest {
        request {
			url "GET /v1/vmInstance/device/address/archive"
			url "GET /v1/vmInstance/device/address/archive/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIQueryVmInstanceDeviceAddressArchiveMsg.class

            desc """"""
            
			params APIQueryMessage.class
        }

        response {
            clz APIQueryVmInstanceDeviceAddressArchiveReply.class
        }
    }
}