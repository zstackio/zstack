package org.zstack.test.integration.storage.primary.addon.zbs

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.identity.AccountConstant
import org.zstack.header.message.MessageReply
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageHostProtocolRefVO
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageHostProtocolRefVO_
import org.zstack.header.storage.addon.primary.PrimaryStorageOutputProtocolRefVO
import org.zstack.header.storage.addon.primary.PrimaryStorageOutputProtocolRefVO_
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_
import org.zstack.header.storage.primary.PrimaryStorageHostStatus
import org.zstack.header.storage.backup.UploadImageToRemoteTargetMsg
import org.zstack.header.storage.backup.UploadImageToRemoteTargetReply
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.volume.VolumeAO_
import org.zstack.header.volume.VolumeProtocol
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.header.volume.CreateVolumeMsg
import org.zstack.header.volume.CreateVolumeReply
import org.zstack.header.volume.VolumeType
import org.zstack.storage.volume.VolumeSystemTags
import org.zstack.header.volume.VolumeConstant
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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Covers the ZBS Vhost output-protocol branches of ZbsStorageController that the
 * CBD-only ZbsPrimaryStorageCase never exercised, driven purely through the SDK +
 * simulators (no DB writes, no raw messages):
 *
 *  - protocol assignment: PS defaultOutputProtocol=Vhost propagates to volumes
 *  - delete path: getActiveClients(Vhost) reached through volume trash/expunge
 *  - full VM-start chain: createVm(Vhost root) -> activate(Vhost) over
 *    CREATE_VHOST_BDEV_PATH (zbs PS agent) -> getActivePath(Vhost) -> StartVmCmd with a
 *    vhost-user-blk root volume (deviceType=vhost, installPath=the SPDK unix socket)
 *
 * Regression guard for gaps that escaped to the real environment: a missing Vhost
 * branch in getActiveClients ("not supported protocol[Vhost] for active") and the
 * activate/getActivePath path that only ran end-to-end on a live host.
 */
class ZbsVhostVolumeCase extends SubCase {
    EnvSpec env
    CloudBus bus
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
            bus = bean(CloudBus.class)

            testDefaultOutputProtocolIsVhost()
            testVhostDataVolumeCreateDeleteLifecycle()
            testChangeVolumeProtocol()
            testCreateDataVolumeWithExplicitProtocol()
            testCreateDataVolumeWithProtocolSystemTag()
            testVolumeProtocolSystemTagConsumedIntoVolume()
            testUnknownVolumeProtocolSystemTagRejected()
            testUnknownRootVolumeProtocolSystemTagRejected()
            testVhostVmStartActivationChain()
            testAddProtocolPreparesHostsAndRecordsProtocolRefs()
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

    // a PS can expose multiple output protocols; APIChangeVolumeProtocolMsg switches
    // an idle volume between them offline (persist VolumeVO.protocol; next activate
    // builds the new path). validates the target protocol is one the PS exposes.
    void testChangeVolumeProtocol() {
        // PS starts with Vhost only; add CBD so the volume can switch to it
        addStorageProtocol {
            uuid = ps.uuid
            outputProtocol = VolumeProtocol.CBD.toString()
        }

        VolumeInventory vol = createDataVolume {
            name = "switch-data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory
        assert vol.protocol == VolumeProtocol.Vhost.toString()

        changeVolumeProtocol {
            volumeUuid = vol.uuid
            protocol = VolumeProtocol.CBD.toString()
        }
        def proto = Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, vol.uuid)
                .select(VolumeAO_.protocol)
                .findValue()
        assert proto == VolumeProtocol.CBD.toString() : \
                "volume protocol not switched to CBD: actual=${proto}"

        // switching to a protocol the PS does not expose must be rejected
        expect(AssertionError.class) {
            changeVolumeProtocol {
                volumeUuid = vol.uuid
                protocol = VolumeProtocol.NBD.toString()
            }
        }

        // switching to the protocol it already uses must be rejected
        expect(AssertionError.class) {
            changeVolumeProtocol {
                volumeUuid = vol.uuid
                protocol = VolumeProtocol.CBD.toString()
            }
        }

        deleteDataVolume { uuid = vol.uuid }
        expungeDataVolume { uuid = vol.uuid }
    }

