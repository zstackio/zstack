package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.allocator.*;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;

/**
 * Create by lining at 2020/08/17
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateHostDryrunFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmAllocateHostDryrunFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected VmInstanceExtensionPointEmitter extEmitter;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        // 试运行，根据策略获取满足条件的可能物理机，这个物理机是有顺序的（满足策略，但是不一定能够成功）
        // 还有集群信息，
        // 依据物理的顺序，对集群排序，
        AllocateHostMsg amsg = prepareMsg(spec);
        bus.send(amsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                    return;
                }
                AllocateHostDryRunReply r = reply.castReply();
                data.put("hostInventories", r.getHosts());
                data.put("clusters", CollectionUtils.transformToList(r.getHosts(), HostInventory::getClusterUuid));
                trigger.next();
            }
        });
    }

    private long getTotalDataDiskSize(VmInstanceSpec spec) {
        long size = 0;
        for (DiskOfferingInventory dinv : spec.getDataDiskOfferings()) {
            size += dinv.getDiskSize();
        }
        return size;
    }

    private AllocateHostMsg prepareMsg(VmInstanceSpec spec) {
        DesignatedAllocateHostMsg msg = new DesignatedAllocateHostMsg();

        List<DiskOfferingInventory> diskOfferings = new ArrayList<>();
        ImageInventory image = spec.getImageSpec().getInventory();
        long diskSize;
        if (image.getMediaType() != null && image.getMediaType().equals(ImageConstant.ImageMediaType.ISO.toString())) {
            DiskOfferingVO dvo = dbf.findByUuid(spec.getRootDiskOffering().getUuid(), DiskOfferingVO.class);
            diskSize = dvo.getDiskSize();
            diskOfferings.add(DiskOfferingInventory.valueOf(dvo));
        } else {
            diskSize = image.getSize();
        }
        diskSize += getTotalDataDiskSize(spec);
        diskOfferings.addAll(spec.getDataDiskOfferings());
        msg.setSoftAvoidHostUuids(spec.getSoftAvoidHostUuids());
        msg.setAvoidHostUuids(spec.getAvoidHostUuids());
        msg.setDiskOfferings(diskOfferings);
        msg.setDiskSize(diskSize);
        msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
        msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
        List<L3NetworkInventory> l3Invs = VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks());
        msg.setL3NetworkUuids(CollectionUtils.transformToList(l3Invs,
                new Function<String, L3NetworkInventory>() {
                    @Override
                    public String call(L3NetworkInventory arg) {
                        return arg.getUuid();
                    }
                }));
        msg.setImage(image);
        msg.setVmOperation(spec.getCurrentVmOperation().toString());

        if (spec.getVmInventory().getZoneUuid() != null) {
            msg.setZoneUuid(spec.getVmInventory().getZoneUuid());
        }
        if (spec.getVmInventory().getClusterUuid() != null) {
            msg.setClusterUuid(spec.getVmInventory().getClusterUuid());
        }
        msg.setHostUuid(spec.getRequiredHostUuid());
        if (spec.getHostAllocatorStrategy() != null) {
            msg.setAllocatorStrategy(spec.getHostAllocatorStrategy());
        } else {
            msg.setAllocatorStrategy(spec.getVmInventory().getAllocatorStrategy());
        }
        if (spec.getRequiredPrimaryStorageUuidForRootVolume() != null) {
            msg.addRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForRootVolume());
        }
        if (spec.getRequiredPrimaryStorageUuidForDataVolume() != null) {
            msg.addRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForDataVolume());
        }
        msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        msg.setVmInstance(spec.getVmInventory());

        if (spec.getImageSpec() != null && spec.getImageSpec().getSelectedBackupStorage() != null) {
            msg.setRequiredBackupStorageUuid(spec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid());
        }

        msg.setListAllHosts(true);
        msg.setDryRun(true);
        msg.setListAllHostsGroupByCluster(true);
        return msg;
    }

    @Override
    public void rollback(final FlowRollback trigger, Map data) {
        trigger.rollback();
    }
}
