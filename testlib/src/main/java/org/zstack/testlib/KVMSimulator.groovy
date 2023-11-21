package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmJobStruct
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmResultStruct
import org.zstack.core.db.SQLBatch
import org.zstack.header.Constants
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.vm.devices.DeviceAddress
import org.zstack.header.vm.devices.VirtualDeviceInfo
import org.zstack.header.volume.VolumeInventory
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.VolumeTO
import org.zstack.testlib.vfs.VFS
import org.zstack.testlib.vfs.extensions.VFSPrimaryStorageTakeSnapshotBackend
import org.zstack.testlib.vfs.extensions.VFSSnapshot
import org.zstack.utils.BeanUtils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import javax.persistence.Tuple
import java.util.concurrent.ConcurrentHashMap

import static org.zstack.kvm.KVMAgentCommands.*

/**
 * Created by xing5 on 2017/6/6.
 */
class KVMSimulator implements Simulator {
    static ConcurrentHashMap<String, KVMAgentCommands.ConnectCmd> connectCmdConcurrentHashMap = new ConcurrentHashMap<>()
    private static Map<String, VFSPrimaryStorageTakeSnapshotBackend> takeSnapshotBackends = [:]

    static {
        BeanUtils.reflections.getSubTypesOf(VFSPrimaryStorageTakeSnapshotBackend.class).each { clz ->
            VFSPrimaryStorageTakeSnapshotBackend backend = clz.getConstructor().newInstance()
            VFSPrimaryStorageTakeSnapshotBackend old = takeSnapshotBackends[backend.primaryStorageType]
            assert old == null : "duplicated VFSPrimaryStorageTakeSnapshotBackend[type: ${backend.primaryStorageType}]," +
                    " ${backend} and ${old}"
            takeSnapshotBackends[backend.primaryStorageType] = backend
        }
    }

    static List<TakeSnapshotsOnKvmResultStruct> takeSnapshotByPrimaryStorage(HttpEntity<String> e, EnvSpec spec, List<TakeSnapshotsOnKvmJobStruct> snapshotJobs) {
        Map<String, List<TakeSnapshotsOnKvmJobStruct>> jobMap = new HashMap<>()
        Map<String, String> primaryStorageTypeMap = new HashMap<>()
        snapshotJobs.each { job ->
            Tuple tuple = SQL.New("select pri.uuid, pri.type from PrimaryStorageVO pri, VolumeVO vol where" +
                    " pri.uuid = vol.primaryStorageUuid and vol.uuid = :volUuid", Tuple.class).param("volUuid", job.volumeUuid)
                    .find()
            assert tuple.get(1) : "cannot find primary storage of volume[uuid: ${job.volumeUuid}]"

            jobMap.putIfAbsent((String) tuple.get(0), new ArrayList<>())
            jobMap.get((String) tuple.get(0)).add(job)
            primaryStorageTypeMap.putIfAbsent((String) tuple.get(0), (String) tuple.get(1))
        }

        List<TakeSnapshotsOnKvmResultStruct> results = new ArrayList<>()
        jobMap.entrySet().each { entry ->
            String psUuid = entry.key
            List<TakeSnapshotsOnKvmJobStruct> jobs = entry.value
            VFSPrimaryStorageTakeSnapshotBackend bkd = getVFSPrimaryStorageTakeSnapshotBackend(primaryStorageTypeMap.get(psUuid))
            results.addAll(bkd.takeSnapshotsOnVolumes(psUuid, e, spec, jobs))
        }


        return results
    }

    static VFSPrimaryStorageTakeSnapshotBackend getVFSPrimaryStorageTakeSnapshotBackend(String primaryStorageType) {
        VFSPrimaryStorageTakeSnapshotBackend bkd = takeSnapshotBackends[primaryStorageType]
        assert bkd != null : "cannot find VFSPrimaryStorageTakeSnapshotBackend[type: ${primaryStorageType}]"
        return bkd
    }

