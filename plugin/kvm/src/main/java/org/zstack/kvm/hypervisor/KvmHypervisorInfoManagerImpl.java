package org.zstack.kvm.hypervisor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.Component;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.kvm.hypervisor.datatype.*;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.CollectionUtils;

import javax.persistence.Tuple;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Map.Entry;
import static org.zstack.kvm.KVMAgentCommands.GetVirtualizerInfoRsp;
import static org.zstack.kvm.KVMAgentCommands.VirtualizerInfoTO;
import static org.zstack.kvm.hypervisor.HypervisorMetadataCollector.HypervisorMetadataDefinition;

/**
 * Created by Wenhao.Zhang on 23/02/23
 */
public class KvmHypervisorInfoManagerImpl implements KvmHypervisorInfoManager, Component {
    @Autowired
    private DatabaseFacade db;
    @Autowired
    private HypervisorMetadataCollector collector;

    private static class ResourceHypervisorInfo {
        String uuid;
        String resourceType;
        String virtualizer;
        String version;

        String matchTargetUuid;
        String matchTargetResourceType;
        String matchTargetVersion;

        KvmHypervisorInfoVO vo;

        static ResourceHypervisorInfo fromVmVirtualizerInfo(VirtualizerInfoTO info) {
            return fromVmVirtualizerInfo(info, null);
        }

        static ResourceHypervisorInfo fromVmVirtualizerInfo(VirtualizerInfoTO info, String hostUuid) {
            ResourceHypervisorInfo result = from(info);
            result.resourceType = VmInstanceVO.class.getSimpleName();
            result.matchTargetResourceType = HostVO.class.getSimpleName();
            result.matchTargetUuid = hostUuid;
            return result;
        }

        static ResourceHypervisorInfo fromHostVirtualizerInfo(VirtualizerInfoTO info) {
            ResourceHypervisorInfo result = from(info);
            result.resourceType = HostVO.class.getSimpleName();
            result.matchTargetResourceType = KvmHostHypervisorMetadataVO.class.getSimpleName();
            return result;
        }

        static ResourceHypervisorInfo from(VirtualizerInfoTO info) {
            ResourceHypervisorInfo result = new ResourceHypervisorInfo();
            result.uuid = info.getUuid();
            result.virtualizer = info.getVirtualizer();
            result.version = info.getVersion();
            return result;
        }

        KvmHypervisorInfoVO generate() {
            if (vo == null) {
                vo = new KvmHypervisorInfoVO();
                vo.setUuid(uuid);
            }
            vo.setHypervisor(virtualizer);
            vo.setVersion(version);
            vo.setMatchState(KvmHypervisorInfoHelper.isQemuVersionMatched(version, matchTargetVersion));

            return vo;
        }
    }

    @Override
    public void save(GetVirtualizerInfoRsp rsp) {
        final String hostUuid = rsp.getHostInfo().getUuid();
        final List<ResourceHypervisorInfo> list = rsp.getVmInfoList().stream()
                .map(info -> ResourceHypervisorInfo.fromVmVirtualizerInfo(info, hostUuid))
                .collect(Collectors.toList());
        list.add(ResourceHypervisorInfo.fromHostVirtualizerInfo(rsp.getHostInfo()));
        save(list);
    }

    @Override
    public void saveHostInfo(VirtualizerInfoTO info) {
        save(Collections.singletonList(ResourceHypervisorInfo.fromHostVirtualizerInfo(info)));
    }

    @Override
    public void saveVmInfo(VirtualizerInfoTO info) {
        save(Collections.singletonList(ResourceHypervisorInfo.fromVmVirtualizerInfo(info)));
    }

