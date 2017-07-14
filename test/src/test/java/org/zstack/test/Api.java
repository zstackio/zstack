package org.zstack.test;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.APIListApplianceVmMsg;
import org.zstack.appliancevm.APIListApplianceVmReply;
import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.core.MessageCommandRecorder;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusEventListener;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.APIListGlobalConfigMsg;
import org.zstack.core.config.APIListGlobalConfigReply;
import org.zstack.core.config.GlobalConfigInventory;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.debug.APIDebugSignalEvent;
import org.zstack.core.debug.APIDebugSignalMsg;
import org.zstack.core.debug.DebugSignal;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.network.securitygroup.VmNicSecurityGroupRefInventory;
import org.zstack.header.allocator.APIGetCpuMemoryCapacityReply;
import org.zstack.header.apimediator.APIIsReadyToGoMsg;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.cluster.*;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.*;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.console.ConsoleInventory;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.*;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountResourceRefInventory;
import org.zstack.header.identity.PolicyInventory;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.header.identity.QuotaInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.UserGroupInventory;
import org.zstack.header.identity.UserInventory;
import org.zstack.header.image.*;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.managementnode.*;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIReply;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.Event;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2VlanNetworkInventory;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.l3.FreeIpInventory;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.*;
import org.zstack.header.network.service.NetworkServiceProviderInventory;
import org.zstack.header.query.*;
import org.zstack.header.search.*;
import org.zstack.header.simulator.ChangeVmStateOnSimulatorHostMsg;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.header.simulator.storage.backup.SimulatorBackupStorageConstant;
import org.zstack.header.simulator.storage.backup.SimulatorBackupStorageDetails;
import org.zstack.header.simulator.storage.primary.APIAddSimulatorPrimaryStorageMsg;
import org.zstack.header.simulator.storage.primary.SimulatorPrimaryStorageConstant;
import org.zstack.header.simulator.storage.primary.SimulatorPrimaryStorageDetails;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeInventory;
import org.zstack.header.tag.TagInventory;
import org.zstack.header.tag.TagType;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.volume.*;
import org.zstack.header.volume.APIGetVolumeFormatReply.VolumeFormatReplyStruct;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.zone.*;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.kvm.APIAddKVMHostMsg;
import org.zstack.kvm.APIUpdateKVMHostMsg;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.network.securitygroup.*;
import org.zstack.network.service.eip.APIUpdateEipEvent;
import org.zstack.network.service.eip.APIUpdateEipMsg;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.eip.EipStateEvent;
import org.zstack.network.service.lb.*;
import org.zstack.network.service.lb.LoadBalancerInventory;
import org.zstack.network.service.lb.LoadBalancerListenerInventory;
import org.zstack.network.service.portforwarding.*;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.vip.APIUpdateVipEvent;
import org.zstack.network.service.vip.APIUpdateVipMsg;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipStateEvent;
import org.zstack.network.service.virtualrouter.APICreateVirtualRouterOfferingMsg;
import org.zstack.network.service.virtualrouter.APIUpdateVirtualRouterOfferingMsg;
import org.zstack.network.service.virtualrouter.VirtualRouterOfferingInventory;
import org.zstack.portal.managementnode.ManagementNodeManager;
import org.zstack.sdk.*;
import org.zstack.storage.backup.sftp.APIReconnectSftpBackupStorageEvent;
import org.zstack.storage.backup.sftp.APIReconnectSftpBackupStorageMsg;
import org.zstack.storage.backup.sftp.APIUpdateSftpBackupStorageMsg;
import org.zstack.storage.backup.sftp.SftpBackupStorageInventory;
import org.zstack.storage.ceph.backup.*;
import org.zstack.storage.ceph.backup.CephBackupStorageInventory;
import org.zstack.storage.ceph.primary.*;
import org.zstack.storage.ceph.primary.CephPrimaryStorageInventory;
import org.zstack.storage.primary.local.*;
import org.zstack.storage.primary.local.LocalStorageResourceRefInventory;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.Callable;
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
    private int timeout = 15;

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    static {
        loader = Platform.getComponentLoader();

        ZSClient.configure(
                new ZSConfig.Builder()
                        .setHostname("127.0.0.1")
                        .setPort(8989)
                        .setDefaultPollingInterval(100, TimeUnit.MILLISECONDS)
                        .setDefaultPollingTimeout(15, TimeUnit.SECONDS)
                        .setReadTimeout(10, TimeUnit.MINUTES)
                        .setWriteTimeout(10, TimeUnit.MINUTES)
                        .build()
        );
    }

    private void start() {
        mgr.startNode();
    }

    public void prepare() {
        try {
            adminSession = this.loginAsAdmin();
        } catch (ApiSenderException e1) {
            throw new CloudRuntimeException(e1);
        }
    }

    public void startServer() {
        mgr = loader.getComponent(ManagementNodeManager.class);
        start();

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
        GetVmMigrationCandidateHostsAction action = new GetVmMigrationCandidateHostsAction();
        action.sessionId = adminSession.getUuid();
        action.vmInstanceUuid = vmUuid;
        GetVmMigrationCandidateHostsAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.toCollection(
                JSONObjectUtil.toJsonString(res.value.getInventories()),
                ArrayList.class,
                HostInventory.class
        );
    }

    public VolumeSnapshotInventory createSnapshot(String volUuid) throws ApiSenderException {
        return createSnapshot(volUuid, null);
    }

    public VolumeSnapshotInventory createSnapshot(String volUuid, SessionInventory session) throws ApiSenderException {
        MessageCommandRecorder.reset();
        MessageCommandRecorder.start(APICreateVolumeSnapshotMsg.class);

        CreateVolumeSnapshotAction action = new CreateVolumeSnapshotAction();
        action.sessionId = session == null ? adminSession.getUuid() : session.getUuid();
        action.name = String.format("Snapshot-%s", volUuid);
        action.description = "test snapshot";
        action.volumeUuid = volUuid;
        CreateVolumeSnapshotAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        logger.debug(MessageCommandRecorder.endAndToString());

        return JSONObjectUtil.rehashObject(res.value.getInventory(), VolumeSnapshotInventory.class);
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
        CreateDataVolumeFromVolumeSnapshotAction action = new CreateDataVolumeFromVolumeSnapshotAction();
        action.sessionId = session == null ? adminSession.getUuid() : session.getUuid();
        action.primaryStorageUuid = priUuid;
        action.name = String.format("volume-from-snapshot-%s", snapshotUuid);
        action.volumeSnapshotUuid = snapshotUuid;
        CreateDataVolumeFromVolumeSnapshotAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), VolumeInventory.class);
    }

    public ImageInventory createTemplateFromSnapshot(String snapshotUuid, List<String> backupStorageUuids) throws ApiSenderException {
        return createTemplateFromSnapshot(snapshotUuid, backupStorageUuids, null);
    }

    public ImageInventory createTemplateFromSnapshot(String snapshotUuid, String bsUuid, SessionInventory session) throws ApiSenderException {
        return createTemplateFromSnapshot(snapshotUuid, Collections.singletonList(bsUuid), session);
    }

    public ImageInventory createTemplateFromSnapshot(String snapshotUuid, List<String> backupStorageUuids, SessionInventory session) throws ApiSenderException {
        MessageCommandRecorder.reset();
        MessageCommandRecorder.start(APICreateRootVolumeTemplateFromVolumeSnapshotMsg.class);

        CreateRootVolumeTemplateFromVolumeSnapshotAction action = new CreateRootVolumeTemplateFromVolumeSnapshotAction();
        action.sessionId = session == null ? adminSession.getUuid() : session.getUuid();
        action.backupStorageUuids = backupStorageUuids;
        action.snapshotUuid = snapshotUuid;
        action.name = String.format("image-from-snapshot-%s", snapshotUuid);
        action.guestOsType = "CentOS";
        CreateRootVolumeTemplateFromVolumeSnapshotAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        logger.debug(MessageCommandRecorder.endAndToString());
        return JSONObjectUtil.rehashObject(res.value.getInventory(), ImageInventory.class);
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

        DeleteVolumeSnapshotAction action = new DeleteVolumeSnapshotAction();
        action.sessionId = session == null ? adminSession.getUuid() : session.getUuid();
        action.uuid = snapshotUuid;
        DeleteVolumeSnapshotAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
        logger.debug(MessageCommandRecorder.endAndToString());
    }

    public void revertVolumeToSnapshot(String snapshotUuid) throws ApiSenderException {
        revertVolumeToSnapshot(snapshotUuid, null);
    }

    public void revertVolumeToSnapshot(String snapshotUuid, SessionInventory session) throws ApiSenderException {
        RevertVolumeFromSnapshotAction action = new RevertVolumeFromSnapshotAction();
        action.sessionId = session == null ? adminSession.getUuid() : session.getUuid();
        action.uuid = snapshotUuid;
        RevertVolumeFromSnapshotAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public List<ZoneInventory> createZones(int num) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        List<ZoneInventory> ret = new ArrayList<ZoneInventory>();
        for (int i = 0; i < num; i++) {
            APICreateZoneMsg msg = new APICreateZoneMsg();

            CreateZoneAction action = new CreateZoneAction();
            action.sessionId = adminSession.getUuid();
            action.name = String.format("Zone-%s", i);
            action.description = "test zone";
            CreateZoneAction.Result res = action.call();
            throwExceptionIfNeed(res.error);

            ret.add(JSONObjectUtil.rehashObject(res.value.getInventory(), ZoneInventory.class));
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
        DeleteZoneAction action = new DeleteZoneAction();
        action.sessionId = adminSession.getUuid();
        action.uuid = uuid;
        DeleteZoneAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public ZoneInventory changeZoneState(String uuid, ZoneStateEvent evt) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIChangeZoneStateMsg msg = new APIChangeZoneStateMsg(uuid, evt.toString());

        ChangeZoneStateAction action = new ChangeZoneStateAction();
        action.uuid = uuid;
        action.sessionId = adminSession.getUuid();
        action.stateEvent = evt.toString();
        ChangeZoneStateAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), ZoneInventory.class);
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
            CreateClusterAction action = new CreateClusterAction();
            action.name = String.format("Cluster-%s", i);
            action.description = "test cluster";
            action.hypervisorType = SimulatorConstant.SIMULATOR_HYPERVISOR_TYPE;
            action.zoneUuid = zoneUuid;
            action.sessionId = adminSession.getUuid();
            CreateClusterAction.Result res = action.call();
            throwExceptionIfNeed(res.error);

            ret.add(JSONObjectUtil.rehashObject(res.value.getInventory(), ClusterInventory.class));
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

        ChangeClusterStateAction action = new ChangeClusterStateAction();
        action.sessionId = adminSession.getUuid();
        action.stateEvent = evt.toString();
        action.uuid = uuid;
        ChangeClusterStateAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), ClusterInventory.class);
    }

    public void deleteCluster(String uuid) throws ApiSenderException {
        DeleteClusterAction action = new DeleteClusterAction();
        action.sessionId = adminSession.getUuid();
        action.uuid = uuid;
        DeleteClusterAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public List<HostInventory> createHost(int num, String clusterUuid) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        List<HostInventory> rets = new ArrayList<HostInventory>();
        for (int i = 0; i < num; i++) {
            AddSimulatorHostAction action = new AddSimulatorHostAction();
            action.clusterUuid = clusterUuid;
            action.description = "test host";
            action.managementIp = "10.0.0." + i;
            action.memoryCapacity = SizeUnit.GIGABYTE.toByte(8);
            action.cpuCapacity = 2400 * 4;
            action.sessionId = adminSession.getUuid();
            action.name = "host" + i;
            AddSimulatorHostAction.Result res = action.call();
            throwExceptionIfNeed(res.error);

            rets.add(JSONObjectUtil.rehashObject(res.value.getInventory(), HostInventory.class));
        }

        return rets;
    }

    public HostInventory changeHostState(String uuid, HostStateEvent evt) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);

        ChangeHostStateAction action = new ChangeHostStateAction();
        action.sessionId = adminSession.getUuid();
        action.stateEvent = evt.toString();
        action.uuid = uuid;
        ChangeHostStateAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, HostInventory.class);
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
        DeleteHostAction action = new DeleteHostAction();
        action.uuid = uuid;
        action.sessionId = adminSession.getUuid();
        DeleteHostAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public HostInventory maintainHost(String uuid) throws ApiSenderException {
        return changeHostState(uuid, HostStateEvent.maintain);
    }

    public List<PrimaryStorageInventory> createSimulatoPrimaryStorage(int num, SimulatorPrimaryStorageDetails details) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        List<PrimaryStorageInventory> rets = new ArrayList<PrimaryStorageInventory>();
        for (int i = 0; i < num; i++) {
            APIAddSimulatorPrimaryStorageMsg msg = new APIAddSimulatorPrimaryStorageMsg();

            AddSimulatorPrimaryStorageAction action = new AddSimulatorPrimaryStorageAction();
            action.url = details.getUrl() + "-" + i;
            action.type = SimulatorPrimaryStorageConstant.SIMULATOR_PRIMARY_STORAGE_TYPE;
            action.description = "simulator";
            action.name = "SimulatorPrimaryStorage-" + i;
            action.sessionId = adminSession.getUuid();
            action.totalCapacity = details.getTotalCapacity();
            action.availableCapacity = details.getAvailableCapacity();
            action.zoneUuid = details.getZoneUuid();
            AddSimulatorPrimaryStorageAction.Result res = action.call();
            throwExceptionIfNeed(res.error);

            rets.add(JSONObjectUtil.rehashObject(res.value.inventory, PrimaryStorageInventory.class));
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

    public PrimaryStorageInventory changePrimaryStorageState(String uuid, PrimaryStorageStateEvent event) throws ApiSenderException {
        ChangePrimaryStorageStateAction action = new ChangePrimaryStorageStateAction();
        action.sessionId = adminSession.getUuid();
        action.uuid = uuid;
        action.stateEvent = event.toString();
        ChangePrimaryStorageStateAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, PrimaryStorageInventory.class);
    }

    public void deletePrimaryStorage(String uuid) throws ApiSenderException {
        DeletePrimaryStorageAction action = new DeletePrimaryStorageAction();
        action.sessionId = adminSession.getUuid();
        action.uuid = uuid;
        DeletePrimaryStorageAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public PrimaryStorageInventory attachPrimaryStorage(String clusterUuid, String uuid) throws ApiSenderException {
        AttachPrimaryStorageToClusterAction action = new AttachPrimaryStorageToClusterAction();
        action.sessionId = adminSession.getUuid();
        action.clusterUuid = clusterUuid;
        action.primaryStorageUuid = uuid;
        AttachPrimaryStorageToClusterAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, PrimaryStorageInventory.class);
    }

    public PrimaryStorageInventory detachPrimaryStorage(String uuid, String clusterUuid) throws ApiSenderException {
        DetachPrimaryStorageFromClusterAction action = new DetachPrimaryStorageFromClusterAction();
        action.primaryStorageUuid = uuid;
        action.clusterUuid = clusterUuid;
        action.sessionId = adminSession.getUuid();
        DetachPrimaryStorageFromClusterAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, PrimaryStorageInventory.class);
    }

    public List<BackupStorageInventory> createSimulatorBackupStorage(int num, SimulatorBackupStorageDetails details) throws ApiSenderException {
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        List<BackupStorageInventory> rets = new ArrayList<BackupStorageInventory>();
        for (int i = 0; i < num; i++) {
            AddSimulatorBackupStorageAction action = new AddSimulatorBackupStorageAction();
            action.sessionId = adminSession.getUuid();
            action.name = "SimulatoryBackupStorage-" + i;
            action.url = details.getUrl() + "-" + i;
            action.type = SimulatorBackupStorageConstant.SIMULATOR_BACKUP_STORAGE_TYPE;
            action.description = "test";
            action.totalCapacity = details.getTotalCapacity();
            action.availableCapacity = details.getTotalCapacity() - details.getUsedCapacity();
            AddSimulatorBackupStorageAction.Result res = action.call();
            throwExceptionIfNeed(res.error);

            rets.add(JSONObjectUtil.rehashObject(res.value.getInventory(), BackupStorageInventory.class));
        }

        return rets;
    }

    public BackupStorageInventory changeBackupStorageState(String uuid, BackupStorageStateEvent event) throws ApiSenderException {
        ChangeBackupStorageStateAction action = new ChangeBackupStorageStateAction();
        action.uuid = uuid;
        action.stateEvent = event.toString();
        action.sessionId = adminSession.getUuid();
        ChangeBackupStorageStateAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, BackupStorageInventory.class);
    }

    public void deleteBackupStorage(String uuid) throws ApiSenderException {
        DeleteBackupStorageAction action = new DeleteBackupStorageAction();
        action.uuid = uuid;
        action.sessionId = adminSession.getUuid();
        DeleteBackupStorageAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public BackupStorageInventory attachBackupStorage(String zoneUuid, String uuid) throws ApiSenderException {
        AttachBackupStorageToZoneAction action = new AttachBackupStorageToZoneAction();
        action.zoneUuid = zoneUuid;
        action.backupStorageUuid = uuid;
        action.sessionId = adminSession.getUuid();
        AttachBackupStorageToZoneAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, BackupStorageInventory.class);
    }

    public BackupStorageInventory detachBackupStorage(String uuid, String zoneUuid) throws ApiSenderException {
        DetachBackupStorageFromZoneAction action = new DetachBackupStorageFromZoneAction();
        action.sessionId = adminSession.getUuid();
        action.zoneUuid = zoneUuid;
        action.backupStorageUuid = uuid;
        DetachBackupStorageFromZoneAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, BackupStorageInventory.class);
    }

    public ImageInventory addImage(ImageInventory inv, String... bsUuids) throws ApiSenderException {
        return addImage(inv, null, bsUuids);
    }

    public ImageInventory addImage(ImageInventory inv, SessionInventory session, String... bsUuids) throws ApiSenderException {
        AddImageAction action = new AddImageAction();
        action.resourceUuid = inv.getUuid();
        action.sessionId = session == null ? adminSession.getUuid() : session.getUuid();
        action.description = inv.getDescription();
        action.mediaType = inv.getMediaType();
        action.guestOsType = inv.getGuestOsType();
        action.format = inv.getFormat();
        action.name = inv.getName();
        action.backupStorageUuids = asList(bsUuids);
        action.url = inv.getUrl();
        action.type = ImageConstant.ZSTACK_IMAGE_TYPE;
        AddImageAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, ImageInventory.class);
    }

    public void deleteImage(String uuid, List<String> bsUuids) throws ApiSenderException {
        deleteImage(uuid, bsUuids, null);
    }

    public void deleteImage(String uuid, List<String> bsUuids, SessionInventory session) throws ApiSenderException {
        DeleteImageAction action = new DeleteImageAction();
        action.uuid = uuid;
        action.backupStorageUuids = bsUuids;
        action.sessionId = session == null ? adminSession.getUuid() : session.getUuid();
        DeleteImageAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public void deleteImage(String uuid, SessionInventory session) throws ApiSenderException {
        deleteImage(uuid, null, session);
    }

    public void deleteImage(String uuid) throws ApiSenderException {
        deleteImage(uuid, null, null);
    }

    public ImageInventory syncImageSize(String imageUuid, SessionInventory session) throws ApiSenderException {
        SyncImageSizeAction action = new SyncImageSizeAction();
        action.uuid = imageUuid;
        action.sessionId = session == null ? adminSession.getUuid() : session.getUuid();
        SyncImageSizeAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, ImageInventory.class);
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

    private String getSessionUuid(SessionInventory session) {
        return session == null ? adminSession.getUuid() : session.getUuid();
    }

    public InstanceOfferingInventory changeInstanceOfferingState(String uuid, InstanceOfferingStateEvent sevt, SessionInventory session) throws ApiSenderException {
        ChangeInstanceOfferingStateAction action = new ChangeInstanceOfferingStateAction();
        action.uuid = uuid;
        action.stateEvent = sevt.toString();
        action.sessionId = getSessionUuid(session);
        ChangeInstanceOfferingStateAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, InstanceOfferingInventory.class);
    }

    public InstanceOfferingInventory addInstanceOffering(InstanceOfferingInventory inv) throws ApiSenderException {
        return addInstanceOffering(inv, null);
    }

    public InstanceOfferingInventory addInstanceOffering(InstanceOfferingInventory inv, SessionInventory session) throws ApiSenderException {
        CreateInstanceOfferingAction action = new CreateInstanceOfferingAction();
        action.sessionId = session == null ? adminSession.getUuid() : session.getUuid();
        action.name = inv.getName();
        action.cpuNum = inv.getCpuNum();
        action.memorySize = inv.getMemorySize();
        action.description = inv.getDescription();
        action.allocatorStrategy = inv.getAllocatorStrategy();
        CreateInstanceOfferingAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), InstanceOfferingInventory.class);
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
        DeleteInstanceOfferingAction action = new DeleteInstanceOfferingAction();
        action.sessionId = getSessionUuid(session);
        action.uuid = uuid;
        DeleteInstanceOfferingAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public DiskOfferingInventory addDiskOffering(DiskOfferingInventory inv) throws ApiSenderException {
        CreateDiskOfferingAction action = new CreateDiskOfferingAction();
        action.sessionId = getSessionUuid(adminSession);
        action.name = inv.getName();
        action.diskSize = inv.getDiskSize();
        action.description = inv.getDescription();
        CreateDiskOfferingAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, DiskOfferingInventory.class);
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
        DeleteDiskOfferingAction action = new DeleteDiskOfferingAction();
        action.sessionId = getSessionUuid(session);
        action.uuid = uuid;
        DeleteDiskOfferingAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
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
        CreateDataVolumeAction action = new CreateDataVolumeAction();
        action.sessionId = getSessionUuid(session);
        action.primaryStorageUuid = primaryStorageUuid;
        action.name = name;
        action.diskOfferingUuid = diskOfferingUuid;
        CreateDataVolumeAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, VolumeInventory.class);
    }

    public VolumeInventory changeVolumeState(String uuid, VolumeStateEvent stateEvent) throws ApiSenderException {
        return changeVolumeState(uuid, stateEvent, null);
    }

    public VolumeInventory changeVolumeState(String uuid, VolumeStateEvent stateEvent, SessionInventory session) throws ApiSenderException {
        ChangeVolumeStateAction action = new ChangeVolumeStateAction();
        action.sessionId = getSessionUuid(session);
        action.uuid = uuid;
        action.stateEvent = stateEvent.toString();
        ChangeVolumeStateAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, VolumeInventory.class);
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
        DeleteDataVolumeAction action = new DeleteDataVolumeAction();
        action.sessionId = getSessionUuid(session);
        action.uuid = uuid;
        DeleteDataVolumeAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
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
        UpdateGlobalConfigAction action = new UpdateGlobalConfigAction();
        action.sessionId = getSessionUuid(adminSession);
        action.category = inv.getCategory();
        action.name = inv.getName();
        action.value = inv.getValue();
        UpdateGlobalConfigAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, GlobalConfigInventory.class);
    }

    public L2NetworkInventory createNoVlanL2Network(String zoneUuid, String iface) throws ApiSenderException {
        CreateL2NoVlanNetworkAction action = new CreateL2NoVlanNetworkAction();
        action.sessionId = getSessionUuid(adminSession);
        action.name = "test l2";
        action.description = "test";
        action.zoneUuid = zoneUuid;
        action.physicalInterface = iface;
        action.type = L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE;
        CreateL2NoVlanNetworkAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, L2NetworkInventory.class);
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

        DeleteL2NetworkAction action = new DeleteL2NetworkAction();
        action.sessionId = getSessionUuid(adminSession);
        action.uuid = uuid;
        DeleteL2NetworkAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public L2NetworkInventory attachL2NetworkToCluster(String l2NetworkUuid, String clusterUuid) throws ApiSenderException {
        AttachL2NetworkToClusterAction action = new AttachL2NetworkToClusterAction();
        action.sessionId = getSessionUuid(adminSession);
        action.l2NetworkUuid = l2NetworkUuid;
        action.clusterUuid = clusterUuid;
        AttachL2NetworkToClusterAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, L2NetworkInventory.class);
    }

    public void detachL2NetworkFromCluster(String l2NetworkUuid, String clusterUuid) throws ApiSenderException {
        DetachL2NetworkFromClusterAction action = new DetachL2NetworkFromClusterAction();
        action.l2NetworkUuid = l2NetworkUuid;
        action.clusterUuid = clusterUuid;
        action.sessionId = getSessionUuid(adminSession);
        DetachL2NetworkFromClusterAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public List<L3NetworkInventory> getInterdependentL3NetworksByImageUuid(String imageUuid, String zoneUuid, SessionInventory session) throws ApiSenderException {
        GetInterdependentL3NetworksImagesAction action = new GetInterdependentL3NetworksImagesAction();
        action.imageUuid = imageUuid;
        action.zoneUuid = zoneUuid;
        action.sessionId = getSessionUuid(session);
        GetInterdependentL3NetworksImagesAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.toCollection(
                JSONObjectUtil.toJsonString(res.value.getInventories()),
                ArrayList.class,
                L3NetworkInventory.class
        );
    }

    public L3NetworkInventory createL3BasicNetwork(String l2NetworkUuid) throws ApiSenderException {
        return createL3BasicNetwork(l2NetworkUuid, null);
    }

    public L3NetworkInventory createL3BasicNetwork(String l2NetworkUuid, SessionInventory session) throws ApiSenderException {
        CreateL3NetworkAction action = new CreateL3NetworkAction();
        action.sessionId = getSessionUuid(session);
        action.l2NetworkUuid = l2NetworkUuid;
        action.type = L3NetworkConstant.L3_BASIC_NETWORK_TYPE;
        action.name = "Test-L3Network";
        action.description = "test";
        CreateL3NetworkAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
        return JSONObjectUtil.rehashObject(res.value.inventory, L3NetworkInventory.class);
    }

    public L3NetworkInventory changeL3NetworkState(String uuid, L3NetworkStateEvent sevnt) throws ApiSenderException {
        return changeL3NetworkState(uuid, sevnt, null);
    }

    public L3NetworkInventory changeL3NetworkState(String uuid, L3NetworkStateEvent sevnt, SessionInventory session) throws ApiSenderException {
        ChangeL3NetworkStateAction action = new ChangeL3NetworkStateAction();
        action.sessionId = getSessionUuid(session);
        action.stateEvent = sevnt.toString();
        action.uuid = uuid;
        ChangeL3NetworkStateAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
        return JSONObjectUtil.rehashObject(res.value.inventory, L3NetworkInventory.class);
    }

    public void deleteL3Network(String uuid) throws ApiSenderException {
        deleteL3Network(uuid, null);
    }

    public void deleteL3Network(String uuid, SessionInventory session) throws ApiSenderException {
        DeleteL3NetworkAction action = new DeleteL3NetworkAction();
        action.sessionId = getSessionUuid(session);
        action.uuid = uuid;
        DeleteL3NetworkAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
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
        GetIpAddressCapacityAction action = new GetIpAddressCapacityAction();
        action.sessionId = getSessionUuid(adminSession);
        action.ipRangeUuids = iprUuids;
        action.l3NetworkUuids = l3Uuids;
        action.zoneUuids = zoneUuids;
        GetIpAddressCapacityAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        APIGetIpAddressCapacityReply reply = new APIGetIpAddressCapacityReply();
        reply.setTotalCapacity(res.value.totalCapacity);
        reply.setAvailableCapacity(res.value.availableCapacity);
        return reply;
    }

    public APIGetIpAddressCapacityReply getIpAddressCapacityByAll() throws ApiSenderException {
        return getIpAddressCapacityByAll(null);
    }

    public APIGetIpAddressCapacityReply getIpAddressCapacityByAll(SessionInventory session) throws ApiSenderException {
        GetIpAddressCapacityAction action = new GetIpAddressCapacityAction();
        action.sessionId = getSessionUuid(adminSession);
        action.all = true;
        GetIpAddressCapacityAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        APIGetIpAddressCapacityReply reply = new APIGetIpAddressCapacityReply();
        reply.setTotalCapacity(res.value.totalCapacity);
        reply.setAvailableCapacity(res.value.availableCapacity);
        return reply;
    }

    public IpRangeInventory addIpRangeByCidr(String l3NetworkUuid, String cidr) throws ApiSenderException {
        return addIpRangeByCidr(l3NetworkUuid, cidr, null);
    }

    public IpRangeInventory addIpRangeByCidr(String l3NetworkUuid, String cidr, SessionInventory session) throws ApiSenderException {
        AddIpRangeByNetworkCidrAction action = new AddIpRangeByNetworkCidrAction();
        action.sessionId = getSessionUuid(adminSession);
        action.l3NetworkUuid = l3NetworkUuid;
        action.networkCidr = cidr;
        action.name = "TestIpRange";
        action.description = "test";
        AddIpRangeByNetworkCidrAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
        return JSONObjectUtil.rehashObject(res.value.inventory, IpRangeInventory.class);
    }

    public IpRangeInventory addIpRange(String l3NetworkUuid, String startIp, String endIp, String gateway, String netmask) throws ApiSenderException {
        return addIpRange(l3NetworkUuid, startIp, endIp, gateway, netmask, null);
    }

    public IpRangeInventory addIpRange(String l3NetworkUuid, String startIp, String endIp, String gateway, String netmask, SessionInventory session) throws ApiSenderException {
        AddIpRangeAction action = new AddIpRangeAction();
        action.sessionId = getSessionUuid(session == null ? adminSession : session);
        action.l3NetworkUuid = l3NetworkUuid;
        action.startIp = startIp;
        action.endIp = endIp;
        action.netmask = netmask;
        action.gateway = gateway;
        action.name = "TestIpRange";
        action.description = "test";
        AddIpRangeAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, IpRangeInventory.class);
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
        CheckIpAvailabilityAction action = new CheckIpAvailabilityAction();
        action.ip = ip;
        action.l3NetworkUuid = l3Uuid;
        action.sessionId = getSessionUuid(session);
        CheckIpAvailabilityAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return res.value.available;
    }

    public List<FreeIpInventory> getFreeIp(String l3Uuid, String ipRangeUuid, int limit, String start, SessionInventory session) throws ApiSenderException {
        if (l3Uuid != null) {
            GetFreeIpOfL3NetworkAction action = new GetFreeIpOfL3NetworkAction();
            action.sessionId = getSessionUuid(session);
            action.l3NetworkUuid = l3Uuid;
            action.limit = limit;
            action.start = start;
            GetFreeIpOfL3NetworkAction.Result res = action.call();
            throwExceptionIfNeed(res.error);

            return JSONObjectUtil.toCollection(
                    JSONObjectUtil.toJsonString(res.value.inventories),
                    ArrayList.class,
                    FreeIpInventory.class
            );
        } else {
            GetFreeIpOfIpRangeAction action = new GetFreeIpOfIpRangeAction();
            action.sessionId = getSessionUuid(session);
            action.ipRangeUuid = ipRangeUuid;
            action.limit = limit;
            action.start = start;
            GetFreeIpOfIpRangeAction.Result res = action.call();
            throwExceptionIfNeed(res.error);

            return JSONObjectUtil.toCollection(
                    JSONObjectUtil.toJsonString(res.value.inventories),
                    ArrayList.class,
                    FreeIpInventory.class
            );
        }
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
        DeleteIpRangeAction action = new DeleteIpRangeAction();
        action.sessionId = getSessionUuid(session);
        action.uuid = uuid;
        DeleteIpRangeAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }


    public L3NetworkInventory removeDnsFromL3Network(String dns, String l3NetworkUuid) throws ApiSenderException {
        return removeDnsFromL3Network(dns, l3NetworkUuid, null);
    }

    public L3NetworkInventory removeDnsFromL3Network(String dns, String l3NetworkUuid, SessionInventory session) throws ApiSenderException {
        RemoveDnsFromL3NetworkAction action = new RemoveDnsFromL3NetworkAction();
        action.dns = dns;
        action.l3NetworkUuid = l3NetworkUuid;
        action.sessionId = getSessionUuid(session);
        RemoveDnsFromL3NetworkAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, L3NetworkInventory.class);
    }

    public ZoneInventory createZoneByFullConfig(ZoneInventory inv) throws ApiSenderException {
        CreateZoneAction action = new CreateZoneAction();
        action.sessionId = adminSession.getUuid();
        action.name = inv.getName();
        action.description = inv.getDescription();
        CreateZoneAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), ZoneInventory.class);
    }

    public ClusterInventory createClusterByFullConfig(ClusterInventory inv) throws ApiSenderException {
        CreateClusterAction action = new CreateClusterAction();
        action.sessionId = getSessionUuid(adminSession);
        action.name = inv.getName();
        action.description = inv.getDescription();
        action.hypervisorType = inv.getHypervisorType();
        action.type = inv.getType();
        action.zoneUuid = inv.getZoneUuid();
        CreateClusterAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, ClusterInventory.class);
    }

    public HostInventory addHostByFullConfig(HostInventory inv) throws ApiSenderException {
        AddSimulatorHostAction action = new AddSimulatorHostAction();
        action.sessionId = adminSession.getUuid();
        action.description = inv.getDescription();
        action.name = inv.getName();
        action.managementIp = inv.getManagementIp();
        action.memoryCapacity = inv.getAvailableMemoryCapacity();
        action.cpuCapacity = inv.getAvailableCpuCapacity();
        action.clusterUuid = inv.getClusterUuid();
        AddSimulatorHostAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), HostInventory.class);
    }

    public APIGetPrimaryStorageCapacityReply getPrimaryStorageCapacityByAll() throws ApiSenderException {
        GetPrimaryStorageCapacityAction action = new GetPrimaryStorageCapacityAction();
        action.sessionId = getSessionUuid(adminSession);
        action.all = true;
        GetPrimaryStorageCapacityAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        APIGetPrimaryStorageCapacityReply reply = new APIGetPrimaryStorageCapacityReply();
        reply.setAvailableCapacity(res.value.availableCapacity);
        reply.setTotalCapacity(res.value.totalCapacity);

        return reply;
    }

    public APIGetPrimaryStorageCapacityReply getPrimaryStorageCapacity(List<String> zoneUuids, List<String> clusterUuids, List<String> psUuids) throws ApiSenderException {

        GetPrimaryStorageCapacityAction action = new GetPrimaryStorageCapacityAction();
        action.sessionId = getSessionUuid(adminSession);
        action.zoneUuids = zoneUuids;
        action.clusterUuids = clusterUuids;
        action.primaryStorageUuids = psUuids;
        GetPrimaryStorageCapacityAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        APIGetPrimaryStorageCapacityReply reply = new APIGetPrimaryStorageCapacityReply();
        reply.setAvailableCapacity(res.value.availableCapacity);
        reply.setTotalCapacity(res.value.totalCapacity);

        return reply;
    }

    public PrimaryStorageInventory addPrimaryStorageByFullConfig(PrimaryStorageInventory inv) throws ApiSenderException {
        AddSimulatorPrimaryStorageAction action = new AddSimulatorPrimaryStorageAction();

        action.sessionId = adminSession.getUuid();
        action.name = inv.getName();
        action.description = inv.getDescription();
        action.type = SimulatorPrimaryStorageConstant.SIMULATOR_PRIMARY_STORAGE_TYPE;
        action.url = inv.getUrl();
        action.totalCapacity = inv.getTotalCapacity();
        action.availableCapacity = inv.getAvailableCapacity();
        action.zoneUuid = inv.getZoneUuid();
        AddSimulatorPrimaryStorageAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), PrimaryStorageInventory.class);
    }

    public APIGetBackupStorageCapacityReply getBackupStorageCapacityByAll() throws ApiSenderException {
        GetBackupStorageCapacityAction action = new GetBackupStorageCapacityAction();
        action.sessionId = getSessionUuid(adminSession);
        action.all = true;
        GetBackupStorageCapacityAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        APIGetBackupStorageCapacityReply reply = new APIGetBackupStorageCapacityReply();
        reply.setAvailableCapacity(res.value.availableCapacity);
        reply.setTotalCapacity(res.value.totalCapacity);

        return reply;
    }

    public APIGetBackupStorageCapacityReply getBackupStorageCapacity(List<String> zoneUuids, List<String> bsUuids) throws ApiSenderException {
        GetBackupStorageCapacityAction action = new GetBackupStorageCapacityAction();
        action.sessionId = getSessionUuid(adminSession);
        action.zoneUuids = zoneUuids;
        action.backupStorageUuids = bsUuids;
        GetBackupStorageCapacityAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        APIGetBackupStorageCapacityReply reply = new APIGetBackupStorageCapacityReply();
        reply.setAvailableCapacity(res.value.availableCapacity);
        reply.setTotalCapacity(res.value.totalCapacity);

        return reply;
    }

    public BackupStorageInventory addBackupStorageByFullConfig(BackupStorageInventory inv) throws ApiSenderException {
        AddSimulatorBackupStorageAction action = new AddSimulatorBackupStorageAction();
        action.sessionId = adminSession.getUuid();
        action.name = inv.getName();
        action.description = inv.getDescription();
        action.totalCapacity = inv.getTotalCapacity();
        action.url = inv.getUrl();
        action.availableCapacity = inv.getAvailableCapacity();
        action.type = SimulatorBackupStorageConstant.SIMULATOR_BACKUP_STORAGE_TYPE;
        AddSimulatorBackupStorageAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), BackupStorageInventory.class);
    }


    public L2VlanNetworkInventory createL2VlanNetworkByFullConfig(L2VlanNetworkInventory inv) throws ApiSenderException {
        CreateL2VlanNetworkAction action = new CreateL2VlanNetworkAction();
        action.sessionId = adminSession.getUuid();
        action.description = inv.getDescription();
        action.name = inv.getName();
        action.physicalInterface = inv.getPhysicalInterface();
        action.type = inv.getType();
        action.zoneUuid = inv.getZoneUuid();
        action.vlan = inv.getVlan();
        CreateL2VlanNetworkAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), L2VlanNetworkInventory.class);
    }

    public L2NetworkInventory createL2NetworkByFullConfig(L2NetworkInventory inv) throws ApiSenderException {
        CreateL2NoVlanNetworkAction action = new CreateL2NoVlanNetworkAction();
        action.sessionId = adminSession.getUuid();
        action.description = inv.getDescription();
        action.name = inv.getName();
        action.physicalInterface = inv.getPhysicalInterface();
        action.type = inv.getType();
        action.zoneUuid = inv.getZoneUuid();
        CreateL2NoVlanNetworkAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), L2NetworkInventory.class);
    }

    public L3NetworkInventory createL3NetworkByFullConfig(L3NetworkInventory inv) throws ApiSenderException {
        return createL3NetworkByFullConfig(inv, adminSession);
    }

    public L3NetworkInventory createL3NetworkByFullConfig(L3NetworkInventory inv, SessionInventory session) throws ApiSenderException {
        CreateL3NetworkAction action = new CreateL3NetworkAction();
        action.sessionId = session.getUuid();
        action.description = inv.getDescription();
        action.l2NetworkUuid = inv.getL2NetworkUuid();
        action.name = inv.getName();
        action.dnsDomain = inv.getDnsDomain();
        action.type = inv.getType();
        CreateL3NetworkAction.Result res = action.call();

        return JSONObjectUtil.rehashObject(res.value.getInventory(), L3NetworkInventory.class);
    }

    public IpRangeInventory addIpRangeByFullConfig(IpRangeInventory inv, SessionInventory session) throws ApiSenderException {
        AddIpRangeAction action = new AddIpRangeAction();
        action.name = inv.getName();
        action.sessionId = session.getUuid();
        action.l3NetworkUuid = inv.getL3NetworkUuid();
        action.startIp = inv.getStartIp();
        action.endIp = inv.getEndIp();
        action.netmask = inv.getNetmask();
        action.gateway = inv.getGateway();
        action.description = inv.getDescription();
        AddIpRangeAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), IpRangeInventory.class);
    }

    public IpRangeInventory addIpRangeByFullConfig(IpRangeInventory inv) throws ApiSenderException {
        return addIpRangeByFullConfig(inv, adminSession);
    }

    public DiskOfferingInventory addDiskOfferingByFullConfig(DiskOfferingInventory inv) throws ApiSenderException {
        return addDiskOfferingByFullConfig(inv, null);
    }

    public DiskOfferingInventory addDiskOfferingByFullConfig(DiskOfferingInventory inv, SessionInventory session) throws ApiSenderException {
        CreateDiskOfferingAction action = new CreateDiskOfferingAction();
        action.sessionId = getSessionUuid(session);
        action.name = inv.getName();
        action.diskSize = inv.getDiskSize();
        action.description = inv.getDescription();
        action.allocationStrategy = inv.getAllocatorStrategy();
        CreateDiskOfferingAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, DiskOfferingInventory.class);
    }

    public VmInstanceInventory createVmFromClone(VmInstanceInventory toClone) throws ApiSenderException {

        CreateVmInstanceAction action = new CreateVmInstanceAction();
        action.sessionId = getSessionUuid(adminSession);
        action.name = String.format("clone-%s", toClone.getName());
        action.imageUuid = toClone.getImageUuid();
        action.dataDiskOfferingUuids = toClone.getAllVolumes().stream()
                .filter(v -> v.getType().equals(VolumeType.Data.toString()))
                .map(VolumeInventory::getDiskOfferingUuid).collect(Collectors.toList());
        action.l3NetworkUuids = toClone.getVmNics().stream().map(VmNicInventory::getL3NetworkUuid).collect(Collectors.toList());
        action.defaultL3NetworkUuid = toClone.getDefaultL3NetworkUuid();
        action.type = toClone.getType();
        action.instanceOfferingUuid = toClone.getInstanceOfferingUuid();
        action.description = String.format("clone from vm[uuid:%s]", toClone.getUuid());
        CreateVmInstanceAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, VmInstanceInventory.class);
    }

    public VmInstanceInventory createVmByFullConfig(VmInstanceInventory inv, String rootDiskOfferingUuid, List<String> l3NetworkUuids,
                                                    List<String> diskOfferingUuids, SessionInventory session) throws ApiSenderException {
        VmCreator creator = new VmCreator(this);
        creator.clusterUUid = inv.getClusterUuid();
        creator.diskOfferingUuids = diskOfferingUuids;
        creator.session = session;
        creator.description = inv.getDescription();
        creator.hostUuid = inv.getHostUuid();
        creator.imageUuid = inv.getImageUuid();
        creator.instanceOfferingUuid = inv.getInstanceOfferingUuid();
        creator.l3NetworkUuids = l3NetworkUuids;
        creator.name = inv.getName();
        creator.zoneUuid = inv.getZoneUuid();
        creator.rootDiskOfferingUuid = rootDiskOfferingUuid;
        creator.defaultL3NetworkUuid = inv.getDefaultL3NetworkUuid();
        if (creator.defaultL3NetworkUuid == null && creator.l3NetworkUuids.size() > 1) {
            creator.defaultL3NetworkUuid = creator.l3NetworkUuids.get(0);
        }

        return creator.create();
    }

    public VmInstanceInventory createVmByFullConfigWithSpecifiedPS(VmInstanceInventory inv,
                                                                   String rootDiskOfferingUuid,
                                                                   List<String> l3NetworkUuids,
                                                                   List<String> diskOfferingUuids,
                                                                   List<String> sysTags,
                                                                   String psUuid,
                                                                   SessionInventory session) throws ApiSenderException {

        VmCreator creator = new VmCreator(this);
        creator.zoneUuid = inv.getZoneUuid();
        creator.clusterUUid = inv.getClusterUuid();
        creator.hostUuid = inv.getHostUuid();
        creator.diskOfferingUuids = diskOfferingUuids;
        creator.session = session;
        creator.description = inv.getDescription();
        creator.hostUuid = inv.getHostUuid();
        creator.imageUuid = inv.getImageUuid();
        creator.instanceOfferingUuid = inv.getInstanceOfferingUuid();
        creator.l3NetworkUuids = l3NetworkUuids;
        creator.name = inv.getName();
        creator.rootDiskOfferingUuid = rootDiskOfferingUuid;
        creator.defaultL3NetworkUuid = inv.getDefaultL3NetworkUuid();
        if (creator.defaultL3NetworkUuid == null && creator.l3NetworkUuids.size() > 1) {
            creator.defaultL3NetworkUuid = creator.l3NetworkUuids.get(0);
        }
        creator.primaryStorageUuidForRootVolume = psUuid;

        return creator.create();
    }

    public VmInstanceInventory changeInstanceOffering(String vmUuid, String instanceOfferingUuid) throws ApiSenderException {
        ChangeInstanceOfferingAction action = new ChangeInstanceOfferingAction();
        action.vmInstanceUuid = vmUuid;
        action.sessionId = getSessionUuid(adminSession);
        action.instanceOfferingUuid = instanceOfferingUuid;
        ChangeInstanceOfferingAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, VmInstanceInventory.class);
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
        StopVmInstanceAction action = new StopVmInstanceAction();
        action.uuid = uuid;
        action.sessionId = getSessionUuid(session);
        StopVmInstanceAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, VmInstanceInventory.class);
    }

    public VmInstanceInventory forcefullyStopVmInstance(String uuid) throws ApiSenderException {
        return forcefullyStopVmInstance(uuid, null);
    }

    public VmInstanceInventory forcefullyStopVmInstance(String uuid, SessionInventory session) throws ApiSenderException {
        StopVmInstanceAction action = new StopVmInstanceAction();
        action.uuid = uuid;
        action.sessionId = getSessionUuid(session);
        action.type = "cold";
        StopVmInstanceAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, VmInstanceInventory.class);
    }

    public VmAccountPreference changeVmPassword(VmAccountPreference account)
            throws ApiSenderException {

        ChangeVmPasswordAction action = new ChangeVmPasswordAction();
        action.sessionId = getSessionUuid(adminSession);
        action.uuid = account.getVmUuid();
        action.account = account.getUserAccount();
        action.password = account.getAccountPassword();
        ChangeVmPasswordAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
        return account;
    }

    public VmInstanceInventory pauseVmInstance(String uuid) throws ApiSenderException {
        return pauseVmInstance(uuid, null);
    }

    public VmInstanceInventory pauseVmInstance(String uuid, SessionInventory session) throws ApiSenderException {
        APIPauseVmInstanceMsg msg = new APIPauseVmInstanceMsg();

        PauseVmInstanceAction action = new PauseVmInstanceAction();
        action.sessionId = getSessionUuid(session);
        action.uuid = uuid;
        PauseVmInstanceAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
        return JSONObjectUtil.rehashObject(res.value.inventory, VmInstanceInventory.class);
    }

    public VmInstanceInventory resumeVmInstance(String uuid) throws ApiSenderException {
        return resumeVmInstance(uuid, null);
    }

    public VmInstanceInventory resumeVmInstance(String uuid, SessionInventory session) throws ApiSenderException {
        ResumeVmInstanceAction action = new ResumeVmInstanceAction();
        action.sessionId = getSessionUuid(session);
        action.uuid = uuid;
        ResumeVmInstanceAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, VmInstanceInventory.class);
    }

    public VmInstanceInventory rebootVmInstance(String uuid) throws ApiSenderException {
        return rebootVmInstance(uuid, null);
    }

    public VmInstanceInventory rebootVmInstance(String uuid, SessionInventory session) throws ApiSenderException {
        RebootVmInstanceAction action = new RebootVmInstanceAction();
        action.uuid = uuid;
        action.sessionId = getSessionUuid(session);
        RebootVmInstanceAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
        return JSONObjectUtil.rehashObject(res.value.inventory, VmInstanceInventory.class);
    }

    public void destroyVmInstance(String uuid) throws ApiSenderException {
        destroyVmInstance(uuid, null);
    }

    public void destroyVmInstance(String uuid, SessionInventory session) throws ApiSenderException {
        APIDestroyVmInstanceMsg msg = new APIDestroyVmInstanceMsg();

        DestroyVmInstanceAction action = new DestroyVmInstanceAction();
        action.sessionId = getSessionUuid(session);
        action.uuid = uuid;
        DestroyVmInstanceAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public VmInstanceInventory startVmInstance(String uuid) throws ApiSenderException {
        return startVmInstance(uuid, null);
    }

    public VmInstanceInventory startVmInstance(String uuid, SessionInventory session) throws ApiSenderException {
        StartVmInstanceAction action = new StartVmInstanceAction();
        action.uuid = uuid;
        action.sessionId = getSessionUuid(session);
        StartVmInstanceAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, VmInstanceInventory.class);
    }

    public VmInstanceInventory migrateVmInstance(String vmUuid, String destHostUuid) throws ApiSenderException {
        return migrateVmInstance(vmUuid, destHostUuid, null);
    }

    public VmInstanceInventory migrateVmInstance(String vmUuid, String destHostUuid, SessionInventory session) throws ApiSenderException {
        MigrateVmAction action = new MigrateVmAction();
        action.hostUuid = destHostUuid;
        action.vmInstanceUuid = vmUuid;
        action.sessionId = getSessionUuid(session);
        MigrateVmAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, VmInstanceInventory.class);
    }

    public List<VmInstanceInventory> getDataVolumeCandidateVmForAttaching(String volUuid) throws ApiSenderException {
        return getDataVolumeCandidateVmForAttaching(volUuid, null);
    }

    public List<VmInstanceInventory> getDataVolumeCandidateVmForAttaching(String volUuid, SessionInventory session) throws ApiSenderException {
        GetDataVolumeAttachableVmAction action = new GetDataVolumeAttachableVmAction();
        action.sessionId = getSessionUuid(session);
        action.volumeUuid = volUuid;
        GetDataVolumeAttachableVmAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.toCollection(
                JSONObjectUtil.toJsonString(res.value.inventories),
                ArrayList.class,
                VmInstanceInventory.class
        );
    }

    public List<VolumeInventory> getVmAttachableVolume(String vmUuid) throws ApiSenderException {
        return getVmAttachableVolume(vmUuid, null);
    }

    public List<VolumeInventory> getVmAttachableVolume(String vmUuid, SessionInventory session) throws ApiSenderException {
        GetVmAttachableDataVolumeAction action = new GetVmAttachableDataVolumeAction();
        action.sessionId = getSessionUuid(session);
        action.vmInstanceUuid = vmUuid;
        GetVmAttachableDataVolumeAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.toCollection(
                JSONObjectUtil.toJsonString(res.value.inventories),
                ArrayList.class,
                VolumeInventory.class
        );
    }

    public VolumeInventory attachVolumeToVm(String vmUuid, String volumeUuid) throws ApiSenderException {
        return attachVolumeToVm(vmUuid, volumeUuid, null);
    }

    public VolumeInventory attachVolumeToVm(String vmUuid, String volumeUuid, SessionInventory session) throws ApiSenderException {
        AttachDataVolumeToVmAction action = new AttachDataVolumeToVmAction();
        action.sessionId = getSessionUuid(session);
        action.vmInstanceUuid = vmUuid;
        action.volumeUuid = volumeUuid;
        AttachDataVolumeToVmAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, VolumeInventory.class);
    }

    public VolumeInventory createDataVolumeFromTemplate(String imageUuid, String primaryStorageUuid) throws ApiSenderException {
        return createDataVolumeFromTemplate(imageUuid, primaryStorageUuid, null);
    }

    public VolumeInventory createDataVolumeFromTemplate(String imageUuid, String primaryStorageUuid, SessionInventory session) throws ApiSenderException {
        CreateDataVolumeFromVolumeTemplateAction action = new CreateDataVolumeFromVolumeTemplateAction();
        action.sessionId = getSessionUuid(session);
        action.name = "data";
        action.imageUuid = imageUuid;
        action.primaryStorageUuid = primaryStorageUuid;
        CreateDataVolumeFromVolumeTemplateAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, VolumeInventory.class);
    }

    public ImageInventory addDataVolumeTemplateFromDataVolume(String volUuid, List<String> bsUuids) throws ApiSenderException {
        return addDataVolumeTemplateFromDataVolume(volUuid, bsUuids, null);
    }

    public ImageInventory addDataVolumeTemplateFromDataVolume(String volUuid, List<String> bsUuids, SessionInventory session) throws ApiSenderException {
        CreateDataVolumeTemplateFromVolumeAction action = new CreateDataVolumeTemplateFromVolumeAction();
        action.name = "data-volume";
        action.sessionId = getSessionUuid(session);
        action.backupStorageUuids = bsUuids;
        action.volumeUuid = volUuid;
        CreateDataVolumeTemplateFromVolumeAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), ImageInventory.class);
    }

    public List<VolumeFormatReplyStruct> getVolumeFormats() throws ApiSenderException {
        GetVolumeFormatAction action = new GetVolumeFormatAction();
        action.sessionId = getSessionUuid(adminSession);
        GetVolumeFormatAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.toCollection(
                JSONObjectUtil.toJsonString(res.value.formats),
                ArrayList.class,
                VolumeFormatReplyStruct.class
        );
    }

    public VolumeInventory detachVolumeFromVm(String volumeUuid) throws ApiSenderException {
        return detachVolumeFromVm(volumeUuid, null);
    }

    public VolumeInventory detachVolumeFromVm(String volumeUuid, SessionInventory session) throws ApiSenderException {
        DetachDataVolumeFromVmAction action = new DetachDataVolumeFromVmAction();
        action.sessionId = getSessionUuid(session);
        action.uuid = volumeUuid;
        DetachDataVolumeFromVmAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, VolumeInventory.class);
    }

    public VolumeInventory detachVolumeFromVmEx(String volumeUuid, String vmUuid, SessionInventory session) throws ApiSenderException {
        APIDetachDataVolumeFromVmMsg msg = new APIDetachDataVolumeFromVmMsg();
        msg.setSession(session == null ? adminSession : session);
        msg.setUuid(volumeUuid);
        msg.setVmUuid(vmUuid);
        ApiSender sender = new ApiSender();
        sender.setTimeout(timeout);
        APIDetachDataVolumeFromVmEvent evt = sender.send(msg, APIDetachDataVolumeFromVmEvent.class);
        return evt.getInventory();
    }

    public SessionInventory loginAsAdmin() throws ApiSenderException {
        return loginByAccount(AccountConstant.INITIAL_SYSTEM_ADMIN_NAME, AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD);
    }

    public SessionInventory loginByAccount(String accountName, String password) throws ApiSenderException {
        LogInByAccountAction a = new LogInByAccountAction();
        a.accountName = accountName;
        a.password = password;
        LogInByAccountAction.Result res = a.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, SessionInventory.class);
    }

    public SessionInventory loginByUserAccountName(String userName, String password, String accountName) throws ApiSenderException {
        LogInByUserAction a = new LogInByUserAction();
        a.accountName = accountName;
        a.userName = userName;
        a.password = password;
        LogInByUserAction.Result res = a.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, SessionInventory.class);
    }

    public SessionInventory loginByUser(String userName, String password, String accountUuid) throws ApiSenderException {
        LogInByUserAction a = new LogInByUserAction();
        a.accountUuid = accountUuid;
        a.userName = userName;
        a.password = password;
        LogInByUserAction.Result res = a.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, SessionInventory.class);
    }

    public void logout(String sessionUuid) throws ApiSenderException {
        LogOutAction action = new LogOutAction();
        action.sessionUuid = sessionUuid;
        LogOutAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public boolean validateSession(String sessionUuid) throws ApiSenderException {
        ValidateSessionAction action = new ValidateSessionAction();
        action.sessionUuid = sessionUuid;
        ValidateSessionAction.Result res = action.call();
        return res.value.valid;
    }

    public AccountInventory createAccount(String name, String password) throws ApiSenderException {
        CreateAccountAction action = new CreateAccountAction();
        action.sessionId = getSessionUuid(adminSession);
        action.name = name;
        action.password = password;
        CreateAccountAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
        return JSONObjectUtil.rehashObject(res.value.inventory, AccountInventory.class);
    }

    public QuotaInventory updateQuota(String identityUuid, String name, long value) throws ApiSenderException {
        UpdateQuotaAction action = new UpdateQuotaAction();
        action.identityUuid = identityUuid;
        action.name = name;
        action.value = value;
        action.sessionId = getSessionUuid(adminSession);
        UpdateQuotaAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, QuotaInventory.class);
    }

    public QuotaInventory getQuota(String name, String accountUuid, SessionInventory session) throws ApiSenderException {
        QueryQuotaAction action = new QueryQuotaAction();
        action.conditions = Arrays.asList(
                String.format("identityUuid=%s", accountUuid),
                String.format("name=%s", name)
        );
        action.sessionId = getSessionUuid(session);
        QueryQuotaAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return res.value.inventories.isEmpty() ? null : JSONObjectUtil.rehashObject(res.value.inventories.get(0), QuotaInventory.class);
    }

    public List<Quota.QuotaUsage> getQuotaUsage(String accountUuid, SessionInventory session) throws ApiSenderException {
        GetAccountQuotaUsageAction action = new GetAccountQuotaUsageAction();
        if (accountUuid != null) {
            action.uuid = accountUuid;
            action.sessionId = getSessionUuid(adminSession);
        } else {
            action.uuid = session.getAccountUuid();
            action.sessionId = getSessionUuid(session);
        }

        GetAccountQuotaUsageAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.toCollection(
                JSONObjectUtil.toJsonString(res.value.usages),
                ArrayList.class,
                Quota.QuotaUsage.class
        );
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
        UpdateAccountAction action = new UpdateAccountAction();
        action.uuid = uuid;
        action.password = password;
        action.sessionId = getSessionUuid(session);
        UpdateAccountAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, AccountInventory.class);
    }

    public UserInventory createUser(String accountUuid, String userName, String password, SessionInventory session) throws ApiSenderException {
        CreateUserAction action = new CreateUserAction();
        action.name = userName;
        action.password = password;
        action.sessionId = getSessionUuid(session);
        CreateUserAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, UserInventory.class);
    }

    public void resetUserPassword(String uuid, String password, SessionInventory session) throws ApiSenderException {
        UpdateUserAction action = new UpdateUserAction();
        action.uuid = uuid;
        action.password = password;
        action.sessionId = getSessionUuid(session);
        UpdateUserAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public PolicyInventory createPolicy(String name, List<Statement> s, SessionInventory session) throws ApiSenderException {
        CreatePolicyAction action = new CreatePolicyAction();
        action.name = name;
        action.statements = s;
        action.sessionId = getSessionUuid(session);
        CreatePolicyAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, PolicyInventory.class);
    }

    public void attachPolicyToUser(String userUuid, String policyUuid, SessionInventory session) throws ApiSenderException {
        AttachPolicyToUserAction action = new AttachPolicyToUserAction();
        action.sessionId = getSessionUuid(session);
        action.userUuid = userUuid;
        action.policyUuid = policyUuid;
        AttachPolicyToUserAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public void attachPolicesToUser(String userUuid, List<String> puuids, SessionInventory session) throws ApiSenderException {
        AttachPoliciesToUserAction action = new AttachPoliciesToUserAction();
        action.userUuid = userUuid;
        action.sessionId = getSessionUuid(session);
        action.policyUuids = puuids;
        AttachPoliciesToUserAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public void detachPolicyFromUser(String userUuid, String policyUuid, SessionInventory session) throws ApiSenderException {
        DetachPolicyFromUserAction action = new DetachPolicyFromUserAction();
        action.sessionId = getSessionUuid(session);
        action.userUuid = userUuid;
        action.policyUuid = policyUuid;
        DetachPolicyFromUserAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public void attachPolicyToUser(String accountUuid, String userUuid, String policyUuid, SessionInventory session) throws ApiSenderException {
        attachPolicyToUser(userUuid, policyUuid, session);
    }

    public void detachPoliciesFromUser(String userUuid, List<String> puuids, SessionInventory session) throws ApiSenderException {
        DetachPoliciesFromUserAction action = new DetachPoliciesFromUserAction();
        action.userUuid = userUuid;
        action.policyUuids = puuids;
        action.sessionId = getSessionUuid(session);
        DetachPoliciesFromUserAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public UserGroupInventory createGroup(String accountUuid, String name, SessionInventory session) throws ApiSenderException {
        CreateUserGroupAction action = new CreateUserGroupAction();
        action.sessionId = getSessionUuid(session);
        action.name = name;
        CreateUserGroupAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, UserGroupInventory.class);
    }

    public void deleteGroup(String uuid, SessionInventory session) throws ApiSenderException {
        DeleteUserAction action = new DeleteUserAction();
        action.sessionId = getSessionUuid(session);
        action.uuid = uuid;
        DeleteUserAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public void deleteAccount(String uuid, SessionInventory session) throws ApiSenderException {
        DeleteAccountAction action = new DeleteAccountAction();
        action.sessionId = getSessionUuid(session);
        action.uuid = uuid;
        DeleteAccountAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public void deleteUser(String uuid, SessionInventory session) throws ApiSenderException {
        DeleteUserAction action = new DeleteUserAction();
        action.sessionId = getSessionUuid(session);
        action.uuid = uuid;
        DeleteUserAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public void deletePolicy(String uuid, SessionInventory session) throws ApiSenderException {
        DeletePolicyAction action = new DeletePolicyAction();
        action.sessionId = getSessionUuid(session);
        action.uuid = uuid;
        DeletePolicyAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public void attachPolicyToGroup(String groupUuid, String policyUuid, SessionInventory session) throws ApiSenderException {
        APIAttachPolicyToUserGroupMsg msg = new APIAttachPolicyToUserGroupMsg();

        AttachPolicyToUserGroupAction action = new AttachPolicyToUserGroupAction();
        action.groupUuid = groupUuid;
        action.policyUuid = policyUuid;
        action.sessionId = getSessionUuid(session);
        AttachPolicyToUserGroupAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public void detachPolicyFromGroup(String groupUuid, String policyUuid, SessionInventory session) throws ApiSenderException {
        DetachPolicyFromUserGroupAction action = new DetachPolicyFromUserGroupAction();
        action.groupUuid = groupUuid;
        action.policyUuid = policyUuid;
        action.sessionId = getSessionUuid(session);
        DetachPolicyFromUserGroupAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public void attachPolicyToGroup(String accountUuid, String groupUuid, String policyUuid, SessionInventory session) throws ApiSenderException {
        attachPolicyToGroup(groupUuid, policyUuid, session);
    }

    public void addUserToGroup(String userUuid, String groupUuid, SessionInventory session) throws ApiSenderException {
        AddUserToGroupAction action = new AddUserToGroupAction();
        action.userUuid = userUuid;
        action.groupUuid = groupUuid;
        action.sessionId = getSessionUuid(session);
        AddUserToGroupAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public void removeUserFromGroup(String userUuid, String groupUuid, SessionInventory session) throws ApiSenderException {
        RemoveUserFromGroupAction action = new RemoveUserFromGroupAction();
        action.userUuid = userUuid;
        action.groupUuid = groupUuid;
        action.sessionId = getSessionUuid(session);
        RemoveUserFromGroupAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public void attachUserToGroup(String accountUuid, String userUuid, String groupUuid, SessionInventory session) throws ApiSenderException {
        AddUserToGroupAction action = new AddUserToGroupAction();
        action.userUuid = userUuid;
        action.groupUuid = groupUuid;
        AddUserToGroupAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
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
        AttachNetworkServiceToL3NetworkAction action = new AttachNetworkServiceToL3NetworkAction();
        Map<String, List<String>> ntypes = new HashMap<String, List<String>>(1);
        ntypes.put(providerUuid, types);
        action.l3NetworkUuid = l3NetworkUuid;
        action.networkServices = ntypes;
        action.sessionId = session == null ? adminSession.getUuid() : session.getUuid();
        AttachNetworkServiceToL3NetworkAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
        return JSONObjectUtil.rehashObject(res.value.getInventory(), L3NetworkInventory.class);
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
        AddDnsToL3NetworkAction action = new AddDnsToL3NetworkAction();
        action.l3NetworkUuid = l3NetworkUuid;
        action.dns = dns;
        action.sessionId = session == null ? adminSession.getUuid() : session.getUuid();
        AddDnsToL3NetworkAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
        return JSONObjectUtil.rehashObject(res.value.getInventory(), L3NetworkInventory.class);
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
        GetCpuMemoryCapacityAction action = new GetCpuMemoryCapacityAction();
        action.sessionId = getSessionUuid(adminSession);
        action.all = true;
        GetCpuMemoryCapacityAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value, APIGetCpuMemoryCapacityReply.class);
    }

    public APIGetCpuMemoryCapacityReply retrieveHostCapacity(List<String> zoneUuids, List<String> clusterUuids, List<String> hostUuids) throws ApiSenderException {
        GetCpuMemoryCapacityAction action = new GetCpuMemoryCapacityAction();
        action.sessionId = getSessionUuid(adminSession);
        action.zoneUuids = zoneUuids;
        action.clusterUuids = clusterUuids;
        action.hostUuids = hostUuids;
        GetCpuMemoryCapacityAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value, APIGetCpuMemoryCapacityReply.class);
    }

    public SecurityGroupInventory createSecurityGroup(String name) throws ApiSenderException {
        return createSecurityGroup(name, null);
    }

    public SecurityGroupInventory createSecurityGroup(String name, SessionInventory session) throws ApiSenderException {
        CreateSecurityGroupAction action = new CreateSecurityGroupAction();
        action.name = name;
        action.sessionId = getSessionUuid(session);
        CreateSecurityGroupAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
        return JSONObjectUtil.rehashObject(res.value.inventory, SecurityGroupInventory.class);
    }

    public SecurityGroupInventory changeSecurityGroupState(String uuid, SecurityGroupStateEvent sevt) throws ApiSenderException {
        return changeSecurityGroupState(uuid, sevt, null);
    }

    public SecurityGroupInventory changeSecurityGroupState(String uuid, SecurityGroupStateEvent sevt, SessionInventory session) throws ApiSenderException {
        ChangeSecurityGroupStateAction action = new ChangeSecurityGroupStateAction();
        action.uuid = uuid;
        action.stateEvent = sevt.toString();
        action.sessionId = getSessionUuid(session);
        ChangeSecurityGroupStateAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, SecurityGroupInventory.class);
    }

    public SecurityGroupInventory createSecurityGroupByFullConfig(SecurityGroupInventory inv, SessionInventory session) throws ApiSenderException {
        CreateSecurityGroupAction action = new CreateSecurityGroupAction();
        action.name = inv.getName();
        action.description = inv.getDescription();
        action.sessionId = session.getUuid();
        CreateSecurityGroupAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), SecurityGroupInventory.class);
    }

    public SecurityGroupInventory createSecurityGroupByFullConfig(SecurityGroupInventory inv) throws ApiSenderException {
        return createSecurityGroupByFullConfig(inv, adminSession);
    }

    public SecurityGroupInventory addSecurityGroupRuleByFullConfig(String securityGroupUuid, APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO ao) throws ApiSenderException {
        List<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO> aos = new ArrayList<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO>(1);
        aos.add(ao);
        return addSecurityGroupRuleByFullConfig(securityGroupUuid, aos);
    }

    public SecurityGroupInventory addSecurityGroupRuleByFullConfig(String securityGroupUuid, List<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO> aos) throws ApiSenderException {
        return addSecurityGroupRuleByFullConfig(securityGroupUuid, aos, adminSession);
    }

    public List<VmNicInventory> getCandidateVmNicFromSecurityGroup(String sgUuid) throws ApiSenderException {
        return getCandidateVmNicFromSecurityGroup(sgUuid, null);
    }

    public List<VmNicInventory> getCandidateVmNicFromSecurityGroup(String sgUuid, SessionInventory session) throws ApiSenderException {
        GetCandidateVmNicForSecurityGroupAction action = new GetCandidateVmNicForSecurityGroupAction();
        action.securityGroupUuid = sgUuid;
        action.sessionId = getSessionUuid(session);
        GetCandidateVmNicForSecurityGroupAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.toCollection(
                JSONObjectUtil.toJsonString(res.value.inventories),
                ArrayList.class,
                VmNicInventory.class
        );
    }

    public SecurityGroupInventory addSecurityGroupRuleByFullConfig(String securityGroupUuid, List<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO> aos, SessionInventory session)
            throws ApiSenderException {
        AddSecurityGroupRuleAction action = new AddSecurityGroupRuleAction();
        action.rules = aos;
        action.securityGroupUuid = securityGroupUuid;
        action.sessionId = session.getUuid();
        AddSecurityGroupRuleAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), SecurityGroupInventory.class);
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
        DeleteSecurityGroupRuleAction action = new DeleteSecurityGroupRuleAction();
        action.ruleUuids = ruleUuids;
        action.sessionId = getSessionUuid(session);
        DeleteSecurityGroupRuleAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, SecurityGroupInventory.class);
    }

    public void removeVmNicFromSecurityGroup(String securityGroupUuid, String vmNicUuid) throws ApiSenderException {
        removeVmNicFromSecurityGroup(securityGroupUuid, vmNicUuid, null);
    }

    public void removeVmNicFromSecurityGroup(String securityGroupUuid, String vmNicUuid, SessionInventory session) throws ApiSenderException {
        DeleteVmNicFromSecurityGroupAction action = new DeleteVmNicFromSecurityGroupAction();
        action.securityGroupUuid = securityGroupUuid;
        action.vmNicUuids = asList(vmNicUuid);
        action.sessionId = getSessionUuid(session);
        DeleteVmNicFromSecurityGroupAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
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
        AddVmNicToSecurityGroupAction action = new AddVmNicToSecurityGroupAction();
        action.securityGroupUuid = securityGroupUuid;
        action.vmNicUuids = vmNicUuids;
        action.sessionId = getSessionUuid(session);
        AddVmNicToSecurityGroupAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public SecurityGroupInventory attachSecurityGroupToL3Network(String securityGroupUuid, String l3NetworkUuid) throws ApiSenderException {
        return attachSecurityGroupToL3Network(securityGroupUuid, l3NetworkUuid, null);
    }

    public SecurityGroupInventory attachSecurityGroupToL3Network(String securityGroupUuid, String l3NetworkUuid, SessionInventory session) throws ApiSenderException {
        AttachSecurityGroupToL3NetworkAction action = new AttachSecurityGroupToL3NetworkAction();
        action.l3NetworkUuid = l3NetworkUuid;
        action.securityGroupUuid = securityGroupUuid;
        action.sessionId = session == null ? adminSession.getUuid() : session.getUuid();
        AttachSecurityGroupToL3NetworkAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), SecurityGroupInventory.class);
    }

    public SecurityGroupInventory detachSecurityGroupFromL3Network(String securityGroupUuid, String l3NetworkUuid) throws ApiSenderException {
        return detachSecurityGroupFromL3Network(securityGroupUuid, l3NetworkUuid, null);
    }

    public SecurityGroupInventory detachSecurityGroupFromL3Network(String securityGroupUuid, String l3NetworkUuid, SessionInventory session) throws ApiSenderException {
        DetachSecurityGroupFromL3NetworkAction action = new DetachSecurityGroupFromL3NetworkAction();
        action.l3NetworkUuid = l3NetworkUuid;
        action.securityGroupUuid = securityGroupUuid;
        action.sessionId = getSessionUuid(session);
        DetachSecurityGroupFromL3NetworkAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, SecurityGroupInventory.class);
    }

    public void deleteSecurityGroup(String securityGroupUuid) throws ApiSenderException {
        deleteSecurityGroup(securityGroupUuid, null);
    }

    public void deleteSecurityGroup(String securityGroupUuid, SessionInventory session) throws ApiSenderException {
        DeleteSecurityGroupAction action = new DeleteSecurityGroupAction();
        action.uuid = securityGroupUuid;
        action.sessionId = getSessionUuid(session);
        DeleteSecurityGroupAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public void reconnectPrimaryStorage(String psUuid) throws ApiSenderException {
        ReconnectPrimaryStorageAction action = new ReconnectPrimaryStorageAction();
        action.uuid = psUuid;
        action.sessionId = getSessionUuid(adminSession);
        ReconnectPrimaryStorageAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public void reconnectHost(String hostUuid) throws ApiSenderException {
        ReconnectHostAction action = new ReconnectHostAction();
        action.uuid = hostUuid;
        action.sessionId = getSessionUuid(adminSession);
        ReconnectHostAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public BackupStorageInventory reconnectBackupStorage(String bsUuid) throws ApiSenderException {
        ReconnectBackupStorageAction action = new ReconnectBackupStorageAction();
        action.uuid = bsUuid;
        action.sessionId = getSessionUuid(adminSession);
        ReconnectBackupStorageAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, BackupStorageInventory.class);
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
        CreateRootVolumeTemplateFromRootVolumeAction action = new CreateRootVolumeTemplateFromRootVolumeAction();
        action.name = name;
        action.backupStorageUuids = backupStorageUuids;
        action.rootVolumeUuid = rootVolumeUuid;
        action.sessionId = getSessionUuid(session);
        CreateRootVolumeTemplateFromRootVolumeAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, ImageInventory.class);
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

    public void throwExceptionIfNeed(ErrorCode err) throws ApiSenderException {
        if (err != null) {
            throw new ApiSenderException(new org.zstack.header.errorcode.ErrorCode(err.code, err.description, err.details));
        }
    }

    public VipInventory acquireIp(String l3NetworkUuid, String requiredIp, SessionInventory session) throws ApiSenderException {
        CreateVipAction action = new CreateVipAction();
        action.name = "vip";
        action.l3NetworkUuid = l3NetworkUuid;
        action.sessionId = session == null ? adminSession.getUuid() : session.getUuid();
        action.requiredIp = requiredIp;
        CreateVipAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
        return JSONObjectUtil.rehashObject(res.value.getInventory(), VipInventory.class);
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
        ChangeVipStateAction action = new ChangeVipStateAction();
        action.uuid = uuid;
        action.stateEvent = sevt.toString();
        action.sessionId = getSessionUuid(session);
        ChangeVipStateAction.Result res = action.call();

        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, VipInventory.class);
    }

    public void releaseIp(String ipUuid) throws ApiSenderException {
        releaseIp(ipUuid, null);
    }

    public void releaseIp(String ipUuid, SessionInventory session) throws ApiSenderException {
        DeleteVipAction action = new DeleteVipAction();
        action.uuid = ipUuid;
        action.sessionId = getSessionUuid(session);
        DeleteVipAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public PortForwardingRuleInventory changePortForwardingRuleState(String uuid, PortForwardingRuleStateEvent sevt) throws ApiSenderException {
        return changePortForwardingRuleState(uuid, sevt, null);
    }

    public PortForwardingRuleInventory changePortForwardingRuleState(String uuid, PortForwardingRuleStateEvent sevt, SessionInventory session) throws ApiSenderException {
        ChangePortForwardingRuleStateAction action = new ChangePortForwardingRuleStateAction();
        action.uuid = uuid;
        action.stateEvent = sevt.toString();
        action.sessionId = getSessionUuid(session);
        ChangePortForwardingRuleStateAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, PortForwardingRuleInventory.class);
    }

    public PortForwardingRuleInventory createPortForwardingRuleByFullConfig(PortForwardingRuleInventory rule) throws ApiSenderException {
        return createPortForwardingRuleByFullConfig(rule, null);
    }

    public PortForwardingRuleInventory createPortForwardingRuleByFullConfig(PortForwardingRuleInventory rule, SessionInventory session) throws ApiSenderException {
        CreatePortForwardingRuleAction action = new CreatePortForwardingRuleAction();
        action.name = rule.getName();
        action.description = rule.getDescription();
        action.allowedCidr = rule.getAllowedCidr();
        action.privatePortEnd = rule.getPrivatePortEnd();
        action.privatePortStart = rule.getPrivatePortStart();
        action.vipUuid = rule.getVipUuid();
        action.vipPortEnd = rule.getVipPortEnd();
        action.vipPortStart = rule.getVipPortStart();
        action.vmNicUuid = rule.getVmNicUuid();
        action.protocolType = rule.getProtocolType();
        action.sessionId = session == null ? adminSession.getUuid() : session.getUuid();
        CreatePortForwardingRuleAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.getInventory(), PortForwardingRuleInventory.class);
    }

    public void revokePortForwardingRule(String ruleUuid) throws ApiSenderException {
        revokePortForwardingRule(ruleUuid, null);
    }

    public void revokePortForwardingRule(String ruleUuid, SessionInventory session) throws ApiSenderException {
        DeletePortForwardingRuleAction action = new DeletePortForwardingRuleAction();
        action.sessionId = getSessionUuid(session);
        action.uuid = ruleUuid;
        DeletePortForwardingRuleAction.Result res = action.call();
        throwExceptionIfNeed(res.error);
    }

    public PortForwardingRuleInventory attachPortForwardingRule(String ruleUuid, String vmNicUuid) throws ApiSenderException {
        return attachPortForwardingRule(ruleUuid, vmNicUuid, null);
    }

    public PortForwardingRuleInventory attachPortForwardingRule(String ruleUuid, String vmNicUuid, SessionInventory session) throws ApiSenderException {
        AttachPortForwardingRuleAction action = new AttachPortForwardingRuleAction();
        action.sessionId = getSessionUuid(session);
        action.ruleUuid = ruleUuid;
        action.vmNicUuid = vmNicUuid;
        AttachPortForwardingRuleAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, PortForwardingRuleInventory.class);
    }

    public PortForwardingRuleInventory detachPortForwardingRule(String ruleUuid) throws ApiSenderException {
        return detachPortForwardingRule(ruleUuid, null);
    }

    public PortForwardingRuleInventory detachPortForwardingRule(String ruleUuid, SessionInventory session) throws ApiSenderException {
        DetachPortForwardingRuleAction action = new DetachPortForwardingRuleAction();
        action.uuid = ruleUuid;
        action.sessionId = getSessionUuid(session);
        DetachPortForwardingRuleAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, PortForwardingRuleInventory.class);
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
        GetPortForwardingAttachableVmNicsAction action = new GetPortForwardingAttachableVmNicsAction();
        action.ruleUuid = ruleUuid;
        action.sessionId = getSessionUuid(adminSession);
        GetPortForwardingAttachableVmNicsAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.toCollection(
                JSONObjectUtil.toJsonString(res.value.inventories),
                ArrayList.class,
                VmNicInventory.class
        );
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
        GetHypervisorTypesAction action = new GetHypervisorTypesAction();
        action.sessionId = getSessionUuid(adminSession);
        GetHypervisorTypesAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return res.value.hypervisorTypes;
    }

    public Map<String, List<String>> getNetworkServiceTypes() throws ApiSenderException {
        GetNetworkServiceTypesAction action = new GetNetworkServiceTypesAction();
        action.sessionId = getSessionUuid(adminSession);
        GetNetworkServiceTypesAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return res.value.types;
    }

    public List<String> getL2NetworkTypes() throws ApiSenderException {
        GetL2NetworkTypesAction action = new GetL2NetworkTypesAction();
        action.sessionId = getSessionUuid(adminSession);
        GetL2NetworkTypesAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return res.value.types;
    }

    public List<String> getL3NetworkTypes() throws ApiSenderException {
        return getL3NetworkTypes(null);
    }

    public List<String> getL3NetworkTypes(SessionInventory session) throws ApiSenderException {
        GetL3NetworkTypesAction action = new GetL3NetworkTypesAction();
        action.sessionId = getSessionUuid(session);
        GetL3NetworkTypesAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return res.value.types;
    }

    public List<String> getPrimaryStorageTypes() throws ApiSenderException {
        GetPrimaryStorageTypesAction action = new GetPrimaryStorageTypesAction();
        action.sessionId = getSessionUuid(adminSession);
        GetPrimaryStorageTypesAction.Result res = action.call();
        throwExceptionIfNeed(res.error);

        return res.value.types;
    }

    public List<String> getBackupStorageTypes() throws ApiSenderException {
        GetBackupStorageTypesAction a = new GetBackupStorageTypesAction();
        a.sessionId = getSessionUuid(adminSession);
        GetBackupStorageTypesAction.Result r = a.call();
        throwExceptionIfNeed(r.error);

        return r.value.types;
    }

    public ImageInventory changeImageState(String uuid, ImageStateEvent evt, SessionInventory session) throws ApiSenderException {
        ChangeImageStateAction a = new ChangeImageStateAction();
        a.uuid = uuid;
        a.stateEvent = evt.toString();
        a.sessionId = getSessionUuid(session);
        ChangeImageStateAction.Result r = a.call();
        throwExceptionIfNeed(r.error);

        return JSONObjectUtil.rehashObject(r.value.inventory, ImageInventory.class);
    }

    public ImageInventory changeImageState(String uuid, ImageStateEvent evt) throws ApiSenderException {
        return changeImageState(uuid, evt, null);
    }

    public List<String> getHostAllocatorStrategies() throws ApiSenderException {
        GetHostAllocatorStrategiesAction a = new GetHostAllocatorStrategiesAction();
        a.sessionId = getSessionUuid(adminSession);
        GetHostAllocatorStrategiesAction.Result r = a.call();
        throwExceptionIfNeed(r.error);

        return r.value.strategies;
    }

    public List<String> getPrimaryStorageAllocatorStrategies() throws ApiSenderException {
        GetPrimaryStorageAllocatorStrategiesAction a = new GetPrimaryStorageAllocatorStrategiesAction();
        a.sessionId = getSessionUuid(adminSession);
        GetPrimaryStorageAllocatorStrategiesAction.Result r = a.call();
        throwExceptionIfNeed(r.error);
        return r.value.strategies;
    }

    public DiskOfferingInventory changeDiskOfferingState(String uuid, DiskOfferingStateEvent sevt) throws ApiSenderException {
        return changeDiskOfferingState(uuid, sevt, null);
    }

    public DiskOfferingInventory changeDiskOfferingState(String uuid, DiskOfferingStateEvent sevt, SessionInventory session) throws ApiSenderException {
        ChangeDiskOfferingStateAction a = new ChangeDiskOfferingStateAction();
        a.uuid = uuid;
        a.stateEvent = sevt.toString();
        a.sessionId = getSessionUuid(session);
        ChangeDiskOfferingStateAction.Result r = a.call();
        throwExceptionIfNeed(r.error);

        return JSONObjectUtil.rehashObject(r.value.inventory, DiskOfferingInventory.class);
    }

    public VmInstanceInventory attachNic(String vmUuid, String l3Uuid) throws ApiSenderException {
        return attachNic(vmUuid, l3Uuid, null);
    }

    public VmInstanceInventory attachNic(String vmUuid, String l3Uuid, String staticIp) throws ApiSenderException {
        AttachL3NetworkToVmAction a = new AttachL3NetworkToVmAction();
        a.l3NetworkUuid = l3Uuid;
        a.staticIp = staticIp;
        a.vmInstanceUuid = vmUuid;
        a.sessionId = adminSession.getUuid();
        AttachL3NetworkToVmAction.Result r = a.call();

        throwExceptionIfNeed(r.error);
        return JSONObjectUtil.rehashObject(r.value.inventory, VmInstanceInventory.class);
    }

    public List<L3NetworkInventory> getVmAttachableL3Networks(String vmUuid) throws ApiSenderException {
        return getVmAttachableL3Networks(vmUuid, null);
    }

    public List<L3NetworkInventory> getVmAttachableL3Networks(String vmUuid, SessionInventory session) throws ApiSenderException {
        GetVmAttachableL3NetworkAction a = new GetVmAttachableL3NetworkAction();
        a.vmInstanceUuid = vmUuid;
        a.sessionId = getSessionUuid(session);
        GetVmAttachableL3NetworkAction.Result r = a.call();
        throwExceptionIfNeed(r.error);

        return JSONObjectUtil.toCollection(
                JSONObjectUtil.toJsonString(r.value.inventories),
                ArrayList.class,
                L3NetworkInventory.class
        );
    }

    public VmInstanceInventory detachNic(String nicUuid) throws ApiSenderException {
        return detachNic(nicUuid, null);
    }

    public VmInstanceInventory detachNic(String niUuid, SessionInventory session) throws ApiSenderException {
        DetachL3NetworkFromVmAction a = new DetachL3NetworkFromVmAction();
        a.sessionId = getSessionUuid(session);
        a.vmNicUuid = niUuid;
        DetachL3NetworkFromVmAction.Result r = a.call();
        throwExceptionIfNeed(r.error);

        return JSONObjectUtil.rehashObject(r.value.inventory, VmInstanceInventory.class);
    }

    public ConsoleInventory getConsole(String vmUuid) throws ApiSenderException {
        RequestConsoleAccessAction a = new RequestConsoleAccessAction();
        a.sessionId = getSessionUuid(adminSession);
        a.vmInstanceUuid = vmUuid;
        RequestConsoleAccessAction.Result res = a.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, ConsoleInventory.class);
    }

    public EipInventory createEip(String name, String vipUuid, String vmNicUuid) throws ApiSenderException {
        return createEip(name, vipUuid, vmNicUuid, null);
    }

    public EipInventory createEip(String name, String vipUuid, String vmNicUuid, SessionInventory session) throws ApiSenderException {
        CreateEipAction a = new CreateEipAction();
        a.name = name;
        a.description = name;
        a.vipUuid = vipUuid;
        a.vmNicUuid = vmNicUuid;
        a.sessionId = getSessionUuid(session);
        CreateEipAction.Result r = a.call();

        throwExceptionIfNeed(r.error);

        return JSONObjectUtil.rehashObject(r.value.inventory, EipInventory.class);
    }

    public void removeEip(String eipUuid) throws ApiSenderException {
        removeEip(eipUuid, null);
    }

    public void removeEip(String eipUuid, SessionInventory session) throws ApiSenderException {
        DeleteEipAction a = new DeleteEipAction();
        a.uuid = eipUuid;
        a.sessionId = getSessionUuid(session);
        DeleteEipAction.Result r = a.call();
        throwExceptionIfNeed(r.error);
    }

    public EipInventory attachEip(String eipUuid, String vmNicUuid) throws ApiSenderException {
        return attachEip(eipUuid, vmNicUuid, null);
    }

    public EipInventory attachEip(String eipUuid, String vmNicUuid, SessionInventory session) throws ApiSenderException {
        AttachEipAction a = new AttachEipAction();
        a.eipUuid = eipUuid;
        a.vmNicUuid = vmNicUuid;
        a.sessionId = getSessionUuid(session);
        AttachEipAction.Result res = a.call();

        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, EipInventory.class);
    }

    public EipInventory changeEipState(String eipUuid, EipStateEvent sevt) throws ApiSenderException {
        return changeEipState(eipUuid, sevt, null);
    }

    public EipInventory changeEipState(String eipUuid, EipStateEvent sevt, SessionInventory session) throws ApiSenderException {
        ChangeEipStateAction a = new ChangeEipStateAction();
        a.uuid = eipUuid;
        a.stateEvent = sevt.toString();
        a.sessionId = getSessionUuid(adminSession);
        ChangeEipStateAction.Result r = a.call();
        throwExceptionIfNeed(r.error);

        return JSONObjectUtil.rehashObject(r.value.inventory, EipInventory.class);
    }

    public EipInventory detachEip(String eipUuid) throws ApiSenderException {
        return detachEip(eipUuid, null);
    }

    public EipInventory detachEip(String eipUuid, SessionInventory session) throws ApiSenderException {
        DetachEipAction a = new DetachEipAction();
        a.uuid = eipUuid;
        a.sessionId = getSessionUuid(session);
        DetachEipAction.Result res = a.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, EipInventory.class);
    }

    public List<VmNicInventory> getEipAttachableVmNicsByEipUuid(String eipUuid) throws ApiSenderException {
        return getEipAttachableVmNics(eipUuid, null);
    }

    public List<VmNicInventory> getEipAttachableVmNicsByVipUuid(String vipUuid) throws ApiSenderException {
        return getEipAttachableVmNics(null, vipUuid);
    }

    private List<VmNicInventory> getEipAttachableVmNics(String eipUuid, String vipUuid) throws ApiSenderException {
        GetEipAttachableVmNicsAction a = new GetEipAttachableVmNicsAction();
        a.eipUuid = eipUuid;
        a.vipUuid = vipUuid;
        a.sessionId = getSessionUuid(adminSession);
        GetEipAttachableVmNicsAction.Result r = a.call();
        throwExceptionIfNeed(r.error);

        return JSONObjectUtil.toCollection(
                JSONObjectUtil.toJsonString(r.value.inventories),
                ArrayList.class,
                VmNicInventory.class
        );
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
        if (type == TagType.System) {
            CreateSystemTagAction a = new CreateSystemTagAction();
            a.sessionId = getSessionUuid(session);
            a.resourceType = entityClass.getSimpleName();
            a.resourceUuid = resourceUuid;
            a.tag = tag;
            CreateSystemTagAction.Result r = a.call();
            throwExceptionIfNeed(r.error);

            return JSONObjectUtil.rehashObject(r.value.getInventory(), TagInventory.class);
        } else {
            CreateUserTagAction a = new CreateUserTagAction();
            a.sessionId = getSessionUuid(session);
            a.resourceType = entityClass.getSimpleName();
            a.resourceUuid = resourceUuid;
            a.tag = tag;
            CreateUserTagAction.Result r = a.call();
            throwExceptionIfNeed(r.error);

            return JSONObjectUtil.rehashObject(r.value.getInventory(), TagInventory.class);
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
        UpdateSystemTagAction a = new UpdateSystemTagAction();
        a.uuid = uuid;
        a.tag = tag;
        a.sessionId = getSessionUuid(session);
        UpdateSystemTagAction.Result res = a.call();
        throwExceptionIfNeed(res.error);

        return JSONObjectUtil.rehashObject(res.value.inventory, TagInventory.class);
    }

    public void deleteTag(String tagUuid) throws ApiSenderException {
        deleteTag(tagUuid, null);
    }

    public void deleteTag(String tagUuid, SessionInventory session) throws ApiSenderException {
        DeleteTagAction a = new DeleteTagAction();
        a.sessionId = getSessionUuid(session);
        a.uuid = tagUuid;
        DeleteTagAction.Result r = a.call();
        throwExceptionIfNeed(r.error);
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
        ReconnectVirtualRouterAction a = new ReconnectVirtualRouterAction();
        a.sessionId = getSessionUuid(adminSession);
        a.vmInstanceUuid = uuid;
        ReconnectVirtualRouterAction.Result r = a.call();
        throwExceptionIfNeed(r.error);

        return JSONObjectUtil.rehashObject(r.value.inventory, ApplianceVmInventory.class);
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
        UpdateZoneAction a = new UpdateZoneAction();
        a.sessionId = getSessionUuid(adminSession);
        a.name = inv.getName();
        a.description = inv.getDescription();
        a.uuid = inv.getUuid();
        UpdateZoneAction.Result r = a.call();
        throwExceptionIfNeed(r.error);

        return JSONObjectUtil.rehashObject(r.value.inventory, ZoneInventory.class);
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

    public VmInstanceInventory updateCpuMemory(String uuid, Integer cpu, Long memory) throws ApiSenderException {
        APIUpdateVmInstanceMsg msg = new APIUpdateVmInstanceMsg();
        msg.setSession(adminSession);
        msg.setUuid(uuid);
        msg.setCpuNum(cpu);
        msg.setMemorySize(memory);
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
        UpdateL3NetworkAction a = new UpdateL3NetworkAction();
        a.sessionId = getSessionUuid(session);
        a.name = inv.getName();
        a.description = inv.getDescription();
        a.uuid = inv.getUuid();
        UpdateL3NetworkAction.Result r = a.call();
        throwExceptionIfNeed(r.error);

        return JSONObjectUtil.rehashObject(r.value.inventory, L3NetworkInventory.class);
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
        APIUpdateCephBackupStorageMonEvent evt = sender.send(msg, APIUpdateCephBackupStorageMonEvent.class);
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
        APIUpdateCephPrimaryStorageMonEvent evt = sender.send(msg, APIUpdateCephPrimaryStorageMonEvent.class);
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

    public List<VmNicInventory> getVmNicCandidatesForLoadBalancer(String listenerUuid, SessionInventory session) throws ApiSenderException {
        APIGetCandidateVmNicsForLoadBalancerMsg msg = new APIGetCandidateVmNicsForLoadBalancerMsg();
        msg.setListenerUuid(listenerUuid);
        msg.setSession(session == null ? adminSession : session);
        ApiSender sender = new ApiSender();
        APIGetCandidateVmNicsForLoadBalancerReply reply = sender.call(msg, APIGetCandidateVmNicsForLoadBalancerReply.class);
        return reply.getInventories();
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
        RecoverVmInstanceAction a = new RecoverVmInstanceAction();
        a.uuid = vmUuid;
        a.sessionId = getSessionUuid(session);
        RecoverVmInstanceAction.Result res = a.call();
        throwExceptionIfNeed(res.error);
        return JSONObjectUtil.rehashObject(res.value.inventory, VmInstanceInventory.class);
    }

    public void expungeVm(String vmUuid, SessionInventory session) throws ApiSenderException {
        MessageCommandRecorder.reset();
        MessageCommandRecorder.start(APIExpungeVmInstanceMsg.class);

        ExpungeVmInstanceAction a = new ExpungeVmInstanceAction();
        a.sessionId = getSessionUuid(session);
        a.uuid = vmUuid;
        ExpungeVmInstanceAction.Result res = a.call();
        throwExceptionIfNeed(res.error);

        logger.debug(MessageCommandRecorder.endAndToString());
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

    public List<VmNicInventory> getL3NetworkVmNics(String uuid) throws ApiSenderException {
        QueryVmNicAction a = new QueryVmNicAction();
        a.conditions = Arrays.asList(
                String.format("l3NetworkUuid=%s", uuid),
                String.format("vmInstance.type=%s", VmInstanceConstant.USER_VM_TYPE)
        );
        a.sessionId = getSessionUuid(adminSession);
        QueryVmNicAction.Result r = a.call();
        throwExceptionIfNeed(r.error);
        return JSONObjectUtil.toCollection(
                JSONObjectUtil.toJsonString(r.value.inventories),
                ArrayList.class,
                VmNicInventory.class
        );
    }
}
