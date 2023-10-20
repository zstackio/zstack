package org.zstack.test.integration.network.sdnController

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.DatabaseFacade
import org.zstack.network.hostNetwork.lldp.*
import org.zstack.sdk.*
import org.zstack.sdnController.header.SdnControllerConstant
import org.zstack.sdnController.header.SdnControllerVO
import org.zstack.header.network.l3.L3NetworkConstant
import org.zstack.sdnController.h3cVcfc.H3cVcfcSdnControllerSystemTags
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.sql.Timestamp

class ChangeLldpModeCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
//    HostNetworkInterfaceLLdpInventory lldpMode
//    GetHostNetworkInterfaceLldpResult ms
//    GetHostNetworkInterfaceLldpResult ms2
//    GetHostNetworkInterfaceLldpResult ms3

    @Override
    void setup() {
        spring {
            useSpring(HostNetworkTest.springSpec)
        }
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

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(20)
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(20)
                    }

                    attachPrimaryStorage("local")

                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                attachBackupStorage("sftp")
            }
        }
    }


    @Override
    void test() {
        env.create {
            host = env.inventoryByName("kvm") as HostInventory
            cluster = env.inventoryByName("cluster") as ClusterInventory
            eth0 = new HostNetworkInterfaceInventory()
            eth0.uuid = "eth0"
            eth0.hostUuid = host.uuid
            eth0.bondingUuid = null
            eth0.interfaceName = "eth0"
            eth0.interfaceType = NetworkInterfaceType.noBridge.toString()
            eth0.ipAddresses = ["192.168.1.10/24"]
            eth0.gateway = "192.168.1.1"
            eth0.callBackIp = "172.20.24.1"

            eth1 = new HostNetworkInterfaceInventory()
            eth1.uuid = "eth1"
            eth1.hostUuid = host.uuid
            eth1.bondingUuid = null
            eth1.interfaceName = "eth1"
            eth1.interfaceType = NetworkInterfaceType.noBridge.toString()
            eth1.ipAddresses = ["192.168.2.10/24"]
            eth1.gateway = "192.168.2.1"
            eth1.callBackIp = "172.20.24.1"

            eth2 = new HostNetworkInterfaceInventory()
            eth2.uuid = "eth2"
            eth2.hostUuid = host.uuid
            eth2.bondingUuid = null
            eth2.interfaceName = "eth2"
            eth2.interfaceType = NetworkInterfaceType.noBridge.toString()
            eth2.ipAddresses = ["192.168.3.10/24"]
            eth2.gateway = "192.168.3.1"
            eth2.callBackIp = "172.20.24.1"

            bond1 = new HostNetworkBondingInventory()
            bond1.setUuid(Platform.uuid)
            bond1.setHostUuid(host.uuid)
            bond1.setBondingName("bond1")
            bond1.setBondingType(NetworkInterfaceType.noBridge.toString())
            bond1.setIpAddresses(Collections.singletonList("172.20.0.116/16"))
            bond1.setMac("ac:1f:6b:93:6c:8c")
            bond1.setMiimon(100L)
            bond1.setMiiStatus("up")
            bond1.setMode("active-backup 1")
            bond1.setXmitHashPolicy("layer2 0")
            bond1.setAllSlavesActive(true)
            bond1.setCreateDate(new Timestamp(DocUtils.date))
            bond1.setLastOpDate(new Timestamp(DocUtils.date))

            testChangeLldpMode()
            testGetLldpInfo()
        }

        env.create {
            dbf = bean(DatabaseFacade.class)

        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testChangeLldpMode() {
//        def zone = env.inventoryByName("zone") as ZoneInventory
//        def cluster = env.inventoryByName("cluster") as ClusterInventory
//
//        def lldpMode = changeHostNetworkInterfaceLldpMode {
//            interfaceUuid =
//            mode = "rx_only"
//        } as HostNetworkInterfaceLLdpInventory
//
//        assert lldpMode.mode = "rx_only"
    }

    void testGetLldpInfo() {
//        env.simulator(LldpConstant.GET_LLDP_INFO_PATH) { HttpEntity<String> entity, EnvSpec spec ->
//            def reply = new LldpKvmAgentCommands.GetLldpInfoResponse()
//            HostNetworkInterfaceLldpRefInventory inv = new HostNetworkInterfaceLldpRefInventory()
//            inv.setInterfaceUuid(Platform.uuid)
//            inv.setChassisId("mac 00:1e:08:1d:05:ba")
//            inv.setTimeToLive(120)
//            inv.setManagementAddress("172.25.2.4")
//            inv.setSystemName("BM-MN-3")
//            inv.setSystemDescription(" CentecOS software, E530, Version 7.4.7 Copyright (C) 2004-2021 Centec Networks Inc.  All Rights Reserved.")
//            inv.setSystemCapabilities("Bridge, on  Router, on")
//            inv.setPortId("ifname eth-0-5")
//            inv.setPortDescription("eth-0-4")
//            inv.setVlanId(3999)
//            inv.setAggregationPortId(4294965248L)
//            inv.setMtu(9600)
//            inv.setCreateDate(new Timestamp(DocUtils.date))
//            inv.setLastOpDate(new Timestamp(DocUtils.date))
//
//            reply.lldpInventory = inv
//            return reply
//        }
//
//        def ms = getHostNetworkInterfaceLldp {
//            interfaceUuid = host.uuid
//        } as GetHostNetworkFactsResult
//
//        assert ms.lldpInventory.size() == 1
//
//        env.simulator(LldpConstant.GET_LLDP_INFO_PATH) { HttpEntity<String> entity, EnvSpec spec ->
//            def reply = new LldpKvmAgentCommands.GetLldpInfoResponse()
//            HostNetworkInterfaceLldpRefInventory inv = new HostNetworkInterfaceLldpRefInventory()
//            inv.setInterfaceUuid(Platform.uuid)
//            inv.setChassisId("mac 00:1e:08:1d:05:ba")
//            inv.setTimeToLive(119)
//            inv.setManagementAddress("172.25.2.4")
//            inv.setSystemName("BM-MN-3")
//            inv.setSystemDescription(" CentecOS software, E530, Version 7.4.7 Copyright (C) 2004-2021 Centec Networks Inc.  All Rights Reserved.")
//            inv.setSystemCapabilities("Bridge, on  Router, on")
//            inv.setPortId("ifname eth-0-5")
//            inv.setPortDescription("eth-0-4")
//            inv.setVlanId(3999)
//            inv.setAggregationPortId(4294965248L)
//            inv.setMtu(9600)
//            inv.setCreateDate(new Timestamp(DocUtils.date))
//            inv.setLastOpDate(new Timestamp(DocUtils.date))
//
//            reply.lldpInventory = inv
//            return reply
//        }
//
//        ms2 = getHostNetworkInterfaceLldp {
//            interfaceUuid = host.uuid
//        } as GetHostNetworkFactsResult
//
//        ms3 = getHostNetworkInterfaceLldp {
//            interfaceUuid = host.uuid
//        } as GetHostNetworkFactsResult
//
//        assert ms.lldpInventory[0].ttl != ms2.lldpInventory[0].ttl
//        assert ms2.lldpInventory[0].interfaceUuid == ms3.lldpInventory[0].interfaceUuid
//        assert ms2.lldpInventory[0].managementAddress == "172.25.2.4"
    }
}
