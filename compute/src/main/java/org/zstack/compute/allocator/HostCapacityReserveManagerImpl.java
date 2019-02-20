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
                ReservedHostCapacity extHc = ext.getReservedHostCapacity(huuid);
                if (hc.getReservedMemoryCapacity() == -1) {
                    hc.setReservedMemoryCapacity(extHc.getReservedMemoryCapacity());
                }
                if (hc.getReservedCpuCapacity() == -1) {
                    hc.setReservedCpuCapacity(extHc.getReservedCpuCapacity());
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
            if (requiredMemory == 0 || hvo.getCapacity().getAvailableMemory() - hc.getReservedMemoryCapacity() >= ratioMgr.calculateMemoryByRatio(hvo.getUuid(), requiredMemory)) {
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
