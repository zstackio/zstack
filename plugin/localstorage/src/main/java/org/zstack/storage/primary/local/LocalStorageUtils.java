package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.storage.primary.PrimaryStoragePhysicalCapacityManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created by lining on 2017/11/26.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalStorageUtils {

    private final static CLogger logger = Utils.getLogger(LocalStorageUtils.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    private PluginRegistry pluginRgty;

    public static boolean isOnlyAttachedLocalStorage(String clusterUuid) {
        boolean result = SQL.New("select pri.uuid" +
                " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                " where pri.uuid = ref.primaryStorageUuid" +
                " and ref.clusterUuid = :cuuid" +
                " and pri.type != :ptype", String.class)
                .param("cuuid", clusterUuid)
                .param("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE)
                .list().isEmpty();

        return result;
    }

    public static boolean isLocalStorage(String psUuid) {
        String psType = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.type)
                .eq(PrimaryStorageVO_.uuid, psUuid)
                .findValue();

        return LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(psType);
    }

    public long caculateSizeByRatio(String psUuid, long capacity) {
        return ratioMgr.calculateByRatio(psUuid, capacity);
    }

    @Transactional
    public void reserveCapacityOnHost(String hostUuid, long size, String psUuid, PrimaryStorageVO self, boolean ignoreError) {
        String sql = "select ref" +
                " from LocalStorageHostRefVO ref" +
                " where ref.hostUuid = :huuid" +
                " and ref.primaryStorageUuid = :psUuid";
        TypedQuery<LocalStorageHostRefVO> q = dbf.getEntityManager().createQuery(sql, LocalStorageHostRefVO.class);
        q.setParameter("huuid", hostUuid);
        q.setParameter("psUuid", psUuid);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        List<LocalStorageHostRefVO> refs = q.getResultList();

        if (refs.isEmpty()) {
            String errInfo = String.format("cannot find host[uuid: %s] of local primary storage[uuid: %s]",
                    hostUuid, self.getUuid());
            if (ignoreError) {
                logger.error(errInfo);
                return;
            } else {
                throw new CloudRuntimeException(errInfo);
            }
        }

        LocalStorageHostRefVO ref = refs.get(0);

        if (!ignoreError && !physicalCapacityMgr.checkCapacityByRatio(
                self.getUuid(),
                ref.getTotalPhysicalCapacity(),
                ref.getAvailablePhysicalCapacity())) {
            throw new OperationFailureException(operr("cannot reserve enough space for primary storage[uuid: %s] on host[uuid: %s], not enough physical capacity", self.getUuid(), hostUuid));
        }

        LocalStorageHostCapacityStruct s = new LocalStorageHostCapacityStruct();
        s.setLocalStorage(PrimaryStorageInventory.valueOf(self));
        s.setHostUuid(ref.getHostUuid());
        s.setSizeBeforeOverProvisioning(size);
        s.setSize(size);

        for (LocalStorageReserveHostCapacityExtensionPoint ext : pluginRgty.getExtensionList(
                LocalStorageReserveHostCapacityExtensionPoint.class)) {
            ext.beforeReserveLocalStorageCapacityOnHost(s);
        }

        long avail = ref.getAvailableCapacity() - s.getSize();
        if (avail < 0) {
            if (ignoreError) {
                avail = 0;
            } else {
                throw new OperationFailureException(operr("host[uuid: %s] of local primary storage[uuid: %s] doesn't have enough capacity" +
                        "[current: %s bytes, needed: %s]", hostUuid, self.getUuid(), ref.getAvailableCapacity(), size));
            }

        }

        logCapacityChange(psUuid, hostUuid, ref.getAvailableCapacity(), avail);
        ref.setAvailableCapacity(avail);
        dbf.getEntityManager().merge(ref);
    }

    @Transactional
    public void returnStorageCapacityToHost(String hostUuid, long size, PrimaryStorageVO self) {
        String sql = "select ref from LocalStorageHostRefVO ref where ref.hostUuid = :huuid and ref.primaryStorageUuid = :primaryStorageUuid";
        TypedQuery<LocalStorageHostRefVO> q = dbf.getEntityManager().createQuery(sql, LocalStorageHostRefVO.class);
        q.setParameter("huuid", hostUuid);
        q.setParameter("primaryStorageUuid", self.getUuid());
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        List<LocalStorageHostRefVO> refs = q.getResultList();

        if (refs.isEmpty()) {
            throw new CloudRuntimeException(String.format("cannot find host[uuid: %s] of local primary storage[uuid: %s]",
                    hostUuid, self.getUuid()));
        }

        LocalStorageHostRefVO ref = refs.get(0);

        LocalStorageHostCapacityStruct s = new LocalStorageHostCapacityStruct();
        s.setSizeBeforeOverProvisioning(size);
        s.setHostUuid(hostUuid);
        s.setLocalStorage(PrimaryStorageInventory.valueOf(self));
        s.setSize(size);

        for (LocalStorageReturnHostCapacityExtensionPoint ext : pluginRgty.getExtensionList(
                LocalStorageReturnHostCapacityExtensionPoint.class)) {
            ext.beforeReturnLocalStorageCapacityOnHost(s);
        }

        logCapacityChange(self.getUuid(), hostUuid, ref.getAvailableCapacity(), ref.getAvailableCapacity() + s.getSize());
        ref.setAvailableCapacity(ref.getAvailableCapacity() + s.getSize());
        dbf.getEntityManager().merge(ref);
    }

    public static String getHostUuidByResourceUuid(String resUuid) {
        String huuid;
        huuid = new SQLBatchWithReturn<String>() {
            private String findHostByUuid(String uuid) {
                return sql("select uuid from HostVO where uuid = :uuid", String.class).param("uuid", uuid).find();
            }

            @Override
            protected String scripts() {
                String uuid = sql("select hostUuid from LocalStorageResourceRefVO where resourceUuid = :resUuid", String.class)
                        .param("resUuid", resUuid)
                        .find();
                if (uuid == null) {
                    throw new OperationFailureException(operr("cannot find any host which has resource[uuid:%s]", resUuid));
                } else if (findHostByUuid(uuid) == null) {
                    throw new OperationFailureException(
                            operr("Resource[uuid:%s] can only be operated on host[uuid:%s], but the host has been deleted",
                                    resUuid, uuid));
                }
                return uuid;
            }
        }.execute();
        return huuid;
    }

    public static void logCapacityChange(String psUuid, String hostUuid, long before, long after) {
        if (logger.isTraceEnabled()) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            int index = 0;
            String fileName = LocalStorageUtils.class.getSimpleName() + ".java";
            for (int i = 0; i < stackTraceElements.length; i++) {
                if (fileName.equals(stackTraceElements[i].getFileName())) {
                    index = i;
                }
            }
            StackTraceElement caller = stackTraceElements[index + 1];
            logger.trace(String.format("[Local Storage Capacity] %s:%s:%s changed the capacity of the local storage[uuid:%s], host[uuid:%s] as:\n" +
                            "available: %s --> %s\n",
                    caller.getFileName(), caller.getMethodName(), caller.getLineNumber(), psUuid, hostUuid,
                    before, after));
        }
    }
}
