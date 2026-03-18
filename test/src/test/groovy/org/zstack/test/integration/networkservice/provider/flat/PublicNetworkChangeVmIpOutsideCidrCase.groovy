package org.zstack.test.integration.networkservice.provider.flat

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmNicVO
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Test IP outside CIDR behavior for public networks.
 * Outside-range IPs are always allowed (no global config needed).
 *
 * Public network combinations (2 combos):
 *   1. pubL3_range_noDhcp  — has IP range, no DHCP  (enableIPAM=true, enableIpAddressAllocation()=false)
 *   2. pubL3_range_dhcp    — has IP range, has DHCP  (enableIPAM=true, enableIpAddressAllocation()=true)
 *
 * Each scenario tests: setVmStaticIp, changeVmNicNetwork, DHCP skip.
 * No EIP tests — public networks do not need EIP testing.
 */
class PublicNetworkChangeVmIpOutsideCidrCase extends SubCase {

    EnvSpec env
    DatabaseFacade dbf

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2-pub-range-noDhcp")
                    attachL2Network("l2-pub-range-dhcp")
                    attachL2Network("l2-dest")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                // ========== Public: has IP range, no DHCP ==========
                l2NoVlanNetwork {
                    name = "l2-pub-range-noDhcp"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "pubL3_range_noDhcp"
                        category = "Public"

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "12.100.10.10"
                            endIp = "12.100.10.200"
                            netmask = "255.255.255.0"
                            gateway = "12.100.10.1"
                        }
                    }
                }

                // ========== Public: has IP range, has DHCP ==========
                l2NoVlanNetwork {
                    name = "l2-pub-range-dhcp"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "pubL3_range_dhcp"
                        category = "Public"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(),
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }
                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "12.100.20.10"
                            endIp = "12.100.20.200"
                            netmask = "255.255.255.0"
                            gateway = "12.100.20.1"
                        }
                    }
                }

                // ========== Destination L3 for changeVmNicNetwork tests ==========
                l2NoVlanNetwork {
                    name = "l2-dest"
                    physicalInterface = "eth2"

                    l3Network {
                        name = "flatL3_dest"
                        enableIPAM = false

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }
                        // No IP range, no DHCP — used as changeVmNicNetwork source
                    }
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            // ==========================================
            // Outside-range IPs are always allowed (no global config needed)
            // ==========================================

            // --- Public: has IP range, no DHCP ---
            testSetStaticIp_pubRangeNoDhcp()
            testChangeNicNetwork_pubRangeNoDhcp()
            testDhcpSkip_pubRangeNoDhcp()

            // --- Public: has IP range, has DHCP ---
            testSetStaticIp_pubRangeDhcp()
            testChangeNicNetwork_pubRangeDhcp()
            testDhcpSkip_pubRangeDhcp()
        }
    }

    // ================================================================
    //  Helper: create a VM on a given L3
    // ================================================================

    VmInstanceInventory createVmOnL3(String vmName, String l3Uuid) {
        return createVmInstance {
            name = vmName
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [l3Uuid]
        }
    }

    // ================================================================
    //  Public: has IP range, no DHCP
    // ================================================================

    /**
     * Public/range/no-DHCP: outside-range IP should succeed; in-range IP also works.
     */
    void testSetStaticIp_pubRangeNoDhcp() {
        L3NetworkInventory l3 = env.inventoryByName("pubL3_range_noDhcp")

        VmInstanceInventory vm = createVmOnL3("vm-pub-range-noDhcp-set", l3.uuid)

        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3.uuid
            ip = "10.0.2.50"
            netmask = "255.255.255.0"
            gateway = "10.0.2.1"
        }

        UsedIpVO usedIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.vmNicUuid, vm.vmNics[0].uuid)
                .eq(UsedIpVO_.ip, "10.0.2.50")
                .find()
        assert usedIp != null
        assert usedIp.ipRangeUuid == null : "ipRangeUuid should be null for outside-range IP"
        assert usedIp.netmask == "255.255.255.0"
        assert usedIp.gateway == "10.0.2.1"

        VmNicVO nicVO = dbFindByUuid(vm.vmNics[0].uuid, VmNicVO.class)
        assert nicVO.ip == "10.0.2.50"
    }

    /**
     * Public/range/no-DHCP: changeVmNicNetwork with outside-range IP should succeed.
     */
    void testChangeNicNetwork_pubRangeNoDhcp() {
        L3NetworkInventory l3 = env.inventoryByName("pubL3_range_noDhcp")
        L3NetworkInventory srcL3 = env.inventoryByName("flatL3_dest")

        VmInstanceInventory vm = createVmOnL3("vm-pub-range-noDhcp-change", srcL3.uuid)
        VmNicInventory vmNic = vm.vmNics[0]

        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = l3.uuid
            systemTags = [
                    String.format("staticIp::%s::10.0.2.60", l3.uuid),
                    String.format("ipv4Netmask::%s::255.255.255.0", l3.uuid),
                    String.format("ipv4Gateway::%s::10.0.2.1", l3.uuid)
            ]
        }

        VmNicVO nicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        assert nicVO.l3NetworkUuid == l3.uuid
        assert nicVO.ip == "10.0.2.60"

        UsedIpVO usedIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.vmNicUuid, vmNic.uuid)
                .eq(UsedIpVO_.ip, "10.0.2.60")
                .find()
        assert usedIp != null
        assert usedIp.ipRangeUuid == null : "ipRangeUuid should be null for outside-range IP"
    }

    /**
     * Public/range/no-DHCP: outside-range IP should NOT appear in DHCP messages on reboot.
     */
    void testDhcpSkip_pubRangeNoDhcp() {
        VmInstanceInventory vm = queryVmInstance { conditions = ["name=vm-pub-range-noDhcp-set"] }[0]

        stopVmInstance { uuid = vm.uuid }

        boolean dhcpApplied = false
        env.afterSimulator(FlatDhcpBackend.APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            FlatDhcpBackend.ApplyDhcpCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.ApplyDhcpCmd.class)
            for (def dhcp : cmd.dhcp) {
                if (dhcp.ip == "10.0.2.50") { dhcpApplied = true }
            }
            return rsp
        }
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            FlatDhcpBackend.BatchApplyDhcpCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            for (def dhcpInfo : cmd.dhcpInfos) {
                for (def dhcp : dhcpInfo.dhcp) {
                    if (dhcp.ip == "10.0.2.50") { dhcpApplied = true }
                }
            }
            return rsp
        }

        startVmInstance { uuid = vm.uuid }

        assert !dhcpApplied : "DHCP should NOT include outside-range IP 10.0.2.50"
    }

    // ================================================================
    //  Part 2: Global config ON — Public: has IP range, has DHCP
    // ================================================================

    /**
     * Public/range/DHCP: outside-range IP should succeed; in-range IP also works.
     */
    void testSetStaticIp_pubRangeDhcp() {
        L3NetworkInventory l3 = env.inventoryByName("pubL3_range_dhcp")

        VmInstanceInventory vm = createVmOnL3("vm-pub-range-dhcp-set", l3.uuid)

        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3.uuid
            ip = "10.0.3.50"
            netmask = "255.255.255.0"
            gateway = "10.0.3.1"
        }

        UsedIpVO usedIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.vmNicUuid, vm.vmNics[0].uuid)
                .eq(UsedIpVO_.ip, "10.0.3.50")
                .find()
        assert usedIp != null
        assert usedIp.ipRangeUuid == null : "ipRangeUuid should be null for outside-range IP"
        assert usedIp.netmask == "255.255.255.0"
        assert usedIp.gateway == "10.0.3.1"

        VmNicVO nicVO = dbFindByUuid(vm.vmNics[0].uuid, VmNicVO.class)
        assert nicVO.ip == "10.0.3.50"
    }

    /**
     * Public/range/DHCP: changeVmNicNetwork with outside-range IP should succeed.
     */
    void testChangeNicNetwork_pubRangeDhcp() {
        L3NetworkInventory l3 = env.inventoryByName("pubL3_range_dhcp")
        L3NetworkInventory srcL3 = env.inventoryByName("flatL3_dest")

        VmInstanceInventory vm = createVmOnL3("vm-pub-range-dhcp-change", srcL3.uuid)
        VmNicInventory vmNic = vm.vmNics[0]

        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = l3.uuid
            systemTags = [
                    String.format("staticIp::%s::10.0.3.60", l3.uuid),
                    String.format("ipv4Netmask::%s::255.255.255.0", l3.uuid),
                    String.format("ipv4Gateway::%s::10.0.3.1", l3.uuid)
            ]
        }

        VmNicVO nicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        assert nicVO.l3NetworkUuid == l3.uuid
        assert nicVO.ip == "10.0.3.60"

        UsedIpVO usedIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.vmNicUuid, vmNic.uuid)
                .eq(UsedIpVO_.ip, "10.0.3.60")
                .find()
        assert usedIp != null
        assert usedIp.ipRangeUuid == null : "ipRangeUuid should be null for outside-range IP"
    }

    /**
     * Public/range/DHCP: outside-range IP should NOT appear in DHCP messages on reboot,
     * even though DHCP service is enabled on this L3.
     */
    void testDhcpSkip_pubRangeDhcp() {
        VmInstanceInventory vm = queryVmInstance { conditions = ["name=vm-pub-range-dhcp-set"] }[0]

        stopVmInstance { uuid = vm.uuid }

        boolean dhcpAppliedForOutsideIp = false
        boolean dhcpTriggered = false
        env.afterSimulator(FlatDhcpBackend.APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            dhcpTriggered = true
            FlatDhcpBackend.ApplyDhcpCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.ApplyDhcpCmd.class)
            for (def dhcp : cmd.dhcp) {
                if (dhcp.ip == "10.0.3.50") { dhcpAppliedForOutsideIp = true }
            }
            return rsp
        }
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            dhcpTriggered = true
            FlatDhcpBackend.BatchApplyDhcpCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            for (def dhcpInfo : cmd.dhcpInfos) {
                for (def dhcp : dhcpInfo.dhcp) {
                    if (dhcp.ip == "10.0.3.50") { dhcpAppliedForOutsideIp = true }
                }
            }
            return rsp
        }

        startVmInstance { uuid = vm.uuid }

        assert !dhcpTriggered : "expected DHCP backend to run on a DHCP-enabled public L3"
        assert !dhcpAppliedForOutsideIp : "DHCP should NOT include outside-range IP 10.0.3.50 even on DHCP-enabled public L3"
    }
}

