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
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.network.service.portforwarding.PortForwardingRuleState
import org.zstack.network.service.portforwarding.PortForwardingRuleVO
import org.zstack.network.service.portforwarding.PortForwardingRuleVO_
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
        env = VirtualRouterNetworkServiceEnv.oneVmOneHostVyosOnEipEnv()
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
            testOnlyDeleteUsedIp()
        }
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
        new SQLBatch(){
            @Override
            protected void scripts() {
                assert !q(VipVO.class).eq(VipVO_.uuid, vipUuid).isExists()
                assert !q(UsedIpVO.class).eq(UsedIpVO_.uuid, vip.usedIpUuid).isExists()
            }
        }.execute()

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
        bus.send(rmsg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                new SQLBatch(){
                    @Override
                    protected void scripts() {
                        assert !q(EipVO.class).eq(EipVO_.uuid, eipInv.uuid).isExists()
                        assert !q(VipVO.class).eq(VipVO_.uuid, vipVO.uuid).isExists()
                        assert !q(UsedIpVO.class).eq(UsedIpVO_.uuid, vipVO.usedIpUuid).isExists()
                    }
                }.execute()
            }
        })


    }

}
