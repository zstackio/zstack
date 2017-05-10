package org.zstack.test.integration.storage.primary.nfs

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.Constants
import org.zstack.header.agent.AgentResponse
import org.zstack.header.host.Host
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_
import org.zstack.header.storage.primary.PrimaryStorageHostStatus
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.sdk.AttachPrimaryStorageToClusterAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.ReconnectBackupStorageAction
import org.zstack.sdk.ReconnectHostAction
import org.zstack.sdk.ReconnectPrimaryStorageAction
import org.zstack.sdk.UpdatePrimaryStorageAction
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.NfsPrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by MaJin on 2017-04-24.
 */
class InvalidUrlCase extends SubCase {
    EnvSpec env
    ClusterInventory cluInv
    PrimaryStorageInventory psInv
    HostInventory host
    HostInventory host1

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.nfsOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            cluInv = env.inventoryByName("cluster") as ClusterInventory
            psInv = env.inventoryByName("nfs") as PrimaryStorageInventory
            host = env.inventoryByName("kvm") as HostInventory
            host1 = env.inventoryByName("kvm1") as HostInventory
            testDetachNfsFromCluster()
            testAttachNfsToClusterWithInvalidUrl()
            testAttachNfsToCluster()
            testDetachNfsFromCluster()
            testAttachNfsToClusterWithInvalidUrl()
            testAttachNfsToCluster()
            testUpdateValidUrl()
            testUpdateUrlNotAllHostReturnFailure();
            testReconnectHostWithInvalidNfsUrl();
            testReconnectHost();
            testUpdateValidUrl();
            testUpdateInvalidUrl()
            testReconnectHostWithInvalidNfsUrl()
            testReconnectHost()
            testReconnectNfsWithInvalidUrl()
            testReconnectHost()
            testReconnectNfs()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
    void testDetachNfsFromCluster() {
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = psInv.uuid
            clusterUuid = cluInv.uuid
            sessionId = currentEnvSpec.session.uuid
        }
    }

    void testAttachNfsToCluster(){
        env.simulator(NfsPrimaryStorageKVMBackend.MOUNT_PRIMARY_STORAGE_PATH) { HttpEntity<String> e, EnvSpec espec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.MountCmd.class)
            NfsPrimaryStorageSpec spec = espec.specByUuid(cmd.uuid) as NfsPrimaryStorageSpec
            def rsp = new NfsPrimaryStorageKVMBackendCommands.MountAgentResponse()
            rsp.totalCapacity = spec.totalCapacity
            rsp.availableCapacity = spec.availableCapacity
            return rsp
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = psInv.uuid
            clusterUuid = cluInv.uuid
            sessionId = currentEnvSpec.session.uuid
        }



    }

    void testAttachNfsToClusterWithInvalidUrl() {
        env.simulator(NfsPrimaryStorageKVMBackend.MOUNT_PRIMARY_STORAGE_PATH){
            AgentResponse rsp = new AgentResponse()
            rsp.setError("No such file")
            rsp.setSuccess(false)
            return rsp
        }

        AttachPrimaryStorageToClusterAction a = new AttachPrimaryStorageToClusterAction()
        a.clusterUuid = cluInv.uuid
        a.primaryStorageUuid = psInv.uuid
        a.sessionId = currentEnvSpec.session.uuid

        retryInSecs{
            assert a.call().error != null
        }

    }

    void testUpdateInvalidUrl(){
        env.simulator(NfsPrimaryStorageKVMBackend.UPDATE_MOUNT_POINT_PATH){
            AgentResponse rsp = new AgentResponse()
            rsp.setError("No such file")
            rsp.setSuccess(false)
            return rsp
        }

        updatePrimaryStorage {
            uuid = psInv.uuid
            url = psInv.url
            sessionId = currentEnvSpec.session.uuid
        }

        UpdatePrimaryStorageAction a = new UpdatePrimaryStorageAction()
        a.uuid = psInv.uuid
        a.url = "172.20.11.11:/wrong/directory"
        a.sessionId = currentEnvSpec.session.uuid;

        assert a.call().error != null

        assert Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.url)
                .eq(PrimaryStorageVO_.uuid, psInv.uuid)
                .findValue() == psInv.url

    }

    void testUpdateValidUrl(){
        env.simulator(NfsPrimaryStorageKVMBackend.UPDATE_MOUNT_POINT_PATH) { HttpEntity<String> e, EnvSpec espec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.UpdateMountPointCmd.class)
            NfsPrimaryStorageSpec spec = espec.specByUuid(cmd.uuid) as NfsPrimaryStorageSpec
            def rsp = new NfsPrimaryStorageKVMBackendCommands.UpdateMountPointRsp()
            rsp.totalCapacity = spec.totalCapacity
            rsp.availableCapacity = spec.availableCapacity
            return rsp
        }
        updatePrimaryStorage {
            uuid = psInv.uuid
            url = "172.20.11.11:/true/directory"
            sessionId = currentEnvSpec.session.uuid
        }
        updatePrimaryStorage {
            uuid = psInv.uuid
            url = psInv.url
            sessionId = currentEnvSpec.session.uuid
        }
    }

    void testUpdateUrlNotAllHostReturnFailure(){
        env.simulator(NfsPrimaryStorageKVMBackend.UPDATE_MOUNT_POINT_PATH) { HttpEntity<String> e, EnvSpec espec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.UpdateMountPointCmd.class)
            def huuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            if( huuid == host.uuid){
                AgentResponse rsp = new AgentResponse()
                rsp.setError("Connection error")
                rsp.setSuccess(false)
                return rsp
            }else {
                NfsPrimaryStorageSpec spec = espec.specByUuid(cmd.uuid) as NfsPrimaryStorageSpec
                def rsp = new NfsPrimaryStorageKVMBackendCommands.UpdateMountPointRsp()
                rsp.totalCapacity = spec.totalCapacity
                rsp.availableCapacity = spec.availableCapacity
                return rsp
            }
        }

        updatePrimaryStorage {
            uuid = psInv.uuid
            url = "172.20.11.11:/true/directory"
            sessionId = currentEnvSpec.session.uuid
        }

        assert Q.New(PrimaryStorageHostRefVO.class).select(PrimaryStorageHostRefVO_.status)
                .eq(PrimaryStorageHostRefVO_.hostUuid, host.uuid)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, psInv.uuid)
                .findValue() == PrimaryStorageHostStatus.Disconnected

        assert Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, psInv.uuid)
                .select(PrimaryStorageVO_.url).findValue() == "172.20.11.11:/true/directory"
    }

    void testReconnectHostWithInvalidNfsUrl(){
        env.simulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH){
            AgentResponse rsp = new AgentResponse()
            rsp.setError("No such file")
            rsp.setSuccess(false)
            return rsp
        }
        ReconnectHostAction a = new ReconnectHostAction()
        a.uuid = host.uuid
        a.sessionId = currentEnvSpec.session.uuid


        assert a.call().error != null

    }

    void testReconnectHost(){
        env.simulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH) { HttpEntity<String> e, EnvSpec espec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.RemountCmd.class)
            NfsPrimaryStorageSpec spec = espec.specByUuid(cmd.uuid) as NfsPrimaryStorageSpec
            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            rsp.totalCapacity = spec.totalCapacity
            rsp.availableCapacity = spec.availableCapacity
            return rsp
        }

        reconnectHost {
            uuid = host.uuid
            sessionId = currentEnvSpec.session.uuid
        }
    }

    void testReconnectNfs(){
        env.simulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH) { HttpEntity<String> e, EnvSpec espec ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), NfsPrimaryStorageKVMBackendCommands.RemountCmd.class)
            NfsPrimaryStorageSpec spec = espec.specByUuid(cmd.uuid) as NfsPrimaryStorageSpec
            def rsp = new NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse()
            rsp.totalCapacity = spec.totalCapacity
            rsp.availableCapacity = spec.availableCapacity
            return rsp
        }

        reconnectPrimaryStorage {
            uuid = psInv.uuid
            sessionId = currentEnvSpec.session.uuid
        }
    }

    void testReconnectNfsWithInvalidUrl(){
        env.simulator(NfsPrimaryStorageKVMBackend.REMOUNT_PATH){
            AgentResponse rsp = new AgentResponse()
            rsp.setError("No such file")
            rsp.setSuccess(false)
            return rsp
        }

        ReconnectPrimaryStorageAction a = new ReconnectPrimaryStorageAction()
        a.uuid = psInv.uuid
        a.sessionId = currentEnvSpec.session.uuid

        retryInSecs{
            assert a.call().error != null
        }
    }
}
