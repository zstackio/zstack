package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.Platform
import org.zstack.kvm.KVMAgentCommands
import org.zstack.storage.zbs.LogicalPoolInfo
import org.zstack.cbd.kvm.KvmCbdCommands
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.zbs.ZbsPrimaryStorageMdsBase
import org.zstack.storage.zbs.ZbsStorageController
import org.zstack.utils.Utils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.logging.CLogger
import org.zstack.utils.gson.JSONObjectUtil

import javax.servlet.http.HttpServletRequest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Xingwei Yu
 * @date 2024/4/19 下午2:28
 */
class ExternalPrimaryStorageSpec extends PrimaryStorageSpec {
    private static final CLogger logger = Utils.getLogger(ExternalPrimaryStorageSpec.class);


    static ConcurrentHashMap<String, String> mdsAddrVersionHashMap = new ConcurrentHashMap<>()

    @SpecParam(required = true)
    String identity
    @SpecParam(required = true)
    String defaultOutputProtocol
    @SpecParam(required = true)
    String config
    @SpecParam(required = true)
    String url

    ExternalPrimaryStorageSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    static class Simulators implements Simulator {
        @Override
        void registerSimulators(EnvSpec espec) {
            def simulator = { arg1, arg2 ->
                espec.simulator(arg1, arg2)
            }

            def actualSize = SizeUnit.GIGABYTE.toByte(1)
            def targetSize = SizeUnit.GIGABYTE.toByte(2)

            simulator(ZbsPrimaryStorageMdsBase.ECHO_PATH) { HttpEntity<String> entity ->
                checkHttpCallType(entity, true)
                return [:]
            }

            simulator(ZbsPrimaryStorageMdsBase.PING_PATH) { HttpEntity<String> e ->
                def cmd = JSONObjectUtil.toObject(e.body, ZbsPrimaryStorageMdsBase.PingCmd.class)
                ZbsPrimaryStorageMdsBase.PingRsp rsp = new ZbsPrimaryStorageMdsBase.PingRsp()
                rsp.success = true
                rsp.agentVersion = mdsAddrVersionHashMap.get(cmd.addr)
                return rsp
            }

            simulator(ZbsPrimaryStorageMdsBase.SYNC_METADATA_PATH) { HttpEntity<String> e ->
                def cmd = JSONObjectUtil.toObject(e.body, ZbsPrimaryStorageMdsBase.SyncMetadataCmd.class)
                ZbsPrimaryStorageMdsBase.SyncMetadataRsp rsp = new ZbsPrimaryStorageMdsBase.SyncMetadataRsp()
                rsp.success = true
                rsp.externalAddr = "127.0.0.1"
                mdsAddrVersionHashMap.put(cmd.addr, cmd.agentVersion)
                return rsp
            }

            simulator(ZbsStorageController.DEPLOY_CLIENT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.DeployClientCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.DeployClientCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.DeployClientRsp()
                rsp.success = true

                return rsp
            }

            simulator(ZbsStorageController.UPDATE_HOST_DEPENDENCY_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def rsp = new ZbsStorageController.UpdateHostDependencyRsp()
                rsp.success = true

                return rsp
            }

            simulator(ZbsStorageController.GET_FACTS_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.GetFactsCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.GetFactsCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.GetFactsRsp()
                rsp.uuid = "123456789"
                rsp.version = "1.6.1-for-test"
                rsp.success = true

                return rsp
            }

            simulator(ZbsStorageController.CHECK_HOST_STORAGE_CONNECTION_PATH) { HttpEntity<String> e ->
                ZbsStorageController.CheckHostStorageConnectionCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CheckHostStorageConnectionCmd)
                assert cmd.hostUuid != null

                def rsp = new ZbsStorageController.CheckHostStorageConnectionRsp()
                rsp.success = true

                return rsp
            }

            simulator(ZbsStorageController.GET_CAPACITY_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.GetCapacityCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.GetCapacityCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                LogicalPoolInfo.RedundanceAndPlaceMentPolicy redundanceAndPlaceMentPolicy = new LogicalPoolInfo.RedundanceAndPlaceMentPolicy()
                redundanceAndPlaceMentPolicy.setCopysetNum(300)
                redundanceAndPlaceMentPolicy.setReplicaNum(3)
                redundanceAndPlaceMentPolicy.setZoneNum(3)

                LogicalPoolInfo logicalPoolInfo = new LogicalPoolInfo()
                logicalPoolInfo.setPhysicalPoolID(1);
                logicalPoolInfo.setRedundanceAndPlaceMentPolicy(redundanceAndPlaceMentPolicy);
                logicalPoolInfo.setLogicalPoolID(1);
                logicalPoolInfo.setUsedSize(322961408);
                logicalPoolInfo.setQuota(0);
                logicalPoolInfo.setCreateTime(1735875794);
                logicalPoolInfo.setType(0);
                logicalPoolInfo.setRawWalUsedSize(0);
                logicalPoolInfo.setAllocateStatus(0);
                logicalPoolInfo.setRawUsedSize(968884224);
                logicalPoolInfo.setPhysicalPoolName("pool1");
                logicalPoolInfo.setCapacity(579933831168);
                logicalPoolInfo.setLogicalPoolName(cmd.logicalPoolNames[0]);
                logicalPoolInfo.setUserPolicy("eyJwb2xpY3kiIDogMX0=");
                logicalPoolInfo.setAllocatedSize(3221225472);

                List<LogicalPoolInfo> logicalPoolInfos = new ArrayList<>()
                logicalPoolInfos.add(logicalPoolInfo)

                logicalPoolInfos.add(new LogicalPoolInfo(
                    physicalPoolID: 2,
                    logicalPoolID: 2,
                    logicalPoolName: "lpool2",
                    physicalPoolName: "pool2",
                    capacity: 579933831168,
                    usedSize: 123456789,
                    allocatedSize: 987654321,
                    quota: 0,
                    createTime: 1735875794,
                    type: 0,
                    rawWalUsedSize: 0,
                    allocateStatus: 0,
                    rawUsedSize: 123456789,
                    redundanceAndPlaceMentPolicy: redundanceAndPlaceMentPolicy,
                    userPolicy: "eyJwb2xpY3kiIDogMX0="
                ))

                def rsp = new ZbsStorageController.GetCapacityRsp()
                rsp.setLogicalPoolInfos(logicalPoolInfos)

                return rsp
            }

            simulator(ZbsStorageController.CREATE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.CreateVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CreateVolumeCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.CreateVolumeRsp()
                rsp.setSize(cmd.getSizeInBytes())
                rsp.setActualSize(actualSize)
                rsp.setInstallPath(String.format("cbd:pool1/%s/%s", cmd.logicalPool, cmd.volume))

                return rsp
            }

            simulator(ZbsStorageController.DELETE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.DeleteVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.DeleteVolumeCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                return new ZbsStorageController.DeleteVolumeRsp()
            }

            simulator(ZbsStorageController.CREATE_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.CreateSnapshotCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CreateSnapshotCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.CreateSnapshotRsp()
                rsp.setSize(actualSize)
                rsp.setInstallPath(cmd.path + "@" + cmd.snapshot)

                return rsp
            }

            simulator(ZbsStorageController.CLONE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.CloneVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CloneVolumeCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.CloneVolumeRsp()
                rsp.setSize(actualSize)
                // replace volume name
                rsp.setInstallPath(cmd.path.replaceAll("([^/]+)\$", cmd.dstVolume))
                return rsp
            }

            simulator(ZbsStorageController.QUERY_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.QueryVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.QueryVolumeCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.QueryVolumeRsp()
                rsp.setSize(actualSize)

                return rsp
            }

            simulator(ZbsStorageController.EXPAND_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.ExpandVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.ExpandVolumeCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.ExpandVolumeRsp()
                rsp.setSize(targetSize)

                return rsp
            }

            simulator(ZbsStorageController.COPY_PATH) { HttpEntity<String> e, EnvSpec spec ->
                ZbsStorageController.CopyCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CopyCmd.class)
                ExternalPrimaryStorageSpec zspec = spec.specByUuid(cmd.uuid)
                assert zspec != null: "cannot found zbs primary storage[uuid:${cmd.uuid}], check your environment()."

                def rsp = new ZbsStorageController.CopyRsp()
                if (cmd.dstPool != null) {
                    // lpool1 physical pool is pool1
                    rsp.installPath = "cbd:${cmd.dstPool.substring(1)}/${cmd.dstPool}/${cmd.dstVolume}"
                } else {
                    rsp.setInstallPath(cmd.path.replaceAll("([^/]+)\$", cmd.dstVolume))
                }
                rsp.setSize(actualSize)

                return rsp
            }


