package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.upgrade.UpgradeGlobalConfig
import org.zstack.compute.host.HostManagerImpl
import org.zstack.header.agent.versioncontrol.AgentVersionVO
import org.zstack.header.host.ConnectHostMsg
import org.zstack.header.host.ConnectHostReply
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.host.ReconnectHostMsg
import org.zstack.sdk.HostInventory
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.header.message.MessageReply
import org.zstack.header.host.HostConstant
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.sdk.ReconnectHostAction
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import java.util.function.Consumer

import static org.zstack.core.Platform.operr


class GrayscaleUpgradeCase extends SubCase {

    EnvSpec env
    CloudBus bus


    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        bus = bean(CloudBus.class)
        env.create {
            testManualReconnectHost()
            testGrayscaleUpgradeGlobalConfigValueTrue()
            testGrayscaleUpgradeGlobalConfigValueFalse()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    //场景1、2：灰度配置是否开启不影响手动重连逻辑, 开不开启全局配置重连都会成功
    void testManualReconnectHost() {

        boolean flag = false
        HostInventory host = env.inventoryByName('kvm')

        //灰度升级全局配置开启
        updateGlobalConfig {
            category = UpgradeGlobalConfig.GRAYSCALE_UPGRADE.category
            name = UpgradeGlobalConfig.GRAYSCALE_UPGRADE.name
            value = true
        }

        assert UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)

        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
            flag = true
            return rsp
        }

        ReconnectHostAction action = new ReconnectHostAction()
        action.uuid = host.getUuid()
        action.sessionId = adminSession()
        ReconnectHostAction.Result ret = action.call()
        assert ret.error == null

        retryInSecs {
            assert flag
        }

        //灰度升级全局配置关闭
        updateGlobalConfig {
            category = UpgradeGlobalConfig.GRAYSCALE_UPGRADE.category
            name = UpgradeGlobalConfig.GRAYSCALE_UPGRADE.name
            value = false
        }

        assert !UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)

        flag = false

        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> entity ->
            flag = true
            return rsp
        }

        action = new ReconnectHostAction()
        action.uuid = host.getUuid()
        action.sessionId = adminSession()
        ret = action.call()
        assert ret.error == null

        retryInSecs {
            assert flag
        }

    }

    //场景3：灰度开启，直接发送ConnectHostMsg，不会去重连（不发送http请求：/host/connect）
    void testGrayscaleUpgradeGlobalConfigValueTrue() {

        //开启全局配置
        HostInventory host = env.inventoryByName('kvm')

        updateGlobalConfig {
            category = UpgradeGlobalConfig.GRAYSCALE_UPGRADE.category
            name = UpgradeGlobalConfig.GRAYSCALE_UPGRADE.name
            value = true
        }

        assert UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)

        boolean flag = false

        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
            flag = true
            return rsp
        }

        boolean connectHostDone = false
        sendConnectHostInternalMessage(host.uuid, {reply ->
            reply.success = true
            connectHostDone = true
        })

        retryInSecs {
            assert !flag
            assert connectHostDone
        }

    }

    //场景4：灰度关闭，直接发送ConnectHostMsg，会去重连（发送http请求：/host/connect）
    void testGrayscaleUpgradeGlobalConfigValueFalse() {

        HostInventory host = env.inventoryByName('kvm')

        //关闭全局配置
        updateGlobalConfig {
            category = UpgradeGlobalConfig.GRAYSCALE_UPGRADE.category
            name = UpgradeGlobalConfig.GRAYSCALE_UPGRADE.name
            value = false
        }

        assert !UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class)

        boolean flag = false

        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> entity ->
            flag = true
            return rsp
        }

        boolean connectHostDone = false
        sendConnectHostInternalMessage(host.uuid, { reply ->
            reply.success = true
            connectHostDone = true
        })

        retryInSecs{
            assert flag
            assert connectHostDone
        }
    }

    void sendConnectHostInternalMessage(String hostUuid, Consumer<MessageReply> callback) {
        def msg = new ConnectHostMsg()
        msg.uuid = hostUuid
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid)
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            void run(MessageReply reply) {
                callback.accept(reply)
            }
        })
    }
}
