package org.zstack.test.integration.storage.primary.addon.zbs

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostSystemTags
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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class ZbsVhostVolumeCase extends SubCase {
    private static final String HOST_MANAGEMENT_IP = "127.0.0.1"
    private static final String HOST_EXTRA_IP_1 = "127.0.0.99"
    private static final String HOST_EXTRA_IP_2 = "127.0.0.11"
    private static final String UNKNOWN_CLIENT_IP = "127.0.0.9"
    private static final String SECOND_HOST_MANAGEMENT_IP = "127.0.0.20"
    private static final int VHOST_CLIENT_PORT = 9001

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
                        managementIp = HOST_MANAGEMENT_IP
                        username = "root"
                        password = "password"
                        systemTags = [HostSystemTags.EXTRA_IPS.instantiateTag([
                                (HostSystemTags.EXTRA_IPS_TOKEN): "${HOST_EXTRA_IP_1},${HOST_EXTRA_IP_2}".toString()
                        ])]
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
            testProtocolRecoveryBackoff()
            testReconnectSkipsConnectedProtocolDeploy()
        }
    }

    void testDefaultOutputProtocolIsVhost() {
        assert Q.New(PrimaryStorageOutputProtocolRefVO.class)
                .eq(PrimaryStorageOutputProtocolRefVO_.primaryStorageUuid, ps.uuid)
                .eq(PrimaryStorageOutputProtocolRefVO_.outputProtocol, VolumeProtocol.Vhost.toString())
                .isExists()
    }

    void testVhostDataVolumeCreateDeleteLifecycle() {
        AtomicReference<String> trackedInstallPath = new AtomicReference<>()
        AtomicReference<String> activeClientIp = new AtomicReference<>()
        AtomicReference<String> deleteBdevTargetIp = new AtomicReference<>()
        AtomicBoolean getClientsCalled = new AtomicBoolean(false)
        AtomicBoolean deleteBdevCalled = new AtomicBoolean(false)
        AtomicBoolean deleteVolumeCalled = new AtomicBoolean(false)
        AtomicInteger callSequence = new AtomicInteger(0)
        AtomicInteger deleteBdevOrder = new AtomicInteger(0)
        AtomicInteger deleteVolumeOrder = new AtomicInteger(0)

        env.simulator(ZbsStorageController.GET_VOLUME_CLIENTS_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new ZbsStorageController.GetVolumeClientsRsp()
            if (trackedInstallPath.get() != null) {
                getClientsCalled.set(true)
                if (activeClientIp.get() != null) {
                    rsp.clients = [new ZbsStorageController.ClientInfo(activeClientIp.get(), VHOST_CLIENT_PORT)]
                }
            }
            return rsp
        }

        env.simulator(ZbsStorageController.DELETE_VHOST_BDEV_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.DeleteVhostBdevCmd.class)
            if (trackedInstallPath.get() != null) {
                deleteBdevCalled.set(true)
                deleteBdevTargetIp.set(cmd.hostIp)
                deleteBdevOrder.compareAndSet(0, callSequence.incrementAndGet())
            }
            return new ZbsStorageController.AgentResponse()
        }

        env.preSimulator(ZbsStorageController.DELETE_VOLUME_PATH) { HttpEntity<String> e ->
            if (trackedInstallPath.get() != null) {
                deleteVolumeCalled.set(true)
                deleteVolumeOrder.compareAndSet(0, callSequence.incrementAndGet())
            }
        }

        Closure trackClient = { VolumeInventory volume, String clientIp ->
            trackedInstallPath.set(volume.installPath)
            activeClientIp.set(clientIp)
            deleteBdevTargetIp.set(null)
            getClientsCalled.set(false)
            deleteBdevCalled.set(false)
            deleteVolumeCalled.set(false)
            callSequence.set(0)
            deleteBdevOrder.set(0)
            deleteVolumeOrder.set(0)
        }

        Closure expungeWithResolvedClient = { String volumeName, String clientIp, String clientKind ->
            VolumeInventory volume = createDataVolume {
                name = volumeName
                diskOfferingUuid = diskOffering.uuid
                primaryStorageUuid = ps.uuid
            } as VolumeInventory
            assert volume.protocol == VolumeProtocol.Vhost.toString() : \
                    "${clientKind} lifecycle created the wrong volume protocol: " +
                    "expected=${VolumeProtocol.Vhost} actual=${volume.protocol} volumeUuid=${volume.uuid}"
            assert volume.installPath.startsWith(ZbsConstants.SCHEME_PREFIX) : \
                    "${clientKind} lifecycle created a malformed ZBS installPath: " +
                    "expectedPrefix=${ZbsConstants.SCHEME_PREFIX} actual=${volume.installPath} volumeUuid=${volume.uuid}"

            trackClient(volume, clientIp)
            deleteDataVolume { uuid = volume.uuid }
            expungeDataVolume { uuid = volume.uuid }

            assert getClientsCalled.get() : \
                    "GET_VOLUME_CLIENTS_PATH was not called for ${clientKind}: " +
                    "clientIp=${clientIp} installPath=${volume.installPath}"
            assert deleteBdevCalled.get() : \
                    "DELETE_VHOST_BDEV_PATH was not called for ${clientKind}: " +
                    "clientIp=${clientIp} installPath=${volume.installPath}"
            assert deleteBdevTargetIp.get() == HOST_MANAGEMENT_IP : \
                    "DELETE_VHOST_BDEV_PATH targeted the wrong host IP for ${clientKind}: " +
                    "expectedManagementIp=${HOST_MANAGEMENT_IP} actual=${deleteBdevTargetIp.get()} clientIp=${clientIp}"
            assert deleteVolumeCalled.get() : \
                    "DELETE_VOLUME_PATH was not called after deactivating ${clientKind}: " +
                    "clientIp=${clientIp} installPath=${volume.installPath}"
            assert deleteBdevOrder.get() > 0 && deleteBdevOrder.get() < deleteVolumeOrder.get() : \
                    "Vhost bdev was not deleted before the ZBS volume for ${clientKind}: " +
                    "bdevDeleteOrder=${deleteBdevOrder.get()} volumeDeleteOrder=${deleteVolumeOrder.get()} " +
                    "clientIp=${clientIp} installPath=${volume.installPath}"
            boolean volumeExists = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.uuid).isExists()
            assert !volumeExists : \
                    "expunged ${clientKind} volume still exists: expectedExists=false actualExists=${volumeExists} " +
                    "volumeUuid=${volume.uuid}"

            activeClientIp.set(null)
            trackedInstallPath.set(null)
        }

        Closure expungeWithUnresolvedClient = { String volumeName, String clientIp, String clientKind ->
            VolumeInventory volume = createDataVolume {
                name = volumeName
                diskOfferingUuid = diskOffering.uuid
                primaryStorageUuid = ps.uuid
            } as VolumeInventory
            trackClient(volume, clientIp)
            deleteDataVolume { uuid = volume.uuid }

            AssertionError expungeFailure = null
            try {
                expungeDataVolume { uuid = volume.uuid }
            } catch (AssertionError failure) {
                expungeFailure = failure
            }

            assert expungeFailure != null : \
                    "Vhost expunge did not fail closed for ${clientKind}: clientIp=${clientIp} " +
                    "installPath=${volume.installPath} physicalVolumeDeleted=${deleteVolumeCalled.get()}"
            assert getClientsCalled.get() : \
                    "GET_VOLUME_CLIENTS_PATH was not called before rejecting ${clientKind}: " +
                    "clientIp=${clientIp} installPath=${volume.installPath}"
            assert !deleteBdevCalled.get() : \
                    "DELETE_VHOST_BDEV_PATH must not target an unresolved ${clientKind}: " +
                    "clientIp=${clientIp} actualTargetIp=${deleteBdevTargetIp.get()}"
            assert !deleteVolumeCalled.get() : \
                    "DELETE_VOLUME_PATH must be blocked when ${clientKind} cannot resolve uniquely: " +
                    "clientIp=${clientIp} installPath=${volume.installPath}"
            boolean volumeExists = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.uuid).isExists()
            assert volumeExists : \
                    "fail-closed expunge removed the volume record for ${clientKind}: " +
                    "expectedExists=true actualExists=${volumeExists} volumeUuid=${volume.uuid}"

            activeClientIp.set(null)
            deleteVolumeCalled.set(false)
            expungeDataVolume { uuid = volume.uuid }
            assert deleteVolumeCalled.get() : \
                    "cleanup expunge did not call DELETE_VOLUME_PATH after clearing ${clientKind}: " +
                    "installPath=${volume.installPath}"
            boolean volumeExistsAfterCleanup = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.uuid).isExists()
            assert !volumeExistsAfterCleanup : \
                    "cleanup expunge left the ${clientKind} volume behind: " +
                    "expectedExists=false actualExists=${volumeExistsAfterCleanup} volumeUuid=${volume.uuid}"

            trackedInstallPath.set(null)
        }

        expungeWithResolvedClient("vhost-extra-ip-data", HOST_EXTRA_IP_2, "host extra IP client")
        expungeWithResolvedClient("vhost-management-ip-data", HOST_MANAGEMENT_IP, "host management IP client")
        expungeWithUnresolvedClient("vhost-unknown-ip-data", UNKNOWN_CLIENT_IP, "unknown client IP")

        HostInventory secondHost = addKVMHost {
            name = "kvm-shared-extra-ip"
            managementIp = SECOND_HOST_MANAGEMENT_IP
            username = "root"
            password = "password"
            clusterUuid = cluster.uuid
            systemTags = [HostSystemTags.EXTRA_IPS.instantiateTag([
                    (HostSystemTags.EXTRA_IPS_TOKEN): HOST_EXTRA_IP_2
            ])]
        } as HostInventory
        try {
            expungeWithUnresolvedClient("vhost-ambiguous-ip-data", HOST_EXTRA_IP_2, "ambiguous client IP")
        } finally {
            activeClientIp.set(null)
            trackedInstallPath.set(null)
            deleteHost { uuid = secondHost.uuid }
        }
    }

    void testChangeVolumeProtocol() {
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

        expect(AssertionError.class) {
            changeVolumeProtocol {
                volumeUuid = vol.uuid
                protocol = VolumeProtocol.NBD.toString()
            }
        }

        expect(AssertionError.class) {
            changeVolumeProtocol {
                volumeUuid = vol.uuid
                protocol = VolumeProtocol.CBD.toString()
            }
        }

        deleteDataVolume { uuid = vol.uuid }
        expungeDataVolume { uuid = vol.uuid }
    }

    void testCreateDataVolumeWithExplicitProtocol() {
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

        expect(AssertionError.class) {
            createDataVolume {
                name = "explicit-nbd"
                diskOfferingUuid = diskOffering.uuid
                primaryStorageUuid = ps.uuid
                protocol = VolumeProtocol.NBD.toString()
            }
        }
    }

    void testCreateDataVolumeWithProtocolSystemTag() {
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
        def vol = reply.inventory
        assert vol.protocol == VolumeProtocol.CBD.toString() : \
                "systemTag protocol not consumed: expected=CBD actual=${vol.protocol}"
        assert !VolumeSystemTags.VOLUME_PROTOCOL.hasTag(vol.uuid) : \
                "volumeProtocol tag must be stripped after consume, still present on ${vol.uuid}"

        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, vol.uuid).hardDelete()
    }

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
            assert JSONObjectUtil.toJsonString(delegate).contains("unsupported volume protocol") : \
                    "rejected for the wrong reason: ${JSONObjectUtil.toJsonString(delegate)}"
        }
    }

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

    void testVhostVmStartActivationChain() {
        AtomicBoolean activateCalled = new AtomicBoolean(false)
        AtomicBoolean startedWithVhostRoot = new AtomicBoolean(false)

        env.afterSimulator(ZbsStorageController.CREATE_VOLUME_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CreateVolumeCmd)
            if (cmd.volume == ZbsConstants.ZBS_HEARTBEAT_VOLUME_NAME) {
                def vrsp = new ZbsStorageController.CreateVolumeRsp()
                vrsp.installPath = "zbs://${cmd.logicalPool}/${cmd.volume}".toString()
                return vrsp
            }
            return rsp
        }

        env.simulator(ZbsStorageController.DEPLOY_VHOST_PATH) { HttpEntity<String> e, EnvSpec spec ->
            return new ZbsStorageController.AgentResponse()
        }
        env.simulator(ZbsStorageController.PREPARE_VHOST_TARGET_ENV_PATH) { HttpEntity<String> e, EnvSpec spec ->
            return new ZbsStorageController.AgentResponse()
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        env.message(UploadImageToRemoteTargetMsg.class) { UploadImageToRemoteTargetMsg msg, CloudBus b ->
            b.reply(msg, new UploadImageToRemoteTargetReply())
        }

        env.simulator(ZbsStorageController.CREATE_VHOST_BDEV_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CreateVhostBdevCmd.class)
            assert cmd.bdevName != null && cmd.bdevName.startsWith(ZbsConstants.VHOST_BDEV_NAME_PREFIX) : \
                    "create-bdev cmd bdevName malformed: ${cmd.bdevName}"
            assert cmd.hostIp != null : "create-bdev cmd missing target host IP for zbsadm SSH"
            assert cmd.sshPassword != null : "create-bdev cmd missing host SSH password"
            assert cmd.logicalPool == "lpool1" : \
                    "create-bdev cmd logicalPool expected=lpool1 actual=${cmd.logicalPool}"
            assert cmd.volume != null && !cmd.volume.isEmpty() : \
                    "create-bdev cmd missing volume name: ${cmd.volume}"
            activateCalled.set(true)
            def rsp = new ZbsStorageController.CreateVhostBdevRsp()
            rsp.socketPath = ZbsConstants.VHOST_SOCKET_DIR + "/" + cmd.bdevName
            return rsp
        }

        env.simulator(ZbsStorageController.DELETE_VHOST_BDEV_PATH) { HttpEntity<String> e, EnvSpec spec ->
            return new ZbsStorageController.AgentResponse()
        }

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

        createSystemTag {
            resourceUuid = image.uuid
            resourceType = org.zstack.header.image.ImageVO.getSimpleName()
            tag = "bootMode::UEFI"
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

    void testAddProtocolPreparesHostsAndRecordsProtocolRefs() {
        HostInventory host = env.inventoryByName("kvm-1") as HostInventory

        AtomicBoolean ensureCalled = new AtomicBoolean(false)
        AtomicBoolean vhostTargetHealthy = new AtomicBoolean(true)
        AtomicBoolean redeployedWhileDown = new AtomicBoolean(false)

        env.simulator(ZbsStorageController.DEPLOY_VHOST_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.DeployVhostCmd.class)
            assert cmd.hostIp != null : "deploy cmd missing target host IP for zbsadm SSH"
            assert cmd.hugepageSize == ZbsConstants.VHOST_TARGET_HUGEPAGE_SIZE_MB : \
                    "deploy cmd should pin zbsadm vhost memory size"
            assert cmd.hugepageDir == null : \
                    "deploy should choose the 2MB hugepage mount on the target host"
            ensureCalled.set(true)
            if (!vhostTargetHealthy.get()) {
                redeployedWhileDown.set(true)
            }
            return new ZbsStorageController.AgentResponse()
        }
        env.simulator(ZbsStorageController.PREPARE_VHOST_TARGET_ENV_PATH) { HttpEntity<String> e, EnvSpec spec ->
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

        retryInSecs {
            assert Q.New(ExternalPrimaryStorageHostProtocolRefVO.class)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.primaryStorageUuid, ps.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.hostUuid, host.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.protocol, VolumeProtocol.Vhost.toString())
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.status, PrimaryStorageHostStatus.Connected)
                    .isExists()
        }

        def refs = queryExternalPrimaryStorageHostProtocolRef {
            conditions = ["primaryStorageUuid=${ps.uuid}".toString()]
        } as List
        assert refs.find { it.protocol == VolumeProtocol.Vhost.toString() && it.hostUuid == host.uuid } != null : \
                "query api returned no Vhost connectivity row: ${refs}"

        updateGlobalConfig {
            category = "host"
            name = "ping.interval"
            value = 1
        }

        retryInSecs(30) {
            assert Q.New(ExternalPrimaryStorageHostProtocolRefVO.class)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.primaryStorageUuid, ps.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.hostUuid, host.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.protocol, VolumeProtocol.CBD.toString())
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.status, PrimaryStorageHostStatus.Connected)
                    .isExists()
        }

        redeployedWhileDown.set(false)
        vhostTargetHealthy.set(false)
        retryInSecs(30) {
            assert Q.New(ExternalPrimaryStorageHostProtocolRefVO.class)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.primaryStorageUuid, ps.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.hostUuid, host.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.protocol, VolumeProtocol.Vhost.toString())
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                    .isExists()
        }

        retryInSecs(30) {
            assert redeployedWhileDown.get() : \
                    "periodic ping did not self-heal: DEPLOY_VHOST_PATH not re-issued while vhost target was down"
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
                    .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Connected)
                    .isExists() : "host-level ref stays Connected while CBD is up: aggregate is any-protocol-connected, not all"
        }

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

    void testProtocolRecoveryBackoff() {
        HostInventory host = env.inventoryByName("kvm-1") as HostInventory

        AtomicBoolean vhostDown = new AtomicBoolean(false)
        Set<Long> redeploySeconds = Collections.synchronizedSet(new HashSet<Long>())

        env.simulator(ZbsStorageController.DEPLOY_VHOST_PATH) { HttpEntity<String> e, EnvSpec spec ->
            if (vhostDown.get()) {
                redeploySeconds.add((System.currentTimeMillis() / 1000) as Long)
            }
            throw new RuntimeException("vhost target redeploy fails on purpose")
        }
        env.simulator(ZbsStorageController.PREPARE_VHOST_TARGET_ENV_PATH) { HttpEntity<String> e, EnvSpec spec ->
            return new ZbsStorageController.AgentResponse()
        }
        env.simulator(ZbsStorageController.VHOST_TARGET_HEALTH_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new ZbsStorageController.VhostTargetHealthRsp()
            rsp.targetRunning = !vhostDown.get()
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

        updateGlobalConfig {
            category = "host"
            name = "ping.interval"
            value = 1
        }

        updateGlobalConfig {
            category = "externalPrimaryStorage"
            name = "attach.hostDeployFailureRatioThreshold"
            value = 1
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        retryInSecs(30) {
            assert Q.New(ExternalPrimaryStorageHostProtocolRefVO.class)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.primaryStorageUuid, ps.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.hostUuid, host.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.protocol, VolumeProtocol.Vhost.toString())
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.status, PrimaryStorageHostStatus.Connected)
                    .isExists()
        }

        long downStartMs = System.currentTimeMillis()
        vhostDown.set(true)

        retryInSecs(30) {
            assert !redeploySeconds.isEmpty() : "framework never re-drove recovery for the down vhost target"
        }

        Thread.sleep(24000)
        long downSecs = (System.currentTimeMillis() - downStartMs) / 1000
        assert downSecs >= 20 : "down window too short to judge backoff: ${downSecs}s"
        assert redeploySeconds.size() >= 2 : \
                "framework did not retry the still-down target: seconds=${redeploySeconds}"
        assert redeploySeconds.size() <= 12 : \
                "framework recovery backoff did not throttle: redeployed in ${redeploySeconds.size()} " +
                "distinct seconds over ${downSecs}s down (doubling window should keep it sparse): ${redeploySeconds}"

        vhostDown.set(false)
        retryInSecs(30) {
            assert Q.New(ExternalPrimaryStorageHostProtocolRefVO.class)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.primaryStorageUuid, ps.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.hostUuid, host.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.protocol, VolumeProtocol.Vhost.toString())
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.status, PrimaryStorageHostStatus.Connected)
                    .isExists()
        }

        redeploySeconds.clear()
        vhostDown.set(true)
        retryInSecs(15) {
            assert !redeploySeconds.isEmpty() : \
                    "recovery did not reset the backoff: a fresh outage did not redeploy within 15s"
        }
        vhostDown.set(false)
        retryInSecs(30) {
            assert Q.New(ExternalPrimaryStorageHostProtocolRefVO.class)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.primaryStorageUuid, ps.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.hostUuid, host.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.protocol, VolumeProtocol.Vhost.toString())
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.status, PrimaryStorageHostStatus.Connected)
                    .isExists()
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }
    }

    void testReconnectSkipsConnectedProtocolDeploy() {
        HostInventory host = env.inventoryByName("kvm-1") as HostInventory
        AtomicInteger deployCount = new AtomicInteger(0)

        env.simulator(ZbsStorageController.DEPLOY_VHOST_PATH) { HttpEntity<String> e, EnvSpec spec ->
            deployCount.incrementAndGet()
            return new ZbsStorageController.AgentResponse()
        }
        env.simulator(ZbsStorageController.PREPARE_VHOST_TARGET_ENV_PATH) { HttpEntity<String> e, EnvSpec spec ->
            return new ZbsStorageController.AgentResponse()
        }
        env.simulator(ZbsStorageController.VHOST_TARGET_HEALTH_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new ZbsStorageController.VhostTargetHealthRsp()
            rsp.targetRunning = true
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

        retryInSecs {
            assert Q.New(ExternalPrimaryStorageHostProtocolRefVO.class)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.primaryStorageUuid, ps.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.hostUuid, host.uuid)
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.protocol, VolumeProtocol.Vhost.toString())
                    .eq(ExternalPrimaryStorageHostProtocolRefVO_.status, PrimaryStorageHostStatus.Connected)
                    .isExists()
        }

        deployCount.set(0)
        reconnectHost { uuid = host.uuid }
        Thread.sleep(2000)
        assert deployCount.get() == 0 : \
                "reconnect redeployed an already-Connected vhost target ${deployCount.get()} time(s); " +
                "host-connect must skip protocols already Connected"

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