    @Transactional
    private void save(List<ResourceHypervisorInfo> list) {
        Map<String, ResourceHypervisorInfo> uuidInfoMap = list.stream()
                .collect(Collectors.toMap(info -> info.uuid, Function.identity()));

        collectVmMatchTargetUuid(uuidInfoMap);
        collectVmMatchTargetVersion(uuidInfoMap);
        collectHypervisorInfoVo(uuidInfoMap);

        // Save
        List<ResourceHypervisorInfo> toUpdateList = new ArrayList<>();
        List<ResourceHypervisorInfo> toPersistList = new ArrayList<>();

        uuidInfoMap.forEach((uuid, info) -> {
            List<ResourceHypervisorInfo> targets = (info.vo == null) ? toPersistList : toUpdateList;
            targets.add(info);
        });

        if (!toUpdateList.isEmpty()) {
            db.updateCollection(toUpdateList.stream()
                    .map(ResourceHypervisorInfo::generate)
                    .collect(Collectors.toList()));
        }

        if (!toPersistList.isEmpty()) {
            db.persistCollection(toPersistList.stream()
                    .map(ResourceHypervisorInfo::generate)
                    .collect(Collectors.toList()));
        }
    }

    private void collectVmMatchTargetUuid(Map<String, ResourceHypervisorInfo> uuidInfoMap) {
        List<String> vmUuidListNeedFindHost = uuidInfoMap.values().stream()
                .filter(info -> HostVO.class.getSimpleName().equals(info.matchTargetResourceType))
                .filter(info -> VmInstanceVO.class.getSimpleName().equals(info.resourceType))
                .filter(info -> info.matchTargetUuid == null)
                .map(info -> info.uuid)
                .collect(Collectors.toList());
        if (vmUuidListNeedFindHost.isEmpty()) {
            return;
        }

        final List<Tuple> vmHostTuples = Q.New(VmInstanceVO.class)
                .in(VmInstanceVO_.uuid, vmUuidListNeedFindHost)
                .select(VmInstanceVO_.uuid, VmInstanceVO_.hostUuid)
                .listTuple();
        vmHostTuples.forEach(tuple ->
                uuidInfoMap.get(tuple.get(0, String.class)).matchTargetUuid = tuple.get(1, String.class));
    }

    private void collectVmMatchTargetVersion(Map<String, ResourceHypervisorInfo> uuidInfoMap) {
        Map<String, List<ResourceHypervisorInfo>> targetUuidInfoMap = uuidInfoMap.values().stream()
                .filter(info -> info.matchTargetVersion == null)
                .filter(info -> HostVO.class.getSimpleName().equals(info.matchTargetResourceType))
                .filter(info -> VmInstanceVO.class.getSimpleName().equals(info.resourceType))
                .collect(Collectors.toMap(info -> info.matchTargetUuid, CollectionDSL::list, CollectionDSL::concat));
        if (targetUuidInfoMap.isEmpty()) {
            return;
        }

        // find match info from uuidInfoMap
        for (Iterator<Entry<String, List<ResourceHypervisorInfo>>> it = targetUuidInfoMap.entrySet().iterator(); it.hasNext();) {
            final Entry<String, List<ResourceHypervisorInfo>> next = it.next();
            final ResourceHypervisorInfo matchInfo = uuidInfoMap.get(next.getKey());
            if (matchInfo == null) {
                continue;
            }

            next.getValue().forEach(info -> info.matchTargetVersion = matchInfo.version);
            it.remove();
        }
        if (targetUuidInfoMap.isEmpty()) {
            return;
        }

        // find match info from database
        List<Tuple> tuples = Q.New(KvmHypervisorInfoVO.class)
                .in(KvmHypervisorInfoVO_.uuid, targetUuidInfoMap.keySet())
                .select(KvmHypervisorInfoVO_.uuid, KvmHypervisorInfoVO_.version)
                .listTuple();
        for (Tuple tuple : tuples) {
            String targetUuid = tuple.get(0, String.class);
            targetUuidInfoMap.get(targetUuid).forEach(info -> info.matchTargetVersion = tuple.get(1, String.class));
        }
    }

    private void collectHypervisorInfoVo(Map<String, ResourceHypervisorInfo> uuidInfoMap) {
        List<KvmHypervisorInfoVO> voList = Q.New(KvmHypervisorInfoVO.class)
                .in(KvmHypervisorInfoVO_.uuid, uuidInfoMap.keySet())
                .list();

        for (KvmHypervisorInfoVO vo : voList) {
            final ResourceHypervisorInfo info = uuidInfoMap.get(vo.getUuid());
            if (info != null) {
                info.vo = vo;
            }
        }
    }