    @Override
    void registerSimulators(EnvSpec spec) {
        spec.simulator(KVMConstant.KVM_HOST_CAPACITY_PATH) { HttpEntity<String> e, EnvSpec espec ->
            def rsp = new KVMAgentCommands.HostCapacityResponse()

            KVMHostSpec kspec = espec.specByUuid(e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID))
            rsp.success = true

            if (kspec == null) {
                rsp.usedCpu = 0
                rsp.cpuNum = 8
                rsp.totalMemory = SizeUnit.GIGABYTE.toByte(32)
                rsp.usedMemory = 0
                rsp.cpuSpeed = 1
                rsp.cpuSockets = 2
            } else {
                rsp.usedCpu = kspec.usedCpu
                rsp.cpuNum = kspec.totalCpu
                rsp.totalMemory = kspec.totalMem
                rsp.usedMemory = kspec.usedMem
                rsp.cpuSpeed = 1
                rsp.cpuSockets = kspec.cpuSockets
            }

            return rsp
        }

        spec.simulator(KVMConstant.KVM_HARDEN_CONSOLE_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        spec.simulator(KVMConstant.KVM_DELETE_CONSOLE_FIREWALL_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        spec.simulator(KVMConstant.GET_VM_DEVICE_ADDRESS_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.GetVmDeviceAddressCmd.class)
            def rsp = new KVMAgentCommands.GetVmDeviceAddressRsp()
            if (cmd.deviceTOs.keySet().contains(VolumeVO.class.simpleName)) {
                rsp.addresses = ["VolumeVO": []]
                for (Object o : cmd.deviceTOs.get(VolumeVO.class.simpleName)) {
                    VolumeTO to = JSONObjectUtil.rehashObject(o, VolumeTO.class)
                    rsp.addresses[VolumeVO.class.simpleName].add(new KVMAgentCommands.VmDeviceAddressTO(
                            addressType: "pci",
                            address: String.format("0000:%02d:00:0", to.deviceId),
                            deviceType: "disk",
                            uuid: to.volumeUuid
                    ))
                }
            }

            return rsp
        }

        spec.simulator(KVMConstant.GET_VIRTUALIZER_INFO_PATH) { HttpEntity<String> e ->
            def rsp = new GetVirtualizerInfoRsp()
            rsp.hostInfo = new VirtualizerInfoTO()
            rsp.hostInfo.version = "4.2.0-627.g36ee592.el7"
            rsp.hostInfo.virtualizer = "qemu-kvm"
            String hostUuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            rsp.hostInfo.uuid = hostUuid

            def cmd = JSONObjectUtil.toObject(e.body, GetVirtualizerInfoCmd.class)
            rsp.vmInfoList = cmd.vmUuids.collect { vmUuid ->
                def to = new VirtualizerInfoTO()
                to.uuid = vmUuid
                to.version = "4.2.0-627.g36ee592.el7"
                to.virtualizer = "qemu-kvm"
                return to
            }

            return rsp
        }

        spec.simulator(KVMConstant.KVM_HOST_FACT_PATH) { HttpEntity<String> e ->
            def hostUuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            def rsp = new HostFactResponse()

            rsp.osDistribution = "zstack"
            rsp.osRelease = "kvmSimulator"
            rsp.osVersion = "0.1"
            rsp.qemuImgVersion = "2.0.0"
            rsp.libvirtVersion = "1.2.9"
            rsp.cpuModelName = "Broadwell"
            rsp.cpuProcessorNum = "10"
            rsp.cpuGHz = "2.10"
            rsp.hostCpuModelName = "Broadwell @ 2.10GHz"
            rsp.ipmiAddress = "None"
            rsp.eptFlag = "ept"
            rsp.libvirtCapabilities = ["incrementaldrivemirror", "blockcopynetworktarget"]
            rsp.powerSupplyModelName = ""
            rsp.powerSupplyManufacturer = ""
            rsp.hvmCpuFlag = ""
            rsp.cpuCache = "64.0,4096.0,16384.0"
            rsp.iscsiInitiatorName = "iqn.1994-05.com.redhat:" + hostUuid.substring(0, 12)

            rsp.virtualizerInfo = new VirtualizerInfoTO()
            rsp.virtualizerInfo.version = "4.2.0-627.g36ee592.el7"
            rsp.virtualizerInfo.virtualizer = "qemu-kvm"
            return rsp
        }

        spec.simulator(KVMConstant.KVM_VM_UPDATE_PRIORITY_PATH) {
            return new KVMAgentCommands.UpdateVmPriorityRsp()
        }

        spec.simulator(KVMConstant.KVM_VM_CHECK_STATE) { HttpEntity<String> e ->
            KVMAgentCommands.CheckVmStateCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.CheckVmStateCmd.class)
            List<VmInstanceVO> vms = Q.New(VmInstanceVO.class).in(VmInstanceVO_.uuid, cmd.vmUuids).list()
            KVMAgentCommands.CheckVmStateRsp rsp = new KVMAgentCommands.CheckVmStateRsp()
            rsp.states = [:]
            vms.each {
                def kstate = KVMConstant.KvmVmState.fromVmInstanceState(it.state)
                if (kstate != null) {
                    rsp.states[(it.uuid)] = kstate.toString()
                } else {
                    rsp.states[(it.uuid)] = KVMConstant.KvmVmState.Shutdown.toString()
                }
            }

            return rsp
        }

