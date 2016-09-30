package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.allocator.*;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstance;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class TagAllocatorFlow extends AbstractHostAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(TagAllocatorFlow.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    private List<InstanceOfferingTagAllocatorExtensionPoint> instanceOfferingExtensions;
    private List<DiskOfferingTagAllocatorExtensionPoint> diskOfferingExtensions;

    public TagAllocatorFlow() {
        instanceOfferingExtensions = pluginRgty.getExtensionList(InstanceOfferingTagAllocatorExtensionPoint.class);
        diskOfferingExtensions = pluginRgty.getExtensionList(DiskOfferingTagAllocatorExtensionPoint.class);
    }

    @Override
    public void allocate() {
        throwExceptionIfIAmTheFirstFlow();

        if (!instanceOfferingExtensions.isEmpty()) {
            SimpleQuery<SystemTagVO> q  = dbf.createQuery(SystemTagVO.class);
            q.add(SystemTagVO_.resourceType, Op.EQ, VmInstanceVO.class.getSimpleName());
            q.add(SystemTagVO_.resourceUuid, Op.EQ, spec.getVmInstance().getUuid());
            List<SystemTagVO> tvos = q.list();
            if (!tvos.isEmpty()) {
                List tinvs = SystemTagInventory.valueOf(tvos);
                List<HostVO> tmp = candidates;
                for (InstanceOfferingTagAllocatorExtensionPoint extp : instanceOfferingExtensions) {
                    List<HostVO> ret = extp.allocateHost(tinvs, tmp, spec);
                    if (ret == null) {
                        continue;
                    }

                    tmp = ret;

                    if (tmp.isEmpty()) {
                        fail(String.format("InstanceOfferingTagAllocatorExtensionPoint[%s] return zero candidate host", extp.getClass().getName()));
                        return;
                    } else {
                        logger.debug(String.format("[Host Allocation]: InstanceOfferingTagAllocatorExtensionPoint[%s] successfully found %s candidate hosts for vm[uuid:%s, name:%s]",
                                extp.getClass().getName(), tmp.size(), spec.getVmInstance().getUuid(), spec.getVmInstance().getName()));
                    }
                }

                candidates = tmp;
            }
        }

        if (!diskOfferingExtensions.isEmpty() && spec.getDiskOfferings() != null && !spec.getDiskOfferings().isEmpty()) {
            List<String> diskOfferingUuids = CollectionUtils.transformToList(spec.getDiskOfferings(), new Function<String, DiskOfferingInventory>() {
                @Override
                public String call(DiskOfferingInventory arg) {
                    return arg.getUuid();
                }
            });

            SimpleQuery<SystemTagVO> q  = dbf.createQuery(SystemTagVO.class);
            q.add(SystemTagVO_.resourceType, Op.EQ, DiskOfferingVO.class.getSimpleName());
            q.add(SystemTagVO_.resourceUuid, Op.IN, diskOfferingUuids);
            List<SystemTagVO> tvos = q.list();
            if (!tvos.isEmpty()) {
                List tinvs = SystemTagInventory.valueOf(tvos);
                List<HostVO> tmp = candidates;
                for (DiskOfferingTagAllocatorExtensionPoint extp : diskOfferingExtensions) {
                    List<HostVO> ret = extp.allocateHost(tinvs, tmp, spec);
                    if (ret == null) {
                        continue;
                    }

                    tmp = ret;

                    if (tmp.isEmpty()) {
                        fail(String.format("DiskOfferingTagAllocatorExtensionPoint[%s] return zero candidate host", extp.getClass().getName()));
                        return;
                    } else {
                        logger.debug(String.format("[Host Allocation]: DiskOfferingTagAllocatorExtensionPoint[%s] successfully found %s candidate hosts for vm[uuid:%s, name:%s]",
                                extp.getClass().getName(), tmp.size(), spec.getVmInstance().getUuid(), spec.getVmInstance().getName()));
                    }
                }

                candidates = tmp;
            }
        }

        next(candidates);
    }
}
