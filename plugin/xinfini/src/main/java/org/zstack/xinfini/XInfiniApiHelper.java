package org.zstack.xinfini;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import org.zstack.xinfini.sdk.iscsi.GatewayModule;
import org.zstack.xinfini.sdk.iscsi.QueryGatewayRequest;
import org.zstack.xinfini.sdk.iscsi.QueryGatewayResponse;
import org.zstack.xinfini.sdk.metric.PoolMetrics;
import org.zstack.xinfini.sdk.metric.QueryMetricRequest;
import org.zstack.xinfini.sdk.metric.QueryMetricResponse;
import org.zstack.xinfini.sdk.node.*;
import org.zstack.xinfini.sdk.pool.*;
import org.zstack.xinfini.sdk.vhost.*;
import org.zstack.xinfini.sdk.volume.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class XInfiniApiHelper {

    private static CLogger logger = Utils.getLogger(XInfiniApiHelper.class);
    XInfiniClient client;

    private static final Cache<String, String> snapshotClientCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build();

    XInfiniApiHelper(XInfiniClient client) {
        this.client = client;
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

    public List<NodeModule> queryNodes() {
        QueryNodeRequest req = new QueryNodeRequest();
        return queryErrorOut(req, QueryNodeResponse.class).getItems();
    }

    public NodeModule getNode(int id) {
        GetNodeRequest req = new GetNodeRequest();
        req.setId(id);
        return callErrorOut(req, GetNodeResponse.class).toModule();
    }

    public List<GatewayModule> queryGateways() {
        QueryGatewayRequest req = new QueryGatewayRequest();
        return queryErrorOut(req, QueryGatewayResponse.class).getItems();
    }

    public List<BdcModule> queryBdcs() {
        QueryBdcRequest req = new QueryBdcRequest();
        return queryErrorOut(req, QueryBdcResponse.class).getItems();
    }

    public BdcModule queryBdcByIp(String ip) {
        QueryBdcRequest req = new QueryBdcRequest();
        req.q = String.format("spec.ip:%s", ip);
        QueryBdcResponse rsp = queryErrorOut(req, QueryBdcResponse.class);
        if (rsp.getMetadata().getPagination().getCount() == 0) {
            return null;
        }

        return rsp.getItems().get(0);
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
        BsPolicyModule policy = getBsPolicy(pool.getSpec().getDefaultBsPolicyId());
        PoolCapacity capacity = new PoolCapacity();
        long usedCapacity = SizeUnit.KILOBYTE.toByte(getPoolMetricValue(PoolMetrics.USED_KBYTES, pool)) / policy.getSpec().getDataReplicaNum();
        capacity.setTotalCapacity(SizeUnit.KILOBYTE.toByte(getPoolMetricValue(PoolMetrics.TOTAL_KBYTES, pool)) / policy.getSpec().getDataReplicaNum());
        capacity.setAvailableCapacity(capacity.getTotalCapacity() - usedCapacity);
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
        return retryUtilStateActive(req, GetVolumeResponse.class,
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
        return retryUtilStateActive(req, GetVolumeSnapshotResponse.class,
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
        return retryUtilStateActive(req, GetVolumeResponse.class,
                (GetVolumeResponse gvp) -> gvp.toModule().getMetadata().getState().getState()).toModule();
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
        return retryUtilStateActive(req, GetBdcBdevResponse.class,
                (GetBdcBdevResponse gvp) -> gvp.toModule().getMetadata().getState().getState()).toModule();
    }

    public BdcBdevModule queryBdcBdevByVolumeIdAndBdcId(int volId, int bdcId) {
        QueryBdcBdevRequest req = new QueryBdcBdevRequest();
        req.q = String.format("((spec.bdc_id:%s) AND (spec.bs_volume_id:%s)", bdcId, volId);
        QueryBdcBdevResponse rsp = queryErrorOut(req, QueryBdcBdevResponse.class);
        if (rsp.getMetadata().getPagination().getCount() == 1) {
            return null;
        }

        return rsp.getItems().get(0);
    }

    public BdcBdevModule getBdcBdev(int id) {
        GetBdcBdevRequest req = new GetBdcBdevRequest();
        req.setId(id);
        return callErrorOut(req, GetBdcBdevResponse.class).toModule();
    }

    public void deleteBdcBdev(int bdevId) {
        GetBdcBdevRequest gReq = new GetBdcBdevRequest();
        gReq.setId(bdevId);
        GetBdcBdevResponse rsp = call(gReq, GetBdcBdevResponse.class);
        if (rsp.resourceIsDeleted()) {
            logger.info(String.format("bdev %s has been deleted, skip send delete req", bdevId));
            return;
        }

        DeleteBdcBdevRequest req = new DeleteBdcBdevRequest();
        req.setId(bdevId);
        callErrorOut(req, DeleteBdcBdevResponse.class);
    }

    public void deleteVolume(int volId, boolean force) {
        GetVolumeRequest gReq = new GetVolumeRequest();
        gReq.setId(volId);
        GetVolumeResponse rsp = call(gReq, GetVolumeResponse.class);
        if (rsp.resourceIsDeleted()) {
            logger.info(String.format("volume %s has been deleted, skip send delete req", volId));
            return;
        }

        DeleteVolumeRequest req = new DeleteVolumeRequest();
        req.setId(volId);
        callErrorOut(req, DeleteVolumeResponse.class);
    }

    public void deleteVolumeSnapshot(int snapShotId) {
        GetVolumeSnapshotRequest gReq = new GetVolumeSnapshotRequest();
        gReq.setId(snapShotId);
        GetVolumeSnapshotResponse rsp = call(gReq, GetVolumeSnapshotResponse.class);
        if (rsp.resourceIsDeleted()) {
            logger.info(String.format("volume snapshot %s has been deleted, skip send delete req", snapShotId));
            return;
        }

        // check snapshot cloned volume
        if (snapshotHasClonedVolume(snapShotId)) {
            throw new OperationFailureException(operr("snapshot[id: %s] has cloned volume, please delete or flatten volumes", snapShotId));
        }

        DeleteVolumeSnapshotRequest req = new DeleteVolumeSnapshotRequest();
        req.setId(snapShotId);
        callErrorOut(req, DeleteVolumeSnapshotResponse.class);
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

    public VolumeSnapshotModule queryVolumeSnapshotByName(String name) {
        QueryVolumeSnapshotRequest req = new QueryVolumeSnapshotRequest();
        req.q = String.format("spec.name:%s", name);
        QueryVolumeSnapshotResponse rsp = queryErrorOut(req, QueryVolumeSnapshotResponse.class);
        if (rsp.getMetadata().getPagination().getCount() == 0) {
            return null;
        }
        return rsp.getItems().get(0);
    }
}
