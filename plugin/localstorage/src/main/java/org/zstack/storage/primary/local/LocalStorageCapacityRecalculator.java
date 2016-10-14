package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.storage.primary.PrimaryStorageCapacityUpdaterRunnable;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 1/23/2016.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalStorageCapacityRecalculator {
    private static final CLogger logger = Utils.getLogger(LocalStorageCapacityRecalculator.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;

    @Transactional
    public LocalStorageCapacityRecalculator calculateByHostUuids(String psUuid, List<String> huuids) {
        DebugUtils.Assert(!huuids.isEmpty(), "hostUuids cannot be empty");

        Map<String, Long> hostCap = new HashMap<>();

        String sql = "select sum(vol.size), ref.hostUuid" +
                " from VolumeVO vol, LocalStorageResourceRefVO ref" +
                " where vol.primaryStorageUuid = :psUuid" +
                " and vol.uuid = ref.resourceUuid" +
                " and ref.primaryStorageUuid = vol.primaryStorageUuid" +
                " and ref.hostUuid in (:huuids)" +
                " group by ref.hostUuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("psUuid", psUuid);
        q.setParameter("huuids", huuids);
        List<Tuple> ts = q.getResultList();
        for (Tuple t : ts) {
            if (t.get(0, Long.class) == null) {
                // no volume
                continue;
            }

            long cap = t.get(0, Long.class);
            String hostUuid = t.get(1, String.class);
            hostCap.put(hostUuid, ratioMgr.calculateByRatio(psUuid, cap));
        }

        for (String huuid : huuids) {
            // note: templates in image cache are physical size
            // do not calculate over provisioning for them
            sql = "select sum(i.size)" +
                    " from ImageCacheVO i" +
                    " where i.installUrl like :mark" +
                    " and i.primaryStorageUuid = :psUuid" +
                    " group by i.primaryStorageUuid";
            TypedQuery<Long> iq = dbf.getEntityManager().createQuery(sql, Long.class);
            iq.setParameter("psUuid", psUuid);
            iq.setParameter("mark", String.format("%%hostUuid://%s%%", huuid));
            List<Long> is = iq.getResultList();
            if (!is.isEmpty()) {
                Long isize = is.get(0);
                Long ncap = hostCap.get(huuid);
                ncap = ncap == null ? isize : ncap + isize;
                hostCap.put(huuid, ncap);
            } else {
                // if the host has no volume and image cache
                // set its used capacity to zero
                Long ncap = hostCap.get(huuid);
                ncap = ncap == null ? 0 : ncap;
                hostCap.put(huuid, ncap);
            }
        }

        for (Map.Entry<String, Long> e : hostCap.entrySet()) {
            String hostUuid = e.getKey();
            long used = e.getValue();

            LocalStorageHostRefVO ref = dbf.getEntityManager().find(LocalStorageHostRefVO.class, hostUuid, LockModeType.PESSIMISTIC_WRITE);
            long old = ref.getAvailableCapacity();
            long avail = ref.getTotalCapacity() - used - ref.getSystemUsedCapacity();
            ref.setAvailableCapacity(avail);
            dbf.getEntityManager().merge(ref);
            logger.debug(String.format("re-calculated available capacity[before:%s, now: %s] of host[uuid:%s] of the local storage[uuid:%s] with" +
                    " over-provisioning ratio[%s]", old, avail, hostUuid, psUuid, ratioMgr.getRatio(psUuid)));
        }

        return this;
    }

    @Transactional
    public LocalStorageCapacityRecalculator calculateByPrimaryStorageUuid(String psUuid) {
        // hmm, in some case, the mysql returns duplicate hostUuid
        // which I didn't figure out how. So use a groupby to remove the duplicates
        String sql = "select ref.hostUuid" +
                " from LocalStorageHostRefVO ref" +
                " where ref.primaryStorageUuid = :psUuid" +
                " group by ref.hostUuid";
        TypedQuery<String> hq = dbf.getEntityManager().createQuery(sql, String.class);
        hq.setParameter("psUuid", psUuid);
        List<String> huuids = hq.getResultList();
        if (!huuids.isEmpty()) {
            calculateByHostUuids(psUuid, huuids);
        }
        return this;
    }

    @Transactional
    public LocalStorageCapacityRecalculator calculateTotalCapacity(String psUuid) {
        String sql = "select sum(ref.totalCapacity), sum(ref.totalPhysicalCapacity), sum(ref.availablePhysicalCapacity)" +
                " from LocalStorageHostRefVO ref" +
                " where ref.primaryStorageUuid = :psUuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("psUuid", psUuid);
        Tuple ts = q.getSingleResult();

        final long total;
        final long tp;
        final long pa;
        PrimaryStorageCapacityUpdater pupdater = new PrimaryStorageCapacityUpdater(psUuid);
        if (ts != null) {
            total = ts.get(0) == null ? 0 : ts.get(0, Long.class);
            tp = ts.get(1) == null ? 0 : ts.get(1, Long.class);
            pa = ts.get(2) == null ? 0 : ts.get(2, Long.class);
        } else {
            total = 0;
            tp = 0;
            pa = 0;
        }
        pupdater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                cap.setTotalCapacity(total);
                cap.setTotalPhysicalCapacity(tp);
                cap.setAvailablePhysicalCapacity(pa);
                return cap;
            }
        });

        return this;
    }
}
