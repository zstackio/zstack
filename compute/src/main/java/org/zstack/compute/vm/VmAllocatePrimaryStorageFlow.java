package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.VolumeSpec;
import org.zstack.utils.Bucket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocatePrimaryStorageFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;

    private static final String SUCCESS = VmAllocatePrimaryStorageFlow.class.getName();

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final List<AllocatePrimaryStorageMsg> msgs = new ArrayList<AllocatePrimaryStorageMsg>();
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        HostInventory destHost = spec.getDestHost();
        final ImageInventory iminv = spec.getImageSpec().getInventory();
        AllocatePrimaryStorageMsg rmsg = new AllocatePrimaryStorageMsg();
        rmsg.setVmInstanceUuid(spec.getVmInventory().getUuid());
        rmsg.setImageUuid(spec.getImageSpec().getInventory().getUuid());
        if (ImageMediaType.ISO.toString().equals(iminv.getMediaType())) {
            rmsg.setSize(spec.getRootDiskOffering().getDiskSize());
            rmsg.setAllocationStrategy(spec.getRootDiskOffering().getAllocatorStrategy());
            rmsg.setHostUuid(destHost.getUuid());
            rmsg.setDiskOfferingUuid(spec.getRootDiskOffering().getUuid());
        } else {
            //TODO: find a way to allow specifying strategy for root disk
            rmsg.setSize(iminv.getSize());
            rmsg.setHostUuid(destHost.getUuid());
        }
        bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
        msgs.add(rmsg);

        for (DiskOfferingInventory dinv : spec.getDataDiskOfferings()) {
            AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
            amsg.setSize(dinv.getDiskSize());
            amsg.setHostUuid(destHost.getUuid());
            amsg.setAllocationStrategy(dinv.getAllocatorStrategy());
            amsg.setDiskOfferingUuid(dinv.getUuid());
            bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
            msgs.add(amsg);
        }

        bus.send(msgs, 1, new CloudBusListCallBack(trigger) {
            @Override
            public void run(List<MessageReply> replies) {
                List<Bucket> ret = new ArrayList<Bucket>();
                ErrorCode err = null;
                for (MessageReply r : replies) {
                    if (!r.isSuccess()) {
                        err = r.getError();
                    } else {
                        AllocatePrimaryStorageMsg msg = msgs.get(replies.indexOf(r));
                        AllocatePrimaryStorageReply ar = r.castReply();
                        ret.add(Bucket.newBucket(ar.getPrimaryStorageInventory(), msg.getSize()));
                    }
                }

                data.put(SUCCESS, ret);

                if (err != null) {
                    trigger.fail(err);
                } else {
                    VolumeSpec rootSpec = new VolumeSpec();
                    PrimaryStorageInventory rootPri = ret.get(0).get(0);
                    Long size = ret.get(0).get(1);
                    rootSpec.setPrimaryStorageInventory(rootPri);
                    rootSpec.setRoot(true);
                    rootSpec.setSize(size);
                    spec.getVolumeSpecs().add(rootSpec);

                    if (ret.size() > 1) {
                        for (int i=1; i<ret.size(); i++) {
                            Bucket b = ret.get(i);
                            PrimaryStorageInventory pri = b.get(0);
                            Long dsize = b.get(1);
                            DiskOfferingInventory dinv = spec.getDataDiskOfferings().get(i-1);
                            VolumeSpec vspec = new VolumeSpec();
                            vspec.setDiskOfferingUuid(dinv.getUuid());
                            vspec.setPrimaryStorageInventory(pri);
                            vspec.setSize(dsize);
                            vspec.setRoot(false);
                            spec.getVolumeSpecs().add(vspec);
                        }
                    }

                    trigger.next();
                }
            }
        });
    }

    @Override
    public void rollback(FlowTrigger chain, Map data) {
        List<Bucket> buckets = (List<Bucket>) data.get(SUCCESS);
        if (buckets != null) {
            for (Bucket b : buckets) {
                ReturnPrimaryStorageCapacityMsg msg = new ReturnPrimaryStorageCapacityMsg();
                PrimaryStorageInventory pri = b.get(0);
                Long size = b.get(1);
                msg.setDiskSize(size);
                msg.setPrimaryStorageUuid(pri.getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, pri.getUuid());
                bus.send(msg);
            }
        }

        chain.rollback();
    }
}
