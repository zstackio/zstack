package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.*;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostOsVersionAllocatorFlow  extends AbstractHostAllocatorFlow {
    @Override
    public void allocate() {
        throwExceptionIfIAmTheFirstFlow();

        Map<String, HostVO> hostMap = new HashMap<String, HostVO>();
        for (HostVO h : candidates) {
            hostMap.put(h.getUuid(), h);
        }

        List<String> hostUuids = new ArrayList<String>();
        hostUuids.addAll(hostMap.keySet());

        String distro = HostSystemTags.OS_DISTRIBUTION.getTokenByResourceUuid(spec.getVmInstance().getHostUuid(), HostSystemTags.OS_DISTRIBUTION_TOKEN);
        String release = HostSystemTags.OS_RELEASE.getTokenByResourceUuid(spec.getVmInstance().getHostUuid(), HostSystemTags.OS_RELEASE_TOKEN);
        String version = HostSystemTags.OS_VERSION.getTokenByResourceUuid(spec.getVmInstance().getHostUuid(), HostSystemTags.OS_VERSION_TOKEN);
        String currentVersion = String.format("%s;%s;%s", distro, release, version);

        Map<String, List<String>> distroMap = HostSystemTags.OS_DISTRIBUTION.getTags(hostUuids);
        Map<String, List<String>> releaseMap = HostSystemTags.OS_RELEASE.getTags(hostUuids);
        Map<String, List<String>> versionMap = HostSystemTags.OS_VERSION.getTags(hostUuids);

        Map<String, String> candidateVersions = new HashMap<String, String>();
        for (String huuid : hostUuids) {
            List<String> ds = distroMap.get(huuid);
            String d = ds == null ? null : HostSystemTags.OS_DISTRIBUTION.getTokenByTag(ds.get(0), HostSystemTags.OS_DISTRIBUTION_TOKEN);
            List<String> rs = releaseMap.get(huuid);
            String r = rs == null ? null : HostSystemTags.OS_RELEASE.getTokenByTag(rs.get(0), HostSystemTags.OS_RELEASE_TOKEN);
            List<String> vs = versionMap.get(huuid);
            String v = vs == null ? null : HostSystemTags.OS_VERSION.getTokenByTag(vs.get(0), HostSystemTags.OS_VERSION_TOKEN);
            candidateVersions.put(huuid, String.format("%s;%s;%s", d, r, v));
        }

        List<HostVO> ret = new ArrayList<HostVO>();
        for (Entry<String, String> e : candidateVersions.entrySet()) {
            String huuid = e.getKey();
            String ver = e.getValue();
            if (ver.equals(currentVersion)) {
                ret.add(hostMap.get(huuid));
            }
        }

        if (ret.isEmpty()) {
            fail(String.format("no candidate host has version[%s]", currentVersion));
        } else {
            next(ret);
        }
    }
}
