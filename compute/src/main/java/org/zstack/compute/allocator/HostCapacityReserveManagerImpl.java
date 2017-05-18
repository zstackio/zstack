package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.cluster.ClusterSystemTags;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.compute.zone.ZoneSystemTags;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.Component;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.allocator.HostReservedCapacityExtensionPoint;
import org.zstack.header.allocator.ReservedHostCapacity;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.Map.Entry;

/**
 */
public class HostCapacityReserveManagerImpl implements HostCapacityReserveManager, Component {
    private static final CLogger logger = Utils.getLogger(HostCapacityReserveManagerImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private HostCapacityOverProvisioningManager ratioMgr;

    private Map<String, HostReservedCapacityExtensionPoint> exts = new HashMap<>();

    private void populateExtensions() {
        for (HostReservedCapacityExtensionPoint extp : pluginRgty.getExtensionList(HostReservedCapacityExtensionPoint.class)) {
            HostReservedCapacityExtensionPoint ext = exts.get(extp.getHypervisorTypeForHostReserveCapacityExtension());
            if (ext != null) {
                throw new CloudRuntimeException(String.format("duplicate HostReserveCapacityExtensionPoint[%s, %s] for hypervisor type[%s]",
                        extp.getClass().getName(), ext.getClass().getName(), extp.getHypervisorTypeForHostReserveCapacityExtension()));
            }

            exts.put(extp.getHypervisorTypeForHostReserveCapacityExtension(), extp);
        }
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private class ReservedCapacityFinder {
        List<String> hostUuids;
        Map<String, ReservedHostCapacity> result = new HashMap<>();

        private void findReservedCapacityByHostTag() {
            if (!HostAllocatorGlobalConfig.HOST_LEVEL_RESERVE_CAPACITY.value(Boolean.class)) {
                return;
            }

            Map<String, List<String>> cpuTags = HostSystemTags.RESERVED_CPU_CAPACITY.getTags(hostUuids);
            for (Entry<String, List<String>> e : cpuTags.entrySet()) {
                ReservedHostCapacity hc = result.get(e.getKey());
                String capacityString = HostSystemTags.RESERVED_CPU_CAPACITY.getTokenByTag(e.getValue().get(0), "capacity");
                hc.setReservedCpuCapacity(SizeUtils.sizeStringToBytes(capacityString));
            }

            Map<String, List<String>> memTags = HostSystemTags.RESERVED_MEMORY_CAPACITY.getTags(hostUuids);
            for (Entry<String, List<String>> e : memTags.entrySet()) {
                ReservedHostCapacity hc = result.get(e.getKey());
                String capacityString = HostSystemTags.RESERVED_MEMORY_CAPACITY.getTokenByTag(e.getValue().get(0), "capacity");
                hc.setReservedMemoryCapacity(SizeUtils.sizeStringToBytes(capacityString));
            }
        }

        private void findReservedCapacityByClusterTag() {
            if (!HostAllocatorGlobalConfig.CLUSTER_LEVEL_RESERVE_CAPACITY.value(Boolean.class)) {
                return;
            }

            SimpleQuery<HostVO> clusterq = dbf.createQuery(HostVO.class);
            clusterq.select(HostVO_.uuid, HostVO_.clusterUuid);
            clusterq.add(HostVO_.uuid, Op.IN, hostUuids);
            clusterq.add(HostVO_.state,Op.EQ, HostState.Enabled);
            clusterq.add(HostVO_.status,Op.EQ, HostStatus.Connected);
            List<Tuple> clusterTuple = clusterq.listTuple();

            if (clusterTuple.isEmpty()) {
                return;
            }

            Map<String, List<String>> clusterHostUuidMap = new HashMap<>(clusterTuple.size());
            List<String> clusterUuids = new ArrayList<>(clusterTuple.size());
            for (Tuple t : clusterTuple) {
                String huuid = t.get(0, String.class);
                String cuuid = t.get(1, String.class);
                List<String> huuids = clusterHostUuidMap.get(cuuid);
                if (huuids == null) {
                    huuids = new ArrayList<>();
                    clusterHostUuidMap.put(cuuid, huuids);
                }
                huuids.add(huuid);
                clusterUuids.add(cuuid);
            }

            Map<String, List<String>> cpuTags = ClusterSystemTags.HOST_RESERVED_CPU_CAPACITY.getTags(clusterUuids);
            for (Entry<String, List<String>> e : cpuTags.entrySet()) {
                List<String> huuids = clusterHostUuidMap.get(e.getKey());
                for (String huuid : huuids) {
                    ReservedHostCapacity hc = result.get(huuid);
                    if (hc.getReservedCpuCapacity() != -1) {
                        continue;
                    }

                    String capacityString = ClusterSystemTags.HOST_RESERVED_CPU_CAPACITY.getTokenByTag(e.getValue().get(0), "capacity");
                    hc.setReservedCpuCapacity(SizeUtils.sizeStringToBytes(capacityString));
                }
            }

            Map<String, List<String>> memTags = ClusterSystemTags.HOST_RESERVED_MEMORY_CAPACITY.getTags(clusterUuids);
            for (Entry<String, List<String>> e : memTags.entrySet()) {
                List<String> huuids = clusterHostUuidMap.get(e.getKey());
                for (String huuid : huuids) {
                    ReservedHostCapacity hc = result.get(huuid);
                    if (hc.getReservedMemoryCapacity() != -1) {
                        continue;
                    }

                    String capacityString = ClusterSystemTags.HOST_RESERVED_MEMORY_CAPACITY.getTokenByTag(e.getValue().get(0), "capacity");
                    hc.setReservedMemoryCapacity(SizeUtils.sizeStringToBytes(capacityString));
                }
            }
        }

        private void findReservedCapacityByZoneTag() {
            if (!HostAllocatorGlobalConfig.ZONE_LEVEL_RESERVE_CAPACITY.value(Boolean.class)) {
                return;
            }

            SimpleQuery<HostVO> zoneq = dbf.createQuery(HostVO.class);
            zoneq.select(HostVO_.uuid, HostVO_.zoneUuid);
            zoneq.add(HostVO_.uuid, Op.IN, hostUuids);
            zoneq.add(HostVO_.state,Op.EQ, HostState.Enabled);
            zoneq.add(HostVO_.status,Op.EQ, HostStatus.Connected);
            List<Tuple> zoneTuples = zoneq.listTuple();

            if (zoneTuples.isEmpty()) {
                return;
            }

            List<String> zoneUuids = new ArrayList<>();
            Map<String, List<String>> zoneHostUuidMap = new HashMap<>();

            for (Tuple t : zoneTuples) {
                String huuid = t.get(0, String.class);
                String zuuid = t.get(1, String.class);
                List<String> huuids = zoneHostUuidMap.get(zuuid);
                if (huuids == null) {
                    huuids = new ArrayList<>();
                    zoneHostUuidMap.put(zuuid, huuids);
                }

                huuids.add(huuid);
                zoneUuids.add(zuuid);
            }

            Map<String, List<String>> ctags = ZoneSystemTags.HOST_RESERVED_CPU_CAPACITY.getTags(zoneUuids);
            for (Entry<String, List<String>> e : ctags.entrySet()) {
                List<String> huuids = zoneHostUuidMap.get(e.getKey());
                for (String huuid : huuids) {
                    ReservedHostCapacity hc = result.get(huuid);
                    if (hc.getReservedCpuCapacity() != -1) {
                        continue;
                    }

                    String capacityString = ZoneSystemTags.HOST_RESERVED_CPU_CAPACITY.getTokenByTag(e.getValue().get(0), "capacity");
                    hc.setReservedCpuCapacity(SizeUtils.sizeStringToBytes(capacityString));
                }
            }

            Map<String, List<String>> memTags = ZoneSystemTags.HOST_RESERVED_MEMORY_CAPACITY.getTags(zoneUuids);
            for (Entry<String, List<String>> e : memTags.entrySet()) {
                List<String> huuids = zoneHostUuidMap.get(e.getKey());
                for (String huuid : huuids) {
                    ReservedHostCapacity hc = result.get(huuid);
                    if (hc.getReservedMemoryCapacity() != -1) {
                        continue;
                    }

                    String capacityString = ZoneSystemTags.HOST_RESERVED_MEMORY_CAPACITY.getTokenByTag(e.getValue().get(0), "capacity");
                    hc.setReservedMemoryCapacity(SizeUtils.sizeStringToBytes(capacityString));
                }
            }
        }

        private void findReservedCapacityByHypervisorType() {
            SimpleQuery<HostVO> hq = dbf.createQuery(HostVO.class);
            hq.select(HostVO_.uuid, HostVO_.hypervisorType);
            hq.add(HostVO_.uuid, Op.IN, hostUuids);
            hq.add(HostVO_.state,Op.EQ, HostState.Enabled);
            hq.add(HostVO_.status,Op.EQ, HostStatus.Connected);
            List<Tuple> tuples = hq.listTuple();

            for (Tuple t : tuples) {
                String huuid = t.get(0, String.class);
                String hvType = t.get(1, String.class);

                HostReservedCapacityExtensionPoint ext = exts.get(hvType);
                if (ext == null) {
                    continue;
                }

                ReservedHostCapacity hc = result.get(huuid);
                if (hc.getReservedMemoryCapacity() == -1) {
                    hc.setReservedMemoryCapacity(ext.getReservedHostCapacity().getReservedMemoryCapacity());
                }
                if (hc.getReservedCpuCapacity() == -1) {
                    hc.setReservedCpuCapacity(ext.getReservedHostCapacity().getReservedCpuCapacity());
                }
            }
        }

        private void squeeze() {
            result.entrySet()
                    .stream()
                    .filter(e -> e.getValue().getReservedCpuCapacity() != -1 && e.getValue().getReservedMemoryCapacity() != -1)
                    .forEach(e -> hostUuids.remove(e.getKey()));
        }

        private void done() {
            for (ReservedHostCapacity hc : result.values()) {
                if (hc.getReservedCpuCapacity() == -1) {
                    hc.setReservedCpuCapacity(0);
                }
                if (hc.getReservedMemoryCapacity() == -1) {
                    hc.setReservedMemoryCapacity(0);
                }
            }
        }

        Map<String, ReservedHostCapacity> find() {
            if (hostUuids.isEmpty()) {
                return result;
            }

            for (String huuid : hostUuids) {
                ReservedHostCapacity hc = new ReservedHostCapacity();
                hc.setReservedCpuCapacity(-1);
                hc.setReservedMemoryCapacity(-1);
                result.put(huuid, hc);
            }

            findReservedCapacityByHostTag();
            squeeze();
            if (hostUuids.isEmpty()) {
                return result;
            }

            findReservedCapacityByClusterTag();
            squeeze();
            if (hostUuids.isEmpty()) {
                return result;
            }

            findReservedCapacityByZoneTag();
            squeeze();
            if (hostUuids.isEmpty()) {
                return result;
            }

            findReservedCapacityByHypervisorType();
            done();
            return result;
        }
    }

    @Override
    public List<HostVO> filterOutHostsByReservedCapacity(List<HostVO> candidates, long requiredCpu, long requiredMemory) {
        ReservedCapacityFinder finder = new ReservedCapacityFinder();
        finder.hostUuids = CollectionUtils.transformToList(candidates, new Function<String, HostVO>() {
            @Override
            public String call(HostVO arg) {
                return arg.getUuid();
            }
        });

        Map<String, ReservedHostCapacity> reserves = finder.find();
        List<HostVO> ret = new ArrayList<>(candidates.size());
        for (HostVO hvo : candidates) {
            ReservedHostCapacity hc = reserves.get(hvo.getUuid());
            if (hvo.getCapacity().getAvailableMemory() - hc.getReservedMemoryCapacity() >= ratioMgr.calculateMemoryByRatio(hvo.getUuid(), requiredMemory)) {
                ret.add(hvo);
            } else {
                if (logger.isTraceEnabled()) {
                    if (hvo.getCapacity().getAvailableMemory() - hc.getReservedMemoryCapacity() < requiredMemory) {
                        logger.trace(String.format("remove host[uuid:%s] from candidates;because after subtracting reserved memory[%s bytes]," +
                                        " it cannot provide required memory[%s bytes]",
                                hvo.getUuid(), hc.getReservedMemoryCapacity(), requiredMemory));
                    }

                    if (hvo.getCapacity().getAvailableCpu() - hc.getReservedCpuCapacity() < requiredCpu) {
                        logger.trace(String.format("remove host[uuid:%s] from candidates;because after subtracting reserved cpu[%s]," +
                                        " it cannot provide required cpu[%s]",
                                hvo.getUuid(), hc.getReservedCpuCapacity(), requiredCpu));
                    }
                }
            }

        }

        return ret;
    }

    @Override
    public ReservedHostCapacity getReservedHostCapacityByZones(List<String> zoneUuids) {
        ReservedHostCapacity ret = new ReservedHostCapacity();
        ret.setReservedCpuCapacity(0);
        ret.setReservedMemoryCapacity(0);

        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.zoneUuid, Op.IN, zoneUuids);
        List<String> huuids = q.listValue();
        if (huuids.isEmpty()) {
            return ret;
        }

        ReservedCapacityFinder finder = new ReservedCapacityFinder();
        finder.hostUuids = huuids;
        Collection<ReservedHostCapacity> col = finder.find().values();
        for (ReservedHostCapacity rc : col) {
            ret.setReservedMemoryCapacity(ret.getReservedMemoryCapacity() + rc.getReservedMemoryCapacity());
            ret.setReservedCpuCapacity(ret.getReservedCpuCapacity() + rc.getReservedCpuCapacity());
        }

        return ret;
    }

    @Override
    public ReservedHostCapacity getReservedHostCapacityByClusters(List<String> clusterUuids) {
        ReservedHostCapacity ret = new ReservedHostCapacity();
        ret.setReservedCpuCapacity(0);
        ret.setReservedMemoryCapacity(0);

        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.uuid);
        q.add(HostVO_.clusterUuid, Op.IN, clusterUuids);
        List<String> huuids = q.listValue();
        if (huuids.isEmpty()) {
            return ret;
        }

        ReservedCapacityFinder finder = new ReservedCapacityFinder();
        finder.hostUuids = huuids;
        Collection<ReservedHostCapacity> col = finder.find().values();
        for (ReservedHostCapacity rc : col) {
            ret.setReservedMemoryCapacity(ret.getReservedMemoryCapacity() + rc.getReservedMemoryCapacity());
            ret.setReservedCpuCapacity(ret.getReservedCpuCapacity() + rc.getReservedCpuCapacity());
        }

        return ret;
    }

    @Override
    public ReservedHostCapacity getReservedHostCapacityByHosts(List<String> hostUuids) {
        ReservedCapacityFinder finder = new ReservedCapacityFinder();
        finder.hostUuids = hostUuids;
        return finder.find().values().iterator().next();
    }
}
