package org.zstack.test.integration.network.l3network.ipv6

import com.googlecode.ipv6.IPv6Address
import org.springframework.http.HttpEntity
import org.zstack.compute.vm.StaticIpOperator
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.network.l3.NormalIpRangeVO
import org.zstack.header.network.l3.NormalIpRangeVO_
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.vm.VmNicVO
import org.zstack.network.service.NetworkServiceManager
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkSystemTags
import org.zstack.sdk.GetL3NetworkDhcpIpAddressResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.UsedIpInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants
import org.zstack.utils.network.IPv6NetworkUtils

import java.util.stream.Collectors

/**
 * Created by shixin on 2018/09/10.
 */
class SetStaticIpv6AddressCase extends SubCase {
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
            testCreateVmOfPrivateIPv6Network()
            testCreateVmWithStaticIpv6()
            testAttachL3NetworkToVmNicWithStaticIpv6()
            testDeleteVmStaticIpCleansCustomNetworkParamsAndRestoresL3Policy()
        }
    }

    void testCreateVmOfPrivateIPv6Network() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L3NetworkInventory l3_vlan = env.inventoryByName("l3-vlan-ipv4")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        /* create vm with 2 nic: 1 nic with ipv4, the other with ipv6 */
        VmInstanceInventory vm = createVmInstance {
            name = "vm-static-ip"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3_statefull.uuid, l3_vlan.uuid]
            defaultL3NetworkUuid = l3_statefull.uuid
        }
        assert vm.getVmNics().size() == 2

        VmNicInventory nic
        for (VmNicInventory nicInv: vm.getVmNics()) {
            assert nicInv.getUsedIps().size() == 1
            for (UsedIpInventory ip : nicInv.usedIps) {
                if (ip.l3NetworkUuid == l3_statefull.uuid) {
                    nic = nicInv
                }
            }

        }
        assert nic != null

        /* add ipv4 to the ipv6 nic */
        addIpRangeByNetworkCidr {
            name = "ipr4-1"
            l3NetworkUuid = l3_statefull.getUuid()
            networkCidr = "192.168.110.0/24"
        }

        /* reboot the vm to allocate ipv6 address */
        rebootVmInstance {
            uuid = vm.uuid
        }

        UsedIpInventory ip4 = null
        UsedIpInventory ip6 = null
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}"]
        } [0]
        nic = queryVmNic {conditions = ["uuid=${nic.uuid}"]} [0]
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.ipVersion == IPv6Constants.IPv4) {
                ip4 = ip
            } else if (ip.ipVersion == IPv6Constants.IPv6) {
                ip6 = ip
            }
        }
        nic.getUsedIps().size() == 2
        assert ip4 != null
        assert ip6 != null

        /* static ip should not be dhcp server ip or other used ip */
        GetL3NetworkDhcpIpAddressResult ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l3_statefull.uuid
        }
        assert ret.ip != null
        assert ret.ip6 != null
        assert ret.ip != ret.ip6

        List<String> ip6s = new ArrayList<>()
        ip6s.add(IPv6Address.fromString("2001:2003::02").toString())
        ip6s.add(IPv6Address.fromString("2001:2003::03").toString())
        ip6s.add(IPv6Address.fromString("2001:2003::04").toString())
        ip6s.add(IPv6Address.fromString("2001:2003::05").toString())
        ip6s.remove(ip6.ip)
        ip6s.remove(ret.ip6)
        String ip6Str = ip6s.get(0)

        List<String> ip4s = new ArrayList<>()
        ip4s.add("192.168.110.11")
        ip4s.add("192.168.110.12")
        ip4s.add("192.168.110.13")
        ip4s.add("192.168.110.14")
        ip4s.remove(ip4.ip)
        ip4s.remove(ret.ip)
        String ip4Str = ip4s.get(0)

        stopVmInstance {
            uuid = vm.uuid
        }

        setVmStaticIp {
            delegate.vmInstanceUuid = vm.uuid
            delegate.l3NetworkUuid = l3_statefull.uuid
            delegate.ip = ip4Str
            delegate.ip6 = ip6Str
        }

        nic = queryVmNic {conditions = ["uuid=${nic.uuid}"]} [0]
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.ipVersion == IPv6Constants.IPv4) {
                assert ip.ip == ip4Str
            } else if (ip.ipVersion == IPv6Constants.IPv6) {
                assert ip.ip == ip6Str
            }
        }

        Map<String, List<String>> staticIpMap = new StaticIpOperator().getStaticIpbyVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) != null
        assert staticIpMap.get(l3_statefull.uuid).size() == 2
        assert staticIpMap.get(l3_statefull.uuid).contains(ip4Str)
        assert staticIpMap.get(l3_statefull.uuid).contains(ip6Str)

        deleteVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3_statefull.uuid
        }
        staticIpMap = new StaticIpOperator().getStaticIpbyVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) == null

        ip4Str = ip4s.get(1)
        ip6Str = ip6s.get(1)
        setVmStaticIp {
            delegate.vmInstanceUuid = vm.uuid
            delegate.l3NetworkUuid = l3_statefull.uuid
            delegate.ip = ip4Str
            delegate.ip6 = ip6Str
        }

        deleteVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3_statefull.uuid
            staticIp = ip4Str
        }
        staticIpMap = new StaticIpOperator().getStaticIpbyVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) != null
        assert staticIpMap.get(l3_statefull.uuid).size() == 1
        assert staticIpMap.get(l3_statefull.uuid).contains(ip6Str)

        deleteVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3_statefull.uuid
            staticIp = ip6Str
        }
        staticIpMap = new StaticIpOperator().getStaticIpbyVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) == null

        destroyVmInstance {
            uuid = vm.uuid
        }
    }

    void testCreateVmWithStaticIpv6() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        /* static ip should not be dhcp server ip or other used ip */
        GetL3NetworkDhcpIpAddressResult ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l3_statefull.uuid
        }
        assert ret.ip != null
        assert ret.ip6 != null
        assert ret.ip != ret.ip6

        List<String> ip6s = new ArrayList<>()
        ip6s.add(IPv6Address.fromString("2001:2003::12").toString())
        ip6s.add(IPv6Address.fromString("2001:2003::13").toString())
        ip6s.add(IPv6Address.fromString("2001:2003::14").toString())
        ip6s.remove(ret.ip6)
        String ip6Str = ip6s.get(0)

        List<String> ip4s = new ArrayList<>()
        ip4s.add("192.168.110.21")
        ip4s.add("192.168.110.22")
        ip4s.add("192.168.110.23")
        ip4s.remove(ret.ip)
        String ip4Str = ip4s.get(0)

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3_statefull.uuid]
            defaultL3NetworkUuid = l3_statefull.uuid
            systemTags = [String.format("staticIp::%s::%s", l3_statefull.uuid, ip4Str),
                          String.format("staticIp::%s::%s", l3_statefull.uuid, IPv6NetworkUtils.ipv6AddessToTagValue(ip6Str))]
        }
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}"]
        } [0]
        VmNicInventory nic = vm.getVmNics().get(0)
        assert nic.usedIps.size() == 2
        for (UsedIpInventory ip : nic.usedIps) {
            if (ip.ipVersion == IPv6Constants.IPv4) {
                assert ip.ip == ip4Str
            } else if (ip.ipVersion == IPv6Constants.IPv6){
                assert ip.ip == ip6Str
            }
        }
        Map<String, List<String>> staticIpMap = new StaticIpOperator().getStaticIpbyVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) != null
        assert staticIpMap.get(l3_statefull.uuid).size() == 2
        assert staticIpMap.get(l3_statefull.uuid).contains(ip4Str)
        assert staticIpMap.get(l3_statefull.uuid).contains(ip6Str)

        expect(AssertionError.class) {
            deleteVmStaticIp {
                vmInstanceUuid = vm.uuid
                l3NetworkUuid = "invalid uuid"
            }
        }

        deleteVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3_statefull.uuid
            staticIp = ip6Str
        }
        staticIpMap = new StaticIpOperator().getStaticIpbyVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) != null
        assert staticIpMap.get(l3_statefull.uuid).size() == 1
        assert staticIpMap.get(l3_statefull.uuid).contains(ip4Str)

        destroyVmInstance {
            uuid = vm.uuid
        }
    }

    void testAttachL3NetworkToVmNicWithStaticIpv6() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            defaultL3NetworkUuid = l3.uuid
        }

        /* static ip should not be dhcp server ip or other used ip */
        GetL3NetworkDhcpIpAddressResult ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l3_statefull.uuid
        }
        assert ret.ip != null
        assert ret.ip6 != null
        assert ret.ip != ret.ip6

        List<String> ip6s = new ArrayList<>()
        ip6s.add(IPv6Address.fromString("2001:2003::22").toString())
        ip6s.add(IPv6Address.fromString("2001:2003::23").toString())
        ip6s.add(IPv6Address.fromString("2001:2003::24").toString())
        ip6s.remove(ret.ip6)
        String ip6Str = ip6s.get(0)

        List<String> ip4s = new ArrayList<>()
        ip4s.add("192.168.110.31")
        ip4s.add("192.168.110.32")
        ip4s.add("192.168.110.33")
        ip4s.remove(ret.ip)
        String ip4Str = ip4s.get(0)

        attachL3NetworkToVm {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3_statefull.uuid
            systemTags = [String.format("staticIp::%s::%s", l3_statefull.uuid, ip4Str),
                          String.format("staticIp::%s::%s", l3_statefull.uuid, IPv6NetworkUtils.ipv6AddessToTagValue(ip6Str))]
        }
        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}"]
        } [0]

        for (VmNicInventory nic : vm.vmNics) {
            if (nic.l3NetworkUuid == l3_statefull.uuid) {
                for (UsedIpInventory ip : nic.getUsedIps()) {
                    if (ip.ipVersion == IPv6Constants.IPv4) {
                        assert ip.ip == ip4Str
                    } else if (ip.ipVersion == IPv6Constants.IPv6){
                        assert ip.ip == ip6Str
                    }
                }
            }
        }

        Map<String, List<String>> staticIpMap = new StaticIpOperator().getStaticIpbyVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) != null
        assert staticIpMap.get(l3_statefull.uuid).size() == 2
        assert staticIpMap.get(l3_statefull.uuid).contains(ip4Str)
        assert staticIpMap.get(l3_statefull.uuid).contains(ip6Str)

        stopVmInstance {
            uuid = vm.uuid
        }
        ip6Str = ip6s.get(1)
        ip4Str = ip4s.get(1)
        setVmStaticIp {
            delegate.vmInstanceUuid = vm.uuid
            delegate.l3NetworkUuid = l3_statefull.uuid
            delegate.ip = ip4Str
            delegate.ip6 = ip6Str
        }
        staticIpMap = new StaticIpOperator().getStaticIpbyVmUuid(vm.uuid)
        assert staticIpMap.get(l3_statefull.uuid) != null
        assert staticIpMap.get(l3_statefull.uuid).size() == 2
        assert staticIpMap.get(l3_statefull.uuid).contains(ip4Str)
        assert staticIpMap.get(l3_statefull.uuid).contains(ip6Str)
    }

    void testDeleteVmStaticIpCleansCustomNetworkParamsAndRestoresL3Policy() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        NetworkServiceManager nsMgr = bean(NetworkServiceManager.class)
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        addDnsToL3Network {
            l3NetworkUuid = l3_statefull.uuid
            dns = "8.8.8.8"
        }

        NormalIpRangeVO ipv4Range = Q.New(NormalIpRangeVO.class)
                .eq(NormalIpRangeVO_.l3NetworkUuid, l3_statefull.uuid)
                .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4)
                .find()
        assert ipv4Range != null

        GetL3NetworkDhcpIpAddressResult dhcpRet = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l3_statefull.uuid
        }

        VmInstanceInventory vm = null
        VmInstanceInventory vm2 = null
        try {
            vm = createVmInstance {
                name = "vm-delete-static-params"
                instanceOfferingUuid = offering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3_statefull.uuid]
                defaultL3NetworkUuid = l3_statefull.uuid
            }

            List<String> vmInitialIps = vm.vmNics[0].usedIps.collect { it.ip }
            List<String> ip4Candidates = ["192.168.110.70", "192.168.110.71", "192.168.110.72", "192.168.110.73"]
            ip4Candidates.remove(dhcpRet.ip)
            ip4Candidates.removeAll(vmInitialIps)
            String staticIp4 = ip4Candidates[0]
            String staticIp4Dual = ip4Candidates[1]

            List<String> ip6Candidates = [
                    IPv6Address.fromString("2001:2003::70").toString(),
                    IPv6Address.fromString("2001:2003::71").toString(),
                    IPv6Address.fromString("2001:2003::72").toString()
            ]
            ip6Candidates.remove(dhcpRet.ip6)
            ip6Candidates.removeAll(vmInitialIps)
            String staticIp6Dual = ip6Candidates[0]

            vm2 = createVmInstance {
                name = "vm-delete-dual-static-params"
                instanceOfferingUuid = offering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3_statefull.uuid]
                defaultL3NetworkUuid = l3_statefull.uuid
                systemTags = [
                        "staticIp::${l3_statefull.uuid}::${staticIp4Dual}".toString(),
                        "ipv4Netmask::${l3_statefull.uuid}::255.255.255.0".toString(),
                        "ipv4Gateway::${l3_statefull.uuid}::192.168.110.1".toString(),
                        "staticIp::${l3_statefull.uuid}::${IPv6NetworkUtils.ipv6AddessToTagValue(staticIp6Dual)}".toString(),
                        "ipv6Prefix::${l3_statefull.uuid}::64".toString(),
                        "ipv6Gateway::${l3_statefull.uuid}::${IPv6NetworkUtils.ipv6AddessToTagValue('2001:2003::1')}".toString(),
                        "staticDns::${l3_statefull.uuid}::9.9.9.9".toString()
                ]
            }

            setVmStaticIp {
                vmInstanceUuid = vm.uuid
                l3NetworkUuid = l3_statefull.uuid
                ip = staticIp4
                netmask = "255.255.0.0"
                gateway = "192.168.0.1"
                dnsAddresses = ["9.9.9.9"]
            }
            VmSystemTags.SYNC_PORTS.delete(vm.uuid)

            assert !VmSystemTags.STATIC_DNS.getTags(vm.uuid).isEmpty()
            assert nsMgr.getVmNicDns(vm.uuid, l3_statefull.uuid) == ["9.9.9.9"]

            // Deleting another VM's IP on the shared L3 must keep both VMs' static IP, netmask, gateway, and DNS intact
            deleteVmStaticIp {
                vmInstanceUuid = vm2.uuid
                l3NetworkUuid = l3_statefull.uuid
                staticIp = staticIp4
            }
            assert new StaticIpOperator().getStaticIpbyVmUuid(vm2.uuid).get(l3_statefull.uuid).containsAll([staticIp4Dual, staticIp6Dual])
            assert VmSystemTags.IPV4_NETMASK.getTokenByResourceUuid(vm2.uuid, VmSystemTags.IPV4_NETMASK_TOKEN) == "255.255.255.0"
            assert VmSystemTags.IPV4_GATEWAY.getTokenByResourceUuid(vm2.uuid, VmSystemTags.IPV4_GATEWAY_TOKEN) == "192.168.110.1"
            assert nsMgr.getVmNicDns(vm2.uuid, l3_statefull.uuid) == ["9.9.9.9"]

            deleteVmStaticIp {
                vmInstanceUuid = vm.uuid
                l3NetworkUuid = l3_statefull.uuid
                staticIp = staticIp4Dual
            }
            assert new StaticIpOperator().getStaticIpbyVmUuid(vm.uuid).get(l3_statefull.uuid) == [staticIp4]
            assert nsMgr.getVmNicDns(vm.uuid, l3_statefull.uuid) == ["9.9.9.9"]
            assert !VmSystemTags.SYNC_PORTS.hasTag(vm.uuid)
            VmNicVO nicUnchanged = dbFindByUuid(vm.vmNics[0].uuid, VmNicVO.class)
            assert nicUnchanged.ip == staticIp4
            assert nicUnchanged.netmask == "255.255.0.0"
            assert nicUnchanged.gateway == "192.168.0.1"

            // First DHCP refresh failure surfaces error to caller; retry succeeds and pushes L3 DNS and range defaults
            boolean failDhcpOnce = true
            FlatDhcpBackend.BatchApplyDhcpCmd onlineDeleteCmd = null
            env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { FlatDhcpBackend.ApplyDhcpRsp rsp, HttpEntity<String> e ->
                if (failDhcpOnce) {
                    rsp.success = false
                    rsp.error = "simulated dhcp apply failure"
                    return rsp
                }
                onlineDeleteCmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
                return rsp
            }

            expect(AssertionError.class) {
                deleteVmStaticIp {
                    vmInstanceUuid = vm.uuid
                    l3NetworkUuid = l3_statefull.uuid
                    staticIp = staticIp4
                }
            }
            assert onlineDeleteCmd == null

            failDhcpOnce = false
            deleteVmStaticIp {
                vmInstanceUuid = vm.uuid
                l3NetworkUuid = l3_statefull.uuid
                staticIp = staticIp4
            }

            assert new StaticIpOperator().getStaticIpbyVmUuid(vm.uuid).get(l3_statefull.uuid) == null
            assert VmSystemTags.STATIC_DNS.getTags(vm.uuid).isEmpty() : "staticDns tag must be deleted on DeleteVmStaticIp"
            assert VmSystemTags.IPV4_NETMASK.getTags(vm.uuid).isEmpty()
            assert VmSystemTags.IPV4_GATEWAY.getTags(vm.uuid).isEmpty()
            assert VmSystemTags.SYNC_PORTS.hasTag(vm.uuid) : "SYNC_PORTS tag must be set so QGA syncs restored L3 DNS/params"

            List<String> dnsAfterDelete = nsMgr.getVmNicDns(vm.uuid, l3_statefull.uuid)
            assert dnsAfterDelete.contains("8.8.8.8") && !dnsAfterDelete.contains("9.9.9.9")

            VmNicVO nicAfterDelete = dbFindByUuid(vm.vmNics[0].uuid, VmNicVO.class)
            assert nicAfterDelete.netmask == ipv4Range.netmask : "NIC netmask should revert to L3 range default, got ${nicAfterDelete.netmask}"
            assert nicAfterDelete.gateway == ipv4Range.gateway : "NIC gateway should revert to L3 range default, got ${nicAfterDelete.gateway}"
            UsedIpVO usedIp4AfterDelete = Q.New(UsedIpVO.class)
                    .eq(UsedIpVO_.vmNicUuid, nicAfterDelete.uuid)
                    .eq(UsedIpVO_.ipVersion, IPv6Constants.IPv4)
                    .find()
            assert usedIp4AfterDelete.netmask == ipv4Range.netmask
            assert usedIp4AfterDelete.gateway == ipv4Range.gateway

            assert onlineDeleteCmd != null : "Retried DeleteVmStaticIp on running VM must refresh DHCP"
            def onlineDhcp = onlineDeleteCmd.dhcpInfos.collectMany { it.dhcp }.find { it.ip == staticIp4 }
            assert onlineDhcp != null
            assert onlineDhcp.dns.contains("8.8.8.8") && !onlineDhcp.dns.contains("9.9.9.9")
            assert onlineDhcp.netmask == ipv4Range.netmask
            assert onlineDhcp.gateway == ipv4Range.gateway

            // When UsedIpVO already matches range defaults but primary VmNicVO drifted, DeleteVmStaticIp repairs VmNicVO
            nicAfterDelete.setNetmask("255.255.0.0")
            nicAfterDelete.setGateway("192.168.0.1")
            dbf.update(nicAfterDelete)
            assert dbFindByUuid(usedIp4AfterDelete.uuid, UsedIpVO.class).netmask == ipv4Range.netmask

            deleteVmStaticIp {
                vmInstanceUuid = vm.uuid
                l3NetworkUuid = l3_statefull.uuid
            }
            VmNicVO nicAfterDriftRepair = dbFindByUuid(vm.vmNics[0].uuid, VmNicVO.class)
            assert nicAfterDriftRepair.netmask == ipv4Range.netmask
            assert nicAfterDriftRepair.gateway == ipv4Range.gateway

            stopVmInstance {
                uuid = vm.uuid
            }

            FlatDhcpBackend.BatchApplyDhcpCmd restartCmd = null
            env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
                restartCmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
                return rsp
            }

            startVmInstance {
                uuid = vm.uuid
            }

            assert restartCmd != null
            def restartDhcp = restartCmd.dhcpInfos.collectMany { it.dhcp }.find { it.ip == staticIp4 }
            assert restartDhcp != null
            assert restartDhcp.dns.contains("8.8.8.8") && !restartDhcp.dns.contains("9.9.9.9")
            assert restartDhcp.netmask == ipv4Range.netmask
            assert restartDhcp.gateway == ipv4Range.gateway

            // Dual-stack partial delete cleans version-specific tags, and last delete cleans staticDns
            deleteVmStaticIp {
                vmInstanceUuid = vm2.uuid
                l3NetworkUuid = l3_statefull.uuid
                staticIp = staticIp4Dual
            }
            assert VmSystemTags.IPV4_NETMASK.getTags(vm2.uuid).isEmpty()
            assert VmSystemTags.IPV4_GATEWAY.getTags(vm2.uuid).isEmpty()
            assert !VmSystemTags.IPV6_PREFIX.getTags(vm2.uuid).isEmpty()
            assert !VmSystemTags.IPV6_GATEWAY.getTags(vm2.uuid).isEmpty()
            assert !VmSystemTags.STATIC_DNS.getTags(vm2.uuid).isEmpty()

            deleteVmStaticIp {
                vmInstanceUuid = vm2.uuid
                l3NetworkUuid = l3_statefull.uuid
                staticIp = staticIp6Dual
            }
            assert VmSystemTags.IPV6_PREFIX.getTags(vm2.uuid).isEmpty()
            assert VmSystemTags.IPV6_GATEWAY.getTags(vm2.uuid).isEmpty()
            assert VmSystemTags.STATIC_DNS.getTags(vm2.uuid).isEmpty()
        } finally {
            env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
                return rsp
            }
            if (vm != null) {
                destroyVmInstance {
                    uuid = vm.uuid
                }
                expungeVmInstance {
                    uuid = vm.uuid
                }
            }
            if (vm2 != null) {
                destroyVmInstance {
                    uuid = vm2.uuid
                }
                expungeVmInstance {
                    uuid = vm2.uuid
                }
            }
            removeDnsFromL3Network {
                l3NetworkUuid = l3_statefull.uuid
                dns = "8.8.8.8"
            }
        }
    }
}