    private KvmHypervisorInfoVO updateKvmHypervisorInfoVO(String uuid, KvmHypervisorInfoVO vo, VirtualizerInfoTO to) {
        if (vo == null) {
            vo = new KvmHypervisorInfoVO();
            vo.setUuid(uuid);
        }
        vo.setHypervisor(to.getVirtualizer());
        vo.setVersion(to.getVersion());
        return vo;
    }

    @Override
    public void clean(String uuid) {
        SQL.New(KvmHypervisorInfoVO.class).eq(KvmHypervisorInfoVO_.uuid, uuid).delete();
    }

    @Override
    public void refreshMetadata() {
        List<HypervisorMetadataDefinition> collected = collector.collect();
        saveMetadataList(collected);
    }

    private void saveMetadataList(List<HypervisorMetadataDefinition> definitions) {
        List<HostOsCategoryVO> categoryVOS = definitions.stream()
                .map(this::mapToHostOsCategory)
                .collect(Collectors.toList());
        saveHostOsCategoryList(categoryVOS);
    }

    @Transactional
    protected void saveHostOsCategoryList(List<HostOsCategoryVO> categoryVOS) {
        // refresh all metadata with current management node
        SQL.New(KvmHostHypervisorMetadataVO.class)
                .eq(KvmHostHypervisorMetadataVO_.managementNodeUuid, Platform.getManagementServerId())
                .delete();
        if (CollectionUtils.isEmpty(categoryVOS)) {
            return;
        }

        Set<String> requestArchitectures = categoryVOS.stream()
                .map(HostOsCategoryVO::getArchitecture)
                .collect(Collectors.toSet());
        Set<String> requestOsReleaseVersions = categoryVOS.stream()
                .map(HostOsCategoryVO::getOsReleaseVersion)
                .collect(Collectors.toSet());
        List<HostOsCategoryVO> existsCategories = Q.New(HostOsCategoryVO.class)
                .in(HostOsCategoryVO_.architecture, requestArchitectures)
                .in(HostOsCategoryVO_.osReleaseVersion, requestOsReleaseVersions)
                .list();

        List<HostOsCategoryVO> needPersistCategories = new ArrayList<>();
        List<KvmHostHypervisorMetadataVO> metadataList = new ArrayList<>();

        // fill KvmHostHypervisorMetadataVO.categoryUuid
        // fill HostOsCategoryVO.uuid if it is new
        for (HostOsCategoryVO category : categoryVOS) {
            HostOsCategoryVO realCategory = existsCategories.stream()
                    .filter(c -> c.getArchitecture().equals(category.getArchitecture()))
                    .filter(c -> c.getOsReleaseVersion().equals(category.getOsReleaseVersion()))
                    .findAny().orElse(null);
            
            if (realCategory == null) {
                String newUuid = Platform.getUuid();
                category.setUuid(newUuid);
                category.getMetadataList().forEach(m -> m.setCategoryUuid(newUuid));
                needPersistCategories.add(category);
            } else {
                String uuid = realCategory.getUuid();
                category.getMetadataList().forEach(m -> m.setCategoryUuid(uuid));
            }

            metadataList.addAll(category.getMetadataList());
        }

        if (!needPersistCategories.isEmpty()) {
            db.persistCollection(needPersistCategories);
        }
        if (!metadataList.isEmpty()) {
            db.persistCollection(metadataList);
        }
    }

    /**
     * note:
     *   HostOsCategoryVO.uuid is empty
     *   KvmHostHypervisorMetadataVO.categoryUuid is empty
     */
    private HostOsCategoryVO mapToHostOsCategory(HypervisorMetadataDefinition definition) {
        HostOsCategoryVO vo = new HostOsCategoryVO();

        vo.setArchitecture(definition.getArchitecture());
        vo.setOsReleaseVersion(definition.getOsReleaseVersion());

        KvmHostHypervisorMetadataVO metadata = new KvmHostHypervisorMetadataVO();
        metadata.setUuid(Platform.getUuid());
        metadata.setManagementNodeUuid(Platform.getManagementServerId());
        metadata.setHypervisor(definition.getHypervisor());
        metadata.setVersion(definition.getVersion());
        vo.setMetadataList(Arrays.asList(metadata));

        return vo;
    }

    @Override
    public boolean start() {
        refreshMetadata();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
