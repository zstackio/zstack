package org.zstack.kvm.hypervisor;

import org.zstack.core.db.Q;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.hypervisor.datatype.HostOsCategoryVO;
import org.zstack.kvm.hypervisor.datatype.HostOsCategoryVO_;
import org.zstack.kvm.hypervisor.datatype.HypervisorVersionState;
import org.zstack.utils.TagUtils;
import org.zstack.utils.data.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.compute.host.HostSystemTags.*;

/**
 * Created by Wenhao.Zhang on 23/02/21
 */
public class KvmHypervisorInfoHelper {
    /**
     * @return map
     *   key: host uuid
     *   value: HostOsCategoryVO
     */
    public static Map<String, HostOsCategoryVO> collectExpectedHypervisorInfoForHosts(
            Collection<String> hostUuidList) {
        Map<String, String> hostArchMap = Q.New(HostVO.class)
                .select(HostVO_.uuid, HostVO_.architecture)
                .in(HostVO_.uuid, hostUuidList)
                .listTuple()
                .stream()
                .collect(Collectors.toMap(tuple -> tuple.get(0, String.class), tuple -> tuple.get(1, String.class)));

        Map<String, String> hostDistributionMap = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.resourceUuid, SystemTagVO_.tag)
                .in(SystemTagVO_.resourceUuid, hostUuidList)
                .like(SystemTagVO_.tag, TagUtils.tagPatternToSqlPattern(OS_DISTRIBUTION.getTagFormat()))
                .listTuple()
                .stream()
                .collect(Collectors.toMap(tuple -> tuple.get(0, String.class), tuple -> tuple.get(1, String.class)));
        Map<String, String> hostOsVersionMap = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.resourceUuid, SystemTagVO_.tag)
                .in(SystemTagVO_.resourceUuid, hostUuidList)
                .like(SystemTagVO_.tag, TagUtils.tagPatternToSqlPattern(OS_VERSION.getTagFormat()))
                .listTuple()
                .stream()
                .collect(Collectors.toMap(tuple -> tuple.get(0, String.class), tuple -> tuple.get(1, String.class)));
        Map<String, String> hostReleaseMap = Q.New(SystemTagVO.class)
                .select(SystemTagVO_.resourceUuid, SystemTagVO_.tag)
                .in(SystemTagVO_.resourceUuid, hostUuidList)
                .like(SystemTagVO_.tag, TagUtils.tagPatternToSqlPattern(OS_RELEASE.getTagFormat()))
                .listTuple()
                .stream()
                .collect(Collectors.toMap(tuple -> tuple.get(0, String.class), tuple -> tuple.get(1, String.class)));

        final Map<Pair<String, String>, HostOsCategoryVO> caches = new HashMap<>();
        final Map<String, HostOsCategoryVO> results = new HashMap<>();
        for (String hostUuid : hostUuidList) {
            String architecture = hostArchMap.get(hostUuid);
            String distributionTag = hostDistributionMap.get(hostUuid);
            String distribution = OS_DISTRIBUTION.getTokenByTag(distributionTag, OS_DISTRIBUTION_TOKEN);
            String osVersionTag = hostOsVersionMap.get(hostUuid);
            String osVersion = OS_VERSION.getTokenByTag(osVersionTag, OS_VERSION_TOKEN);
            String osReleaseTag = hostReleaseMap.get(hostUuid);
            String osRelease = OS_RELEASE.getTokenByTag(osReleaseTag, OS_RELEASE_TOKEN);
            String osReleaseVersion = generateOsReleaseVersion(distribution, osRelease, osVersion);

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

    private static String generateOsReleaseVersion(String distribution, String osRelease, String osVersion) {
        return String.format("%s %s %s", distribution, osRelease, osVersion);
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
