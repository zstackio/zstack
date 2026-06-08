package org.zstack.kvm.hypervisor;

import org.zstack.core.Platform;
import org.zstack.kvm.hypervisor.datatype.HostOsCategoryVO;
import org.zstack.kvm.hypervisor.datatype.KvmHostHypervisorMetadataVO;
import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.zstack.kvm.hypervisor.HypervisorMetadataCollector.HypervisorMetadataDefinition;

public class KvmHypervisorMetadataStoreImpl implements KvmHypervisorMetadataStore {
    private final AtomicReference<MetadataSnapshot> snapshot = new AtomicReference<>(MetadataSnapshot.empty());

    @Override
    public boolean refresh(List<HypervisorMetadataDefinition> definitions) {
        if (CollectionUtils.isEmpty(definitions)) {
            return false;
        }

        MetadataSnapshot next = MetadataSnapshot.from(definitions);
        if (!next.isAvailable()) {
            return false;
        }

        snapshot.set(next);
        return true;
    }

    @Override
    public HostExpectedHypervisorMetadata find(String architecture, String osReleaseVersion, String hypervisor) {
        return snapshot.get().find(architecture, osReleaseVersion, hypervisor);
    }

    @Override
    public List<HostOsCategoryVO> listForCompatibility() {
        return snapshot.get().categories();
    }

    @Override
    public boolean isAvailable() {
        return snapshot.get().isAvailable();
    }

    private static class MetadataSnapshot {
        private final Map<String, HostExpectedHypervisorMetadata> expectedByKey;
        private final List<HostOsCategoryVO> categories;

        private MetadataSnapshot(Map<String, HostExpectedHypervisorMetadata> expectedByKey,
                                 List<HostOsCategoryVO> categories) {
            this.expectedByKey = expectedByKey;
            this.categories = categories;
        }

        static MetadataSnapshot empty() {
            return new MetadataSnapshot(new HashMap<>(), new ArrayList<>());
        }

        static MetadataSnapshot from(List<HypervisorMetadataDefinition> definitions) {
            Map<String, HostExpectedHypervisorMetadata> expectedByKey = new HashMap<>();
            Map<String, HostOsCategoryVO> categoriesByKey = new HashMap<>();

            for (HypervisorMetadataDefinition definition : definitions) {
                String categoryKey = categoryKey(definition.getArchitecture(), definition.getOsReleaseVersion());
                HostOsCategoryVO category = categoriesByKey.computeIfAbsent(categoryKey, key -> {
                    HostOsCategoryVO vo = new HostOsCategoryVO();
                    vo.setUuid(Platform.getUuid());
                    vo.setArchitecture(definition.getArchitecture());
                    vo.setOsReleaseVersion(definition.getOsReleaseVersion());
                    vo.setMetadataList(new ArrayList<>());
                    return vo;
                });

                KvmHostHypervisorMetadataVO metadata = new KvmHostHypervisorMetadataVO();
                metadata.setUuid(Platform.getUuid());
                metadata.setCategoryUuid(category.getUuid());
                metadata.setManagementNodeUuid(Platform.getManagementServerId());
                metadata.setHypervisor(definition.getHypervisor());
                metadata.setVersion(definition.getVersion());
                category.getMetadataList().add(metadata);

                HostExpectedHypervisorMetadata expected = new HostExpectedHypervisorMetadata();
                expected.setUuid(metadata.getUuid());
                expected.setArchitecture(definition.getArchitecture());
                expected.setOsReleaseVersion(definition.getOsReleaseVersion());
                expected.setHypervisor(definition.getHypervisor());
                expected.setVersion(definition.getVersion());
                expectedByKey.put(expectedKey(definition.getArchitecture(), definition.getOsReleaseVersion(), definition.getHypervisor()), expected);
            }

            List<HostOsCategoryVO> categories = categoriesByKey.values().stream()
                    .sorted(Comparator.comparing(HostOsCategoryVO::getArchitecture)
                            .thenComparing(HostOsCategoryVO::getOsReleaseVersion))
                    .collect(Collectors.toList());
            categories.forEach(c -> c.getMetadataList().sort(Comparator.comparing(KvmHostHypervisorMetadataVO::getHypervisor)));

            return new MetadataSnapshot(expectedByKey, categories);
        }

        HostExpectedHypervisorMetadata find(String architecture, String osReleaseVersion, String hypervisor) {
            return expectedByKey.get(expectedKey(architecture, osReleaseVersion, hypervisor));
        }

        List<HostOsCategoryVO> categories() {
            return categories.stream()
                    .map(MetadataSnapshot::copy)
                    .collect(Collectors.toList());
        }

        boolean isAvailable() {
            return !expectedByKey.isEmpty();
        }

        private static String categoryKey(String architecture, String osReleaseVersion) {
            return String.format("%s|%s", architecture, osReleaseVersion);
        }

        private static String expectedKey(String architecture, String osReleaseVersion, String hypervisor) {
            return String.format("%s|%s|%s", architecture, osReleaseVersion, hypervisor);
        }

        private static HostOsCategoryVO copy(HostOsCategoryVO source) {
            HostOsCategoryVO category = new HostOsCategoryVO();
            category.setUuid(source.getUuid());
            category.setArchitecture(source.getArchitecture());
            category.setOsReleaseVersion(source.getOsReleaseVersion());
            category.setMetadataList(source.getMetadataList().stream()
                    .map(MetadataSnapshot::copy)
                    .collect(Collectors.toList()));
            return category;
        }

        private static KvmHostHypervisorMetadataVO copy(KvmHostHypervisorMetadataVO source) {
            KvmHostHypervisorMetadataVO metadata = new KvmHostHypervisorMetadataVO();
            metadata.setUuid(source.getUuid());
            metadata.setCategoryUuid(source.getCategoryUuid());
            metadata.setManagementNodeUuid(source.getManagementNodeUuid());
            metadata.setHypervisor(source.getHypervisor());
            metadata.setVersion(source.getVersion());
            return metadata;
        }
    }
}
