package org.zstack.test.integration.storage.primary.nfs

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.Constants
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor
import org.zstack.header.message.AbstractBeforeSendMessageInterceptor
import org.zstack.header.message.Event
import org.zstack.header.message.Message
import org.zstack.header.message.MessageReply
import org.zstack.header.storage.primary.PingPrimaryStorageMsg
import org.zstack.header.storage.primary.PrimaryStorageConstant
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_
import org.zstack.header.storage.primary.PrimaryStorageHostStatus
import org.zstack.header.storage.primary.PrimaryStorageStatus
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands
import org.zstack.test.integration.storage.NfsEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger

import java.util.concurrent.ConcurrentHashMap

/**
 * Created by MaJin on 2017-05-07.
 */
class PingWhenOnlyOneHostAccessNfsUrlCase extends SubCase{
    private final static CLogger logger = Utils.getLogger(PingWhenOnlyOneHostAccessNfsUrlCase.class)
    EnvSpec env
    HostInventory host1, host2
    PrimaryStorageInventory ps1, ps2
    CloudBus bus
    Map<String, Boolean> hardwareConnectionStatus = new HashMap<>()
    Map<String, Boolean> pingChecked = new ConcurrentHashMap<>()
    List<Boolean> pingResults = Collections.synchronizedList(new ArrayList<Boolean>())
    String currentPingMsgId = null
    boolean caseOver = false
    static int retryTimes = 8

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = NfsEnv.TwoNfsTwoHostOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            host1 = env.inventoryByName("kvm") as HostInventory
            host2 = env.inventoryByName("kvm1") as HostInventory
            ps1 = env.inventoryByName("nfs1") as PrimaryStorageInventory
            ps2 = env.inventoryByName("nfs2") as PrimaryStorageInventory

            bus = bean(CloudBus.class)
            recoverEnviroment()
            simulateEnv()
            caseOver = false

            // follow 3 tests is host1 and host2 ping ps1
            testNotAllHostPingNfsSuccess()
            testAllHostPingNfsFail()
            testAllHostPingNfsSucess()

