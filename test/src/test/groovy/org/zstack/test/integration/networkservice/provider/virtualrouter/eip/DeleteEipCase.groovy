package org.zstack.test.integration.networkservice.provider.virtualrouter.eip

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.core.db.Q
import org.zstack.core.db.SQLBatch
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.message.MessageReply
import org.zstack.header.network.l3.L3NetworkConstant
import org.zstack.header.network.l3.ReturnIpMsg
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.portforwarding.PortForwardingRuleState
import org.zstack.network.service.portforwarding.PortForwardingRuleVO
import org.zstack.network.service.portforwarding.PortForwardingRuleVO_
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.EipInventory
import org.zstack.network.service.eip.EipVO
import org.zstack.network.service.eip.EipVO_
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PortForwardingRuleInventory
import org.zstack.sdk.VipInventory
import org.zstack.network.service.vip.VipVO
import org.zstack.network.service.vip.VipVO_
import org.zstack.sdk.DeleteVipAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by MaJin on 2017-04-08.
 */
class DeleteEipCase extends SubCase{
    EnvSpec env
    VmInstanceInventory vm

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

                eip {
                    name = "eip"
                    useVip("pubL3")
                }

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
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory
            testAttachEipToVm()
            env.recreate("eip")
            testCreatePortForwarding()
            env.recreate("eip")
            testDeleteEipAfterTheVmDestroyed()
        }
    }

    void testDeleteEipAfterTheVmDestroyed() {
        EipInventory eipInv = env.inventoryByName("eip") as EipInventory
        destroyVmInstance {
            uuid = vm.getUuid()
        }

        boolean called = false
        env.afterSimulator(VirtualRouterConstant.VR_REMOVE_EIP) {
            called = true
        }

        deleteEip {
            uuid = eipInv.getUuid()
        }

        assert !dbIsExists(eipInv.getUuid(), EipVO.class)
        assert !called
    }

    void testAttachEipToVm(){
        EipInventory eipInv = env.inventoryByName("eip") as EipInventory
        attachEip {
            eipUuid = eipInv.uuid
            vmNicUuid = vm.vmNics.get(0).uuid
        }
        testDeleteEipAction()
        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).select(VmInstanceVO_.state).findValue() == VmInstanceState.Running

        testDeleteVipAction(eipInv.vipUuid)
        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).select(VmInstanceVO_.state).findValue() == VmInstanceState.Running
        assert !Q.New(EipVO.class).eq(EipVO_.uuid, eipInv.uuid).isExists()
    }

    void testCreatePortForwarding(){
        VipInventory vipInv = createVip {
            name = "vip"
            l3NetworkUuid = (env.inventoryByName("pubL3") as L3NetworkInventory).uuid
            sessionId = currentEnvSpec.session.uuid
        } as VipInventory

        PortForwardingRuleInventory port =  createPortForwardingRule {
            vipUuid = vipInv.uuid
            vipPortStart = 21L
            protocolType = "UDP"
            name = "port"
            sessionId = currentEnvSpec.session.uuid
        } as PortForwardingRuleInventory

        attachPortForwardingRule {
            vmNicUuid = vm.vmNics.get(0).uuid
            ruleUuid = port.uuid
            sessionId = currentEnvSpec.session.uuid
        }

        testDeleteVipAction(vipInv.uuid)
        assert !Q.New(PortForwardingRuleVO.class).eq(PortForwardingRuleVO_.uuid, port.uuid).isExists()
        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).select(VmInstanceVO_.state).findValue() == VmInstanceState.Running
    }


    void testDeleteVipAction(String vipUuid){
        VipVO vip = dbFindByUuid(vipUuid, VipVO.class)

        deleteVip{
            uuid = "aaa"
        }

        deleteVip{
            uuid = vipUuid
        }

        assert !Q.New(VipVO.class).eq(VipVO_.uuid, vipUuid).isExists()
        retryInSecs(){
            assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, vip.usedIpUuid).isExists()
        }

    }

    void testDeleteEipAction(){
        EipInventory eipInv = env.inventoryByName("eip") as EipInventory
        VipVO vipVO = dbFindByUuid(eipInv.vipUuid, VipVO.class)

        deleteEip{
            uuid = "aaa"
        }
        deleteEip {
            uuid = eipInv.uuid
        }

        new SQLBatch(){
            @Override
            protected void scripts() {
                assert !q(EipVO.class).eq(EipVO_.uuid, eipInv.uuid).isExists()
                assert q(VipVO.class).eq(VipVO_.uuid, vipVO.uuid).isExists()
                assert q(UsedIpVO.class).eq(UsedIpVO_.uuid, vipVO.usedIpUuid).isExists()
            }
        }.execute()
    }

    void testOnlyDeleteUsedIp(){
        EipInventory eipInv = env.inventoryByName("eip") as EipInventory
        L3NetworkInventory l3Inv = env.inventoryByName("pubL3") as L3NetworkInventory
        VipVO vipVO = dbFindByUuid(eipInv.vipUuid, VipVO.class)
        CloudBus bus = bean(CloudBus.class)

        ReturnIpMsg rmsg = new ReturnIpMsg()
        rmsg.usedIpUuid = vipVO.usedIpUuid
        rmsg.l3NetworkUuid = l3Inv.uuid
        bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, l3Inv.uuid)
        assert bus.call(rmsg).success
        retryInSecs(){
            new SQLBatch(){
                @Override
                protected void scripts() {
                    assert q(EipVO.class).eq(EipVO_.uuid, eipInv.uuid).isExists()
                    assert q(VipVO.class).eq(VipVO_.uuid, vipVO.uuid).isExists()
                    assert !q(UsedIpVO.class).eq(UsedIpVO_.uuid, vipVO.usedIpUuid).isExists()
                }
            }.execute()
        }
    }
}
