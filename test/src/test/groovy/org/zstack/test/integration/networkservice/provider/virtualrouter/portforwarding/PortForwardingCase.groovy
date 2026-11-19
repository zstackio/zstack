package org.zstack.test.integration.networkservice.provider.virtualrouter.portforwarding

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.portforwarding.PortForwardingProtocolType
import org.zstack.network.service.portforwarding.PortForwardingRuleState
import org.zstack.network.service.portforwarding.PortForwardingRuleStateEvent
import org.zstack.network.service.portforwarding.PortForwardingRuleVO
import org.zstack.network.service.vip.VipVO
import org.zstack.network.service.vip.VipVO_
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO_
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO_
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PortForwardingRuleInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by AlanJager on 2017/4/21.
 */
class PortForwardingCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
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
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }
            }

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testDeleteOneUnattchedPortForwadingRuleAndAttachAnotherWillRequireVip()
            testDeletePortForwardingRuleWhenNotAttachAnyVm()
            testChangePortForwardingRuleStateSyncWithVr()
        }
    }

    void testDeleteOneUnattchedPortForwadingRuleAndAttachAnotherWillRequireVip() {
        L3NetworkInventory l3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")
        VipInventory vip = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        }

        assert Q.New(VipVO.class).select(VipVO_.uuid).eq(VipVO_.uuid, vip.uuid).listValues().size() == 1
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().isEmpty()

        PortForwardingRuleInventory portForwarding = createPortForwardingRule {
            name = "test"
            vipUuid = vip.uuid
            vipPortStart = 22
            vipPortEnd = 22
            privatePortEnd = 100
            privatePortStart = 100
            protocolType = PortForwardingProtocolType.TCP.toString()
        }

        PortForwardingRuleInventory portForwarding2 = createPortForwardingRule {
            name = "test"
            vipUuid = vip.uuid
            vipPortStart = 3306
            vipPortEnd = 3306
            privatePortEnd = 3306
            privatePortStart = 3306
            protocolType = PortForwardingProtocolType.TCP.toString()
        }

        deletePortForwardingRule {
            uuid = portForwarding.getUuid()
        }
        assert Q.New(VipVO.class).select(VipVO_.uuid).eq(VipVO_.uuid, vip.uuid).listValues().size() == 1
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().size() == 0

        VirtualRouterCommands.RemoveVipCmd removeVipCmd = null
        env.afterSimulator(VirtualRouterConstant.VR_REMOVE_VIP) { VirtualRouterCommands.RemoveVipRsp rsp, HttpEntity<String> e ->
            removeVipCmd = JSONObjectUtil.toObject(e.body,  VirtualRouterCommands.RemoveVipCmd.class)
            return rsp
        }

        attachPortForwardingRule {
            vmNicUuid = vm.getVmNics().get(0).uuid
            ruleUuid = portForwarding2.uuid
        }
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().size() == 1
        assert Q.New(VipVO.class).select(VipVO_.uuid).eq(VipVO_.uuid, vip.uuid).listValues().size() == 1

        deletePortForwardingRule {
            uuid = portForwarding2.uuid
        }
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().size() == 0
        assert Q.New(VipVO.class).select(VipVO_.uuid).eq(VipVO_.uuid, vip.uuid).listValues().size() == 1
        assert removeVipCmd != null

        deleteVip {
            uuid = vip.uuid
        }
        assert !Q.New(VipVO.class).eq(VipVO_.uuid, vip.uuid).isExists()
    }

    void testDeletePortForwardingRuleWhenNotAttachAnyVm() {
        L3NetworkInventory l3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")
        VipInventory vip = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        }

        assert Q.New(VipVO.class).select(VipVO_.uuid).eq(VipVO_.uuid, vip.uuid).listValues().size() == 1
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().isEmpty()

        PortForwardingRuleInventory portForwarding = createPortForwardingRule {
            name = "test"
            vipUuid = vip.uuid
            vipPortStart = 22
            vipPortEnd = 22
            privatePortEnd = 100
            privatePortStart = 100
            protocolType = PortForwardingProtocolType.TCP.toString()
        }

        attachPortForwardingRule {
            vmNicUuid = vm.getVmNics().get(0).uuid
            ruleUuid = portForwarding.uuid
        }
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().size() == 1
        VipVO vipVO = dbFindByUuid(vip.uuid, VipVO.class)
        assert vipVO.serviceProvider != null

        PortForwardingRuleInventory portForwarding2 = createPortForwardingRule {
            name = "test"
            vipUuid = vip.uuid
            vipPortStart = 3306
            vipPortEnd = 3306
            privatePortEnd = 3306
            privatePortStart = 3306
            protocolType = PortForwardingProtocolType.TCP.toString()
        }
        vipVO = dbFindByUuid(vip.uuid, VipVO.class)
        assert vipVO.serviceProvider != null

        deletePortForwardingRule {
            uuid = portForwarding.getUuid()
        }
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().size() == 0
        vipVO = dbFindByUuid(vip.uuid, VipVO.class)
        assert vipVO.serviceProvider == null

        deletePortForwardingRule {
            uuid = portForwarding2.getUuid()
        }
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().size() == 0
        assert Q.New(VipVO.class).eq(VipVO_.uuid, vip.uuid).isExists()

        deleteVip {
            uuid = vip.uuid
        }
        assert !Q.New(VipVO.class).eq(VipVO_.uuid, vip.uuid).isExists()
    }

    void testCreatePortForwardingRuleWillNotSetVipServiceProviderToNull() {
        L3NetworkInventory l3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")
        VipInventory vip = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        }

        assert Q.New(VipVO.class).select(VipVO_.uuid).eq(VipVO_.uuid, vip.uuid).listValues().size() == 1
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().isEmpty()

        PortForwardingRuleInventory portForwarding = createPortForwardingRule {
            name = "test"
            vipUuid = vip.uuid
            vipPortStart = 22
            vipPortEnd = 22
            privatePortEnd = 100
            privatePortStart = 100
            protocolType = PortForwardingProtocolType.TCP.toString()
            vmNicUuid = vm.getVmNics().get(0).uuid
        }
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().size() == 1

        stopVmInstance {
            uuid = vm.uuid
        }
        VmInstanceVO vmInstanceVO = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vmInstanceVO.state == VmInstanceState.Stopped

        PortForwardingRuleInventory portForwarding2 = createPortForwardingRule {
            name = "test"
            vipUuid = vip.uuid
            vipPortStart = 3306
            vipPortEnd = 3306
            privatePortEnd = 3306
            privatePortStart = 3306
            protocolType = PortForwardingProtocolType.TCP.toString()
            vmNicUuid = vm.vmNics.get(0).uuid
        }
        VipVO vipVO = dbFindByUuid(vip.uuid, VipVO.class)
        assert vipVO.serviceProvider != null
    }

    void testChangePortForwardingRuleStateSyncWithVr() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")
        VipInventory vip = createVip {
            name = "vip-pf-state"
            l3NetworkUuid = pubL3.uuid
        }

        PortForwardingRuleInventory pf = createPortForwardingRule {
            name = "pf-state-rule"
            vipUuid = vip.uuid
            vipPortStart = 18000
            vipPortEnd = 18000
            privatePortStart = 8000
            privatePortEnd = 8000
            protocolType = PortForwardingProtocolType.TCP.toString()
            vmNicUuid = vm.vmNics[0].uuid
        }

        List<VirtualRouterCommands.RevokePortForwardingRuleCmd> revokeCmds = Collections.synchronizedList(new ArrayList<>())
        List<VirtualRouterCommands.CreatePortForwardingRuleCmd> createCmds = Collections.synchronizedList(new ArrayList<>())
        List<VirtualRouterCommands.SyncPortForwardingRuleCmd> syncCmds = Collections.synchronizedList(new ArrayList<>())

        env.afterSimulator(VirtualRouterConstant.VR_REVOKE_PORT_FORWARDING) { VirtualRouterCommands.RevokePortForwardingRuleRsp rsp, HttpEntity<String> e ->
            revokeCmds.add(JSONObjectUtil.toObject(e.body, VirtualRouterCommands.RevokePortForwardingRuleCmd.class))
            return rsp
        }
        env.afterSimulator(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING) { VirtualRouterCommands.CreatePortForwardingRuleRsp rsp, HttpEntity<String> e ->
            createCmds.add(JSONObjectUtil.toObject(e.body, VirtualRouterCommands.CreatePortForwardingRuleCmd.class))
            return rsp
        }
        env.afterSimulator(VirtualRouterConstant.VR_SYNC_PORT_FORWARDING) { VirtualRouterCommands.SyncPortForwardingRuleRsp rsp, HttpEntity<String> e ->
            syncCmds.add(JSONObjectUtil.toObject(e.body, VirtualRouterCommands.SyncPortForwardingRuleCmd.class))
            return rsp
        }

        // 1. Disabling an attached PF rule must revoke it on VR while keeping DB binding
        PortForwardingRuleInventory disabledPf = changePortForwardingRuleState {
            uuid = pf.uuid
            stateEvent = PortForwardingRuleStateEvent.disable.toString()
        }
        assert disabledPf.state == PortForwardingRuleState.Disabled.toString()
        assert disabledPf.vmNicUuid == vm.vmNics[0].uuid
        assert revokeCmds.any { cmd -> cmd.rules.any { it.uuid == pf.uuid } } :
                "Disabling attached PF rule must revoke it on VR, got revokeCmds=${revokeCmds.size()}"

        // 2. While disabled, destroying old VR and recreating VR via VM stop/start must NOT push disabled rule
        createCmds.clear()
        syncCmds.clear()
        String oldVrUuid = Q.New(VirtualRouterVmVO.class).select(VirtualRouterVmVO_.uuid).eq(VirtualRouterVmVO_.state, VmInstanceState.Running).findValue()
        assert oldVrUuid != null
        destroyVmInstance { uuid = oldVrUuid }

        stopVmInstance { uuid = vm.uuid }
        startVmInstance { uuid = vm.uuid }

        String newVrUuid = Q.New(VirtualRouterVmVO.class).select(VirtualRouterVmVO_.uuid).eq(VirtualRouterVmVO_.state, VmInstanceState.Running).findValue()
        assert newVrUuid != null && newVrUuid != oldVrUuid
        assert !syncCmds.any { cmd -> cmd.rules.any { it.uuid == pf.uuid } } :
                "Newly created VR must not sync disabled PF rule"
        assert !createCmds.any { cmd -> cmd.rules.any { it.uuid == pf.uuid } } :
                "Starting VM must not apply disabled PF rule"

        // 3. Re-enabling rule must apply it to the new VR, and subsequent reconnect must sync it
        createCmds.clear()
        syncCmds.clear()
        PortForwardingRuleInventory enabledPf = changePortForwardingRuleState {
            uuid = pf.uuid
            stateEvent = PortForwardingRuleStateEvent.enable.toString()
        }
        assert enabledPf.state == PortForwardingRuleState.Enabled.toString()
        assert createCmds.any { cmd -> cmd.rules.any { it.uuid == pf.uuid } } :
                "Re-enabling attached PF rule must apply it on VR"

        reconnectVirtualRouter { vmInstanceUuid = newVrUuid }
        assert syncCmds.any { cmd -> cmd.rules.any { it.uuid == pf.uuid } } :
                "Reconnecting VR after re-enabling PF rule must include rule in sync"

        deletePortForwardingRule { uuid = pf.uuid }
        deleteVip { uuid = vip.uuid }
    }
}
