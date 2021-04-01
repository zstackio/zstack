package org.zstack.storage.ceph.primary.capacity;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.storage.primary.PrimaryStorageCapacityUpdaterRunnable;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.storage.ceph.CephCapacity;
import org.zstack.storage.ceph.CephPoolCapacity;
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lining on 2021/1/22.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class EnterpriseCephPrimaryCapacityBaseUpdater implements CephPrimaryCapacityUpdater {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void update(CephCapacity cephCapacity) {
        String fsid = cephCapacity.getFsid();
        List<CephPoolCapacity> poolCapacities = cephCapacity.getPoolCapacities();

        CephPrimaryStorageVO cephPs = SQL.New("select pri from CephPrimaryStorageVO pri where pri.fsid = :fsid",CephPrimaryStorageVO.class)
                .param("fsid", fsid)
                .find();
        if (cephPs == null) {
            return;
        }

        if (poolCapacities == null || poolCapacities.isEmpty()) {
            return;
        }

        List<String> poolNames = new ArrayList<>();
        List<Long> poolTotalCapacities = new ArrayList<>();
        List<Long> poolAvailableCapacities = new ArrayList<>();

        new SQLBatch() {
            @Override
            protected void scripts() {
                List<CephPrimaryStoragePoolVO> pools = sql("select pool from CephPrimaryStoragePoolVO pool, CephPrimaryStorageVO ps" +
                        " where pool.primaryStorageUuid = ps.uuid and ps.fsid = :fsid", CephPrimaryStoragePoolVO.class)
                        .param("fsid", fsid)
                        .list();
                if (pools == null) {
                    pools = new ArrayList<>();
                }

                for (CephPrimaryStoragePoolVO poolVO : pools) {
                    if (!poolCapacities.stream().anyMatch((e) -> poolVO.getPoolName().equals(e.getName()))) {
                        if (poolNames.contains(poolVO.getPoolName())) {
                            continue;
                        }
                        poolNames.add(poolVO.getPoolName());
                        poolAvailableCapacities.add(poolVO.getAvailableCapacity());
                        poolTotalCapacities.add(poolVO.getTotalCapacity());
                        continue;
                    }

                    CephPoolCapacity poolCapacity = poolCapacities.stream()
                            .filter(e -> poolVO.getPoolName().equals(e.getName()))
                            .findAny().get();

                    if (!poolNames.contains(poolVO.getPoolName())) {
                        poolNames.add(poolVO.getPoolName());
                        poolAvailableCapacities.add(poolCapacity.getAvailableCapacity());
                        poolTotalCapacities.add(poolCapacity.getTotalCapacity());
                    }

                    poolVO.setAvailableCapacity(poolCapacity.getAvailableCapacity());
                    poolVO.setReplicatedSize(poolCapacity.getReplicatedSize());
                    poolVO.setUsedCapacity(poolCapacity.getUsedCapacity());
                    poolVO.setTotalCapacity(poolCapacity.getTotalCapacity());
                    dbf.getEntityManager().merge(poolVO);
                }

                long psTotalPhysicalCapacity = poolTotalCapacities.stream().mapToLong(Long::longValue).sum();
                long psAvailablePhysicalCapacity = poolAvailableCapacities.stream().mapToLong(Long::longValue).sum();

                PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(cephPs.getUuid());
                updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
                    @Override
                    public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                        if (cap.getTotalCapacity() == 0 || cap.getAvailableCapacity() == 0) {
                            // init
                            cap.setAvailableCapacity(psTotalPhysicalCapacity);
                        }
                        cap.setTotalCapacity(psTotalPhysicalCapacity);
                        cap.setTotalPhysicalCapacity(psTotalPhysicalCapacity);
                        cap.setAvailablePhysicalCapacity(psAvailablePhysicalCapacity);

                        return cap;
                    }
                });
            }
        }.execute();
    }
}
