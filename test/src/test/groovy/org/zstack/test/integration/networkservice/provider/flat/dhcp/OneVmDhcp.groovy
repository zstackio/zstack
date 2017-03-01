package org.zstack.test.integration.networkservice.provider.flat.dhcp

import org.springframework.http.HttpEntity
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.network.service.flat.BridgeNameFinder
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatDhcpBackend.DhcpInfo
import org.zstack.network.service.flat.FlatNetworkSystemTags
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.Env

import org.zstack.testlib.EnvSpec
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.InstanceOfferingSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class OneVmDhcp extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    L3NetworkInventory l3

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneHostNoVmEnv()
    }

    @Override
    void test() {
        env.create {
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory

            testSetDhcpWhenCreateVm()
            testReleaseDhcpWhenStopVm()
            testSetDhcpWhenStartVm()
            testSetDhcpReleaseDhcpWhenRebootVm()
            testSetDhcpWhenReconnectHost()
            testReleaseDhcpWhenDestroyVm()
            testDeleteNamespaceAndDhcpIpWhenDeleteL3Network()
        }
    }

    void testDeleteNamespaceAndDhcpIpWhenDeleteL3Network() {
        FlatDhcpBackend.DeleteNamespaceCmd cmd = null

        def dhcpServerIpUuid = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP
                .getTokenByResourceUuid(l3.uuid, FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN)
        String brName = new BridgeNameFinder().findByL3Uuid(l3.uuid)

        env.afterSimulator(FlatDhcpBackend.DHCP_DELETE_NAMESPACE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.DeleteNamespaceCmd.class)
            return rsp
        }

        deleteL3Network {
            uuid = l3.uuid
        }

        assert cmd != null
        assert cmd.namespaceName != null

        assert cmd.bridgeName == brName
        // make sure the DHCP server IP has been returned
        assert dbFindByUuid(dhcpServerIpUuid, UsedIpVO.class) == null

        // assure relevant tags are deleted
        assert !FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.hasTag(l3.uuid)
        assert new BridgeNameFinder().findByL3Uuid(l3.uuid, false) == null
    }

    void testSetDhcpWhenReconnectHost() {
        testSetDhcpWhenVmOperations {
            reconnectHost {
                uuid = vm.hostUuid
            }
        }
    }

    void testReleaseDhcpWhenDestroyVm() {
        testReleseDhcpWhenVmOperations {
            destroyVmInstance {
                uuid = vm.uuid
            }
        }
    }

    void testSetDhcpReleaseDhcpWhenRebootVm() {
        testReleseDhcpWhenVmOperations {
            rebootVmInstance {
                uuid = vm.uuid
            }
        }

        testSetDhcpWhenVmOperations {
            rebootVmInstance {
                uuid = vm.uuid
            }
        }
    }

    void testSetDhcpWhenStartVm() {
        testSetDhcpWhenVmOperations {
            startVmInstance {
                uuid = vm.uuid
            }
        }
    }

    void assertDhcpInfoVmNic(DhcpInfo info, VmNicInventory nic) {
        assert nic.ip == info.ip
        assert nic.netmask == info.netmask
        assert nic.gateway == info.gateway
        assert info.isDefaultL3Network
        assert info.l3NetworkUuid == nic.l3NetworkUuid
    }

    private testReleseDhcpWhenVmOperations(Closure vmOperation) {
        FlatDhcpBackend.ReleaseDhcpCmd cmd = null

        env.afterSimulator(FlatDhcpBackend.RELEASE_DHCP_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.ReleaseDhcpCmd.class)
            return rsp
        }

        vmOperation()

        assert cmd != null
        assert cmd.dhcp.size() == 1
        DhcpInfo info = cmd.dhcp[0]
        VmNicInventory nic = vm.vmNics[0]
        assertDhcpInfoVmNic(info, nic)
    }

    void testReleaseDhcpWhenStopVm() {
        testReleseDhcpWhenVmOperations {
            stopVmInstance {
                uuid = vm.uuid
            }
        }
    }

    private void testSetDhcpWhenVmOperations(Closure vmOperation) {
        FlatDhcpBackend.ApplyDhcpCmd cmd = null

        env.afterSimulator(FlatDhcpBackend.APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.ApplyDhcpCmd.class)
            return rsp
        }

        FlatDhcpBackend.PrepareDhcpCmd pcmd = null
        env.afterSimulator(FlatDhcpBackend.PREPARE_DHCP_PATH) { rsp, HttpEntity<String> e ->
            pcmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.PrepareDhcpCmd.class)
            return rsp
        }

        vmOperation()

        // check ApplyDhcpCmd
        assert cmd != null
        assert cmd.l3NetworkUuid == l3.uuid
        assert cmd.dhcp.size() == 1

        DhcpInfo info = cmd.dhcp[0]
        VmNicInventory vmNic = vm.vmNics[0]
        assertDhcpInfoVmNic(info, vmNic)

        String brName = new BridgeNameFinder().findByL3Uuid(l3.uuid)
        assert brName == info.bridgeName
        assert info.namespaceName != null

        // check PrepareDhcpCmd
        assert pcmd != null
        assert pcmd.namespaceName != null
        assert pcmd.bridgeName == brName
        assert pcmd.dhcpNetmask == vmNic.netmask

        // check the DHCP IP
        // the DHCP server will occupy an IP
        def tokens = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTokensByResourceUuid(l3.uuid)
        String dhcpServerIp = tokens.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN)
        String dhcpServerIpUuid = tokens.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN)
        assert dhcpServerIp == pcmd.dhcpServerIp

        UsedIpVO dhcpIpVO = dbFindByUuid(dhcpServerIpUuid, UsedIpVO.class)
        assert dhcpIpVO != null
        assert dhcpIpVO.ip == dhcpServerIp
        assert dhcpIpVO.netmask == vmNic.netmask
        assert dhcpIpVO.gateway == vmNic.gateway
        assert dhcpIpVO.l3NetworkUuid == l3.uuid
    }

    void testSetDhcpWhenCreateVm() {
        testSetDhcpWhenVmOperations {
            ImageSpec image = env.specByName("image")
            InstanceOfferingSpec instanceOffering = env.specByName("instanceOffering")

            vm = createVmInstance {
                name = "vm"
                imageUuid = image.inventory.uuid
                l3NetworkUuids = [l3.uuid]
                instanceOfferingUuid = instanceOffering.inventory.uuid
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
