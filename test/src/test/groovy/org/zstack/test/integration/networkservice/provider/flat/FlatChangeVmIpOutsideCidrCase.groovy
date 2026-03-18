package org.zstack.test.integration.networkservice.provider.flat

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants

/**
 * Test IP outside CIDR behavior for flat networks.
 * Outside-range IPs are always allowed (no global config needed).
 *
 * Flat network combinations (3 combos):
 *   1. flatL3_noRange_noDhcp  — no IP range, no DHCP  (enableIPAM=false, enableIpAddressAllocation()=false)
 *   2. flatL3_range_noDhcp    — has IP range, no DHCP  (enableIPAM=true,  enableIpAddressAllocation()=false)
 *   3. flatL3_range_dhcp      — has IP range, has DHCP  (enableIPAM=true,  enableIpAddressAllocation()=true)
 *
 * pubL3_range_dhcp is included only as VIP network for EIP tests.
 *
 * Each scenario tests: setVmStaticIp, changeVmNicNetwork, DHCP skip, EIP rejection.
 * Additional: orphan IP backfill when adding IP range.
 *
 * Netmask/gateway auto-resolve tests (Case A–D):
 *   Uses flatL3_range_noDhcp (CIDR: 192.168.100.0/24) as destination.
 *   Tests the unified resolveIpv4NetmaskAndGateway logic via changeVmNicNetwork and setVmStaticIp.
 *   Case C (netmask mismatch) requires multi-NIC VMs via flatL3_second (no IPAM).
 *   Case D-3 (existing IP reuse) is unique to setVmStaticIp.
 */
