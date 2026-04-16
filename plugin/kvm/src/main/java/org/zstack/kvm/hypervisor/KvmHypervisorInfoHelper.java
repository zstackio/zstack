package org.zstack.kvm.hypervisor;

import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.HostOperationSystem;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostVO;
import org.zstack.kvm.KVMHostVO_;
import org.zstack.kvm.hypervisor.datatype.HostOsCategoryVO;
import org.zstack.kvm.hypervisor.datatype.HostOsCategoryVO_;
import org.zstack.kvm.hypervisor.datatype.HypervisorVersionState;
import org.zstack.utils.data.Pair;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Created by Wenhao.Zhang on 23/02/21
 */
public class KvmHypervisorInfoHelper {
    /**
     * @return map
     *   key: host uuid
     *   value: HostOsCategoryVO, may be null
     */
    public static Map<String, HostOsCategoryVO> collectExpectedHypervisorInfoForHosts(
            Collection<String> hostUuidList) {
        List<Tuple> tuples = Q.New(KVMHostVO.class)
                .select(KVMHostVO_.uuid, KVMHostVO_.architecture,
                        KVMHostVO_.osDistribution, KVMHostVO_.osRelease, KVMHostVO_.osVersion)
                .notNull(HostVO_.architecture)
                .in(KVMHostVO_.uuid, hostUuidList)
                .listTuple();
        Map<String, String> hostArchMap = tuples.stream()
                .collect(Collectors.toMap(tuple -> tuple.get(0, String.class), tuple -> tuple.get(1, String.class)));
        Map<String, HostOperationSystem> hostOsMap = tuples.stream()
                .collect(Collectors.toMap(tuple -> tuple.get(0, String.class),
                        tuple -> HostOperationSystem.of(
                                tuple.get(2, String.class), tuple.get(3, String.class), tuple.get(4, String.class))));

        final Map<Pair<String, String>, HostOsCategoryVO> caches = new HashMap<>();
        final Map<String, HostOsCategoryVO> results = new HashMap<>();
        DatabaseFacade dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);
        for (String hostUuid : hostUuidList) {
            String architecture = hostArchMap.get(hostUuid);
            if (architecture == null) {
                results.put(hostUuid, null);
                continue;
            }

            HostOperationSystem os = hostOsMap.get(hostUuid);
            String osReleaseVersion = String.format("%s %s", os.distribution, normalizeOsVersion(os.version));

            Pair<String, String> key = new Pair<>(architecture, osReleaseVersion);
            HostOsCategoryVO vo = caches.get(key);
            if (vo != null) {
                results.put(hostUuid, vo);
                continue;
            }

            String sql = " select h from HostOsCategoryVO h, KvmHostHypervisorMetadataVO k where k.categoryUuid = h.uuid " +
                    "and k.managementNodeUuid = :mnId " +
                    "and h.architecture = :arch " +
                    "and h.osReleaseVersion = :os " +
                    "order by k.createDate desc";

            List<HostOsCategoryVO> resultList = dbf.getEntityManager().createQuery(sql, HostOsCategoryVO.class)
                    .setParameter("arch", architecture)
                    .setParameter("os", osReleaseVersion)
                    .setParameter("mnId", Platform.getManagementServerId())
                    .setMaxResults(1)
                    .getResultList();
            if (!resultList.isEmpty()) {
                vo = resultList.get(0);
            }

            caches.put(key, vo);
            results.put(hostUuid, vo);
        }

        return results;
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
