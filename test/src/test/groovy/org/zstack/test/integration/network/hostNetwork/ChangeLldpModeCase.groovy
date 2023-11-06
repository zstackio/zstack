package org.zstack.test.integration.network.hostNetwork

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.Platform
import org.zstack.kvm.KVMAgentCommands
import org.zstack.network.hostNetwork.HostNetworkInterfaceVO
import org.zstack.network.hostNetwork.HostNetworkInterfaceVO_
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpInventory
import org.zstack.network.hostNetwork.lldp.LldpConstant
import org.zstack.network.hostNetwork.lldp.LldpKvmAgentCommands
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpVO
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpVO_
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpRefVO
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpRefVO_
import org.zstack.sdk.*
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.sql.Timestamp

class ChangeLldpModeCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    HostNetworkInterfaceLldpInventory lldpMode
    GetHostNetworkInterfaceLldpResult ms
    GetHostNetworkInterfaceLldpResult ms2
    GetHostNetworkInterfaceLldpResult ms3
    String TEST_UUID = "36c22e8f-f05c-2780-bf6d-2fa65700f22e"

    @Override
    void setup() {
        spring {
            useSpring(HostNetworkTest.springSpec)
        }
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone1"
                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

                    attachL2Network("l2")
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"
                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            dbf = bean(DatabaseFacade.class)
            testChangeLldpMode()
            testGetLldpInfo()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testChangeLldpMode() {
        def host = env.inventoryByName("host1") as org.zstack.sdk.HostInventory

        HostNetworkInterfaceVO vo = new HostNetworkInterfaceVO();
        vo.setUuid(Platform.getUuid())
        vo.setHostUuid(host.getUuid())
        vo.setInterfaceName("enp101s0f2")
        vo.setSpeed(10000L)
        vo.setCarrierActive(true)
        vo.setMac("ac:1f:6b:93:6c:8e")
        vo.setPciDeviceAddress("0e:00.2")
        vo.setInterfaceType("noMaster")
        vo.setAccountUuid("36c27e8ff05c4780bf6d2fa65700f22e")
        vo.setResourceName("test")
        dbf.persist(vo)

        List<HostNetworkInterfaceVO> interfaceVOS = Q.New(HostNetworkInterfaceVO.class)
                .eq(HostNetworkInterfaceVO_.hostUuid, host.getUuid())
                .list()
        assert interfaceVOS.size() == 1

        def lldpMode = changeHostNetworkInterfaceLldpMode {
            interfaceUuids = [interfaceVOS.get(0).uuid]
            mode = "rx_only"
        } as HostNetworkInterfaceLldpInventory

        assert lldpMode.mode == "rx_only"

        List<HostNetworkInterfaceLldpVO> lldpVOS = Q.New(HostNetworkInterfaceLldpVO.class)
                .eq(HostNetworkInterfaceLldpVO_.interfaceUuid, interfaceVOS.get(0).uuid)
                .list()
        assert lldpVOS.size() == 1

        dbf.remove(vo)
    }

    void testGetLldpInfo() {
        env.simulator(LldpConstant.GET_LLDP_INFO_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def reply = new LldpKvmAgentCommands.GetLldpInfoResponse()
            HostNetworkInterfaceLldpRefInventory inv = new HostNetworkInterfaceLldpRefInventory()
            inv.setInterfaceUuid(Platform.uuid)
            inv.setChassisId("mac 00:1e:08:1d:05:ba")
            inv.setTimeToLive(120)
            inv.setManagementAddress("172.25.2.4")
            inv.setSystemName("BM-MN-3")
            inv.setSystemDescription(" CentecOS software, E530, Version 7.4.7 Copyright (C) 2004-2021 Centec Networks Inc.  All Rights Reserved.")
            inv.setSystemCapabilities("Bridge, on  Router, on")
            inv.setPortId("ifname eth-0-5")
            inv.setPortDescription("eth-0-4")
            inv.setVlanId(3999)
            inv.setAggregationPortId(4294965248L)
            inv.setMtu(9600)
            inv.setCreateDate(new Timestamp(DocUtils.date))
            inv.setLastOpDate(new Timestamp(DocUtils.date))

            reply.lldpInventory = inv
            return reply
        }

        def ms = getHostNetworkInterfaceLldp {
            interfaceUuid = host.uuid
        } as GetHostNetworkInterfaceLldpResult

        assert ms.lldpInventory.size() == 1

        env.simulator(LldpConstant.GET_LLDP_INFO_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def reply = new LldpKvmAgentCommands.GetLldpInfoResponse()
            HostNetworkInterfaceLldpRefInventory inv = new HostNetworkInterfaceLldpRefInventory()
            inv.setInterfaceUuid(TEST_UUID)
            inv.setChassisId("mac 00:1e:08:1d:05:ba")
            inv.setTimeToLive(119)
            inv.setManagementAddress("172.25.2.4")
            inv.setSystemName("BM-MN-3")
            inv.setSystemDescription(" CentecOS software, E530, Version 7.4.7 Copyright (C) 2004-2021 Centec Networks Inc.  All Rights Reserved.")
            inv.setSystemCapabilities("Bridge, on  Router, on")
            inv.setPortId("ifname eth-0-5")
            inv.setPortDescription("eth-0-4")
            inv.setVlanId(3999)
            inv.setAggregationPortId(4294965248L)
            inv.setMtu(9600)
            inv.setCreateDate(new Timestamp(DocUtils.date))
            inv.setLastOpDate(new Timestamp(DocUtils.date))

            reply.lldpInventory = inv
            return reply
        }

        ms2 = getHostNetworkInterfaceLldp {
            interfaceUuid = host.uuid
        } as GetHostNetworkInterfaceLldpResult

        ms3 = getHostNetworkInterfaceLldp {
            interfaceUuid = host.uuid
        } as GetHostNetworkInterfaceLldpResult

        assert ms.lldpInventory[0].ttl != ms2.lldpInventory[0].ttl
        assert ms2.lldpInventory[0].interfaceUuid == ms3.lldpInventory[0].interfaceUuid
        assert ms2.lldpInventory[0].managementAddress == "172.25.2.4"
    }
}
