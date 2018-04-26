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

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.distinctByKey;

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

    private boolean skip = true;

    private static int defaultPriroty = 10;

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


        List<PriorityMap> priMap = new ArrayList<>();
        for (BackupStoragePrimaryStorageExtensionPoint ext : pluginRgty.getExtensionList(BackupStoragePrimaryStorageExtensionPoint.class)) {
            priMap.addAll(formatPriority(ext.getPrimaryStoragePriorityMap(bs, spec.getImage())));
        }

        priMap = priMap.stream()
                .sorted(Comparator.comparingInt(it -> it.priority))
                .filter(distinctByKey(it -> it.PS))
                .collect(Collectors.toList());

        adjustCandidates(priMap);
    }

    private void adjustCandidates(List<PriorityMap> priMap) {
        if (priMap.size() == 0) {
            prepareForNext(candidates);
            return;
        }

        logger.debug(String.format("before PrimaryStoragePrioritySortFlow adjustCandidates: %s", candidates.stream().map(HostInventory::getName).collect(Collectors.toList())));
        String psPriorityCondition = String.join(" ", priMap.stream()
                .map(it -> String.format("when '%s' then %d", it.PS, it.priority))
                .collect(Collectors.toList()));
        logger.debug(String.format("ps priority condition : %s", psPriorityCondition));

        List<Tuple> ts = SQL.New("select min(case pr.type " +
                psPriorityCondition +
                " else :defaultPriority end) as priority, h.uuid" +
                " from PrimaryStorageClusterRefVO ref, HostVO h, ClusterVO c, PrimaryStorageVO pr" +
                " where h.uuid in (:huuids)" +
                " and h.clusterUuid=c.uuid" +
                " and c.uuid=ref.clusterUuid" +
                " and pr.uuid=ref.primaryStorageUuid " +
                " group by h.uuid", Tuple.class)
                .param("huuids", candidates.stream().map(HostInventory::getUuid).collect(Collectors.toList()))
                .param("defaultPriority", defaultPriroty)
                .list();

        Map<String, Integer> hostPriority = new HashMap<>();
        Integer topPriority = ts.stream()
                .peek(it -> hostPriority.put(it.get(1, String.class), it.get(0, Integer.class)))
                .mapToInt(it -> it.get(0, Integer.class))
                .min().orElse(defaultPriroty);

        // sort by priority
        List<HostInventory> sorted = candidates.stream()
                .sorted(Comparator.comparingInt(it -> hostPriority.get(it.getUuid())))
                .collect(Collectors.toList());
        candidates.clear();
        candidates.addAll(sorted);

        // choose hosts on top priority ps
        sorted.removeIf(it -> hostPriority.get(it.getUuid()) > topPriority);
        prepareForNext(sorted);

        logger.debug(String.format("subCandidates: %s",subCandidates.stream().map(HostInventory::getName).collect(Collectors.toList())));
        logger.debug(String.format("after PrimaryStoragePrioritySortFlow adjustCandidates: %s", candidates.stream().map(HostInventory::getName).collect(Collectors.toList())));
    }

    class PriorityMap {
        String PS;
        Integer priority;
    }

    @SuppressWarnings("unchecked")
    // priorityStr format is: [{"PS":"Ceph", "priority":"5"},{"PS":"LocalStorage", "priority":"10"}]
    private List<PriorityMap> formatPriority(final String priorityStr) {
        if (priorityStr != null) {
            return JSONObjectUtil.toCollection(priorityStr, ArrayList.class, PriorityMap.class);
        }
        return new ArrayList<>();
    }

    private void prepareForNext(List<HostInventory> hosts) {
        if (hosts != null && hosts.size() > 0) {
            subCandidates.clear();
            subCandidates.addAll(hosts);
            skip = false;
        }
    }

    @Override
    public boolean skipNext() {
        return skip;
    }
}
