package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.storage.primary.PrimaryStorageCapacityUpdaterRunnable;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.AgentResponse;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

/**
 * Created by frank on 11/10/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalStorageCapacityUpdater {
    private static CLogger logger = Utils.getLogger(LocalStorageCapacityUpdater.class);

    @Autowired
    private DatabaseFacade dbf;

    @Transactional
    public void updatePhysicalCapacityByKvmAgentResponse(String psUuid, String hostUuid, AgentResponse rsp) {
        String sqlLocalStorageHostRefVO = "select ref" +
                " from LocalStorageHostRefVO ref" +
                " where hostUuid = :hostUuid" +
                " and primaryStorageUuid = :primaryStorageUuid";
        TypedQuery<LocalStorageHostRefVO> query = dbf.getEntityManager().
                createQuery(sqlLocalStorageHostRefVO, LocalStorageHostRefVO.class);
        query.setParameter("hostUuid", hostUuid);
        query.setParameter("primaryStorageUuid", psUuid);
        LocalStorageHostRefVO ref = query.setLockMode(LockModeType.PESSIMISTIC_WRITE).getSingleResult();
        if (ref == null) {
            return;
        }

        if (ref.getAvailablePhysicalCapacity() == rsp.getAvailableCapacity()
                && ref.getTotalPhysicalCapacity() == rsp.getTotalCapacity()) {
            return;
        }

        long originalPhysicalTotal = ref.getTotalPhysicalCapacity();
        long originalPhysicalAvailable = ref.getAvailablePhysicalCapacity();

        ref.setTotalPhysicalCapacity(rsp.getTotalCapacity());
        ref.setAvailablePhysicalCapacity(rsp.getAvailableCapacity());
        dbf.getEntityManager().merge(ref);

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[Local Storage Capacity] changed the physical capacity of the host[uuid:%s] of " +
                            "the local primary storage[uuid:%s] as:\n" +
                            "physical total: %s --> %s\n" +
                            "physical available: %s --> %s\n",
                    hostUuid, psUuid, originalPhysicalTotal, ref.getTotalPhysicalCapacity(),
                    originalPhysicalAvailable, ref.getAvailablePhysicalCapacity()));
        }

        final long totalChange = rsp.getTotalCapacity() - ref.getTotalPhysicalCapacity();
        final long availChange = rsp.getAvailableCapacity() - ref.getAvailablePhysicalCapacity();

        new PrimaryStorageCapacityUpdater(psUuid).run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                cap.setTotalPhysicalCapacity(cap.getTotalPhysicalCapacity() + totalChange);
                cap.setAvailablePhysicalCapacity(cap.getAvailablePhysicalCapacity() + availChange);
                return cap;
            }
        });
    }
}
