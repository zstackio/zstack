package org.zstack.kvm.hypervisor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.Component;
import org.zstack.header.host.GetVirtualizerInfoMsg;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmCanonicalEvents;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.hypervisor.datatype.*;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

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
    private static final CLogger logger = Utils.getLogger(KvmHypervisorInfoManagerImpl.class);

    @Autowired
    private DatabaseFacade db;
    @Autowired
    private HypervisorMetadataCollector collector;
    @Autowired
    private EventFacade events;
    @Autowired
    private CloudBus bus;
    @Autowired
    private KvmHypervisorMetadataStore metadataStore;

    @Override
    public void save(GetVirtualizerInfoRsp rsp) {
        final String hostUuid = rsp.getHostInfo().getUuid();
        final List<ResourceHypervisorInfo> list = rsp.getVmInfoList().stream()
                .map(info -> ResourceHypervisorInfo.fromVmVirtualizerInfo(info, hostUuid))
                .collect(Collectors.toList());
        list.add(ResourceHypervisorInfo.fromHostVirtualizerInfo(rsp.getHostInfo()));
        save(list);

        logger.debug(String.format("save GetVirtualizerInfoRsp for host[uuid:%s] successfully",
                rsp.getHostInfo().getUuid()));
    }

    @Override
    public void saveHostInfo(VirtualizerInfoTO info) {
        save(Collections.singletonList(ResourceHypervisorInfo.fromHostVirtualizerInfo(info)));
        logger.debug(String.format("save VirtualizerInfoTO for host[uuid:%s] successfully", info.getUuid()));
    }

    @Override
    public void saveVmInfo(VirtualizerInfoTO info) {
        save(Collections.singletonList(ResourceHypervisorInfo.fromVmVirtualizerInfo(info)));
        logger.debug(String.format("save VirtualizerInfoTO for vm[uuid:%s] successfully", info.getUuid()));
    }

    @Transactional
    private void save(List<ResourceHypervisorInfo> list) {
        Map<String, ResourceHypervisorInfo> uuidInfoMap = list.stream()
                .collect(Collectors.toMap(info -> info.uuid, Function.identity()));

        collectVmMatchTargetUuid(uuidInfoMap);
        collectVmMatchTargetVersion(uuidInfoMap);
        collectHostMatchTargetInfo(uuidInfoMap);
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

    private void collectHostMatchTargetInfo(Map<String, ResourceHypervisorInfo> uuidInfoMap) {
        Set<String> hostUuidSet = uuidInfoMap.values().stream()
                .filter(info -> info.matchTargetVersion == null)
                .filter(info -> HostVO.class.getSimpleName().equals(info.resourceType))
                .filter(info -> HostExpectedHypervisorMetadata.class.getSimpleName().equals(info.matchTargetResourceType))
                .map(info -> info.uuid)
                .collect(Collectors.toSet());
        if (hostUuidSet.isEmpty()) {
            return;
        }

        Map<String, List<ResourceHypervisorInfo>> hostInfosByHypervisor = uuidInfoMap.values().stream()
                .filter(info -> hostUuidSet.contains(info.uuid))
                .collect(Collectors.groupingBy(info -> info.virtualizer));
        hostInfosByHypervisor.forEach((hypervisor, infos) -> {
            final Map<String, HostExpectedHypervisorMetadata> uuidExpectedMap =
                    KvmHypervisorInfoHelper.collectExpectedHypervisorInfoForHosts(
                            infos.stream().map(info -> info.uuid).collect(Collectors.toSet()), hypervisor);

            uuidExpectedMap.forEach((uuid, metadata) -> {
                if (metadata == null) {
                    return;
                }

                ResourceHypervisorInfo info = uuidInfoMap.get(uuid);
                info.matchTargetUuid = metadata.getUuid();
                info.matchTargetVersion = metadata.getVersion();
            });
        });
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

    @Override
    public void clean(String uuid) {
        SQL.New(KvmHypervisorInfoVO.class).eq(KvmHypervisorInfoVO_.uuid, uuid).delete();
    }

    @Override
    public void refreshMetadata() {
        List<HypervisorMetadataDefinition> collected = collector.collect();
        boolean metadataUpdated = metadataStore.refresh(collected);
        if (!metadataUpdated) {
            logger.warn("no hypervisor metadata collected from DVD, skip refresh to preserve existing metadata");
            return;
        }

        saveMetadataList(metadataStore.listForCompatibility());
        refreshHostMatchState();
    }

    private void registerRefreshVmHypervisorHooks() {
        events.on(VmCanonicalEvents.VM_LIBVIRT_REPORT_REBOOT, new EventCallback<Object>() {
            @Override
            protected void run(Map<String, String> tokens, Object data) {
                onVmStart(data.toString());
            }
        });
        events.on(VmCanonicalEvents.VM_LIBVIRT_REPORT_START, new EventCallback<Object>() {
            @Override
            protected void run(Map<String, String> tokens, Object data) {
                onVmStart(data.toString());
            }
        });
    }

    private void onVmStart(String vmUuid) {
        final String hostUuid = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .eq(VmInstanceVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE)
                .select(VmInstanceVO_.hostUuid)
                .findValue();
        if (hostUuid == null) {
            // This is not a KVM VM (maybe bare-metal)
            return;
        }

        final GetVirtualizerInfoMsg message = new GetVirtualizerInfoMsg();
        message.setHostUuid(hostUuid);
        message.setVmInstanceUuids(Collections.singletonList(vmUuid));
        bus.makeTargetServiceIdByResourceUuid(message, HostConstant.SERVICE_ID, hostUuid);
        bus.send(message); // no need to reply
    }

    private boolean saveMetadataList(List<HostOsCategoryVO> categoryVOS) {
        return saveHostOsCategoryList(categoryVOS);
    }

    @Transactional
    protected boolean saveHostOsCategoryList(List<HostOsCategoryVO> categoryVOS) {
        if (CollectionUtils.isEmpty(categoryVOS)) {
            logger.warn("no hypervisor metadata collected from DVD, skip refresh to preserve existing metadata");
            return false;
        }

        // refresh all compatibility metadata with current management node
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

        for (HostOsCategoryVO category : categoryVOS) {
            HostOsCategoryVO realCategory = existsCategories.stream()
                    .filter(c -> c.getArchitecture().equals(category.getArchitecture()))
                    .filter(c -> c.getOsReleaseVersion().equals(category.getOsReleaseVersion()))
                    .findAny().orElse(null);

            if (realCategory == null) {
                needPersistCategories.add(category);
            } else {
                category.getMetadataList().forEach(m -> m.setCategoryUuid(realCategory.getUuid()));
            }

            metadataList.addAll(category.getMetadataList());
        }

        boolean anyRecordUpdated = false;
        if (!needPersistCategories.isEmpty()) {
            db.persistCollection(needPersistCategories);
            anyRecordUpdated = true;
        }
        if (!metadataList.isEmpty()) {
            db.persistCollection(metadataList);
            anyRecordUpdated = true;
        }
        return anyRecordUpdated;
    }

    private void refreshHostMatchState() {
        Function<String, String> sqlGenerator = fragment -> String.format(
                "select " +
                    "%s " +
                "from " +
                    "KvmHypervisorInfoVO hyper," +
                    "ResourceVO resource " +
                "where " +
                    "hyper.uuid = resource.uuid " +
                    "and resource.resourceType = :resourceType", fragment);

        Long count = SQL.New(sqlGenerator.apply("count(*)"), Long.class)
                .param("resourceType", HostVO.class.getSimpleName())
                .find();

        Map<String, HypervisorVersionState> originStateMap = new HashMap<>();
        Map<String, HypervisorVersionState> stateMap = new HashMap<>();
        SQL.New(sqlGenerator.apply("hyper.uuid, hyper.matchState "), Tuple.class)
                .param("resourceType", HostVO.class.getSimpleName())
                .limit(100)
                .paginate(count, (List<Tuple> tuples) -> {
                        final Map<String, HypervisorVersionState> map = tuples.stream().collect(Collectors.toMap(
                                tuple -> tuple.get(0, String.class),
                                tuple -> tuple.get(1, HypervisorVersionState.class)
                        ));
                        originStateMap.putAll(map);
                        stateMap.putAll(refreshHostMatchStateFragment(map.keySet()));
                });

        // key: state, value: [KvmHypervisorInfoVO.uuid]
        Map<HypervisorVersionState, List<String>> updatedMap = new HashMap<>();
        originStateMap.forEach((uuid, originState) -> {
            final HypervisorVersionState state = stateMap.get(uuid);
            if (originState == state) {
                return;
            }

            updatedMap.compute(state, (v, list) -> {
                if (list == null) {
                    return new ArrayList<>(Arrays.asList(uuid));
                }
                list.add(uuid);
                return list;
            });
        });

        updatedMap.forEach((state, uuidList) ->
            SQL.New(KvmHypervisorInfoVO.class)
                    .in(KvmHypervisorInfoVO_.uuid, uuidList)
                    .set(KvmHypervisorInfoVO_.matchState, state)
                    .update()
        );
    }

    private Map<String, HypervisorVersionState> refreshHostMatchStateFragment(Set<String> hyperUuidSet) {
        return ResourceHypervisorInfo.from(hyperUuidSet).stream()
                .collect(Collectors.toMap(
                        hyper -> hyper.uuid,
                        hyper -> KvmHypervisorInfoHelper.isQemuVersionMatched(hyper.version, hyper.matchTargetVersion)
                ));
    }

    @Override
    public boolean start() {
        refreshMetadata();
        registerRefreshVmHypervisorHooks();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
