package org.zstack.test.integration.storage.primary.addon.zbs

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.storage.addon.primary.PrimaryStorageOutputProtocolRefVO
import org.zstack.header.storage.addon.primary.PrimaryStorageOutputProtocolRefVO_
import org.zstack.header.storage.backup.UploadImageToRemoteTargetMsg
import org.zstack.header.storage.backup.UploadImageToRemoteTargetReply
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.volume.VolumeAO_
import org.zstack.header.volume.VolumeProtocol
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.VolumeTO
import org.zstack.sdk.*
import org.zstack.storage.zbs.ZbsConstants
import org.zstack.storage.zbs.ZbsStorageController
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Covers the ZBS Vhost output-protocol branches of ZbsStorageController that the
 * CBD-only ZbsPrimaryStorageCase never exercised, driven purely through the SDK +
 * simulators (no DB writes, no raw messages):
 *
 *  - protocol assignment: PS defaultOutputProtocol=Vhost propagates to volumes
 *  - delete path: getActiveClients(Vhost) reached through volume trash/expunge
 *  - full VM-start chain: createVm(Vhost root) -> activate(Vhost) over
 *    VHOST_ACTIVATE_PATH -> getActivePath(Vhost) -> StartVmCmd with a vhost-user-blk
 *    root volume (deviceType=vhost, installPath=the SPDK unix socket)
 *
 * Regression guard for gaps that escaped to the real environment: a missing Vhost
 * branch in getActiveClients ("not supported protocol[Vhost] for active") and the
 * activate/getActivePath path that only ran end-to-end on a live host.
 */
class ZbsVhostVolumeCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory ps
    DiskOfferingInventory diskOffering
    ClusterInventory cluster
    InstanceOfferingInventory instanceOffering
    ImageInventory image
    L3NetworkInventory l3

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(2)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "127.0.0.2"

                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                    size = SizeUnit.GIGABYTE.toByte(1)
                    virtio = true
                }
            }

            zone {
                name = "zone"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm-1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
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

                externalPrimaryStorage {
                    name = "zbs-vhost"
                    identity = "zbs"
                    defaultOutputProtocol = "Vhost"
                    config = "{\"mdsUrls\":[\"root:password@127.0.1.1\",\"root:password@127.0.1.2\",\"root:password@127.0.1.3\"],\"logicalPoolName\":\"lpool1\"}"
                    url = "zbs"
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            ps = env.inventoryByName("zbs-vhost") as PrimaryStorageInventory
            diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
            cluster = env.inventoryByName("cluster") as ClusterInventory
            instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
            image = env.inventoryByName("image") as ImageInventory
            l3 = env.inventoryByName("l3") as L3NetworkInventory

            testDefaultOutputProtocolIsVhost()
            testVhostDataVolumeCreateDeleteLifecycle()
            testVhostVmStartActivationChain()
        }
    }

    void testDefaultOutputProtocolIsVhost() {
        assert Q.New(PrimaryStorageOutputProtocolRefVO.class)
                .eq(PrimaryStorageOutputProtocolRefVO_.primaryStorageUuid, ps.uuid)
                .eq(PrimaryStorageOutputProtocolRefVO_.outputProtocol, VolumeProtocol.Vhost.toString())
                .isExists()
    }

    // create a Vhost data volume and delete it. delete -> trashVolume ->
    // ExternalPrimaryStorage.deactivateAndDeleteVolume -> node.getActiveClients(path, "Vhost").
    // a Vhost volume is opened on the host by the SPDK target over the same cbd
    // backend, so the MDS tracks it as a client exactly like CBD. getActiveClients
    // MUST query the MDS GET_VOLUME_CLIENTS_PATH (not short-circuit to an empty list),
    // or in-use detection / HA fencing goes blind to vhost volumes.
    void testVhostDataVolumeCreateDeleteLifecycle() {
        boolean getClientsCalledForVhost = false
        String clientIp = "127.0.0.1"
        env.simulator(ZbsStorageController.GET_VOLUME_CLIENTS_PATH) { HttpEntity<String> e, EnvSpec spec ->
            getClientsCalledForVhost = true
            def rsp = new ZbsStorageController.GetVolumeClientsRsp()
            rsp.clients = [new ZbsStorageController.ClientInfo(clientIp, 9001)]
            return rsp
        }

        VolumeInventory vol = createDataVolume {
            name = "vhost-data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        assert vol.protocol == VolumeProtocol.Vhost.toString()
        assert vol.installPath.startsWith(ZbsConstants.SCHEME_PREFIX)

        // the delete path invokes getActiveClients(Vhost). it must reach the MDS
        // clients endpoint (same as CBD) rather than resolve to a local empty list.
        deleteDataVolume {
            uuid = vol.uuid
        }
        expungeDataVolume {
            uuid = vol.uuid
        }

        assert getClientsCalledForVhost : \
                "getActiveClients(Vhost) did not query the MDS GET_VOLUME_CLIENTS_PATH; " +
                "the SPDK target registers as a cbd client and must be enumerated like CBD, not short-circuited to empty"
        assert !Q.New(org.zstack.header.volume.VolumeVO.class)
                .eq(org.zstack.header.volume.VolumeVO_.uuid, vol.uuid)
                .isExists()
    }

    // full VM-start chain. a Vhost root volume forces the framework to activate the
    // volume on the host before boot, routing through ZbsStorageController:
    //   activate(Vhost) -> KVMHostAsyncHttpCallMsg(VHOST_ACTIVATE_PATH) -> socketPath
    //   getActivePath(Vhost) -> VHOST_SOCKET_DIR/<controllerName>
    // and landing a vhost-user-blk disk in StartVmCmd. this path only ran end-to-end
    // on a live host before; the unit case keeps it covered.
    void testVhostVmStartActivationChain() {
        AtomicBoolean activateCalled = new AtomicBoolean(false)
        AtomicBoolean startedWithVhostRoot = new AtomicBoolean(false)

        // host allocation for a VM needs the addon PS attached to the cluster, which
        // prepares hosts (deploy client + per-pool heartbeat volume). give the
        // heartbeat volume create a concrete installPath as the real MDS would.
        env.afterSimulator(ZbsStorageController.CREATE_VOLUME_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CreateVolumeCmd)
            if (cmd.volume == ZbsConstants.ZBS_HEARTBEAT_VOLUME_NAME) {
                def vrsp = new ZbsStorageController.CreateVolumeRsp()
                vrsp.installPath = "zbs://${cmd.logicalPool}/${cmd.volume}".toString()
                return vrsp
            }
            return rsp
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        // image cache copies the template onto the PS over the export protocol; ack it.
        env.message(UploadImageToRemoteTargetMsg.class) { UploadImageToRemoteTargetMsg msg, CloudBus b ->
            b.reply(msg, new UploadImageToRemoteTargetReply())
        }

        // the activate request the controller sends to the host SPDK target plugin.
        // mirror the agent: socketPath = socketDir/controllerName, so it matches
        // getActivePath(Vhost) = buildVhostSocketPath(installPath).
        env.simulator(ZbsStorageController.VHOST_ACTIVATE_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.VhostActivateCmd.class)
            assert cmd.controllerName != null && cmd.controllerName.startsWith(ZbsConstants.VHOST_CONTROLLER_NAME_PREFIX) : \
                    "activate cmd controllerName malformed: ${cmd.controllerName}"
            assert cmd.socketDir == ZbsConstants.VHOST_SOCKET_DIR : \
                    "activate cmd socketDir expected=${ZbsConstants.VHOST_SOCKET_DIR} actual=${cmd.socketDir}"
            activateCalled.set(true)
            def rsp = new ZbsStorageController.VhostActivateRsp()
            rsp.socketPath = cmd.socketDir + "/" + cmd.controllerName
            return rsp
        }

        // end of the chain: the VM boots with a vhost-user-blk root disk whose path is
        // the SPDK unix socket. assert the TO the framework built from getActivePath.
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            assert cmd.rootVolume.deviceType == VolumeTO.VHOST : \
                    "root volume not vhost-user-blk: deviceType=${cmd.rootVolume.deviceType} (expected ${VolumeTO.VHOST})"
            assert cmd.rootVolume.installPath != null &&
                    cmd.rootVolume.installPath.startsWith(ZbsConstants.VHOST_SOCKET_DIR) : \
                    "root volume installPath not the SPDK socket: ${cmd.rootVolume.installPath} (expected under ${ZbsConstants.VHOST_SOCKET_DIR})"
            startedWithVhostRoot.set(true)
            return rsp
        }

        VmInstanceInventory vm = createVmInstance {
            name = "vhost-vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            primaryStorageUuidForRootVolume = ps.uuid
        } as VmInstanceInventory

        assert vm.state == VmInstanceState.Running.toString() : \
                "vhost VM did not reach Running: state=${vm.state}"
        assert activateCalled.get() : \
                "activate(Vhost) over VHOST_ACTIVATE_PATH was never invoked during VM start"
        assert startedWithVhostRoot.get() : \
                "StartVmCmd was not built with a vhost-user-blk root volume"

        def proto = Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, vm.rootVolumeUuid)
                .select(VolumeAO_.protocol)
                .findValue()
        assert proto == VolumeProtocol.Vhost.toString() : \
                "root volume protocol expected=Vhost actual=${proto}"

        // tear down the imperative state (VM + PS-cluster attachment) created by this
        // test. env.delete() only knows the env-spec nodes; an attached PS or a VM
        // left on it makes deletePrimaryStorage fail during clean().
        destroyVmInstance {
            uuid = vm.uuid
        }
        expungeVmInstance {
            uuid = vm.uuid
        }
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }
    }
}