class FlatChangeVmIpOutsideCidrCase extends SubCase {

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
                    attachL2Network("l2-flat-noRange-noDhcp")
                    attachL2Network("l2-flat-range-noDhcp")
                    attachL2Network("l2-flat-range-dhcp")
                    attachL2Network("l2-pub-range-dhcp")
                    attachL2Network("l2-backfill")
                    attachL2Network("l2-dest")
                    attachL2Network("l2-second")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                // ========== Flat: no IP range, no DHCP ==========
                l2NoVlanNetwork {
                    name = "l2-flat-noRange-noDhcp"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "flatL3_noRange_noDhcp"
                        enableIPAM = false

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }
                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }
                        // No IP range, no DHCP
                    }
                }

                // ========== Flat: has IP range, no DHCP ==========
                l2NoVlanNetwork {
                    name = "l2-flat-range-noDhcp"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "flatL3_range_noDhcp"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }
                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.200"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                // ========== Flat: has IP range, has DHCP ==========
                l2NoVlanNetwork {
                    name = "l2-flat-range-dhcp"
                    physicalInterface = "eth2"

                    l3Network {
                        name = "flatL3_range_dhcp"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(),
                                     UserdataConstant.USERDATA_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }
                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.200.10"
                            endIp = "192.168.200.200"
                            netmask = "255.255.255.0"
                            gateway = "192.168.200.1"
                        }
                    }
                }

                // ========== Public: has IP range, has DHCP (VIP network for EIP tests only) ==========
                l2NoVlanNetwork {
                    name = "l2-pub-range-dhcp"
                    physicalInterface = "eth4"

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

                // ========== Dedicated L2/L3 for orphan IP backfill test ==========
                l2NoVlanNetwork {
                    name = "l2-backfill"
                    physicalInterface = "eth5"

                    l3Network {
                        name = "flatL3_backfill"
                        enableIPAM = false

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }
                        // No IP range initially, no DHCP
                    }
                }

                // ========== Destination L3 for changeVmNicNetwork tests ==========
                l2NoVlanNetwork {
                    name = "l2-dest"
                    physicalInterface = "eth6"

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

                // ========== Second L3: no IPAM, used as extra NIC for multi-NIC resolve tests ==========
                l2NoVlanNetwork {
                    name = "l2-second"
                    physicalInterface = "eth7"

                    l3Network {
                        name = "flatL3_second"
                        enableIPAM = false

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }
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

            // --- Flat: no IP range, no DHCP ---
            testSetStaticIp_flatNoRangeNoDhcp()
            testChangeNicNetwork_flatNoRangeNoDhcp()
            testDhcpSkip_flatNoRangeNoDhcp()
            testEipReject_flatNoRangeNoDhcp()

            // --- Flat: has IP range, no DHCP ---
            testSetStaticIp_flatRangeNoDhcp()
            testChangeNicNetwork_flatRangeNoDhcp()
            testDhcpSkip_flatRangeNoDhcp()
            testEipReject_flatRangeNoDhcp()

            // --- Flat: has IP range, has DHCP ---
            testSetStaticIp_flatRangeDhcp()
            testChangeNicNetwork_flatRangeDhcp()
            testDhcpSkip_flatRangeDhcp()
            testEipReject_flatRangeDhcp()

            // --- NIC DNS priority ---
            testNicDnsPreservedWhenApiOmitsDns_flatRangeDhcp()
            testNicDnsRemovedWhenEmptyDnsList_flatRangeDhcp()


            // ==========================================
            // Orphan IP backfill
            // ==========================================
            testOrphanIpBackfillOnAddIpRange()

            // ==========================================
            // Netmask/gateway auto-resolve (Case A–D)
            // Uses flatL3_range_noDhcp (CIDR 192.168.100.0/24)
            // ==========================================
            testResolve_A1_bothProvided_gatewayInCidr_success()
            testResolve_A2_bothProvided_gatewayNotInCidr_error()
            testResolve_B1_gatewayAndIpBothInCidr_success()
            testResolve_B2_ipInCidrButGatewayNotInCidr_error()
            testResolve_B3_ipNotInAnyCidr_error()
            testResolve_C1_netmaskMatchesCidr_success()
            testResolve_C4_netmaskMismatch_nonDefaultNonSole_success()
            testResolve_D1_ipInCidr_success()
            testResolve_D2_ipOutsideCidr_error()
            testResolve_D3_setStaticIp_existingIpReuse_success()
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

    VmInstanceInventory createVmOnL3(String vmName, List<String> l3Uuids, String defaultL3Uuid = null) {
        return createVmInstance {
            name = vmName
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = l3Uuids
            if (defaultL3Uuid != null) {
                delegate.defaultL3NetworkUuid = defaultL3Uuid
            }
        }
    }

    // ================================================================
    //  Flat: no IP range, no DHCP
    // ================================================================

    /**
     * Flat/no-range/no-DHCP: setVmStaticIp with outside-range IP should succeed.
     * Must provide netmask/gateway explicitly (no IP range to default from).
     */
    void testSetStaticIp_flatNoRangeNoDhcp() {
        L3NetworkInventory l3 = env.inventoryByName("flatL3_noRange_noDhcp")

        VmInstanceInventory vm = createVmOnL3("vm-flat-noRange-noDhcp-set", l3.uuid)

        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3.uuid
            ip = "172.16.0.50"
            netmask = "255.255.0.0"
            gateway = "172.16.0.1"
        }

        // Verify UsedIpVO
        UsedIpVO usedIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.vmNicUuid, vm.vmNics[0].uuid)
                .eq(UsedIpVO_.ip, "172.16.0.50")
                .find()
        assert usedIp != null
        assert usedIp.ipRangeUuid == null : "ipRangeUuid should be null for outside-range IP"
        assert usedIp.netmask == "255.255.0.0"
        assert usedIp.gateway == "172.16.0.1"

        // Verify VmNicVO
        VmNicVO nicVO = dbFindByUuid(vm.vmNics[0].uuid, VmNicVO.class)
        assert nicVO.ip == "172.16.0.50"
        assert nicVO.netmask == "255.255.0.0"
        assert nicVO.gateway == "172.16.0.1"
    }

    /**
     * Flat/no-range/no-DHCP: changeVmNicNetwork with outside-range IP should succeed.
     */
    void testChangeNicNetwork_flatNoRangeNoDhcp() {
        L3NetworkInventory l3 = env.inventoryByName("flatL3_noRange_noDhcp")
        L3NetworkInventory srcL3 = env.inventoryByName("flatL3_dest")

        VmInstanceInventory vm = createVmOnL3("vm-flat-noRange-noDhcp-change", srcL3.uuid)
        VmNicInventory vmNic = vm.vmNics[0]

        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = l3.uuid
            systemTags = [
                    String.format("staticIp::%s::172.16.0.60", l3.uuid),
                    String.format("ipv4Netmask::%s::255.255.0.0", l3.uuid),
                    String.format("ipv4Gateway::%s::172.16.0.1", l3.uuid)
            ]
        }

        VmNicVO nicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        assert nicVO.l3NetworkUuid == l3.uuid
        assert nicVO.ip == "172.16.0.60"

        UsedIpVO usedIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.vmNicUuid, vmNic.uuid)
                .eq(UsedIpVO_.ip, "172.16.0.60")
                .find()
        assert usedIp != null
        assert usedIp.ipRangeUuid == null : "ipRangeUuid should be null for outside-range IP"
    }

    /**
     * Flat/no-range/no-DHCP: outside-range IP should NOT appear in DHCP messages on reboot.
     */
    void testDhcpSkip_flatNoRangeNoDhcp() {
        VmInstanceInventory vm = queryVmInstance { conditions = ["name=vm-flat-noRange-noDhcp-set"] }[0]

        stopVmInstance { uuid = vm.uuid }

        boolean dhcpApplied = false
        env.afterSimulator(FlatDhcpBackend.APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            FlatDhcpBackend.ApplyDhcpCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.ApplyDhcpCmd.class)
            for (def dhcp : cmd.dhcp) {
                if (dhcp.ip == "172.16.0.50") { dhcpApplied = true }
            }
            return rsp
        }
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            FlatDhcpBackend.BatchApplyDhcpCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            for (def dhcpInfo : cmd.dhcpInfos) {
                for (def dhcp : dhcpInfo.dhcp) {
                    if (dhcp.ip == "172.16.0.50") { dhcpApplied = true }
                }
            }
            return rsp
        }

        startVmInstance { uuid = vm.uuid }

        assert !dhcpApplied : "DHCP should NOT include outside-range IP 172.16.0.50 on no-DHCP L3"
    }


    /**
     * Flat/no-range/no-DHCP: EIP should reject binding to NIC with outside-range IP.
     */
    void testEipReject_flatNoRangeNoDhcp() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3_range_dhcp")
        VmInstanceInventory vm = queryVmInstance { conditions = ["name=vm-flat-noRange-noDhcp-set"] }[0]
        L3NetworkInventory l3 = env.inventoryByName("flatL3_noRange_noDhcp")
        VmNicInventory nic = vm.vmNics.find { it.l3NetworkUuid == l3.uuid }
        assert nic != null

        VipInventory vip = createVip {
            name = "vip-flat-noRange-noDhcp"
            l3NetworkUuid = pubL3.uuid
        }
        EipInventory eip = createEip {
            name = "eip-flat-noRange-noDhcp"
            vipUuid = vip.uuid
        }

        expect(AssertionError.class) {
            attachEip {
                eipUuid = eip.uuid
                vmNicUuid = nic.uuid
            }
        }
    }

    // ================================================================
    //  Part 2: Global config ON — Flat: has IP range, no DHCP
    // ================================================================

    /**
     * Flat/range/no-DHCP: outside-range IP should succeed; in-range IP also works.
     */
    void testSetStaticIp_flatRangeNoDhcp() {
        L3NetworkInventory l3 = env.inventoryByName("flatL3_range_noDhcp")

        VmInstanceInventory vm = createVmOnL3("vm-flat-range-noDhcp-set", l3.uuid)

        // Outside-range IP should succeed
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3.uuid
            ip = "10.0.0.50"
            netmask = "255.255.255.0"
            gateway = "10.0.0.1"
        }

        UsedIpVO usedIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.vmNicUuid, vm.vmNics[0].uuid)
                .eq(UsedIpVO_.ip, "10.0.0.50")
                .find()
        assert usedIp != null
        assert usedIp.ipRangeUuid == null : "ipRangeUuid should be null for outside-range IP"
        assert usedIp.netmask == "255.255.255.0"
        assert usedIp.gateway == "10.0.0.1"

        VmNicVO nicVO = dbFindByUuid(vm.vmNics[0].uuid, VmNicVO.class)
        assert nicVO.ip == "10.0.0.50"
    }

    /**
     * Flat/range/no-DHCP: changeVmNicNetwork with outside-range IP should succeed.
     */
    void testChangeNicNetwork_flatRangeNoDhcp() {
        L3NetworkInventory l3 = env.inventoryByName("flatL3_range_noDhcp")
        L3NetworkInventory srcL3 = env.inventoryByName("flatL3_dest")

        VmInstanceInventory vm = createVmOnL3("vm-flat-range-noDhcp-change", srcL3.uuid)
        VmNicInventory vmNic = vm.vmNics[0]

        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = l3.uuid
            systemTags = [
                    String.format("staticIp::%s::10.0.0.60", l3.uuid),
                    String.format("ipv4Netmask::%s::255.255.255.0", l3.uuid),
                    String.format("ipv4Gateway::%s::10.0.0.1", l3.uuid)
            ]
        }

        VmNicVO nicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        assert nicVO.l3NetworkUuid == l3.uuid
        assert nicVO.ip == "10.0.0.60"

        UsedIpVO usedIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.vmNicUuid, vmNic.uuid)
                .eq(UsedIpVO_.ip, "10.0.0.60")
                .find()
        assert usedIp != null
        assert usedIp.ipRangeUuid == null : "ipRangeUuid should be null for outside-range IP"
    }

    /**
     * Flat/range/no-DHCP: outside-range IP should NOT appear in DHCP messages on reboot.
     */
    void testDhcpSkip_flatRangeNoDhcp() {
        VmInstanceInventory vm = queryVmInstance { conditions = ["name=vm-flat-range-noDhcp-set"] }[0]

        stopVmInstance { uuid = vm.uuid }

        boolean dhcpApplied = false
        env.afterSimulator(FlatDhcpBackend.APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            FlatDhcpBackend.ApplyDhcpCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.ApplyDhcpCmd.class)
            for (def dhcp : cmd.dhcp) {
                if (dhcp.ip == "10.0.0.50") { dhcpApplied = true }
            }
            return rsp
        }
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            FlatDhcpBackend.BatchApplyDhcpCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            for (def dhcpInfo : cmd.dhcpInfos) {
                for (def dhcp : dhcpInfo.dhcp) {
                    if (dhcp.ip == "10.0.0.50") { dhcpApplied = true }
                }
            }
            return rsp
        }

        startVmInstance { uuid = vm.uuid }

        assert !dhcpApplied : "DHCP should NOT include outside-range IP 10.0.0.50 on no-DHCP L3"
    }


    /**
     * Flat/range/no-DHCP: EIP should reject binding to NIC with outside-range IP.
     */
    void testEipReject_flatRangeNoDhcp() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3_range_dhcp")
        VmInstanceInventory vm = queryVmInstance { conditions = ["name=vm-flat-range-noDhcp-set"] }[0]
        L3NetworkInventory l3 = env.inventoryByName("flatL3_range_noDhcp")
        VmNicInventory nic = vm.vmNics.find { it.l3NetworkUuid == l3.uuid }
        assert nic != null

        VipInventory vip = createVip {
            name = "vip-flat-range-noDhcp"
            l3NetworkUuid = pubL3.uuid
        }
        EipInventory eip = createEip {
            name = "eip-flat-range-noDhcp"
            vipUuid = vip.uuid
        }

        expect(AssertionError.class) {
            attachEip {
                eipUuid = eip.uuid
                vmNicUuid = nic.uuid
            }
        }
    }

    // ================================================================
    //  Part 2: Global config ON — Flat: has IP range, has DHCP
    // ================================================================

    /**
     * Flat/range/DHCP: outside-range IP should succeed with global config ON;
     * in-range IP also works normally.
     */
    void testSetStaticIp_flatRangeDhcp() {
        L3NetworkInventory l3 = env.inventoryByName("flatL3_range_dhcp")

        VmInstanceInventory vm = createVmOnL3("vm-flat-range-dhcp-set", l3.uuid)

        // Outside-range IP should succeed with global config ON
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3.uuid
            ip = "10.0.1.50"
            netmask = "255.255.255.0"
            gateway = "10.0.1.1"
        }

        UsedIpVO usedIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.vmNicUuid, vm.vmNics[0].uuid)
                .eq(UsedIpVO_.ip, "10.0.1.50")
                .find()
        assert usedIp != null
        assert usedIp.ipRangeUuid == null : "ipRangeUuid should be null for outside-range IP"
        assert usedIp.netmask == "255.255.255.0"
        assert usedIp.gateway == "10.0.1.1"

        VmNicVO nicVO = dbFindByUuid(vm.vmNics[0].uuid, VmNicVO.class)
        assert nicVO.ip == "10.0.1.50"
    }

    /**
     * Flat/range/DHCP: changeVmNicNetwork with outside-range IP should succeed.
     */
    void testChangeNicNetwork_flatRangeDhcp() {
        L3NetworkInventory l3 = env.inventoryByName("flatL3_range_dhcp")
        L3NetworkInventory srcL3 = env.inventoryByName("flatL3_dest")

        VmInstanceInventory vm = createVmOnL3("vm-flat-range-dhcp-change", srcL3.uuid)
        VmNicInventory vmNic = vm.vmNics[0]

        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = l3.uuid
            systemTags = [
                    String.format("staticIp::%s::10.0.1.60", l3.uuid),
                    String.format("ipv4Netmask::%s::255.255.255.0", l3.uuid),
                    String.format("ipv4Gateway::%s::10.0.1.1", l3.uuid)
            ]
        }

        VmNicVO nicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        assert nicVO.l3NetworkUuid == l3.uuid
        assert nicVO.ip == "10.0.1.60"

        UsedIpVO usedIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.vmNicUuid, vmNic.uuid)
                .eq(UsedIpVO_.ip, "10.0.1.60")
                .find()
        assert usedIp != null
        assert usedIp.ipRangeUuid == null : "ipRangeUuid should be null for outside-range IP"
    }

    /**
     * Flat/range/DHCP: outside-range IP should NOT appear in DHCP messages on reboot,
     * even though DHCP service is enabled on this L3.
     */
    void testDhcpSkip_flatRangeDhcp() {
        VmInstanceInventory vm = queryVmInstance { conditions = ["name=vm-flat-range-dhcp-set"] }[0]

        stopVmInstance { uuid = vm.uuid }

        boolean dhcpAppliedForOutsideIp = false
        env.afterSimulator(FlatDhcpBackend.APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            FlatDhcpBackend.ApplyDhcpCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.ApplyDhcpCmd.class)
            for (def dhcp : cmd.dhcp) {
                if (dhcp.ip == "10.0.1.50") { dhcpAppliedForOutsideIp = true }
            }
            return rsp
        }
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            FlatDhcpBackend.BatchApplyDhcpCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            for (def dhcpInfo : cmd.dhcpInfos) {
                for (def dhcp : dhcpInfo.dhcp) {
                    if (dhcp.ip == "10.0.1.50") { dhcpAppliedForOutsideIp = true }
                }
            }
            return rsp
        }

        startVmInstance { uuid = vm.uuid }

        assert !dhcpAppliedForOutsideIp : "DHCP should NOT include outside-range IP 10.0.1.50 even on DHCP-enabled L3"
    }


    /**
     * Flat/range/DHCP: EIP should reject binding to NIC with outside-range IP.
     */
    void testEipReject_flatRangeDhcp() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3_range_dhcp")
        VmInstanceInventory vm = queryVmInstance { conditions = ["name=vm-flat-range-dhcp-set"] }[0]
        L3NetworkInventory l3 = env.inventoryByName("flatL3_range_dhcp")
        VmNicInventory nic = vm.vmNics.find { it.l3NetworkUuid == l3.uuid }
        assert nic != null

        VipInventory vip = createVip {
            name = "vip-flat-range-dhcp"
            l3NetworkUuid = pubL3.uuid
        }
        EipInventory eip = createEip {
            name = "eip-flat-range-dhcp"
            vipUuid = vip.uuid
        }

        expect(AssertionError.class) {
            attachEip {
                eipUuid = eip.uuid
                vmNicUuid = nic.uuid
            }
        }
    }

    // ================================================================
    //  NIC DNS priority: NIC DNS > L3 DNS
    // ================================================================

    /**
     * When setVmStaticIp is called with dnsAddresses, NIC-level DNS is used in DHCP.
     * When setVmStaticIp is called again WITHOUT dnsAddresses (null), old NIC DNS is preserved.
     */
    void testNicDnsPreservedWhenApiOmitsDns_flatRangeDhcp() {
        L3NetworkInventory l3 = env.inventoryByName("flatL3_range_dhcp")

        // Add L3-level DNS
        addDnsToL3Network {
            l3NetworkUuid = l3.uuid
            dns = "8.8.8.8"
        }

        // Create a VM on the DHCP-enabled L3
        VmInstanceInventory vm = createVmOnL3("vm-nic-dns-preserve", l3.uuid)

        // Set NIC-level DNS via setVmStaticIp
        List<FreeIpInventory> freeIps = getFreeIp {
            l3NetworkUuid = l3.uuid
            ipVersion = IPv6Constants.IPv4
            limit = 1
        } as List<FreeIpInventory>
        String ip1 = freeIps.get(0).getIp()

        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3.uuid
            ip = ip1
            dnsAddresses = ["1.1.1.1", "1.0.0.1"]
        }

        // Intercept DHCP apply and reboot to trigger DHCP re-apply
        FlatDhcpBackend.BatchApplyDhcpCmd cmd1 = null
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            cmd1 = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            return rsp
        }

        rebootVmInstance { uuid = vm.uuid }

        assert cmd1 != null
        def dhcp1 = cmd1.dhcpInfos.collectMany { it.dhcp }.find { it.ip == ip1 }
        assert dhcp1 != null : "DHCP entry for IP ${ip1} should exist"
        assert dhcp1.dns == ["1.1.1.1", "1.0.0.1"] : "NIC DNS should override L3 DNS, got: ${dhcp1.dns}"

        // Now call setVmStaticIp again WITHOUT dnsAddresses (only change IP)
        // dnsAddresses is null => old NIC DNS should be preserved
        List<FreeIpInventory> freeIps2 = getFreeIp {
            l3NetworkUuid = l3.uuid
            ipVersion = IPv6Constants.IPv4
            limit = 1
        } as List<FreeIpInventory>
        String ip2 = freeIps2.get(0).getIp()

        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3.uuid
            ip = ip2
            // dnsAddresses is NOT set => null in the message
        }

        FlatDhcpBackend.BatchApplyDhcpCmd cmd2 = null
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            cmd2 = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            return rsp
        }

        rebootVmInstance { uuid = vm.uuid }

        assert cmd2 != null
        def dhcp2 = cmd2.dhcpInfos.collectMany { it.dhcp }.find { it.ip == ip2 }
        assert dhcp2 != null : "DHCP entry for IP ${ip2} should exist"
        assert dhcp2.dns == ["1.1.1.1", "1.0.0.1"] : "NIC DNS should be preserved when API omits dnsAddresses, got: ${dhcp2.dns}"
    }

    /**
     * When setVmStaticIp is called with an empty dnsAddresses list ([]),
     * NIC DNS is removed and DHCP falls back to L3 network DNS.
     */
    void testNicDnsRemovedWhenEmptyDnsList_flatRangeDhcp() {
        L3NetworkInventory l3 = env.inventoryByName("flatL3_range_dhcp")

        // The previous test already added "8.8.8.8" as L3 DNS and created "vm-nic-dns-preserve"
        // with NIC DNS ["1.1.1.1", "1.0.0.1"]. Reuse that VM.
        VmInstanceInventory vm = queryVmInstance { conditions = ["name=vm-nic-dns-preserve"] }[0]

        // Must use a different IP because setVmStaticIp rejects the same IP
        List<FreeIpInventory> freeIps = getFreeIp {
            l3NetworkUuid = l3.uuid
            ipVersion = IPv6Constants.IPv4
            limit = 1
        } as List<FreeIpInventory>
        String newIp = freeIps.get(0).getIp()

        // Call setVmStaticIp with a new IP and explicit empty dnsAddresses => removes NIC DNS
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3.uuid
            ip = newIp
            dnsAddresses = []
        }

        // Intercept DHCP apply and reboot
        FlatDhcpBackend.BatchApplyDhcpCmd cmd = null
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            return rsp
        }

        rebootVmInstance { uuid = vm.uuid }

        assert cmd != null
        def dhcp = cmd.dhcpInfos.collectMany { it.dhcp }.find { it.ip == newIp }
        assert dhcp != null : "DHCP entry for IP ${newIp} should exist"
        assert dhcp.dns.contains("8.8.8.8") : "After removing NIC DNS, DHCP should fall back to L3 DNS (8.8.8.8), got: ${dhcp.dns}"
        assert !dhcp.dns.contains("1.1.1.1") : "NIC DNS 1.1.1.1 should no longer be present after clearing, got: ${dhcp.dns}"
        assert !dhcp.dns.contains("1.0.0.1") : "NIC DNS 1.0.0.1 should no longer be present after clearing, got: ${dhcp.dns}"
    }


    // ================================================================
    //  Part 3: Orphan IP backfill (global config ON)
    // ================================================================

    /**
     * Create orphan IPs on flatL3_backfill (no IP range),
     * then add an IP range covering the orphan IPs.
     * Verify ipRangeUuid is backfilled and capacity increases.
     */
    void testOrphanIpBackfillOnAddIpRange() {
        L3NetworkInventory backfillL3 = env.inventoryByName("flatL3_backfill")

        // Step 1: create VMs and assign outside-range IPs
        VmInstanceInventory orphanVm1 = createVmOnL3("vm-backfill-orphan-1", backfillL3.uuid)
        setVmStaticIp {
            vmInstanceUuid = orphanVm1.uuid
            l3NetworkUuid = backfillL3.uuid
            ip = "172.16.0.80"
            netmask = "255.255.0.0"
            gateway = "172.16.0.1"
        }

        VmInstanceInventory orphanVm2 = createVmOnL3("vm-backfill-orphan-2", backfillL3.uuid)
        setVmStaticIp {
            vmInstanceUuid = orphanVm2.uuid
            l3NetworkUuid = backfillL3.uuid
            ip = "172.16.0.90"
            netmask = "255.255.0.0"
            gateway = "172.16.0.1"
        }

        // Confirm orphan IPs exist
        long outsideCount = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, backfillL3.uuid)
                .isNull(UsedIpVO_.ipRangeUuid)
                .count()
        assert outsideCount == 2 : "There should be 2 orphan IPs on flatL3_backfill"

        // Step 2: record capacity before backfill
        GetIpAddressCapacityResult beforeBackfill = getIpAddressCapacity {
            l3NetworkUuids = [backfillL3.uuid]
        }

        // Step 3: add IP range covering the orphan IPs (172.16.0.80, 172.16.0.90)
        IpRangeInventory ipRange = addIpRange {
            delegate.name = "backfill-ip-range"
            delegate.l3NetworkUuid = backfillL3.uuid
            delegate.startIp = "172.16.0.2"
            delegate.endIp = "172.16.0.253"
            delegate.gateway = "172.16.0.1"
            delegate.netmask = "255.255.0.0"
        }

        // Step 4: verify orphan IPs now have ipRangeUuid backfilled
        long backfilledCount = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, backfillL3.uuid)
                .eq(UsedIpVO_.ipRangeUuid, ipRange.uuid)
                .count()
        assert backfilledCount == outsideCount :
                "all ${outsideCount} orphan IPs should now be associated with the new IP range"

        // No more orphan IPs
        long remainingOrphanCount = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, backfillL3.uuid)
                .isNull(UsedIpVO_.ipRangeUuid)
                .count()
        assert remainingOrphanCount == 0 : "all orphan IPs should now have ipRangeUuid"

        // Step 5: capacity should now include the backfilled IPs
        GetIpAddressCapacityResult afterBackfill = getIpAddressCapacity {
            l3NetworkUuids = [backfillL3.uuid]
        }
        assert afterBackfill.totalCapacity > beforeBackfill.totalCapacity :
                "totalCapacity should increase after adding new IP range"
        assert afterBackfill.usedIpAddressNumber == beforeBackfill.usedIpAddressNumber + outsideCount :
                "usedIpAddressNumber should increase by ${outsideCount} after backfill"
    }

    // ================================================================
    //  Netmask/gateway auto-resolve tests (Case A–D)
    //  Uses flatL3_range_noDhcp (CIDR: 192.168.100.0/24, gw: 192.168.100.1)
    //  Source L3: flatL3_dest (no IPAM)
    //  Second L3: flatL3_second (no IPAM, for multi-NIC tests)
    // ================================================================

    /**
     * a-1: both netmask+gateway provided, gateway in CIDR(ip/netmask).
     * Expected: success, use user input as-is.
     */
    void testResolve_A1_bothProvided_gatewayInCidr_success() {
        L3NetworkInventory destL3 = env.inventoryByName("flatL3_range_noDhcp")
        L3NetworkInventory srcL3 = env.inventoryByName("flatL3_dest")

        VmInstanceInventory vm = createVmOnL3("vm-resolve-a1", srcL3.uuid)
        VmNicInventory vmNic = vm.vmNics[0]

        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = destL3.uuid
            systemTags = [
                    String.format("staticIp::%s::192.168.100.40", destL3.uuid),
                    String.format("ipv4Netmask::%s::255.255.255.0", destL3.uuid),
                    String.format("ipv4Gateway::%s::192.168.100.1", destL3.uuid)
            ]
        }

        VmNicVO nicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        assert nicVO.l3NetworkUuid == destL3.uuid
        assert nicVO.ip == "192.168.100.40"
        assert nicVO.netmask == "255.255.255.0"
        assert nicVO.gateway == "192.168.100.1"

        UsedIpVO usedIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.vmNicUuid, vmNic.uuid)
                .eq(UsedIpVO_.ip, "192.168.100.40")
                .find()
        assert usedIp != null
        assert usedIp.netmask == "255.255.255.0"
        assert usedIp.gateway == "192.168.100.1"
    }

    /**
     * a-2: both netmask+gateway provided, gateway NOT in CIDR(ip/netmask).
     * Expected: error.
     */
    void testResolve_A2_bothProvided_gatewayNotInCidr_error() {
        L3NetworkInventory destL3 = env.inventoryByName("flatL3_range_noDhcp")
        L3NetworkInventory srcL3 = env.inventoryByName("flatL3_dest")

        VmInstanceInventory vm = createVmOnL3("vm-resolve-a2", srcL3.uuid)
        VmNicInventory vmNic = vm.vmNics[0]

        expect(AssertionError.class) {
            changeVmNicNetwork {
                vmNicUuid = vmNic.uuid
                destL3NetworkUuid = destL3.uuid
                systemTags = [
                        String.format("staticIp::%s::192.168.100.41", destL3.uuid),
                        String.format("ipv4Netmask::%s::255.255.255.0", destL3.uuid),
                        String.format("ipv4Gateway::%s::10.0.0.1", destL3.uuid)
                ]
            }
        }
    }

    /**
     * b-1: gateway provided (no netmask), IP and gateway both in L3 CIDR.
     * Expected: success, netmask from CIDR.
     */
    void testResolve_B1_gatewayAndIpBothInCidr_success() {
        L3NetworkInventory destL3 = env.inventoryByName("flatL3_range_noDhcp")
        L3NetworkInventory srcL3 = env.inventoryByName("flatL3_dest")

        VmInstanceInventory vm = createVmOnL3("vm-resolve-b1", srcL3.uuid)
        VmNicInventory vmNic = vm.vmNics[0]

        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = destL3.uuid
            systemTags = [
                    String.format("staticIp::%s::192.168.100.50", destL3.uuid),
                    String.format("ipv4Gateway::%s::192.168.100.1", destL3.uuid)
            ]
        }

        VmNicVO nicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        assert nicVO.ip == "192.168.100.50"
        assert nicVO.netmask == "255.255.255.0" : "netmask should be inferred from L3 CIDR"
        assert nicVO.gateway == "192.168.100.1"
    }

    /**
     * b-2: gateway provided (no netmask), IP in CIDR but gateway NOT in CIDR.
     * Expected: error.
     */
    void testResolve_B2_ipInCidrButGatewayNotInCidr_error() {
        L3NetworkInventory destL3 = env.inventoryByName("flatL3_range_noDhcp")
        L3NetworkInventory srcL3 = env.inventoryByName("flatL3_dest")

        VmInstanceInventory vm = createVmOnL3("vm-resolve-b2", srcL3.uuid)
        VmNicInventory vmNic = vm.vmNics[0]

        expect(AssertionError.class) {
            changeVmNicNetwork {
                vmNicUuid = vmNic.uuid
                destL3NetworkUuid = destL3.uuid
                systemTags = [
                        String.format("staticIp::%s::192.168.100.51", destL3.uuid),
                        String.format("ipv4Gateway::%s::10.0.0.1", destL3.uuid)
                ]
            }
        }
    }

    /**
     * b-3: gateway provided (no netmask), IP NOT in any CIDR.
     * Expected: error.
     */
    void testResolve_B3_ipNotInAnyCidr_error() {
        L3NetworkInventory destL3 = env.inventoryByName("flatL3_range_noDhcp")
        L3NetworkInventory srcL3 = env.inventoryByName("flatL3_dest")

        VmInstanceInventory vm = createVmOnL3("vm-resolve-b3", srcL3.uuid)
        VmNicInventory vmNic = vm.vmNics[0]

        expect(AssertionError.class) {
            changeVmNicNetwork {
                vmNicUuid = vmNic.uuid
                destL3NetworkUuid = destL3.uuid
                systemTags = [
                        String.format("staticIp::%s::10.0.0.50", destL3.uuid),
                        String.format("ipv4Gateway::%s::10.0.0.1", destL3.uuid)
                ]
            }
        }
    }

    /**
     * c-1: netmask provided (no gateway), netmask == CIDR netmask.
     * Expected: success, gateway from CIDR.
     */
    void testResolve_C1_netmaskMatchesCidr_success() {
        L3NetworkInventory destL3 = env.inventoryByName("flatL3_range_noDhcp")
        L3NetworkInventory srcL3 = env.inventoryByName("flatL3_dest")

        VmInstanceInventory vm = createVmOnL3("vm-resolve-c1", srcL3.uuid)
        VmNicInventory vmNic = vm.vmNics[0]

        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = destL3.uuid
            systemTags = [
                    String.format("staticIp::%s::192.168.100.60", destL3.uuid),
                    String.format("ipv4Netmask::%s::255.255.255.0", destL3.uuid)
            ]
        }

        VmNicVO nicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        assert nicVO.ip == "192.168.100.60"
        assert nicVO.netmask == "255.255.255.0"
        assert nicVO.gateway == "192.168.100.1" : "gateway should be inferred from L3 CIDR"
    }

    /**
     * c-4: netmask != CIDR, non-default & non-sole.
     * VM has 2 NICs [flatL3_dest(default), flatL3_second].
     * Change flatL3_second NIC → flatL3_range_noDhcp (not default, vmNicCount=2).
     * Expected: success, netmask=user input, gateway="".
     */
    void testResolve_C4_netmaskMismatch_nonDefaultNonSole_success() {
        L3NetworkInventory destL3 = env.inventoryByName("flatL3_range_noDhcp")
        L3NetworkInventory srcL3 = env.inventoryByName("flatL3_dest")
        L3NetworkInventory secondL3 = env.inventoryByName("flatL3_second")

        VmInstanceInventory vm = createVmOnL3("vm-resolve-c4", [srcL3.uuid, secondL3.uuid], srcL3.uuid)

        int nicCount = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vm.uuid).count().intValue()
        assert nicCount == 2

        String defaultL3 = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.defaultL3NetworkUuid)
                .eq(VmInstanceVO_.uuid, vm.uuid)
                .findValue()
        assert defaultL3 == srcL3.uuid
        assert defaultL3 != destL3.uuid

        VmNicInventory nicOnSecond = vm.vmNics.find { it.l3NetworkUuid == secondL3.uuid }
        assert nicOnSecond != null

        changeVmNicNetwork {
            vmNicUuid = nicOnSecond.uuid
            destL3NetworkUuid = destL3.uuid
            systemTags = [
                    String.format("staticIp::%s::192.168.100.90", destL3.uuid),
                    String.format("ipv4Netmask::%s::255.255.0.0", destL3.uuid)
            ]
        }

        VmNicVO nicVO = dbFindByUuid(nicOnSecond.uuid, VmNicVO.class)
        assert nicVO.ip == "192.168.100.90"
        assert nicVO.netmask == "255.255.0.0" : "netmask should be user input"
        assert nicVO.gateway == "" || nicVO.gateway == null : "gateway should be empty"
    }

    /**
     * d-1: neither netmask nor gateway, IP in L3 CIDR.
     * Expected: success, netmask+gateway from CIDR.
     */
    void testResolve_D1_ipInCidr_success() {
        L3NetworkInventory destL3 = env.inventoryByName("flatL3_range_noDhcp")
        L3NetworkInventory srcL3 = env.inventoryByName("flatL3_dest")

        VmInstanceInventory vm = createVmOnL3("vm-resolve-d1", srcL3.uuid)
        VmNicInventory vmNic = vm.vmNics[0]

        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = destL3.uuid
            systemTags = [
                    String.format("staticIp::%s::192.168.100.100", destL3.uuid)
            ]
        }

        VmNicVO nicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        assert nicVO.ip == "192.168.100.100"
        assert nicVO.netmask == "255.255.255.0" : "netmask should be inferred from L3 CIDR"
        assert nicVO.gateway == "192.168.100.1" : "gateway should be inferred from L3 CIDR"
    }

    /**
     * d-2: neither netmask nor gateway, IP NOT in any CIDR.
     * Expected: error.
     */
    void testResolve_D2_ipOutsideCidr_error() {
        L3NetworkInventory destL3 = env.inventoryByName("flatL3_range_noDhcp")
        L3NetworkInventory srcL3 = env.inventoryByName("flatL3_dest")

        VmInstanceInventory vm = createVmOnL3("vm-resolve-d2", srcL3.uuid)
        VmNicInventory vmNic = vm.vmNics[0]

        expect(AssertionError.class) {
            changeVmNicNetwork {
                vmNicUuid = vmNic.uuid
                destL3NetworkUuid = destL3.uuid
                systemTags = [
                        String.format("staticIp::%s::10.0.0.100", destL3.uuid)
                ]
            }
        }
    }

    /**
     * d-3 (setVmStaticIp only): existing IP has params, new IP in old CIDR → reuse.
     * This scenario is unique to APISetVmStaticIpMsg (existing IP reuse via ExistingIpContext).
     */
    void testResolve_D3_setStaticIp_existingIpReuse_success() {
        L3NetworkInventory l3 = env.inventoryByName("flatL3_range_noDhcp")

        VmInstanceInventory vm = createVmOnL3("vm-resolve-d3", l3.uuid)

        // First set a known IP with explicit netmask/gateway
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3.uuid
            ip = "192.168.100.110"
            netmask = "255.255.255.0"
            gateway = "192.168.100.1"
        }

        // Now change to a new IP in the same CIDR, without specifying netmask/gateway
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = l3.uuid
            ip = "192.168.100.111"
        }

        UsedIpVO usedIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, l3.uuid)
                .eq(UsedIpVO_.ip, "192.168.100.111")
                .find()
        assert usedIp != null
        assert usedIp.netmask == "255.255.255.0" : "should reuse old netmask"
        assert usedIp.gateway == "192.168.100.1" : "should reuse old gateway"
    }
}
