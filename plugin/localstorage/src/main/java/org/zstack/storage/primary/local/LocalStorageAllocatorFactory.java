package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.host.HostVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.tag.TagResourceType;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by frank on 7/1/2015.
 */
public class LocalStorageAllocatorFactory implements PrimaryStorageAllocatorStrategyFactory, Component,
        HostAllocatorFilterExtensionPoint, PrimaryStorageAllocatorStrategyExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;

    public static PrimaryStorageAllocatorStrategyType type = new PrimaryStorageAllocatorStrategyType(LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY);

    private List<String> allocatorFlowNames;
    private FlowChainBuilder builder = new FlowChainBuilder();
    private LocalStorageAllocatorStrategy strategy;

    @Override
    public PrimaryStorageAllocatorStrategyType getPrimaryStorageAllocatorStrategyType() {
        return type;
    }

    @Override
    public PrimaryStorageAllocatorStrategy getPrimaryStorageAllocatorStrategy() {
        return strategy;
    }

    public List<String> getAllocatorFlowNames() {
        return allocatorFlowNames;
    }

    public void setAllocatorFlowNames(List<String> allocatorFlowNames) {
        this.allocatorFlowNames = allocatorFlowNames;
    }

    @Override
    public boolean start() {
        builder.setFlowClassNames(allocatorFlowNames).construct();
        strategy = new LocalStorageAllocatorStrategy(builder);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public List<HostVO> filterHostCandidates(List<HostVO> candidates, HostAllocatorSpec spec) {
        List<String> huuids = CollectionUtils.transformToList(candidates, new Function<String, HostVO>() {
            @Override
            public String call(HostVO arg) {
                return arg.getUuid();
            }
        });

        SimpleQuery<LocalStorageHostRefVO> q = dbf.createQuery(LocalStorageHostRefVO.class);
        q.select(LocalStorageHostRefVO_.hostUuid);
        q.add(LocalStorageHostRefVO_.hostUuid, Op.IN, huuids);
        q.add(LocalStorageHostRefVO_.availableCapacity, Op.LT, spec.getDiskSize());
        final List<String> toRemoveHuuids = q.listValue();
        if (toRemoveHuuids.isEmpty()) {
            return candidates;
        }


        return CollectionUtils.transformToList(candidates, new Function<HostVO, HostVO>() {
            @Override
            public HostVO call(HostVO arg) {
                return toRemoveHuuids.contains(arg.getUuid()) ? null : arg;
            }
        });
    }

    @Override
    public String getPrimaryStorageAllocatorStrategyName(final AllocatePrimaryStorageMsg msg) {
        String allocatorType = null;
        if (msg.getExcludeAllocatorStrategies() != null && msg.getExcludeAllocatorStrategies().contains(LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY)) {
            allocatorType = null;
        } else if (LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY.equals(msg.getAllocationStrategy())) {
            allocatorType = LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY;
        } else if (msg.getPrimaryStorageUuid() != null) {
            SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
            q.select(PrimaryStorageVO_.type);
            q.add(PrimaryStorageVO_.uuid, Op.EQ, msg.getPrimaryStorageUuid());
            String type = q.findValue();
            if (LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(type)) {
                allocatorType = LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY;
            }
        } else if (msg.getHostUuid() != null) {
            allocatorType = new Callable<String>() {
                @Override
                @Transactional(readOnly = true)
                public String call() {
                    String sql = "select ps.type from PrimaryStorageVO ps, PrimaryStorageClusterRefVO ref, HostVO host where ps.uuid = ref.primaryStorageUuid and ref.clusterUuid = host.clusterUuid and host.uuid = :huuid";
                    TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                    q.setParameter("huuid", msg.getHostUuid());
                    List<String> types = q.getResultList();
                    for (String type : types) {
                        if (type.equals(LocalStorageConstants.LOCAL_STORAGE_TYPE)) {
                            return LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY;
                        }
                    }
                    return null;
                }
            }.call();
        }

        return allocatorType;
    }
}
