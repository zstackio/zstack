package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.primary.*;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;
import org.zstack.utils.CollectionUtils;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

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
            throw new ApiMessageInterceptionException(argerr("zoneUuids, clusterUuids, primaryStorageUuids must have at least one be none-empty list, or all is set to true"));
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
            throw new ApiMessageInterceptionException(argerr("primary storage[uuid:%s] has not been attached to cluster[uuid:%s] yet",
                            msg.getPrimaryStorageUuid(), msg.getClusterUuid()));
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
                throw new ApiMessageInterceptionException(operr("primary storage[uuid:%s] has been attached to cluster[uuid:%s]",
                                msg.getPrimaryStorageUuid(), msg.getClusterUuid()));
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
                throw new ApiMessageInterceptionException(argerr("primary storage[uuid:%s] and cluster[uuid:%s] are not in the same zone",
                                msg.getPrimaryStorageUuid(), msg.getClusterUuid()));
            }
        }
        {
            String url = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.url)
                    .eq(PrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid())
                    .findValue();
            long count = SQL.New("select count(ps)" +
                    " from PrimaryStorageVO ps" +
                    " where ps.uuid in" +
                    " (" +
                    " select ref.primaryStorageUuid from PrimaryStorageClusterRefVO ref" +
                    " where ref.clusterUuid = :clusterUuid" +
                    " )" +
                    " and ps.url = :url", Long.class)
                    .param("clusterUuid", msg.getClusterUuid())
                    .param("url", url)
                    .find();

            if(count > 0){
                throw new ApiMessageInterceptionException(
                        argerr("url[%s] has been occupied, it cannot be duplicate in same cluster",
                        url));
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
            throw new ApiMessageInterceptionException(operr("primary storage[uuid:%s] cannot be deleted for still " +
                            "being attached to cluster[uuid:%s].",
                    msg.getPrimaryStorageUuid(), clusterUuidsString));
        }
    }
}
