package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.APIAttachIsoToVmInstanceMsg;
import org.zstack.header.vm.VmInstanceAO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.Volume;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;
import org.zstack.utils.CollectionUtils;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 4:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class PrimaryStorageApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof PrimaryStorageMessage) {
            PrimaryStorageMessage pmsg = (PrimaryStorageMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, pmsg.getPrimaryStorageUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeletePrimaryStorageMsg) {
            validate((APIDeletePrimaryStorageMsg) msg);
        } else if (msg instanceof APIAttachPrimaryStorageToClusterMsg) {
            validate((APIAttachPrimaryStorageToClusterMsg) msg);
        } else if (msg instanceof APIDetachPrimaryStorageFromClusterMsg) {
            validate((APIDetachPrimaryStorageFromClusterMsg) msg);
        } else if (msg instanceof APIGetPrimaryStorageCapacityMsg) {
            validate((APIGetPrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof APIAttachIsoToVmInstanceMsg) {
            validate((APIAttachIsoToVmInstanceMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APIGetPrimaryStorageCapacityMsg msg) {
        boolean pass = false;
        if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
            pass = true;
        }
        if (msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty()) {
            pass = true;
        }
        if (msg.getPrimaryStorageUuids() != null && !msg.getPrimaryStorageUuids().isEmpty()) {
            pass = true;
        }

        if (!pass && !msg.isAll()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("zoneUuids, clusterUuids, primaryStorageUuids must have at least one be none-empty list, or all is set to true")
            ));
        }

        if (msg.isAll() && (msg.getZoneUuids() == null || msg.getZoneUuids().isEmpty())) {
            SimpleQuery<ZoneVO> q = dbf.createQuery(ZoneVO.class);
            q.select(ZoneVO_.uuid);
            List<String> zuuids = q.listValue();
            msg.setZoneUuids(zuuids);

            if (msg.getZoneUuids().isEmpty()) {
                APIGetPrimaryStorageCapacityReply reply = new APIGetPrimaryStorageCapacityReply();
                bus.reply(msg, reply);
                throw new StopRoutingException();
            }
        }
    }

    private void validate(APIDetachPrimaryStorageFromClusterMsg msg) {
        SimpleQuery<PrimaryStorageClusterRefVO> q = dbf.createQuery(PrimaryStorageClusterRefVO.class);
        q.add(PrimaryStorageClusterRefVO_.clusterUuid, Op.EQ, msg.getClusterUuid());
        q.add(PrimaryStorageClusterRefVO_.primaryStorageUuid, Op.EQ, msg.getPrimaryStorageUuid());
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("primary storage[uuid:%s] has not been attached to cluster[uuid:%s] yet",
                            msg.getPrimaryStorageUuid(), msg.getClusterUuid())
            ));
        }
    }

    @Transactional
    private void validate(APIAttachPrimaryStorageToClusterMsg msg) {
        {
            String sql = "select count(ref)" +
                    " from PrimaryStorageClusterRefVO ref" +
                    " where ref.clusterUuid = :clusterUuid" +
                    " and ref.primaryStorageUuid = :psUuid";
            TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
            q.setParameter("psUuid", msg.getPrimaryStorageUuid());
            q.setParameter("clusterUuid", msg.getClusterUuid());
            long count = q.getSingleResult();
            if (count != 0) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                        String.format("primary storage[uuid:%s] has been attached to cluster[uuid:%s]",
                                msg.getPrimaryStorageUuid(), msg.getClusterUuid())
                ));
            }
        }
        {
            String sql = "select count(ps)" +
                    " from PrimaryStorageVO ps, ClusterVO cluster" +
                    " where cluster.zoneUuid = ps.zoneUuid" +
                    " and cluster.uuid = :clusterUuid" +
                    " and ps.uuid = :psUuid";
            TypedQuery<Long> jq = dbf.getEntityManager().createQuery(sql, Long.class);
            jq.setParameter("psUuid", msg.getPrimaryStorageUuid());
            jq.setParameter("clusterUuid", msg.getClusterUuid());
            long count = jq.getSingleResult();
            if (count == 0) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("primary storage[uuid:%s] and cluster[uuid:%s] are not in the same zone",
                                msg.getPrimaryStorageUuid(), msg.getClusterUuid())
                ));
            }
        }
    }

    private void validate(APIDeletePrimaryStorageMsg msg) {
        if (!dbf.isExist(msg.getUuid(), PrimaryStorageVO.class)) {
            APIDeletePrimaryStorageEvent evt = new APIDeletePrimaryStorageEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

        SimpleQuery<PrimaryStorageClusterRefVO> sq = dbf.createQuery(PrimaryStorageClusterRefVO.class);
        sq.add(PrimaryStorageClusterRefVO_.primaryStorageUuid, Op.EQ, msg.getPrimaryStorageUuid());
        List<PrimaryStorageClusterRefVO> pscRefs = sq.list();
        if (!pscRefs.isEmpty()) {
            String clusterUuidsString = pscRefs.stream()
                    .map(PrimaryStorageClusterRefVO::getClusterUuid)
                    .collect(Collectors.joining(", "));
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("primary storage[uuid:%s] cannot be deleted for still " +
                                    "being attached to cluster[uuid:%s].",
                            msg.getPrimaryStorageUuid(), clusterUuidsString)
            ));
        }
    }
    private void validate(APIAttachIsoToVmInstanceMsg msg) {

        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.add(VolumeVO_.vmInstanceUuid, Op.EQ, msg.getVmInstanceUuid()).
                add(VolumeVO_.type, Op.EQ, VolumeType.Root);
        q.select(VolumeVO_.primaryStorageUuid);
        final String psUuid = q.findValue();

        if(dbf.findByUuid(psUuid,PrimaryStorageVO.class).getState() == PrimaryStorageState.Maintenance){
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("ISO cannot be attach to a vm whose primary storage[uuid:%s] is 'Maintenance'",psUuid)));
        }
    }
}
