package org.zstack.test.integration.networkservice.provider.flat.userdata

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.network.service.flat.FlatUserdataBackend
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class CreateRebootVmKeepUserdataContentCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = FlatNetworkServiceEnv.oneHostNoVmEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateAndRebootKeepUserdataContent()
        }
    }

    void testCreateAndRebootKeepUserdataContent() {
        String userdata = "#cloud-config\nruncmd:\n - echo keep-userdata-content\n"
        String encodedUserdata = new String(Base64.getEncoder().encode(userdata.getBytes()))
        FlatUserdataBackend.ApplyUserdataCmd applyCmd = null

        env.afterSimulator(FlatUserdataBackend.APPLY_USER_DATA) { rsp, HttpEntity<String> e ->
            applyCmd = json(e.body, FlatUserdataBackend.ApplyUserdataCmd.class)
            return rsp
        }

        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            systemTags = [VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): encodedUserdata])]
        }

        assertUserdataNotModified(applyCmd, userdata)

        applyCmd = null
        rebootVmInstance {
            uuid = vm.uuid
        }

        assertUserdataNotModified(applyCmd, userdata)
    }

    private void assertUserdataNotModified(FlatUserdataBackend.ApplyUserdataCmd cmd, String expectedUserdata) {
        assert cmd != null
        assert cmd.userdata.userdataList == [expectedUserdata]
        assert cmd.userdata.metadata.vmHostname == null
    }

    @Override
    void clean() {
        env.delete()
    }
}
