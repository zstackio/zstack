package org.zstack.kvm.hypervisor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.Component;
import org.zstack.kvm.hypervisor.datatype.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Override
    public void save(GetVirtualizerInfoRsp rsp) {
        List<VirtualizerInfoTO> tos = new ArrayList<>(rsp.getVmInfoList());
        tos.add(rsp.getHostInfo());
        save(tos);
    }

    @Override
    public void save(VirtualizerInfoTO info) {
        save(Collections.singletonList(info));
    }

    @Transactional
    private void save(List<VirtualizerInfoTO> list) {
        Map<String, VirtualizerInfoTO> uuidToMap = list.stream()
                .collect(Collectors.toMap(VirtualizerInfoTO::getUuid, Function.identity()));

        List<KvmHypervisorInfoVO> voList = Q.New(KvmHypervisorInfoVO.class)
                .in(KvmHypervisorInfoVO_.uuid, new ArrayList<>(uuidToMap.keySet()))
                .list();
        Map<String, KvmHypervisorInfoVO> uuidVoMapToUpdate = voList.stream()
                .collect(Collectors.toMap(KvmHypervisorInfoVO::getUuid, Function.identity()));
        if (!uuidVoMapToUpdate.isEmpty()) {
            uuidVoMapToUpdate.forEach((uuid, vo) -> updateKvmHypervisorInfoVO(uuid, vo, uuidToMap.get(uuid)));
            db.updateCollection(voList);
        }

        List<KvmHypervisorInfoVO> voListToPersist = uuidToMap.entrySet().stream()
                .filter(entry -> !uuidVoMapToUpdate.containsKey(entry.getKey()))
                .map(entry -> updateKvmHypervisorInfoVO(entry.getKey(), null, uuidToMap.get(entry.getKey())))
                .collect(Collectors.toList());
        if (!voListToPersist.isEmpty()) {
            db.persistCollection(voListToPersist);
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
