package org.zstack.test.integration.networkservice.provider.flat

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmNicVO
import org.zstack.network.l3.L3NetworkGlobalConfig
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

/**
 * Test IP outside CIDR behavior for flat/public networks controlled by
 * L3NetworkGlobalConfig.ALLOW_IP_OUTSIDE_RANGE (default false) and L3NetworkVO.enableIPAM.
 *
 * Valid network combinations:
 *   Flat network (3 combos):
 *     1. flatL3_noRange_noDhcp  — no IP range, no DHCP  (enableIPAM=false)
 *     2. flatL3_range_noDhcp    — has IP range, no DHCP  (enableIPAM=true)
 *     3. flatL3_range_dhcp      — has IP range, has DHCP  (enableIPAM=true)
 *   Public network (2 combos):
 *     4. pubL3_range_noDhcp     — has IP range, no DHCP  (enableIPAM=true)
 *     5. pubL3_range_dhcp       — has IP range, has DHCP  (enableIPAM=true)
 *   VPC network (1 combo, not tested here):
 *     - has IP range, has DHCP  (enableIPAM=true)
 *
 * Outside-range IP rules:
 *   - enableIPAM=false: always allow outside-range IPs (no global config needed)
 *   - enableIPAM=true + global config OFF: outside-range IPs rejected
 *   - enableIPAM=true + global config ON:  outside-range IPs allowed
 *
 * Each scenario tests: setVmStaticIp, changeVmNicNetwork, DHCP, security group.
 * Flat networks with EIP service also test EIP rejection for outside-range IP.
 *
 * Additional tests:
 *   - Global config OFF: outside-range IPs rejected on enableIPAM=true networks,
 *     allowed on enableIPAM=false networks
 *   - Orphan IP backfill when adding IP range (under global config ON)
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
                    attachL2Network("l2-pub-range-noDhcp")
                    attachL2Network("l2-pub-range-dhcp")
                    attachL2Network("l2-backfill")
                    attachL2Network("l2-dest")
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

                // ========== Public: has IP range, no DHCP ==========
                l2NoVlanNetwork {
                    name = "l2-pub-range-noDhcp"
                    physicalInterface = "eth3"

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

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            // ==========================================
            // Part 1: Global config OFF — outside-range IPs rejected
            // ==========================================
            testGlobalConfigOff_SetStaticIpRejected()
            testGlobalConfigOff_ChangeNicNetworkRejected()

            // ==========================================
            // Part 2: Global config ON — outside-range IPs allowed
            // ==========================================
            turnOnGlobalConfig()

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

            // --- Public: has IP range, no DHCP ---
            testSetStaticIp_pubRangeNoDhcp()
            testChangeNicNetwork_pubRangeNoDhcp()
            testDhcpSkip_pubRangeNoDhcp()

            // --- Public: has IP range, has DHCP ---
            testSetStaticIp_pubRangeDhcp()
            testChangeNicNetwork_pubRangeDhcp()
            testDhcpSkip_pubRangeDhcp()

            // ==========================================
            // Part 3: Orphan IP backfill (global config ON)
            // ==========================================
            testOrphanIpBackfillOnAddIpRange()
        }
    }

    // ================================================================
    //  Helper: turn global config on / off
    // ================================================================

    void turnOnGlobalConfig() {
        updateGlobalConfig {
            category = L3NetworkGlobalConfig.CATEGORY
            name = L3NetworkGlobalConfig.ALLOW_IP_OUTSIDE_RANGE.name
            value = "true"
        }
    }

    void turnOffGlobalConfig() {
        updateGlobalConfig {
            category = L3NetworkGlobalConfig.CATEGORY
            name = L3NetworkGlobalConfig.ALLOW_IP_OUTSIDE_RANGE.name
            value = "false"
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
    //  Part 1: Global config OFF — reject outside-range IPs
    // ================================================================

    /**
     * Config OFF: setVmStaticIp with outside-range IP should fail on all L3 types
     * that have IP ranges. Error: ORG_ZSTACK_COMPUTE_VM_10109
     */
    void testGlobalConfigOff_SetStaticIpRejected() {
        // --- flat network with IP range + DHCP ---
        L3NetworkInventory flatL3Dhcp = env.inventoryByName("flatL3_range_dhcp")
        VmInstanceInventory vm1 = createVmOnL3("vm-configoff-set-flat-dhcp", flatL3Dhcp.uuid)

        expect(AssertionError.class) {
            setVmStaticIp {
                vmInstanceUuid = vm1.uuid
                l3NetworkUuid = flatL3Dhcp.uuid
                ip = "10.0.0.50"
            }
        }

        VmNicVO nicVO1 = dbFindByUuid(vm1.vmNics[0].uuid, VmNicVO.class)
        assert nicVO1.ip != "10.0.0.50" : "outside-range IP should not be set when config OFF"

        // --- flat network with IP range, no DHCP ---
        L3NetworkInventory flatL3NoDhcp = env.inventoryByName("flatL3_range_noDhcp")
        VmInstanceInventory vm2 = createVmOnL3("vm-configoff-set-flat-nodhcp", flatL3NoDhcp.uuid)

        expect(AssertionError.class) {
            setVmStaticIp {
                vmInstanceUuid = vm2.uuid
                l3NetworkUuid = flatL3NoDhcp.uuid
                ip = "10.0.0.51"
                netmask = "255.255.255.0"
                gateway = "10.0.0.1"
            }
        }

        // --- public network with IP range + DHCP ---
        L3NetworkInventory pubL3Dhcp = env.inventoryByName("pubL3_range_dhcp")
        VmInstanceInventory vm3 = createVmOnL3("vm-configoff-set-pub-dhcp", pubL3Dhcp.uuid)

        expect(AssertionError.class) {
            setVmStaticIp {
                vmInstanceUuid = vm3.uuid
                l3NetworkUuid = pubL3Dhcp.uuid
                ip = "10.0.0.52"
            }
        }

        // --- public network with IP range, no DHCP ---
        L3NetworkInventory pubL3NoDhcp = env.inventoryByName("pubL3_range_noDhcp")
        VmInstanceInventory vm4 = createVmOnL3("vm-configoff-set-pub-nodhcp", pubL3NoDhcp.uuid)

        expect(AssertionError.class) {
            setVmStaticIp {
                vmInstanceUuid = vm4.uuid
                l3NetworkUuid = pubL3NoDhcp.uuid
                ip = "10.0.0.53"
                netmask = "255.255.255.0"
                gateway = "10.0.0.1"
            }
        }
    }

    /**
     * Config OFF: changeVmNicNetwork with outside-range IP should fail.
     * Error: ORG_ZSTACK_COMPUTE_VM_10109
     */
    void testGlobalConfigOff_ChangeNicNetworkRejected() {
        L3NetworkInventory flatL3Dhcp = env.inventoryByName("flatL3_range_dhcp")
        L3NetworkInventory flatL3NoDhcp = env.inventoryByName("flatL3_range_noDhcp")

        VmInstanceInventory vm = createVmOnL3("vm-configoff-change", flatL3Dhcp.uuid)
        VmNicInventory vmNic = vm.vmNics[0]

        // changeVmNicNetwork to flatL3_range_noDhcp with outside-range IP
        expect(AssertionError.class) {
            changeVmNicNetwork {
                vmNicUuid = vmNic.uuid
                destL3NetworkUuid = flatL3NoDhcp.uuid
                systemTags = [
                        String.format("staticIp::%s::10.10.10.50", flatL3NoDhcp.uuid),
                        String.format("ipv4Netmask::%s::255.255.255.0", flatL3NoDhcp.uuid),
                        String.format("ipv4Gateway::%s::10.10.10.1", flatL3NoDhcp.uuid)
                ]
            }
        }

        // Verify NIC still on original L3
        VmNicVO nicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        assert nicVO.l3NetworkUuid == flatL3Dhcp.uuid : "NIC should remain on original L3"
    }

    // ================================================================
    //  Part 2: Global config ON — Flat: no IP range, no DHCP
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

        // Also verify in-range IP works
        VmInstanceInventory vm2 = createVmOnL3("vm-flat-range-noDhcp-inrange", l3.uuid)
        setVmStaticIp {
            vmInstanceUuid = vm2.uuid
            l3NetworkUuid = l3.uuid
            ip = "192.168.100.100"
        }

        UsedIpVO inRangeIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.vmNicUuid, vm2.vmNics[0].uuid)
                .eq(UsedIpVO_.ip, "192.168.100.100")
                .find()
        assert inRangeIp != null
        assert inRangeIp.ipRangeUuid != null : "ipRangeUuid should not be null for in-range IP"
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

        // Also verify in-range IP works
        VmInstanceInventory vm2 = createVmOnL3("vm-flat-range-dhcp-inrange", l3.uuid)
        setVmStaticIp {
            vmInstanceUuid = vm2.uuid
            l3NetworkUuid = l3.uuid
            ip = "192.168.200.100"
        }

        UsedIpVO inRangeIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.vmNicUuid, vm2.vmNics[0].uuid)
                .eq(UsedIpVO_.ip, "192.168.200.100")
                .find()
        assert inRangeIp != null
        assert inRangeIp.ipRangeUuid != null : "ipRangeUuid should not be null for in-range IP"
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
    //  Part 2: Global config ON — Public: has IP range, no DHCP
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

        // Also verify in-range IP works
        VmInstanceInventory vm2 = createVmOnL3("vm-pub-range-noDhcp-inrange", l3.uuid)
        setVmStaticIp {
            vmInstanceUuid = vm2.uuid
            l3NetworkUuid = l3.uuid
            ip = "12.100.10.100"
        }

        UsedIpVO inRangeIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.vmNicUuid, vm2.vmNics[0].uuid)
                .eq(UsedIpVO_.ip, "12.100.10.100")
                .find()
        assert inRangeIp != null
        assert inRangeIp.ipRangeUuid != null : "ipRangeUuid should not be null for in-range IP"
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

        // Also verify in-range IP works
        VmInstanceInventory vm2 = createVmOnL3("vm-pub-range-dhcp-inrange", l3.uuid)
        setVmStaticIp {
            vmInstanceUuid = vm2.uuid
            l3NetworkUuid = l3.uuid
            ip = "12.100.20.100"
        }

        UsedIpVO inRangeIp = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.vmNicUuid, vm2.vmNics[0].uuid)
                .eq(UsedIpVO_.ip, "12.100.20.100")
                .find()
        assert inRangeIp != null
        assert inRangeIp.ipRangeUuid != null : "ipRangeUuid should not be null for in-range IP"
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
        env.afterSimulator(FlatDhcpBackend.APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            FlatDhcpBackend.ApplyDhcpCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.ApplyDhcpCmd.class)
            for (def dhcp : cmd.dhcp) {
                if (dhcp.ip == "10.0.3.50") { dhcpAppliedForOutsideIp = true }
            }
            return rsp
        }
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            FlatDhcpBackend.BatchApplyDhcpCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            for (def dhcpInfo : cmd.dhcpInfos) {
                for (def dhcp : dhcpInfo.dhcp) {
                    if (dhcp.ip == "10.0.3.50") { dhcpAppliedForOutsideIp = true }
                }
            }
            return rsp
        }

        startVmInstance { uuid = vm.uuid }

        assert !dhcpAppliedForOutsideIp : "DHCP should NOT include outside-range IP 10.0.3.50 even on DHCP-enabled public L3"
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
}
