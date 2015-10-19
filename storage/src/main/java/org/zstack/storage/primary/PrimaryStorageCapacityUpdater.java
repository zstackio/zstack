package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.storage.primary.PrimaryStorageCapacityUpdaterRunnable;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 10/19/2015.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageCapacityUpdater {
    private static CLogger logger = Utils.getLogger(PrimaryStorageCapacityUpdater.class);

    @Autowired
    private DatabaseFacade dbf;

    private String primaryStorageUuid;
    private TypedQuery<PrimaryStorageCapacityVO> query;
    private PrimaryStorageCapacityVO capacityVO;

    private long totalForLog;
    private long availForLog;
    private long totalPhysicalForLog;
    private long availPhysicalForLog;

    public PrimaryStorageCapacityUpdater(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public PrimaryStorageCapacityUpdater(TypedQuery<PrimaryStorageCapacityVO> query) {
        this.query = query;
    }

    private void logDeletedPrimaryStorage() {
        logger.warn(String.format("[Primary Storage Capacity] unable to update capacity for the primary storage[uuid:%s]. It may have been deleted, cannot find it in database",
                primaryStorageUuid));
    }

    private void logCapacityChange() {
        if (logger.isTraceEnabled()) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            int index = 0;
            String fileName = PrimaryStorageCapacityUpdater.class.getSimpleName() + ".java";
            for (int i=0; i<stackTraceElements.length; i++) {
                if (fileName.equals(stackTraceElements[i].getFileName())) {
                    index = i;
                }
            }
            StackTraceElement caller = stackTraceElements[index+1];
            logger.trace(String.format("[Primary Storage Capacity] %s:%s:%s changed capacity of the primary storage[uuid:%s] as:\n" +
                    "total: %s --> %s\n" +
                    "available: %s --> %s\n" +
                    "physical total: %s --> %s\n" +
                    "physical available: %s --> %s\n", caller.getFileName(), caller.getMethodName(), caller.getLineNumber(), capacityVO.getUuid(),
                    totalForLog, capacityVO.getTotalCapacity(),
                    availForLog, capacityVO.getAvailableCapacity(),
                    totalPhysicalForLog, capacityVO.getTotalPhysicalCapacity(),
                    availPhysicalForLog, capacityVO.getAvailablePhysicalCapacity()));
        }
    }

    private boolean lockCapacity() {
        if (primaryStorageUuid != null) {
            capacityVO = dbf.getEntityManager().find(PrimaryStorageCapacityVO.class, primaryStorageUuid, LockModeType.PESSIMISTIC_WRITE);
        } else if (query != null) {
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
            List<PrimaryStorageCapacityVO> caps = query.getResultList();
            capacityVO = caps.isEmpty() ? null : caps.get(0);
        }

        if (capacityVO != null) {
            totalForLog = capacityVO.getTotalCapacity();
            availForLog = capacityVO.getAvailableCapacity();
            totalPhysicalForLog = capacityVO.getTotalPhysicalCapacity();
            availPhysicalForLog = capacityVO.getAvailablePhysicalCapacity();
        }

        return capacityVO != null;
    }

    private void merge() {
        capacityVO = dbf.getEntityManager().merge(capacityVO);
        logCapacityChange();
    }

    @Transactional
    public boolean updateAvailableCapacity(long avail) {
        if (!lockCapacity()) {
            logDeletedPrimaryStorage();
            return false;
        }

        capacityVO.setAvailableCapacity(avail);
        merge();
        return true;
    }

    @Transactional
    public boolean increaseAvailableCapacity(long size) {
        if (!lockCapacity()) {
            logDeletedPrimaryStorage();
            return false;
        }

        long n = capacityVO.getAvailableCapacity() + size;
        if (n > capacityVO.getTotalCapacity()) {
            throw new CloudRuntimeException(String.format("invalid primary storage[uuid:%s] capacity, available capacity[%s] > total capacity[%s]",
                    capacityVO.getUuid(), n, capacityVO.getTotalCapacity()));
        }

        capacityVO.setAvailableCapacity(n);
        merge();
        return true;
    }

    @Transactional
    public boolean decreaseAvailableCapacity(long size) {
        if (!lockCapacity()) {
            logDeletedPrimaryStorage();
            return false;
        }

        capacityVO.setAvailableCapacity(capacityVO.getAvailableCapacity() - size);
        merge();
        return true;
    }

    @Transactional
    public boolean update(Long total, Long avail, Long physicalTotal, Long physicalAvail) {
        if (!lockCapacity()) {
            logDeletedPrimaryStorage();
            return false;
        }

        if (total != null) {
            capacityVO.setTotalCapacity(total);
        }
        if (avail != null) {
            capacityVO.setAvailableCapacity(avail);
        }
        if (physicalTotal != null) {
            capacityVO.setTotalPhysicalCapacity(physicalTotal);
        }
        if (physicalAvail != null) {
            capacityVO.setAvailablePhysicalCapacity(physicalAvail);
        }
        merge();
        return true;
    }

    @Transactional
    public boolean run(PrimaryStorageCapacityUpdaterRunnable runnable) {
        if (!lockCapacity()) {
            logDeletedPrimaryStorage();
            return false;
        }

        PrimaryStorageCapacityVO cap = runnable.call(capacityVO);
        if (cap != null) {
            capacityVO = cap;
            merge();
            return true;
        }
        return false;
    }
}