            simulator(ZbsStorageController.GET_VOLUME_CLIENTS_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new ZbsStorageController.GetVolumeClientsRsp()
            }

            simulator(KvmCbdCommands.SETUP_CBD_SELF_FENCER_PATH) {
                return new KvmCbdCommands.AgentRsp()
            }

            simulator(KvmCbdCommands.CANCEL_CBD_SELF_FENCER_PATH) {
                return new KvmCbdCommands.AgentRsp()
            }
        }
    }

    static class ExponSimulators implements Simulator {
        static final long TOTAL_CAPACITY = SizeUnit.TERABYTE.toByte(2)
        static final long AVAILABLE_CAPACITY = SizeUnit.TERABYTE.toByte(1)
        static final String POOL_ID = "test-pool-id-001"
        static final String POOL_NAME = "pool"
        static final String TIANSHU_ID = "test-tianshu-id-001"
        static final String TIANSHU_NAME = "tianshu"

        static ConcurrentHashMap<String, Map> volumes = new ConcurrentHashMap<>()
        static ConcurrentHashMap<String, Map> snapshots = new ConcurrentHashMap<>()
        static Set<String> vhostBoundUss = ConcurrentHashMap.newKeySet()
        static ConcurrentHashMap<String, String> vhostNameToId = new ConcurrentHashMap<>()
        // Track iSCSI client group to snapshot mappings: clientId -> Set<snapId>
        static ConcurrentHashMap<String, Set<String>> iscsiClientSnapshots = new ConcurrentHashMap<>()
        static AtomicInteger volumeCounter = new AtomicInteger(0)
        static AtomicInteger snapshotCounter = new AtomicInteger(0)

        static void clear() {
            volumes.clear()
            snapshots.clear()
            vhostBoundUss.clear()
            vhostNameToId.clear()
            iscsiClientSnapshots.clear()
            volumeCounter.set(0)
            snapshotCounter.set(0)
        }

        @Override
        void registerSimulators(EnvSpec espec) {
            def simulator = { arg1, arg2 ->
                espec.simulator(arg1, arg2)
            }

            // Login: POST /api/v1/login
            simulator("/api/v1/login") {
                return [ret_code: "0", message: "", access_token: "test-session-token", refresh_token: "test-refresh-token", token_type: "Bearer"]
            }

            // Logout: POST /api/v1/v2/logout
            simulator("/api/v1/v2/logout") {
                return [ret_code: "0", message: ""]
            }

            // Task status: GET /api/v1/tasks/{id}
            simulator("/api/v1/tasks/.*") {
                return [ret_code: "0", message: "", status: "SUCCESS", ret_msg: "", progress: 100, id: "test-task-id"]
            }

            // Query pools (QueryFailureDomainRequest): GET /api/v2/failure_domain
            simulator("/api/v2/failure_domain") {
                return [ret_code: "0", message: "", total: 1, failure_domains: [
                    [id: POOL_ID, failure_domain_name: POOL_NAME, valid_size: TOTAL_CAPACITY,
                     real_data_size: TOTAL_CAPACITY - AVAILABLE_CAPACITY, raw_size: TOTAL_CAPACITY * 3,
                     data_size: TOTAL_CAPACITY - AVAILABLE_CAPACITY, redundancy_ploy: "replicated",
                     replicate_size: 3, health_status: "health", run_status: "normal",
                     tianshu_id: TIANSHU_ID, tianshu_name: TIANSHU_NAME,
                     create_time: System.currentTimeMillis(), update_time: System.currentTimeMillis()]
                ]]
            }

            // Get pool detail (GetFailureDomainRequest): GET /api/v2/failure_domain/{id}
            simulator("/api/v2/failure_domain/[^/]+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                return [ret_code: "0", message: "", members: [
                    id: POOL_ID, failure_domain_name: POOL_NAME, valid_size: TOTAL_CAPACITY,
                    real_data_size: TOTAL_CAPACITY - AVAILABLE_CAPACITY, raw_size: TOTAL_CAPACITY * 3,
                    data_size: TOTAL_CAPACITY - AVAILABLE_CAPACITY, redundancy_ploy: "replicated",
                    replicate_size: 3, health_status: "health", run_status: "normal",
                    tianshu_id: TIANSHU_ID, tianshu_name: TIANSHU_NAME,
                    create_time: System.currentTimeMillis(), update_time: System.currentTimeMillis()
                ]]
            }

            // Get blacklist (GetFailureDomainBlacklistRequest): GET /api/v2/failure_domain/black_list/{id}
            simulator("/api/v2/failure_domain/black_list/[^/]+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                return [ret_code: "0", message: "", entries: []]
            }

            // Clear blacklist: PUT /api/v2/failure_domain/black_list/clean
            simulator("/api/v2/failure_domain/black_list/clean") {
                return [ret_code: "0", message: ""]
            }

            // Add volume path to blacklist: PUT /api/v2/failure_domain/black_list
            simulator("/api/v2/failure_domain/black_list") {
                return [ret_code: "0", message: ""]
            }

            // Query clusters (QueryTianshuClusterRequest): GET /api/v2/tianshu
            simulator("/api/v2/tianshu") {
                return [ret_code: "0", message: "", total: 1, result: [
                    [id: TIANSHU_ID, name: TIANSHU_NAME, health_status: "health", run_status: "normal",
                     create_time: System.currentTimeMillis(), update_time: System.currentTimeMillis()]
                ]]
            }

            // Query iSCSI targets (QueryIscsiTargetRequest): GET /api/v2/block/iscsi/gateways
            // also matches sync variant
            simulator("/api/v2/(sync/)?block/iscsi/gateways") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def targetName = req.getParameter("name") ?: "iscsi-target-default"
                def targetId = "test-iscsi-target-" + targetName
                return [ret_code: "0", message: "", total: 1, gateways: [
                    [id: targetId, name: targetName, status: "health", port: 3260,
                     iqn: "iqn.2022-07.com.expontech.wds:" + targetId,
                     tianshu_id: TIANSHU_ID, tianshu_name: TIANSHU_NAME,
                     create_time: System.currentTimeMillis(), update_time: System.currentTimeMillis()]
                ]]
            }

            // Create iSCSI target: POST /api/v2/sync/block/iscsi/gateways
            simulator("/api/v2/sync/block/iscsi/gateways") { HttpEntity<String> e ->
                def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                def targetId = Platform.getUuid()
                def targetName = body?.name ?: "iscsi-target-" + targetId
                return [ret_code: "0", message: "", id: targetId, name: targetName]
            }

            // iSCSI target operations with id: GET/DELETE/PUT /api/v2/[sync/]block/iscsi/gateways/{id}/...
            simulator("/api/v2/(sync/)?block/iscsi/gateways/[^/]+(/.*)?") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def uri = req.getRequestURI()
                def matcher = (uri =~ /\/block\/iscsi\/gateways\/([^\/]+)/)
                def targetId = matcher ? matcher[0][1] : Platform.getUuid()
                def targetName = targetId.startsWith("test-iscsi-target-") ? targetId.substring("test-iscsi-target-".length()) : targetId

                return [ret_code: "0", message: "",
                        id: targetId, name: targetName,
                        iqn: "iqn.2022-07.com.expontech.wds:" + targetId,
                        port: 3260, lun_count: 0,
                        total: 0, gateways: [],
                        nodes: [
                            [gateway_ip: "127.0.0.1", manager_ip: "localhost", name: "localhost",
                             server_id: Platform.getUuid(), tianshu_id: TIANSHU_ID,
                             uss_gw_id: "test-uss-vhost_localhost", uss_name: "iscsi_zstack"]
                        ],
                        server: []]
            }

            // Query iSCSI client groups: GET /api/v2/block/iscsi/clients
            simulator("/api/v2/(sync/)?block/iscsi/clients") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def nameParam = req.getParameter("name")
                if (nameParam != null) {
                    // Query by name
                    def clientId = "test-iscsi-client-" + nameParam
                    int snapNum = iscsiClientSnapshots.getOrDefault(clientId, Collections.emptySet()).size()
                    return [ret_code: "0", message: "", total: 1, clients: [
                        [id: clientId, name: nameParam, status: "health", hosts: [],
                         iscsi_gw_count: 0, snap_num: snapNum,
                         create_time: System.currentTimeMillis(), update_time: System.currentTimeMillis()]
                    ]]
                }
                // Query all - return all tracked clients with snapshots
                def allClients = iscsiClientSnapshots.collect { cId, snaps ->
                    [id: cId, name: cId, status: "health", hosts: [],
                     iscsi_gw_count: 0, snap_num: snaps.size(),
                     create_time: System.currentTimeMillis(), update_time: System.currentTimeMillis()]
                }
                if (allClients.isEmpty()) {
                    allClients = [[id: "test-iscsi-client-default", name: "iscsi-client-default", status: "health", hosts: [],
                         iscsi_gw_count: 0, snap_num: 0,
                         create_time: System.currentTimeMillis(), update_time: System.currentTimeMillis()]]
                }
                return [ret_code: "0", message: "", total: allClients.size(), clients: allClients]
            }

            // Create iSCSI client group: POST /api/v2/sync/block/iscsi/clients
            simulator("/api/v2/sync/block/iscsi/clients") {
                def clientId = Platform.getUuid()
                return [ret_code: "0", message: "", id: clientId]
            }

            // iSCSI client group operations with id (including snapshot attachment)
            simulator("/api/v2/(sync/)?block/iscsi/clients/[^/]+(/.*)?") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def uri = req.getRequestURI()
                def matcher = (uri =~ /\/block\/iscsi\/clients\/([^\/]+)/)
                def clientId = matcher ? matcher[0][1] : ""

                // Handle snapshot add/remove: PUT /block/iscsi/clients/{id}/snapshots
                if (uri.contains("/snapshots") && "PUT".equalsIgnoreCase(req.getMethod())) {
                    def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                    def action = body?.action
                    def luns = body?.luns ?: []
                    luns.each { lun ->
                        def snapId = lun?.id ?: lun?.lun_id
                        if (snapId != null) {
                            if ("add".equals(action)) {
                                iscsiClientSnapshots.computeIfAbsent(clientId, { ConcurrentHashMap.newKeySet() }).add(snapId.toString())
                            } else if ("remove".equals(action)) {
                                iscsiClientSnapshots.getOrDefault(clientId, Collections.emptySet()).remove(snapId.toString())
                            }
                        }
                    }
                    return [ret_code: "0", message: ""]
                }

                // Handle snapshot query: GET /block/iscsi/clients/{id}/snapshots
                if (uri.contains("/snapshots") && "GET".equalsIgnoreCase(req.getMethod())) {
                    def snaps = iscsiClientSnapshots.getOrDefault(clientId, Collections.emptySet())
                    def lunList = snaps.collect { snapId -> [id: snapId, lun_id: snapId] }
                    return [ret_code: "0", message: "", total: lunList.size(), luns: lunList, snapshots: lunList]
                }

                // Handle gateways query: GET /block/iscsi/clients/{id}/gateways
                if (uri.contains("/gateways") && "GET".equalsIgnoreCase(req.getMethod())) {
                    return [ret_code: "0", message: "", total: 1, gateways: [
                        [id: "test-iscsi-target-default", name: "iscsi-target-default",
                         iqn: "iqn.2022-07.com.expontech.wds:test-iscsi-target-default",
                         port: 3260, run_status: "normal",
                         create_time: System.currentTimeMillis(), update_time: System.currentTimeMillis()]
                    ]]
                }

                // Handle client GET by ID: GET /block/iscsi/clients/{id}
                if ("GET".equalsIgnoreCase(req.getMethod())) {
                    int snapNum = iscsiClientSnapshots.getOrDefault(clientId, Collections.emptySet()).size()
                    return [ret_code: "0", message: "", id: clientId, name: clientId, hosts: [],
                            run_status: "normal", snap_num: snapNum, vol_num: 0, iscsi_gw_count: 1,
                            create_time: System.currentTimeMillis(), update_time: System.currentTimeMillis()]
                }

                return [ret_code: "0", message: "", total: 0, gateways: [], luns: [], snapshots: []]
            }

            // Query USS gateways (QueryUssGatewayRequest): GET /api/v2/wds/uss
            simulator("/api/v2/(sync/)?wds/uss") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def nameParam = req.getParameter("name")
                def ussName = nameParam ?: "vhost_localhost"
                def ussId = "test-uss-" + ussName
                return [ret_code: "0", message: "", total: 1, uss_gateways: [
                    [id: ussId, name: ussName, type: "uss", status: "health",
                     tianshu_id: TIANSHU_ID, tianshu_name: TIANSHU_NAME,
                     manager_ip: "127.0.0.1", business_port: 4420, business_network: "127.0.0.1/8",
                     create_time: System.currentTimeMillis(), update_time: System.currentTimeMillis()]
                ]]
            }

            // Query vhost controllers (QueryVhostControllerRequest): GET /api/v2/block/vhost
            simulator("/api/v2/(sync/)?block/vhost") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def vhostName = req.getParameter("name")
                if (vhostName != null && !vhostNameToId.containsKey(vhostName)) {
                    return [ret_code: "0", message: "", total: 0, vhosts: []]
                }
                vhostName = vhostName ?: "vhost-default"
                def vhostId = vhostNameToId.getOrDefault(vhostName, "test-vhost-" + vhostName)
                return [ret_code: "0", message: "", total: 1, vhosts: [
                    [id: vhostId, name: vhostName, status: "health",
                     path: "/var/run/vhost/" + vhostName,
                     create_time: System.currentTimeMillis(), update_time: System.currentTimeMillis()]
                ]]
            }

            // Create vhost controller: POST /api/v2/sync/block/vhost
            simulator("/api/v2/sync/block/vhost") { HttpEntity<String> e ->
                def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                def vhostId = Platform.getUuid()
                def vhostName = body?.name ?: "vhost-" + vhostId
                vhostNameToId.put(vhostName, vhostId)
                return [ret_code: "0", message: "", id: vhostId, name: vhostName]
            }

            // Vhost controller operations with id (including bind/unbind USS, DELETE)
            simulator("/api/v2/(sync/)?block/vhost/[^/]+(/.*)?") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def uri = req.getRequestURI()

                // Handle DELETE vhost controller
                if ("DELETE".equalsIgnoreCase(req.getMethod())) {
                    def matcher = (uri =~ /\/block\/vhost\/([^\/]+)/)
                    def vhostId = matcher ? matcher[0][1] : null
                    if (vhostId != null) {
                        vhostNameToId.entrySet().removeIf { it.value == vhostId }
                        vhostBoundUss.remove(vhostId)
                    }
                    return [ret_code: "0", message: ""]
                }

                if (uri.contains("bind_uss") && !uri.contains("vhost_binded_uss")) {
                    def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                    def vhostId = body?.vhost_id
                    if (vhostId != null) {
                        if (uri.contains("unbind_uss")) {
                            vhostBoundUss.remove(vhostId)
                        } else {
                            vhostBoundUss.add(vhostId)
                        }
                    }
                    return [ret_code: "0", message: ""]
                }
                if (uri.contains("vhost_binded_uss")) {
                    def matcher = (uri =~ /\/block\/vhost\/([^\/]+)\/vhost_binded_uss/)
                    def vhostId = matcher ? matcher[0][1] : null
                    if (vhostId != null && vhostBoundUss.contains(vhostId)) {
                        return [ret_code: "0", message: "", uss: [
                            [id: "test-uss-vhost_localhost", name: "vhost_localhost", type: "uss", status: "health",
                             tianshu_id: TIANSHU_ID, tianshu_name: TIANSHU_NAME,
                             manager_ip: "127.0.0.1", business_port: 4420, business_network: "127.0.0.1/8"]
                        ]]
                    }
                    return [ret_code: "0", message: "", uss: []]
                }
                return [ret_code: "0", message: "", uss: []]
            }

            // Query NVMf targets: GET /api/v2/block/nvmf
            simulator("/api/v2/(sync/)?block/nvmf") {
                return [ret_code: "0", message: "", total: 0, nvmfs: []]
            }

            // Create NVMf target: POST /api/v2/sync/block/nvmf
            simulator("/api/v2/sync/block/nvmf") {
                def nvmfId = Platform.getUuid()
                return [ret_code: "0", message: "", id: nvmfId]
            }

            // NVMf bind/unbind USS
            simulator("/api/v2/sync/block/nvmf/(un)?bind_uss") {
                return [ret_code: "0", message: ""]
            }

            // NVMf target operations with id
            simulator("/api/v2/(sync/)?block/nvmf/[^/]+(/.*)?") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                return [ret_code: "0", message: "", uss: []]
            }

            // Query NVMf client groups
            simulator("/api/v2/(sync/)?block/nvmf_client/?") {
                return [ret_code: "0", message: "", total: 0, clients: []]
            }

            // Create NVMf client group: POST /api/v2/sync/block/nvmf_client
            simulator("/api/v2/sync/block/nvmf_client") {
                def clientId = Platform.getUuid()
                return [ret_code: "0", message: "", id: clientId]
            }

            // NVMf client group operations with id
            simulator("/api/v2/(sync/)?block/nvmf_client/[^/]+(/.*)?") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                return [ret_code: "0", message: ""]
            }

            // Create volume: POST /api/v2/sync/block/volumes
            simulator("/api/v2/sync/block/volumes") { HttpEntity<String> e ->
                def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                def volId = Platform.getUuid()
                def volName = body?.name ?: "vol-" + volumeCounter.incrementAndGet()
                long volSize = body?.volume_size ?: SizeUnit.GIGABYTE.toByte(1)

                volumes.put(volId, [
                    id: volId, name: volName, volume_name: volName, pool_id: POOL_ID, pool_name: POOL_NAME,
                    volume_size: volSize, data_size: 0, is_delete: false, run_status: "normal",
                    wwn: "wwn-" + volId
                ])

                return [ret_code: "0", message: "", id: volId]
            }

            // Query volumes (QueryVolumeRequest): GET /api/v2/block/volumes
            simulator("/api/v2/block/volumes") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def allVols = volumes.values().toList()
                return [ret_code: "0", message: "", total: allVols.size(), volumes: allVols]
            }

            // Get volume detail (GetVolumeRequest): GET /api/v2/block/volumes/{volId}
            simulator("/api/v2/block/volumes/[^/]+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def uri = req.getRequestURI()
                def volId = uri.substring(uri.lastIndexOf("/") + 1)
                def vol = volumes.get(volId)
                if (vol == null) {
                    // try lookup by stripping dashes (expon IDs may or may not have dashes)
                    vol = volumes.get(volId.replace("-", ""))
                }
                if (vol == null) {
                    vol = [id: volId, name: "unknown", volume_name: "unknown", pool_id: POOL_ID, pool_name: POOL_NAME,
                           volume_size: SizeUnit.GIGABYTE.toByte(1), data_size: 0, is_delete: false, run_status: "normal"]
                }
                return [ret_code: "0", message: "", volume_detail: vol]
            }

            // Delete volume: DELETE /api/v2/sync/block/volumes/{volId}
            simulator("/api/v2/sync/block/volumes/[^/]+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def uri = req.getRequestURI()
                def segments = uri.split("/")
                def volId = segments[segments.length - 1]
                volumes.remove(volId)
                return [ret_code: "0", message: ""]
            }

            // Expand volume: PUT /api/v2/sync/block/volumes/{id}/expand
            simulator("/api/v2/sync/block/volumes/[^/]+/expand") { HttpEntity<String> e ->
                return [ret_code: "0", message: ""]
            }

            // Set volume QoS: PUT /api/v2/sync/block/volumes/{volId}/qos
            simulator("/api/v2/sync/block/volumes/[^/]+/qos") { HttpEntity<String> e ->
                return [ret_code: "0", message: ""]
            }

            // Get volume LUN detail: GET /api/v2/sync/block/volumes/{volId}/lun_detail
            simulator("/api/v2/sync/block/volumes/[^/]+/lun_detail") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                return [ret_code: "0", message: "", lun_details: [[lun_id: 0, lun_name: "lun-0"]]]
            }

            // Get volume bound path: GET /api/v2/sync/block/volumes/{volId}/bind_status
            simulator("/api/v2/sync/block/volumes/[^/]+/bind_status") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                return [ret_code: "0", message: "", bind_paths: []]
            }

            // Get volume bound iSCSI client groups: GET /api/v2/block/volumes/{volumeId}/clients
            simulator("/api/v2/block/volumes/[^/]+/clients") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                return [ret_code: "0", message: "", clients: []]
            }

            // Recovery volume snapshot: PUT /api/v2/sync/block/volumes/{volumeId}/recovery
            simulator("/api/v2/sync/block/volumes/[^/]+/recovery") { HttpEntity<String> e ->
                return [ret_code: "0", message: ""]
            }

            // Get volume task progress: GET /api/v2/sync/block/volumes/tasks/{taskId}
            simulator("/api/v2/sync/block/volumes/tasks/.*") {
                return [ret_code: "0", message: "", status: "SUCCESS", progress: 100]
            }

            // Update volume name: PUT /api/v2/block/volumes/{id}/name
            simulator("/api/v2/block/volumes/[^/]+/name") { HttpEntity<String> e ->
                return [ret_code: "0", message: ""]
            }

            // Create snapshot: POST /api/v2/sync/block/snaps
            simulator("/api/v2/sync/block/snaps") { HttpEntity<String> e ->
                def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                def snapId = Platform.getUuid()
                def snapName = body?.name ?: "snap-" + snapshotCounter.incrementAndGet()
                def volId = body?.volume_id ?: ""

                def vol = volumes.get(volId)
                long snapSize = vol != null ? (long) vol.get("volume_size") : SizeUnit.GIGABYTE.toByte(1)

                snapshots.put(snapId, [
                    id: snapId, name: snapName, snap_name: snapName, snap_size: snapSize,
                    data_size: 0, volume_id: volId, volume_name: vol?.get("name") ?: "",
                    pool_id: POOL_ID, pool_name: POOL_NAME, is_delete: false,
                    wwn: "wwn-snap-" + snapId
                ])

                return [ret_code: "0", message: "", id: snapId]
            }

            // Query snapshots (QueryVolumeSnapshotRequest): GET /api/v2/block/snaps
            simulator("/api/v2/block/snaps") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def allSnaps = snapshots.values().toList()
                return [ret_code: "0", message: "", total: allSnaps.size(), snaps: allSnaps, volumes: []]
            }

            // Get snapshot detail: GET /api/v2/sync/block/snaps/{id}
            simulator("/api/v2/(sync/)?block/snaps/[^/]+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def uri = req.getRequestURI()
                def snapId = uri.substring(uri.lastIndexOf("/") + 1)
                def snap = snapshots.get(snapId)
                if (snap == null) {
                    snap = [id: snapId, name: "unknown", snap_name: "unknown", snap_size: SizeUnit.GIGABYTE.toByte(1),
                            data_size: 0, volume_id: "", pool_id: POOL_ID, pool_name: POOL_NAME, is_delete: false]
                }
                return [ret_code: "0", message: "", snap_detail: snap]
            }

            // Delete snapshot: DELETE /api/v2/sync/block/snaps/{snapshotId}
            simulator("/api/v2/sync/block/snaps/[^/]+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def uri = req.getRequestURI()
                def snapId = uri.substring(uri.lastIndexOf("/") + 1)
                snapshots.remove(snapId)
                return [ret_code: "0", message: ""]
            }

            // Clone volume from snapshot: POST /api/v2/sync/block/snaps/{snapshotId}/clone
            simulator("/api/v2/sync/block/snaps/[^/]+/clone") { HttpEntity<String> e ->
                def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                def volId = Platform.getUuid()
                def volName = body?.name ?: "clone-" + volumeCounter.incrementAndGet()

                volumes.put(volId, [
                    id: volId, name: volName, volume_name: volName, pool_id: POOL_ID, pool_name: POOL_NAME,
                    volume_size: SizeUnit.GIGABYTE.toByte(1), data_size: 0, is_delete: false, run_status: "normal",
                    wwn: "wwn-" + volId
                ])

                return [ret_code: "0", message: "", id: volId]
            }

            // Copy snapshot: PUT /api/v2/sync/block/snaps/{snapshotId}/copy_clone
            simulator("/api/v2/sync/block/snaps/[^/]+/copy_clone") { HttpEntity<String> e ->
                return [ret_code: "0", message: "", task_id: Platform.getUuid()]
            }

            // Update snapshot: PUT /api/v2/block/snaps/{id}
            simulator("/api/v2/block/snaps/[^/]+") { HttpEntity<String> e ->
                return [ret_code: "0", message: ""]
            }

            // Set trash expire time: PUT /api/v1/sys_config/trash_recycle
            simulator("/api/v1/sys_config/trash_recycle") {
                return [ret_code: "0", message: ""]
            }
        }
    }

    static class XinfiniSimulators implements Simulator {
        static final int POOL_ID = 1
        static final String POOL_NAME = "pool1"
        static final int BS_POLICY_ID = 1
        static final String CLUSTER_UUID = "test-xinfini-cluster-uuid"
        static final int BDC_ID = 1
        static final int ISCSI_GATEWAY_ID = 1
        static final long TOTAL_CAPACITY_KB = 2L * 1024 * 1024 * 1024  // 2TB in KB
        static final long USED_CAPACITY_KB = 700L * 1024 * 1024        // ~0.7TB in KB

        static ConcurrentHashMap<Integer, Map> volumes = new ConcurrentHashMap<>()
        static ConcurrentHashMap<Integer, Map> snapshots = new ConcurrentHashMap<>()
        static ConcurrentHashMap<Integer, Map> bdcBdevs = new ConcurrentHashMap<>()
        static ConcurrentHashMap<Integer, Map> iscsiClients = new ConcurrentHashMap<>()
        static ConcurrentHashMap<Integer, Map> iscsiClientGroups = new ConcurrentHashMap<>()
        static ConcurrentHashMap<Integer, Map> volumeClientGroupMappings = new ConcurrentHashMap<>()

        static AtomicInteger volumeCounter = new AtomicInteger(0)
        static AtomicInteger snapshotCounter = new AtomicInteger(0)
        static AtomicInteger bdcBdevCounter = new AtomicInteger(0)
        static AtomicInteger iscsiClientCounter = new AtomicInteger(0)
        static AtomicInteger iscsiClientGroupCounter = new AtomicInteger(0)
        static AtomicInteger volumeClientGroupMappingCounter = new AtomicInteger(0)

        static void clear() {
            volumes.clear()
            snapshots.clear()
            bdcBdevs.clear()
            iscsiClients.clear()
            iscsiClientGroups.clear()
            volumeClientGroupMappings.clear()
            volumeCounter.set(0)
            snapshotCounter.set(0)
            bdcBdevCounter.set(0)
            iscsiClientCounter.set(0)
            iscsiClientGroupCounter.set(0)
            volumeClientGroupMappingCounter.set(0)
        }

        static Map makeQueryResponse(List items) {
            return [
                metadata: [pagination: [count: items.size(), total_count: items.size(), offset: 0, limit: 100]],
                items: items
            ]
        }

        static Map makeItemResponse(Map item) {
            return [
                metadata: [id: item.spec?.id, name: item.spec?.name, state: [state: "active"]],
                spec: item.spec,
                status: item.status
            ]
        }

        static Map makeDeleteResponse() {
            return [:]
        }

        static Map makeNotFoundResponse() {
            throw new HttpError(404, "not found")
        }

        static List filterItems(List<Map> items, String qParam) {
            if (qParam == null || qParam.isEmpty()) {
                return items
            }

            // Strip outer parentheses pairs
            String q = qParam.trim()

            // Handle compound AND filters: ((spec.field1:val1) AND (spec.field2:val2))
            if (q.contains(" AND ")) {
                def parts = q.split(" AND ")
                List result = items
                for (String part : parts) {
                    String cleaned = part.replaceAll("[()]", "").trim()
                    result = applySimpleFilter(result, cleaned)
                }
                return result
            }

            // Simple filter: spec.field:value or (val1 val2) list
            String cleaned = q.replaceAll("^\\(+", "").replaceAll("\\)+\$", "").trim()
            return applySimpleFilter(items, cleaned)
        }

        static List applySimpleFilter(List<Map> items, String filter) {
            // Match pattern: spec.field:value or spec.field:(val1 val2 ...)
            def matcher = (filter =~ /spec\.(\w+):\(?([^)]+)\)?/)
            if (!matcher.find()) {
                return items
            }
            String field = matcher.group(1)
            String valueStr = matcher.group(2).trim()

            // Check if it's a list of values: (val1 val2 val3)
            if (valueStr.contains(" ")) {
                def values = valueStr.split("\\s+").toList()
                return items.findAll { item ->
                    String itemVal = item.spec?.get(field)?.toString()
                    return itemVal != null && values.contains(itemVal)
                }
            }

            return items.findAll { item ->
                String itemVal = item.spec?.get(field)?.toString()
                return itemVal == valueStr
            }
        }

        static int extractIdFromUri(String uri) {
            // Extract numeric ID from URI like /afa/v1/bs-volumes/5
            def matcher = (uri =~ /\/(\d+)(:[a-z-]+)?$/)
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1))
            }
            // Try extracting ID before :action like /afa/v1/bs-volumes/5:flatten
            matcher = (uri =~ /\/(\d+):/)
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1))
            }
            // Try extracting ID before /:action like /afa/v1/bs-volumes/5/:add-client-group-mappings
            matcher = (uri =~ /\/(\d+)\/:[a-z-]+/)
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1))
            }
            return -1
        }

        @Override
        void registerSimulators(EnvSpec espec) {
            def simulator = { arg1, arg2 ->
                espec.simulator(arg1, arg2)
            }

            // ========== SDDC Category ==========

            // 1. GET /sddc/v1/cluster - QueryClusterRequest
            simulator("/sddc/v1/cluster") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                return makeQueryResponse([
                    [
                        metadata: [id: 1, name: "xinfini-cluster", state: [state: "active"]],
                        spec: [id: 1, name: "xinfini-cluster", uuid: CLUSTER_UUID],
                        status: [id: 1]
                    ]
                ])
            }

            // 2. GET /sddc/v1/samples/query - QueryMetricRequest
            simulator("/sddc/v1/samples/query") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def metricParam = req.getParameter("metric")
                long value
                if (metricParam != null && metricParam.contains("data_kbytes")) {
                    // Used capacity
                    value = USED_CAPACITY_KB
                } else {
                    // Total capacity (actual_kbytes)
                    value = TOTAL_CAPACITY_KB
                }
                return [data: [result_type: "vector", result: [[value: value]]]]
            }

            // ========== AFA Category: Pool & Node ==========

            // 3. GET /afa/v1/pools - QueryPoolRequest
            simulator("/afa/v1/pools") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def now = java.time.OffsetDateTime.now().toString()
                return makeQueryResponse([
                    [
                        metadata: [id: POOL_ID, name: POOL_NAME, state: [state: "active"]],
                        spec: [id: POOL_ID, name: POOL_NAME, default_bs_policy_id: BS_POLICY_ID, created_at: now, updated_at: now],
                        status: [id: POOL_ID]
                    ]
                ])
            }

            // 4. GET /afa/v1/pools/{id} - GetPoolRequest
            simulator("/afa/v1/pools/\\d+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def now = java.time.OffsetDateTime.now().toString()
                return [
                    metadata: [id: POOL_ID, name: POOL_NAME, state: [state: "active"]],
                    spec: [id: POOL_ID, name: POOL_NAME, default_bs_policy_id: BS_POLICY_ID, created_at: now, updated_at: now],
                    status: [id: POOL_ID]
                ]
            }

            // 5. GET /afa/v1/bs-policies/{id} - GetBsPolicyRequest
            simulator("/afa/v1/bs-policies/\\d+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                return [
                    metadata: [id: BS_POLICY_ID, name: "default-policy", state: [state: "active"]],
                    spec: [id: BS_POLICY_ID, name: "default-policy", data_replica_type: "replica", data_replica_num: 3],
                    status: [id: BS_POLICY_ID]
                ]
            }

            // 6. GET /afa/v1/nodes - QueryNodeRequest
            simulator("/afa/v1/nodes") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                return makeQueryResponse([
                    [
                        metadata: [id: 1, name: "node-1", state: [state: "active"]],
                        spec: [id: 1, name: "node-1", ip: "127.0.0.1", port: 80, admin_ip: "127.0.0.1", role_afa_admin: true, role_afa_server: true, storage_public_ip: "127.0.0.1", storage_private_ip: "127.0.0.1"],
                        status: [id: 1, run_state: "Active"]
                    ]
                ])
            }

            // 7. GET /afa/v1/nodes/{id} - GetNodeRequest
            simulator("/afa/v1/nodes/\\d+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                return [
                    metadata: [id: 1, name: "node-1", state: [state: "active"]],
                    spec: [id: 1, name: "node-1", ip: "127.0.0.1", port: 80, admin_ip: "127.0.0.1", role_afa_admin: true, role_afa_server: true, storage_public_ip: "127.0.0.1", storage_private_ip: "127.0.0.1"],
                    status: [id: 1, run_state: "Active"]
                ]
            }

            // ========== AFA Category: Volume ==========

            // 8 & 9. /afa/v1/bs-volumes - POST create, GET query
            simulator("/afa/v1/bs-volumes") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                if (req.getMethod() == "POST") {
                    // 8. CreateVolumeRequest
                    def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                    def specData = body?.spec ?: body
                    int volId = volumeCounter.incrementAndGet()
                    String volName = specData?.name ?: "vol-${volId}"
                    int poolId = specData?.pool_id ?: POOL_ID
                    long sizeMb = specData?.size_mb ?: 1024

                    def volSpec = [
                        id: volId, name: volName, pool_id: poolId, size_mb: sizeMb,
                        bs_policy_id: BS_POLICY_ID, serial: "serial-${volId}".toString(),
                        loaded: false, flattened: true, max_total_iops: 0, max_total_bw_bps: 0,
                        creator: specData?.creator ?: "zstack", uuid: Platform.getUuid(), etag: Platform.getUuid()
                    ]
                    def volStatus = [
                        id: volId, size_mb: sizeMb, allocated_size_byte: 0,
                        loaded: false, spring_id: 0, protocol: "", mapping_num: 0
                    ]
                    def volItem = [spec: volSpec, status: volStatus]
                    volumes.put(volId, volItem)

                    return makeItemResponse(volItem)
                } else {
                    // 9. QueryVolumeRequest
                    def qParam = req.getParameter("q")
                    def allItems = volumes.values().collect { vol ->
                        [
                            metadata: [id: vol.spec.id, name: vol.spec.name, state: [state: "active"]],
                            spec: vol.spec,
                            status: vol.status
                        ]
                    }
                    def filtered = filterItems(allItems, qParam)
                    return makeQueryResponse(filtered)
                }
            }

            // 10, 11, 12. /afa/v1/bs-volumes/{id} - GET get, PATCH update, DELETE delete
            simulator("/afa/v1/bs-volumes/\\d+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                int volId = extractIdFromUri(req.getRequestURI())
                if (req.getMethod() == "DELETE") {
                    // 12. DeleteVolumeRequest
                    volumes.remove(volId)
                    return makeDeleteResponse()
                } else if (req.getMethod() == "PATCH") {
                    // 11. UpdateVolumeRequest
                    def vol = volumes.get(volId)
                    if (vol != null) {
                        def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                        def specData = body?.spec ?: body
                        if (specData?.size_mb) {
                            vol.spec.size_mb = specData.size_mb
                            vol.status.size_mb = specData.size_mb
                        }
                        if (specData?.max_total_iops != null) {
                            vol.spec.max_total_iops = specData.max_total_iops
                        }
                        if (specData?.max_total_bw_bps != null) {
                            vol.spec.max_total_bw_bps = specData.max_total_bw_bps
                        }
                    }
                    if (vol == null) {
                        return makeNotFoundResponse()
                    }
                    return makeItemResponse(vol)
                } else {
                    // 10. GetVolumeRequest
                    def vol = volumes.get(volId)
                    if (vol == null) {
                        return makeNotFoundResponse()
                    }
                    return makeItemResponse(vol)
                }
            }

            // 13. POST /afa/v1/bs-volumes/:clone - CloneVolumeRequest
            simulator("/afa/v1/bs-volumes/:clone") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                def specData = body?.spec ?: body
                int volId = volumeCounter.incrementAndGet()
                String volName = specData?.name ?: "clone-${volId}"
                int bsSnapId = specData?.bs_snap_id ?: 0

                // Get size from source snapshot if available
                long sizeMb = 1024
                def srcSnap = snapshots.get(bsSnapId)
                if (srcSnap != null) {
                    sizeMb = srcSnap.spec.size_mb ?: 1024
                }

                def volSpec = [
                    id: volId, name: volName, pool_id: POOL_ID, size_mb: sizeMb,
                    bs_policy_id: BS_POLICY_ID, bs_snap_id: bsSnapId,
                    serial: "serial-${volId}".toString(), loaded: false, flattened: false,
                    max_total_iops: 0, max_total_bw_bps: 0,
                    creator: specData?.creator ?: "zstack", uuid: Platform.getUuid(), etag: Platform.getUuid()
                ]
                def volStatus = [
                    id: volId, size_mb: sizeMb, allocated_size_byte: 0,
                    loaded: false, spring_id: 0, protocol: "", mapping_num: 0
                ]
                def volItem = [spec: volSpec, status: volStatus]
                volumes.put(volId, volItem)

                return makeItemResponse(volItem)
            }

            // 14. POST /afa/v1/bs-volumes/{id}/:flatten - FlattenVolumeRequest
            simulator("/afa/v1/bs-volumes/\\d+/:flatten") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                int volId = extractIdFromUri(req.getRequestURI())
                def vol = volumes.get(volId)
                if (vol != null) {
                    vol.spec.flattened = true
                }
                if (vol == null) {
                    return makeNotFoundResponse()
                }
                return makeItemResponse(vol)
            }

            // 15. POST /afa/v1/bs-volumes/{id}/:rollback - RollbackSnapshotRequest
            simulator("/afa/v1/bs-volumes/\\d+/:rollback") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                int volId = extractIdFromUri(req.getRequestURI())
                def vol = volumes.get(volId)
                if (vol == null) {
                    return makeNotFoundResponse()
                }
                return makeItemResponse(vol)
            }

            // ========== AFA Category: Snapshot ==========

            // 16 & 17. /afa/v1/bs-snaps - POST create, GET query
            simulator("/afa/v1/bs-snaps") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                if (req.getMethod() == "POST") {
                    // 16. CreateVolumeSnapshotRequest
                    def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                    def specData = body?.spec ?: body
                    int snapId = snapshotCounter.incrementAndGet()
                    String snapName = specData?.name ?: "snap-${snapId}"
                    int bsVolumeId = specData?.bs_volume_id ?: 0

                    long sizeMb = 1024
                    def srcVol = volumes.get(bsVolumeId)
                    if (srcVol != null) {
                        sizeMb = srcVol.spec.size_mb ?: 1024
                    }

                    def snapSpec = [
                        id: snapId, name: snapName, pool_id: POOL_ID,
                        bs_volume_id: bsVolumeId, bs_policy_id: BS_POLICY_ID,
                        size_mb: sizeMb, creator: specData?.creator ?: "zstack",
                        uuid: Platform.getUuid()
                    ]
                    def snapStatus = [id: snapId, size_mb: sizeMb]
                    def snapItem = [spec: snapSpec, status: snapStatus]
                    snapshots.put(snapId, snapItem)

                    return makeItemResponse(snapItem)
                } else {
                    // 17. QueryVolumeSnapshotRequest
                    def qParam = req.getParameter("q")
                    def allItems = snapshots.values().collect { snap ->
                        [
                            metadata: [id: snap.spec.id, name: snap.spec.name, state: [state: "active"]],
                            spec: snap.spec,
                            status: snap.status
                        ]
                    }
                    def filtered = filterItems(allItems, qParam)
                    return makeQueryResponse(filtered)
                }
            }

            // 18 & 19. /afa/v1/bs-snaps/{id} - GET get, DELETE delete
            simulator("/afa/v1/bs-snaps/\\d+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                int snapId = extractIdFromUri(req.getRequestURI())
                if (req.getMethod() == "DELETE") {
                    // 19. DeleteVolumeSnapshotRequest
                    snapshots.remove(snapId)
                    return makeDeleteResponse()
                } else {
                    // 18. GetVolumeSnapshotRequest
                    def snap = snapshots.get(snapId)
                    if (snap == null) {
                        return makeNotFoundResponse()
                    }
                    return makeItemResponse(snap)
                }
            }

            // ========== AFA Category: BDC / BdcBdev (Vhost) ==========

            // 20. GET /afa/v1/bdcs - QueryBdcRequest
            simulator("/afa/v1/bdcs") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                // In UNIT_TEST_ON mode, queryBdcByIp uses sortBy=spec.id:desc instead of q=spec.ip:xxx
                // Return all BDCs sorted by id desc
                def bdcItems = [
                    [
                        metadata: [id: BDC_ID, name: "bdc-1", state: [state: "active"]],
                        spec: [id: BDC_ID, name: "bdc-1", ip: "127.0.0.1", port: 9500],
                        status: [id: BDC_ID, run_state: "Active", installed: true, hostname: "localhost", version: "1.0.0"]
                    ]
                ]
                return makeQueryResponse(bdcItems)
            }

            // 21. GET /afa/v1/bdcs/{id} - GetBdcRequest
            simulator("/afa/v1/bdcs/\\d+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                return [
                    metadata: [id: BDC_ID, name: "bdc-1", state: [state: "active"]],
                    spec: [id: BDC_ID, name: "bdc-1", ip: "127.0.0.1", port: 9500],
                    status: [id: BDC_ID, run_state: "Active", installed: true, hostname: "localhost", version: "1.0.0"]
                ]
            }

            // 22 & 23. /afa/v1/bdc-bdevs - POST create, GET query
            simulator("/afa/v1/bdc-bdevs") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                if (req.getMethod() == "POST") {
                    // 22. CreateBdcBdevRequest
                    def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                    def specData = body?.spec ?: body
                    int bdevId = bdcBdevCounter.incrementAndGet()
                    int bdcId = specData?.bdc_id ?: BDC_ID
                    int bsVolumeId = specData?.bs_volume_id ?: 0
                    String bdevName = specData?.name ?: "volume-${bdevId}"
                    int queueNum = specData?.queue_num ?: 1
                    String socketPath = "/var/run/bdc-${CLUSTER_UUID}/${bdevName}"

                    def bdevSpec = [
                        id: bdevId, name: bdevName, bdc_id: bdcId, node_ip: "127.0.0.1",
                        bs_volume_id: bsVolumeId, socket_path: socketPath, queue_num: queueNum,
                        bs_volume_name: bdevName, bs_volume_uuid: Platform.getUuid(), numa_node_ids: []
                    ]
                    def bdevStatus = [id: bdevId]
                    def bdevItem = [spec: bdevSpec, status: bdevStatus]
                    bdcBdevs.put(bdevId, bdevItem)

                    return makeItemResponse(bdevItem)
                } else {
                    // 23. QueryBdcBdevRequest
                    def qParam = req.getParameter("q")
                    def allItems = bdcBdevs.values().collect { bdev ->
                        [
                            metadata: [id: bdev.spec.id, name: bdev.spec.name, state: [state: "active"]],
                            spec: bdev.spec,
                            status: bdev.status
                        ]
                    }
                    def filtered = filterItems(allItems, qParam)
                    return makeQueryResponse(filtered)
                }
            }

            // 24 & 25. /afa/v1/bdc-bdevs/{id} - GET get, DELETE delete
            simulator("/afa/v1/bdc-bdevs/\\d+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                int bdevId = extractIdFromUri(req.getRequestURI())
                if (req.getMethod() == "DELETE") {
                    // 25. DeleteBdcBdevRequest
                    bdcBdevs.remove(bdevId)
                    return makeDeleteResponse()
                } else {
                    // 24. GetBdcBdevRequest
                    def bdev = bdcBdevs.get(bdevId)
                    if (bdev == null) {
                        return makeNotFoundResponse()
                    }
                    return makeItemResponse(bdev)
                }
            }

            // ========== AFA Category: iSCSI ==========

            // 26. GET /afa/v1/iscsi-gateways - QueryIscsiGatewayRequest
            simulator("/afa/v1/iscsi-gateways") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                def gwItems = [
                    [
                        metadata: [id: ISCSI_GATEWAY_ID, name: "iscsi-gw-1", state: [state: "active"]],
                        spec: [id: ISCSI_GATEWAY_ID, name: "iscsi-gw-1", node_id: 1, ips: ["127.0.0.1"], port: 3260],
                        status: [id: ISCSI_GATEWAY_ID, node_state: "ACTIVE"]
                    ]
                ]
                return makeQueryResponse(gwItems)
            }

            // 27 & 29. /afa/v1/iscsi-clients - GET query, POST create
            simulator("/afa/v1/iscsi-clients") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                if (req.getMethod() == "POST") {
                    // 29. CreateIscsiClientRequest
                    def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                    def specData = body?.spec ?: body
                    int clientId = iscsiClientCounter.incrementAndGet()
                    String clientName = specData?.name ?: "iscsi-client-${clientId}"
                    String code = specData?.code ?: "iqn.2000-01.com.example:client-${clientId}"
                    Integer clientGroupId = specData?.iscsi_client_group_id

                    def clientSpec = [
                        id: clientId, name: clientName, code: code,
                        iscsi_client_group_id: clientGroupId
                    ]
                    def clientStatus = [id: clientId, target_iqns: []]
                    def clientItem = [spec: clientSpec, status: clientStatus]
                    iscsiClients.put(clientId, clientItem)

                    return makeItemResponse(clientItem)
                } else {
                    // 27. QueryIscsiClientRequest
                    def qParam = req.getParameter("q")
                    def allItems = iscsiClients.values().collect { client ->
                        [
                            metadata: [id: client.spec.id, name: client.spec.name, state: [state: "active"]],
                            spec: client.spec,
                            status: client.status
                        ]
                    }
                    def filtered = filterItems(allItems, qParam)
                    return makeQueryResponse(filtered)
                }
            }

            // 28 & 30. /afa/v1/iscsi-clients/{id} - GET get, DELETE delete
            simulator("/afa/v1/iscsi-clients/\\d+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                int clientId = extractIdFromUri(req.getRequestURI())
                if (req.getMethod() == "DELETE") {
                    // 30. DeleteIscsiClientRequest
                    iscsiClients.remove(clientId)
                    return makeDeleteResponse()
                } else {
                    // 28. GetIscsiClientRequest
                    def client = iscsiClients.get(clientId)
                    if (client == null) {
                        return makeNotFoundResponse()
                    }
                    return makeItemResponse(client)
                }
            }

            // 31 & 33. /afa/v1/iscsi-client-groups - GET query, POST create
            simulator("/afa/v1/iscsi-client-groups") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                if (req.getMethod() == "POST") {
                    // 33. CreateIscsiClientGroupRequest
                    def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                    def specData = body?.spec ?: body
                    int groupId = iscsiClientGroupCounter.incrementAndGet()
                    String groupName = specData?.name ?: "iscsi-client-group-${groupId}"

                    // Also create iSCSI clients for each client code (IQN)
                    def clientCodes = specData?.iscsi_client_codes ?: []
                    String targetIqn = "iqn.2022-07.com.xinfini:target-${groupId}".toString()
                    clientCodes.each { code ->
                        int cId = iscsiClientCounter.incrementAndGet()
                        def cSpec = [id: cId, name: "iscsi-client-${cId}".toString(), code: code, iscsi_client_group_id: groupId]
                        def cStatus = [id: cId, target_iqns: [targetIqn]]
                        def cItem = [spec: cSpec, status: cStatus]
                        iscsiClients.put(cId, cItem)
                    }

                    def groupSpec = [id: groupId, name: groupName]
                    def groupStatus = [id: groupId]
                    def groupItem = [spec: groupSpec, status: groupStatus]
                    iscsiClientGroups.put(groupId, groupItem)

                    return makeItemResponse(groupItem)
                } else {
                    // 31. QueryIscsiClientGroupRequest
                    def qParam = req.getParameter("q")
                    def allItems = iscsiClientGroups.values().collect { group ->
                        [
                            metadata: [id: group.spec.id, name: group.spec.name, state: [state: "active"]],
                            spec: group.spec,
                            status: group.status
                        ]
                    }
                    def filtered = filterItems(allItems, qParam)
                    return makeQueryResponse(filtered)
                }
            }

            // 32. GET /afa/v1/iscsi-client-groups/{id} - GetIscsiClientGroupRequest
            simulator("/afa/v1/iscsi-client-groups/\\d+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                int groupId = extractIdFromUri(req.getRequestURI())
                def group = iscsiClientGroups.get(groupId)
                if (group == null) {
                    return makeNotFoundResponse()
                }
                return makeItemResponse(group)
            }

            // 34. GET /afa/v1/iscsi-gateway-client-group-mappings - QueryIscsiGatewayClientGroupMappingRequest
            simulator("/afa/v1/iscsi-gateway-client-group-mappings") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                // Return mappings based on existing client groups
                def mappingItems = iscsiClientGroups.values().collect { group ->
                    [
                        metadata: [id: group.spec.id, name: "gw-group-mapping-${group.spec.id}".toString(), state: [state: "active"]],
                        spec: [id: group.spec.id, iscsi_gateway_id: ISCSI_GATEWAY_ID, iscsi_client_group_id: group.spec.id],
                        status: [id: group.spec.id]
                    ]
                }
                return makeQueryResponse(mappingItems)
            }

            // 35 & 37. /afa/v1/bs-volume-client-group-mappings - GET query (35), also handle DELETE for {id} (37)
            simulator("/afa/v1/bs-volume-client-group-mappings") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                // 35. QueryVolumeClientGroupMappingRequest
                def qParam = req.getParameter("q")
                def allItems = volumeClientGroupMappings.values().collect { mapping ->
                    [
                        metadata: [id: mapping.spec.id, name: "vol-group-mapping-${mapping.spec.id}".toString(), state: [state: "active"]],
                        spec: mapping.spec,
                        status: mapping.status
                    ]
                }
                def filtered = filterItems(allItems, qParam)
                return makeQueryResponse(filtered)
            }

            // 36 & 37. /afa/v1/bs-volume-client-group-mappings/{id} - GET get (36), DELETE delete (37)
            simulator("/afa/v1/bs-volume-client-group-mappings/\\d+") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                int mappingId = extractIdFromUri(req.getRequestURI())
                if (req.getMethod() == "DELETE") {
                    // 37. DeleteVolumeClientGroupMappingRequest
                    volumeClientGroupMappings.remove(mappingId)
                    return makeDeleteResponse()
                } else {
                    // 36. GetVolumeClientGroupMappingRequest
                    def mapping = volumeClientGroupMappings.get(mappingId)
                    if (mapping == null) {
                        return makeNotFoundResponse()
                    }
                    return makeItemResponse(mapping)
                }
            }

            // 38. POST /afa/v1/bs-volumes/{id}/:add-client-group-mappings - AddVolumeClientGroupMappingRequest
            simulator("/afa/v1/bs-volumes/\\d+/:add-client-group-mappings") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                int volId = extractIdFromUri(req.getRequestURI())
                def body = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
                def specData = body?.spec ?: body
                def groupIds = specData?.iscsi_client_group_ids ?: []

                def createdMappings = []
                for (gid in groupIds) {
                    int mappingId = volumeClientGroupMappingCounter.incrementAndGet()
                    int groupId = gid instanceof Integer ? gid : Integer.parseInt(gid.toString())
                    def mappingSpec = [
                        id: mappingId, bs_volume_id: volId, iscsi_client_group_id: groupId, lun_id: mappingId
                    ]
                    def mappingStatus = [id: mappingId]
                    def mappingItem = [spec: mappingSpec, status: mappingStatus]
                    volumeClientGroupMappings.put(mappingId, mappingItem)
                    createdMappings.add([
                        metadata: [id: mappingId, name: "vol-group-mapping-${mappingId}".toString(), state: [state: "active"]],
                        spec: mappingSpec,
                        status: mappingStatus
                    ])
                }

                return makeQueryResponse(createdMappings)
            }

            // 39. GET /afa/v1/bs-volume-client-mappings - QueryVolumeClientMappingRequest
            simulator("/afa/v1/bs-volume-client-mappings") { HttpServletRequest req, HttpEntity<String> e, EnvSpec spec ->
                // Build volume-client mappings from volume-client-group mappings and iscsi clients
                def mappingItems = []
                int mappingIdSeq = 0
                volumeClientGroupMappings.values().each { vcgMapping ->
                    int volId = vcgMapping.spec.bs_volume_id
                    int groupId = vcgMapping.spec.iscsi_client_group_id
                    // Find clients in this group
                    iscsiClients.values().each { client ->
                        if (client.spec.iscsi_client_group_id == groupId) {
                            mappingIdSeq++
                            mappingItems.add([
                                metadata: [id: mappingIdSeq, name: "vol-client-mapping-${mappingIdSeq}".toString(), state: [state: "active"]],
                                spec: [
                                    id: mappingIdSeq, bs_volume_id: volId,
                                    iscsi_client_id: client.spec.id,
                                    iscsi_client_group_id: groupId,
                                    protocol: "iSCSI", lun_id: vcgMapping.spec.lun_id
                                ],
                                status: [id: mappingIdSeq]
                            ])
                        }
                    }
                }

                def qParam = req.getParameter("q")
                def filtered = filterItems(mappingItems, qParam)
                return makeQueryResponse(filtered)
            }
        }
    }

    @Override
    SpecID create(String uuid, String sessionId) {
        inventory = addExternalPrimaryStorage {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.url = url
            delegate.sessionId = sessionId
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.identity = identity
            delegate.config = config
            delegate.defaultOutputProtocol = defaultOutputProtocol
        } as PrimaryStorageInventory

        postCreate {
            inventory = queryPrimaryStorage {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }
}
