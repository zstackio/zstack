package org.zstack.test.integration.storage.primary.addon.xinfini

import org.springframework.http.HttpEntity
import javax.servlet.http.HttpServletRequest
import org.zstack.compute.host.HostTracker
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.MessageCommandRecorder
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.thread.ThreadFacade
import org.zstack.xinfini.XInfiniStorageController
import org.zstack.xinfini.XInfiniApiHelper
import org.zstack.xinfini.XInfiniPathHelper
import org.zstack.xinfini.sdk.vhost.BdcModule
import org.zstack.xinfini.sdk.vhost.BdcBdevModule
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.host.HostConstant
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.host.PingHostMsg
import org.zstack.header.message.MessageReply
import org.zstack.header.storage.addon.primary.BaseVolumeInfo
import org.zstack.header.storage.addon.primary.SetHostStorageNodeBlacklistMsg
import org.zstack.header.storage.backup.DownloadImageFromRemoteTargetMsg
import org.zstack.header.storage.backup.DownloadImageFromRemoteTargetReply
import org.zstack.header.storage.backup.UploadImageToRemoteTargetReply
import org.zstack.header.storage.backup.UploadImageToRemoteTargetMsg
import org.zstack.header.storage.primary.ImageCacheShadowVO
import org.zstack.header.storage.primary.ImageCacheShadowVO_
import org.zstack.header.storage.primary.ImageCacheVO
import org.zstack.header.storage.primary.ImageCacheVO_
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.vm.VmBootDevice
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.vm.VmStateChangedOnHostMsg
import org.zstack.header.vm.devices.DeviceAddress
import org.zstack.header.vm.devices.VirtualDeviceInfo
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.kvm.VolumeTO
import org.zstack.sdk.*
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory
import org.zstack.storage.backup.BackupStorageSystemTags
import org.zstack.tag.SystemTagCreator
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.ExternalPrimaryStorageSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import static java.util.Arrays.asList

class XinfiniPrimaryStorageCase extends SubCase {
    EnvSpec env
    ClusterInventory cluster
    InstanceOfferingInventory instanceOffering
    DiskOfferingInventory diskOffering
    ImageInventory image, iso
    L3NetworkInventory l3
    PrimaryStorageInventory ps
    BackupStorageInventory bs
    VmInstanceInventory vm
    VolumeInventory vol, vol2
    HostInventory host1, host2
    CloudBus bus
    XInfiniStorageController controller

    String xinfiniUrl = "http://127.0.0.1:8989"
    String xinfiniConfig = '{"token":"test-token","pools":[{"id":1,"name":"pool1"}],"nodes":[{"ip":"127.0.0.1","port":8989}]}'
    String exportProtocol = "iscsi://"

    @Override
    void clean() {
        System.setProperty("useImageSpecSize", "false")
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

                image {
                    name = "iso"
                    url = "http://zstack.org/download/test.iso"
                    size = SizeUnit.GIGABYTE.toByte(1)
                    format = "iso"
                    virtio = true
                }
            }

