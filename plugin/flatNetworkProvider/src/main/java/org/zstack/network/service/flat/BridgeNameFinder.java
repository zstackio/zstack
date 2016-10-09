package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.utils.TagUtils;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2016/4/4.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BridgeNameFinder {
    @Autowired
    private DatabaseFacade dbf;

    public String findByL3Uuid(String l3Uuid) {
        return findByL3Uuid(l3Uuid, true);
    }

    public String findByL3Uuid(String l3Uuid, boolean exceptionOnNotFound) {
        Map<String, String> bridgeNames = findByL3Uuids(list(l3Uuid));
        String brName = bridgeNames.get(l3Uuid);
        if (brName == null && exceptionOnNotFound) {
            throw new CloudRuntimeException(String.format("cannot find L2 bridge name for the L3 network[uuid:%s]", l3Uuid));
        }

        return brName;
    }

    @Transactional(readOnly = true)
    public Map<String, String> findByL3Uuids(Collection<String> l3Uuids) {
        String sql = "select t.tag, l3.uuid" +
                " from SystemTagVO t, L3NetworkVO l3" +
                " where t.resourceType = :ttype" +
                " and t.tag like :tag" +
                " and t.resourceUuid = l3.l2NetworkUuid" +
                " and l3.uuid in (:l3Uuids)" +
                " group by l3.uuid";
        TypedQuery<Tuple> tq = dbf.getEntityManager().createQuery(sql, Tuple.class);
        tq.setParameter("tag", TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.getTagFormat()));
        tq.setParameter("l3Uuids", l3Uuids);
        tq.setParameter("ttype", L2NetworkVO.class.getSimpleName());
        List<Tuple> ts = tq.getResultList();

        Map<String, String> bridgeNames = new HashMap<>();
        for (Tuple t : ts) {
            String brToken = t.get(0, String.class);
            String l3Uuid = t.get(1, String.class);
            bridgeNames.put(l3Uuid, KVMSystemTags.L2_BRIDGE_NAME.getTokenByTag(brToken, KVMSystemTags.L2_BRIDGE_NAME_TOKEN));
        }

        return bridgeNames;
    }
}
