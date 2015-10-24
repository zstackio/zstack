package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.allocator.*;
import org.zstack.header.cluster.ReportHostCapacityMessage;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class HostAllocatorManagerImpl extends AbstractService implements HostAllocatorManager {
	private static final CLogger logger = Utils.getLogger(HostAllocatorManagerImpl.class);

	private Map<String, HostAllocatorStrategyFactory> factories = Collections.synchronizedMap(new HashMap<String, HostAllocatorStrategyFactory>());

	@Autowired
	private CloudBus bus;
	@Autowired
	private DatabaseFacade dbf;
	@Autowired
	private PluginRegistry pluginRgty;
    @Autowired
    private HostCapacityReserveManager reserveMgr;
    @Autowired
    private HostCapacityOverProvisioningManager ratioMgr;

	@Override
    @MessageSafe
	public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
	}

	private void handleLocalMessage(Message msg) {
		if (msg instanceof AllocateHostMsg) {
			handle((AllocateHostMsg) msg);
		} else if (msg instanceof ReportHostCapacityMessage) {
			handle((ReportHostCapacityMessage) msg);
		} else if (msg instanceof ReturnHostCapacityMsg) {
		    handle((ReturnHostCapacityMsg)msg);
		} else {
			bus.dealWithUnknownMessage(msg);
		}
	}

	private void handle(ReturnHostCapacityMsg msg) {
	    returnCapacity(msg.getHost().getUuid(), msg.getCpuCapacity(), msg.getMemoryCapacity());
    }

    private void handle(ReportHostCapacityMessage msg) {
        HostCapacityVO vo = dbf.findByUuid(msg.getHostUuid(), HostCapacityVO.class);
        long availCpu = msg.getTotalCpu() - msg.getUsedCpu();
        availCpu = availCpu > 0 ? availCpu : 0;
        long availMem = msg.getTotalMemory() - msg.getUsedMemory();
        availMem = availMem > 0 ? availMem : 0;
        if (vo == null) {
            vo = new HostCapacityVO();
            vo.setUuid(msg.getHostUuid());
            vo.setTotalCpu(msg.getTotalCpu());
            vo.setAvailableCpu(availCpu);
            vo.setTotalMemory(msg.getTotalMemory());
            vo.setAvailableMemory(availMem);
            vo.setTotalPhysicalMemory(msg.getTotalMemory());
            vo.setAvailablePhysicalMemory(availMem);

            HostCapacityStruct s = new HostCapacityStruct();
            s.setCapacityVO(vo);
            s.setTotalCpu(msg.getTotalCpu());
            s.setTotalMemory(msg.getTotalMemory());
            s.setUsedCpu(msg.getUsedCpu());
            s.setUsedMemory(msg.getUsedMemory());
            s.setInit(true);
            for (ReportHostCapacityExtensionPoint ext : pluginRgty.getExtensionList(ReportHostCapacityExtensionPoint.class)) {
                vo = ext.reportHostCapacity(s);
            }
            dbf.persist(vo);
        } else {
            vo.setTotalCpu(msg.getTotalCpu());
            vo.setAvailableCpu(availCpu);
            vo.setTotalPhysicalMemory(msg.getTotalMemory());
            vo.setAvailablePhysicalMemory(availMem);

            HostCapacityStruct s = new HostCapacityStruct();
            s.setCapacityVO(vo);
            s.setTotalCpu(msg.getTotalCpu());
            s.setTotalMemory(msg.getTotalMemory());
            s.setUsedCpu(msg.getUsedCpu());
            s.setUsedMemory(msg.getUsedMemory());
            s.setInit(false);
            for (ReportHostCapacityExtensionPoint ext : pluginRgty.getExtensionList(ReportHostCapacityExtensionPoint.class)) {
                vo = ext.reportHostCapacity(s);
            }
            dbf.update(vo);
        }
    }

	private void handle(final AllocateHostMsg msg) {
        HostAllocatorSpec spec = HostAllocatorSpec.fromAllocationMsg(msg);
        String allocatorStrategyType = null;
        for (HostAllocatorStrategyExtensionPoint ext : pluginRgty.getExtensionList(HostAllocatorStrategyExtensionPoint.class)) {
            allocatorStrategyType = ext.getHostAllocatorStrategyName(spec);
            if (allocatorStrategyType != null) {
                logger.debug(String.format("%s returns allocator strategy type[%s]", ext.getClass(), allocatorStrategyType));
                break;
            }
        }

        if (allocatorStrategyType == null) {
            allocatorStrategyType = msg.getAllocatorStrategy();
        }

        HostAllocatorStrategyFactory factory = getHostAllocatorStrategyFactory(HostAllocatorStrategyType.valueOf(allocatorStrategyType));
        HostAllocatorStrategy strategy = factory.getHostAllocatorStrategy();
        factory.marshalSpec(spec, msg);

        if (msg.isDryRun()) {
            final AllocateHostDryRunReply reply = new AllocateHostDryRunReply();
            strategy.dryRun(spec, new ReturnValueCompletion<List<HostInventory>>() {
                @Override
                public void success(List<HostInventory> returnValue) {
                    reply.setHosts(returnValue);
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        } else {
            final AllocateHostReply reply = new AllocateHostReply();
            strategy.allocate(spec,  new ReturnValueCompletion<HostInventory>(msg) {
                @Override
                public void success(HostInventory returnValue) {
                    reply.setHost(returnValue);
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        }
	}

	private void handleApiMessage(APIMessage msg) {
	    if (msg instanceof APIGetCpuMemoryCapacityMsg) {
	        handle((APIGetCpuMemoryCapacityMsg) msg);
        } else  if (msg instanceof APIGetHostAllocatorStrategiesMsg) {
            handle((APIGetHostAllocatorStrategiesMsg) msg);
	    } else {
	        bus.dealWithUnknownMessage(msg);
	    }
	}

    private void handle(APIGetHostAllocatorStrategiesMsg msg) {
        APIGetHostAllocatorStrategiesReply reply = new APIGetHostAllocatorStrategiesReply();
        reply.setHostAllocatorStrategies(HostAllocatorStrategyType.getAllExposedTypeNames());
        bus.reply(msg, reply);
    }


    private void handle(final APIGetCpuMemoryCapacityMsg msg) {
        APIGetCpuMemoryCapacityReply reply = new APIGetCpuMemoryCapacityReply();

        Tuple ret = new Callable<Tuple>() {
            @Override
            @Transactional(readOnly = true)
            public Tuple call() {
                if (msg.getHostUuids() != null && !msg.getHostUuids().isEmpty()) {
                    String sql = "select sum(hc.totalCpu), sum(hc.availableCpu), sum(hc.availableMemory), sum(hc.totalMemory) from HostCapacityVO hc where hc.uuid in (:hostUuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("hostUuids", msg.getHostUuids());
                    return q.getSingleResult();
                }  else if (msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty()) {
                    String sql = "select sum(hc.totalCpu), sum(hc.availableCpu), sum(hc.availableMemory), sum(hc.totalMemory) from HostCapacityVO hc, HostVO host where hc.uuid = host.uuid and host.clusterUuid in (:clusterUuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("clusterUuids", msg.getClusterUuids());
                    return q.getSingleResult();
                } else if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
                    String sql = "select sum(hc.totalCpu), sum(hc.availableCpu), sum(hc.availableMemory), sum(hc.totalMemory) from HostCapacityVO hc, HostVO host where hc.uuid = host.uuid and host.zoneUuid in (:zoneUuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("zoneUuids", msg.getZoneUuids());
                    return q.getSingleResult();
                }

                throw new CloudRuntimeException("should not be here");
            }
        }.call();

        long totalCpu = ret.get(0, Long.class) == null ? 0 : ret.get(0, Long.class) ;
        long availCpu = ret.get(1, Long.class) == null ? 0 : ret.get(1, Long.class);
        long availMemory = ret.get(2, Long.class) == null ? 0 : ret.get(2, Long.class);
        long totalMemory = ret.get(3, Long.class) == null ? 0 : ret.get(3, Long.class);

        ReservedHostCapacity rc = null;
        if (msg.getHostUuids() != null && !msg.getHostUuids().isEmpty()) {
            rc = reserveMgr.getReservedHostCapacityByHosts(msg.getHostUuids());
        } else if (msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty()) {
            rc = reserveMgr.getReservedHostCapacityByClusters(msg.getClusterUuids());
        } else if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
            rc = reserveMgr.getReservedHostCapacityByZones(msg.getZoneUuids());
        } else {
            throw new CloudRuntimeException("should not be here");
        }

        availCpu = availCpu - rc.getReservedCpuCapacity();
        availMemory = availMemory - rc.getReservedMemoryCapacity();
        availCpu = availCpu > 0 ? availCpu : 0;
        availMemory = availMemory > 0 ? availMemory : 0;

        reply.setTotalCpu(totalCpu);
        reply.setTotalMemory(totalMemory);
        reply.setAvailableCpu(availCpu);
        reply.setAvailableMemory(availMemory);
        bus.reply(msg, reply);
    }

    @Override
	public String getId() {
		return bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID);
	}

	private void populateHostAllocatorStrategyFactory() {
        for (HostAllocatorStrategyFactory ext : pluginRgty.getExtensionList(HostAllocatorStrategyFactory.class)) {
            HostAllocatorStrategyFactory old = factories.get(ext.getHostAllocatorStrategyType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate HostAllocatorStrategyFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getHostAllocatorStrategy()));
            }
            factories.put(ext.getHostAllocatorStrategyType().toString(), ext);
        }
	}
	
	@Override
	public boolean start() {
		populateHostAllocatorStrategyFactory();
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public HostAllocatorStrategyFactory getHostAllocatorStrategyFactory(HostAllocatorStrategyType type) {
		HostAllocatorStrategyFactory factory = factories.get(type.toString());
		if (factory == null) {
			throw new CloudRuntimeException(String.format("Unable to find HostAllocatorStrategyFactory with type[%s]", type));
		}

		return factory;
	}
	
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
    public void returnCapacity(String hostUuid, long cpu, long memory) {
		HostCapacityVO vo = dbf.getEntityManager().find(HostCapacityVO.class, hostUuid, LockModeType.PESSIMISTIC_WRITE);
		if (vo == null) {
			logger.warn(String.format("Unable to return cpu[%s], memory[%s] to host[uuid:%s], it may have been deleted", cpu, memory, hostUuid));
			return;
		}

        long availCpu = vo.getAvailableCpu() + cpu;
        availCpu = availCpu > vo.getTotalCpu() ? vo.getTotalCpu() : availCpu;
        vo.setAvailableCpu(availCpu);

        memory = ratioMgr.calculateMemoryByRatio(hostUuid, memory);
        long availMemory = vo.getAvailableMemory() + memory;
        if (availMemory > vo.getTotalMemory()) {
            throw new CloudRuntimeException(String.format("invalid memory capacity of host[uuid:%s], available memory[%s] is greater than total memory[%s]",
                    hostUuid, availMemory, vo.getTotalMemory()));
        }
        vo.setAvailableMemory(availMemory);

		dbf.getEntityManager().merge(vo);
		logger.debug(String.format("Successfully returned cpu[%s HZ], memory[%s bytes] to host[uuid:%s]", cpu, memory, hostUuid));
    }
}
