package org.zstack.kvm.hypervisor;

import org.zstack.core.db.Q;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.HostOperationSystem;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostVO;
import org.zstack.kvm.KVMHostVO_;
import org.zstack.kvm.hypervisor.datatype.HypervisorVersionState;

import javax.persistence.Tuple;
import java.util.*;


/**
 * Created by Wenhao.Zhang on 23/02/21
 */
public class KvmHypervisorInfoHelper {
    /**
     * @return map
     *   key: host uuid
     *   value: expected hypervisor metadata, may be null
     */
    public static Map<String, HostExpectedHypervisorMetadata> collectExpectedHypervisorInfoForHosts(
            Collection<String> hostUuidList, String hypervisor) {
        if (hostUuidList == null || hostUuidList.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Tuple> tuples = Q.New(KVMHostVO.class)
                .select(KVMHostVO_.uuid, KVMHostVO_.architecture,
                        KVMHostVO_.osDistribution, KVMHostVO_.osRelease, KVMHostVO_.osVersion)
                .notNull(HostVO_.architecture)
                .in(KVMHostVO_.uuid, hostUuidList)
                .listTuple();

        KvmHypervisorMetadataStore metadataStore = org.zstack.core.Platform.getComponentLoader()
                .getComponent(KvmHypervisorMetadataStore.class);
        Map<String, HostExpectedHypervisorMetadata> results = new HashMap<>();
        for (Tuple tuple : tuples) {
            String hostUuid = tuple.get(0, String.class);
            String architecture = tuple.get(1, String.class);
            HostOperationSystem os = HostOperationSystem.of(
                    tuple.get(2, String.class),
                    tuple.get(3, String.class),
                    tuple.get(4, String.class));
            if (os == null || os.distribution == null) {
                results.put(hostUuid, null);
                continue;
            }
            String osReleaseVersion = String.format("%s %s", os.distribution, normalizeOsVersion(os.version));
            results.put(hostUuid, metadataStore.find(architecture, osReleaseVersion, hypervisor));
        }

        return results;
    }

    public static boolean isExpectedHypervisorMetadataAvailable() {
        KvmHypervisorMetadataStore metadataStore = org.zstack.core.Platform.getComponentLoader()
                .getComponent(KvmHypervisorMetadataStore.class);
        return metadataStore.isAvailable();
    }

    /**
     * Strip a leading {@code V} or {@code v} from an OS version string when the
     * next character is a digit. Some distributions (notably Kylin Linux Advanced
     * Server) expose {@code VERSION_ID="V10"} via {@code /etc/os-release}, while
     * the matching DVD metadata script outputs the same release as a plain
     * {@code 10}. Without normalization the two sides build different
     * {@code osReleaseVersion} keys (e.g. {@code "kylin V10"} vs {@code "kylin 10"})
     * and the metadata join silently returns no rows, leaving
     * {@code matchTargetVersion} null and the host stuck in {@code Unknown}.
     * See ZSTAC-83682.
     */
    public static String normalizeOsVersion(String version) {
        if (version == null) {
            return null;
        }
        String trimmed = version.trim();
        if (trimmed.length() > 1
                && (trimmed.charAt(0) == 'V' || trimmed.charAt(0) == 'v')
                && Character.isDigit(trimmed.charAt(1))) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    public static HypervisorVersionState isQemuVersionMatched(String v1, String v2) {
        if (v1 == null || v2 == null) {
            return HypervisorVersionState.Unknown;
        }
        return Objects.equals(v1, v2) ? HypervisorVersionState.Matched : HypervisorVersionState.Unmatched;
    }

    public static boolean isQemuBased(String virtualizerInfo) {
        return KVMConstant.VIRTUALIZER_QEMU_KVM.equals(virtualizerInfo) ||
                KVMConstant.VIRTUALIZER_QEMU.equals(virtualizerInfo);
    }
}
