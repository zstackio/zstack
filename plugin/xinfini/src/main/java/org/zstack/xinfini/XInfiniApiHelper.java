package org.zstack.xinfini;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.Q;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.volume.VolumeConfigs;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.header.xinfini.XInfiniConstants;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.xinfini.sdk.MetadataState;
import org.zstack.xinfini.sdk.XInfiniClient;
import org.zstack.xinfini.sdk.XInfiniQueryRequest;
import org.zstack.xinfini.sdk.XInfiniQueryResponse;
import org.zstack.xinfini.sdk.XInfiniRequest;
import org.zstack.xinfini.sdk.XInfiniResponse;
import org.zstack.xinfini.sdk.cluster.QueryClusterRequest;
import org.zstack.xinfini.sdk.cluster.QueryClusterResponse;
import org.zstack.xinfini.sdk.iscsi.AddVolumeClientGroupMappingRequest;
import org.zstack.xinfini.sdk.iscsi.AddVolumeClientGroupMappingResponse;
import org.zstack.xinfini.sdk.iscsi.CreateIscsiClientGroupRequest;
import org.zstack.xinfini.sdk.iscsi.CreateIscsiClientGroupResponse;
import org.zstack.xinfini.sdk.iscsi.CreateIscsiClientRequest;
import org.zstack.xinfini.sdk.iscsi.CreateIscsiClientResponse;
import org.zstack.xinfini.sdk.iscsi.DeleteIscsiClientRequest;
import org.zstack.xinfini.sdk.iscsi.DeleteIscsiClientResponse;
import org.zstack.xinfini.sdk.iscsi.DeleteVolumeClientGroupMappingRequest;
import org.zstack.xinfini.sdk.iscsi.DeleteVolumeClientGroupMappingResponse;
import org.zstack.xinfini.sdk.iscsi.GetIscsiClientGroupRequest;
import org.zstack.xinfini.sdk.iscsi.GetIscsiClientGroupResponse;
import org.zstack.xinfini.sdk.iscsi.GetIscsiClientRequest;
import org.zstack.xinfini.sdk.iscsi.GetIscsiClientResponse;
import org.zstack.xinfini.sdk.iscsi.GetVolumeClientGroupMappingRequest;
import org.zstack.xinfini.sdk.iscsi.GetVolumeClientGroupMappingResponse;
import org.zstack.xinfini.sdk.iscsi.IscsiClientGroupModule;
import org.zstack.xinfini.sdk.iscsi.IscsiClientModule;
import org.zstack.xinfini.sdk.iscsi.IscsiGatewayClientGroupMappingModule;
import org.zstack.xinfini.sdk.iscsi.IscsiGatewayModule;
import org.zstack.xinfini.sdk.iscsi.QueryIscsiClientGroupRequest;
import org.zstack.xinfini.sdk.iscsi.QueryIscsiClientGroupResponse;
import org.zstack.xinfini.sdk.iscsi.QueryIscsiClientRequest;
import org.zstack.xinfini.sdk.iscsi.QueryIscsiClientResponse;
import org.zstack.xinfini.sdk.iscsi.QueryIscsiGatewayClientGroupMappingRequest;
import org.zstack.xinfini.sdk.iscsi.QueryIscsiGatewayClientGroupMappingResponse;
import org.zstack.xinfini.sdk.iscsi.QueryIscsiGatewayRequest;
import org.zstack.xinfini.sdk.iscsi.QueryIscsiGatewayResponse;
import org.zstack.xinfini.sdk.iscsi.QueryVolumeClientGroupMappingRequest;
import org.zstack.xinfini.sdk.iscsi.QueryVolumeClientGroupMappingResponse;
import org.zstack.xinfini.sdk.iscsi.QueryVolumeClientMappingRequest;
import org.zstack.xinfini.sdk.iscsi.QueryVolumeClientMappingResponse;
import org.zstack.xinfini.sdk.iscsi.VolumeClientGroupMappingModule;
import org.zstack.xinfini.sdk.iscsi.VolumeClientMappingModule;
import org.zstack.xinfini.sdk.metric.PoolMetrics;
import org.zstack.xinfini.sdk.metric.QueryMetricRequest;
import org.zstack.xinfini.sdk.metric.QueryMetricResponse;
import org.zstack.xinfini.sdk.node.GetNodeRequest;
import org.zstack.xinfini.sdk.node.GetNodeResponse;
import org.zstack.xinfini.sdk.node.NodeModule;
import org.zstack.xinfini.sdk.node.QueryNodeRequest;
import org.zstack.xinfini.sdk.node.QueryNodeResponse;
import org.zstack.xinfini.sdk.pool.BsPolicyModule;
import org.zstack.xinfini.sdk.pool.GetBsPolicyRequest;
import org.zstack.xinfini.sdk.pool.GetBsPolicyResponse;
import org.zstack.xinfini.sdk.pool.GetPoolRequest;
import org.zstack.xinfini.sdk.pool.GetPoolResponse;
import org.zstack.xinfini.sdk.pool.PoolCapacity;
import org.zstack.xinfini.sdk.pool.PoolModule;
import org.zstack.xinfini.sdk.pool.QueryPoolRequest;
import org.zstack.xinfini.sdk.pool.QueryPoolResponse;
import org.zstack.xinfini.sdk.vhost.BdcBdevModule;
import org.zstack.xinfini.sdk.vhost.BdcModule;
import org.zstack.xinfini.sdk.vhost.BdcRunState;
import org.zstack.xinfini.sdk.vhost.CreateBdcBdevRequest;
import org.zstack.xinfini.sdk.vhost.CreateBdcBdevResponse;
import org.zstack.xinfini.sdk.vhost.DeleteBdcBdevRequest;
import org.zstack.xinfini.sdk.vhost.DeleteBdcBdevResponse;
import org.zstack.xinfini.sdk.vhost.GetBdcBdevRequest;
import org.zstack.xinfini.sdk.vhost.GetBdcBdevResponse;
import org.zstack.xinfini.sdk.vhost.GetBdcRequest;
import org.zstack.xinfini.sdk.vhost.GetBdcResponse;
import org.zstack.xinfini.sdk.vhost.QueryBdcBdevRequest;
import org.zstack.xinfini.sdk.vhost.QueryBdcBdevResponse;
import org.zstack.xinfini.sdk.vhost.QueryBdcRequest;
import org.zstack.xinfini.sdk.vhost.QueryBdcResponse;
import org.zstack.xinfini.sdk.volume.CloneVolumeRequest;
import org.zstack.xinfini.sdk.volume.CloneVolumeResponse;
import org.zstack.xinfini.sdk.volume.CreateVolumeRequest;
import org.zstack.xinfini.sdk.volume.CreateVolumeResponse;
import org.zstack.xinfini.sdk.volume.CreateVolumeSnapshotRequest;
import org.zstack.xinfini.sdk.volume.CreateVolumeSnapshotResponse;
import org.zstack.xinfini.sdk.volume.DeleteVolumeRequest;
import org.zstack.xinfini.sdk.volume.DeleteVolumeResponse;
import org.zstack.xinfini.sdk.volume.DeleteVolumeSnapshotRequest;
import org.zstack.xinfini.sdk.volume.DeleteVolumeSnapshotResponse;
import org.zstack.xinfini.sdk.volume.FlattenVolumeRequest;
import org.zstack.xinfini.sdk.volume.FlattenVolumeResponse;
import org.zstack.xinfini.sdk.volume.GetVolumeRequest;
import org.zstack.xinfini.sdk.volume.GetVolumeResponse;
import org.zstack.xinfini.sdk.volume.GetVolumeSnapshotRequest;
import org.zstack.xinfini.sdk.volume.GetVolumeSnapshotResponse;
import org.zstack.xinfini.sdk.volume.QueryVolumeRequest;
import org.zstack.xinfini.sdk.volume.QueryVolumeResponse;
import org.zstack.xinfini.sdk.volume.QueryVolumeSnapshotRequest;
import org.zstack.xinfini.sdk.volume.QueryVolumeSnapshotResponse;
import org.zstack.xinfini.sdk.volume.RollbackSnapshotRequest;
import org.zstack.xinfini.sdk.volume.RollbackSnapshotResponse;
import org.zstack.xinfini.sdk.volume.UpdateVolumeRequest;
import org.zstack.xinfini.sdk.volume.UpdateVolumeResponse;
import org.zstack.xinfini.sdk.volume.VolumeModule;
import org.zstack.xinfini.sdk.volume.VolumeSnapshotModule;
import org.zstack.xinfini.sdk.volume.XinfiniVolumeQos;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

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

    public <T extends XInfiniResponse> T callErrorOutWithNode(XInfiniRequest req, Class<T> clz, XInfiniConfig.Node node) {
        T rsp = client.call(req, clz, node);
        errorOut(rsp);
        return rsp;
    }

    public <T extends XInfiniResponse> T callErrorOut(XInfiniRequest req, Class<T> clz) {
        T rsp = client.call(req, clz);
        errorOut(rsp);
        return rsp;
    }

    public <T extends XInfiniResponse> T callErrorOutWithRetry(XInfiniRequest req, Class<T> clz, int retryTimes) {
        while (retryTimes-- > 0) {
            T rsp = client.call(req, clz);
            if (!rsp.isSuccess()) {
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                return rsp;
            }
        }

        throw new OperationFailureException(operr(ORG_ZSTACK_XINFINI_10000, "xinfini request failed, message: %s.",
                req.getClass().getSimpleName()));
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

            completion.fail(operr(ORG_ZSTACK_XINFINI_10001, "xinfini request failed, message: %s.", result.getMessage()));
        });
    }

    public void errorOut(XInfiniResponse rsp) {
        if (!rsp.isSuccess()) {
            throw new OperationFailureException(operr(ORG_ZSTACK_XINFINI_10002, "xinfini request failed, message: %s.", rsp.getMessage()));
        }
    }

    public <T extends XInfiniQueryResponse> T query(XInfiniQueryRequest req, Class<T> clz) {
        return call(req, clz);
    }

    public <T extends XInfiniQueryResponse> T queryErrorOut(XInfiniQueryRequest req, Class<T> clz) {
        return callErrorOut(req, clz);
    }

    public <T extends XInfiniQueryResponse> T queryErrorOut(XInfiniQueryRequest req, Class<T> clz, XInfiniConfig.Node node) {
        return callErrorOutWithNode(req, clz, node);
    }

    public List<PoolModule> queryPools() {
        QueryPoolRequest req = new QueryPoolRequest();
        return queryErrorOut(req, QueryPoolResponse.class).getItems();
    }

    public List<PoolModule> queryPools(XInfiniConfig.Node node) {
        QueryPoolRequest req = new QueryPoolRequest();
        return queryErrorOut(req, QueryPoolResponse.class, node).getItems();
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

    public String getClusterUuid(XInfiniConfig.Node node) {
        QueryClusterRequest req = new QueryClusterRequest();
        return queryErrorOut(req, QueryClusterResponse.class, node).toModule().getUuid();
    }

    public Map<String, NodeStatus> checkNodesConnection(List<XInfiniConfig.Node> nodes) {
        Map<String, NodeStatus> nodesStatus = Maps.newConcurrentMap();
        for (XInfiniConfig.Node node : nodes) {
            try {
                QueryClusterRequest req = new QueryClusterRequest();
                QueryClusterResponse rsp = callWithNode(req, QueryClusterResponse.class, node);
                nodesStatus.put(node.getIp(), rsp.isSuccess() ? NodeStatus.Connected : NodeStatus.Disconnected);
            } catch (Exception e) {
                logger.warn(String.format("get node %s connection failed, change node status to disconnected, %s",
                        node.getIp(), e.getMessage()));
                nodesStatus.put(node.getIp(), NodeStatus.Disconnected);
            }
        }

        return nodesStatus;
    }

    public List<NodeModule> queryNodes(XInfiniConfig.Node node) {
        QueryNodeRequest req = new QueryNodeRequest();
        return callErrorOutWithNode(req, QueryNodeResponse.class, node).getItems();
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
                throw new OperationFailureException(operr(ORG_ZSTACK_XINFINI_10003, "bdc with ip %s not found.", ip));
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

    public BsPolicyModule getBsPolicy(int id, XInfiniConfig.Node node) {
        GetBsPolicyRequest req = new GetBsPolicyRequest();
        req.setId(id);
        return callErrorOutWithNode(req, GetBsPolicyResponse.class, node).toModule();
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

    public PoolCapacity getPoolCapacity(PoolModule pool, XInfiniConfig.Node node) {
        PoolCapacity capacity = new PoolCapacity();
        long usedCapacity = SizeUnit.KILOBYTE.toByte(getPoolMetricValue(PoolMetrics.DATA_KBYTES, pool, node));
        long totalCapacity = SizeUnit.KILOBYTE.toByte(getPoolMetricValue(PoolMetrics.ACTUAL_KBYTES, pool, node));
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

    public long getPoolMetricValue(String metricName, PoolModule pool, XInfiniConfig.Node node) {
        QueryMetricRequest req = new QueryMetricRequest();
        req.metric = metricName;
        req.lables = String.format("pool_id=%s", pool.getSpec().getId());
        QueryMetricResponse rsp = callErrorOutWithNode(req, QueryMetricResponse.class, node);
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

    public VolumeModule queryVolumeByNameAndSnapId(String name, int snapId) {
        QueryVolumeRequest req = new QueryVolumeRequest();
        req.q = String.format("((spec.name:%s) AND (spec.bs_snap_id:%s))", name, snapId);
        QueryVolumeResponse rsp = queryErrorOut(req, QueryVolumeResponse.class);
        if (rsp.getMetadata().getPagination().getCount() == 0) {
            return null;
        }

        return rsp.getItems().get(0);
    }

    public VolumeModule queryVolumeById(int id) {
        QueryVolumeRequest req = new QueryVolumeRequest();
        req.q = String.format("spec.id:%s", id);
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

    public VolumeModule cloneVolume(int snapId, String name, String desc, boolean flatten, XinfiniVolumeQos qos) {
        CloneVolumeRequest req = new CloneVolumeRequest();
        req.setName(name);
        req.setDescription(desc);
        req.setBsSnapId(snapId);
        req.setFlatten(flatten);
        if (qos != null) {
            req.setQos(qos);
        }

        CloneVolumeResponse rsp = callErrorOut(req, CloneVolumeResponse.class);
        GetVolumeRequest gReq = new GetVolumeRequest();
        gReq.setId(rsp.getSpec().getId());
        return retryUtilStateActive(gReq, GetVolumeResponse.class,
                (GetVolumeResponse gvp) -> gvp.toModule().getMetadata().getState().getState()).toModule();
    }

    public VolumeModule flattenVolume(int volId) {
        FlattenVolumeRequest req = new FlattenVolumeRequest();
        req.setId(volId);
        req.setCreator(XInfiniConstants.DEFAULT_CREATOR);
        callErrorOut(req, FlattenVolumeResponse.class);
        GetVolumeRequest gReq = new GetVolumeRequest();
        gReq.setId(volId);
        return retryUtilStateActive(gReq, GetVolumeResponse.class,
                (GetVolumeResponse gvp) -> gvp.toModule().getMetadata().getState().getState()).toModule();
    }

    public VolumeModule expandVolume(int volId, long size) {
        UpdateVolumeRequest req = new UpdateVolumeRequest();
        req.setCreator(XInfiniConstants.DEFAULT_CREATOR);
        req.setId(volId);
        req.setSizeMb(size);
        callErrorOut(req, UpdateVolumeResponse.class);
        GetVolumeRequest gReq = new GetVolumeRequest();
        gReq.setId(volId);
        return retryUtilStateActive(gReq, GetVolumeResponse.class,
                (GetVolumeResponse gvp) -> gvp.toModule().getMetadata().getState().getState()).toModule();
    }

    public VolumeModule setVolumeQos(int volId, XinfiniVolumeQos qos) {
        UpdateVolumeRequest req = new UpdateVolumeRequest();
        req.setCreator(XInfiniConstants.DEFAULT_CREATOR);
        req.setId(volId);
        req.setQos(qos);
        callErrorOut(req, UpdateVolumeResponse.class);
        return getVolume(volId);
    }

    public VolumeModule deleteVolumeQos(int volId) {
        UpdateVolumeRequest req = new UpdateVolumeRequest();
        req.setCreator(XInfiniConstants.DEFAULT_CREATOR);
        req.setId(volId);
        req.setQos(new XinfiniVolumeQos());
        callErrorOut(req, UpdateVolumeResponse.class);
        return getVolume(volId);
    }

    private <T extends XInfiniResponse> void retryUtilResourceDeletedIn10Secs(XInfiniRequest req,
                                                                      Class<T> rsp) {
        new Retry<Void>() {
            @Override
            @RetryCondition(times = 5, interval = 2)
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

    private <T extends XInfiniResponse> void retryUtilResourceDeleted(XInfiniRequest req,
                                                                      Class<T> rsp) {
        new Retry<Void>() {
            @Override
            @RetryCondition(times = 150, interval = 2)
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

    public BdcBdevModule createBdcBdev(int bdcId, int volumeId, String name, VolumeConfigs vcfs) {
        CreateBdcBdevRequest req = new CreateBdcBdevRequest();
        req.setName(name);
        req.setBdcId(bdcId);
        req.setBsVolumeId(volumeId);
        req.setQueueNum(vcfs.getQueueNum());
        CreateBdcBdevResponse rsp = callErrorOutWithRetry(req, CreateBdcBdevResponse.class, 3);
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

    public BdcBdevModule getOrCreateBdcBdevByVolumeIdAndBdcId(int volId, int bdcId, String bdevName, VolumeConfigs vcfs) {
        QueryBdcBdevRequest req = new QueryBdcBdevRequest();
        req.q = String.format("((spec.bdc_id:%s) AND (spec.bs_volume_id:%s))", bdcId, volId);
        QueryBdcBdevResponse rsp = queryErrorOut(req, QueryBdcBdevResponse.class);
        if (rsp.getMetadata().getPagination().getCount() == 0) {
            return createBdcBdev(bdcId, volId, bdevName, vcfs);
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

    public void deleteBdcBdev(int bdevId, int bdcId) {
        DeleteBdcBdevRequest req = new DeleteBdcBdevRequest();
        req.setId(bdevId);
        DeleteBdcBdevResponse rsp = call(req, DeleteBdcBdevResponse.class);
        if (!rsp.isSuccess()) {
            if (rsp.resourceIsDeleted()) {
                logger.info(String.format("bdev %s has been deleted, skip send delete req", bdevId));
                return;
            }

            throw new OperationFailureException(operr(ORG_ZSTACK_XINFINI_10004, "delete bdev failed %s", rsp.getMessage()));
        }

        GetBdcBdevRequest gReq = new GetBdcBdevRequest();
        gReq.setId(bdevId);
        BdcModule bdc = getBdc(bdcId);
        if (!BdcRunState.Active.toString().equals(bdc.getStatus().getRunState())) {
            logger.info(String.format("bdc %s is not active, current %s, check bdev deleted in 30s",
                    bdcId, bdc.getStatus().getRunState()));
            retryUtilResourceDeletedIn10Secs(gReq, GetBdcBdevResponse.class);
        } else {
            retryUtilResourceDeleted(gReq, GetBdcBdevResponse.class);
        }
    }

    public VolumeModule rollbackSnapshot(int volId, int snapId) {
        RollbackSnapshotRequest req = new RollbackSnapshotRequest();
        req.setId(volId);
        req.setBsSnapId(snapId);
        callErrorOut(req, RollbackSnapshotResponse.class);
        GetVolumeRequest gReq = new GetVolumeRequest();
        gReq.setId(volId);
        return retryUtilStateActive(gReq, GetVolumeResponse.class,
                (GetVolumeResponse gvp) -> gvp.toModule().getMetadata().getState().getState()).toModule();
    }

    public void deleteVolume(int volId, boolean force) {
        List<VolumeClientGroupMappingModule> mappings = queryVolumeClientGroupMappingByVolId(volId);
        if (!CollectionUtils.isEmpty(mappings)) {
            logger.info(String.format("find volume %s has %s related client group mappings, delete mappings", volId, mappings.size()));
            for (VolumeClientGroupMappingModule mapping : mappings) {
                deleteVolumeClientGroupMapping(mapping.getSpec().getId());
            }
        }

        DeleteVolumeRequest req = new DeleteVolumeRequest();
        req.setId(volId);
        DeleteVolumeResponse rsp = call(req, DeleteVolumeResponse.class);
        if (!rsp.isSuccess()) {
            if (rsp.resourceIsDeleted()) {
                logger.info(String.format("volume %s has been deleted, skip send delete req", volId));
                return;
            }

            throw new OperationFailureException(operr(ORG_ZSTACK_XINFINI_10005, "delete volume failed %s", rsp.getMessage()));
        }

        GetVolumeRequest gReq = new GetVolumeRequest();
        gReq.setId(volId);
        retryUtilResourceDeleted(gReq, GetVolumeResponse.class);
    }

    public void deleteVolumeAndSnapshot(int volId) {
        for (VolumeSnapshotModule mod : queryVolumeSnapshotByVolumeId(volId)) {
            deleteVolumeSnapshot(mod.getSpec().getId());
        }
        deleteVolume(volId, true);
    }

    public void deleteVolumeSnapshot(int snapShotId) {
        // check snapshot cloned volume
        QueryVolumeRequest vReq = new QueryVolumeRequest();
        vReq.q = String.format("spec.bs_snap_id:%s", snapShotId);
        QueryVolumeResponse vRsp = queryErrorOut(vReq, QueryVolumeResponse.class);
        if (vRsp.getMetadata().getPagination().getCount() > 0) {
            List<String> volNames = vRsp.getItems().stream().map(VolumeModule::getSpec).map(VolumeModule.VolumeSpec::getName).collect(Collectors.toList());
            List<String> installPaths = vRsp.getItems().stream()
                    .map(v -> XInfiniPathHelper.buildXInfiniPath(v.getSpec().getPoolId(), v.getSpec().getId()))
                    .collect(Collectors.toList());

            logger.info(String.format("find cloned volumes paths: %s", installPaths));

            boolean exist = Q.New(VolumeVO.class)
                    .in(VolumeVO_.installPath, installPaths)
                    .isExists();

            if (exist) {
                VolumeSnapshotModule snap = getVolumeSnapshot(snapShotId);
                throw new OperationFailureException(operr(ORG_ZSTACK_XINFINI_10006, "snapshot [id:%s, name:%s] has %d cloned volumes, volumes names: %s", snapShotId, snap.getSpec().getName(), vRsp.getMetadata().getPagination().getCount(), volNames));
            }
            logger.info("all cloned volumes not exist in database, try to delete them");
            // try to delete cloned volumes if not exist in db
            for (VolumeModule v : vRsp.getItems()) {
                deleteVolumeAndSnapshot(v.getSpec().getId());
            }
        }

        DeleteVolumeSnapshotRequest req = new DeleteVolumeSnapshotRequest();
        req.setId(snapShotId);
        DeleteVolumeSnapshotResponse rsp = call(req, DeleteVolumeSnapshotResponse.class);

        if (!rsp.isSuccess()) {
            if (rsp.resourceIsDeleted()) {
                logger.info(String.format("volume snapshot %s has been deleted, skip send delete req", snapShotId));
                return;
            }

            throw new OperationFailureException(operr(ORG_ZSTACK_XINFINI_10007, "delete volume snapshot failed %s", rsp.getMessage()));
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
        if (!rsp.isSuccess()) {
            if (rsp.resourceIsDeleted()) {
                logger.info(String.format("volume-client-group-mapping %s has been deleted, skip send delete req", mapId));
                return;
            }

            throw new OperationFailureException(operr(ORG_ZSTACK_XINFINI_10008, "delete volume-client-group-mapping failed %s", rsp.getMessage()));
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
        if (!rsp.isSuccess()) {
            if (rsp.resourceIsDeleted()) {
                logger.info(String.format("iscsi-client %s has been deleted, skip send delete req", iscsiClientId));
                return;
            }

            throw new OperationFailureException(operr(ORG_ZSTACK_XINFINI_10009, "delete iscsi client failed %s", rsp.getMessage()));
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

        GetIscsiClientGroupRequest gReq = new GetIscsiClientGroupRequest();
        gReq.setId(rsp.getSpec().getId());
        return retryUtilStateActive(gReq, GetIscsiClientGroupResponse.class,(GetIscsiClientGroupResponse gvp) -> gvp.toModule().getMetadata().getState().getState()).toModule();
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