            // follow test is host1 ping ps1 and ps2
            testHostPingAllNfsFail()
            caseOver = true
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testNotAllHostPingNfsSuccess(){
        down(host1.uuid)
        MessageReply reply = pingPs(ps1.uuid)

        assert pingChecked[ps1.uuid] && pingChecked[host2.uuid]
        assert reply.success
        assert (pingResults.get(0) && pingResults.size() == 1) || (!pingResults.get(0) && pingResults.size() == 2)
        if (pingChecked[host1.uuid]) {
            assert Q.New(PrimaryStorageHostRefVO.class)
                    .eq(PrimaryStorageHostRefVO_.hostUuid, host1.getUuid())
                    .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps1.uuid)
                    .select(PrimaryStorageHostRefVO_.status)
                    .findValue() == PrimaryStorageHostStatus.Disconnected
            assert Q.New(HostVO.class)
                    .eq(HostVO_.uuid, host1.getUuid())
                    .select(HostVO_.status)
                    .findValue() == HostStatus.Connected

            SQL.New(PrimaryStorageHostRefVO.class)
                    .eq(PrimaryStorageHostRefVO_.hostUuid, host1.getUuid())
                    .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps1.uuid)
                    .set(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Connected)
                    .update()
        }

        recoverEnviroment()
    }

    void testAllHostPingNfsFail(){
        down(ps1.uuid)
        MessageReply reply = pingPs(ps1.uuid)

        logger.debug(String .format("before assert pingChecked: %s", pingChecked))
        assert pingChecked[ps1.uuid] && pingChecked[host1.uuid] && pingChecked[host2.uuid]
        assert pingResults.size() == 2
        assert !reply.success
        assert Q.New(PrimaryStorageHostRefVO.class)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps1.uuid)
                .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                .count() == 2L
        assert Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, ps1.uuid)
                .select(PrimaryStorageVO_.status).findValue() == PrimaryStorageStatus.Disconnected
        assert Q.New(HostVO.class).eq(HostVO_.uuid, host1.uuid)
                .select(HostVO_.status).findValue() == HostStatus.Connected

        recoverEnviroment()
    }

    void testAllHostPingNfsSucess(){
        pingPs(ps1.uuid)

        retryInSecs(){
            return {
                assert pingChecked[ps1.uuid]
                assert pingResults.size() == 1
                assert Q.New(PrimaryStorageHostRefVO.class)
                        .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps1.uuid)
                        .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Connected)
                        .count() == 2L // ps has been reconnected auto, all host-nfs is set to Connected
                assert  Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, ps1.uuid)
                        .select(PrimaryStorageVO_.status).findValue() == PrimaryStorageStatus.Connected
                assert  Q.New(HostVO.class).eq(HostVO_.uuid, host1.uuid)
                        .select(HostVO_.status).findValue() == HostStatus.Connected
            }
        }
    }

    void testHostPingAllNfsFail(){
        for (int i = 0; i < retryTimes; i++) {
            recoverEnviroment()
            down(host1.uuid)
            MessageReply reply = pingPs(ps1.uuid)

            assert pingChecked[ps1.uuid]
            assert reply.success
            if (pingChecked[host1.uuid]) {
                break
            }
            if (i == retryTimes - 1) {
                logger.warn("abandon this testHostPingAllNfsFail because ping ps1 not by selected host after retryTimes")
                return
            }
        }
        assert pingChecked[host1.uuid]

        for (int i = 0; i < retryTimes; i++) {
            recoverEnviroment()
            down(host1.uuid)
            MessageReply reply = pingPs(ps2.uuid)

            assert pingChecked[ps2.uuid]
            assert reply.success
            if (pingChecked[host1.uuid]) {
                break
            }

            if (i == retryTimes - 1) {
                logger.warn("abandon this testHostPingAllNfsFail because ping ps2 not by selected host after retryTimes")
                return
            }
        }
        assert pingChecked[host1.uuid]

        retryInSecs{
            assert Q.New(HostVO.class).eq(HostVO_.uuid, host1.uuid).select(HostVO_.status).findValue() == HostStatus.Disconnected
        }

        recoverEnviroment()
    }

    private void simulateEnv(){
        env.simulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH){ HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.PingCmd.class)
            def huuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)

            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            if (!hardwareConnectionStatus[cmd.uuid] || !hardwareConnectionStatus[huuid]){
                rsp.setError("on purpose Connection error")
                rsp.setSuccess(false)
            }
            return rsp
        }

        env.simulator(NfsPrimaryStorageKVMBackend.PING_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.PingCmd.class)
            def huuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            pingChecked[cmd.uuid] = true
            pingChecked[huuid] = true
            logger.debug(String .format("after setValue pingChecked: %s", pingChecked))

            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            if (!hardwareConnectionStatus[cmd.uuid] || !hardwareConnectionStatus[huuid]){
                rsp.setError("on purpose Connection error")
                pingResults.add(false)
            } else {
                pingResults.add(true)
            }

            return rsp
        }

        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            void intercept(Message msg) {
                logger.debug("intercept msg id:" + msg.id)
                if (msg.id == currentPingMsgId) {
                    return
                }

                retryInSecs(600){
                    return caseOver
                }
            }
        }, PingPrimaryStorageMsg.class)
    }

    private final void initConnectionStatus(){
        hardwareConnectionStatus = [(host1.uuid): true, (host2.uuid): true, (ps1.uuid): true, (ps2.uuid): true]

    }

    private final void initChecked(){
        pingChecked = [(host1.uuid): false, (host2.uuid): false, (ps1.uuid): false, (ps2.uuid): false]
    }

    private final void initResults(){
        pingResults.clear()
    }

    private final void down(String ...uuids){
        for (String uuid : uuids) {
            hardwareConnectionStatus[uuid] = false
        }
    }

    private final void up(String ...uuids){
        for (String uuid : uuids) {
            hardwareConnectionStatus[uuid] = true
        }
    }

    private recoverEnviroment(){
        initChecked()
        initConnectionStatus()
        initResults()
    }

    private MessageReply pingPs(String uuid) {
        PingPrimaryStorageMsg msg = new PingPrimaryStorageMsg()
        msg.setPrimaryStorageUuid(uuid)
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, uuid)
        currentPingMsgId = msg.id
        logger.debug("currentPingMsgId:" + currentPingMsgId)
        return bus.call(msg)
    }
}
