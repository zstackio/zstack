package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.allocator.AbstractHostSortorFlow;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.image.ImageVO;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStoragePrimaryStorageExtensionPoint;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by mingjian.deng on 2017/10/31.
 * the bellow flow adjust the priority of ps if it indicated in xml
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStoragePrioritySortFlow extends AbstractHostSortorFlow {
    private static CLogger logger = Utils.getLogger(PrimaryStoragePrioritySortFlow.class);
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected DatabaseFacade dbf;
    @Override
    public void sort() {
        DebugUtils.Assert(candidates != null && !candidates.isEmpty(), "HostInventory cannot be none");

        if (spec.getImage() == null || dbf.findByUuid(spec.getImage().getUuid(), ImageVO.class) == null) {
            prepareForNext(candidates);
            return;
        }

        List<String> bsUuids = Q.New(ImageBackupStorageRefVO.class).eq(ImageBackupStorageRefVO_.imageUuid, spec.getImage().getUuid()).
                select(ImageBackupStorageRefVO_.backupStorageUuid).listValues();

        DebugUtils.Assert(bsUuids.size() > 0, String.format("imageUuid [%s] not in any BackupStorage", spec.getImage().getUuid()));
        //TODO: we suppose imageUuid is only in 1 bs, if it could be in 2 or more bss, then we should improve the bellow code
        BackupStorageInventory bs = BackupStorageInventory.valueOf(dbf.findByUuid(bsUuids.get(0), BackupStorageVO.class));

        List<BackupStoragePrimaryStorageExtensionPoint> extenstions = pluginRgty.getExtensionList(BackupStoragePrimaryStorageExtensionPoint.class);
        Map<String, Integer> priMap = new HashMap<>();
        extenstions.forEach(ext -> priMap.putAll(formatPriority(ext.getPrimaryStoragePriorityMap(bs))));

        adjustCandidates(priMap);
    }

    // get ps type from hostuuid
    private Map<String, List<HostInventory>> groupByPSType() {
        Map<String, List<HostInventory>> res = new HashMap<>();
        String sql = "select distinct(pr.type) from PrimaryStorageVO pr where pr.uuid in " +
                "(select ref.primaryStorageUuid from PrimaryStorageClusterRefVO ref, HostVO h, ClusterVO c " +
                "where h.clusterUuid=c.uuid and c.uuid=ref.clusterUuid and h.uuid= :huuid)";
        for(HostInventory host: candidates) {
            List<String> types = SQL.New(sql).param("huuid", host.getUuid()).list();
            types.forEach(type -> {
                List<HostInventory> hs = res.get(type);
                if (hs == null) {
                    res.put(type, CollectionDSL.list(host));
                } else {
                    hs.add(host);
                    res.put(type, hs);
                }
            });
        }
        return res;
    }

    private void addAll(final List<HostInventory> list) {
        for (HostInventory host: list) {
            if (candidates.indexOf(host) == -1) {
                candidates.add(host);
            }
        }
    }

    private void adjustCandidates(Map<String, Integer> priMap) {
        if (priMap.size() == 0) {
            prepareForNext(candidates);
            return;
        }
        logger.debug(String.format("before PrimaryStoragePrioritySortFlow adjustCandidates: %s", candidates.stream().map(HostInventory::getName).collect(Collectors.toList())));
        Map<String, List<HostInventory>> hostPsMap = groupByPSType();

        List<List<HostInventory>> sorted = hostPsMap.entrySet().stream().sorted((e1, e2) -> {
            logger.debug(priMap.toString());
            int p1 = priMap.get(e1.getKey().toLowerCase()) == null ? 10 : priMap.get(e1.getKey().toLowerCase());
            int p2 = priMap.get(e2.getKey().toLowerCase()) == null ? 10 : priMap.get(e2.getKey().toLowerCase());
            return p1 - p2;
        }).map(w -> w.getValue()).collect(Collectors.toList());
        for (List<HostInventory> sub: sorted) {
            if (sub.size() > 0) {
                // we choose the first ps type as sub candidates
                prepareForNext(sub);
                logger.debug(String.format("subCandidates: %s",candidates.stream().map(HostInventory::getName).collect(Collectors.toList()).toString()));
                break;
            }
        }

        candidates.clear();
        sorted.forEach(list -> addAll(list));
        logger.debug(String.format("after PrimaryStoragePrioritySortFlow adjustCandidates: %s", candidates.stream().map(HostInventory::getName).collect(Collectors.toList())));
    }

    class PriorityMap {
        String PS;
        Integer priority;
    }

    @SuppressWarnings("unchecked")
    // priorityStr format is: [{"PS":"Ceph", "priority":"5"},{"PS":"LocalStorage", "priority":"10"}]
    private Map<String, Integer> formatPriority(final String priorityStr) {
        Map<String, Integer> priMap = new HashMap<>();
        if (priorityStr != null) {
            List<PriorityMap> maps = JSONObjectUtil.toCollection(priorityStr, ArrayList.class, PriorityMap.class);
            maps.forEach(map -> priMap.put(map.PS.toLowerCase(), map.priority));
        }
        return priMap;
    }

    private void prepareForNext(List<HostInventory> hosts) {
        if (hosts != null && hosts.size() > 0) {
            subCandidates.clear();
            subCandidates.addAll(hosts);
            skip = false;
        }
    }
}
