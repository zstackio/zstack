package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.host.HostManager;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.host.*;
import org.zstack.header.vo.ResourceVO;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Filter out hosts that do not match the operating system of the specific host
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostOsVersionAllocatorFlow  extends AbstractHostAllocatorFlow {
    @Autowired
    private HostManager manager;

    @Override
    public void allocate() {
        throwExceptionIfIAmTheFirstFlow();

        Map<String, HostVO> hostMap = new HashMap<>();
        for (HostVO h : candidates) {
            hostMap.put(h.getUuid(), h);
        }

        final String currentHostUuid = spec.getVmInstance().getHostUuid();
        List<HostVO> allHostList = new ArrayList<>(candidates);
        final HostVO currentHost = Q.New(HostVO.class)
                .eq(HostVO_.uuid, currentHostUuid)
                .find();
        allHostList.add(currentHost);
        final Map<String, HostOperationSystem> hostOsMap = generateHostUuidOsMap(allHostList);

        final HostOperationSystem currentHostOs = hostOsMap.get(currentHostUuid);
        allHostList.remove(currentHost);

        List<HostVO> matchedHosts = allHostList.stream()
                .map(HostVO::getUuid)
                .filter(hostUuid -> {
                    final HostOperationSystem hostOs = hostOsMap.get(hostUuid);
                    return hostOs == null ? true : hostOs.equals(currentHostOs);
                })
                .map(hostMap::get)
                .collect(Collectors.toList());

        if (matchedHosts.isEmpty()) {
            fail(Platform.operr("no candidate host has version[%s]", currentHostOs));
        } else {
            next(matchedHosts);
        }
    }

    private Map<String, HostOperationSystem> generateHostUuidOsMap(List<HostVO> hostList) {
        final Map<String, String> hostHypervisorTypeMap = hostList.stream()
                .collect(Collectors.toMap(ResourceVO::getUuid, HostAO::getHypervisorType));
        final Set<String> hypervisorTypeSet = new HashSet<>(hostHypervisorTypeMap.values());

        final Map<String, HostOperationSystem> results = new HashMap<>(hostList.size());
        for (String hypervisorTypeString : hypervisorTypeSet) {
            final HypervisorFactory factory = manager.getHypervisorFactory(
                    HypervisorType.valueOf(hypervisorTypeString));
            final List<String> hostsWithThisHypervisorType = hostHypervisorTypeMap.entrySet().stream()
                    .filter(entry -> hypervisorTypeString.equals(entry.getValue()))
                    .map(Entry::getKey)
                    .collect(Collectors.toList());

            if (factory.supportGetHostOs()) {
                results.putAll(factory.getHostOsMap(hostsWithThisHypervisorType));
            }
        }

        return results;
    }
}
