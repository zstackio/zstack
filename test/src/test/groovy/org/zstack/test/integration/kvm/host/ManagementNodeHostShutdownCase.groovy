package org.zstack.test.integration.kvm.host

import org.zstack.core.Platform
import org.zstack.core.cloudbus.ResourceDestinationMakerImpl
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.managementnode.ManagementNodeChangeListener
import org.zstack.header.managementnode.ManagementNodeInventory
import org.zstack.header.managementnode.ManagementNodeState
import org.zstack.header.managementnode.ManagementNodeVO
import org.zstack.header.managementnode.ManagementNodeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.portal.managementnode.ManagementNodeManagerImpl
import org.zstack.sdk.KVMHostInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.atomic.AtomicInteger

/**
 * Scenario: Shut down with two and three MN records, update a host, then shut down with an unjoined peer and alone.
 * Expected: Joined peers complete shutdown; ordinary updates and shutdown without a joined peer still succeed.
 * MN membership and agent responses are simulated; real MN process exit requires live verification.
 */
class ManagementNodeHostShutdownCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    ResourceDestinationMakerImpl destinationMaker
    ManagementNodeChangeListener nodeLifeCycle
    ManagementNodeVO peer
    ManagementNodeVO extraNode
    KVMHostInventory mnHost
    KVMHostInventory normalHost
    AtomicInteger shutdownCommands = new AtomicInteger()

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.noHostBasicEnv()
    }

    @Override
    void test() {
        env.create {}
        dbf = bean(DatabaseFacade.class)
        destinationMaker = bean(ResourceDestinationMakerImpl.class)
        // Lifecycle registration prevents health checks from rejoining deliberately absent ring entries.
        nodeLifeCycle = bean(ManagementNodeManagerImpl.class).nodeLifeCycle as ManagementNodeChangeListener
        prepareHostsAndPeer()
        env.simulator(KVMConstant.HOST_SHUTDOWN) { new KVMAgentCommands.ShutdownHostResponse() }
        env.afterSimulator(KVMConstant.HOST_SHUTDOWN) { rsp ->
            shutdownCommands.incrementAndGet()
            return rsp
        }

        testPeerCompletesShutdown()
        testHostUpdateSucceeds()
        testMultipleNodesShutdownSucceeds()
        testUnjoinedPeerKeepsShutdown()
        testSingleNodeShutdownSucceeds()
    }

    @Override
    void clean() {
        [peer, extraNode].findAll { it != null }.each { ManagementNodeVO mn ->
            dbf.removeByPrimaryKey(mn.uuid, ManagementNodeVO.class)
            nodeLifeCycle.nodeLeft(ManagementNodeInventory.valueOf(mn))
        }
        env.delete()
    }

    void prepareHostsAndPeer() {
        assert dbf.count(ManagementNodeVO.class) == 1 : "fixture requires one management node"
        ManagementNodeVO current = dbf.findByUuid(Platform.managementServerId, ManagementNodeVO.class)
        assert current.state == ManagementNodeState.RUNNING : "fixture management node must be running"
        peer = new ManagementNodeVO(uuid: Platform.uuid, hostName: "127.0.0.2",
                port: current.port, state: ManagementNodeState.RUNNING)

        // Choose ownership in the public hash ring, then create both hosts through the API on the real node.
        destinationMaker.nodeJoin(ManagementNodeInventory.valueOf(peer))
        String mnHostUuid = uuidOnNode(peer.uuid)
        String normalHostUuid = uuidOnNode(current.uuid)
        destinationMaker.nodeLeft(ManagementNodeInventory.valueOf(peer))
        String targetClusterUuid = env.inventoryByName("cluster").uuid
        mnHost = addKVMHost {
            name = "management-node-host"
            resourceUuid = mnHostUuid
            managementIp = peer.hostName
            username = "root"
            password = "password"
            clusterUuid = targetClusterUuid
        } as KVMHostInventory
        normalHost = addKVMHost {
            name = "normal-host"
            resourceUuid = normalHostUuid
            managementIp = current.hostName
            username = "root"
            password = "password"
            clusterUuid = targetClusterUuid
        } as KVMHostInventory

        // MN membership has no public creation API; keep this fixture alive only for the short business flow.
        peer.heartBeat = new Date().toTimestamp()
        dbf.persist(peer)
        nodeLifeCycle.nodeJoin(ManagementNodeInventory.valueOf(peer))
        assert destinationMaker.makeDestination(mnHost.uuid) == peer.uuid : "target host must belong to its own management node"
        assert destinationMaker.makeDestination(normalHost.uuid) == current.uuid : "normal host must belong to the real management node"
    }

    void testPeerCompletesShutdown() {
        assert Q.New(ManagementNodeVO.class).eq(ManagementNodeVO_.state, ManagementNodeState.RUNNING).count() == 2 : "shutdown requires two running management nodes"
        def result = shutdownHost {
            uuid = mnHost.uuid
            method = "AGENT"
            returnEarly = false
            apiId = uuidOnNode(Platform.managementServerId)
            timeout = 30000
        }

        assert result.uuid == mnHost.uuid : "shutdown must return the target host"
        assert shutdownCommands.get() == 1 : "shutdown must send one agent command: actual=${shutdownCommands.get()}"
        assert Q.New(HostVO.class).eq(HostVO_.uuid, mnHost.uuid).eq(HostVO_.status, HostStatus.Disconnected).isExists() : "shutdown must disconnect the target host"
    }

    void testHostUpdateSucceeds() {
        assert Q.New(ManagementNodeVO.class).eq(ManagementNodeVO_.state, ManagementNodeState.RUNNING).count() == 2 : "host update must retain the two-node condition"
        String updatedName = "renamed-normal-host"
        def result = updateHost {
            uuid = normalHost.uuid
            name = updatedName
            apiId = uuidOnNode(Platform.managementServerId)
            timeout = 30000
        }

        assert result.name == updatedName : "update must return the new host name"
        assert Q.New(HostVO.class).eq(HostVO_.uuid, normalHost.uuid).select(HostVO_.name).findValue() == updatedName : "update must persist the new host name"
        assert shutdownCommands.get() == 1 : "ordinary update must not send a shutdown command"
    }

    void testMultipleNodesShutdownSucceeds() {
        destinationMaker.nodeLeft(ManagementNodeInventory.valueOf(peer))
        reconnectHost {
            uuid = mnHost.uuid
            timeout = 30000
        }
        assert Q.New(HostVO.class).eq(HostVO_.uuid, mnHost.uuid).eq(HostVO_.status, HostStatus.Connected).isExists() : "reconnected MN host must be connected"
        destinationMaker.nodeJoin(ManagementNodeInventory.valueOf(peer))

        extraNode = new ManagementNodeVO(uuid: Platform.uuid, hostName: "127.0.0.250",
                heartBeat: new Date().toTimestamp(), port: peer.port, state: ManagementNodeState.RUNNING)
        dbf.persist(extraNode)
        nodeLifeCycle.nodeJoin(ManagementNodeInventory.valueOf(extraNode))
        destinationMaker.nodeLeft(ManagementNodeInventory.valueOf(extraNode))
        assert Q.New(ManagementNodeVO.class).eq(ManagementNodeVO_.state, ManagementNodeState.RUNNING).count() == 3 : "shutdown requires three running MN records"
        assert destinationMaker.makeDestination(mnHost.uuid) == peer.uuid : "MN host must still belong to its own management node"
        assert !destinationMaker.managementNodesInHashRing.contains(extraNode.uuid) : "extra MN must not be an execution candidate"

        def result = shutdownHost {
            uuid = mnHost.uuid
            method = "AGENT"
            returnEarly = false
            apiId = uuidOnNode(Platform.managementServerId)
            timeout = 30000
        }

        assert result.uuid == mnHost.uuid : "multi-node shutdown must return the target host"
        assert shutdownCommands.get() == 2 : "multi-node shutdown must send one additional agent command: actual=${shutdownCommands.get()}"
        assert Q.New(HostVO.class).eq(HostVO_.uuid, mnHost.uuid).eq(HostVO_.status, HostStatus.Disconnected).isExists() : "multi-node shutdown must disconnect the target host"
        dbf.removeByPrimaryKey(extraNode.uuid, ManagementNodeVO.class)
    }

    void testUnjoinedPeerKeepsShutdown() {
        destinationMaker.nodeLeft(ManagementNodeInventory.valueOf(peer))
        assert Q.New(ManagementNodeVO.class).eq(ManagementNodeVO_.state, ManagementNodeState.RUNNING).count() == 2 : "unjoined-peer shutdown must retain two running MN records"
        assert !destinationMaker.managementNodesInHashRing.contains(peer.uuid) : "peer must be absent from the local hash ring"
        assert Q.New(HostVO.class).eq(HostVO_.uuid, normalHost.uuid).eq(HostVO_.status, HostStatus.Connected).isExists() : "normal host must be connected before shutdown"

        def result = shutdownHost {
            uuid = normalHost.uuid
            method = "AGENT"
            returnEarly = false
            timeout = 30000
        }

        assert result.uuid == normalHost.uuid : "unjoined-peer shutdown must return the target host"
        assert shutdownCommands.get() == 3 : "unjoined-peer shutdown must send one additional agent command: actual=${shutdownCommands.get()}"
        assert Q.New(HostVO.class).eq(HostVO_.uuid, normalHost.uuid).eq(HostVO_.status, HostStatus.Disconnected).isExists() : "unjoined-peer shutdown must disconnect the target host"
    }

    void testSingleNodeShutdownSucceeds() {
        dbf.removeByPrimaryKey(peer.uuid, ManagementNodeVO.class)
        assert dbf.count(ManagementNodeVO.class) == 1 : "fallback shutdown must have no peer management node"
        reconnectHost {
            uuid = normalHost.uuid
            timeout = 30000
        }
        assert Q.New(HostVO.class).eq(HostVO_.uuid, normalHost.uuid).eq(HostVO_.status, HostStatus.Connected).isExists() : "single-node host must be connected before shutdown"
        def result = shutdownHost {
            uuid = normalHost.uuid
            method = "AGENT"
            returnEarly = false
            timeout = 30000
        }

        assert result.uuid == normalHost.uuid : "fallback shutdown must return the target host"
        assert shutdownCommands.get() == 4 : "fallback must send one additional agent command: actual=${shutdownCommands.get()}"
        assert Q.New(HostVO.class).eq(HostVO_.uuid, normalHost.uuid).eq(HostVO_.status, HostStatus.Disconnected).isExists() : "fallback shutdown must disconnect the target host"
    }

    String uuidOnNode(String nodeUuid) {
        // The single real process must own API IDs while the fixture adds a second node to the hash ring.
        for (int attempt = 0; attempt < 1000; attempt++) {
            String uuid = Platform.uuid
            if (destinationMaker.makeDestination(uuid) == nodeUuid) {
                return uuid
            }
        }
        assert false : "cannot select a fixture UUID owned by node ${nodeUuid}"
        return null
    }
}
