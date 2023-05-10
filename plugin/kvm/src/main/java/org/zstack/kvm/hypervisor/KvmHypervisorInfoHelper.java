package org.zstack.kvm.hypervisor;

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
                        KVMHostVO_.osDistribution, KVMHostVO_.osVersion)
                .notNull(HostVO_.architecture)
                .in(KVMHostVO_.uuid, hostUuidList)
                .listTuple();
        Map<String, String> hostArchMap = tuples.stream()
                .collect(Collectors.toMap(tuple -> tuple.get(0, String.class), tuple -> tuple.get(1, String.class)));
        Map<String, HostOperationSystem> hostOsMap = tuples.stream()
                .collect(Collectors.toMap(tuple -> tuple.get(0, String.class),
                        tuple -> HostOperationSystem.of(
                                tuple.get(2, String.class), tuple.get(3, String.class))));

        final Map<Pair<String, String>, HostOsCategoryVO> caches = new HashMap<>();
        final Map<String, HostOsCategoryVO> results = new HashMap<>();
        for (String hostUuid : hostUuidList) {
            String architecture = hostArchMap.get(hostUuid);
            if (architecture == null) {
                results.put(hostUuid, null);
                continue;
            }

            HostOperationSystem os = hostOsMap.get(hostUuid);
            String osReleaseVersion = os.toString();

            Pair<String, String> key = new Pair<>(architecture, osReleaseVersion);
            HostOsCategoryVO vo = caches.get(key);
            if (vo != null) {
                results.put(hostUuid, vo);
                continue;
            }

            vo = Q.New(HostOsCategoryVO.class)
                    .eq(HostOsCategoryVO_.architecture, architecture)
                    .eq(HostOsCategoryVO_.osReleaseVersion, osReleaseVersion)
                    .find();
            caches.put(key, vo);
            results.put(hostUuid, vo);
        }

        return results;
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