            cephBackupStorage {
                name = "ceph-bs"
                fsid = "ceph-template-cluster"
                monUrls = ["root:password@127.0.0.4/?monPort=7777"]

                image {
                    name = "ceph-data-template"
                    url = "http://zstack.org/download/test-volume.qcow2"
                    mediaType = "DataVolumeTemplate"
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
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }
                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.3"
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

                attachBackupStorage("sftp")
                attachBackupStorage("ceph-bs")
            }
        }
    }

    @Override
    void test() {
        System.setProperty("useImageSpecSize", "true")
        env.create {
            cluster = env.inventoryByName("cluster") as ClusterInventory
            instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
            diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
            image = env.inventoryByName("image") as ImageInventory
            iso = env.inventoryByName("iso") as ImageInventory
            l3 = env.inventoryByName("l3") as L3NetworkInventory
            bs = env.inventoryByName("sftp") as BackupStorageInventory
            host1 = env.inventoryByName("kvm") as HostInventory
            host2 = env.inventoryByName("kvm2") as HostInventory
            bus = bean(CloudBus.class)

            KVMGlobalConfig.VM_SYNC_ON_HOST_PING.updateValue(true)
            simulatorEnv()
            testCreateXinfiniStorage()
            testCreateVm()
            testRejectDeletingBdevActivation()
            testNodeBlacklistKeepsActiveClients()
            testPreventSplitBrainOnDeactivateFailure()
            testHandleInactiveVolume()
            testCreateVolumeRollback()
            testAttachIso()
            testCreateDataVolume()
            testRejectCephBackupStorage()
            testCreateSnapshot()
            testCreateTemplate()
            testClean()
            testImageCacheClean()
            testDeletePs()
        }
    }

    void simulatorEnv() {
        env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME) { KVMAgentCommands.AttachDataVolumeResponse rsp, HttpEntity<String> e ->
            KVMAgentCommands.AttachDataVolumeCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.AttachDataVolumeCmd.class)

            VirtualDeviceInfo info = new VirtualDeviceInfo()
            info.resourceUuid = cmd.volume.resourceUuid
            info.deviceAddress = new DeviceAddress()
            info.deviceAddress.domain = "0000"
            info.deviceAddress.bus = "00"
            info.deviceAddress.slot = Long.toHexString(Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, cmd.vmUuid).count())
            info.deviceAddress.function = "0"

            rsp.virtualDeviceInfoList = []
            rsp.virtualDeviceInfoList.addAll(info)
            return rsp
        }

        SystemTagCreator creator = BackupStorageSystemTags.ISCSI_INITIATOR_NAME.newSystemTagCreator(bs.uuid);
        creator.setTagByTokens(Collections.singletonMap(BackupStorageSystemTags.ISCSI_INITIATOR_NAME_TOKEN, "iqn.1994-05.com.redhat:fc16b4d4fb3f"));
        creator.inherent = false;
        creator.recreate = true;
        creator.create();
    }

    void testCreateXinfiniStorage() {
        def zone = env.inventoryByName("zone") as ZoneInventory

        ps = addExternalPrimaryStorage {
            name = "test"
            zoneUuid = zone.uuid
            url = xinfiniUrl
            identity = "xinfini"
            config = xinfiniConfig
            defaultOutputProtocol = "Vhost"
        } as ExternalPrimaryStorageInventory

        ps = queryPrimaryStorage {}[0] as ExternalPrimaryStorageInventory
        assert ps.getAddonInfo() != null

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        ExternalPrimaryStorageFactory factory = Platform.getComponentLoader().getComponent(ExternalPrimaryStorageFactory.class)
        controller = factory.getControllerSvc(ps.uuid) as XInfiniStorageController

        PingHostMsg pmsg = new PingHostMsg()
        pmsg.hostUuid = host1.uuid
        bus.makeTargetServiceIdByResourceUuid(pmsg, HostConstant.SERVICE_ID, host1.uuid)
        MessageReply r = bus.call(pmsg)
        assert r.success
        assert Q.New(PrimaryStorageHostRefVO.class).eq(PrimaryStorageHostRefVO_.hostUuid, host1.uuid).find().status.toString() == "Connected"

        pmsg = new PingHostMsg()
        pmsg.hostUuid = host2.uuid
        bus.makeTargetServiceIdByResourceUuid(pmsg, HostConstant.SERVICE_ID, host2.uuid)
        r = bus.call(pmsg)
        assert r.success
        assert Q.New(PrimaryStorageHostRefVO.class).eq(PrimaryStorageHostRefVO_.hostUuid, host2.uuid).find().status.toString() == "Connected"

        // ping again
        pmsg = new PingHostMsg()
        pmsg.hostUuid = host1.uuid
        bus.makeTargetServiceIdByResourceUuid(pmsg, HostConstant.SERVICE_ID, host1.uuid)
        r = bus.call(pmsg)
        assert r.success
        assert Q.New(PrimaryStorageHostRefVO.class).eq(PrimaryStorageHostRefVO_.hostUuid, host1.uuid).find().status.toString() == "Connected"

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
    }

    void testCreateVm() {
        def result = getCandidatePrimaryStoragesForCreatingVm {
            l3NetworkUuids = [l3.uuid]
            imageUuid = image.uuid
        } as GetCandidatePrimaryStoragesForCreatingVmResult

        assert result.getRootVolumePrimaryStorages().size() == 1

        env.message(UploadImageToRemoteTargetMsg.class) { UploadImageToRemoteTargetMsg msg, CloudBus bus ->
            UploadImageToRemoteTargetReply r = new UploadImageToRemoteTargetReply()
            assert msg.getRemoteTargetUrl().startsWith(exportProtocol)
            assert msg.getFormat() == "raw"
            bus.reply(msg, r)
        }

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            assert cmd.rootVolume.deviceType == VolumeTO.VHOST
            assert cmd.rootVolume.installPath.startsWith("/var/run/bdc-")
            assert cmd.rootVolume.format == "raw"
            if (cmd.cdRoms != null) {
                cmd.cdRoms.forEach {
                    if (!it.isEmpty() && it.getPath() != null) {
                        assert it.getPath().startsWith(exportProtocol)
                    }
                }
            }
            return rsp
        }

        def otherVm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            rootDiskOfferingUuid = diskOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host1.uuid
        } as VmInstanceInventory

        assert otherVm.allVolumes[0].size == diskOffering.diskSize

        vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host1.uuid
        } as VmInstanceInventory

        deleteVm(otherVm.uuid)

        stopVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
            hostUuid = host1.uuid
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        def vm2 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            rootDiskOfferingUuid = diskOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host1.uuid
        } as VmInstanceInventory

        deleteVm(vm2.uuid)
    }

    void testRejectDeletingBdevActivation() {
        String collectionPath = "/afa/v1/bdc-bdevs"
        String itemPath = "/afa/v1/bdc-bdevs/\\d+"
        Closure collectionSimulator = env.getSimulator(collectionPath)
        Closure itemSimulator = env.getSimulator(itemPath)
        def store = ExternalPrimaryStorageSpec.XinfiniSimulators.bdcBdevs
        String volumePath = dbFindByUuid(vm.rootVolumeUuid, VolumeVO.class).installPath
        int volumeId = XInfiniPathHelper.getVolIdFromPath(volumePath)
        def headerHost = org.zstack.header.host.HostInventory.valueOf(dbFindByUuid(host1.uuid, HostVO.class))
        def volumeInfo = new BaseVolumeInfo(uuid: vm.rootVolumeUuid, protocol: "Vhost", installPath: volumePath)
        def original = store.values().find { it.spec.bs_volume_id == volumeId }
        assert original != null: "the running VM must have a root-volume mapping"
        String existingState = "active"
        String firstPollState = null
        int postCount = 0
        int pollCount = 0
        int postedId = -1
        env.simulator(collectionPath) { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
            if (req.method == "POST") {
                def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                if (body.spec.bs_volume_id == volumeId) {
                    postCount++
                    if (store.containsKey(original.spec.id)) {
                        postedId = original.spec.id
                        return [metadata: [state: [state: existingState]], spec: original.spec, status: original.status]
                    }
                }
            }
            def rsp = collectionSimulator.call(req, e, spec)
            if (req.method == "POST" && rsp.spec.bs_volume_id == volumeId) {
                postedId = rsp.spec.id
            } else if (req.method == "GET") {
                rsp.items.findAll { it.spec.bs_volume_id == volumeId }.each { it.metadata.state.state = existingState }
            }
            return rsp
        }
        env.simulator(itemPath) { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
            def rsp = itemSimulator.call(req, e, spec)
            if (req.method == "GET" && rsp.spec?.id == postedId) {
                pollCount++
                rsp.metadata.state.state = pollCount == 1 && firstPollState != null ? firstPollState : "active"
            }
            return rsp
        }
        try {
            stopVmInstance { uuid = vm.uuid }
            ["deleting", "deleted"].each { String state ->
                store.put(original.spec.id as Integer, original)
                existingState = state
                postCount = 0
                expect(OperationFailureException.class) { controller.getActivePath(volumeInfo, headerHost, false) }
                def result = new StartVmInstanceAction(uuid: vm.uuid, hostUuid: host1.uuid,
                        sessionId: adminSession()).call()
                if (result.error == null) {
                    stopVmInstance { uuid = vm.uuid }
                }
                assert result.error != null: "starting with an existing ${state} mapping must fail before VM start"
                assert result.error.details.contains(state): "activation error must identify mapping state ${state}"
                assert postCount == 0: "an existing ${state} mapping must not be recreated: POST=${postCount}"
                assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Stopped:
                        "failed root-volume activation must leave the VM stopped"
            }
            existingState = "active"
            store.put(original.spec.id as Integer, original)
            postCount = 0
            assert controller.getActivePath(volumeInfo, headerHost, false) == original.spec.socket_path:
                    "an active mapping must retain its original socket"
            startVmInstance { uuid = vm.uuid; hostUuid = host1.uuid }
            assert postCount == 0: "an active mapping must be reused without a creation POST"
            stopVmInstance { uuid = vm.uuid }

            ["deleting", "deleted"].each { String state ->
                firstPollState = state
                postCount = 0
                pollCount = 0
                def result = new StartVmInstanceAction(uuid: vm.uuid, hostUuid: host1.uuid,
                        sessionId: adminSession()).call()
                if (result.error == null) {
                    stopVmInstance { uuid = vm.uuid }
                }
                assert result.error != null: "POST acceptance followed by ${state} must not report activation success"
                assert result.error.details.contains(state): "creation error must identify mapping state ${state}"
                assert pollCount == 1: "${state} is not an activation wait state: GET=${pollCount}"
                assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Stopped:
                        "failed new mapping activation must leave the VM stopped"
                store.remove(postedId)
            }
            firstPollState = "creating"
            postCount = 0
            pollCount = 0
            startVmInstance { uuid = vm.uuid; hostUuid = host1.uuid }
            assert postCount == 1: "a retry after removal must create a new mapping"
            assert pollCount == 2: "a creating mapping must still be allowed to converge to active"
        } finally {
            env.simulator(collectionPath, collectionSimulator)
            env.simulator(itemPath, itemSimulator)
        }
    }

    void testNodeBlacklistKeepsActiveClients() {
        String path = dbFindByUuid(vm.rootVolumeUuid, VolumeVO.class).installPath
        def host = org.zstack.header.host.HostInventory.valueOf(dbFindByUuid(host1.uuid, HostVO.class))
        assert controller.reportCapabilities().supportNodeBlacklist:
                "Xinfini must advertise its node blacklist implementation"
        assert !controller.reportCapabilities().supportPathBlacklist:
                "Xinfini must not advertise unsupported path isolation"
        int originalClients = controller.getActiveClients(path, "Vhost").size()
        assert originalClients > 0: "the volume must have an old client before fencing"

        String desired = "unfenced"
        String actual = "unfenced"
        boolean rejectPatch = true
        int patches = 0
        int polls = 0
        env.simulator("/afa/v1/bdcs/\\d+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec ignored ->
            if (req.method == "PATCH") {
                if (rejectPatch) {
                    return [message: "fencing rejected for test"]
                }
                String requested = JSONObjectUtil.toObject(e.body, LinkedHashMap.class).spec.storage_fenced_state
                assert requested in ["fenced", "unfenced"]: "node state changes must declare the intended fence state"
                patches++
                if (desired != requested) {
                    desired = requested
                    actual = requested == "fenced" ? "fencing" : "unfencing"
                    polls = 0
                }
            } else if (actual in ["fencing", "unfencing"] && ++polls >= 3) {
                actual = desired
            }
            return [metadata: [state: [state: actual == desired ? "active" : "reconciling"]],
                    spec: [id: 1, ip: "127.0.0.1", storage_fenced_state: desired],
                    status: [id: 1, run_state: "Active", storage_fenced_state: actual]]
        }

        expect(OperationFailureException.class) { controller.blacklistNode(host) }

        Closure setNodeBlacklist = { boolean blacklist ->
            def msg = new SetHostStorageNodeBlacklistMsg(hostUuid: host.uuid, blacklist: blacklist,
                    primaryStorageUuid: dbFindByUuid(vm.rootVolumeUuid, VolumeVO.class).primaryStorageUuid)
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.uuid)
            MessageReply reply = bus.call(msg)
            assert reply.success: "the main storage plugin must handle node isolation without premium: ${reply.error}"
        }
        rejectPatch = false
        setNodeBlacklist(true)
        assert actual == "fenced" && desired == "fenced" && polls >= 3:
                "blacklistNode must wait for both requested and actual fenced state"
        def clients = controller.getActiveClients(path, "Vhost")
        assert clients.size() == originalClients: "node fencing must retain existing bdev client records"
        assert clients.every { !it.inBlacklist }: "node fencing must leave old Vhost clients eligible for deactivate"
        controller.blacklistNode(host)
        assert patches == 1: "repeated node blacklist must not patch an already fenced BDC"

        def currentTag = {
            querySystemTag { conditions = ["resourceUuid=" + host.uuid, "tag=nodeBlacklist::" + ps.uuid] }*.uuid.find()
        }
        String originalTag = currentTag()
        assert originalTag
        Closure originalVmUsageSimulator = env.getSimulator(KVMConstant.KVM_CHECK_STORAGE_VM_USAGE_PATH)
        String vmUsage = "busy"
        env.simulator(KVMConstant.KVM_CHECK_STORAGE_VM_USAGE_PATH) { HttpEntity<String> request ->
            def cmd = JSONObjectUtil.toObject(request.body, KVMAgentCommands.CheckStorageVmUsageCmd.class)
            assert cmd.primaryStorageUuids == [ps.uuid]: "only storages in the host blacklist tags need a usage check"
            def rsp = new KVMAgentCommands.CheckStorageVmUsageRsp()
            rsp.unusedPrimaryStorageUuids = vmUsage == "busy" ? [] : [ps.uuid]
            if (vmUsage == "error") {
                rsp.success = false
                rsp.error = "vm usage check failed"
            }
            return rsp
        }
        def threads = bean(ThreadFacade.class)
        String syncQueue = "sync-volume-state-after-ping-host-${host.uuid}-success"
        def ping = {
            def msg = new PingHostMsg(hostUuid: host.uuid)
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.uuid)
            MessageCommandRecorder.start(PingHostMsg.class)
            try {
                assert bus.call(msg).success
                retryInSecs { assert !threads.isChainTaskRunning(syncQueue) }
                return MessageCommandRecorder.end()
            } finally {
                MessageCommandRecorder.reset()
            }
        }
        def tracker = bean(HostTracker.class)
        def hostUuids = [host.uuid, host2.uuid]
        tracker.untrackHost(hostUuids)
        try {
            ["busy", "error"].each { mode ->
                vmUsage = mode
                def sent = ping()
                assert sent.contains(KVMAgentCommands.CheckStorageVmUsageCmd.class)
                assert !sent.contains(SetHostStorageNodeBlacklistMsg.class): "usage ${mode} must keep the fence"
                assert actual == "fenced" && currentTag() == originalTag && patches == 1:
                        "vm usage ${mode} must retain the fence"
            }
            vmUsage = "unused"
            assert ping().contains(SetHostStorageNodeBlacklistMsg.class): "unused storage must request unfencing"
            retryInSecs { assert actual == "unfenced" && !currentTag() && patches == 2 }

            2.times {
                def sent = ping()
                assert sent.contains(KVMAgentCommands.VolumeSyncCmd.class): "ping must still sync volumes"
                assert !sent.contains(KVMAgentCommands.CheckStorageVmUsageCmd.class): "untagged host must skip usage"
            }

            vmUsage = "busy"
            setNodeBlacklist(true)
            assert actual == "fenced" && currentTag() && patches == 3: "a stopped heartbeat must fence the BDC again"
            setNodeBlacklist(false)
            assert actual == "unfenced" && !currentTag() && patches == 4:
                    "an explicit unblacklist request must unfence the tagged BDC"
        } finally {
            env.simulator(KVMConstant.KVM_CHECK_STORAGE_VM_USAGE_PATH, originalVmUsageSimulator)
            tracker.trackHost(hostUuids)
        }
    }

    // when the old host's storage client cannot be isolated, deactivate must report
    // a failure instead of silently succeeding, otherwise the VM gets started on a
    // new host while the old QEMU still holds the volume (split-brain).
    void testPreventSplitBrainOnDeactivateFailure() {
        def rootVolPath = Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, vm.rootVolumeUuid)
                .select(VolumeVO_.installPath).findValue() as String
        int volId = XInfiniPathHelper.getVolIdFromPath(rootVolPath)
        BdcModule bdc = controller.apiHelper.queryBdcByIp(host1.managementIp)
        BdcBdevModule bdev = controller.apiHelper.queryBdcBdevByVolumeIdAndBdcId(volId, bdc.spec.id)
        assert bdev != null

        // simMode: 0 = default, 1 = delete request fails, 2 = delete accepted but bdev never disappears
        int[] simMode = [0]
        boolean[] bdcInactive = [false]
        int nodeFenceRequests = 0
        int confirmedDeletes = 0
        int deleteRequests = 0
        boolean nodeFenced = false

        env.simulator("/afa/v1/bdcs/\\d+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
            if (req.method == "PATCH") {
                nodeFenceRequests++
                return [message: "ordinary VM startup must not fence a node"]
            }
            return [
                metadata: [id: 1, name: "bdc-1", state: [state: "active"]],
                spec    : [id: 1, name: "bdc-1", ip: "127.0.0.1", port: 9500,
                           storage_fenced_state: nodeFenced ? "fenced" : "unfenced"],
                status  : [id: 1, run_state: bdcInactive[0] ? "Offline" : "Active",
                           storage_fenced_state: nodeFenced ? "fenced" : "unfenced",
                           installed: true, hostname: "localhost", version: "1.0.0"]
            ]
        }

        env.simulator("/afa/v1/bdc-bdevs/\\d+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
            def matcher = (req.getRequestURI() =~ /\/(\d+)$/)
            int bdevId = matcher.find() ? Integer.parseInt(matcher.group(1)) : -1
            def store = ExternalPrimaryStorageSpec.XinfiniSimulators.bdcBdevs
            if (req.getMethod() == "DELETE") {
                deleteRequests++
                if (simMode[0] == 1) {
                    return [message: "delete bdev failed on purpose"]
                }
                if (simMode[0] != 2) {
                    store.remove(bdevId)
                }
                return [:]
            }

            def stored = store.get(bdevId)
            if (stored == null) {
                confirmedDeletes++
                throw new HttpError(404, "not found")
            }
            String state = simMode[0] == 2 ? "deleting" : "active"
            return [metadata: [id: stored.spec?.id, name: stored.spec?.name, state: [state: state]],
                    spec: stored.spec, status: stored.status]
        }

        def hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, host1.uuid).find()
        def headerHost = org.zstack.header.host.HostInventory.valueOf(hostVO)

        // scenario 1: the delete bdev request fails -> deactivate must report failure
        simMode[0] = 1
        boolean[] result = [false, false]   // [success, fail]
        controller.deactivate(rootVolPath, "Vhost", headerHost, new Completion(null) {
            @Override
            void success() { result[0] = true }

            @Override
            void fail(ErrorCode errorCode) { result[1] = true }
        })
        assert result[1]: "deactivate must fail when the bdev cannot be deleted"
        assert !result[0]: "deactivate must not report success when the bdev cannot be deleted"

        // scenario 2: the delete request is accepted but the bdev stays attached ->
        // deleteBdcBdev must error out instead of silently treating it as deleted
        simMode[0] = 2
        bdcInactive[0] = true
        boolean errored = false
        try {
            controller.apiHelper.deleteBdcBdev(bdev.spec.id, bdc.spec.id)
        } catch (OperationFailureException ignored) {
            errored = true
        }
        assert errored: "deleteBdcBdev must error out when the bdev is still attached"

        simMode[0] = 0
        bdcInactive[0] = false
        def store = ExternalPrimaryStorageSpec.XinfiniSimulators.bdcBdevs
        def oldMapping = store.get(bdev.spec.id)
        stopVmInstance { uuid = vm.uuid }
        store.put(bdev.spec.id, oldMapping)
        Closure startSimulator = env.getPostSimulator(KVMConstant.KVM_START_VM_PATH)
        int targetStarts = 0
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            String uuid = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class).vmInstanceUuid
            if (uuid == vm.uuid) {
                assert confirmedDeletes > 0:
                        "an unfenced old client must reach DELETE confirmation before target KVM start"
                targetStarts++
            }
            return startSimulator.call(rsp, e)
        }
        try {
            [1, 2].each { int failure ->
                simMode[0] = failure
                bdcInactive[0] = failure == 2
                nodeFenced = failure == 2
                confirmedDeletes = 0
                deleteRequests = 0
                def started = new StartVmInstanceAction(uuid: vm.uuid, hostUuid: host2.uuid, timeout: 180000,
                        sessionId: adminSession()).call()
                assert started.error != null && deleteRequests > 0:
                        "startup must attempt to remove the old mapping; mode=${failure}, nodeFenced=${nodeFenced}"
                assert nodeFenceRequests == 0 && targetStarts == 0 && confirmedDeletes == 0:
                        "DELETE failure or persistent deleting must block KVM start without node PATCH"
                assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Stopped:
                        "the VM must remain stopped while its old client cannot be deactivated"
            }
            simMode[0] = 0
            bdcInactive[0] = false
            nodeFenced = false
            startVmInstance { uuid = vm.uuid; hostUuid = host2.uuid }
            assert targetStarts == 1 && confirmedDeletes > 0 && nodeFenceRequests == 0:
                    "confirmed old-client DELETE must allow startup without node isolation"
        } finally {
            simMode[0] = 0
            bdcInactive[0] = false
            nodeFenced = false
            env.afterSimulator(KVMConstant.KVM_START_VM_PATH, startSimulator)
        }
        stopVmInstance { uuid = vm.uuid }
        startVmInstance { uuid = vm.uuid; hostUuid = host1.uuid }
    }

    void testHandleInactiveVolume() {
        def rootVolInstallPath = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vm.rootVolumeUuid).select(VolumeVO_.installPath).findValue()
        int volId = XInfiniPathHelper.getVolIdFromPath(rootVolInstallPath as String)
        BdcModule bdc = controller.apiHelper.queryBdcByIp(host1.managementIp)
        BdcBdevModule bdev = controller.apiHelper.queryBdcBdevByVolumeIdAndBdcId(volId, bdc.spec.id)
        assert bdev != null

        def clusterUuid = controller.apiHelper.getClusterUuid()
        def vhostSocketDir = "/var/run/bdc-${clusterUuid}/"

        env.simulator(KVMConstant.KVM_VOLUME_SYNC_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.VolumeSyncCmd.class)
            assert cmd.storagePaths.get(0).endsWith("/volume-*")
            def rsp = new KVMAgentCommands.VolumeSyncRsp()
            rsp.inactiveVolumePaths = new HashMap<>()
            rsp.inactiveVolumePaths.put(cmd.storagePaths.get(0), asList("${vhostSocketDir}volume-${vm.rootVolumeUuid}" as String))
            return rsp
        }

        def msg = new PingHostMsg()
        msg.hostUuid = host1.uuid
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host1.uuid)
        MessageReply r = bus.call(msg)
        assert r.success

        sleep(1000)
        // vm in running, not deactivate volume
        bdev = controller.apiHelper.queryBdcBdevByVolumeIdAndBdcId(volId, bdc.spec.id)
        assert bdev != null

        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).set(VmInstanceVO_.hostUuid, null).set(VmInstanceVO_.state, VmInstanceState.Starting).update()
        env.message(VmStateChangedOnHostMsg.class) { VmStateChangedOnHostMsg cmsg, CloudBus bus ->
            bus.reply(cmsg, new MessageReply())
        }
        r = bus.call(msg)
        assert r.success

        sleep(1000)
        // vm in starting, not deactivate volume
        bdev = controller.apiHelper.queryBdcBdevByVolumeIdAndBdcId(volId, bdc.spec.id)
        assert bdev != null
        env.cleanMessageHandlers()

        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).set(VmInstanceVO_.hostUuid, null).set(VmInstanceVO_.state, VmInstanceState.Stopped).update()
        r = bus.call(msg)
        assert r.success

        sleep(1000)
        // vm in stop, deactivate volume
        retryInSecs {
            bdev = controller.apiHelper.queryBdcBdevByVolumeIdAndBdcId(volId, bdc.spec.id)
            assert bdev == null
        }

        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).set(VmInstanceVO_.hostUuid, host1.uuid).set(VmInstanceVO_.state, VmInstanceState.Running).update()
        env.simulator(KVMConstant.KVM_VOLUME_SYNC_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.VolumeSyncCmd.class)
            assert cmd.storagePaths.get(0).endsWith("/volume-*")
            return new KVMAgentCommands.VolumeSyncRsp()
        }
    }

    void testCreateVolumeRollback() {
        def vol = createDataVolume {
            name = "test"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME) { rsp, HttpEntity<String> e ->
            rsp.setError("on purpose")
            return rsp
        }

        expectError {
            attachDataVolumeToVm {
                vmInstanceUuid = vm.uuid
                volumeUuid = vol.uuid
            }
        }

        env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME) { rsp, HttpEntity<String> e ->
            return rsp
        }

        deleteVolume(vol.uuid)
    }

    void testAttachIso() {
        env.afterSimulator(KVMConstant.KVM_ATTACH_ISO_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.AttachIsoCmd.class)
            assert cmd.iso.getPath().startsWith(exportProtocol)
            return rsp
        }

        attachIsoToVmInstance {
            vmInstanceUuid = vm.uuid
            isoUuid = iso.uuid
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.CdRom.toString(), VmBootDevice.HardDisk.toString(), VmBootDevice.Network.toString())
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
            hostUuid = host1.uuid
        }

        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.HardDisk.toString(), VmBootDevice.CdRom.toString(), VmBootDevice.Network.toString())
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
            hostUuid = host1.uuid
        }

        detachIsoFromVmInstance {
            vmInstanceUuid = vm.uuid
        }
    }

    void testCreateDataVolume() {
        vol = createDataVolume {
            name = "test"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        deleteVolume(vol.uuid)

        vol = createDataVolume {
            name = "test"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = vol.uuid
        }

        vol2 = createDataVolume {
            name = "test"
            diskOfferingUuid = diskOffering.uuid
        } as VolumeInventory

        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = vol2.uuid
        }

        detachDataVolumeFromVm {
            uuid = vol2.uuid
        }

        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = vol2.uuid
        }
    }

    void testRejectCephBackupStorage() {
        BackupStorageInventory cephBs = env.inventoryByName("ceph-bs")
        ImageInventory cephImage = env.inventoryByName("ceph-data-template")
        long volumeCount = Q.New(VolumeVO.class).count()
        long snapshotCount = Q.New(VolumeSnapshotVO.class).count()

        def restore = new CreateDataVolumeFromVolumeTemplateAction()
        restore.name = "ceph-template-to-vhost"
        restore.imageUuid = cephImage.uuid
        restore.primaryStorageUuid = ps.uuid
        restore.sessionId = env.session.uuid
        def restoreResult = restore.call()
        assert restoreResult.error?.globalErrorCode == "ORG_ZSTACK_STORAGE_VOLUME_10088" :
                "Ceph template must be rejected before downloading to Addon PS: actual=${JSONObjectUtil.toJsonString(restoreResult.error)}"
        assert Q.New(VolumeVO.class).count() == volumeCount :
                "Rejected template restore must not leave a volume: expected=${volumeCount}, actual=${Q.New(VolumeVO.class).count()}"

        def backup = new CreateDataVolumeTemplateFromVolumeAction()
        backup.name = "vhost-template-to-ceph"
        backup.volumeUuid = vol.uuid
        backup.backupStorageUuids = [cephBs.uuid]
        backup.sessionId = env.session.uuid
        def backupResult = backup.call()
        assert backupResult.error?.details?.contains("ORG_ZSTACK_STORAGE_BACKUP_10013") :
                "Explicit Ceph BS must be rejected by the allocator for Addon PS: actual=${JSONObjectUtil.toJsonString(backupResult.error)}"
        assert Q.New(VolumeSnapshotVO.class).count() == snapshotCount :
                "Rejected template creation must not leave a snapshot: expected=${snapshotCount}, actual=${Q.New(VolumeSnapshotVO.class).count()}"
    }

    void testCreateSnapshot() {
        def snapshot = createVolumeSnapshot {
            name = "test"
            volumeUuid = vol.uuid
        } as VolumeSnapshotInventory

        stopVmInstance {
            uuid = vm.uuid
        }

        revertVolumeFromSnapshot {
            uuid = snapshot.uuid
        }

        deleteVolumeSnapshot {
            uuid = snapshot.uuid
        }

        startVmInstance {
            uuid = vm.uuid
            hostUuid = host1.uuid
        }
    }

    void testCreateTemplate() {
        env.message(DownloadImageFromRemoteTargetMsg.class) { DownloadImageFromRemoteTargetMsg msg, CloudBus bus ->
            DownloadImageFromRemoteTargetReply r = new DownloadImageFromRemoteTargetReply()
            assert msg.getRemoteTargetUrl().startsWith(exportProtocol)
            r.setInstallPath("zstore://test/image")
            r.setSize(100L)
            bus.reply(msg, r)
        }

        def dataImage = createDataVolumeTemplateFromVolume {
            name = "vol-image"
            volumeUuid = vol.uuid
            backupStorageUuids = [bs.uuid]
        } as ImageInventory

        stopVmInstance {
            uuid = vm.uuid
        }

        def rootImage = createRootVolumeTemplateFromRootVolume {
            name = "root-image"
            rootVolumeUuid = vm.rootVolumeUuid
            backupStorageUuids = [bs.uuid]
        } as ImageInventory
    }

    void testClean() {
        deleteVm(vm.uuid)

        deleteVolume(vol2.uuid)

        deleteDataVolume {
            uuid = vol.uuid
        }

        expungeDataVolume {
            uuid = vol.uuid
        }
    }

    void testImageCacheClean() {
        deleteImage {
            uuid = image.uuid
        }

        expungeImage {
            imageUuid = image.uuid
        }

        cleanUpImageCacheOnPrimaryStorage {
            uuid = ps.uuid
        }

        retryInSecs {
            assert Q.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, image.uuid).count() == 0
            assert Q.New(ImageCacheShadowVO.class).eq(ImageCacheShadowVO_.imageUuid, image.uuid).count() == 0
        }
    }

    void testDeletePs() {
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        deletePrimaryStorage {
            uuid = ps.uuid
        }
    }

    void deleteVm(String vmUuid) {
        destroyVmInstance {
            uuid = vmUuid
        }

        expungeVmInstance {
            uuid = vmUuid
        }
    }

    void deleteVolume(String volUuid) {
        deleteDataVolume {
            uuid = volUuid
        }

        expungeDataVolume {
            uuid = volUuid
        }
    }
}
