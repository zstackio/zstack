package org.zstack.test.unittest.network.sdncontroller

import org.junit.Test
import org.zstack.header.message.GsonTransient
import org.zstack.header.network.sdncontroller.SdnControllerInventory
import org.zstack.header.network.sdncontroller.SdnControllerVO
import org.zstack.header.rest.APINoSee
import org.zstack.utils.gson.JSONObjectUtil

import java.lang.reflect.Field

class SdnControllerInventoryCase {
    @Test
    void testInventoryIpStripsEndpointPort() {
        assert inventoryIp(" 10.0.0.10:8080 ") == "10.0.0.10"
        assert inventoryIp(" controller.example.com:6640 ") == "controller.example.com"
        assert inventoryIp(" [fe80::1]:6640 ") == "fe80::1"
        assert inventoryIp(" 2001:db8::1 ") == "2001:db8::1"
        assert inventoryIp(" controller.example.com ") == "controller.example.com"
    }

    @Test
    void testInventoryOmitsPlaintextPassword() {
        SdnControllerVO vo = new SdnControllerVO()
        vo.setUuid("sdn-uuid-1")
        vo.setUsername("admin")
        vo.setPassword("PlainTextSecret!123")

        SdnControllerInventory inv = SdnControllerInventory.valueOf(vo)
        assert inv.getPassword() == null

        Field pwdField = SdnControllerInventory.class.getDeclaredField("password")
        assert pwdField.isAnnotationPresent(GsonTransient.class)
        assert pwdField.isAnnotationPresent(APINoSee.class)

        inv.setPassword("PlainTextSecret!123")
        String json = JSONObjectUtil.toJsonString(inv)
        assert !json.contains("PlainTextSecret!123")
        assert !json.contains('"password"')
    }

    private static String inventoryIp(String endpoint) {
        SdnControllerVO vo = new SdnControllerVO()
        vo.setIp(endpoint)
        return SdnControllerInventory.valueOf(vo).getIp()
    }
}