    // create-time protocol selection: an explicit protocol on the create request
    // overrides the PS default (Vhost here) and must be one the PS exposes. mirror of
    // APIChangeVolumeProtocolMsg validation so create and change agree on outputProtocols.
    void testCreateDataVolumeWithExplicitProtocol() {
        // CBD was added to the PS by testChangeVolumeProtocol; default is still Vhost.
        // asking for CBD explicitly must win over the Vhost default.
        VolumeInventory vol = createDataVolume {
            name = "explicit-cbd"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
            protocol = VolumeProtocol.CBD.toString()
        } as VolumeInventory
        assert vol.protocol == VolumeProtocol.CBD.toString() : \
                "explicit create protocol ignored: expected=CBD actual=${vol.protocol}"

        deleteDataVolume { uuid = vol.uuid }
        expungeDataVolume { uuid = vol.uuid }

        // a protocol the PS does not expose must be rejected at create, same as change
        expect(AssertionError.class) {
            createDataVolume {
                name = "explicit-nbd"
                diskOfferingUuid = diskOffering.uuid
                primaryStorageUuid = ps.uuid
                protocol = VolumeProtocol.NBD.toString()
            }
        }
    }

    // the standalone APICreateDataVolume path (handle(CreateDataVolumeMsg)) is a
    // separate entry point from VmAllocateVolumeFlow's createVolume(): it carries its
    // own systemTag consume. an ephemeral volumeProtocol::{protocol} tag on the create
    // request must be read into VolumeVO.protocol just like the explicit protocol field,
    // and must never persist as a resident tag (createTags skips ephemeral tags).
    void testCreateDataVolumeWithProtocolSystemTag() {
        // CBD is exposed on the PS (added by testChangeVolumeProtocol); the systemTag
        // asks for CBD, overriding the Vhost default exactly like the explicit field.
        String protocolTag = VolumeSystemTags.VOLUME_PROTOCOL.instantiateTag(
                [(VolumeSystemTags.VOLUME_PROTOCOL_TOKEN): VolumeProtocol.CBD.toString()])

        VolumeInventory vol = createDataVolume {
            name = "systag-data-cbd"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
            systemTags = [protocolTag]
        } as VolumeInventory
        assert vol.protocol == VolumeProtocol.CBD.toString() : \
                "systemTag protocol not consumed on standalone create: expected=CBD actual=${vol.protocol}"
        assert !VolumeSystemTags.VOLUME_PROTOCOL.hasTag(vol.uuid) : \
                "ephemeral volumeProtocol tag must not persist on ${vol.uuid}"

        deleteDataVolume { uuid = vol.uuid }
        expungeDataVolume { uuid = vol.uuid }

        // a bogus protocol token on the standalone create must be rejected at API time
        // by the same enum guard, proving the field-fallback reaches validateVolumeProtocol.
        expectApiFailure({
            createDataVolume {
                name = "systag-data-bogus"
                diskOfferingUuid = diskOffering.uuid
                primaryStorageUuid = ps.uuid
                systemTags = [VolumeSystemTags.VOLUME_PROTOCOL.instantiateTag(
                        [(VolumeSystemTags.VOLUME_PROTOCOL_TOKEN): "BOGUS"])]
            }
        }) {
            assert JSONObjectUtil.toJsonString(delegate).contains("unsupported volume protocol") : \
                    "standalone create rejected for the wrong reason: ${JSONObjectUtil.toJsonString(delegate)}"
        }
    }

