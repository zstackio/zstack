package org.zstack.xinfini;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.xinfini.XInfiniConstants;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.xinfini.sdk.*;
import org.zstack.xinfini.sdk.cluster.QueryClusterRequest;
import org.zstack.xinfini.sdk.cluster.QueryClusterResponse;
import org.zstack.xinfini.sdk.iscsi.*;
import org.zstack.xinfini.sdk.metric.PoolMetrics;
import org.zstack.xinfini.sdk.metric.QueryMetricRequest;
import org.zstack.xinfini.sdk.metric.QueryMetricResponse;
import org.zstack.xinfini.sdk.node.*;
import org.zstack.xinfini.sdk.pool.*;
import org.zstack.xinfini.sdk.vhost.*;
import org.zstack.xinfini.sdk.volume.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class XInfiniApiHelper {

    private static CLogger logger = Utils.getLogger(XInfiniApiHelper.class);
    XInfiniClient client;

    private final static double POOL_RESERVED_SIZE_RATIO = 0.15;

    private static final Cache<String, String> snapshotClientCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build();

    XInfiniApiHelper(XInfiniClient client) {
        this.client = client;
    }

    public <T extends XInfiniResponse> T callWithNode(XInfiniRequest req, Class<T> clz, XInfiniConfig.Node node) {
        return client.call(req, clz, node);
    }

    public <T extends XInfiniResponse> T callErrorOut(XInfiniRequest req, Class<T> clz) {
        T rsp = client.call(req, clz);
        errorOut(rsp);
        return rsp;
    }

    public <T extends XInfiniResponse> T call(XInfiniRequest req, Class<T> clz) {
        return client.call(req, clz);
    }

    public <T extends XInfiniResponse> void call(XInfiniRequest req, Completion completion) {
        client.call(req, result -> {
            if (result.getMessage() == null) {
                completion.success();
                return;
            }

            completion.fail(operr("xinfini request failed, message: %s.", result.getMessage()));
        });
    }

    public void errorOut(XInfiniResponse rsp) {
        if (!rsp.isSuccess()) {
            throw new OperationFailureException(operr("xinfini request failed, message: %s.", rsp.getMessage()));
        }
    }

    public <T extends XInfiniQueryResponse> T query(XInfiniQueryRequest req, Class<T> clz) {
        return call(req, clz);
    }

    public <T extends XInfiniQueryResponse> T queryErrorOut(XInfiniQueryRequest req, Class<T> clz) {
        return callErrorOut(req, clz);
    }
    public List<PoolModule> queryPools() {
        QueryPoolRequest req = new QueryPoolRequest();
        return queryErrorOut(req, QueryPoolResponse.class).getItems();
    }

    public PoolModule getPool(int id) {
        GetPoolRequest req = new GetPoolRequest();
        req.setId(id);
        return callErrorOut(req, GetPoolResponse.class).toModule();
    }

    public String getClusterUuid() {
        QueryClusterRequest req = new QueryClusterRequest();
        return queryErrorOut(req, QueryClusterResponse.class).toModule().getUuid();
    }

    public Map<String, NodeStatus> checkNodesConnection(List<XInfiniConfig.Node> nodes) {
        Map<String, NodeStatus> nodesStatus = Maps.newConcurrentMap();
        for (XInfiniConfig.Node node : nodes) {
            QueryClusterRequest req = new QueryClusterRequest();
            QueryClusterResponse rsp = callWithNode(req, QueryClusterResponse.class, node);
            nodesStatus.put(node.getIp(), rsp.isSuccess() ? NodeStatus.Connected : NodeStatus.Disconnected);
        }

        return nodesStatus;
    }

    public List<NodeModule> queryNodes() {
        QueryNodeRequest req = new QueryNodeRequest();
        return queryErrorOut(req, QueryNodeResponse.class).getItems();
    }

    public NodeModule getNode(int id) {
        GetNodeRequest req = new GetNodeRequest();
        req.setId(id);
        return callErrorOut(req, GetNodeResponse.class).toModule();
    }

    public List<BdcModule> queryBdcs() {
        QueryBdcRequest req = new QueryBdcRequest();
        return queryErrorOut(req, QueryBdcResponse.class).getItems();
    }

    public BdcModule queryBdcByIp(String ip, boolean errorIfNotExist) {
        QueryBdcRequest req = new QueryBdcRequest();
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            req.sortBy = "spec.id:desc";
        } else {
            req.q = String.format("spec.ip:%s", ip);
        }

        QueryBdcResponse rsp = queryErrorOut(req, QueryBdcResponse.class);
        if (rsp.getMetadata().getPagination().getCount() == 0) {
            if (errorIfNotExist) {
                throw new OperationFailureException(operr("bdc with ip %s not found.", ip));
            }

            return null;
        }

        return rsp.getItems().get(0);
    }

    public BdcModule queryBdcByIp(String ip) {
        return queryBdcByIp(ip, true);
    }

    public BdcModule getBdc(int id) {
        GetBdcRequest req = new GetBdcRequest();
        req.setId(id);
        return callErrorOut(req, GetBdcResponse.class).toModule();
    }

    public BsPolicyModule getBsPolicy(int id) {
        GetBsPolicyRequest req = new GetBsPolicyRequest();
        req.setId(id);
        return callErrorOut(req, GetBsPolicyResponse.class).toModule();
    }

    public PoolCapacity getPoolCapacity(PoolModule pool) {
        PoolCapacity capacity = new PoolCapacity();
        long usedCapacity = SizeUnit.KILOBYTE.toByte(getPoolMetricValue(PoolMetrics.DATA_KBYTES, pool));
        long totalCapacity = SizeUnit.KILOBYTE.toByte(getPoolMetricValue(PoolMetrics.ACTUAL_KBYTES, pool));
        long reservedCapacity = (long) (totalCapacity * POOL_RESERVED_SIZE_RATIO);
        capacity.setTotalCapacity(totalCapacity);
        capacity.setAvailableCapacity(totalCapacity - usedCapacity - reservedCapacity);
        return capacity;
    }

    public long getPoolMetricValue(String metricName, PoolModule pool) {
        QueryMetricRequest req = new QueryMetricRequest();
        req.metric = metricName;
        req.lables = String.format("pool_id=%s", pool.getSpec().getId());
        QueryMetricResponse rsp = callErrorOut(req, QueryMetricResponse.class);
        if (rsp.getData() == null || CollectionUtils.isEmpty(rsp.getData().getResult())) {
            logger.warn(String.format("get pool[id=%s, name=%s] metric %s value failed", pool.getSpec().getId(), pool.getSpec().getName(), metricName));
            return 0;
        }

        return rsp.getData().getResult().get(0).getValue();
    }

    public VolumeModule queryVolumeByName(String name) {
        QueryVolumeRequest req = new QueryVolumeRequest();
        req.q = String.format("spec.name:%s", name);
        QueryVolumeResponse rsp = queryErrorOut(req, QueryVolumeResponse.class);
        if (rsp.getMetadata().getPagination().getCount() == 0) {
            return null;
        }

        return rsp.getItems().get(0);
    }

    public VolumeModule getVolume(int id) {
        GetVolumeRequest req = new GetVolumeRequest();
        req.setId(id);
        return callErrorOut(req, GetVolumeResponse.class).toModule();
    }

    public VolumeModule createVolume(String name, int poolId, long size) {
        CreateVolumeRequest req = new CreateVolumeRequest();
        req.setName(name);
        req.setPoolId(poolId);
        req.setSizeMb(size);
        CreateVolumeResponse rsp = callErrorOut(req, CreateVolumeResponse.class);
        GetVolumeRequest gReq = new GetVolumeRequest();
        gReq.setId(rsp.getSpec().getId());
        return retryUtilStateActive(gReq, GetVolumeResponse.class,
                (GetVolumeResponse gvp) -> gvp.toModule().getMetadata().getState().getState()).toModule();
    }

    public VolumeSnapshotModule getVolumeSnapshot(int id) {
        GetVolumeSnapshotRequest req = new GetVolumeSnapshotRequest();
        req.setId(id);
        return callErrorOut(req, GetVolumeSnapshotResponse.class).toModule();
    }

    public VolumeSnapshotModule createVolumeSnapshot(int volumeId, String name) {
        CreateVolumeSnapshotRequest req = new CreateVolumeSnapshotRequest();
        req.setName(name);
        req.setBsVolumeId(volumeId);
        CreateVolumeSnapshotResponse rsp = callErrorOut(req, CreateVolumeSnapshotResponse.class);
        GetVolumeSnapshotRequest gReq = new GetVolumeSnapshotRequest();
        gReq.setId(rsp.getSpec().getId());
        return retryUtilStateActive(gReq, GetVolumeSnapshotResponse.class,
                (GetVolumeSnapshotResponse gvp) -> gvp.toModule().getMetadata().getState().getState()).toModule();
    }

    public VolumeModule cloneVolume(int snapId, String name, String desc, boolean flatten) {
        CloneVolumeRequest req = new CloneVolumeRequest();
        req.setName(name);
        req.setDescription(desc);
        req.setBsSnapId(snapId);
        req.setFlatten(flatten);
        CloneVolumeResponse rsp = callErrorOut(req, CloneVolumeResponse.class);
        GetVolumeRequest gReq = new GetVolumeRequest();
        gReq.setId(rsp.getSpec().getId());
        return retryUtilStateActive(gReq, GetVolumeResponse.class,
                (GetVolumeResponse gvp) -> gvp.toModule().getMetadata().getState().getState()).toModule();
    }

    public VolumeModule expandVolume(int volId, long size) {
        ExpandVolumeRequest req = new ExpandVolumeRequest();
        req.setId(volId);
        req.setSizeMb(size);
        callErrorOut(req, ExpandVolumeResponse.class);
        GetVolumeRequest gReq = new GetVolumeRequest();
        gReq.setId(volId);
        return retryUtilStateActive(gReq, GetVolumeResponse.class,
                (GetVolumeResponse gvp) -> gvp.toModule().getMetadata().getState().getState()).toModule();
    }

    private <T extends XInfiniResponse> void retryUtilResourceDeleted(XInfiniRequest req,
                                                                      Class<T> rsp) {
        new Retry<Void>() {
            @Override
            @RetryCondition(times = XInfiniConstants.DEFAULT_POLLING_TIMES)
            protected Void call() {
                T r = XInfiniApiHelper.this.call(req, rsp);
                if (!r.resourceIsDeleted()) {
                    throw new RetryException("resource not deleted yet");
                }

                return null;
            }

            @Override
            // not error out if delete failed
            protected boolean onFailure(Throwable t) {
                return false;
            }
        }.run();
    }

    private <T extends XInfiniResponse> T retryUtilStateActive(XInfiniRequest req,
                                                               Class<T> rsp,
                                                               Function<String, T> activeGetter) {
        return new Retry<T>() {
            @Override
            @RetryCondition(onExceptions = {RetryException.class},
                    times = XInfiniConstants.DEFAULT_POLLING_TIMES)
            protected T call() {
                T r = callErrorOut(req, rsp);
                if (!activeGetter.call(r).equals(MetadataState.active.toString())) {
                    throw new RetryException("state not active yet");
                }
                return r;
            }
        }.run();
    }

    public BdcBdevModule createBdcBdev(int bdcId, int volumeId, String name) {
        CreateBdcBdevRequest req = new CreateBdcBdevRequest();
        req.setName(name);
        req.setBdcId(bdcId);
        req.setBsVolumeId(volumeId);
        CreateBdcBdevResponse rsp = callErrorOut(req, CreateBdcBdevResponse.class);
        GetBdcBdevRequest gReq = new GetBdcBdevRequest();
        gReq.setId(rsp.getSpec().getId());
        return retryUtilStateActive(gReq, GetBdcBdevResponse.class,
                (GetBdcBdevResponse gvp) -> gvp.toModule().getMetadata().getState().getState()).toModule();
    }

    public BdcBdevModule queryBdcBdevByVolumeIdAndBdcId(int volId, int bdcId) {
        QueryBdcBdevRequest req = new QueryBdcBdevRequest();
        req.q = String.format("((spec.bdc_id:%s) AND (spec.bs_volume_id:%s))", bdcId, volId);
        QueryBdcBdevResponse rsp = queryErrorOut(req, QueryBdcBdevResponse.class);
        if (rsp.getMetadata().getPagination().getCount() == 0) {
            return null;
        }

        return rsp.getItems().get(0);
    }

    public BdcBdevModule getOrCreateBdcBdevByVolumeIdAndBdcId(int volId, int bdcId, String bdevName) {
        QueryBdcBdevRequest req = new QueryBdcBdevRequest();
        req.q = String.format("((spec.bdc_id:%s) AND (spec.bs_volume_id:%s))", bdcId, volId);
        QueryBdcBdevResponse rsp = queryErrorOut(req, QueryBdcBdevResponse.class);
        if (rsp.getMetadata().getPagination().getCount() == 0) {
            return createBdcBdev(bdcId, volId, bdevName);
        }

        return rsp.getItems().get(0);
    }

    public List<BdcBdevModule> queryBdcBdevByVolumeId(int volId) {
        QueryBdcBdevRequest req = new QueryBdcBdevRequest();
        req.q = String.format("spec.bs_volume_id:%s", volId);
        return queryErrorOut(req, QueryBdcBdevResponse.class).getItems();
    }

    public BdcBdevModule getBdcBdev(int id) {
        GetBdcBdevRequest req = new GetBdcBdevRequest();
        req.setId(id);
        return callErrorOut(req, GetBdcBdevResponse.class).toModule();
    }

    public void deleteBdcBdev(int bdevId) {
        DeleteBdcBdevRequest req = new DeleteBdcBdevRequest();
        req.setId(bdevId);
        DeleteBdcBdevResponse rsp = call(req, DeleteBdcBdevResponse.class);
        if (rsp.resourceIsDeleted()) {
            logger.info(String.format("bdev %s has been deleted, skip send delete req", bdevId));
            return;
        }

        GetBdcBdevRequest gReq = new GetBdcBdevRequest();
        gReq.setId(bdevId);
        retryUtilResourceDeleted(gReq, GetBdcBdevResponse.class);
    }

    public void deleteVolume(int volId, boolean force) {
        DeleteVolumeRequest req = new DeleteVolumeRequest();
        req.setId(volId);
        DeleteVolumeResponse rsp = call(req, DeleteVolumeResponse.class);
        if (rsp.resourceIsDeleted()) {
            logger.info(String.format("volume %s has been deleted, skip send delete req", volId));
            return;
        }

        GetVolumeRequest gReq = new GetVolumeRequest();
        gReq.setId(volId);
        retryUtilResourceDeleted(gReq, GetVolumeResponse.class);
    }

    public void deleteVolumeSnapshot(int snapShotId) {
        // check snapshot cloned volume
        if (snapshotHasClonedVolume(snapShotId)) {
            throw new OperationFailureException(operr("snapshot[id: %s] has cloned volume, please delete or flatten volumes", snapShotId));
        }

        DeleteVolumeSnapshotRequest req = new DeleteVolumeSnapshotRequest();
        req.setId(snapShotId);
        DeleteVolumeSnapshotResponse rsp = call(req, DeleteVolumeSnapshotResponse.class);
        if (rsp.resourceIsDeleted()) {
            logger.info(String.format("volume snapshot %s has been deleted, skip send delete req", snapShotId));
            return;
        }
        GetVolumeSnapshotRequest gReq = new GetVolumeSnapshotRequest();
        gReq.setId(snapShotId);
        retryUtilResourceDeleted(gReq, GetVolumeSnapshotResponse.class);
    }

    public boolean snapshotHasClonedVolume(int snapId) {
        QueryVolumeRequest req = new QueryVolumeRequest();
        req.q = String.format("spec.bs_snap_id:%s", snapId);
        QueryVolumeResponse rsp = queryErrorOut(req, QueryVolumeResponse.class);
        if (rsp.getMetadata().getPagination().getCount() == 0) {
            return false;
        }

        List<String> volNames = rsp.getItems().stream().map(VolumeModule::getSpec).map(VolumeModule.VolumeSpec::getName).collect(Collectors.toList());

        logger.info(String.format("snapshot %s has %d cloned volume, volume names: %s", snapId, rsp.getMetadata().getPagination().getCount(), volNames));
        return true;
    }

    public List<VolumeSnapshotModule> queryVolumeSnapshotByVolumeId(int volId) {
        QueryVolumeSnapshotRequest req = new QueryVolumeSnapshotRequest();
        req.q = String.format("spec.bs_volume_id:%s", volId);
        return queryErrorOut(req, QueryVolumeSnapshotResponse.class).getItems();
    }

    public VolumeSnapshotModule queryVolumeSnapshotByName(String name) {
        QueryVolumeSnapshotRequest req = new QueryVolumeSnapshotRequest();
        req.q = String.format("spec.name:%s", name);
        QueryVolumeSnapshotResponse rsp = queryErrorOut(req, QueryVolumeSnapshotResponse.class);
        if (rsp.getMetadata().getPagination().getCount() == 0) {
            return null;
        }
        return rsp.getItems().get(0);
    }

    public List<IscsiGatewayModule> queryIscsiGateways() {
        QueryIscsiGatewayRequest req = new QueryIscsiGatewayRequest();
        return queryErrorOut(req, QueryIscsiGatewayResponse.class).getItems();
    }

    public List<IscsiGatewayModule> queryIscsiGatewaysByIds(List<Integer> ids) {
        QueryIscsiGatewayRequest req = new QueryIscsiGatewayRequest();
        req.q = String.format("spec.id:(%s)", ids.stream().map(String::valueOf).collect(Collectors.joining(" ")));
        return queryErrorOut(req, QueryIscsiGatewayResponse.class).getItems();
    }

    public List<IscsiClientGroupModule> queryIscsiClientGroups() {
        QueryIscsiClientGroupRequest req = new QueryIscsiClientGroupRequest();
        return queryErrorOut(req, QueryIscsiClientGroupResponse.class).getItems();
    }

    public IscsiClientGroupModule queryIscsiClientGroupByName(String name) {
        QueryIscsiClientGroupRequest req = new QueryIscsiClientGroupRequest();
        req.q = String.format("spec.name:%s", name);
        QueryIscsiClientGroupResponse rsp = queryErrorOut(req, QueryIscsiClientGroupResponse.class);
        if (rsp.getMetadata().getPagination().getCount() == 0) {
            return null;
        }

        return rsp.getItems().get(0);
    }

    public IscsiClientModule createIscsiClient(String name, String code, int iscsiClientGroupId) {
        CreateIscsiClientRequest req = new CreateIscsiClientRequest();
        req.setName(name);
        req.setCode(code);
        req.setIscsiClientGroupId(iscsiClientGroupId);
        CreateIscsiClientResponse rsp =  callErrorOut(req, CreateIscsiClientResponse.class);
        GetIscsiClientRequest gReq = new GetIscsiClientRequest();
        gReq.setId(rsp.getSpec().getId());
        return retryUtilStateActive(gReq, GetIscsiClientResponse.class,
                (GetIscsiClientResponse gvp) -> gvp.toModule().getMetadata().getState().getState()).toModule();
    }

    public IscsiClientModule getIscsiClient(int id) {
        GetIscsiClientRequest req = new GetIscsiClientRequest();
        req.setId(id);
        return callErrorOut(req, GetIscsiClientResponse.class).toModule();
    }

    public void addVolumeClientGroupMapping(int volumeId, int iscsiClientGroupId) {
        QueryVolumeClientGroupMappingRequest qReq = new QueryVolumeClientGroupMappingRequest();
        qReq.q = String.format("((spec.iscsi_client_group_id:%s) AND (spec.bs_volume_id:%s))", iscsiClientGroupId, volumeId);
        if (queryErrorOut(qReq, QueryVolumeClientGroupMappingResponse.class).getMetadata().getPagination().getCount() > 0) {
            logger.info(String.format("volume %s has already been mapped to iscsi client group %s, skip add", volumeId, iscsiClientGroupId));
            return;
        }

        AddVolumeClientGroupMappingRequest req = new AddVolumeClientGroupMappingRequest();
        req.setId(volumeId);
        req.setIscsiClientGroupIds(Collections.singletonList(iscsiClientGroupId));
        callErrorOut(req, AddVolumeClientGroupMappingResponse.class);

        GetVolumeRequest gReq = new GetVolumeRequest();
        gReq.setId(volumeId);
        retryUtilStateActive(gReq, GetVolumeResponse.class,
                (GetVolumeResponse gvp) -> gvp.toModule().getMetadata().getState().getState()).toModule();
    }

    public void deleteVolumeClientGroupMapping(int mapId) {
        DeleteVolumeClientGroupMappingRequest req = new DeleteVolumeClientGroupMappingRequest();
        req.setId(mapId);
        DeleteVolumeClientGroupMappingResponse rsp = call(req, DeleteVolumeClientGroupMappingResponse.class);
        if (rsp.resourceIsDeleted()) {
            logger.info(String.format("volume-client-group-mapping %s has been deleted, skip send delete req", mapId));
            return;
        }

        GetVolumeClientGroupMappingRequest gReq = new GetVolumeClientGroupMappingRequest();
        gReq.setId(mapId);
        retryUtilResourceDeleted(gReq, GetVolumeClientGroupMappingResponse.class);
    }

    public List<IscsiGatewayClientGroupMappingModule> queryIscsiGatewayClientGroupMappingByGroupId(int groupId) {
        QueryIscsiGatewayClientGroupMappingRequest req = new QueryIscsiGatewayClientGroupMappingRequest();
        req.q = String.format("spec.iscsi_client_group_id:%s", groupId);
        return queryErrorOut(req, QueryIscsiGatewayClientGroupMappingResponse.class).getItems();
    }

    public List<IscsiClientModule> queryIscsiClientByGroupId(int groupId) {
        QueryIscsiClientRequest req = new QueryIscsiClientRequest();
        req.q = String.format("spec.iscsi_client_group_id:%s", groupId);
        return queryErrorOut(req, QueryIscsiClientResponse.class).getItems();
    }

    public List<IscsiClientModule> queryIscsiClientByIds(List<Integer> ids) {
        QueryIscsiClientRequest req = new QueryIscsiClientRequest();
        req.q = String.format("spec.id:(%s)", ids.stream().map(String::valueOf).collect(Collectors.joining(" ")));
        return queryErrorOut(req, QueryIscsiClientResponse.class).getItems();
    }

    public IscsiClientModule queryIscsiClientByIqn(String code) {
        QueryIscsiClientRequest req = new QueryIscsiClientRequest();
        req.q = String.format("spec.code:%s", code);
        QueryIscsiClientResponse rsp = queryErrorOut(req, QueryIscsiClientResponse.class);
        if (rsp.getMetadata().getPagination().getCount() == 0) {
            return null;
        }

        return rsp.getItems().get(0);
    }

    public void deleteIscsiClient(int iscsiClientId) {
        DeleteIscsiClientRequest req = new DeleteIscsiClientRequest();
        req.setId(iscsiClientId);
        DeleteIscsiClientResponse rsp = call(req, DeleteIscsiClientResponse.class);
        if (rsp.resourceIsDeleted()) {
            logger.info(String.format("iscsi-client %s has been deleted, skip send delete req", iscsiClientId));
            return;
        }

        GetIscsiClientRequest gReq = new GetIscsiClientRequest();
        gReq.setId(iscsiClientId);
        retryUtilResourceDeleted(gReq, GetIscsiClientResponse.class);
    }

    public IscsiClientGroupModule createIscsiClientGroup(String name, List<Integer> iscsiGatewayIds, List<String> iscsiClientCodes) {
        CreateIscsiClientGroupRequest req = new CreateIscsiClientGroupRequest();
        req.setName(name);
        req.setIscsiGatewayIds(iscsiGatewayIds);
        req.setIscsiClientCodes(iscsiClientCodes);
        CreateIscsiClientGroupResponse rsp =  callErrorOut(req, CreateIscsiClientGroupResponse.class);
        // TODO xinfini not support yet
        // return retryUtilStateActive(req, CreateIscsiClientGroupResponse.class,(CreateIscsiClientGroupResponse gvp) -> gvp.toModule().getMetadata().getState().getState()).toModule();
        return rsp.toModule();
    }

    public IscsiClientGroupModule getIscsiClientGroup(int id) {
        GetIscsiClientGroupRequest req = new GetIscsiClientGroupRequest();
        req.setId(id);
        return call(req, GetIscsiClientGroupResponse.class).toModule();
    }

    public VolumeClientGroupMappingModule queryVolumeClientGroupMappingByGroupIdAndVolId(int groupId, int volId) {
        QueryVolumeClientGroupMappingRequest req = new QueryVolumeClientGroupMappingRequest();
        req.q = String.format("((spec.iscsi_client_group_id:%s) AND (spec.bs_volume_id:%s))", groupId, volId);
        QueryVolumeClientGroupMappingResponse rsp = queryErrorOut(req, QueryVolumeClientGroupMappingResponse.class);
        if (rsp.getMetadata().getPagination().getCount() == 0) {
            return null;
        }

        return rsp.getItems().get(0);
    }

    public List<VolumeClientGroupMappingModule> queryVolumeClientGroupMappingByVolId(int volId) {
        QueryVolumeClientGroupMappingRequest req = new QueryVolumeClientGroupMappingRequest();
        req.q = String.format("spec.bs_volume_id:%s", volId);
        return queryErrorOut(req, QueryVolumeClientGroupMappingResponse.class).getItems();
    }

    public List<VolumeClientMappingModule> queryVolumeClientMappingByVolId(int volId) {
        QueryVolumeClientMappingRequest req = new QueryVolumeClientMappingRequest();
        req.q = String.format("spec.bs_volume_id:%s", volId);
        return queryErrorOut(req, QueryVolumeClientMappingResponse.class).getItems();
    }


    public List<VolumeClientGroupMappingModule> queryVolumeClientGroupMappings() {
        QueryVolumeClientGroupMappingRequest req = new QueryVolumeClientGroupMappingRequest();
        return queryErrorOut(req, QueryVolumeClientGroupMappingResponse.class).getItems();
    }

    public IscsiClientGroupModule queryIscsiClientGroupByVolumeId(int volId) {
        QueryVolumeClientGroupMappingRequest req = new QueryVolumeClientGroupMappingRequest();
        req.q = String.format("spec.bs_volume_id:%s", volId);
        QueryVolumeClientGroupMappingResponse rsp = queryErrorOut(req, QueryVolumeClientGroupMappingResponse.class);
        if (rsp.getMetadata().getPagination().getCount() == 0) {
            return null;
        }

        return getIscsiClientGroup(rsp.getItems().get(0).getSpec().getIscsiClientGroupId());
    }
}
