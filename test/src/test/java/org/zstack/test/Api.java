package org.zstack.test;

import com.sun.org.apache.xerces.internal.xs.StringList;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.alert.APIDeleteAlarmEvent;
import org.zstack.alert.APIDeleteAlarmMsg;
import org.zstack.appliancevm.APIListApplianceVmMsg;
import org.zstack.appliancevm.APIListApplianceVmReply;
import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.billing.*;
import org.zstack.cassandra.APIQueryCassandraMsg;
import org.zstack.core.MessageCommandRecorder;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusEventListener;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.*;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.debug.APIDebugSignalEvent;
import org.zstack.core.debug.APIDebugSignalMsg;
import org.zstack.core.debug.DebugSignal;
import org.zstack.core.scheduler.*;
import org.zstack.header.vm.APIChangeVmPasswordMsg;
import org.zstack.ha.APIDeleteVmInstanceHaLevelMsg;
import org.zstack.ha.APISetVmInstanceHaLevelEvent;
import org.zstack.ha.APISetVmInstanceHaLevelMsg;
import org.zstack.ha.VmHaLevel;
import org.zstack.header.allocator.APIGetCpuMemoryCapacityMsg;
import org.zstack.header.allocator.APIGetCpuMemoryCapacityReply;
import org.zstack.header.allocator.APIGetHostAllocatorStrategiesMsg;
import org.zstack.header.allocator.APIGetHostAllocatorStrategiesReply;
import org.zstack.header.apimediator.APIIsReadyToGoMsg;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.cluster.*;
import org.zstack.header.configuration.*;
import org.zstack.header.console.APIRequestConsoleAccessEvent;
import org.zstack.header.console.APIRequestConsoleAccessMsg;
import org.zstack.header.console.ConsoleInventory;
import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.identity.*;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.header.image.*;
import org.zstack.header.managementnode.*;
import org.zstack.header.message.*;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.header.query.*;
import org.zstack.header.search.*;
import org.zstack.header.simulator.APIAddSimulatorHostMsg;
import org.zstack.header.simulator.ChangeVmStateOnSimulatorHostMsg;
import org.zstack.header.simulator.RemoveVmOnSimulatorMsg;
import org.zstack.header.simulator.SimulatorDetails;
import org.zstack.header.simulator.storage.backup.APIAddSimulatorBackupStorageMsg;
import org.zstack.header.simulator.storage.backup.SimulatorBackupStorageConstant;
import org.zstack.header.simulator.storage.backup.SimulatorBackupStorageDetails;
import org.zstack.header.simulator.storage.primary.APIAddSimulatorPrimaryStorageMsg;
import org.zstack.header.simulator.storage.primary.SimulatorPrimaryStorageConstant;
import org.zstack.header.simulator.storage.primary.SimulatorPrimaryStorageDetails;
import org.zstack.ipsec.*;
import org.zstack.storage.ceph.backup.*;
import org.zstack.storage.ceph.primary.*;
import org.zstack.storage.primary.local.*;
import org.zstack.network.service.eip.*;
import org.zstack.network.service.lb.*;
import org.zstack.network.service.portforwarding.*;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.tag.*;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.header.volume.APIGetVolumeFormatReply.VolumeFormatReplyStruct;
import org.zstack.header.zone.*;
import org.zstack.kvm.APIAddKVMHostMsg;
import org.zstack.kvm.APIUpdateKVMHostMsg;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.license.*;
import org.zstack.logging.APIDeleteLogEvent;
import org.zstack.logging.APIDeleteLogMsg;
import org.zstack.network.securitygroup.*;
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO;
import org.zstack.portal.managementnode.ManagementNodeManager;
import org.zstack.storage.backup.sftp.APIReconnectSftpBackupStorageEvent;
import org.zstack.storage.backup.sftp.APIReconnectSftpBackupStorageMsg;
import org.zstack.storage.backup.sftp.APIUpdateSftpBackupStorageMsg;
import org.zstack.storage.backup.sftp.SftpBackupStorageInventory;
import org.zstack.header.vm.APIChangeVmPasswordEvent;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.zstack.utils.CollectionDSL.list;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class Api implements CloudBusEventListener {
    private static final CLogger logger = Utils.getLogger(Api.class);
    private static ComponentLoader loader;
    private ManagementNodeManager mgr;
    private SessionInventory adminSession;
    private String rootPassword;
    private int timeout = 15;

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    static {
        loader = Platform.getComponentLoader();
    }

    private void start() {
        mgr.startNode();
    }

    public void prepare() {
        try {
            adminSession = this.loginAsAdmin();
            rootPassword = null;
        } catch (ApiSenderException e1) {
            throw new CloudRuntimeException(e1);
        }
    }

    public void startServer() {
        mgr = loader.getComponent(ManagementNodeManager.class);
        start();
        final CountDownLatch count = new CountDownLatch(1);

        APIIsReadyToGoMsg msg = new APIIsReadyToGoMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setManagementNodeId(Platform.getManagementServerId());
        msg.setTimeout(TimeUnit.MINUTES.toMillis(2));
        bus.call(msg);
        logger.info("Server is running ...");

        prepare();
    }

    public void stopServer() {
        ManagementNodeExitMsg msg = new ManagementNodeExitMsg();
        msg.setServiceId(bus.makeLocalServiceId(ManagementNodeConstant.SERVICE_ID));
        bus.send(msg);
    }

    public void stopServerTilManagementNodeDisappear(final String mgmtUuid, long timeout) {
        stopServer();

        class Result {
            boolean success;
        }

        final Result res = new Result();
        TimeUtils.loopExecuteUntilTimeoutIgnoreException(timeout, 1, TimeUnit.SECONDS, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                SimpleQuery<ManagementNodeVO> q = dbf.createQuery(ManagementNodeVO.class);
                q.add(ManagementNodeVO_.uuid, Op.EQ, mgmtUuid);
                ManagementNodeVO vo = q.find();
                res.success = vo == null;
                return res.success;
            }
        });

        if (!res.success) {
            throw new RuntimeException(String.format("failed to stop management server[uuid:%s] after %s secs", mgmtUuid, timeout));
        }
    }

    public ApiSender getApiSender() {
        return new ApiSender();
    }

    public List<HostInventory> getMigrationTargetHost(String vmUuid) throws ApiSenderException {
        APIGetVmMigrationCandidateHostsMsg msg = new APIGetVmMigrationCandidateHostsMsg();
        msg.setVmInstanceUuid(vmUuid);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetVmMigrationCandidateHostsReply reply = sender.call(msg, APIGetVmMigrationCandidateHostsReply.class);
        return reply.getInventories();
    }

    public VolumeSnapshotInventory createSnapshot(String volUuid) throws ApiSenderException {
        return createSnapshot(volUuid, null);
    }

    public VolumeSnapshotInventory createSnapshot(String volUuid, SessionInventory session) throws ApiSenderException {
        MessageCommandRecorder.reset();
        MessageCommandRecorder.start(APICreateVolumeSnapshotMsg.class);

        APICreateVolumeSnapshotMsg msg = new APICreateVolumeSnapshotMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setName("Snapshot-" + volUuid);
        msg.setDescription("Test snapshot");
        msg.setVolumeUuid(volUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateVolumeSnapshotEvent evt = sender.send(msg, APICreateVolumeSnapshotEvent.class);

        logger.debug(MessageCommandRecorder.endAndToString());

        return evt.getInventory();
    }

    public VolumeInventory createDataVolumeFromSnapshot(String snapshotUuid) throws ApiSenderException {
        return createDataVolumeFromSnapshot(snapshotUuid, (SessionInventory) null);
    }

    public VolumeInventory createDataVolumeFromSnapshot(String snapshotUuid, SessionInventory session) throws ApiSenderException {
        return createDataVolumeFromSnapshot(snapshotUuid, null, session);
    }

    public VolumeInventory createDataVolumeFromSnapshot(String snapshotUuid, String priUuid) throws ApiSenderException {
        return createDataVolumeFromSnapshot(snapshotUuid, priUuid, null);
    }

    public VolumeInventory createDataVolumeFromSnapshot(String snapshotUuid, String priUuid, SessionInventory session) throws ApiSenderException {
        APICreateDataVolumeFromVolumeSnapshotMsg msg = new APICreateDataVolumeFromVolumeSnapshotMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setPrimaryStorageUuid(priUuid);
        msg.setName("volume-form-snapshot" + snapshotUuid);
        msg.setVolumeSnapshotUuid(snapshotUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateDataVolumeFromVolumeSnapshotEvent evt = sender.send(msg, APICreateDataVolumeFromVolumeSnapshotEvent.class);
        return evt.getInventory();
    }

    public ImageInventory createTemplateFromSnapshot(String snapshotUuid, List<String> backupStorageUuids) throws ApiSenderException {
        return createTemplateFromSnapshot(snapshotUuid, backupStorageUuids, null);
    }

    public ImageInventory createTemplateFromSnapshot(String snapshotUuid, String bsUuid, SessionInventory session) throws ApiSenderException {
        return createTemplateFromSnapshot(snapshotUuid, asList(bsUuid), session);
    }

    public ImageInventory createTemplateFromSnapshot(String snapshotUuid, List<String> backupStorageUuids, SessionInventory session) throws ApiSenderException {
        MessageCommandRecorder.reset();
        MessageCommandRecorder.start(APICreateRootVolumeTemplateFromVolumeSnapshotMsg.class);

        APICreateRootVolumeTemplateFromVolumeSnapshotMsg msg = new APICreateRootVolumeTemplateFromVolumeSnapshotMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setBackupStorageUuids(backupStorageUuids);
        msg.setSnapshotUuid(snapshotUuid);
        msg.setName(String.format("image-from-snapshot-%s", snapshotUuid));
        msg.setGuestOsType("CentOS");
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateRootVolumeTemplateFromVolumeSnapshotEvent evt = sender.send(msg, APICreateRootVolumeTemplateFromVolumeSnapshotEvent.class);

        logger.debug(MessageCommandRecorder.endAndToString());
        return evt.getInventory();
    }

    public ImageInventory createTemplateFromSnapshot(String snapshotUuid, String backupStorageUuid) throws ApiSenderException {
        return createTemplateFromSnapshot(snapshotUuid, Arrays.asList(backupStorageUuid));
    }

    public void deleteSnapshot(String snapshotUuid) throws ApiSenderException {
        deleteSnapshot(snapshotUuid, null);
    }

    public void deleteSnapshot(String snapshotUuid, SessionInventory session) throws ApiSenderException {
        MessageCommandRecorder.reset();
        MessageCommandRecorder.start(APIDeleteVolumeSnapshotMsg.class);

        APIDeleteVolumeSnapshotMsg msg = new APIDeleteVolumeSnapshotMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setUuid(snapshotUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteVolumeSnapshotEvent.class);
        logger.debug(MessageCommandRecorder.endAndToString());
    }

    public void revertVolumeToSnapshot(String snapshotUuid) throws ApiSenderException {
        revertVolumeToSnapshot(snapshotUuid, null);
    }

    public void revertVolumeToSnapshot(String snapshotUuid, SessionInventory session) throws ApiSenderException {
        APIRevertVolumeFromSnapshotMsg msg = new APIRevertVolumeFromSnapshotMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setUuid(snapshotUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIRevertVolumeFromSnapshotEvent.class);
    }

    public List<ZoneInventory> createZones(int num) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        List<ZoneInventory> ret = new ArrayList<ZoneInventory>();
        for (int i = 0; i < num; i++) {
            APICreateZoneMsg msg = new APICreateZoneMsg();
            msg.setSession(adminSession);
            msg.setName("Zone-" + i);
            msg.setDescription("Test Zone");
            APICreateZoneEvent e = sender.send(msg, APICreateZoneEvent.class);
            ret.add(e.getInventory());
        }
        return ret;
    }

    public List<L2VlanNetworkInventory> listL2VlanNetworks(List<String> uuids) throws ApiSenderException {
        APIListL2VlanNetworkMsg msg = new APIListL2VlanNetworkMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListL2VlanNetworkReply reply = sender.call(msg, APIListL2VlanNetworkReply.class);
        return reply.getInventories();
    }

    public List<ZoneInventory> listZones(List<String> uuids) throws ApiSenderException {
        APIListZonesMsg msg = new APIListZonesMsg(uuids);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListZonesReply reply = sender.call(msg, APIListZonesReply.class);
        return reply.getInventories();
    }

    public void deleteZone(String uuid) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteZoneMsg msg = new APIDeleteZoneMsg(uuid);
        msg.setSession(adminSession);
        sender.send(msg, APIDeleteZoneEvent.class);
    }

    public ZoneInventory changeZoneState(String uuid, ZoneStateEvent evt) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeZoneStateMsg msg = new APIChangeZoneStateMsg(uuid, evt.toString());
        msg.setSession(adminSession);
        APIChangeZoneStateEvent ret = sender.send(msg, APIChangeZoneStateEvent.class);
        return ret.getInventory();
    }

    public void deleteAllZones() throws ApiSenderException {
        List<ZoneInventory> allZones = listZones(null);
        for (ZoneInventory zone : allZones) {
            deleteZone(zone.getUuid());
        }
    }

    public List<ClusterInventory> createClusters(int num, String zoneUuid) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        List<ClusterInventory> ret = new ArrayList<ClusterInventory>();
        for (int i = 0; i < num; i++) {
            APICreateClusterMsg msg = new APICreateClusterMsg();
            msg.setClusterName("Cluster-" + i);
            msg.setDescription("Test Cluster");
            msg.setHypervisorType("Simulator");
            msg.setZoneUuid(zoneUuid);
            msg.setSession(adminSession);
            APICreateClusterEvent evt = sender.send(msg, APICreateClusterEvent.class);
            ret.add(evt.getInventory());
        }
        return ret;
    }

    public List<ClusterInventory> listClusters(List<String> uuids) throws ApiSenderException {
        return listClusters(0, -1, uuids);
    }

    public List<ClusterInventory> listClusters(int offset, int length, List<String> uuids) throws ApiSenderException {
        APIListClusterMsg msg = new APIListClusterMsg(uuids);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setOffset(offset);
        msg.setLength(length);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListClusterReply reply = sender.call(msg, APIListClusterReply.class);
        return reply.getInventories();
    }

    public ClusterInventory changeClusterState(String uuid, ClusterStateEvent evt) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeClusterStateMsg msg = new APIChangeClusterStateMsg(uuid, evt.toString());
        msg.setSession(adminSession);
        APIChangeClusterStateEvent e = sender.send(msg, APIChangeClusterStateEvent.class);
        return e.getInventory();
    }

    public void asyncChangeClusterState(String uuid, ClusterStateEvent evt) throws InterruptedException {
        APIChangeClusterStateMsg msg = new APIChangeClusterStateMsg(uuid, evt.toString());
        msg.setSession(adminSession);
        bus.send(msg);
    }

    public void asyncDeleteCluster(String uuid) throws InterruptedException {
        APIDeleteClusterMsg msg = new APIDeleteClusterMsg(uuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        bus.send(msg);
    }

    public void deleteCluster(String uuid) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteClusterMsg msg = new APIDeleteClusterMsg(uuid);
        msg.setSession(adminSession);
        sender.send(msg, APIDeleteClusterEvent.class);
    }

    public List<HostInventory> createHost(int num, String clusterUuid) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        List<HostInventory> rets = new ArrayList<HostInventory>();
        for (int i = 0; i < num; i++) {
            APIAddSimulatorHostMsg msg = new APIAddSimulatorHostMsg();
            msg.setClusterUuid(clusterUuid);
            msg.setDescription("Test Host");
            msg.setManagementIp("10.0.0." + i);
            msg.setMemoryCapacity(SizeUnit.GIGABYTE.toByte(8));
            msg.setCpuCapacity(2400 * 4);
            msg.setName("Host-" + i);
            msg.setSession(adminSession);
            APIAddHostEvent e = sender.send(msg, APIAddHostEvent.class);
            rets.add(e.getInventory());
        }

        return rets;
    }

    public HostInventory changeHostState(String uuid, HostStateEvent evt) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeHostStateMsg msg = new APIChangeHostStateMsg(uuid, evt.toString());
        msg.setSession(adminSession);
        sender.setTimeout(60); // for maintenance mode which takes longer time
        APIChangeHostStateEvent e = sender.send(msg, APIChangeHostStateEvent.class);
        return e.getInventory();
    }

    public void asyncChangeHostState(String uuid, HostStateEvent evt) {
        APIChangeHostStateMsg msg = new APIChangeHostStateMsg(uuid, evt.toString());
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        bus.send(msg);
    }

    public List<HostInventory> listHosts(List<String> uuids) throws ApiSenderException {
        return listHosts(0, -1, uuids);
    }

    public List<HostInventory> listHosts(int offset, int length, List<String> uuids) throws ApiSenderException {
        APIListHostMsg msg = new APIListHostMsg(uuids);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setOffset(offset);
        msg.setSession(adminSession);
        msg.setLength(length);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListHostReply reply = sender.call(msg, APIListHostReply.class);
        return reply.getInventories();
    }

    public void deleteHost(String uuid) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteHostMsg msg = new APIDeleteHostMsg(uuid);
        msg.setSession(adminSession);
        sender.send(msg, APIDeleteHostEvent.class);
    }

    public HostInventory maintainHost(String uuid) throws ApiSenderException {
        return changeHostState(uuid, HostStateEvent.maintain);
    }

    public List<HostInventory> createSimulator(int num, String clusterUuid, SimulatorDetails details) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        List<HostInventory> rets = new ArrayList<HostInventory>();
        for (int i = 0; i < num; i++) {
            APIAddSimulatorHostMsg msg = new APIAddSimulatorHostMsg();
            msg.setClusterUuid(clusterUuid);
            msg.setDescription("Test Host");
            msg.setManagementIp("10.0.0." + i);
            msg.setSession(adminSession);
            msg.setName("Host-" + i);
            details.fillAPIAddSimulatorHostMsg(msg);
            APIAddHostEvent e = sender.send(msg, APIAddHostEvent.class);
            rets.add(e.getInventory());
        }

        return rets;
    }

    public List<PrimaryStorageInventory> createSimulatoPrimaryStorage(int num, SimulatorPrimaryStorageDetails details) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        List<PrimaryStorageInventory> rets = new ArrayList<PrimaryStorageInventory>();
        for (int i = 0; i < num; i++) {
            APIAddSimulatorPrimaryStorageMsg msg = new APIAddSimulatorPrimaryStorageMsg();
            msg.setUrl(details.getUrl() + "-" + i);
            msg.setType("SimulatorPrimaryStorage");
            msg.setName("SimulatorPrimaryStorage-" + i);
            msg.setDescription("Test Primary Storage");
            msg.setSession(adminSession);
            msg.setTotalCapacity(details.getTotalCapacity());
            msg.setAvailableCapacity(details.getAvailableCapacity());
            msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
            msg.setZoneUuid(details.getZoneUuid());
            APIAddPrimaryStorageEvent e = sender.send(msg, APIAddPrimaryStorageEvent.class);
            rets.add(e.getInventory());
        }

        return rets;
    }

    public List<PrimaryStorageInventory> listPrimaryStorage(List<String> uuids) throws ApiSenderException {
        return listPrimaryStorage(0, -1, uuids);
    }

    public List<PrimaryStorageInventory> listPrimaryStorage(int offset, int length, List<String> uuids) throws ApiSenderException {
        APIListPrimaryStorageMsg msg = new APIListPrimaryStorageMsg(uuids);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        msg.setOffset(offset);
        msg.setLength(length);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListPrimaryStorageReply reply = sender.call(msg, APIListPrimaryStorageReply.class);
        return reply.getInventories();
    }

    public void asyncChangePrimaryStorageState(String uuid, PrimaryStorageStateEvent evt) {
        APIChangePrimaryStorageStateMsg msg = new APIChangePrimaryStorageStateMsg(uuid, evt.toString());
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        bus.send(msg);
    }

    public PrimaryStorageInventory changePrimaryStorageState(String uuid, PrimaryStorageStateEvent event) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangePrimaryStorageStateMsg msg = new APIChangePrimaryStorageStateMsg(uuid, event.toString());
        msg.setSession(adminSession);
        APIChangePrimaryStorageStateEvent e = sender.send(msg, APIChangePrimaryStorageStateEvent.class);
        return e.getInventory();
    }

    public void asyncDeletePrimaryStorage(String uuid) {
        APIDeletePrimaryStorageMsg msg = new APIDeletePrimaryStorageMsg(uuid);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        bus.send(msg);
    }

    public void deletePrimaryStorage(String uuid) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeletePrimaryStorageMsg msg = new APIDeletePrimaryStorageMsg(uuid);
        msg.setSession(adminSession);
        sender.send(msg, APIDeletePrimaryStorageEvent.class);
    }

    public void asyncAttachPrimaryStorage(String clusterUuid, String uuid) throws InterruptedException {
        APIAttachPrimaryStorageToClusterMsg msg = new APIAttachPrimaryStorageToClusterMsg(clusterUuid, uuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        bus.send(msg);
    }

    public PrimaryStorageInventory attachPrimaryStorage(String clusterUuid, String uuid) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(10000);
        APIAttachPrimaryStorageToClusterMsg msg = new APIAttachPrimaryStorageToClusterMsg(clusterUuid, uuid);
        msg.setSession(adminSession);
        APIAttachPrimaryStorageToClusterEvent e = sender.send(msg, APIAttachPrimaryStorageToClusterEvent.class);
        return e.getInventory();
    }

    public void asyncDetachPrimaryStorage(String uuid) throws InterruptedException {
        APIDetachPrimaryStorageFromClusterMsg msg = new APIDetachPrimaryStorageFromClusterMsg(uuid);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        bus.send(msg);
    }

    public PrimaryStorageInventory detachPrimaryStorage(String uuid, String clusterUuid) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDetachPrimaryStorageFromClusterMsg msg = new APIDetachPrimaryStorageFromClusterMsg(uuid);
        msg.setSession(adminSession);
        msg.setClusterUuid(clusterUuid);
        APIDetachPrimaryStorageFromClusterEvent e = sender.send(msg, APIDetachPrimaryStorageFromClusterEvent.class);
        return e.getInventory();
    }

    public List<BackupStorageInventory> createSimulatorBackupStorage(int num, SimulatorBackupStorageDetails details) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        List<BackupStorageInventory> rets = new ArrayList<BackupStorageInventory>();
        for (int i = 0; i < num; i++) {
            APIAddSimulatorBackupStorageMsg msg = new APIAddSimulatorBackupStorageMsg();
            msg.setSession(adminSession);
            msg.setName("SimulatoryBackupStorage-" + i);
            msg.setUrl(details.getUrl() + "-" + i);
            msg.setType(SimulatorBackupStorageConstant.SIMULATOR_BACKUP_STORAGE_TYPE);
            msg.setDescription("Test Backup Storage");
            msg.setTotalCapacity(details.getTotalCapacity());
            msg.setAvailableCapacity(details.getTotalCapacity() - details.getUsedCapacity());
            APIAddBackupStorageEvent e = sender.send(msg, APIAddBackupStorageEvent.class);
            rets.add(e.getInventory());
        }

        return rets;
    }

    public List<BackupStorageInventory> listBackupStorage(List<String> uuids) throws ApiSenderException {
        return listBackupStorage(0, -1, uuids);
    }

    public List<BackupStorageInventory> listBackupStorage(int offset, int length, List<String> uuids) throws ApiSenderException {
        APIListBackupStorageMsg msg = new APIListBackupStorageMsg(uuids);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setOffset(offset);
        msg.setLength(length);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListBackupStorageReply reply = sender.call(msg, APIListBackupStorageReply.class);
        return reply.getInventories();
    }

    public void asyncChangeBackupStorageState(String uuid, BackupStorageStateEvent evt) {
        APIChangeBackupStorageStateMsg msg = new APIChangeBackupStorageStateMsg(uuid, evt.toString());
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        bus.send(msg);
    }

    public BackupStorageInventory changeBackupStorageState(String uuid, BackupStorageStateEvent event) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeBackupStorageStateMsg msg = new APIChangeBackupStorageStateMsg(uuid, event.toString());
        msg.setSession(adminSession);
        APIChangeBackupStorageStateEvent e = sender.send(msg, APIChangeBackupStorageStateEvent.class);
        return e.getInventory();
    }

    public void asyncDeleteBackupStorage(String uuid) {
        APIDeleteBackupStorageMsg msg = new APIDeleteBackupStorageMsg(uuid);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        bus.send(msg);
    }

    public void deleteBackupStorage(String uuid) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteBackupStorageMsg msg = new APIDeleteBackupStorageMsg(uuid);
        msg.setSession(adminSession);
        sender.send(msg, APIDeleteBackupStorageEvent.class);
    }

    public void asyncAttachBackupStorage(String zoneUuid, String uuid) throws InterruptedException {
        APIAttachBackupStorageToZoneMsg msg = new APIAttachBackupStorageToZoneMsg(zoneUuid, uuid);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        bus.send(msg);
    }

    public BackupStorageInventory attachBackupStorage(String zoneUuid, String uuid) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAttachBackupStorageToZoneMsg msg = new APIAttachBackupStorageToZoneMsg(zoneUuid, uuid);
        msg.setSession(adminSession);
        APIAttachBackupStorageToZoneEvent e = sender.send(msg, APIAttachBackupStorageToZoneEvent.class);
        return e.getInventory();
    }

    public void asyncDetachBackupStorage(String uuid) throws InterruptedException {
        APIDetachBackupStorageFromZoneMsg msg = new APIDetachBackupStorageFromZoneMsg(uuid);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        bus.send(msg);
    }

    public BackupStorageInventory detachBackupStorage(String uuid, String zoneUuid) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDetachBackupStorageFromZoneMsg msg = new APIDetachBackupStorageFromZoneMsg(uuid);
        msg.setSession(adminSession);
        msg.setZoneUuid(zoneUuid);
        APIDetachBackupStorageFromZoneEvent e = sender.send(msg, APIDetachBackupStorageFromZoneEvent.class);
        return e.getInventory();
    }

    public ImageInventory addImage(ImageInventory inv, String... bsUuids) throws ApiSenderException {
        return addImage(inv, null, bsUuids);
    }

    public ImageInventory addImage(ImageInventory inv, SessionInventory session, String... bsUuids) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAddImageMsg msg = new APIAddImageMsg();
        msg.setResourceUuid(inv.getUuid());
        msg.setSession(session == null ? adminSession : session);
        msg.setDescription(inv.getDescription());
        msg.setMediaType(inv.getMediaType());
        msg.setGuestOsType(inv.getGuestOsType());
        msg.setFormat(inv.getFormat());
        msg.setName(inv.getName());
        for (String bsUuid : bsUuids) {
            msg.getBackupStorageUuids().add(bsUuid);
        }
        msg.setUrl(inv.getUrl());
        msg.setType(ImageConstant.ZSTACK_IMAGE_TYPE);
        APIAddImageEvent e = sender.send(msg, APIAddImageEvent.class);
        return e.getInventory();
    }

    public void deleteImage(String uuid, List<String> bsUuids) throws ApiSenderException {
        deleteImage(uuid, bsUuids, null);
    }

    public void deleteImage(String uuid, List<String> bsUuids, SessionInventory session) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteImageMsg msg = new APIDeleteImageMsg(uuid);
        msg.setBackupStorageUuids(bsUuids);
        msg.setSession(session == null ? adminSession : session);
        sender.send(msg, APIDeleteImageEvent.class);
    }

    public void deleteImage(String uuid, SessionInventory session) throws ApiSenderException {
        deleteImage(uuid, null, session);
    }

    public void deleteImage(String uuid) throws ApiSenderException {
        deleteImage(uuid, null, null);
    }

    public ImageInventory syncImageSize(String imageUuid, SessionInventory session) throws ApiSenderException {
        APISyncImageSizeMsg msg = new APISyncImageSizeMsg();
        msg.setUuid(imageUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APISyncImageSizeEvent evt = sender.send(msg, APISyncImageSizeEvent.class);
        return evt.getInventory();
    }

    public List<ImageInventory> listImage(List<String> uuids) throws ApiSenderException {
        return listImage(0, -1, uuids);
    }

    public List<ImageInventory> listImage(int offset, int length, List<String> uuids) throws ApiSenderException {
        APIListImageMsg msg = new APIListImageMsg(uuids);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setOffset(offset);
        msg.setLength(length);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListImageReply reply = sender.call(msg, APIListImageReply.class);
        return reply.getInventories();
    }

    public InstanceOfferingInventory changeInstanceOfferingState(String uuid, InstanceOfferingStateEvent sevt) throws ApiSenderException {
        return changeInstanceOfferingState(uuid, sevt, null);
    }

    public InstanceOfferingInventory changeInstanceOfferingState(String uuid, InstanceOfferingStateEvent sevt, SessionInventory session) throws ApiSenderException {
        APIChangeInstanceOfferingStateMsg msg = new APIChangeInstanceOfferingStateMsg();
        msg.setUuid(uuid);
        msg.setStateEvent(sevt.toString());
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeInstanceOfferingStateEvent evt = sender.send(msg, APIChangeInstanceOfferingStateEvent.class);
        return evt.getInventory();
    }

    public InstanceOfferingInventory addInstanceOffering(InstanceOfferingInventory inv) throws ApiSenderException {
        return addInstanceOffering(inv, null);
    }

    public InstanceOfferingInventory addInstanceOffering(InstanceOfferingInventory inv, SessionInventory session) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateInstanceOfferingMsg msg = new APICreateInstanceOfferingMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setName(inv.getName());
        msg.setCpuNum(inv.getCpuNum());
        msg.setCpuSpeed(inv.getCpuSpeed());
        msg.setMemorySize(inv.getMemorySize());
        msg.setDescription(inv.getDescription());
        msg.setAllocatorStrategy(inv.getAllocatorStrategy());
        APICreateInstanceOfferingEvent e = sender.send(msg, APICreateInstanceOfferingEvent.class);
        return e.getInventory();
    }

    public List<InstanceOfferingInventory> listInstanceOffering(List<String> uuids) throws ApiSenderException {
        return listInstanceOffering(0, -1, uuids);
    }

    public List<InstanceOfferingInventory> listInstanceOffering(int offset, int length, List<String> uuids) throws ApiSenderException {
        APIListInstanceOfferingMsg msg = new APIListInstanceOfferingMsg(uuids);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setOffset(offset);
        msg.setLength(length);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListInstanceOfferingReply reply = sender.call(msg, APIListInstanceOfferingReply.class);
        return reply.getInventories();
    }

    public void deleteInstanceOffering(String uuid) throws ApiSenderException {
        deleteInstanceOffering(uuid, null);
    }

    public void deleteInstanceOffering(String uuid, SessionInventory session) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteInstanceOfferingMsg msg = new APIDeleteInstanceOfferingMsg(uuid);
        msg.setSession(session == null ? adminSession : session);
        sender.send(msg, APIDeleteInstanceOfferingEvent.class);
    }

    public DiskOfferingInventory addDiskOffering(DiskOfferingInventory inv) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateDiskOfferingMsg msg = new APICreateDiskOfferingMsg();
        msg.setSession(adminSession);
        msg.setName(inv.getName());
        msg.setDiskSize(inv.getDiskSize());
        msg.setDescription(inv.getDescription());
        APICreateDiskOfferingEvent e = sender.send(msg, APICreateDiskOfferingEvent.class);
        return e.getInventory();
    }

    public List<DiskOfferingInventory> listDiskOffering(List<String> uuids) throws ApiSenderException {
        return listDiskOffering(0, -1, uuids);
    }

    public List<DiskOfferingInventory> listDiskOffering(int offset, int length, List<String> uuids) throws ApiSenderException {
        APIListDiskOfferingMsg msg = new APIListDiskOfferingMsg(uuids);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        msg.setOffset(offset);
        msg.setLength(length);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListDiskOfferingReply reply = sender.call(msg, APIListDiskOfferingReply.class);
        return reply.getInventories();
    }

    public void deleteDiskOffering(String uuid) throws ApiSenderException {
        deleteDiskOffering(uuid, null);
    }

    public void deleteDiskOffering(String uuid, SessionInventory session) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteDiskOfferingMsg msg = new APIDeleteDiskOfferingMsg(uuid);
        msg.setSession(session == null ? adminSession : session);
        sender.send(msg, APIDeleteDiskOfferingEvent.class);
    }

    public VolumeInventory createDataVolume(String name, String diskOfferingUuid) throws ApiSenderException {
        return createDataVolume(name, diskOfferingUuid, null);
    }

    public VolumeInventory createDataVolume(String name, String diskOfferingUuid, SessionInventory session) throws ApiSenderException {
        return createDataVolume(name, diskOfferingUuid, null, session);
    }

    public VolumeInventory createDataVolume(String name,
                                            String diskOfferingUuid,
                                            String primaryStorageUuid,
                                            SessionInventory session) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateDataVolumeMsg msg = new APICreateDataVolumeMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setPrimaryStorageUuid(primaryStorageUuid);
        msg.setName(name);
        msg.setDiskOfferingUuid(diskOfferingUuid);
        APICreateDataVolumeEvent e = sender.send(msg, APICreateDataVolumeEvent.class);
        return e.getInventory();
    }

    public VolumeInventory createDataVolumeOnLocalStorage(String name,
                                                          String diskOfferingUuid,
                                                          String primaryStorageUuid,
                                                          String hostUuid,
                                                          SessionInventory session) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateDataVolumeMsg msg = new APICreateDataVolumeMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setPrimaryStorageUuid(primaryStorageUuid);
        List<String> list = new ArrayList<String>();
        list.add(String.format("localStorage::hostUuid::%s", hostUuid));
        msg.setSystemTags(list);
        msg.setName(name);
        msg.setDiskOfferingUuid(diskOfferingUuid);
        APICreateDataVolumeEvent e = sender.send(msg, APICreateDataVolumeEvent.class);
        return e.getInventory();
    }

    public VolumeInventory changeVolumeState(String uuid, VolumeStateEvent stateEvent) throws ApiSenderException {
        return changeVolumeState(uuid, stateEvent, null);
    }

    public VolumeInventory changeVolumeState(String uuid, VolumeStateEvent stateEvent, SessionInventory session) throws ApiSenderException {
        APIChangeVolumeStateMsg msg = new APIChangeVolumeStateMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(uuid);
        msg.setStateEvent(stateEvent.toString());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeVolumeStateEvent evt = sender.send(msg, APIChangeVolumeStateEvent.class);
        return evt.getInventory();
    }

    public List<VolumeInventory> listVolume(List<String> uuids) throws ApiSenderException {
        return listVolume(0, -1, uuids);
    }

    public List<VolumeInventory> listVolume(int offset, int length, List<String> uuids) throws ApiSenderException {
        APIListVolumeMsg msg = new APIListVolumeMsg(uuids);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setOffset(offset);
        msg.setLength(length);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListVolumeReply reply = sender.call(msg, APIListVolumeReply.class);
        return reply.getInventories();
    }

    public void deleteDataVolume(String uuid) throws ApiSenderException {
        deleteDataVolume(uuid, null);
    }

    public void deleteDataVolume(String uuid, SessionInventory session) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteDataVolumeMsg msg = new APIDeleteDataVolumeMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(uuid);
        sender.send(msg, APIDeleteDataVolumeEvent.class);
    }

    public GlobalConfigInventory[] listGlobalConfig(Long ids[]) throws ApiSenderException {
        return listGlobalConfig(0, -1, ids);
    }

    public GlobalConfigInventory[] listGlobalConfig(int offset, int length, Long[] ids) throws ApiSenderException {
        APIListGlobalConfigMsg msg = new APIListGlobalConfigMsg();
        List<Long> idArr = new ArrayList<Long>();
        if (ids != null) {
            Collections.addAll(idArr, ids);
        }
        msg.setIds(idArr);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setOffset(offset);
        msg.setLength(length);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListGlobalConfigReply reply = sender.call(msg, APIListGlobalConfigReply.class);
        return reply.getInventories();
    }

    public GlobalConfigInventory updateGlobalConfig(GlobalConfigInventory inv) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateGlobalConfigMsg msg = new APIUpdateGlobalConfigMsg();
        msg.setSession(adminSession);
        msg.setCategory(inv.getCategory());
        msg.setName(inv.getName());
        msg.setValue(inv.getValue());
        APIUpdateGlobalConfigEvent e = sender.send(msg, APIUpdateGlobalConfigEvent.class);
        return e.getInventory();
    }

    public L2NetworkInventory createNoVlanL2Network(String zoneUuid, String iface) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateL2NoVlanNetworkMsg msg = new APICreateL2NoVlanNetworkMsg();
        msg.setSession(adminSession);
        msg.setName("TestL2Network");
        msg.setDescription("test");
        msg.setZoneUuid(zoneUuid);
        msg.setPhysicalInterface(iface);
        msg.setType(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
        APICreateL2NetworkEvent e = sender.send(msg, APICreateL2NetworkEvent.class);
        return e.getInventory();
    }

    public List<L2NetworkInventory> listL2Network(List<String> uuids) throws ApiSenderException {
        return listL2Network(0, -1, uuids);
    }

    public List<L2NetworkInventory> listL2Network(int offset, int length, List<String> uuids) throws ApiSenderException {
        APIListL2NetworkMsg msg = new APIListL2NetworkMsg(uuids);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setOffset(offset);
        msg.setLength(length);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListL2NetworkReply reply = sender.call(msg, APIListL2NetworkReply.class);
        return reply.getInventories();
    }

    public void deleteL2Network(String uuid) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteL2NetworkMsg msg = new APIDeleteL2NetworkMsg();
        msg.setSession(adminSession);
        msg.setUuid(uuid);
        sender.send(msg, APIDeleteL2NetworkEvent.class);
    }

    public L2NetworkInventory attachL2NetworkToCluster(String l2NetworkUuid, String clusterUuid) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAttachL2NetworkToClusterMsg msg = new APIAttachL2NetworkToClusterMsg();
        msg.setSession(adminSession);
        msg.setL2NetworkUuid(l2NetworkUuid);
        msg.setClusterUuid(clusterUuid);
        APIAttachL2NetworkToClusterEvent evt = sender.send(msg, APIAttachL2NetworkToClusterEvent.class);
        return evt.getInventory();
    }

    public void detachL2NetworkFromCluster(String l2NetworkUuid, String clusterUuid) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDetachL2NetworkFromClusterMsg msg = new APIDetachL2NetworkFromClusterMsg();
        msg.setSession(adminSession);
        msg.setL2NetworkUuid(l2NetworkUuid);
        msg.setClusterUuid(clusterUuid);
        sender.send(msg, APIDetachL2NetworkFromClusterEvent.class);
    }

    public L3NetworkInventory createL3BasicNetwork(String l2NetworkUuid) throws ApiSenderException {
        return createL3BasicNetwork(l2NetworkUuid, null);
    }

    public L3NetworkInventory createL3BasicNetwork(String l2NetworkUuid, SessionInventory session) throws ApiSenderException {
        APICreateL3NetworkMsg msg = new APICreateL3NetworkMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setL2NetworkUuid(l2NetworkUuid);
        msg.setType(L3NetworkConstant.L3_BASIC_NETWORK_TYPE);
        msg.setName("Test-L3Network");
        msg.setDescription("Test");
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateL3NetworkEvent e = sender.send(msg, APICreateL3NetworkEvent.class);
        return e.getInventory();
    }

    public L3NetworkInventory changeL3NetworkState(String uuid, L3NetworkStateEvent sevnt) throws ApiSenderException {
        return changeL3NetworkState(uuid, sevnt, null);
    }

    public L3NetworkInventory changeL3NetworkState(String uuid, L3NetworkStateEvent sevnt, SessionInventory session) throws ApiSenderException {
        APIChangeL3NetworkStateMsg msg = new APIChangeL3NetworkStateMsg();
        msg.setUuid(uuid);
        msg.setStateEvent(sevnt.toString());
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeL3NetworkStateEvent e = sender.send(msg, APIChangeL3NetworkStateEvent.class);
        return e.getInventory();
    }

    public void deleteL3Network(String uuid) throws ApiSenderException {
        deleteL3Network(uuid, null);
    }

    public void deleteL3Network(String uuid, SessionInventory session) throws ApiSenderException {
        APIDeleteL3NetworkMsg msg = new APIDeleteL3NetworkMsg(uuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteL3NetworkEvent.class);
    }

    public List<L3NetworkInventory> listL3Network(List<String> uuids) throws ApiSenderException {
        return listL3Network(0, -1, uuids);
    }

    public List<L3NetworkInventory> listL3Network(int offset, int length, List<String> uuids) throws ApiSenderException {
        APIListL3NetworkMsg msg = new APIListL3NetworkMsg(uuids);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setOffset(offset);
        msg.setLength(length);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListL3NetworkReply reply = sender.call(msg, APIListL3NetworkReply.class);
        return reply.getInventories();
    }

    public APIGetIpAddressCapacityReply getIpAddressCapacity(List<String> iprUuids, List<String> l3Uuids, List<String> zoneUuids) throws ApiSenderException {
        APIGetIpAddressCapacityMsg msg = new APIGetIpAddressCapacityMsg();
        msg.setSession(adminSession);
        msg.setIpRangeUuids(iprUuids);
        msg.setL3NetworkUuids(l3Uuids);
        msg.setZoneUuids(zoneUuids);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        return sender.call(msg, APIGetIpAddressCapacityReply.class);
    }

    public APIGetIpAddressCapacityReply getIpAddressCapacityByAll() throws ApiSenderException {
        return getIpAddressCapacityByAll(null);
    }

    public APIGetIpAddressCapacityReply getIpAddressCapacityByAll(SessionInventory session) throws ApiSenderException {
        APIGetIpAddressCapacityMsg msg = new APIGetIpAddressCapacityMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setAll(true);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        return sender.call(msg, APIGetIpAddressCapacityReply.class);
    }

    public IpRangeInventory addIpRangeByCidr(String l3NetworkUuid, String cidr) throws ApiSenderException {
        return addIpRangeByCidr(l3NetworkUuid, cidr, null);
    }

    public IpRangeInventory addIpRangeByCidr(String l3NetworkUuid, String cidr, SessionInventory session) throws ApiSenderException {
        APIAddIpRangeByNetworkCidrMsg msg = new APIAddIpRangeByNetworkCidrMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setL3NetworkUuid(l3NetworkUuid);
        msg.setNetworkCidr(cidr);
        msg.setName("TestIpRange");
        msg.setDescription("test");
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAddIpRangeByNetworkCidrEvent e = sender.send(msg, APIAddIpRangeByNetworkCidrEvent.class);
        return e.getInventory();
    }

    public IpRangeInventory addIpRange(String l3NetworkUuid, String startIp, String endIp, String gateway, String netmask) throws ApiSenderException {
        return addIpRange(l3NetworkUuid, startIp, endIp, gateway, netmask, null);
    }

    public IpRangeInventory addIpRange(String l3NetworkUuid, String startIp, String endIp, String gateway, String netmask, SessionInventory session) throws ApiSenderException {
        APIAddIpRangeMsg msg = new APIAddIpRangeMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setL3NetworkUuid(l3NetworkUuid);
        msg.setStartIp(startIp);
        msg.setEndIp(endIp);
        msg.setNetmask(netmask);
        msg.setGateway(gateway);
        msg.setName("TestIpRange");
        msg.setDescription("test");
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAddIpRangeEvent e = sender.send(msg, APIAddIpRangeEvent.class);
        return e.getInventory();
    }

    public List<FreeIpInventory> getFreeIp(String l3Uuid, String ipRangeUuid) throws ApiSenderException {
        return getFreeIp(l3Uuid, ipRangeUuid, 100);
    }

    public List<FreeIpInventory> getFreeIp(String l3Uuid, String ipRangeUuid, int limit) throws ApiSenderException {
        return getFreeIp(l3Uuid, ipRangeUuid, limit, null);
    }

    public List<FreeIpInventory> getFreeIp(String l3Uuid, String ipRangeUuid, int limit, String start) throws ApiSenderException {
        return getFreeIp(l3Uuid, ipRangeUuid, limit, start, null);
    }

    public boolean checkIpAvailability(String l3Uuid, String ip) throws ApiSenderException {
        return checkIpAvailability(l3Uuid, ip, null);
    }

    public boolean checkIpAvailability(String l3Uuid, String ip, SessionInventory session) throws ApiSenderException {
        APICheckIpAvailabilityMsg msg = new APICheckIpAvailabilityMsg();
        msg.setL3NetworkUuid(l3Uuid);
        msg.setIp(ip);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICheckIpAvailabilityReply rely = sender.call(msg, APICheckIpAvailabilityReply.class);
        return rely.isAvailable();
    }

    public List<FreeIpInventory> getFreeIp(String l3Uuid, String ipRangeUuid, int limit, String start, SessionInventory session) throws ApiSenderException {
        APIGetFreeIpMsg msg = new APIGetFreeIpMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setL3NetworkUuid(l3Uuid);
        msg.setIpRangeUuid(ipRangeUuid);
        msg.setLimit(limit);
        msg.setStartIp(start);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetFreeIpReply reply = sender.call(msg, APIGetFreeIpReply.class);
        return reply.getInventories();
    }

    public List<IpRangeInventory> listIpRange(List<String> uuids) throws ApiSenderException {
        return listIpRange(0, -1, uuids);
    }

    public List<IpRangeInventory> listIpRange(int offset, int length, List<String> uuids) throws ApiSenderException {
        APIListIpRangeMsg msg = new APIListIpRangeMsg(uuids);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setOffset(offset);
        msg.setLength(length);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListIpRangeReply reply = sender.call(msg, APIListIpRangeReply.class);
        return reply.getInventories();
    }

    public void deleteIpRange(String uuid) throws ApiSenderException {
        deleteIpRange(uuid, null);
    }

    public void deleteIpRange(String uuid, SessionInventory session) throws ApiSenderException {
        APIDeleteIpRangeMsg msg = new APIDeleteIpRangeMsg(uuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        sender.send(msg, APIDeleteIpRangeEvent.class);
    }


    public L3NetworkInventory removeDnsFromL3Network(String dns, String l3NetworkUuid) throws ApiSenderException {
        return removeDnsFromL3Network(dns, l3NetworkUuid, null);
    }

    public L3NetworkInventory removeDnsFromL3Network(String dns, String l3NetworkUuid, SessionInventory session) throws ApiSenderException {
        APIRemoveDnsFromL3NetworkMsg msg = new APIRemoveDnsFromL3NetworkMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setDns(dns);
        msg.setL3NetworkUuid(l3NetworkUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIRemoveDnsFromL3NetworkEvent evt = sender.send(msg, APIRemoveDnsFromL3NetworkEvent.class);
        return evt.getInventory();
    }

    public ZoneInventory createZoneByFullConfig(ZoneInventory inv) throws ApiSenderException {
        APICreateZoneMsg msg = new APICreateZoneMsg();
        msg.setSession(adminSession);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setType(inv.getType());
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateZoneEvent evt = sender.send(msg, APICreateZoneEvent.class);
        return evt.getInventory();
    }

    public ClusterInventory createClusterByFullConfig(ClusterInventory inv) throws ApiSenderException {
        APICreateClusterMsg msg = new APICreateClusterMsg();
        msg.setSession(adminSession);
        msg.setClusterName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setHypervisorType(inv.getHypervisorType());
        msg.setType(inv.getType());
        msg.setZoneUuid(inv.getZoneUuid());
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateClusterEvent evt = sender.send(msg, APICreateClusterEvent.class);
        return evt.getInventory();
    }

    public HostInventory addHostByFullConfig(HostInventory inv) throws ApiSenderException {
        APIAddSimulatorHostMsg msg = new APIAddSimulatorHostMsg();
        msg.setSession(adminSession);
        msg.setClusterUuid(inv.getClusterUuid());
        msg.setDescription(inv.getDescription());
        msg.setName(inv.getName());
        msg.setManagementIp(inv.getManagementIp());
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setMemoryCapacity(inv.getAvailableMemoryCapacity());
        msg.setCpuCapacity(inv.getAvailableCpuCapacity());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAddHostEvent evt = sender.send(msg, APIAddHostEvent.class);
        return evt.getInventory();
    }

    public APIGetPrimaryStorageCapacityReply getPrimaryStorageCapacityByAll() throws ApiSenderException {
        APIGetPrimaryStorageCapacityMsg msg = new APIGetPrimaryStorageCapacityMsg();
        msg.setSession(adminSession);
        msg.setAll(true);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        return sender.call(msg, APIGetPrimaryStorageCapacityReply.class);
    }

    public APIGetPrimaryStorageCapacityReply getPrimaryStorageCapacity(List<String> zoneUuids, List<String> clusterUuids, List<String> psUuids) throws ApiSenderException {
        APIGetPrimaryStorageCapacityMsg msg = new APIGetPrimaryStorageCapacityMsg();
        msg.setSession(adminSession);
        msg.setZoneUuids(zoneUuids);
        msg.setClusterUuids(clusterUuids);
        msg.setPrimaryStorageUuids(psUuids);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        return sender.call(msg, APIGetPrimaryStorageCapacityReply.class);
    }

    public PrimaryStorageInventory addPrimaryStorageByFullConfig(PrimaryStorageInventory inv) throws ApiSenderException {
        APIAddSimulatorPrimaryStorageMsg msg = new APIAddSimulatorPrimaryStorageMsg();
        msg.setSession(adminSession);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setType(inv.getType());
        msg.setUrl(inv.getUrl());
        msg.setTotalCapacity(inv.getTotalCapacity());
        msg.setAvailableCapacity(inv.getAvailableCapacity());
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setType(SimulatorPrimaryStorageConstant.SIMULATOR_PRIMARY_STORAGE_TYPE);
        msg.setZoneUuid(inv.getZoneUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAddPrimaryStorageEvent evt = sender.send(msg, APIAddPrimaryStorageEvent.class);
        return evt.getInventory();
    }

    public APIGetBackupStorageCapacityReply getBackupStorageCapacityByAll() throws ApiSenderException {
        APIGetBackupStorageCapacityMsg msg = new APIGetBackupStorageCapacityMsg();
        msg.setSession(adminSession);
        msg.setAll(true);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        return sender.call(msg, APIGetBackupStorageCapacityReply.class);
    }

    public APIGetBackupStorageCapacityReply getBackupStorageCapacity(List<String> zoneUuids, List<String> bsUuids) throws ApiSenderException {
        APIGetBackupStorageCapacityMsg msg = new APIGetBackupStorageCapacityMsg();
        msg.setSession(adminSession);
        msg.setBackupStorageUuids(bsUuids);
        msg.setZoneUuids(zoneUuids);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        return sender.call(msg, APIGetBackupStorageCapacityReply.class);
    }

    public BackupStorageInventory addBackupStorageByFullConfig(BackupStorageInventory inv) throws ApiSenderException {
        APIAddSimulatorBackupStorageMsg msg = new APIAddSimulatorBackupStorageMsg();
        msg.setSession(adminSession);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setTotalCapacity(inv.getTotalCapacity());
        msg.setUrl(inv.getUrl());
        msg.setAvailableCapacity(inv.getAvailableCapacity());
        msg.setType(SimulatorBackupStorageConstant.SIMULATOR_BACKUP_STORAGE_TYPE);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAddBackupStorageEvent evt = sender.send(msg, APIAddBackupStorageEvent.class);
        return evt.getInventory();
    }

    public ImageInventory addImageByFullConfig(ImageInventory inv, String bsUuid) throws ApiSenderException {
        return addImageByFullConfig(inv, bsUuid, null);
    }

    public ImageInventory addImageByFullConfig(ImageInventory inv, String bsUuid, SessionInventory session) throws ApiSenderException {
        APIAddImageMsg msg = new APIAddImageMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.getBackupStorageUuids().add(bsUuid);
        msg.setDescription(inv.getDescription());
        msg.setMediaType(inv.getMediaType());
        msg.setGuestOsType(inv.getGuestOsType());
        msg.setFormat(inv.getFormat());
        msg.setPlatform(inv.getPlatform());
        msg.setType(inv.getType());
        msg.setName(inv.getName());
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setUrl(inv.getUrl());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAddImageEvent evt = sender.send(msg, APIAddImageEvent.class);
        return evt.getInventory();
    }

    public L2VlanNetworkInventory createL2VlanNetworkByFullConfig(L2VlanNetworkInventory inv) throws ApiSenderException {
        APICreateL2VlanNetworkMsg msg = new APICreateL2VlanNetworkMsg();
        msg.setSession(adminSession);
        msg.setDescription(inv.getDescription());
        msg.setName(inv.getName());
        msg.setPhysicalInterface(inv.getPhysicalInterface());
        msg.setType(inv.getType());
        msg.setZoneUuid(inv.getZoneUuid());
        msg.setVlan(inv.getVlan());
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateL2NetworkEvent evt = sender.send(msg, APICreateL2NetworkEvent.class);
        return (L2VlanNetworkInventory) evt.getInventory();
    }

    public L2NetworkInventory createL2NetworkByFullConfig(L2NetworkInventory inv) throws ApiSenderException {
        APICreateL2NoVlanNetworkMsg msg = new APICreateL2NoVlanNetworkMsg();
        msg.setSession(adminSession);
        msg.setDescription(inv.getDescription());
        msg.setName(inv.getName());
        msg.setPhysicalInterface(inv.getPhysicalInterface());
        msg.setType(inv.getType());
        msg.setZoneUuid(inv.getZoneUuid());
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateL2NetworkEvent evt = sender.send(msg, APICreateL2NetworkEvent.class);
        return evt.getInventory();
    }

    public L3NetworkInventory createL3NetworkByFullConfig(L3NetworkInventory inv) throws ApiSenderException {
        return createL3NetworkByFullConfig(inv, adminSession);
    }

    public L3NetworkInventory createL3NetworkByFullConfig(L3NetworkInventory inv, SessionInventory session) throws ApiSenderException {
        APICreateL3NetworkMsg msg = new APICreateL3NetworkMsg();
        msg.setSession(session);
        msg.setDescription(inv.getDescription());
        msg.setL2NetworkUuid(inv.getL2NetworkUuid());
        msg.setName(inv.getName());
        msg.setDnsDomain(inv.getDnsDomain());
        msg.setType(inv.getType());
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateL3NetworkEvent evt = sender.send(msg, APICreateL3NetworkEvent.class);
        return evt.getInventory();
    }

    public IpRangeInventory addIpRangeByFullConfig(IpRangeInventory inv, SessionInventory session) throws ApiSenderException {
        APIAddIpRangeMsg msg = new APIAddIpRangeMsg();
        msg.setSession(session);
        msg.setL3NetworkUuid(inv.getL3NetworkUuid());
        msg.setStartIp(inv.getStartIp());
        msg.setEndIp(inv.getEndIp());
        msg.setNetmask(inv.getNetmask());
        msg.setGateway(inv.getGateway());
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAddIpRangeEvent e = sender.send(msg, APIAddIpRangeEvent.class);
        return e.getInventory();
    }

    public IpRangeInventory addIpRangeByFullConfig(IpRangeInventory inv) throws ApiSenderException {
        return addIpRangeByFullConfig(inv, adminSession);
    }

    public DiskOfferingInventory addDiskOfferingByFullConfig(DiskOfferingInventory inv) throws ApiSenderException {
        return addDiskOfferingByFullConfig(inv, null);
    }

    public DiskOfferingInventory addDiskOfferingByFullConfig(DiskOfferingInventory inv, SessionInventory session) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateDiskOfferingMsg msg = new APICreateDiskOfferingMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setName(inv.getName());
        msg.setDiskSize(inv.getDiskSize());
        msg.setDescription(inv.getDescription());
        msg.setAllocationStrategy(inv.getAllocatorStrategy());
        APICreateDiskOfferingEvent e = sender.send(msg, APICreateDiskOfferingEvent.class);
        return e.getInventory();
    }

    public VmInstanceInventory createVmFromClone(VmInstanceInventory toClone) throws ApiSenderException {
        APICreateVmInstanceMsg msg = new APICreateVmInstanceMsg();
        msg.setSession(adminSession);
        msg.setName(String.format("clone-%s", toClone.getName()));
        msg.setImageUuid(toClone.getImageUuid());
        msg.setDataDiskOfferingUuids(CollectionUtils.transformToList(toClone.getAllVolumes(), new Function<String, VolumeInventory>() {
            @Override
            public String call(VolumeInventory arg) {
                if (!arg.getType().equals(VolumeType.Root.toString())) {
                    return arg.getDiskOfferingUuid();
                }
                return null;
            }
        }));
        msg.setL3NetworkUuids(CollectionUtils.transformToList(toClone.getVmNics(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                return arg.getL3NetworkUuid();
            }
        }));
        msg.setDefaultL3NetworkUuid(toClone.getDefaultL3NetworkUuid());
        msg.setType(toClone.getType());
        msg.setInstanceOfferingUuid(toClone.getInstanceOfferingUuid());
        msg.setDescription(String.format("clone from vm[uuid:%s]", toClone.getUuid()));
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateVmInstanceEvent evt = sender.send(msg, APICreateVmInstanceEvent.class);
        return evt.getInventory();
    }

    public void setRootPassword(String rootPassword) {
        this.rootPassword = rootPassword;
    }

    public VmInstanceInventory createVmByFullConfig(VmInstanceInventory inv, String rootDiskOfferingUuid, List<String> l3NetworkUuids,
                                                    List<String> diskOfferingUuids, SessionInventory session) throws ApiSenderException {
        APICreateVmInstanceMsg msg = new APICreateVmInstanceMsg();
        msg.setClusterUuid(inv.getClusterUuid());
        if (diskOfferingUuids != null) {
            msg.setDataDiskOfferingUuids(diskOfferingUuids);
        }
        msg.setSession(session);
        msg.setDescription(inv.getDescription());
        msg.setHostUuid(inv.getHostUuid());
        msg.setImageUuid(inv.getImageUuid());
        msg.setInstanceOfferingUuid(inv.getInstanceOfferingUuid());
        msg.setL3NetworkUuids(l3NetworkUuids);
        msg.setName(inv.getName());
        msg.setType(inv.getType());
        msg.setZoneUuid(inv.getZoneUuid());
        msg.setHostUuid(inv.getHostUuid());
        msg.setClusterUuid(inv.getClusterUuid());
        msg.setRootDiskOfferingUuid(rootDiskOfferingUuid);
        msg.setDefaultL3NetworkUuid(inv.getDefaultL3NetworkUuid());
        if (rootPassword != null) {
            msg.setRootPassword(rootPassword);
        }
        if (msg.getL3NetworkUuids().size() > 1 && msg.getDefaultL3NetworkUuid() == null) {
            msg.setDefaultL3NetworkUuid(msg.getL3NetworkUuids().get(0));
        }
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateVmInstanceEvent evt = sender.send(msg, APICreateVmInstanceEvent.class);
        return evt.getInventory();
    }

    public VmInstanceInventory changeInstanceOffering(String vmUuid, String instanceOfferingUuid) throws ApiSenderException {
        APIChangeInstanceOfferingMsg msg = new APIChangeInstanceOfferingMsg();
        msg.setVmInstanceUuid(vmUuid);
        msg.setInstanceOfferingUuid(instanceOfferingUuid);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeInstanceOfferingEvent evt = sender.send(msg, APIChangeInstanceOfferingEvent.class);
        return evt.getInventory();
    }

    public VmInstanceInventory createVmByFullConfig(VmInstanceInventory inv, String rootDiskOfferingUuid, List<String> l3NetworkUuids,
                                                    List<String> diskOfferingUuids) throws ApiSenderException {
        return createVmByFullConfig(inv, rootDiskOfferingUuid, l3NetworkUuids, diskOfferingUuids, adminSession);
    }

    public List<VmInstanceInventory> listVmInstances(List<String> uuids) throws ApiSenderException {
        APIListVmInstanceMsg msg = new APIListVmInstanceMsg(uuids);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListVmInstanceReply reply = sender.call(msg, APIListVmInstanceReply.class);
        return reply.getInventories();
    }

    public VmInstanceInventory stopVmInstance(String uuid) throws ApiSenderException {
        return stopVmInstance(uuid, null);
    }

    public VmInstanceInventory stopVmInstance(String uuid, SessionInventory session) throws ApiSenderException {
        APIStopVmInstanceMsg msg = new APIStopVmInstanceMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(uuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIStopVmInstanceEvent evt = sender.send(msg, APIStopVmInstanceEvent.class);
        return evt.getInventory();
    }

    public VmInstanceInventory forcefullyStopVmInstance(String uuid) throws ApiSenderException {
        return forcefullyStopVmInstance(uuid, null);
    }

    public VmInstanceInventory forcefullyStopVmInstance(String uuid, SessionInventory session) throws ApiSenderException {
        APIStopVmInstanceMsg msg = new APIStopVmInstanceMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(uuid);
        msg.setType("cold");
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIStopVmInstanceEvent evt = sender.send(msg, APIStopVmInstanceEvent.class);
        return evt.getInventory();
    }

    public VmAccountPerference changeVmPassword(VmAccountPerference account)
            throws ApiSenderException {
        APIChangeVmPasswordMsg msg = new APIChangeVmPasswordMsg();
        msg.setSession(adminSession);
        msg.setVmInstanceUuid(account.getVmUuid());
        msg.setAccount(account.getUserAccount());
        msg.setPassword(account.getAccountPassword());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeVmPasswordEvent evt = sender.send(msg, APIChangeVmPasswordEvent.class);
        return new VmAccountPerference(evt.getVmUuid(), evt.getUserAccount(), evt.getAccountPassword());
    }

    public VmInstanceInventory suspendVmInstance(String uuid) throws ApiSenderException {
        return suspendVmInstance(uuid, null);
    }

    public VmInstanceInventory suspendVmInstance(String uuid, SessionInventory session) throws ApiSenderException {
        APISuspendVmInstanceMsg msg = new APISuspendVmInstanceMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(uuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APISuspendVmInstanceEvent evt = sender.send(msg, APISuspendVmInstanceEvent.class);
        return evt.getInventory();
    }

    public VmInstanceInventory resumeVmInstance(String uuid) throws ApiSenderException {
        return resumeVmInstance(uuid, null);
    }

    public VmInstanceInventory resumeVmInstance(String uuid, SessionInventory session) throws ApiSenderException {
        APIResumeVmInstanceMsg msg = new APIResumeVmInstanceMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(uuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIResumeVmInstanceEvent evt = sender.send(msg, APIStartVmInstanceEvent.class);
        return evt.getInventory();
    }

    public VmInstanceInventory rebootVmInstance(String uuid) throws ApiSenderException {
        return rebootVmInstance(uuid, null);
    }

    public VmInstanceInventory rebootVmInstance(String uuid, SessionInventory session) throws ApiSenderException {
        APIRebootVmInstanceMsg msg = new APIRebootVmInstanceMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(uuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIRebootVmInstanceEvent evt = sender.send(msg, APIRebootVmInstanceEvent.class);
        return evt.getInventory();
    }

    public void destroyVmInstance(String uuid) throws ApiSenderException {
        destroyVmInstance(uuid, null);
    }

    public void destroyVmInstance(String uuid, SessionInventory session) throws ApiSenderException {
        APIDestroyVmInstanceMsg msg = new APIDestroyVmInstanceMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(uuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDestroyVmInstanceEvent.class);
    }

    public VmInstanceInventory startVmInstance(String uuid) throws ApiSenderException {
        return startVmInstance(uuid, null);
    }

    public VmInstanceInventory startVmInstance(String uuid, SessionInventory session) throws ApiSenderException {
        APIStartVmInstanceMsg msg = new APIStartVmInstanceMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(uuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIStartVmInstanceEvent evt = sender.send(msg, APIStartVmInstanceEvent.class);
        return evt.getInventory();
    }

    public VmInstanceInventory migrateVmInstance(String vmUuid, String destHostUuid) throws ApiSenderException {
        return migrateVmInstance(vmUuid, destHostUuid, null);
    }

    public VmInstanceInventory migrateVmInstance(String vmUuid, String destHostUuid, SessionInventory session) throws ApiSenderException {
        APIMigrateVmMsg msg = new APIMigrateVmMsg();
        msg.setVmUuid(vmUuid);
        msg.setSession(session == null ? adminSession : session);
        msg.setHostUuid(destHostUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIMigrateVmEvent evt = sender.send(msg, APIMigrateVmEvent.class);
        return evt.getInventory();
    }

    public List<VmInstanceInventory> getDataVolumeCandidateVmForAttaching(String volUuid) throws ApiSenderException {
        return getDataVolumeCandidateVmForAttaching(volUuid, null);
    }

    public List<VmInstanceInventory> getDataVolumeCandidateVmForAttaching(String volUuid, SessionInventory session) throws ApiSenderException {
        APIGetDataVolumeAttachableVmMsg msg = new APIGetDataVolumeAttachableVmMsg();
        msg.setVolumeUuid(volUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetDataVolumeAttachableVmReply reply = sender.call(msg, APIGetDataVolumeAttachableVmReply.class);
        return reply.getInventories();
    }

    public List<VolumeInventory> getVmAttachableVolume(String vmUuid) throws ApiSenderException {
        return getVmAttachableVolume(vmUuid, null);
    }

    public List<VolumeInventory> getVmAttachableVolume(String vmUuid, SessionInventory session) throws ApiSenderException {
        APIGetVmAttachableDataVolumeMsg msg = new APIGetVmAttachableDataVolumeMsg();
        msg.setVmInstanceUuid(vmUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetVmAttachableDataVolumeReply reply = sender.call(msg, APIGetVmAttachableDataVolumeReply.class);
        return reply.getInventories();
    }

    public VolumeInventory attachVolumeToVm(String vmUuid, String volumeUuid) throws ApiSenderException {
        return attachVolumeToVm(vmUuid, volumeUuid, null);
    }

    public VolumeInventory attachVolumeToVm(String vmUuid, String volumeUuid, SessionInventory session) throws ApiSenderException {
        APIAttachDataVolumeToVmMsg msg = new APIAttachDataVolumeToVmMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setVmUuid(vmUuid);
        msg.setVolumeUuid(volumeUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAttachDataVolumeToVmEvent evt = sender.send(msg, APIAttachDataVolumeToVmEvent.class);
        return evt.getInventory();
    }

    public VolumeInventory createDataVolumeFromTemplate(String imageUuid, String primaryStorageUuid) throws ApiSenderException {
        return createDataVolumeFromTemplate(imageUuid, primaryStorageUuid, null);
    }

    public VolumeInventory createDataVolumeFromTemplate(String imageUuid, String primaryStorageUuid, SessionInventory session) throws ApiSenderException {
        APICreateDataVolumeFromVolumeTemplateMsg msg = new APICreateDataVolumeFromVolumeTemplateMsg();
        msg.setPrimaryStorageUuid(primaryStorageUuid);
        msg.setImageUuid(imageUuid);
        msg.setSession(session == null ? adminSession : session);
        msg.setName("data");
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateDataVolumeFromVolumeTemplateEvent evt = sender.send(msg, APICreateDataVolumeFromVolumeTemplateEvent.class);
        return evt.getInventory();
    }

    public ImageInventory addDataVolumeTemplateFromDataVolume(String volUuid, List<String> bsUuids) throws ApiSenderException {
        return addDataVolumeTemplateFromDataVolume(volUuid, bsUuids, null);
    }

    public ImageInventory addDataVolumeTemplateFromDataVolume(String volUuid, List<String> bsUuids, SessionInventory session) throws ApiSenderException {
        APICreateDataVolumeTemplateFromVolumeMsg msg = new APICreateDataVolumeTemplateFromVolumeMsg();
        msg.setName("data-vol");
        msg.setVolumeUuid(volUuid);
        msg.setSession(session == null ? adminSession : session);
        msg.setBackupStorageUuids(bsUuids);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateDataVolumeTemplateFromVolumeEvent evt = sender.send(msg, APICreateDataVolumeTemplateFromVolumeEvent.class);
        return evt.getInventory();
    }

    public List<VolumeFormatReplyStruct> getVolumeFormats() throws ApiSenderException {
        APIGetVolumeFormatMsg msg = new APIGetVolumeFormatMsg();
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetVolumeFormatReply reply = sender.call(msg, APIGetVolumeFormatReply.class);
        return reply.getFormats();
    }

    public VolumeInventory detachVolumeFromVm(String volumeUuid) throws ApiSenderException {
        return detachVolumeFromVm(volumeUuid, null);
    }

    public VolumeInventory detachVolumeFromVm(String volumeUuid, SessionInventory session) throws ApiSenderException {
        APIDetachDataVolumeFromVmMsg msg = new APIDetachDataVolumeFromVmMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(volumeUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDetachDataVolumeFromVmEvent evt = sender.send(msg, APIDetachDataVolumeFromVmEvent.class);
        return evt.getInventory();
    }


    public SessionInventory loginAsAdmin() throws ApiSenderException {
        return loginByAccount(AccountConstant.INITIAL_SYSTEM_ADMIN_NAME, AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD);
    }

    public SessionInventory loginByAccount(String accountName, String password) throws ApiSenderException {
        APILogInByAccountMsg msg = new APILogInByAccountMsg();
        msg.setAccountName(accountName);
        msg.setPassword(password);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APILogInReply reply = sender.call(msg, APILogInReply.class);
        return reply.getInventory();
    }

    public SessionInventory loginByUserAccountName(String userName, String password, String accountName) throws ApiSenderException {
        APILogInByUserMsg msg = new APILogInByUserMsg();
        msg.setAccountName(accountName);
        msg.setUserName(userName);
        msg.setPassword(password);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APILogInReply reply = sender.call(msg, APILogInReply.class);
        return reply.getInventory();
    }

    public SessionInventory loginByUser(String userName, String password, String accountUuid) throws ApiSenderException {
        APILogInByUserMsg msg = new APILogInByUserMsg();
        msg.setAccountUuid(accountUuid);
        msg.setUserName(userName);
        msg.setPassword(password);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APILogInReply reply = sender.call(msg, APILogInReply.class);
        return reply.getInventory();
    }

    public void logout(String sessionUuid) throws ApiSenderException {
        APILogOutMsg msg = new APILogOutMsg();
        msg.setSessionUuid(sessionUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.call(msg, APILogOutReply.class);
    }

    public boolean validateSession(String sessionUuid) throws ApiSenderException {
        APIValidateSessionMsg msg = new APIValidateSessionMsg();
        msg.setSessionUuid(sessionUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIValidateSessionReply reply = sender.call(msg, APIValidateSessionReply.class);
        return reply.isValidSession();
    }

    public AccountInventory createAccount(String name, String password) throws ApiSenderException {
        APICreateAccountMsg msg = new APICreateAccountMsg();
        msg.setSession(adminSession);
        msg.setName(name);
        msg.setPassword(password);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateAccountEvent evt = sender.send(msg, APICreateAccountEvent.class);
        return evt.getInventory();
    }

    public QuotaInventory updateQuota(String identityUuid, String name, long value) throws ApiSenderException {
        APIUpdateQuotaMsg msg = new APIUpdateQuotaMsg();
        msg.setIdentityUuid(identityUuid);
        msg.setName(name);
        msg.setValue(value);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateQuotaEvent evt = sender.send(msg, APIUpdateQuotaEvent.class);
        return evt.getInventory();
    }

    public QuotaInventory getQuota(String name, String accountUuid, SessionInventory session) throws ApiSenderException {
        APIQueryQuotaMsg msg = new APIQueryQuotaMsg();
        msg.addQueryCondition("name", QueryOp.EQ, name);
        msg.addQueryCondition("identityUuid", QueryOp.EQ, accountUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIQueryQuotaReply r = sender.call(msg, APIQueryQuotaReply.class);
        return r.getInventories().isEmpty() ? null : r.getInventories().get(0);
    }

    public List<Quota.QuotaUsage> getQuotaUsage(String accountUuid, SessionInventory session) throws ApiSenderException {
        APIGetAccountQuotaUsageMsg msg = new APIGetAccountQuotaUsageMsg();
        if (accountUuid != null) {
            msg.setUuid(accountUuid);
            msg.setSession(adminSession);
        } else {
            msg.setUuid(session.getAccountUuid());
            msg.setSession(session);
        }

        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetAccountQuotaUsageReply reply = sender.call(msg, APIGetAccountQuotaUsageReply.class);
        return reply.getUsages();
    }

    public List<ManagementNodeInventory> listManagementNodes() throws ApiSenderException {
        APIListManagementNodeMsg msg = new APIListManagementNodeMsg();
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListManagementNodeReply reply = sender.call(msg, APIListManagementNodeReply.class);
        return reply.getInventories();
    }

    public List<AccountInventory> listAccount(List<String> uuids) throws ApiSenderException {
        APIListAccountMsg msg = new APIListAccountMsg(uuids);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListAccountReply reply = sender.call(msg, APIListAccountReply.class);
        return reply.getInventories();
    }

    public AccountInventory resetAccountPassword(String uuid, String password, SessionInventory session) throws ApiSenderException {
        APIUpdateAccountMsg msg = new APIUpdateAccountMsg();
        msg.setSession(session);
        msg.setPassword(password);
        msg.setUuid(uuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateAccountEvent evt = sender.send(msg, APIUpdateAccountEvent.class);
        return evt.getInventory();
    }

    public UserInventory createUser(String accountUuid, String userName, String password, SessionInventory session) throws ApiSenderException {
        APICreateUserMsg msg = new APICreateUserMsg();
        msg.setSession(session);
        msg.setName(userName);
        msg.setPassword(password);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateUserEvent evt = sender.send(msg, APICreateUserEvent.class);
        return evt.getInventory();
    }

    public void resetUserPassword(String uuid, String password, SessionInventory session) throws ApiSenderException {
        APIUpdateUserMsg msg = new APIUpdateUserMsg();
        msg.setUuid(uuid);
        msg.setPassword(password);
        msg.setSession(session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIUpdateUserEvent.class);
    }

    public PolicyInventory createPolicy(String name, List<Statement> s, SessionInventory session) throws ApiSenderException {
        APICreatePolicyMsg msg = new APICreatePolicyMsg();
        msg.setSession(session);
        msg.setName(name);
        msg.setStatements(s);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreatePolicyEvent evt = sender.send(msg, APICreatePolicyEvent.class);
        return evt.getInventory();
    }

    public void attachPolicyToUser(String userUuid, String policyUuid, SessionInventory session) throws ApiSenderException {
        APIAttachPolicyToUserMsg msg = new APIAttachPolicyToUserMsg();
        msg.setSession(session);
        msg.setUserUuid(userUuid);
        msg.setPolicyUuid(policyUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIAttachPolicyToUserEvent.class);
    }

    public void attachPolicesToUser(String userUuid, List<String> puuids, SessionInventory session) throws ApiSenderException {
        APIAttachPoliciesToUserMsg msg = new APIAttachPoliciesToUserMsg();
        msg.setSession(session);
        msg.setUserUuid(userUuid);
        msg.setPolicyUuids(puuids);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIAttachPoliciesToUserEvent.class);
    }

    public void detachPolicyFromUser(String userUuid, String policyUuid, SessionInventory session) throws ApiSenderException {
        APIDetachPolicyFromUserMsg msg = new APIDetachPolicyFromUserMsg();
        msg.setSession(session);
        msg.setUserUuid(userUuid);
        msg.setPolicyUuid(policyUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDetachPolicyFromUserEvent.class);
    }

    public void attachPolicyToUser(String accountUuid, String userUuid, String policyUuid, SessionInventory session) throws ApiSenderException {
        APIAttachPolicyToUserMsg msg = new APIAttachPolicyToUserMsg();
        msg.setSession(session);
        msg.setUserUuid(userUuid);
        msg.setPolicyUuid(policyUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIAttachPolicyToUserEvent.class);
    }

    public void detachPoliciesFromUser(String userUuid, List<String> puuids, SessionInventory session) throws ApiSenderException {
        APIDetachPoliciesFromUserMsg msg = new APIDetachPoliciesFromUserMsg();
        msg.setSession(session);
        msg.setUserUuid(userUuid);
        msg.setPolicyUuids(puuids);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDetachPoliciesFromUserEvent.class);
    }

    public UserGroupInventory createGroup(String accountUuid, String name, SessionInventory session) throws ApiSenderException {
        APICreateUserGroupMsg msg = new APICreateUserGroupMsg();
        msg.setSession(session);
        msg.setName(name);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateUserGroupEvent evt = sender.send(msg, APICreateUserGroupEvent.class);
        return evt.getInventory();
    }

    public void deleteGroup(String uuid, SessionInventory session) throws ApiSenderException {
        APIDeleteUserGroupMsg msg = new APIDeleteUserGroupMsg();
        msg.setSession(session);
        msg.setUuid(uuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteUserGroupEvent.class);
    }

    public void deleteAccount(String uuid, SessionInventory session) throws ApiSenderException {
        APIDeleteAccountMsg msg = new APIDeleteAccountMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(uuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteAccountEvent.class);
    }

    public void deleteUser(String uuid, SessionInventory session) throws ApiSenderException {
        APIDeleteUserMsg msg = new APIDeleteUserMsg();
        msg.setSession(session);
        msg.setUuid(uuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteUserEvent.class);
    }

    public void deletePolicy(String uuid, SessionInventory session) throws ApiSenderException {
        APIDeletePolicyMsg msg = new APIDeletePolicyMsg();
        msg.setSession(session);
        msg.setUuid(uuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeletePolicyEvent.class);
    }

    public void attachPolicyToGroup(String groupUuid, String policyUuid, SessionInventory session) throws ApiSenderException {
        APIAttachPolicyToUserGroupMsg msg = new APIAttachPolicyToUserGroupMsg();
        msg.setGroupUuid(groupUuid);
        msg.setPolicyUuid(policyUuid);
        msg.setSession(session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIAttachPolicyToUserGroupEvent.class);
    }

    public void detachPolicyFromGroup(String groupUuid, String policyUuid, SessionInventory session) throws ApiSenderException {
        APIDetachPolicyFromUserGroupMsg msg = new APIDetachPolicyFromUserGroupMsg();
        msg.setGroupUuid(groupUuid);
        msg.setPolicyUuid(policyUuid);
        msg.setSession(session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDetachPolicyFromUserGroupEvent.class);
    }

    public void attachPolicyToGroup(String accountUuid, String groupUuid, String policyUuid, SessionInventory session) throws ApiSenderException {
        APIAttachPolicyToUserGroupMsg msg = new APIAttachPolicyToUserGroupMsg();
        msg.setGroupUuid(groupUuid);
        msg.setPolicyUuid(policyUuid);
        msg.setSession(session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIAttachPolicyToUserGroupEvent.class);
    }

    public void addUserToGroup(String userUuid, String groupUuid, SessionInventory session) throws ApiSenderException {
        APIAddUserToGroupMsg msg = new APIAddUserToGroupMsg();
        msg.setUserUuid(userUuid);
        msg.setGroupUuid(groupUuid);
        msg.setSession(session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIAddUserToGroupEvent.class);
    }

    public void removeUserFromGroup(String userUuid, String groupUuid, SessionInventory session) throws ApiSenderException {
        APIRemoveUserFromGroupMsg msg = new APIRemoveUserFromGroupMsg();
        msg.setUserUuid(userUuid);
        msg.setGroupUuid(groupUuid);
        msg.setSession(session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIRemoveUserFromGroupEvent.class);
    }

    public void attachUserToGroup(String accountUuid, String userUuid, String groupUuid, SessionInventory session) throws ApiSenderException {
        APIAddUserToGroupMsg msg = new APIAddUserToGroupMsg();
        msg.setUserUuid(userUuid);
        msg.setGroupUuid(groupUuid);
        msg.setSession(session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIAddUserToGroupEvent.class);
    }

    public void deleteAllIndex() throws ApiSenderException {
        APIDeleteSearchIndexMsg msg = new APIDeleteSearchIndexMsg();
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteSearchIndexEvent.class);
    }

    public void generateInventoryQueryDetails() throws ApiSenderException {
        APIGenerateInventoryQueryDetailsMsg msg = new APIGenerateInventoryQueryDetailsMsg();
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIGenerateInventoryQueryDetailsEvent.class);
    }

    public void generateSqlTrigger() throws ApiSenderException {
        APISearchGenerateSqlTriggerMsg msg = new APISearchGenerateSqlTriggerMsg();
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APISearchGenerateSqlTriggerEvent.class);
    }

    public String search(APISearchMessage msg) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        if (msg.getSession() == null) {
            msg.setSession(adminSession);
        }
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        sender.setTimeout(timeout);
        APISearchReply reply = sender.call(msg, APISearchReply.class);
        return reply.getContent();
    }

    public <T> T query(APIQueryMessage msg, Class<T> replyClass) throws ApiSenderException {
        return query(msg, replyClass, adminSession);
    }

    public <T> T query(APIQueryMessage msg, Class<T> replyClass, SessionInventory session) throws ApiSenderException {
        msg.setSession(session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIReply reply = (APIReply) sender.call(msg, APIReply.class);
        return (T) reply;
    }

    public long queryCount(APIQueryMessage msg, SessionInventory session) throws ApiSenderException {
        msg.setCount(true);
        msg.setSession(session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIQueryReply reply = (APIQueryReply) sender.call(msg, APIReply.class);
        return reply.getTotal();
    }

    public void generateGroovyClass() throws ApiSenderException {
        APIGenerateGroovyClassMsg msg = new APIGenerateGroovyClassMsg();
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIGenerateGroovyClassEvent.class);
    }

    public void generateSqlForeignKey() throws ApiSenderException {
        APIGenerateSqlForeignKeyMsg msg = new APIGenerateSqlForeignKeyMsg();
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIGenerateSqlForeignKeyEvent.class);
    }

    public void generateSqlIndex() throws ApiSenderException {
        APIGenerateSqlIndexMsg msg = new APIGenerateSqlIndexMsg();
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.send(msg, APIGenerateSqlIndexEvent.class);
    }

    public void generateQueryableFields() throws ApiSenderException {
        APIGenerateQueryableFieldsMsg msg = new APIGenerateQueryableFieldsMsg();
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIGenerateQueryableFieldsEvent.class);
    }

    public void generateApiJsonTemplate() throws ApiSenderException {
        APIGenerateApiJsonTemplateMsg msg = new APIGenerateApiJsonTemplateMsg();
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIGenerateApiJsonTemplateEvent.class);
    }

    public void generateTestLinkDocument() throws ApiSenderException {
        APIGenerateTestLinkDocumentMsg msg = new APIGenerateTestLinkDocumentMsg();
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIGenerateTestLinkDocumentEvent.class);
    }

    public L3NetworkInventory attachNetworkServiceToL3Network(String l3NetworkUuid, String providerUuid, List<String> types) throws ApiSenderException {
        return attachNetworkServiceToL3Network(l3NetworkUuid, providerUuid, types, null);
    }

    public L3NetworkInventory attachNetworkServiceToL3Network(String l3NetworkUuid, String providerUuid, List<String> types, SessionInventory session) throws ApiSenderException {
        APIAttachNetworkServiceToL3NetworkMsg msg = new APIAttachNetworkServiceToL3NetworkMsg();
        Map<String, List<String>> ntypes = new HashMap<String, List<String>>(1);
        ntypes.put(providerUuid, types);
        msg.setL3NetworkUuid(l3NetworkUuid);
        msg.setNetworkServices(ntypes);
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAttachNetworkServiceToL3NetworkEvent evt = sender.send(msg, APIAttachNetworkServiceToL3NetworkEvent.class);
        return evt.getInventory();
    }

    public List<NetworkServiceProviderInventory> listNetworkServiceProvider(List<String> uuids) throws ApiSenderException {
        APIListNetworkServiceProviderMsg msg = new APIListNetworkServiceProviderMsg();
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListNetworkServiceProviderReply reply = sender.call(msg, APIListNetworkServiceProviderReply.class);
        return reply.getInventories();
    }

    public L3NetworkInventory addDns(String l3NetworkUuid, String dns) throws ApiSenderException {
        return addDns(l3NetworkUuid, dns, null);
    }

    public L3NetworkInventory addDns(String l3NetworkUuid, String dns, SessionInventory session) throws ApiSenderException {
        APIAddDnsToL3NetworkMsg msg = new APIAddDnsToL3NetworkMsg();
        msg.setL3NetworkUuid(l3NetworkUuid);
        msg.setDns(dns);
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAddDnsToL3NetworkEvent evt = sender.send(msg, APIAddDnsToL3NetworkEvent.class);
        return evt.getInventory();
    }

    public String getInventory(APIGetMessage msg) throws ApiSenderException {
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetReply reply = sender.call(msg, APIGetReply.class);
        return reply.getInventory();
    }

    public APIGetCpuMemoryCapacityReply retrieveHostCapacityByAll() throws ApiSenderException {
        APIGetCpuMemoryCapacityMsg msg = new APIGetCpuMemoryCapacityMsg();
        msg.setSession(adminSession);
        msg.setAll(true);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        return sender.call(msg, APIGetCpuMemoryCapacityReply.class);
    }

    public APIGetCpuMemoryCapacityReply retrieveHostCapacity(List<String> zoneUuids, List<String> clusterUuids, List<String> hostUuids) throws ApiSenderException {
        APIGetCpuMemoryCapacityMsg msg = new APIGetCpuMemoryCapacityMsg();
        msg.setSession(adminSession);
        msg.setHostUuids(hostUuids);
        msg.setClusterUuids(clusterUuids);
        msg.setZoneUuids(zoneUuids);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        return sender.call(msg, APIGetCpuMemoryCapacityReply.class);
    }

    public SecurityGroupInventory createSecurityGroup(String name) throws ApiSenderException {
        return createSecurityGroup(name, null);
    }

    public SecurityGroupInventory createSecurityGroup(String name, SessionInventory session) throws ApiSenderException {
        APICreateSecurityGroupMsg msg = new APICreateSecurityGroupMsg();
        msg.setName(name);
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateSecurityGroupEvent evt = sender.send(msg, APICreateSecurityGroupEvent.class);
        return evt.getInventory();
    }

    public SecurityGroupInventory changeSecurityGroupState(String uuid, SecurityGroupStateEvent sevt) throws ApiSenderException {
        return changeSecurityGroupState(uuid, sevt, null);
    }

    public SecurityGroupInventory changeSecurityGroupState(String uuid, SecurityGroupStateEvent sevt, SessionInventory session) throws ApiSenderException {
        APIChangeSecurityGroupStateMsg msg = new APIChangeSecurityGroupStateMsg();
        msg.setStateEvent(sevt.toString());
        msg.setUuid(uuid);
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeSecurityGroupStateEvent evt = sender.send(msg, APIChangeSecurityGroupStateEvent.class);
        return evt.getInventory();
    }

    public SecurityGroupInventory createSecurityGroupByFullConfig(SecurityGroupInventory inv, SessionInventory session) throws ApiSenderException {
        APICreateSecurityGroupMsg msg = new APICreateSecurityGroupMsg();
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setSession(session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateSecurityGroupEvent evt = sender.send(msg, APICreateSecurityGroupEvent.class);
        return evt.getInventory();
    }

    public SecurityGroupInventory createSecurityGroupByFullConfig(SecurityGroupInventory inv) throws ApiSenderException {
        return createSecurityGroupByFullConfig(inv, adminSession);
    }

    public SecurityGroupInventory addSecurityGroupRuleByFullConfig(String securityGroupUuid, SecurityGroupRuleAO ao) throws ApiSenderException {
        List<SecurityGroupRuleAO> aos = new ArrayList<SecurityGroupRuleAO>(1);
        aos.add(ao);
        return addSecurityGroupRuleByFullConfig(securityGroupUuid, aos);
    }

    public SecurityGroupInventory addSecurityGroupRuleByFullConfig(String securityGroupUuid, List<SecurityGroupRuleAO> aos) throws ApiSenderException {
        return addSecurityGroupRuleByFullConfig(securityGroupUuid, aos, adminSession);
    }

    public List<VmNicInventory> getCandidateVmNicFromSecurityGroup(String sgUuid) throws ApiSenderException {
        return getCandidateVmNicFromSecurityGroup(sgUuid, null);
    }

    public List<VmNicInventory> getCandidateVmNicFromSecurityGroup(String sgUuid, SessionInventory session) throws ApiSenderException {
        APIGetCandidateVmNicForSecurityGroupMsg msg = new APIGetCandidateVmNicForSecurityGroupMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSecurityGroupUuid(sgUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetCandidateVmNicForSecurityGroupReply reply = sender.call(msg, APIGetCandidateVmNicForSecurityGroupReply.class);
        return reply.getInventories();
    }

    public SecurityGroupInventory addSecurityGroupRuleByFullConfig(String securityGroupUuid, List<SecurityGroupRuleAO> aos, SessionInventory session)
            throws ApiSenderException {
        APIAddSecurityGroupRuleMsg msg = new APIAddSecurityGroupRuleMsg();
        msg.setRules(aos);
        msg.setSecurityGroupUuid(securityGroupUuid);
        msg.setSession(session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAddSecurityGroupRuleEvent evt = sender.send(msg, APIAddSecurityGroupRuleEvent.class);
        return evt.getInventory();
    }

    public SecurityGroupInventory removeSecurityGroupRule(String ruleUuid) throws ApiSenderException {
        List<String> ruleUuids = new ArrayList<String>();
        ruleUuids.add(ruleUuid);
        return removeSecurityGroupRule(ruleUuids);
    }

    public SecurityGroupInventory removeSecurityGroupRule(List<String> ruleUuids) throws ApiSenderException {
        return removeSecurityGroupRule(ruleUuids, null);
    }

    public SecurityGroupInventory removeSecurityGroupRule(List<String> ruleUuids, SessionInventory session) throws ApiSenderException {
        APIDeleteSecurityGroupRuleMsg msg = new APIDeleteSecurityGroupRuleMsg();
        msg.setRuleUuids(ruleUuids);
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteSecurityGroupRuleEvent evt = sender.send(msg, APIDeleteSecurityGroupRuleEvent.class);
        return evt.getInventory();
    }

    public void removeVmNicFromSecurityGroup(String securityGroupUuid, String vmNicUuid) throws ApiSenderException {
        removeVmNicFromSecurityGroup(securityGroupUuid, vmNicUuid, null);
    }

    public void removeVmNicFromSecurityGroup(String securityGroupUuid, String vmNicUuid, SessionInventory session) throws ApiSenderException {
        APIDeleteVmNicFromSecurityGroupMsg msg = new APIDeleteVmNicFromSecurityGroupMsg();
        msg.setSecurityGroupUuid(securityGroupUuid);
        msg.setVmNicUuids(Arrays.asList(vmNicUuid));
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteVmNicFromSecurityGroupEvent.class);
    }

    public void addVmNicToSecurityGroup(String securityGroupUuid, String vmNicUuid) throws ApiSenderException {
        List<String> nicUuids = new ArrayList<String>();
        nicUuids.add(vmNicUuid);
        addVmNicToSecurityGroup(securityGroupUuid, nicUuids);
    }

    public void addVmNicToSecurityGroup(String securityGroupUuid, List<String> vmNicUuids) throws ApiSenderException {
        addVmNicToSecurityGroup(securityGroupUuid, vmNicUuids, null);
    }

    public void addVmNicToSecurityGroup(String securityGroupUuid, List<String> vmNicUuids, SessionInventory session) throws ApiSenderException {
        APIAddVmNicToSecurityGroupMsg msg = new APIAddVmNicToSecurityGroupMsg();
        msg.setSecurityGroupUuid(securityGroupUuid);
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setVmNicUuids(vmNicUuids);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIAddVmNicToSecurityGroupEvent.class);
    }

    public SecurityGroupInventory attachSecurityGroupToL3Network(String securityGroupUuid, String l3NetworkUuid) throws ApiSenderException {
        return attachSecurityGroupToL3Network(securityGroupUuid, l3NetworkUuid, null);
    }

    public SecurityGroupInventory attachSecurityGroupToL3Network(String securityGroupUuid, String l3NetworkUuid, SessionInventory session) throws ApiSenderException {
        APIAttachSecurityGroupToL3NetworkMsg msg = new APIAttachSecurityGroupToL3NetworkMsg();
        msg.setL3NetworkUuid(l3NetworkUuid);
        msg.setSecurityGroupUuid(securityGroupUuid);
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAttachSecurityGroupToL3NetworkEvent evt = sender.send(msg, APIAttachSecurityGroupToL3NetworkEvent.class);
        return evt.getInventory();
    }

    public SecurityGroupInventory detachSecurityGroupFromL3Network(String securityGroupUuid, String l3NetworkUuid) throws ApiSenderException {
        return detachSecurityGroupFromL3Network(securityGroupUuid, l3NetworkUuid, null);
    }

    public SecurityGroupInventory detachSecurityGroupFromL3Network(String securityGroupUuid, String l3NetworkUuid, SessionInventory session) throws ApiSenderException {
        APIDetachSecurityGroupFromL3NetworkMsg msg = new APIDetachSecurityGroupFromL3NetworkMsg();
        msg.setL3NetworkUuid(l3NetworkUuid);
        msg.setSecurityGroupUuid(securityGroupUuid);
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDetachSecurityGroupFromL3NetworkEvent evt = sender.send(msg, APIDetachSecurityGroupFromL3NetworkEvent.class);
        return evt.getInventory();
    }

    public void deleteSecurityGroup(String securityGroupUuid) throws ApiSenderException {
        deleteSecurityGroup(securityGroupUuid, null);
    }

    public void deleteSecurityGroup(String securityGroupUuid, SessionInventory session) throws ApiSenderException {
        APIDeleteSecurityGroupMsg msg = new APIDeleteSecurityGroupMsg();
        msg.setUuid(securityGroupUuid);
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteSecurityGroupEvent.class);
    }

    public void reconnectPrimaryStorage(String psUuid) throws ApiSenderException {
        APIReconnectPrimaryStorageMsg msg = new APIReconnectPrimaryStorageMsg();
        msg.setUuid(psUuid);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIReconnectPrimaryStorageEvent.class);
    }

    public void reconnectHost(String hostUuid) throws ApiSenderException {
        APIReconnectHostMsg msg = new APIReconnectHostMsg();
        msg.setUuid(hostUuid);
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIReconnectHostEvent.class);
    }

    public BackupStorageInventory reconnectBackupStorage(String bsUuid) throws ApiSenderException {
        APIReconnectBackupStorageMsg msg = new APIReconnectBackupStorageMsg();
        msg.setSession(adminSession);
        msg.setUuid(bsUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIReconnectBackupStorageEvent evt = sender.send(msg, APIReconnectBackupStorageEvent.class);
        return evt.getInventory();
    }

    public SftpBackupStorageInventory reconnectSftpBackupStorage(String bsUuid) throws ApiSenderException {
        APIReconnectSftpBackupStorageMsg msg = new APIReconnectSftpBackupStorageMsg();
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setUuid(bsUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIReconnectSftpBackupStorageEvent evt = sender.send(msg, APIReconnectSftpBackupStorageEvent.class);
        return evt.getInventory();
    }

    public ImageInventory createTemplateFromRootVolume(String name, String rootVolumeUuid, List<String> backupStorageUuids) throws ApiSenderException {
        return createTemplateFromRootVolume(name, rootVolumeUuid, backupStorageUuids, null);
    }

    public ImageInventory createTemplateFromRootVolume(String name, String rootVolumeUuid, List<String> backupStorageUuids, SessionInventory session) throws ApiSenderException {
        APICreateRootVolumeTemplateFromRootVolumeMsg msg = new APICreateRootVolumeTemplateFromRootVolumeMsg();
        msg.setName(name);
        msg.setBackupStorageUuids(backupStorageUuids);
        msg.setRootVolumeUuid(rootVolumeUuid);
        msg.setGuestOsType("test");
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateRootVolumeTemplateFromRootVolumeEvent evt = sender.send(msg, APICreateRootVolumeTemplateFromRootVolumeEvent.class);
        return evt.getInventory();
    }

    public ImageInventory createTemplateFromRootVolume(String name, String rootVolumeUuid, String backupStorageUuid) throws ApiSenderException {
        return createTemplateFromRootVolume(name, rootVolumeUuid, backupStorageUuid, null);
    }

    public ImageInventory createTemplateFromRootVolume(String name, String rootVolumeUuid, String backupStorageUuid, SessionInventory session) throws ApiSenderException {
        return createTemplateFromRootVolume(name, rootVolumeUuid, Arrays.asList(backupStorageUuid), session);
    }

    public List<VmNicInventory> listVmNic(List<String> uuids) throws ApiSenderException {
        APIListVmNicMsg msg = new APIListVmNicMsg();
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setUuids(uuids);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListVmNicReply reply = sender.call(msg, APIListVmNicReply.class);
        return reply.getInventories();
    }

    public List<SecurityGroupInventory> listSecurityGroup(List<String> uuids) throws ApiSenderException {
        APIListSecurityGroupMsg msg = new APIListSecurityGroupMsg();
        msg.setSession(adminSession);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setUuids(uuids);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListSecurityGroupReply reply = sender.call(msg, APIListSecurityGroupReply.class);
        return reply.getInventories();
    }

    public void removeVmFromSimulatorHost(String hostUuid, String vmUuid) {
        RemoveVmOnSimulatorMsg msg = new RemoveVmOnSimulatorMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmUuid(vmUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.call(msg);
    }

    public void changeVmStateOnSimulatorHost(String hostUuid, String vmUuid, VmInstanceState state) {
        ChangeVmStateOnSimulatorHostMsg msg = new ChangeVmStateOnSimulatorHostMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmState(state.toString());
        msg.setVmUuid(vmUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.call(msg);
    }


    public VipInventory acquireIp(String l3NetworkUuid, String requiredIp) throws ApiSenderException {
        return acquireIp(l3NetworkUuid, requiredIp, null);
    }

    public VipInventory acquireIp(String l3NetworkUuid, String requiredIp, SessionInventory session) throws ApiSenderException {
        APICreateVipMsg msg = new APICreateVipMsg();
        msg.setName("vip");
        msg.setL3NetworkUuid(l3NetworkUuid);
        msg.setSession(session == null ? adminSession : session);
        msg.setRequiredIp(requiredIp);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateVipEvent evt = sender.send(msg, APICreateVipEvent.class);
        return evt.getInventory();
    }

    public VipInventory acquireIp(String l3NetworkUuid) throws ApiSenderException {
        return acquireIp(l3NetworkUuid, (SessionInventory) null);
    }

    public VipInventory acquireIp(String l3NetworkUuid, SessionInventory session) throws ApiSenderException {
        return acquireIp(l3NetworkUuid, null, session);
    }

    public VipInventory changeVipSate(String uuid, VipStateEvent sevt) throws ApiSenderException {
        return changeVipSate(uuid, sevt, null);
    }

    public VipInventory changeVipSate(String uuid, VipStateEvent sevt, SessionInventory session) throws ApiSenderException {
        APIChangeVipStateMsg msg = new APIChangeVipStateMsg();
        msg.setStateEvent(sevt.toString());
        msg.setUuid(uuid);
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeVipStateEvent evt = sender.send(msg, APIChangeVipStateEvent.class);
        return evt.getInventory();
    }

    public void releaseIp(String ipUuid) throws ApiSenderException {
        releaseIp(ipUuid, null);
    }

    public void releaseIp(String ipUuid, SessionInventory session) throws ApiSenderException {
        APIDeleteVipMsg msg = new APIDeleteVipMsg();
        msg.setUuid(ipUuid);
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteVipEvent.class);
    }

    public PortForwardingRuleInventory changePortForwardingRuleState(String uuid, PortForwardingRuleStateEvent sevt) throws ApiSenderException {
        return changePortForwardingRuleState(uuid, sevt, null);
    }

    public PortForwardingRuleInventory changePortForwardingRuleState(String uuid, PortForwardingRuleStateEvent sevt, SessionInventory session) throws ApiSenderException {
        APIChangePortForwardingRuleStateMsg msg = new APIChangePortForwardingRuleStateMsg();
        msg.setUuid(uuid);
        msg.setStateEvent(sevt.toString());
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangePortForwardingRuleStateEvent evt = sender.send(msg, APIChangePortForwardingRuleStateEvent.class);
        return evt.getInventory();
    }

    public PortForwardingRuleInventory createPortForwardingRuleByFullConfig(PortForwardingRuleInventory rule) throws ApiSenderException {
        return createPortForwardingRuleByFullConfig(rule, null);
    }

    public PortForwardingRuleInventory createPortForwardingRuleByFullConfig(PortForwardingRuleInventory rule, SessionInventory session) throws ApiSenderException {
        APICreatePortForwardingRuleMsg msg = new APICreatePortForwardingRuleMsg();
        msg.setName(rule.getName());
        msg.setDescription(rule.getDescription());
        msg.setAllowedCidr(rule.getAllowedCidr());
        msg.setPrivatePortEnd(rule.getPrivatePortEnd());
        msg.setPrivatePortStart(rule.getPrivatePortStart());
        msg.setVipUuid(rule.getVipUuid());
        msg.setVipPortEnd(rule.getVipPortEnd());
        msg.setVipPortStart(rule.getVipPortStart());
        msg.setVmNicUuid(rule.getVmNicUuid());
        msg.setProtocolType(rule.getProtocolType());
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreatePortForwardingRuleEvent evt = sender.send(msg, APICreatePortForwardingRuleEvent.class);
        return evt.getInventory();
    }

    public void revokePortForwardingRule(String ruleUuid) throws ApiSenderException {
        revokePortForwardingRule(ruleUuid, null);
    }

    public void revokePortForwardingRule(String ruleUuid, SessionInventory session) throws ApiSenderException {
        APIDeletePortForwardingRuleMsg msg = new APIDeletePortForwardingRuleMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setUuid(ruleUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeletePortForwardingRuleEvent.class);
    }

    public PortForwardingRuleInventory attachPortForwardingRule(String ruleUuid, String vmNicUuid) throws ApiSenderException {
        return attachPortForwardingRule(ruleUuid, vmNicUuid, null);
    }

    public PortForwardingRuleInventory attachPortForwardingRule(String ruleUuid, String vmNicUuid, SessionInventory session) throws ApiSenderException {
        APIAttachPortForwardingRuleMsg msg = new APIAttachPortForwardingRuleMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setRuleUuid(ruleUuid);
        msg.setVmNicUuid(vmNicUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAttachPortForwardingRuleEvent evt = sender.send(msg, APIAttachPortForwardingRuleEvent.class);
        return evt.getInventory();
    }

    public PortForwardingRuleInventory detachPortForwardingRule(String ruleUuid) throws ApiSenderException {
        return detachPortForwardingRule(ruleUuid, null);
    }

    public PortForwardingRuleInventory detachPortForwardingRule(String ruleUuid, SessionInventory session) throws ApiSenderException {
        APIDetachPortForwardingRuleMsg msg = new APIDetachPortForwardingRuleMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setUuid(ruleUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDetachPortForwardingRuleEvent evt = sender.send(msg, APIDetachPortForwardingRuleEvent.class);
        return evt.getInventory();
    }

    public List<PortForwardingRuleInventory> listPortForwardingRules(List<String> uuids) throws ApiSenderException {
        APIListPortForwardingRuleMsg msg = new APIListPortForwardingRuleMsg(uuids);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListPortForwardingRuleReply reply = sender.call(msg, APIListPortForwardingRuleReply.class);
        return reply.getInventories();
    }

    public List<VmNicInventory> getPortForwardingAttachableNics(String ruleUuid) throws ApiSenderException {
        APIGetPortForwardingAttachableVmNicsMsg msg = new APIGetPortForwardingAttachableVmNicsMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        msg.setRuleUuid(ruleUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetPortForwardingAttachableVmNicsReply reply = sender.call(msg, APIGetPortForwardingAttachableVmNicsReply.class);
        return reply.getInventories();
    }

    public List<VmNicSecurityGroupRefInventory> listVmNicSecurityGroupRef(List<String> uuids) throws ApiSenderException {
        APIListVmNicInSecurityGroupMsg msg = new APIListVmNicInSecurityGroupMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListVmNicInSecurityGroupReply reply = sender.call(msg, APIListVmNicInSecurityGroupReply.class);
        return reply.getInventories();
    }

    public List<String> getHypervisorTypes() throws ApiSenderException {
        APIGetHypervisorTypesMsg msg = new APIGetHypervisorTypesMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetHypervisorTypesReply reply = sender.call(msg, APIGetHypervisorTypesReply.class);
        return reply.getHypervisorTypes();
    }

    public Map<String, List<String>> getNetworkServiceTypes() throws ApiSenderException {
        APIGetNetworkServiceTypesMsg msg = new APIGetNetworkServiceTypesMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetNetworkServiceTypesReply reply = sender.call(msg, APIGetNetworkServiceTypesReply.class);
        return reply.getServiceAndProviderTypes();
    }

    public List<String> getL2NetworkTypes() throws ApiSenderException {
        APIGetL2NetworkTypesMsg msg = new APIGetL2NetworkTypesMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetL2NetworkTypesReply reply = sender.call(msg, APIGetL2NetworkTypesReply.class);
        return reply.getL2NetworkTypes();
    }

    public List<String> getL3NetworkTypes() throws ApiSenderException {
        return getL3NetworkTypes(null);
    }

    public List<String> getL3NetworkTypes(SessionInventory session) throws ApiSenderException {
        APIGetL3NetworkTypesMsg msg = new APIGetL3NetworkTypesMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetL3NetworkTypesReply reply = sender.call(msg, APIGetL3NetworkTypesReply.class);
        return reply.getL3NetworkTypes();
    }

    public List<String> getPrimaryStorageTypes() throws ApiSenderException {
        APIGetPrimaryStorageTypesMsg msg = new APIGetPrimaryStorageTypesMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetPrimaryStorageTypesReply reply = sender.call(msg, APIGetPrimaryStorageTypesReply.class);
        return reply.getPrimaryStorageTypes();
    }

    public List<String> getBackupStorageTypes() throws ApiSenderException {
        APIGetBackupStorageTypesMsg msg = new APIGetBackupStorageTypesMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetBackupStorageTypesReply reply = sender.call(msg, APIGetBackupStorageTypesReply.class);
        return reply.getBackupStorageTypes();
    }

    public ImageInventory changeImageState(String uuid, ImageStateEvent evt, SessionInventory session) throws ApiSenderException {
        APIChangeImageStateMsg msg = new APIChangeImageStateMsg();
        msg.setUuid(uuid);
        msg.setStateEvent(evt.toString());
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeImageStateEvent revt = sender.send(msg, APIChangeImageStateEvent.class);
        return revt.getInventory();
    }

    public ImageInventory changeImageState(String uuid, ImageStateEvent evt) throws ApiSenderException {
        return changeImageState(uuid, evt, null);
    }

    public List<String> getHostAllocatorStrategies() throws ApiSenderException {
        APIGetHostAllocatorStrategiesMsg msg = new APIGetHostAllocatorStrategiesMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetHostAllocatorStrategiesReply reply = sender.call(msg, APIGetHostAllocatorStrategiesReply.class);
        return reply.getHostAllocatorStrategies();
    }

    public List<String> getPrimaryStorageAllocatorStrategies() throws ApiSenderException {
        APIGetPrimaryStorageAllocatorStrategiesMsg msg = new APIGetPrimaryStorageAllocatorStrategiesMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetPrimaryStorageAllocatorStrategiesReply reply = sender.call(msg, APIGetPrimaryStorageAllocatorStrategiesReply.class);
        return reply.getPrimaryStorageAllocatorStrategies();
    }

    public DiskOfferingInventory changeDiskOfferingState(String uuid, DiskOfferingStateEvent sevt) throws ApiSenderException {
        return changeDiskOfferingState(uuid, sevt, null);
    }

    public DiskOfferingInventory changeDiskOfferingState(String uuid, DiskOfferingStateEvent sevt, SessionInventory session) throws ApiSenderException {
        APIChangeDiskOfferingStateMsg msg = new APIChangeDiskOfferingStateMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(uuid);
        msg.setStateEvent(sevt.toString());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeDiskOfferingStateEvent evt = sender.send(msg, APIChangeDiskOfferingStateEvent.class);
        return evt.getInventory();
    }

    public VmInstanceInventory attachNic(String vmUuid, String l3Uuid) throws ApiSenderException {
        return attachNic(vmUuid, l3Uuid, null);
    }

    public VmInstanceInventory attachNic(String vmUuid, String l3Uuid, String staticIp) throws ApiSenderException {
        APIAttachL3NetworkToVmMsg msg = new APIAttachL3NetworkToVmMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        msg.setStaticIp(staticIp);
        msg.setVmInstanceUuid(vmUuid);
        msg.setL3NetworkUuid(l3Uuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAttachL3NetworkToVmEvent evt = sender.send(msg, APIAttachL3NetworkToVmEvent.class);
        return evt.getInventory();
    }

    public List<L3NetworkInventory> getVmAttachableL3Networks(String vmUuid) throws ApiSenderException {
        return getVmAttachableL3Networks(vmUuid, null);
    }

    public List<L3NetworkInventory> getVmAttachableL3Networks(String vmUuid, SessionInventory session) throws ApiSenderException {
        APIGetVmAttachableL3NetworkMsg msg = new APIGetVmAttachableL3NetworkMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session == null ? adminSession : session);
        msg.setVmInstanceUuid(vmUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetVmAttachableL3NetworkReply reply = sender.call(msg, APIGetVmAttachableL3NetworkReply.class);
        return reply.getInventories();
    }

    public VmInstanceInventory detachNic(String nicUuid) throws ApiSenderException {
        return detachNic(nicUuid, null);
    }

    public VmInstanceInventory detachNic(String niUuid, SessionInventory session) throws ApiSenderException {
        APIDetachL3NetworkFromVmMsg msg = new APIDetachL3NetworkFromVmMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session == null ? adminSession : session);
        msg.setVmNicUuid(niUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDetachL3NetworkFromVmEvent evt = sender.send(msg, APIDetachL3NetworkFromVmEvent.class);
        return evt.getInventory();
    }

    public ConsoleInventory getConsole(String vmUuid) throws ApiSenderException {
        APIRequestConsoleAccessMsg msg = new APIRequestConsoleAccessMsg();
        msg.setVmInstanceUuid(vmUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIRequestConsoleAccessEvent evt = sender.send(msg, APIRequestConsoleAccessEvent.class);
        return evt.getInventory();
    }

    public EipInventory createEip(String name, String vipUuid, String vmNicUuid) throws ApiSenderException {
        return createEip(name, vipUuid, vmNicUuid, null);
    }

    public EipInventory createEip(String name, String vipUuid, String vmNicUuid, SessionInventory session) throws ApiSenderException {
        APICreateEipMsg msg = new APICreateEipMsg();
        msg.setName(name);
        msg.setDescription(name);
        msg.setVipUuid(vipUuid);
        msg.setVmNicUuid(vmNicUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateEipEvent evt = sender.send(msg, APICreateEipEvent.class);
        return evt.getInventory();
    }

    public void removeEip(String eipUuid) throws ApiSenderException {
        removeEip(eipUuid, null);
    }

    public void removeEip(String eipUuid, SessionInventory session) throws ApiSenderException {
        APIDeleteEipMsg msg = new APIDeleteEipMsg();
        msg.setUuid(eipUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteEipEvent.class);
    }

    public EipInventory attachEip(String eipUuid, String vmNicUuid) throws ApiSenderException {
        return attachEip(eipUuid, vmNicUuid, null);
    }

    public EipInventory attachEip(String eipUuid, String vmNicUuid, SessionInventory session) throws ApiSenderException {
        APIAttachEipMsg msg = new APIAttachEipMsg();
        msg.setVmNicUuid(vmNicUuid);
        msg.setEipUuid(eipUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAttachEipEvent evt = sender.send(msg, APIAttachEipEvent.class);
        return evt.getInventory();
    }

    public EipInventory changeEipState(String eipUuid, EipStateEvent sevt) throws ApiSenderException {
        return changeEipState(eipUuid, sevt, null);
    }

    public EipInventory changeEipState(String eipUuid, EipStateEvent sevt, SessionInventory session) throws ApiSenderException {
        APIChangeEipStateMsg msg = new APIChangeEipStateMsg();
        msg.setUuid(eipUuid);
        msg.setStateEvent(sevt.toString());
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeEipStateEvent evt = sender.send(msg, APIChangeEipStateEvent.class);
        return evt.getInventory();
    }

    public EipInventory detachEip(String eipUuid) throws ApiSenderException {
        return detachEip(eipUuid, null);
    }

    public EipInventory detachEip(String eipUuid, SessionInventory session) throws ApiSenderException {
        APIDetachEipMsg msg = new APIDetachEipMsg();
        msg.setUuid(eipUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDetachEipEvent evt = sender.send(msg, APIDetachEipEvent.class);
        return evt.getInventory();
    }

    public List<VmNicInventory> getEipAttachableVmNicsByEipUuid(String eipUuid) throws ApiSenderException {
        return getEipAttachableVmNics(eipUuid, null);
    }

    public List<VmNicInventory> getEipAttachableVmNicsByVipUuid(String vipUuid) throws ApiSenderException {
        return getEipAttachableVmNics(null, vipUuid);
    }

    private List<VmNicInventory> getEipAttachableVmNics(String eipUuid, String vipUuid) throws ApiSenderException {
        APIGetEipAttachableVmNicsMsg msg = new APIGetEipAttachableVmNicsMsg();
        msg.setEipUuid(eipUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setVipUuid(vipUuid);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetEipAttachableVmNicsReply reply = sender.call(msg, APIGetEipAttachableVmNicsReply.class);
        return reply.getInventories();
    }


    public List<VolumeSnapshotTreeInventory> getVolumeSnapshotTree(String treeUuid, String volumeUuid) throws ApiSenderException {
        APIGetVolumeSnapshotTreeMsg msg = new APIGetVolumeSnapshotTreeMsg();
        msg.setVolumeUuid(volumeUuid);
        msg.setTreeUuid(treeUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetVolumeSnapshotTreeReply reply = sender.call(msg, APIGetVolumeSnapshotTreeReply.class);
        return reply.getInventories();
    }

    public VolumeSnapshotInventory backupSnapshot(String snapshotUuid) throws ApiSenderException {
        return backupSnapshot(snapshotUuid, null, null);
    }

    public VolumeSnapshotInventory backupSnapshot(String snapshotUuid, String backupStorageUuid) throws ApiSenderException {
        return backupSnapshot(snapshotUuid, backupStorageUuid, null);
    }

    public VolumeSnapshotInventory backupSnapshot(String snapshotUuid, String backupStorageUuid, SessionInventory session) throws ApiSenderException {
        APIBackupVolumeSnapshotMsg msg = new APIBackupVolumeSnapshotMsg();
        msg.setUuid(snapshotUuid);
        msg.setBackupStorageUuid(backupStorageUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIBackupVolumeSnapshotEvent evt = sender.send(msg, APIBackupVolumeSnapshotEvent.class);
        return evt.getInventory();
    }

    public VolumeSnapshotInventory deleteSnapshotFromBackupStorage(String snapshotUuid, String... bsUuids) throws ApiSenderException {
        return deleteSnapshotFromBackupStorage(snapshotUuid, null, bsUuids);
    }

    public VolumeSnapshotInventory deleteSnapshotFromBackupStorage(String snapshotUuid, SessionInventory session, String... bsUuids) throws ApiSenderException {
        APIDeleteVolumeSnapshotFromBackupStorageMsg msg = new APIDeleteVolumeSnapshotFromBackupStorageMsg();
        msg.setUuid(snapshotUuid);
        if (bsUuids != null) {
            msg.setBackupStorageUuids(Arrays.asList(bsUuids));
        } else {
            msg.setBackupStorageUuids(new ArrayList<String>());
        }
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteVolumeSnapshotFromBackupStorageEvent evt = sender.send(msg, APIDeleteVolumeSnapshotFromBackupStorageEvent.class);
        return evt.getInventory();
    }

    private TagInventory createTag(String resourceUuid, String tag, Class entityClass, TagType type) throws ApiSenderException {
        return createTag(resourceUuid, tag, entityClass, type, null);
    }

    private TagInventory createTag(String resourceUuid, String tag, Class entityClass, TagType type, SessionInventory session) throws ApiSenderException {
        APICreateTagMsg msg;
        if (type == TagType.System) {
            msg = new APICreateSystemTagMsg();
        } else {
            msg = new APICreateUserTagMsg();
        }

        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session == null ? adminSession : session);
        msg.setResourceType(entityClass.getSimpleName());
        msg.setResourceUuid(resourceUuid);
        msg.setTag(tag);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        if (type == TagType.System) {
            APICreateSystemTagEvent evt = sender.send(msg, APICreateSystemTagEvent.class);
            return evt.getInventory();
        } else {
            APICreateUserTagEvent evt = sender.send(msg, APICreateUserTagEvent.class);
            return evt.getInventory();
        }
    }

    public TagInventory createUserTag(String resourceUuid, String tag, Class entitiClass) throws ApiSenderException {
        return createUserTag(resourceUuid, tag, entitiClass, null);
    }

    public TagInventory createUserTag(String resourceUuid, String tag, Class entitiClass, SessionInventory session) throws ApiSenderException {
        return createTag(resourceUuid, tag, entitiClass, TagType.User, session);
    }

    public TagInventory createSystemTag(String resourceUuid, String tag, Class entitiClass) throws ApiSenderException {
        return createSystemTag(resourceUuid, tag, entitiClass, null);
    }

    public TagInventory createSystemTag(String resourceUuid, String tag, Class entitiClass, SessionInventory session) throws ApiSenderException {
        return createTag(resourceUuid, tag, entitiClass, TagType.System, session);
    }

    public TagInventory updateSystemTag(String uuid, String tag, SessionInventory session) throws ApiSenderException {
        APIUpdateSystemTagMsg msg = new APIUpdateSystemTagMsg();
        msg.setUuid(uuid);
        msg.setTag(tag);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateSystemTagEvent evt = sender.send(msg, APIUpdateSystemTagEvent.class);
        return evt.getInventory();
    }

    public void deleteTag(String tagUuid) throws ApiSenderException {
        deleteTag(tagUuid, null);
    }

    public void deleteTag(String tagUuid, SessionInventory session) throws ApiSenderException {
        APIDeleteTagMsg msg = new APIDeleteTagMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(tagUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteTagEvent.class);
    }

    public VolumeInventory backupDataVolume(String volUuid, String backupStorgeUuid) throws ApiSenderException {
        APIBackupDataVolumeMsg msg = new APIBackupDataVolumeMsg();
        msg.setUuid(volUuid);
        msg.setBackupStorageUuid(backupStorgeUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIBackupDataVolumeEvent evt = sender.send(msg, APIBackupDataVolumeEvent.class);
        return evt.getInventory();
    }

    public void generateVOViewSql() throws ApiSenderException {
        APIGenerateSqlVOViewMsg msg = new APIGenerateSqlVOViewMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGenerateSqlVOViewEvent evt = sender.send(msg, APIGenerateSqlVOViewEvent.class);
    }

    public void generateTypeScript() throws ApiSenderException {
        APIGenerateApiTypeScriptDefinitionMsg msg = new APIGenerateApiTypeScriptDefinitionMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIGenerateApiTypeScriptDefinitionEvent.class);
    }

    public ApplianceVmInventory reconnectVirtualRouter(String uuid) throws ApiSenderException {
        APIReconnectVirtualRouterMsg msg = new APIReconnectVirtualRouterMsg();
        msg.setVmInstanceUuid(uuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIReconnectVirtualRouterEvent evt = sender.send(msg, APIReconnectVirtualRouterEvent.class);
        return evt.getInventory();
    }

    @Override
    public boolean handleEvent(Event e) {
        logger.trace(JSONObjectUtil.toJsonString(e));
        return false;
    }

    public SessionInventory getAdminSession() {
        return adminSession;
    }

    public void setAdminSession(SessionInventory adminSession) {
        this.adminSession = adminSession;
    }

    public List<ApplianceVmInventory> listApplianceVm() throws ApiSenderException {
        APIListApplianceVmMsg msg = new APIListApplianceVmMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIListApplianceVmReply reply = sender.call(msg, APIListApplianceVmReply.class);
        return reply.getInventories();
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public static ComponentLoader getLoader() {
        return loader;
    }

    public ZoneInventory updateZone(ZoneInventory inv) throws ApiSenderException {
        APIUpdateZoneMsg msg = new APIUpdateZoneMsg();
        msg.setSession(adminSession);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateZoneEvent evt = sender.send(msg, APIUpdateZoneEvent.class);
        return evt.getInventory();
    }

    public ClusterInventory updateCluster(ClusterInventory inv) throws ApiSenderException {
        APIUpdateClusterMsg msg = new APIUpdateClusterMsg();
        msg.setSession(adminSession);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateClusterEvent evt = sender.send(msg, APIUpdateClusterEvent.class);
        return evt.getInventory();
    }

    public HostInventory updateHost(HostInventory inv) throws ApiSenderException {
        APIUpdateHostMsg msg = new APIUpdateHostMsg();
        msg.setSession(adminSession);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateHostEvent evt = sender.send(msg, APIUpdateHostEvent.class);
        return evt.getInventory();
    }

    public VmInstanceInventory updateVm(VmInstanceInventory inv) throws ApiSenderException {
        APIUpdateVmInstanceMsg msg = new APIUpdateVmInstanceMsg();
        msg.setSession(adminSession);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        msg.setState(inv.getState());
        msg.setDefaultL3NetworkUuid(inv.getDefaultL3NetworkUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateVmInstanceEvent evt = sender.send(msg, APIUpdateVmInstanceEvent.class);
        return evt.getInventory();
    }

    public PrimaryStorageInventory updatePrimaryStorage(PrimaryStorageInventory inv) throws ApiSenderException {
        APIUpdatePrimaryStorageMsg msg = new APIUpdatePrimaryStorageMsg();
        msg.setSession(adminSession);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        msg.setUrl(inv.getUrl());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdatePrimaryStorageEvent evt = sender.send(msg, APIUpdatePrimaryStorageEvent.class);
        return evt.getInventory();
    }

    public BackupStorageInventory updateSftpBackupStorage(SftpBackupStorageInventory inv, String password) throws ApiSenderException {
        APIUpdateSftpBackupStorageMsg msg = new APIUpdateSftpBackupStorageMsg();
        msg.setSession(adminSession);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        msg.setUsername(inv.getUsername());
        msg.setPassword(password);
        msg.setSshPort(inv.getSshPort());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateBackupStorageEvent evt = sender.send(msg, APIUpdateBackupStorageEvent.class);
        return evt.getInventory();
    }

    public VolumeInventory updateVolume(VolumeInventory inv) throws ApiSenderException {
        return updateVolume(inv, null);
    }

    public VolumeInventory updateVolume(VolumeInventory inv, SessionInventory session) throws ApiSenderException {
        APIUpdateVolumeMsg msg = new APIUpdateVolumeMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateVolumeEvent evt = sender.send(msg, APIUpdateVolumeEvent.class);
        return evt.getInventory();
    }

    public VolumeSnapshotInventory updateVolumeSnapshot(VolumeSnapshotInventory inv) throws ApiSenderException {
        return updateVolumeSnapshot(inv, null);
    }

    public VolumeSnapshotInventory updateVolumeSnapshot(VolumeSnapshotInventory inv, SessionInventory session) throws ApiSenderException {
        APIUpdateVolumeSnapshotMsg msg = new APIUpdateVolumeSnapshotMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateVolumeSnapshotEvent evt = sender.send(msg, APIUpdateVolumeSnapshotEvent.class);
        return evt.getInventory();
    }

    public VipInventory updateVip(VipInventory inv) throws ApiSenderException {
        return updateVip(inv, null);
    }

    public VipInventory updateVip(VipInventory inv, SessionInventory session) throws ApiSenderException {
        APIUpdateVipMsg msg = new APIUpdateVipMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateVipEvent evt = sender.send(msg, APIUpdateVipEvent.class);
        return evt.getInventory();
    }

    public PortForwardingRuleInventory updatePortForwardingRule(PortForwardingRuleInventory inv) throws ApiSenderException {
        return updatePortForwardingRule(inv, null);
    }

    public PortForwardingRuleInventory updatePortForwardingRule(PortForwardingRuleInventory inv, SessionInventory session) throws ApiSenderException {
        APIUpdatePortForwardingRuleMsg msg = new APIUpdatePortForwardingRuleMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdatePortForwardingRuleEvent evt = sender.send(msg, APIUpdatePortForwardingRuleEvent.class);
        return evt.getInventory();
    }

    public SecurityGroupInventory updateSecurityGroup(SecurityGroupInventory inv) throws ApiSenderException {
        return updateSecurityGroup(inv, null);
    }

    public SecurityGroupInventory updateSecurityGroup(SecurityGroupInventory inv, SessionInventory session) throws ApiSenderException {
        APIUpdateSecurityGroupMsg msg = new APIUpdateSecurityGroupMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateSecurityGroupEvent evt = sender.send(msg, APIUpdateSecurityGroupEvent.class);
        return evt.getInventory();
    }

    public ImageInventory updateImage(ImageInventory inv) throws ApiSenderException {
        APIUpdateImageMsg msg = new APIUpdateImageMsg();
        msg.setSession(adminSession);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        msg.setGuestOsType(inv.getGuestOsType());
        msg.setSystem(inv.isSystem());
        msg.setFormat(inv.getFormat());
        msg.setPlatform(inv.getPlatform());
        msg.setMediaType(inv.getMediaType());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateImageEvent evt = sender.send(msg, APIUpdateImageEvent.class);
        return evt.getInventory();
    }

    public EipInventory updateEip(EipInventory inv) throws ApiSenderException {
        return updateEip(inv, null);
    }

    public EipInventory updateEip(EipInventory inv, SessionInventory session) throws ApiSenderException {
        APIUpdateEipMsg msg = new APIUpdateEipMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateEipEvent evt = sender.send(msg, APIUpdateEipEvent.class);
        return evt.getInventory();
    }

    public DiskOfferingInventory updateDiskOffering(DiskOfferingInventory inv) throws ApiSenderException {
        return updateDiskOffering(inv, null);
    }

    public DiskOfferingInventory updateDiskOffering(DiskOfferingInventory inv, SessionInventory session) throws ApiSenderException {
        APIUpdateDiskOfferingMsg msg = new APIUpdateDiskOfferingMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateDiskOfferingEvent evt = sender.send(msg, APIUpdateDiskOfferingEvent.class);
        return evt.getInventory();
    }

    public VirtualRouterOfferingInventory createVirtualRouterOffering(VirtualRouterOfferingInventory inv) throws ApiSenderException {
        return createVirtualRouterOffering(inv, null);
    }

    public VirtualRouterOfferingInventory createVirtualRouterOffering(VirtualRouterOfferingInventory inv, SessionInventory session) throws ApiSenderException {
        return createVirtualRouterOffering(inv, null, session);
    }

    public VirtualRouterOfferingInventory createVirtualRouterOffering(VirtualRouterOfferingInventory inv, List<String> sysTags, SessionInventory session) throws ApiSenderException {
        APICreateVirtualRouterOfferingMsg msg = new APICreateVirtualRouterOfferingMsg();
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setPublicNetworkUuid(inv.getPublicNetworkUuid());
        msg.setManagementNetworkUuid(inv.getManagementNetworkUuid());
        msg.setZoneUuid(inv.getZoneUuid());
        msg.setImageUuid(inv.getImageUuid());
        msg.setDefault(inv.isDefault());
        msg.setCpuSpeed(inv.getCpuSpeed());
        msg.setCpuNum(inv.getCpuNum());
        msg.setAllocatorStrategy(inv.getAllocatorStrategy());
        msg.setMemorySize(inv.getMemorySize());
        msg.setSystemTags(sysTags);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateInstanceOfferingEvent evt = sender.send(msg, APICreateInstanceOfferingEvent.class);
        return (VirtualRouterOfferingInventory) evt.getInventory();
    }

    public VirtualRouterOfferingInventory updateVirtualRouterOffering(VirtualRouterOfferingInventory offering) throws ApiSenderException {
        return updateVirtualRouterOffering(offering, null);
    }

    public VirtualRouterOfferingInventory updateVirtualRouterOffering(VirtualRouterOfferingInventory offering, SessionInventory session) throws ApiSenderException {
        APIUpdateVirtualRouterOfferingMsg msg = new APIUpdateVirtualRouterOfferingMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(offering.getUuid());
        msg.setName(offering.getName());
        msg.setDescription(offering.getDescription());
        msg.setIsDefault(offering.isDefault());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateInstanceOfferingEvent evt = sender.send(msg, APIUpdateInstanceOfferingEvent.class);
        return (VirtualRouterOfferingInventory) evt.getInventory();
    }

    public InstanceOfferingInventory updateInstanceOffering(InstanceOfferingInventory inv) throws ApiSenderException {
        return updateInstanceOffering(inv, null);
    }

    public InstanceOfferingInventory updateInstanceOffering(InstanceOfferingInventory inv, SessionInventory session) throws ApiSenderException {
        APIUpdateInstanceOfferingMsg msg = new APIUpdateInstanceOfferingMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateInstanceOfferingEvent evt = sender.send(msg, APIUpdateInstanceOfferingEvent.class);
        return evt.getInventory();
    }

    public L2NetworkInventory updateL2Network(L2NetworkInventory inv) throws ApiSenderException {
        APIUpdateL2NetworkMsg msg = new APIUpdateL2NetworkMsg();
        msg.setSession(adminSession);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateL2NetworkEvent evt = sender.send(msg, APIUpdateL2NetworkEvent.class);
        return evt.getInventory();
    }

    public L3NetworkInventory updateL3Network(L3NetworkInventory inv) throws ApiSenderException {
        return updateL3Network(inv, null);
    }

    public L3NetworkInventory updateL3Network(L3NetworkInventory inv, SessionInventory session) throws ApiSenderException {
        APIUpdateL3NetworkMsg msg = new APIUpdateL3NetworkMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateL3NetworkEvent evt = sender.send(msg, APIUpdateL3NetworkEvent.class);
        return evt.getInventory();
    }

    public IpRangeInventory updateIpRange(IpRangeInventory inv) throws ApiSenderException {
        APIUpdateIpRangeMsg msg = new APIUpdateIpRangeMsg();
        msg.setSession(adminSession);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateIpRangeEvent evt = sender.send(msg, APIUpdateIpRangeEvent.class);
        return evt.getInventory();
    }

    public KVMHostInventory updateKvmHost(KVMHostInventory inv, String password) throws ApiSenderException {
        APIUpdateKVMHostMsg msg = new APIUpdateKVMHostMsg();
        msg.setSession(adminSession);
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setUuid(inv.getUuid());
        msg.setUsername(inv.getUsername());
        msg.setPassword(password);
        msg.setSshPort(inv.getSshPort());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateHostEvent evt = sender.send(msg, APIUpdateHostEvent.class);
        return (KVMHostInventory) evt.getInventory();
    }

    public PrimaryStorageInventory syncPrimaryStorageCapacity(String primaryStorageUuid) throws ApiSenderException {
        APISyncPrimaryStorageCapacityMsg msg = new APISyncPrimaryStorageCapacityMsg();
        msg.setPrimaryStorageUuid(primaryStorageUuid);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APISyncPrimaryStorageCapacityEvent evt = sender.send(msg, APISyncPrimaryStorageCapacityEvent.class);
        return evt.getInventory();
    }

    public void revokeResourceSharing(List<String> resUuids, List<String> accountUuids, boolean toPublic) throws ApiSenderException {
        revokeResourceSharing(resUuids, accountUuids, toPublic, null);
    }

    public void revokeResourceSharing(List<String> resUuids, List<String> accountUuids, boolean toPublic, SessionInventory session) throws ApiSenderException {
        APIRevokeResourceSharingMsg msg = new APIRevokeResourceSharingMsg();
        msg.setResourceUuids(resUuids);
        msg.setAccountUuids(accountUuids);
        msg.setToPublic(toPublic);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIRevokeResourceSharingEvent.class);
    }

    public void revokeAllResourceSharing(List<String> resUuids, SessionInventory session) throws ApiSenderException {
        APIRevokeResourceSharingMsg msg = new APIRevokeResourceSharingMsg();
        msg.setAll(true);
        msg.setResourceUuids(resUuids);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIRevokeResourceSharingEvent.class);
    }

    public void shareResource(List<String> resUuids, List<String> accountUuids, boolean toPublic) throws ApiSenderException {
        shareResource(resUuids, accountUuids, toPublic, null);
    }

    public void shareResource(List<String> resUuids, List<String> accountUuids, boolean toPublic, SessionInventory session) throws ApiSenderException {
        APIShareResourceMsg msg = new APIShareResourceMsg();
        msg.setAccountUuids(accountUuids);
        msg.setResourceUuids(resUuids);
        msg.setToPublic(toPublic);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIShareResourceEvent.class);
    }

    public CephBackupStorageInventory addMonToCephBackupStorage(String bsUuid, List<String> monUrls) throws ApiSenderException {
        APIAddMonToCephBackupStorageMsg msg = new APIAddMonToCephBackupStorageMsg();
        msg.setUuid(bsUuid);
        msg.setMonUrls(monUrls);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAddMonToCephBackupStorageEvent evt = sender.send(msg, APIAddMonToCephBackupStorageEvent.class);
        return evt.getInventory();
    }

    public BackupStorageInventory updateCephBackupStorageMon(CephBackupStorageMonVO inv) throws ApiSenderException {
        APIUpdateCephBackupStorageMonMsg msg = new APIUpdateCephBackupStorageMonMsg();
        msg.setSession(adminSession);
        msg.setMonUuid(inv.getUuid());
        msg.setHostname(inv.getHostname());
        msg.setSshUsername(inv.getSshUsername());
        msg.setSshPassword(inv.getSshPassword());
        msg.setSshPort(inv.getSshPort());
        msg.setMonPort(inv.getMonPort());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateMonToCephBackupStorageEvent evt = sender.send(msg, APIUpdateMonToCephBackupStorageEvent.class);
        return evt.getInventory();
    }

    public PrimaryStorageInventory updateCephPrimaryStorageMon(CephPrimaryStorageMonVO inv) throws ApiSenderException {
        APIUpdateCephPrimaryStorageMonMsg msg = new APIUpdateCephPrimaryStorageMonMsg();
        msg.setSession(adminSession);
        msg.setMonUuid(inv.getUuid());
        msg.setHostname(inv.getHostname());
        msg.setSshUsername(inv.getSshUsername());
        msg.setSshPassword(inv.getSshPassword());
        msg.setSshPort(inv.getSshPort());
        msg.setMonPort(inv.getMonPort());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateMonToCephPrimaryStorageEvent evt = sender.send(msg, APIUpdateMonToCephPrimaryStorageEvent.class);
        return evt.getInventory();
    }

    public CephPrimaryStorageInventory addMonToCephPrimaryStorage(String psUuid, List<String> monUrls) throws ApiSenderException {
        APIAddMonToCephPrimaryStorageMsg msg = new APIAddMonToCephPrimaryStorageMsg();
        msg.setUuid(psUuid);
        msg.setMonUrls(monUrls);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAddMonToCephPrimaryStorageEvent evt = sender.send(msg, APIAddMonToCephPrimaryStorageEvent.class);
        return evt.getInventory();
    }

    public CephBackupStorageInventory removeMonFromBackupStorage(String bsUuid, List<String> hostnames) throws ApiSenderException {
        APIRemoveMonFromCephBackupStorageMsg msg = new APIRemoveMonFromCephBackupStorageMsg();
        msg.setUuid(bsUuid);
        msg.setMonHostnames(hostnames);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIRemoveMonFromCephBackupStorageEvent evt = sender.send(msg, APIRemoveMonFromCephBackupStorageEvent.class);
        return evt.getInventory();
    }

    public CephPrimaryStorageInventory removeMonFromPrimaryStorage(String psUuid, List<String> hostnames) throws ApiSenderException {
        APIRemoveMonFromCephPrimaryStorageMsg msg = new APIRemoveMonFromCephPrimaryStorageMsg();
        msg.setUuid(psUuid);
        msg.setMonHostnames(hostnames);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIRemoveMonFromCephPrimaryStorageEvent evt = sender.send(msg, APIRemoveMonFromCephPrimaryStorageEvent.class);
        return evt.getInventory();
    }

    public LoadBalancerInventory createLoadBalancer(String name, String vipUuid, List<String> tags, SessionInventory session) throws ApiSenderException {
        APICreateLoadBalancerMsg msg = new APICreateLoadBalancerMsg();
        msg.setName(name);
        msg.setVipUuid(vipUuid);
        msg.setSystemTags(tags);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateLoadBalancerEvent evt = sender.send(msg, APICreateLoadBalancerEvent.class);
        return evt.getInventory();
    }

    public LoadBalancerListenerInventory createLoadBalancerListener(LoadBalancerListenerInventory inv, SessionInventory session) throws ApiSenderException {
        return createLoadBalancerListener(inv, null, session);
    }

    public LoadBalancerListenerInventory createLoadBalancerListener(LoadBalancerListenerInventory inv, List<String> sysTags, SessionInventory session) throws ApiSenderException {
        APICreateLoadBalancerListenerMsg msg = new APICreateLoadBalancerListenerMsg();
        msg.setResourceUuid(inv.getUuid());
        msg.setLoadBalancerUuid(inv.getLoadBalancerUuid());
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setInstancePort(inv.getInstancePort());
        msg.setLoadBalancerPort(inv.getLoadBalancerPort());
        msg.setProtocol(inv.getProtocol());
        msg.setSession(session == null ? adminSession : session);
        msg.setSystemTags(sysTags);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateLoadBalancerListenerEvent evt = sender.send(msg, APICreateLoadBalancerListenerEvent.class);
        return evt.getInventory();
    }

    public LoadBalancerInventory refreshLoadBalancer(String uuid) throws ApiSenderException {
        return refreshLoadBalancer(uuid, null);
    }

    public LoadBalancerInventory refreshLoadBalancer(String uuid, SessionInventory session) throws ApiSenderException {
        APIRefreshLoadBalancerMsg msg = new APIRefreshLoadBalancerMsg();
        msg.setUuid(uuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIRefreshLoadBalancerEvent evt = sender.send(msg, APIRefreshLoadBalancerEvent.class);
        return evt.getInventory();
    }

    public LoadBalancerListenerInventory addVmNicToLoadBalancerListener(String listenerUuid, String nicUuid) throws ApiSenderException {
        return addVmNicToLoadBalancerListener(listenerUuid, nicUuid, null);
    }

    public LoadBalancerListenerInventory addVmNicToLoadBalancerListener(String listenerUuid, String nicUuid, SessionInventory session) throws ApiSenderException {
        APIAddVmNicToLoadBalancerMsg msg = new APIAddVmNicToLoadBalancerMsg();
        msg.setVmNicUuids(list(nicUuid));
        msg.setListenerUuid(listenerUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAddVmNicToLoadBalancerEvent evt = sender.send(msg, APIAddVmNicToLoadBalancerEvent.class);
        return evt.getInventory();
    }

    public LoadBalancerInventory removeNicFromLoadBalancerListener(String listenerUuid, String nicUuid, SessionInventory session) throws ApiSenderException {
        APIRemoveVmNicFromLoadBalancerMsg msg = new APIRemoveVmNicFromLoadBalancerMsg();
        msg.setListenerUuid(listenerUuid);
        msg.setVmNicUuids(list(nicUuid));
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIRemoveVmNicFromLoadBalancerEvent evt = sender.send(msg, APIRemoveVmNicFromLoadBalancerEvent.class);
        return evt.getInventory();
    }

    public LoadBalancerInventory deleteLoadBalancerListener(String uuid, SessionInventory session) throws ApiSenderException {
        APIDeleteLoadBalancerListenerMsg msg = new APIDeleteLoadBalancerListenerMsg();
        msg.setUuid(uuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteLoadBalancerListenerEvent evt = sender.send(msg, APIDeleteLoadBalancerListenerEvent.class);
        return evt.getInventory();
    }

    public void deleteLoadBalancer(String lbUuid, SessionInventory session) throws ApiSenderException {
        APIDeleteLoadBalancerMsg msg = new APIDeleteLoadBalancerMsg();
        msg.setUuid(lbUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.send(msg, APIDeleteLoadBalancerEvent.class);
    }

    public APIGetLocalStorageHostDiskCapacityReply getLocalStorageHostCapacity(String psUuid, String huuid) throws ApiSenderException {
        APIGetLocalStorageHostDiskCapacityMsg msg = new APIGetLocalStorageHostDiskCapacityMsg();
        msg.setHostUuid(huuid);
        msg.setPrimaryStorageUuid(psUuid);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        return sender.call(msg, APIGetLocalStorageHostDiskCapacityReply.class);
    }

    public void attachIso(String vmUuid, String isoUuid, SessionInventory session) throws ApiSenderException {
        APIAttachIsoToVmInstanceMsg msg = new APIAttachIsoToVmInstanceMsg();
        msg.setVmInstanceUuid(vmUuid);
        msg.setIsoUuid(isoUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIAttachIsoToVmInstanceEvent.class);
    }

    public void detachIso(String vmUuid, SessionInventory session) throws ApiSenderException {
        APIDetachIsoFromVmInstanceMsg msg = new APIDetachIsoFromVmInstanceMsg();
        msg.setVmInstanceUuid(vmUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDetachIsoFromVmInstanceEvent.class);
    }

    public VmInstanceInventory recoverVm(String vmUuid, SessionInventory session) throws ApiSenderException {
        APIRecoverVmInstanceMsg msg = new APIRecoverVmInstanceMsg();
        msg.setUuid(vmUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIRecoverVmInstanceEvent evt = sender.send(msg, APIRecoverVmInstanceEvent.class);
        return evt.getInventory();
    }

    public void expungeVm(String vmUuid, SessionInventory session) throws ApiSenderException {
        APIExpungeVmInstanceMsg msg = new APIExpungeVmInstanceMsg();
        msg.setUuid(vmUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIExpungeVmInstanceEvent.class);
    }

    public VolumeInventory recoverVolume(String volUuid, SessionInventory session) throws ApiSenderException {
        APIRecoverDataVolumeMsg msg = new APIRecoverDataVolumeMsg();
        msg.setUuid(volUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIRecoverDataVolumeEvent evt = sender.send(msg, APIRecoverDataVolumeEvent.class);
        return evt.getInventory();
    }

    public String getVersion() throws ApiSenderException {
        APIGetVersionMsg msg = new APIGetVersionMsg();
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetVersionReply reply = sender.call(msg, APIGetVersionReply.class);
        return reply.getVersion();
    }

    public Map<String, Long> getCurrentTime() throws ApiSenderException {
        APIGetCurrentTimeMsg msg = new APIGetCurrentTimeMsg();
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetCurrentTimeReply reply = sender.call(msg, APIGetCurrentTimeReply.class);
        return reply.getCurrentTime();
    }

    public void expungeImage(String imageUuid, List<String> bsUuids, SessionInventory session) throws ApiSenderException {
        APIExpungeImageMsg msg = new APIExpungeImageMsg();
        msg.setImageUuid(imageUuid);
        msg.setBackupStorageUuids(bsUuids);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIExpungeImageEvent.class);
    }

    public ImageInventory recoverImage(String imageUuid, List<String> bsUuids, SessionInventory session) throws ApiSenderException {
        APIRecoverImageMsg msg = new APIRecoverImageMsg();
        msg.setImageUuid(imageUuid);
        msg.setBackupStorageUuids(bsUuids);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIRecoverImageEvent evt = sender.send(msg, APIRecoverImageEvent.class);
        return evt.getInventory();
    }

    public void expungeDataVolume(String volumeUuid, SessionInventory session) throws ApiSenderException {
        APIExpungeDataVolumeMsg msg = new APIExpungeDataVolumeMsg();
        msg.setUuid(volumeUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIExpungeDataVolumeEvent.class);
    }

    public LocalStorageResourceRefInventory localStorageMigrateVolume(String volUuid, String hostUuid, SessionInventory session) throws ApiSenderException {
        MessageCommandRecorder.reset();
        MessageCommandRecorder.start(APILocalStorageMigrateVolumeMsg.class);

        APILocalStorageMigrateVolumeMsg msg = new APILocalStorageMigrateVolumeMsg();
        msg.setVolumeUuid(volUuid);
        msg.setDestHostUuid(hostUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APILocalStorageMigrateVolumeEvent evt = sender.send(msg, APILocalStorageMigrateVolumeEvent.class);

        logger.debug(MessageCommandRecorder.endAndToString());

        return evt.getInventory();
    }

    public List<HostInventory> getLocalStorageVolumeMigratableHost(String volUuid, SessionInventory session) throws ApiSenderException {
        APILocalStorageGetVolumeMigratableHostsMsg msg = new APILocalStorageGetVolumeMigratableHostsMsg();
        msg.setVolumeUuid(volUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APILocalStorageGetVolumeMigratableReply reply = sender.call(msg, APILocalStorageGetVolumeMigratableReply.class);
        return reply.getInventories();
    }

    public VmInstanceInventory setVmBootOrder(String vmUuid, List<String> order, SessionInventory session) throws ApiSenderException {
        APISetVmBootOrderMsg msg = new APISetVmBootOrderMsg();
        msg.setUuid(vmUuid);
        msg.setBootOrder(order);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APISetVmBootOrderEvent evt = sender.send(msg, APISetVmBootOrderEvent.class);
        return evt.getInventory();
    }


    public List<String> getVmBootOrder(String vmUuid, SessionInventory session) throws ApiSenderException {
        APIGetVmBootOrderMsg msg = new APIGetVmBootOrderMsg();
        msg.setUuid(vmUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetVmBootOrderReply reply = sender.call(msg, APIGetVmBootOrderReply.class);
        return reply.getOrder();
    }

    public VmInstanceInventory setVmConsolePassword(String vmUuid, String vncPassword, SessionInventory session) throws ApiSenderException {
        APISetVmConsolePasswordMsg msg = new APISetVmConsolePasswordMsg();
        msg.setUuid(vmUuid);
        msg.setConsolePassword(vncPassword);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APISetVmConsolePasswordEvent evt = sender.send(msg, APISetVmConsolePasswordEvent.class);
        return evt.getInventory();
    }

    public String getVmConsolePassword(String vmUuid, SessionInventory session) throws ApiSenderException {
        APIGetVmConsolePasswordMsg msg = new APIGetVmConsolePasswordMsg();
        msg.setUuid(vmUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetVmConsolePasswordReply reply = sender.call(msg, APIGetVmConsolePasswordReply.class);
        return reply.getConsolePassword();
    }

    public VmInstanceInventory deleteVmConsolePassword(String vmUuid, SessionInventory session) throws ApiSenderException {
        APIDeleteVmConsolePasswordMsg msg = new APIDeleteVmConsolePasswordMsg();
        msg.setUuid(vmUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteVmConsolePasswordEvent evt = sender.send(msg, APIDeleteVmConsolePasswordEvent.class);
        return evt.getInventory();
    }

    public VmInstanceInventory setVmSshKey(String vmUuid, String sshKey, SessionInventory session) throws ApiSenderException {
        APISetVmSshKeyMsg msg = new APISetVmSshKeyMsg();
        msg.setUuid(vmUuid);
        msg.setSshKey(sshKey);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APISetVmSshKeyEvent evt = sender.send(msg, APISetVmSshKeyEvent.class);
        return evt.getInventory();
    }

    public String getVmSshKey(String vmUuid, SessionInventory session) throws ApiSenderException {
        APIGetVmSshKeyMsg msg = new APIGetVmSshKeyMsg();
        msg.setUuid(vmUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetVmSshKeyReply reply = sender.call(msg, APIGetVmSshKeyReply.class);
        return reply.getSshKey();
    }

    public VmInstanceInventory deleteVmSshKey(String vmUuid, SessionInventory session) throws ApiSenderException {
        APIDeleteVmSshKeyMsg msg = new APIDeleteVmSshKeyMsg();
        msg.setUuid(vmUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteVmSshKeyEvent evt = sender.send(msg, APIDeleteVmSshKeyEvent.class);
        return evt.getInventory();
    }

    public LicenseInventory getLicenseInfo() throws ApiSenderException {
        APIGetLicenseInfoMsg msg = new APIGetLicenseInfoMsg();
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetLicenseInfoReply r = sender.call(msg, APIGetLicenseInfoReply.class);
        return r.getInventory();
    }

    public KVMHostInventory addKvmHost(String name, String mgmtIp, String clusterUuid) throws ApiSenderException {
        APIAddKVMHostMsg msg = new APIAddKVMHostMsg();
        msg.setName(name);
        msg.setManagementIp(mgmtIp);
        msg.setClusterUuid(clusterUuid);
        msg.setUsername("root");
        msg.setPassword("password");
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIAddHostEvent evt = sender.send(msg, APIAddHostEvent.class);
        return (KVMHostInventory) evt.getInventory();
    }

    public Map<String, String> getLicenseCapabilities() throws ApiSenderException {
        APIGetLicenseCapabilitiesMsg msg = new APIGetLicenseCapabilitiesMsg();
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetLicenseCapabilitiesReply reply = sender.call(msg, APIGetLicenseCapabilitiesReply.class);
        return reply.getCapabilities();
    }

    public String exportImage(String bsUuid, String imageUuid) throws ApiSenderException {
        APIExportImageFromBackupStorageMsg msg = new APIExportImageFromBackupStorageMsg();
        msg.setBackupStorageUuid(bsUuid);
        msg.setImageUuid(imageUuid);

        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIExportImageFromBackupStorageEvent evt = sender.send(msg, APIExportImageFromBackupStorageEvent.class);
        return evt.getImageUrl();
    }

    public void delExportedImage(String bsUuid, String imageUuid) throws ApiSenderException {
        APIDeleteExportedImageFromBackupStorageMsg msg = new APIDeleteExportedImageFromBackupStorageMsg();
        msg.setBackupStorageUuid(bsUuid);
        msg.setImageUuid(imageUuid);
        msg.setSession(adminSession);

        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteExportedImageFromBackupStorageEvent.class);
    }

    public void detachNetworkServicesFromL3Network(String l3Uuid, Map<String, List<String>> services) throws ApiSenderException {
        APIDetachNetworkServiceFromL3NetworkMsg msg = new APIDetachNetworkServiceFromL3NetworkMsg();
        msg.setL3NetworkUuid(l3Uuid);
        msg.setNetworkServices(services);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDetachNetworkServiceFromL3NetworkEvent.class);
    }

    public void setHostname(String uuid, String hostname, SessionInventory session) throws ApiSenderException {
        APISetVmHostnameMsg msg = new APISetVmHostnameMsg();
        msg.setUuid(uuid);
        msg.setHostname(hostname);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APISetVmHostnameEvent.class);
    }

    public String getHostname(String uuid, SessionInventory session) throws ApiSenderException {
        APIGetVmHostnameMsg msg = new APIGetVmHostnameMsg();
        msg.setUuid(uuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetVmHostnameReply reply = sender.call(msg, APIGetVmHostnameReply.class);
        return reply.getHostname();
    }

    public void deleteHostname(String uuid, SessionInventory session) throws ApiSenderException {
        APIDeleteVmHostnameMsg msg = new APIDeleteVmHostnameMsg();
        msg.setUuid(uuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteVmHostnameEvent.class);
    }

    public void setStaticIp(String vmUuid, String l3Uuid, String ip) throws ApiSenderException {
        APISetVmStaticIpMsg msg = new APISetVmStaticIpMsg();
        msg.setVmInstanceUuid(vmUuid);
        msg.setL3NetworkUuid(l3Uuid);
        msg.setIp(ip);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APISetVmStaticIpEvent.class);
    }

    public void deleteStaticIp(String vmUuid, String l3Uuid) throws ApiSenderException {
        APIDeleteVmStaticIpMsg msg = new APIDeleteVmStaticIpMsg();
        msg.setVmInstanceUuid(vmUuid);
        msg.setL3NetworkUuid(l3Uuid);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteVmStaticIpEvent.class);
    }

    public Map<String, String> checkUserPolicy(List<String> apiNames, String userUuid, SessionInventory session) throws ApiSenderException {
        APICheckApiPermissionMsg msg = new APICheckApiPermissionMsg();
        msg.setUserUuid(userUuid);
        msg.setApiNames(apiNames);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICheckApiPermissionReply reply = sender.call(msg, APICheckApiPermissionReply.class);
        return reply.getInventory();
    }

    public APICalculateAccountSpendingReply calculateSpending(String accountUuid, Long start, Long end, SessionInventory session) throws ApiSenderException {
        APICalculateAccountSpendingMsg msg = new APICalculateAccountSpendingMsg();
        msg.setAccountUuid(accountUuid);
        msg.setSession(session == null ? adminSession : session);
        msg.setDateStart(start);
        msg.setDateEnd(end);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICalculateAccountSpendingReply reply = sender.call(msg, APICalculateAccountSpendingReply.class);
        return reply;
    }

    public APICalculateAccountSpendingReply calculateSpending(String accountUuid, SessionInventory session) throws ApiSenderException {
        return calculateSpending(accountUuid, null, null, session);
    }

    public PriceInventory createPrice(APICreateResourcePriceMsg msg) throws ApiSenderException {
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateResourcePriceEvent evt = sender.send(msg, APICreateResourcePriceEvent.class);
        return evt.getInventory();
    }

    public void deletePrice(String uuid) throws ApiSenderException {
        APIDeleteResourcePriceMsg msg = new APIDeleteResourcePriceMsg();
        msg.setSession(adminSession);
        msg.setUuid(uuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteResourcePriceEvent.class);
    }

    public AccountInventory updateAccount(AccountInventory acnt, String password, SessionInventory session) throws ApiSenderException {
        APIUpdateAccountMsg msg = new APIUpdateAccountMsg();
        msg.setName(acnt.getName());
        msg.setPassword(password);
        msg.setDescription(acnt.getDescription());
        msg.setUuid(acnt.getUuid());
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateAccountEvent evt = sender.send(msg, APIUpdateAccountEvent.class);
        return evt.getInventory();
    }

    public UserGroupInventory updateUserGroup(UserGroupInventory group, SessionInventory session) throws ApiSenderException {
        APIUpdateUserGroupMsg msg = new APIUpdateUserGroupMsg();
        msg.setUuid(group.getUuid());
        msg.setName(group.getName());
        msg.setDescription(group.getDescription());
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateUserGroupEvent evt = sender.send(msg, APIUpdateUserGroupEvent.class);
        return evt.getInventory();
    }

    public UserInventory updateUser(UserInventory user, String password, SessionInventory session) throws ApiSenderException {
        APIUpdateUserMsg msg = new APIUpdateUserMsg();
        msg.setUuid(user.getUuid());
        msg.setPassword(password);
        msg.setName(user.getName());
        msg.setDescription(user.getDescription());
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateUserEvent evt = sender.send(msg, APIUpdateUserEvent.class);
        return evt.getInventory();
    }

    public void setVmHaLevel(String vmUuid, VmHaLevel level, SessionInventory session) throws ApiSenderException {
        APISetVmInstanceHaLevelMsg msg = new APISetVmInstanceHaLevelMsg();
        msg.setUuid(vmUuid);
        msg.setLevel(level.toString());
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APISetVmInstanceHaLevelEvent.class);
        return;
    }

    public void deleteVmHaLevel(String vmUuid, SessionInventory session) throws ApiSenderException {
        APIDeleteVmInstanceHaLevelMsg msg = new APIDeleteVmInstanceHaLevelMsg();
        msg.setUuid(vmUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteVmHostnameEvent.class);
        return;
    }

    public ImageInventory commitVolumeAsImage(String volumeUuid, String imageName, List<String> bsUuids) throws ApiSenderException {
        APICommitVolumeAsImageMsg msg = new APICommitVolumeAsImageMsg();
        msg.setBackupStorageUuids(bsUuids);
        msg.setVolumeUuid(volumeUuid);
        msg.setName(imageName);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICommitVolumeAsImageEvent evt = sender.send(msg, APICommitVolumeAsImageEvent.class);
        return evt.getInventory();
    }

    public Map<String, AccountInventory> getResourceAccount(List<String> resUuids) throws ApiSenderException {
        APIGetResourceAccountMsg msg = new APIGetResourceAccountMsg();
        msg.setResourceUuids(resUuids);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetResourceAccountReply reply = sender.call(msg, APIGetResourceAccountReply.class);
        return reply.getInventories();
    }

    public AccountResourceRefInventory changeResourceOwner(String resourceUuid, String accountUuid) throws ApiSenderException {
        APIChangeResourceOwnerMsg msg = new APIChangeResourceOwnerMsg();
        msg.setResourceUuid(resourceUuid);
        msg.setAccountUuid(accountUuid);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeResourceOwnerEvent evt = sender.send(msg, APIChangeResourceOwnerEvent.class);
        return evt.getInventory();
    }

    public VolumeInventory syncVolumeSize(String volumeUuid, SessionInventory session) throws ApiSenderException {
        APISyncVolumeSizeMsg msg = new APISyncVolumeSizeMsg();
        msg.setUuid(volumeUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APISyncVolumeSizeEvent evt = sender.send(msg, APISyncVolumeSizeEvent.class);
        return evt.getInventory();
    }

    public APIGetVmStartingCandidateClustersHostsReply getVmStartingCandidateHosts(String vmUuid, SessionInventory session) throws ApiSenderException {
        APIGetVmStartingCandidateClustersHostsMsg msg = new APIGetVmStartingCandidateClustersHostsMsg();
        msg.setUuid(vmUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetVmStartingCandidateClustersHostsReply reply = sender.call(msg, APIGetVmStartingCandidateClustersHostsReply.class);
        return reply;
    }

    public void cleanupImageCache(String primaryStorageUuid) throws ApiSenderException {
        APICleanUpImageCacheOnPrimaryStorageMsg msg = new APICleanUpImageCacheOnPrimaryStorageMsg();
        msg.setUuid(primaryStorageUuid);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APICleanUpImageCacheOnPrimaryStorageEvent.class);
    }

    public <T extends MessageReply> T queryCassandra(APIQueryCassandraMsg msg, Class<T> clz) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        msg.setSession(adminSession);
        sender.setTimeout(timeout);
        T reply = sender.call(msg, clz);
        return reply;
    }

    public Map<String, Object> getVmCapabilities(String vmUuid, SessionInventory session) throws ApiSenderException {
        APIGetVmCapabilitiesMsg msg = new APIGetVmCapabilitiesMsg();
        msg.setUuid(vmUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetVmCapabilitiesReply reply = sender.call(msg, APIGetVmCapabilitiesReply.class);
        return reply.getCapabilities();
    }

    public Map<String, Object> getVolumeCapabilities(String volumeUuid, SessionInventory session) throws ApiSenderException {
        APIGetVolumeCapabilitiesMsg msg = new APIGetVolumeCapabilitiesMsg();
        msg.setUuid(volumeUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetVolumeCapabilitiesReply reply = sender.call(msg, APIGetVolumeCapabilitiesReply.class);
        return reply.getCapabilities();
    }

    public void deleteLog(String uuid, SessionInventory session) throws ApiSenderException {
        APIDeleteLogMsg msg = new APIDeleteLogMsg();
        msg.setUuids(list(uuid));
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteLogEvent evt = sender.send(msg, APIDeleteLogEvent.class);
    }

    public void debugSignal(DebugSignal... ds) throws ApiSenderException {
        APIDebugSignalMsg msg = new APIDebugSignalMsg();
        List<String> lst = new ArrayList<>();
        for (DebugSignal sig : ds) {
            lst.add(sig.toString());
        }
        msg.setSignals(lst);
        msg.setSession(adminSession);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDebugSignalEvent.class);
    }

    public String createVolumeSnapshotScheduler(String volUuid, SessionInventory session, String type, Long startDate, Integer interval, Integer repeatCount) throws ApiSenderException {
        Date date = new Date();
        APICreateVolumeSnapshotSchedulerMsg msg = new APICreateVolumeSnapshotSchedulerMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setSchedulerName("test");
        msg.setInterval(interval);
        if (repeatCount != null) {
            msg.setRepeatCount(repeatCount);
        }
        msg.setType(type);
        msg.setStartTime(startDate);
        msg.setSnapShotName("Snapshot-" + volUuid);
        msg.setVolumeSnapshotDescription("Test snapshot");
        msg.setVolumeUuid(volUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateVolumeSnapshotSchedulerEvent evt = sender.send(msg, APICreateVolumeSnapshotSchedulerEvent.class);
        logger.debug(MessageCommandRecorder.endAndToString());
        return evt.getInventory().getUuid();
    }

    public void createCronScheduler(String volUuid, String type, String cronTask, SessionInventory session) throws ApiSenderException {
        APICreateVolumeSnapshotSchedulerMsg msg = new APICreateVolumeSnapshotSchedulerMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setSchedulerName("testCron");
        msg.setCron(cronTask);
        msg.setType(type);
        msg.setSnapShotName("Snapshot-" + volUuid);
        msg.setVolumeSnapshotDescription("Test snapshot");
        msg.setVolumeUuid(volUuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateVolumeSnapshotSchedulerEvent evt = sender.send(msg, APICreateVolumeSnapshotSchedulerEvent.class);
        logger.debug(MessageCommandRecorder.endAndToString());
    }

    public SchedulerInventory updateScheduler(String uuid, String schedulerName, String schedulerDescription, SessionInventory session) throws ApiSenderException {
        APIUpdateSchedulerMsg msg = new APIUpdateSchedulerMsg();
        if (schedulerName != null) {
            msg.setSchedulerName(schedulerName);
        }

        if (schedulerDescription != null) {
            msg.setSchedulerDescription(schedulerDescription);
        }

        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(uuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIUpdateSchedulerEvent evt = sender.send(msg, APIUpdateSchedulerEvent.class);
        logger.debug(MessageCommandRecorder.endAndToString());
        return evt.getInventory();
    }

    public void deleteScheduler(String uuid, SessionInventory session) throws ApiSenderException {
        APIDeleteSchedulerMsg msg = new APIDeleteSchedulerMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(uuid);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDeleteSchedulerEvent evt = sender.send(msg, APIDeleteSchedulerEvent.class);
        logger.debug(MessageCommandRecorder.endAndToString());
    }

    public void stopVmInstanceScheduler(String vmUuid,
                                        String type,
                                        Long startDate,
                                        Integer interval,
                                        Integer repeatCount,
                                        SessionInventory session) throws ApiSenderException {
        APICreateStopVmInstanceSchedulerMsg msg = new APICreateStopVmInstanceSchedulerMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setSchedulerName("stopvm");
        msg.setInterval(interval);
        if (repeatCount != null) {
            msg.setRepeatCount(repeatCount);
        }
        msg.setType(type);
        msg.setStartTime(startDate);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setVmUuid(vmUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateStopVmInstanceSchedulerEvent evt = sender.send(msg, APICreateStopVmInstanceSchedulerEvent.class);
        logger.debug(MessageCommandRecorder.endAndToString());
    }

    public void startVmInstanceScheduler(String vmUuid,
                                         String type,
                                         Long startDate,
                                         Integer interval,
                                         Integer repeatCount,
                                         SessionInventory session) throws ApiSenderException {
        APICreateStartVmInstanceSchedulerMsg msg = new APICreateStartVmInstanceSchedulerMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setSchedulerName("startvm");
        msg.setInterval(interval);
        if (repeatCount != null) {
            msg.setRepeatCount(repeatCount);
        }
        msg.setType(type);
        msg.setStartTime(startDate);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setVmUuid(vmUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateStartVmInstanceSchedulerEvent evt = sender.send(msg, APICreateStartVmInstanceSchedulerEvent.class);
        logger.debug(MessageCommandRecorder.endAndToString());
    }

    public SchedulerInventory rebootVmInstanceScheduler(String vmUuid,
                                                        String type,
                                                        Long startDate,
                                                        Integer interval,
                                                        Integer repeatCount,
                                                        SessionInventory session) throws ApiSenderException {
        APICreateRebootVmInstanceSchedulerMsg msg = new APICreateRebootVmInstanceSchedulerMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setSchedulerName("rebootvm");
        msg.setInterval(interval);
        if (repeatCount != null) {
            msg.setRepeatCount(repeatCount);
        }
        msg.setType(type);
        msg.setStartTime(startDate);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setVmUuid(vmUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateRebootVmInstanceSchedulerEvent evt = sender.send(msg, APICreateRebootVmInstanceSchedulerEvent.class);
        logger.debug(MessageCommandRecorder.endAndToString());
        return evt.getInventory();
    }

    public SchedulerInventory changeSchedulerState(String uuid, String state, SessionInventory session) throws ApiSenderException {
        APIChangeSchedulerStateMsg msg = new APIChangeSchedulerStateMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(uuid);
        msg.setStateEvent(state);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeSchedulerStateEvent evt = sender.send(msg, APIChangeSchedulerStateEvent.class);
        logger.debug(MessageCommandRecorder.endAndToString());
        return evt.getInventory();
    }

    public List<BackupStorageInventory> getCandidateBackupStorageForCreatingImage(String volUuid, String spUuid, SessionInventory session) throws ApiSenderException {
        APIGetCandidateBackupStorageForCreatingImageMsg msg = new APIGetCandidateBackupStorageForCreatingImageMsg();
        msg.setVolumeUuid(volUuid);
        msg.setVolumeSnapshotUuid(spUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetCandidateBackupStorageForCreatingImageReply reply = sender.call(msg, APIGetCandidateBackupStorageForCreatingImageReply.class);
        return reply.getInventories();
    }

    public <T> T sendApiMessage(APIMessage msg, Class retClass) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        if (msg.getSession() == null) {
            msg.setSession(adminSession);
        }

        if (msg instanceof APISyncCallMessage) {
            return (T) sender.call(msg, retClass);
        } else {
            return (T) sender.send(msg, retClass);
        }
    }

    public void deleteAlarm(String alarmUuid, SessionInventory session) throws ApiSenderException {
        APIDeleteAlarmMsg msg = new APIDeleteAlarmMsg();
        msg.setUuid(alarmUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteAlarmEvent.class);
    }

    public List<VmInstanceInventory> getCandidateVmForAttachingIso(String isoUuid, SessionInventory session) throws ApiSenderException {
        APIGetCandidateVmForAttachingIsoMsg msg = new APIGetCandidateVmForAttachingIsoMsg();
        msg.setIsoUuid(isoUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetCandidateVmForAttachingIsoReply reply = sender.call(msg, APIGetCandidateVmForAttachingIsoReply.class);
        return reply.getInventories();
    }

    public List<ImageInventory> getCandidateIsoForAttachingVm(String vmUuid, SessionInventory session) throws ApiSenderException {
        APIGetCandidateIsoForAttachingVmMsg msg = new APIGetCandidateIsoForAttachingVmMsg();
        msg.setVmInstanceUuid(vmUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIGetCandidateIsoForAttachingVmReply reply = sender.call(msg, APIGetCandidateIsoForAttachingVmReply.class);
        return reply.getInventories();
    }

    public IPsecConnectionInventory createIPsecConnection(IPsecConnectionInventory inv, List<String> peerCidrs, SessionInventory session) throws ApiSenderException {
        APICreateIPsecConnectionMsg msg = new APICreateIPsecConnectionMsg();
        msg.setName(inv.getName());
        msg.setDescription(inv.getDescription());
        msg.setSession(session == null ? adminSession : session);
        msg.setAuthKey(inv.getAuthKey());
        msg.setAuthMode(inv.getAuthMode());
        msg.setIkeAuthAlgorithm(inv.getIkeAuthAlgorithm());
        msg.setIkeDhGroup(inv.getIkeDhGroup());
        msg.setIkeEncryptionAlgorithm(inv.getIkeEncryptionAlgorithm());
        msg.setL3NetworkUuid(inv.getL3NetworkUuid());
        msg.setPeerAddress(inv.getPeerAddress());
        msg.setPeerCidrs(peerCidrs);
        msg.setPfs(inv.getPfs());
        msg.setPolicyAuthAlgorithm(inv.getPolicyAuthAlgorithm());
        msg.setPolicyMode(inv.getPolicyMode());
        msg.setPolicyEncryptionAlgorithm(inv.getPolicyEncryptionAlgorithm());
        msg.setPolicyAuthAlgorithm(inv.getPolicyAuthAlgorithm());
        msg.setVipUuid(inv.getVipUuid());
        msg.setTransformProtocol(inv.getTransformProtocol());
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APICreateIPsecConnectionEvent evt = sender.send(msg, APICreateIPsecConnectionEvent.class);
        return evt.getInventory();
    }

    public void deleteIPsecConnection(String uuid, SessionInventory session) throws ApiSenderException {
        APIDeleteIPsecConnectionMsg msg = new APIDeleteIPsecConnectionMsg();
        msg.setUuid(uuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        sender.send(msg, APIDeleteIPsecConnectionEvent.class);
        return;
    }
}