    // the Cloud VM-create path provides no DiskAO, so a per-volume protocol can only
    // ride in as a volumeProtocol::{protocol} ephemeral system tag. VmAllocateVolumeFlow
    // folds those tags into the CreateVolumeMsg of each volume, and createVolume reads the
    // token into VolumeVO.protocol; being ephemeral, the framework never persists it as a
    // resident tag. drive CreateVolumeMsg directly to prove that convergence point: CBD
    // here overrides the PS Vhost default, exactly like an explicit create protocol would.
    void testVolumeProtocolSystemTagConsumedIntoVolume() {
        String protocolTag = VolumeSystemTags.VOLUME_PROTOCOL.instantiateTag(
                [(VolumeSystemTags.VOLUME_PROTOCOL_TOKEN): VolumeProtocol.CBD.toString()])

        CreateVolumeMsg msg = new CreateVolumeMsg()
        msg.setName("systag-cbd")
        msg.setSize(SizeUnit.GIGABYTE.toByte(1))
        msg.setFormat("qcow2")
        msg.setVolumeType(VolumeType.Data.toString())
        msg.setPrimaryStorageUuid(ps.uuid)
        msg.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
        msg.setSystemTags([protocolTag])
        bus.makeLocalServiceId(msg, VolumeConstant.SERVICE_ID)

        CreateVolumeReply reply = syncSend(msg) as CreateVolumeReply
        assert reply.isSuccess() : "raw CreateVolumeMsg failed: ${reply.error}"
        // reply.inventory is org.zstack.header.volume.VolumeInventory; keep it dynamic
        // so the org.zstack.sdk.* star import does not coerce it to the SDK type.
        def vol = reply.inventory
        assert vol.protocol == VolumeProtocol.CBD.toString() : \
                "systemTag protocol not consumed: expected=CBD actual=${vol.protocol}"
        assert !VolumeSystemTags.VOLUME_PROTOCOL.hasTag(vol.uuid) : \
                "volumeProtocol tag must be stripped after consume, still present on ${vol.uuid}"

        // this volume was minted NotInstantiated via a raw CreateVolumeMsg (installPath
        // null), so the normal expunge path would call deactivateAndDeleteVolume on the
        // external PS with a null installPath. drop the synthetic fixture row directly
        // instead of exercising that delete-on-PS flow.
        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, vol.uuid).hardDelete()
    }

    // the enum guard runs in VolumeApiInterceptor before any allocation flow, reading
    // the protocol tag straight off APICreateVmInstanceMsg.dataVolumeSystemTags. a bad
    // protocol token must be rejected at API time, not silently set on the volume.
    void testUnknownVolumeProtocolSystemTagRejected() {
        expectApiFailure({
            createVmInstance {
                name = "bad-protocol-vm"
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                primaryStorageUuidForRootVolume = ps.uuid
                dataVolumeSystemTags = [VolumeSystemTags.VOLUME_PROTOCOL.instantiateTag(
                        [(VolumeSystemTags.VOLUME_PROTOCOL_TOKEN): "BOGUS"])]
            }
        }) {
            // assert it failed on OUR guard, not some unrelated admission error
            assert JSONObjectUtil.toJsonString(delegate).contains("unsupported volume protocol") : \
                    "rejected for the wrong reason: ${JSONObjectUtil.toJsonString(delegate)}"
        }
    }

    // the root disk has no DiskAO either; its per-volume protocol rides in via
    // rootVolumeSystemTags. the same enum guard must scan that list (not just the data
    // tags), or a bad root protocol slips past API admission while a bad data one is
    // caught - an asymmetry the validator must not have.
    void testUnknownRootVolumeProtocolSystemTagRejected() {
        expectApiFailure({
            createVmInstance {
                name = "bad-root-protocol-vm"
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                primaryStorageUuidForRootVolume = ps.uuid
                rootVolumeSystemTags = [VolumeSystemTags.VOLUME_PROTOCOL.instantiateTag(
                        [(VolumeSystemTags.VOLUME_PROTOCOL_TOKEN): "BOGUS"])]
            }
        }) {
            assert JSONObjectUtil.toJsonString(delegate).contains("unsupported volume protocol") : \
                    "root protocol rejected for the wrong reason: ${JSONObjectUtil.toJsonString(delegate)}"
        }
    }

    // full VM-start chain. a Vhost root volume forces the framework to activate the
    // volume on the host before boot, routing through ZbsStorageController:
    //   activate(Vhost) -> httpCall(CREATE_VHOST_BDEV_PATH) on the zbs PS agent -> socketPath
    //   getActivePath(Vhost) -> VHOST_SOCKET_DIR/<bdevName>
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

        // host-connect/attach ensures the SPDK target via the zbs PS agent; register the
        // simulator before attach so deploy-on-attach is exercised, not a tolerated 404.
        env.simulator(ZbsStorageController.DEPLOY_VHOST_PATH) { HttpEntity<String> e, EnvSpec spec ->
            return new ZbsStorageController.AgentResponse()
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        // image cache copies the template onto the PS over the export protocol; ack it.
        env.message(UploadImageToRemoteTargetMsg.class) { UploadImageToRemoteTargetMsg msg, CloudBus b ->
            b.reply(msg, new UploadImageToRemoteTargetReply())
        }

        // activate(Vhost) now creates the bdev through the zbs PS agent, which SSHes to the
        // compute host via zbsadm. mirror the agent: the socket is named after the bdev under
        // VHOST_SOCKET_DIR, matching getActivePath(Vhost) = buildVhostSocketPath(installPath).
        env.simulator(ZbsStorageController.CREATE_VHOST_BDEV_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CreateVhostBdevCmd.class)
            assert cmd.bdevName != null && cmd.bdevName.startsWith(ZbsConstants.VHOST_BDEV_NAME_PREFIX) : \
                    "create-bdev cmd bdevName malformed: ${cmd.bdevName}"
            // zbsadm SSHes to the compute host, so the cmd must carry its IP + SSH creds
            assert cmd.hostIp != null : "create-bdev cmd missing target host IP for zbsadm SSH"
            assert cmd.sshPassword != null : "create-bdev cmd missing host SSH password"
            // zbsadm create-bdev opens <logicalPool>/<volume>; the controller splits the zbs
            // install path so the physical pool is not needed (no MDS round-trip on activate)
            assert cmd.logicalPool == "lpool1" : \
                    "create-bdev cmd logicalPool expected=lpool1 actual=${cmd.logicalPool}"
            assert cmd.volume != null && !cmd.volume.isEmpty() : \
                    "create-bdev cmd missing volume name: ${cmd.volume}"
            activateCalled.set(true)
            def rsp = new ZbsStorageController.CreateVhostBdevRsp()
            rsp.socketPath = ZbsConstants.VHOST_SOCKET_DIR + "/" + cmd.bdevName
            return rsp
        }

        // vm destroy deactivates the vhost volume (delete-bdev via the zbs PS agent);
        // without a handler the 404 makes the destroy teardown flaky
        env.simulator(ZbsStorageController.DELETE_VHOST_BDEV_PATH) { HttpEntity<String> e, EnvSpec spec ->
            return new ZbsStorageController.AgentResponse()
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
                "activate(Vhost) over CREATE_VHOST_BDEV_PATH was never invoked during VM start"
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

    // adding an output protocol on a PS with connected hosts must prepare every
    // host for the protocol (Vhost -> deploy the SPDK target via the zbs PS agent
    // over DEPLOY_VHOST_PATH) and record per-protocol connectivity rows that the
    // frontend reads through QueryExternalPrimaryStorageHostProtocolRef. the
    // host-level ref row keeps the legacy folded all-protocol semantics.
    void testAddProtocolPreparesHostsAndRecordsProtocolRefs() {
        HostInventory host = env.inventoryByName("kvm-1") as HostInventory

        AtomicBoolean ensureCalled = new AtomicBoolean(false)
        AtomicBoolean vhostTargetHealthy = new AtomicBoolean(true)

        env.simulator(ZbsStorageController.DEPLOY_VHOST_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.DeployVhostCmd.class)
            assert cmd.hostIp != null : "deploy cmd missing target host IP for zbsadm SSH"
            ensureCalled.set(true)
            return new ZbsStorageController.AgentResponse()
        }
        env.simulator(ZbsStorageController.VHOST_TARGET_HEALTH_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new ZbsStorageController.VhostTargetHealthRsp()
            rsp.targetRunning = vhostTargetHealthy.get()
            return rsp
        }
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

        // mimic a storage from before vhost support: drop the Vhost protocol row,
        // then add it back through the API, which must prepare the connected hosts
        SQL.New(PrimaryStorageOutputProtocolRefVO.class)
                .eq(PrimaryStorageOutputProtocolRefVO_.primaryStorageUuid, ps.uuid)
                .eq(PrimaryStorageOutputProtocolRefVO_.outputProtocol, VolumeProtocol.Vhost.toString())
                .delete()

        addStorageProtocol {
            uuid = ps.uuid
            outputProtocol = VolumeProtocol.Vhost.toString()
        }

        assert ensureCalled.get() : \
                "addStorageProtocol(Vhost) did not reach DEPLOY_VHOST_PATH on the zbs PS agent"

        // the protocol row write is fire-and-forget behind the PS queue
        retryInSecs {
            assert Q.New(ExternalPrimaryStorageHostProtocolRefVO.class)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.primaryStorageUuid, ps.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.hostUuid, host.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.protocol, VolumeProtocol.Vhost.toString())
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.status, PrimaryStorageHostStatus.Connected)
                    .isExists()
        }

        // frontend contract: per-protocol connectivity is queryable
        def refs = queryExternalPrimaryStorageHostProtocolRef {
            conditions = ["primaryStorageUuid=${ps.uuid}".toString()]
        } as List
        assert refs.find { it.protocol == VolumeProtocol.Vhost.toString() && it.hostUuid == host.uuid } != null : \
                "query api returned no Vhost connectivity row: ${refs}"

        // periodic pings drive the per-protocol health reports; shorten the
        // interval so the status flips below land within the retry windows
        updateGlobalConfig {
            category = "host"
            name = "ping.interval"
            value = 1
        }

        // every reported protocol gets its own connectivity row on ping
        retryInSecs(30) {
            assert Q.New(ExternalPrimaryStorageHostProtocolRefVO.class)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.primaryStorageUuid, ps.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.hostUuid, host.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.protocol, VolumeProtocol.CBD.toString())
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.status, PrimaryStorageHostStatus.Connected)
                    .isExists()
        }

        // a dead vhost target flips its own protocol row while the CBD row keeps
        // its own state; the host-level row folds all protocols (legacy
        // semantics) so it goes Disconnected too
        vhostTargetHealthy.set(false)
        retryInSecs(30) {
            assert Q.New(ExternalPrimaryStorageHostProtocolRefVO.class)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.primaryStorageUuid, ps.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.hostUuid, host.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.protocol, VolumeProtocol.Vhost.toString())
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                    .isExists()
        }
        assert Q.New(ExternalPrimaryStorageHostProtocolRefVO.class)
                .eq(ExternalPrimaryStorageHostProtocolRefVO_.primaryStorageUuid, ps.uuid)
                .eq(ExternalPrimaryStorageHostProtocolRefVO_.hostUuid, host.uuid)
                .eq(ExternalPrimaryStorageHostProtocolRefVO_.protocol, VolumeProtocol.CBD.toString())
                .eq(ExternalPrimaryStorageHostProtocolRefVO_.status, PrimaryStorageHostStatus.Connected)
                .isExists() : "the CBD row must keep its own state independent of the vhost target"
        retryInSecs(30) {
            assert Q.New(PrimaryStorageHostRefVO.class)
                    .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps.uuid)
                    .eq(PrimaryStorageHostRefVO_.hostUuid, host.uuid)
                    .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                    .isExists()
        }

        // the target recovers: rows self-heal on the next report
        vhostTargetHealthy.set(true)
        retryInSecs(30) {
            assert Q.New(ExternalPrimaryStorageHostProtocolRefVO.class)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.primaryStorageUuid, ps.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.hostUuid, host.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.protocol, VolumeProtocol.Vhost.toString())
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.status, PrimaryStorageHostStatus.Connected)
                    .isExists()
            assert Q.New(PrimaryStorageHostRefVO.class)
                    .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, ps.uuid)
                    .eq(PrimaryStorageHostRefVO_.hostUuid, host.uuid)
                    .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Connected)
                    .isExists()
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }
    }

    private MessageReply syncSend(org.zstack.header.message.Message msg) {
        AtomicReference<MessageReply> ref = new AtomicReference<>()
        CountDownLatch done = new CountDownLatch(1)
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                ref.set(reply)
                done.countDown()
            }
        })
        assert done.await(30, TimeUnit.SECONDS) : "timed out waiting for ${msg.class.simpleName} reply"
        return ref.get()
    }
}
