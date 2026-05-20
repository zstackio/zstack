package org.zstack.test.unittest.network.sdncontroller

import org.junit.Test
import org.zstack.header.network.sdncontroller.SdnControllerInventory
import org.zstack.header.network.sdncontroller.SdnControllerVO

class SdnControllerInventoryCase {
    @Test
    void testInventoryIpStripsEndpointPort() {
        assert inventoryIp(" 10.0.0.10:8080 ") == "10.0.0.10"
        assert inventoryIp(" controller.example.com:6640 ") == "controller.example.com"
        assert inventoryIp(" [fe80::1]:6640 ") == "fe80::1"
        assert inventoryIp(" 2001:db8::1 ") == "2001:db8::1"
        assert inventoryIp(" controller.example.com ") == "controller.example.com"
    }

    private static String inventoryIp(String endpoint) {
        SdnControllerVO vo = new SdnControllerVO()
        vo.setIp(endpoint)
        return SdnControllerInventory.valueOf(vo).getIp()
    }
}
