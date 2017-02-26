package org.zstack.test.integration.networkservice.provider.flat

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.network.service.flat.BridgeNameFinder
import org.zstack.network.service.flat.FlatNetworkSystemTags
import org.zstack.network.service.flat.FlatUserdataBackend
import org.zstack.network.service.userdata.UserdataGlobalProperty
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.testlib.*

/**
 * Created by xing5 on 2017/2/26.
 */
class OneVmUserdata extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    L3NetworkInventory l3
    String userdata = "this test user data"

    @Override
    void setup() {
        useSpring(FlatNetworkProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneHostNoVmEnv()
    }

    @Override
    void test() {
        env.create {
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory

            testSetUserdataWhenCreateVm()
            testDeleteUserdataWhenStopVm()
            testSetUserdataWhenStartVm()
            testSetAndDeleteUserddataWhenRebootVm()
            testDeleteUserdataWhenDestroyVm()
        }
    }

    void testDeleteUserdataWhenDestroyVm() {
        testDeleteUserdataSetWhenVmOperations {
            destroyVmInstance {
                uuid = vm.uuid
            }
        }
    }

    void testSetUserdataWhenStartVm() {
        testSetUserdataSetWhenVmOperations {
            startVmInstance {
                uuid = vm.uuid
            }
        }
    }

    void testSetAndDeleteUserddataWhenRebootVm() {
        testSetUserdataSetWhenVmOperations {
            rebootVmInstance {
                uuid = vm.uuid
            }
        }

        testDeleteUserdataSetWhenVmOperations {
            rebootVmInstance {
                uuid = vm.uuid
            }
        }
    }

    void testDeleteUserdataWhenStopVm() {
        testDeleteUserdataSetWhenVmOperations {
            stopVmInstance {
                uuid = vm.uuid
            }
        }
    }

    void testSetUserdataWhenCreateVm() {
        testSetUserdataSetWhenVmOperations() {
            ImageSpec image = env.specByName("image")
            InstanceOfferingSpec offering = env.specByName("instanceOffering")

            vm = createVmInstance {
                name = "vm"
                imageUuid = image.inventory.uuid
                l3NetworkUuids = [l3.uuid]
                instanceOfferingUuid = offering.inventory.uuid
                systemTags = [VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): userdata])]
            }
        }
    }

    private void testDeleteUserdataSetWhenVmOperations(Closure vmOperation) {
        FlatUserdataBackend.ReleaseUserdataCmd cmd = null

        env.afterSimulator(FlatUserdataBackend.RELEASE_USER_DATA) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, FlatUserdataBackend.ReleaseUserdataCmd.class)
            return rsp
        }

        vmOperation()

        assert cmd != null
        assert cmd.namespaceName != null
        assert cmd.vmIp == vm.vmNics[0].ip
        assert cmd.bridgeName == new BridgeNameFinder().findByL3Uuid(l3.uuid)
    }

    private void testSetUserdataSetWhenVmOperations(Closure vmOperation) {
        FlatUserdataBackend.ApplyUserdataCmd cmd = null

        env.afterSimulator(FlatUserdataBackend.APPLY_USER_DATA) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, FlatUserdataBackend.ApplyUserdataCmd.class)
            return rsp
        }

        vmOperation()

        assert cmd != null
        String dhcpIp = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP
                .getTokenByResourceUuid(l3.uuid, FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpIp == cmd.userdata.dhcpServerIp
        assert new BridgeNameFinder().findByL3Uuid(l3.uuid) == cmd.userdata.bridgeName

        VmNicInventory nic = vm.vmNics[0]
        assert nic.ip == cmd.userdata.vmIp
        assert cmd.userdata.namespaceName != null
        assert UserdataGlobalProperty.HOST_PORT == cmd.userdata.port
        assert userdata == cmd.userdata.userdata

        assert vm.uuid == cmd.userdata.metadata.vmUuid
    }

    @Override
    void clean() {
        env.delete()
    }
}
