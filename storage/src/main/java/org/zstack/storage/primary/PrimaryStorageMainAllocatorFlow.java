package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.PrimaryStorageConstant.AllocatorParams;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageMainAllocatorFlow extends NoRollbackFlow {
    private static CLogger logger = Utils.getLogger(PrimaryStorageMainAllocatorFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected PrimaryStorageOverProvisioningManager ratioMgr;

    private class Result {
        List<PrimaryStorageVO> result;
        String error;
    }

    @Transactional(readOnly = true)
    private Result allocate(Map data) {
        PrimaryStorageAllocationSpec spec = (PrimaryStorageAllocationSpec) data.get(AllocatorParams.SPEC);
        TypedQuery<PrimaryStorageVO> query;
        String errorInfo;
        if (spec.getRequiredPrimaryStorageUuid() != null) {
            String sql = "select pri from PrimaryStorageVO pri where pri.state = :priState and pri.status = :status and pri.uuid = :priUuid";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("priState", PrimaryStorageState.Enabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            query.setParameter("priUuid", spec.getRequiredPrimaryStorageUuid());
            errorInfo = String.format("required primary storage[uuid:%s] cannot satisfy conditions[state:%s, status:%s, size:%s]",
                    spec.getRequiredPrimaryStorageUuid(), PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getSize());
        } else if (spec.getRequiredHostUuid() != null) {
            String sql = "select pri" +
                    " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref, HostVO host" +
                    " where host.uuid = :huuid" +
                    " and host.clusterUuid = ref.clusterUuid" +
                    " and ref.primaryStorageUuid = pri.uuid" +
                    " and pri.status = :status" +
                    " and pri.state = :priState";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("huuid", spec.getRequiredHostUuid());
            query.setParameter("priState", PrimaryStorageState.Enabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            errorInfo = String.format("cannot find primary storage satisfying conditions" +
                            "[attached to cluster having host:%s, state:%s, status: %s, available capacity > %s",
                    spec.getRequiredHostUuid(), PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getSize());
        } else if (spec.getRequiredClusterUuids() != null && !spec.getRequiredClusterUuids().isEmpty()) {
            String sql = "select pri" +
                    " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref, ClusterVO cluster" +
                    " where cluster.uuid = ref.clusterUuid" +
                    " and ref.primaryStorageUuid = pri.uuid" +
                    " and pri.status = :status" +
                    " and pri.state = :priState" +
                    " and cluster.uuid in (:clusterUuids)";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("clusterUuids", spec.getRequiredClusterUuids());
            query.setParameter("status", PrimaryStorageStatus.Connected);
            query.setParameter("priState", PrimaryStorageState.Enabled);
            errorInfo = String.format("cannot find primary storage satisfying conditions" +
                            "[attached to clusters:%s, state:%s, status:%s, available capacity > %s",
                    spec.getRequiredClusterUuids(), PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getSize());
        } else if (spec.getRequiredZoneUuid() != null) {
            String sql = "select pri from PrimaryStorageVO pri where pri.zoneUuid = :zoneUuid and pri.status = :status and pri.state = :priState";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("priState", PrimaryStorageState.Enabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            query.setParameter("zoneUuid", spec.getRequiredZoneUuid());
            errorInfo = String.format("cannot find primary storage satisfying conditions[in zone:%s, state:%s, status:%s, available capacity > %s",
                    spec.getRequiredZoneUuid(), PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getSize());
        } else {
            String sql = "select pri from PrimaryStorageVO pri where pri.status = :status and pri.state = :priState";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("priState", PrimaryStorageState.Enabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            errorInfo = String.format("cannot find primary storage satisfying conditions[state:%s, status:%s, available capacity > %s",
                    PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getSize());
        }

        List<PrimaryStorageVO> vos = query.getResultList();

        if (spec.getRequiredPrimaryStorageTypes() != null && !spec.getRequiredPrimaryStorageTypes().isEmpty()) {
            Iterator<PrimaryStorageVO> it = vos.iterator();
            while (it.hasNext()) {
                PrimaryStorageVO psvo = it.next();
                if (!spec.getRequiredPrimaryStorageTypes().contains(psvo.getType())) {
                    logger.debug(String.format("the primary storage[name:%s, uuid:%s, type:%s] is not in required primary storage types[%s]," +
                            " remove it", psvo.getName(), psvo.getUuid(), psvo.getType(), spec.getRequiredPrimaryStorageTypes()));
                    it.remove();
                }
            }
        }


        List<PrimaryStorageVO> res = new ArrayList<>();
        if (PrimaryStorageAllocationPurpose.CreateNewVm.toString().equals(spec.getPurpose())) {
            res.addAll(considerImageCache(spec, vos));
        } else {
            for (PrimaryStorageVO vo : vos) {
                if (ratioMgr.calculatePrimaryStorageAvailableCapacityByRatio(vo.getUuid(),
                        vo.getCapacity().getAvailableCapacity()) > spec.getSize()) {
                    res.add(vo);
                }
            }
        }

        Result ret = new Result();
        ret.error = errorInfo;
        ret.result = res;
        return ret;
    }

    @Transactional(readOnly = true)
    private Collection<? extends PrimaryStorageVO> considerImageCache(PrimaryStorageAllocationSpec spec, List<PrimaryStorageVO> vos) {
        List<PrimaryStorageVO> res = new ArrayList<>();
        if (vos.isEmpty()) {
            return res;
        }

        List<String> psUuids = CollectionUtils.transformToList(vos, new Function<String, PrimaryStorageVO>() {
            @Override
            public String call(PrimaryStorageVO arg) {
                return arg.getUuid();
            }
        });

        String sql = "select i.primaryStorageUuid from ImageCacheVO i where i.primaryStorageUuid in (:psUuids) and i.imageUuid = :iuuid";
        TypedQuery<String> iq = dbf.getEntityManager().createQuery(sql, String.class);
        iq.setParameter("psUuids", psUuids);
        iq.setParameter("iuuid", spec.getImageUuid());
        List<String> hasImagePrimaryStorage = iq.getResultList();

        sql = "select i.actualSize from ImageVO i where i.uuid = :uuid";
        TypedQuery<Long> sq = dbf.getEntityManager().createQuery(sql, Long.class);
        sq.setParameter("uuid", spec.getImageUuid());
        long imageSize = sq.getSingleResult();

        for (PrimaryStorageVO vo : vos) {
            long requiredSize = ratioMgr.calculateByRatio(vo.getUuid(), spec.getSize());
            if (!hasImagePrimaryStorage.contains(vo.getUuid())) {
                requiredSize += imageSize;
            }

            if (vo.getCapacity().getAvailableCapacity() >= requiredSize) {
                res.add(vo);
            }
        }

        return res;
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        Result ret = allocate(data);
        if (ret.result.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(ret.error));
        }

        data.put(AllocatorParams.CANDIDATES, ret.result);
        trigger.next();
    }
}