        spec.simulator(KVMConstant.KVM_ATTACH_NIC_PATH) { HttpEntity<String> e ->
            KVMAgentCommands.AttachNicCommand cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.AttachNicCommand.class)
            KVMAgentCommands.AttachNicResponse rsp = new KVMAgentCommands.AttachNicResponse()
            VirtualDeviceInfo deviceInfo = new VirtualDeviceInfo()
            deviceInfo.setResourceUuid(cmd.nic.getUuid())
            rsp.setVirtualDeviceInfoList(Arrays.asList(deviceInfo))
            return rsp
        }

        spec.simulator(KVMConstant.KVM_DETACH_NIC_PATH) {
            return new KVMAgentCommands.DetachNicRsp()
        }

        spec.simulator(KVMConstant.KVM_UPDATE_NIC_PATH) {
            return new KVMAgentCommands.UpdateNicRsp()
        }

        spec.simulator(KVMConstant.KVM_ATTACH_ISO_PATH) {
            return new KVMAgentCommands.AttachIsoRsp()
        }

        spec.simulator(KVMConstant.KVM_DETACH_ISO_PATH) {
            return new KVMAgentCommands.DetachIsoRsp()
        }

        spec.simulator(KVMConstant.KVM_MERGE_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec espec ->
            return new KVMAgentCommands.MergeSnapshotRsp()
        }

        VFS.vfsHook(KVMConstant.KVM_MERGE_SNAPSHOT_PATH, spec) { rsp, HttpEntity<String> e, EnvSpec espec ->
            KVMAgentCommands.MergeSnapshotCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.MergeSnapshotCmd.class)
            VolumeInventory volume = null
            String primaryStorageType = null

            new SQLBatch() {
                @Override
                protected void scripts() {
                    VolumeVO vol = sql("select vol from VolumeVO vol where vol.vmInstanceUuid = :vmUuid " +
                            " and vol.deviceId = :deviceId", VolumeVO.class)
                            .param("vmUuid", cmd.vmUuid)
                            .param("deviceId", cmd.volume.getDeviceId())
                            .find()

                    assert vol : "cannot find dest volume[path: ${cmd.destPath}, deviceId: ${cmd.volume.getDeviceId()}] of VM[uuid: ${cmd.vmUuid}] in database"
                    volume = vol.toInventory()

                    primaryStorageType = q(PrimaryStorageVO.class).select(PrimaryStorageVO_.type).eq(PrimaryStorageVO_.uuid, vol.primaryStorageUuid).findValue()
                    assert primaryStorageType : "cannot find primary storage[uuid: ${vol.primaryStorageUuid}]"
                }
            }.execute()

            VFSPrimaryStorageTakeSnapshotBackend bkd = getVFSPrimaryStorageTakeSnapshotBackend(primaryStorageType)
            bkd.mergeSnapshots(e, espec, cmd, volume)
        }

        spec.simulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec espec ->
            return new KVMAgentCommands.TakeSnapshotResponse()
        }

        VFS.vfsHook(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH, spec) { rsp, HttpEntity<String> e, EnvSpec espec ->
            KVMAgentCommands.TakeSnapshotCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.TakeSnapshotCmd.class)

            VolumeVO volume = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, cmd.volumeUuid).find()
            assert volume : "cannot find volume[uuid: ${cmd.volumeUuid}]"
            String primaryStorageType = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type)
                    .eq(PrimaryStorageVO_.uuid, volume.primaryStorageUuid).findValue()
            assert primaryStorageType : "cannot find primary storage[uuid: ${volume.primaryStorageUuid}] from volume[uuid: ${volume.uuid}, name: ${volume.name}]"
            VFSPrimaryStorageTakeSnapshotBackend bkd = getVFSPrimaryStorageTakeSnapshotBackend(primaryStorageType)

            VFSSnapshot snapshot = bkd.takeSnapshot(e, espec, cmd, volume.toInventory() as VolumeInventory)
            rsp.newVolumeInstallPath = snapshot.installPath
            rsp.snapshotInstallPath = cmd.volumeInstallPath
            // size required greater than 0, if no mock value set keep size return 1
            rsp.size = snapshot.size == null || snapshot.size == 0 ? 1 : snapshot.size
            return rsp
        }

        spec.simulator(KVMConstant.KVM_PING_PATH) { HttpEntity<String> e, EnvSpec espec ->
            KVMAgentCommands.PingCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.PingCmd.class)
            assert null != cmd
            assert null != cmd.hostUuid

            def rsp = new KVMAgentCommands.PingResponse()
            rsp.hostUuid = cmd.hostUuid
            rsp.version = connectCmdConcurrentHashMap.get(rsp.hostUuid).version
            rsp.sendCommandUrl = connectCmdConcurrentHashMap.get(rsp.hostUuid).sendCommandUrl
            return rsp
        }

        spec.simulator(KVMConstant.KVM_UPDATE_HOST_CONFIGURATION_PATH) { HttpEntity<String> e, EnvSpec espec ->
            def rsp = new KVMAgentCommands.UpdateHostConfigurationResponse()
            return rsp
        }

        spec.simulator(KVMConstant.KVM_CONNECT_PATH) { HttpEntity<String> e ->
            Spec.checkHttpCallType(e, true)
            KVMAgentCommands.ConnectCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ConnectCmd.class)

            connectCmdConcurrentHashMap.put(cmd.hostUuid, cmd)

            def rsp = new KVMAgentCommands.ConnectResponse()
            rsp.success = true
            rsp.libvirtVersion = "1.0.0"
            rsp.qemuVersion = "1.3.0"
            rsp.iptablesSucc = true
            return rsp
        }

        spec.simulator(KVMConstant.KVM_ECHO_PATH) { HttpEntity<String> e ->
            Spec.checkHttpCallType(e, true)
            return [:]
        }

        spec.simulator(KVMConstant.KVM_DETACH_VOLUME) {
            return new KVMAgentCommands.DetachDataVolumeResponse()
        }

        spec.simulator(KVMConstant.KVM_VM_SYNC_PATH) { HttpEntity<String> e ->
            def hostUuid = e.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)

            List<Tuple> states = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.uuid, VmInstanceVO_.state)
                    .in(VmInstanceVO_.state, [VmInstanceState.Running, VmInstanceState.Unknown])
                    .eq(VmInstanceVO_.hostUuid, hostUuid).listTuple()

            def rsp = new KVMAgentCommands.VmSyncResponse()
            rsp.states = [:]
            states.each {
                String vmUuid = it.get(0, String.class)
                VmInstanceState state = it.get(1, VmInstanceState.class)
                if (state == VmInstanceState.Unknown) {
                    // host reconnecting will set VMs to Unknown in DB
                    // the spec.simulator treat them as Running by default
                    rsp.states[(vmUuid)] = KVMConstant.KvmVmState.Running.toString()
                } else {
                    rsp.states[(vmUuid)] = KVMConstant.KvmVmState.fromVmInstanceState(state).toString()
                }
            }
            rsp.setVmInShutdowns(new ArrayList<String>())
            return rsp
        }

        spec.simulator(KVMConstant.KVM_ATTACH_VOLUME) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.AttachDataVolumeCmd.class)
            // assume all data volumes has same deviceType.
            if (Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type).listValues().stream().distinct().count() == 1) {
                assert (cmd.addons["attachedDataVolumes"] as List<VolumeTO>).stream()
                        .allMatch({vol -> vol.deviceType == cmd.volume.deviceType})
            }
            return new KVMAgentCommands.AttachDataVolumeResponse()
        }

        spec.simulator(KVMConstant.KVM_CHECK_PHYSICAL_NETWORK_INTERFACE_PATH) { HttpEntity<String> e ->
            Spec.checkHttpCallType(e, true)
            return new KVMAgentCommands.CheckPhysicalNetworkInterfaceResponse()
        }

        spec.simulator(KVMConstant.KVM_ADD_INTERFACE_TO_BRIDGE_PATH) {
            return new KVMAgentCommands.AddInterfaceToBridgeResponse()
        }

        spec.simulator(KVMConstant.KVM_REALIZE_L2NOVLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CreateBridgeResponse()
        }

        spec.simulator(KVMConstant.KVM_MIGRATE_VM_PATH) {
            return new KVMAgentCommands.MigrateVmResponse()
        }

        spec.simulator(KVMConstant.KVM_GET_CPU_XML_PATH) {
            return new KVMAgentCommands.VmGetCpuXmlResponse()
        }

        spec.simulator(KVMConstant.KVM_COMPARE_CPU_FUNCTION_PATH) {
            return new KVMAgentCommands.VmCompareCpuFunctionResponse()
        }

        spec.simulator(KVMConstant.KVM_CHECK_L2NOVLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CheckBridgeResponse()
        }

        spec.simulator(KVMConstant.KVM_CHECK_L2VLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CheckVlanBridgeResponse()
        }

        spec.simulator(KVMConstant.KVM_REALIZE_L2VLAN_NETWORK_PATH) {
            return new KVMAgentCommands.CreateVlanBridgeResponse()
        }

        spec.simulator(KVMConstant.KVM_CHECK_OVSDPDK_NETWORK_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        spec.simulator(KVMConstant.KVM_REALIZE_OVSDPDK_NETWORK_PATH) {
            return new KVMAgentCommands.CreateBridgeResponse()
        }

        spec.simulator(KVMConstant.KVM_GENERATE_VDPA_PATH) { HttpEntity<String> e ->
            KVMAgentCommands.GenerateVdpaCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.GenerateVdpaCmd.class)
            def rsp = new KVMAgentCommands.GenerateVdpaResponse()

            ArrayList<KVMAgentCommands.NicTO> nics = cmd.getNics()
            def vdpaPaths = new ArrayList<String>()

            nics.each { KVMAgentCommands.NicTO it ->
                vdpaPaths.add("/var/run/zstack-vdpa/" + it.bridgeName + "/" + it.nicInternalName)
            }
            rsp.setVdpaPaths(vdpaPaths)

            return rsp
        }

        spec.simulator(KVMConstant.KVM_DELETE_VDPA_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        spec.simulator(KVMConstant.KVM_GENERATE_VHOST_USER_CLIENT_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        spec.simulator(KVMConstant.KVM_DELETE_VHOST_USER_CLIENT_PATH) {
             return new KVMAgentCommands.AgentResponse()
        }

        spec.simulator(KVMConstant.KVM_SYNC_VM_DEVICEINFO_PATH) { HttpEntity<String> e ->
            SyncVmDeviceInfoCmd cmd = JSONObjectUtil.toObject(e.body, SyncVmDeviceInfoCmd.class)
            def rsp = new SyncVmDeviceInfoResponse()

            rsp.virtualizerInfo = new VirtualizerInfoTO()
            rsp.virtualizerInfo.uuid = cmd.vmInstanceUuid
            rsp.virtualizerInfo.virtualizer = "qemu-kvm"
            rsp.virtualizerInfo.version = "4.2.0-632.g6a6222b.el7"

            return rsp
        }

        spec.simulator(KVMConstant.KVM_START_VM_PATH) { HttpEntity<String> e ->
            StartVmCmd cmd = JSONObjectUtil.toObject(e.body, StartVmCmd.class)
            assert new HashSet<>(cmd.dataVolumes.deviceId).size() == cmd.dataVolumes.size()
            StartVmResponse  rsp = new StartVmResponse()
            rsp.virtualDeviceInfoList = []
            List<VolumeTO> pciInfo = new ArrayList<VolumeTO>()
            pciInfo.add(cmd.rootVolume)
            pciInfo.addAll(cmd.dataVolumes)

            Integer counter = 0
            pciInfo.each { to ->
                VirtualDeviceInfo info = new VirtualDeviceInfo()
                info.resourceUuid = to.volumeUuid
                info.deviceAddress = new DeviceAddress()
                info.deviceAddress.domain = "0000"
                info.deviceAddress.bus = "00"
                info.deviceAddress.slot = Integer.toHexString(counter)
                info.deviceAddress.function = "0"

                counter++

                rsp.virtualDeviceInfoList.add(info)
            }

            rsp.virtualizerInfo = new VirtualizerInfoTO()
            rsp.virtualizerInfo.uuid = cmd.vmInstanceUuid
            rsp.virtualizerInfo.virtualizer = "qemu-kvm"
            rsp.virtualizerInfo.version = "4.2.0-632.g6a6222b.el7"

            return rsp
        }

        spec.simulator(KVMConstant.KVM_STOP_VM_PATH) {
            return new KVMAgentCommands.StopVmResponse()
        }

        spec.simulator(KVMConstant.KVM_PAUSE_VM_PATH) {
            return new KVMAgentCommands.PauseVmResponse()
        }

        spec.simulator(KVMConstant.KVM_RESUME_VM_PATH) {
            return new KVMAgentCommands.ResumeVmResponse()
        }

        spec.simulator(KVMConstant.KVM_REBOOT_VM_PATH) {
            return new KVMAgentCommands.RebootVmResponse()
        }

        spec.simulator(KVMConstant.KVM_DESTROY_VM_PATH) {
            return new KVMAgentCommands.DestroyVmResponse()
        }

        spec.simulator(KVMConstant.KVM_GET_VNC_PORT_PATH) {
            def rsp = new KVMAgentCommands.GetVncPortResponse()
            rsp.port = 5900
            return rsp
        }

        spec.simulator(KVMConstant.KVM_LOGOUT_ISCSI_PATH) {
            return new KVMAgentCommands.LogoutIscsiTargetRsp()
        }

        spec.simulator(KVMConstant.KVM_LOGIN_ISCSI_PATH) {
            return new KVMAgentCommands.LoginIscsiTargetRsp()
        }

        spec.simulator(KVMConstant.KVM_VM_ONLINE_INCREASE_CPU) {
            return new KVMAgentCommands.IncreaseCpuResponse()
        }

        spec.simulator(KVMConstant.KVM_VM_ONLINE_INCREASE_MEMORY) {
            return new KVMAgentCommands.IncreaseMemoryResponse()
        }

        spec.simulator(KVMConstant.KVM_UPDATE_HOST_OS_PATH) {
            return new KVMAgentCommands.UpdateHostOSRsp()
        }

        spec.simulator(KVMConstant.KVM_HOST_UPDATE_DEPENDENCY_PATH) {
            return new KVMAgentCommands.UpdateDependencyRsp()
        }

        spec.simulator(KVMConstant.HOST_UPDATE_SPICE_CHANNEL_CONFIG_PATH) {
            return new KVMAgentCommands.UpdateSpiceChannelConfigResponse()
        }

        spec.simulator(KVMConstant.KVM_SCAN_VM_PORT_STATUS) {
            return new KVMAgentCommands.ScanVmPortResponse()
        }

        spec.simulator(KVMConstant.GET_DEV_CAPACITY) {
            KVMAgentCommands.GetDevCapacityResponse rsp = new KVMAgentCommands.GetDevCapacityResponse()
            rsp.totalSize = SizeUnit.GIGABYTE.toByte(100)
            rsp.availableSize = SizeUnit.GIGABYTE.toByte(80)
            return rsp
        }

        spec.simulator(KVMConstant.KVM_CONFIG_PRIMARY_VM_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        spec.simulator(KVMConstant.KVM_CONFIG_SECONDARY_VM_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        spec.simulator(KVMConstant.KVM_START_COLO_SYNC_PATH) {
            return new KVMAgentCommands.AgentResponse()
        }

        spec.simulator(KVMConstant.KVM_REGISTER_PRIMARY_VM_HEARTBEAT) {
            return new KVMAgentCommands.AgentResponse()
        }

        spec.simulator(KVMConstant.KVM_HOST_CHECK_FILE_PATH) { HttpEntity<String> e ->
            KVMAgentCommands.CheckFileOnHostCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.CheckFileOnHostCmd.class)
            KVMAgentCommands.CheckFileOnHostResponse response = new KVMAgentCommands.CheckFileOnHostResponse()
            response.existPaths = new HashMap<>()
            cmd.paths.forEach({path -> response.existPaths.put(path, "")})
            return response
        }

        spec.simulator(KVMConstant.KVM_HOST_NUMA_PATH) {
            def rsp = new  KVMAgentCommands.GetHostNUMATopologyResponse()
            return rsp
        }

        spec.simulator(KVMConstant.KVM_HOST_ATTACH_VOLUME_PATH) {
            def rsp = new KVMAgentCommands.AttachVolumeRsp()
            rsp.device = "/dev/nbd0"
            return rsp
        }

        spec.simulator(KVMConstant.KVM_HOST_DETACH_VOLUME_PATH) {
            def rsp = new KVMAgentCommands.DetachVolumeRsp()
            return rsp
        }
    }
}
