package org.zstack.storage.ceph.primary.capacity;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.storage.ceph.CephCapacity;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephPoolCapacity;
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;

import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Created by lining on 2021/1/22.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class OpenSourceCephPrimaryCapacityUpdater implements CephPrimaryCapacityUpdater {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public String getCephManufacturer() {
        return CephConstants.CEPH_MANUFACTURER_OPENSOURCE;
    }

    @Override
    public void update(CephCapacity cephCapacity) {
        String fsid = cephCapacity.getFsid();
        long total = cephCapacity.getTotalCapacity();
        long avail = cephCapacity.getAvailableCapacity();
        List<CephPoolCapacity> poolCapacities = cephCapacity.getPoolCapacities();

        CephPrimaryStorageVO cephPs = SQL.New("select pri from CephPrimaryStorageVO pri where pri.fsid = :fsid",CephPrimaryStorageVO.class)
                .param("fsid", fsid)
                .find();
        if (cephPs == null) {
            return;
        }

        List<String> poolNames = new ArrayList<>();

        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(cephPs.getUuid());
        updater.run(cap -> {
            if (cap.getTotalCapacity() == 0 || cap.getAvailableCapacity() == 0) {
                // init
                cap.setAvailableCapacity(avail);
            }
            cap.setTotalCapacity(total);
            cap.setTotalPhysicalCapacity(total);
            cap.setAvailablePhysicalCapacity(avail);
            if (CollectionUtils.isEmpty(poolCapacities)) {
                return cap;
            }

            Map<String, CephPoolCapacity> osdPoolCapacity = new HashMap<>();
            for (CephPoolCapacity poolCapacitiy : poolCapacities) {
                if (poolCapacitiy.getRelatedOsdCapacity() == null) {
                    continue;
                }
                Map<String, CephPoolCapacity> osdPoolCap = poolCapacitiy.getRelatedOsdCapacity().keySet().stream()
                        .collect(Collectors.toMap(osdName -> osdName, osdName -> {
                            if (!osdPoolCapacity.containsKey(osdName)) {
                                return poolCapacitiy;
                            }
                            // osd multiplexing
                            Float diskUtilization = osdPoolCapacity.get(osdName).getDiskUtilization();
                            return diskUtilization > poolCapacitiy.getDiskUtilization() ? poolCapacitiy : osdPoolCapacity.get(osdName);
                        }));
                osdPoolCapacity.putAll(osdPoolCap);
            }

            long osdsTotalSize = 0;
            long osdsAvailCap = 0;
            for (String osdName : osdPoolCapacity.keySet()) {
                CephPoolCapacity cephPoolCapacity = osdPoolCapacity.get(osdName);
                osdsTotalSize += cephPoolCapacity.getRelatedOsdCapacity().get(osdName).getSize() * cephPoolCapacity.getDiskUtilization();
                osdsAvailCap += cephPoolCapacity.getRelatedOsdCapacity().get(osdName).getAvailableCapacity() * cephPoolCapacity.getDiskUtilization();
            }

            if (osdsTotalSize != 0){
                cap.setTotalCapacity(osdsTotalSize);
                cap.setTotalPhysicalCapacity(osdsTotalSize);
                cap.setAvailablePhysicalCapacity(osdsAvailCap);
            }
            return cap;
        });

        if (poolCapacities == null) {
            return;
        }

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
                        continue;
                    }

                    CephPoolCapacity poolCapacity = poolCapacities.stream()
                            .filter(e -> poolVO.getPoolName().equals(e.getName()))
                            .findAny().get();

                    if (!poolNames.contains(poolVO.getPoolName())) {
                        poolNames.add(poolVO.getPoolName());
                    }

                    poolVO.setAvailableCapacity(poolCapacity.getAvailableCapacity());
                    poolVO.setReplicatedSize(poolCapacity.getReplicatedSize());
                    poolVO.setUsedCapacity(poolCapacity.getUsedCapacity());
                    poolVO.setTotalCapacity(poolCapacity.getTotalCapacity());
                    poolVO.setDiskUtilization(poolCapacity.getDiskUtilization());
                    poolVO.setSecurityPolicy(poolCapacity.getSecurityPolicy());
                    dbf.getEntityManager().merge(poolVO);
                }
            }
        }.execute();
    }
}
