package org.zstack.test.integration.network.l3network.ipv6

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/09/10.
 */
class IPv6DhcpCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        useSpring(KvmTest.springSpec)

    }
    @Override
    void environment() {
        env = Env.Ipv6FlatL3Network()
    }

    @Override
    void test() {
        env.create {
            testAttachDetachL3ToVmNic()
        }
    }

    void testAttachDetachL3ToVmNic() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3_stateless = env.inventoryByName("l3-Stateless-DHCP")
        L3NetworkInventory l3_slaac = env.inventoryByName("l3-SLAAC")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        HostInventory host = env.inventoryByName("kvm-1")

        FlatDhcpBackend.ApplyDhcpCmd cmd = null
        env.afterSimulator(FlatDhcpBackend.APPLY_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            cmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.ApplyDhcpCmd.class)
            return rsp
        }

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_statefull.uuid)
        }
        VmNicInventory nic = vm.getVmNics()[0]
        IpRangeInventory ipr = l3_statefull.getIpRanges().get(0)
        assert cmd != null
        assert cmd.dhcp.size() == 1
        FlatDhcpBackend.DhcpInfo dhcpInfo = cmd.dhcp.get(0)
        assert dhcpInfo.l3NetworkUuid == l3_statefull.uuid
        assert dhcpInfo.ip == nic.ip
        assert dhcpInfo.mac == nic.mac
        assert dhcpInfo.firstIp == ipr.getStartIp()
        assert dhcpInfo.endIp == ipr.getEndIp()
        assert dhcpInfo.ipVersion == nic.getIpVersion()

        cmd = null
        attachL3NetworkToVmNic {
            vmNicUuid = nic.uuid
            l3NetworkUuid = l3_stateless.uuid
        }
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}".toString()]
        } [0]
        nic = vm.getVmNics()[0]
        UsedIpInventory ip2 = null
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.l3NetworkUuid == l3_stateless.uuid) {
                ip2 = ip
                break;
            }
        }
        ipr = l3_stateless.getIpRanges().get(0)
        assert cmd != null
        assert cmd.dhcp.size() == 1
        dhcpInfo = cmd.dhcp.get(0)
        assert dhcpInfo.l3NetworkUuid == ip2.l3NetworkUuid
        assert dhcpInfo.ip == ip2.ip
        assert dhcpInfo.mac == nic.mac
        assert dhcpInfo.firstIp == ipr.getStartIp()
        assert dhcpInfo.endIp == ipr.getEndIp()
        assert dhcpInfo.ipVersion == ip2.ipVersion

        cmd = null
        attachL3NetworkToVmNic {
            vmNicUuid = nic.uuid
            l3NetworkUuid = l3_slaac.uuid
        }
        assert cmd == null

        attachL3NetworkToVmNic {
            vmNicUuid = nic.uuid
            l3NetworkUuid = l3.uuid
        }

        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}".toString()]
        } [0]
        nic = vm.getVmNics()[0]
        UsedIpInventory ip3 = null
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.l3NetworkUuid == l3.uuid) {
                ip3 = ip
                break
            }
        }
        ipr = l3.getIpRanges().get(0)
        assert cmd != null
        assert cmd.dhcp.size() == 1
        dhcpInfo = cmd.dhcp.get(0)
        assert dhcpInfo.l3NetworkUuid == ip3.l3NetworkUuid
        assert dhcpInfo.ip == ip3.ip
        assert dhcpInfo.mac == nic.mac
        assert dhcpInfo.firstIp == ipr.getStartIp()
        assert dhcpInfo.endIp == ipr.getEndIp()
        assert dhcpInfo.ipVersion == ip3.ipVersion

        rebootVmInstance {
            uuid = vm.uuid
        }

        reconnectHost {
            uuid = host.uuid
        }

        FlatDhcpBackend.ReleaseDhcpCmd rcmd = null
        env.afterSimulator(FlatDhcpBackend.RELEASE_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            rcmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.ReleaseDhcpCmd.class)
            return rsp
        }

        UsedIpVO ipVO1 = Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, l3_statefull.uuid).eq(UsedIpVO_.vmNicUuid, nic.uuid).find();
        detachIpAddressFromVmNic {
            vmNicUuid = nic.uuid
            usedIpUuid = ipVO1.uuid
        }
        assert rcmd != null
        assert rcmd.dhcp.size() == 1
        dhcpInfo = rcmd.dhcp.get(0)
        assert dhcpInfo.l3NetworkUuid == ipVO1.l3NetworkUuid
        assert dhcpInfo.ip == ipVO1.ip
        assert dhcpInfo.mac == nic.mac
        assert dhcpInfo.ipVersion == ipVO1.ipVersion

        rcmd = null
        UsedIpVO ipVO2 = Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, l3_stateless.uuid).eq(UsedIpVO_.vmNicUuid, nic.uuid).find();
        detachIpAddressFromVmNic {
            vmNicUuid = nic.uuid
            usedIpUuid = ipVO2.uuid
        }
        assert rcmd != null
        assert rcmd.dhcp.size() == 1
        dhcpInfo = rcmd.dhcp.get(0)
        assert dhcpInfo.l3NetworkUuid == ipVO2.l3NetworkUuid
        assert dhcpInfo.ip == ipVO2.ip
        assert dhcpInfo.mac == nic.mac
        assert dhcpInfo.ipVersion == ipVO2.ipVersion

        rcmd = null
        UsedIpVO ipVO3 = Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, l3_slaac.uuid).eq(UsedIpVO_.vmNicUuid, nic.uuid).find();
        detachIpAddressFromVmNic {
            vmNicUuid = nic.uuid
            usedIpUuid = ipVO3.uuid
        }
        assert rcmd == null

        UsedIpVO ipVO4 = Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, l3.uuid).eq(UsedIpVO_.vmNicUuid, nic.uuid).find();
        detachIpAddressFromVmNic {
            vmNicUuid = nic.uuid
            usedIpUuid = ipVO4.uuid
        }
        assert rcmd != null
        assert rcmd.dhcp.size() == 1
        dhcpInfo = rcmd.dhcp.get(0)
        assert dhcpInfo.l3NetworkUuid == ipVO4.l3NetworkUuid
        assert dhcpInfo.ip == ipVO4.ip
        assert dhcpInfo.mac == nic.mac
        assert dhcpInfo.ipVersion == ipVO4.ipVersion
    }


}

