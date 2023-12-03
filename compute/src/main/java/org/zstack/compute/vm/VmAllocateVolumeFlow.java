package org.zstack.compute.vm;

import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.VolumeSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.*;
import org.zstack.header.volume.VolumeDeletionPolicyManager.VolumeDeletionPolicy;
import org.zstack.identity.AccountManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.function.Function;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.progress.ProgressReportService.taskProgress;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateVolumeFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected AccountManager acntMgr;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private EventFacade evtf;

    protected List<CreateVolumeMsg> prepareMsg(Map<String, Object> ctx) {
        taskProgress("create volumes");

        VmInstanceSpec spec = (VmInstanceSpec) ctx.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(spec.getVmInventory().getUuid());
        if (accountUuid == null) {
            throw new CloudRuntimeException(String.format("accountUuid for vm[uuid:%s] is null", spec.getVmInventory().getUuid()));
        }

        if (MapUtils.isNotEmpty(spec.getDataVolumeSystemTagsOnIndex())) {
            for (Map.Entry<String, List<String>> entry : spec.getDataVolumeSystemTagsOnIndex().entrySet()) {
                if (spec.getDataDiskOfferings().get(Integer.parseInt(entry.getKey())) == null) {
                    continue;
                }
                String offeringUuid = spec.getDataDiskOfferings().get(Integer.parseInt(entry.getKey())).getUuid();
                for (VolumeSpec volumeSpec : spec.getVolumeSpecs()) {
                    if (!Objects.equals(volumeSpec.getType(), VolumeType.Data.toString())) {
                        continue;
                    }
                    if (volumeSpec.getDiskOfferingUuid() == null) {
                        continue;
                    }
                    if (!volumeSpec.getDiskOfferingUuid().equals(offeringUuid)) {
                        continue;
                    }
                    if (volumeSpec.getTags() == null) {
                        volumeSpec.setTags(entry.getValue());
                        break;
                    } else if (volumeSpec.getTags() != null && !volumeSpec.getTags().containsAll(entry.getValue())) {
                        volumeSpec.setTags(entry.getValue());
                        break;
                    }
                }
            }
        }

        List<VolumeSpec> volumeSpecs = spec.getVolumeSpecs();
        List<CreateVolumeMsg> msgs = new ArrayList<>(volumeSpecs.size());
        for (VolumeSpec vspec : volumeSpecs) {
            CreateVolumeMsg msg = new CreateVolumeMsg();
            Set<String> tags = new HashSet<>();
            if (vspec != null) {
                if (vspec.getTags() != null) {
                    tags.addAll(vspec.getTags());
                }
            } else {
                continue;
            }

            DebugUtils.Assert(vspec.getType() != null, "VolumeType can not be null!");

            if (VolumeType.Root.toString().equals(vspec.getType())) {
                msg.setResourceUuid((String) ctx.get("uuid"));
                msg.setName("ROOT-for-" + spec.getVmInventory().getName());
                msg.setDescription(String.format("Root volume for VM[uuid:%s]", spec.getVmInventory().getUuid()));
                msg.setRootImageUuid(spec.getImageSpec().getInventory().getUuid());
                if (spec.getRootVolumeSystemTags() != null) {
                    tags.addAll(spec.getRootVolumeSystemTags());
                }
                if (ImageMediaType.ISO.toString().equals(spec.getImageSpec().getInventory().getMediaType())) {
                    msg.setFormat(VolumeFormat.getVolumeFormatByMasterHypervisorType(spec.getDestHost().getHypervisorType()).toString());
                } else if (spec.getImageSpec().getInventory().getFormat() != null) {
                    VolumeFormat imageFormat = VolumeFormat.valueOf(spec.getImageSpec().getInventory().getFormat());
                    msg.setFormat(imageFormat.getOutputFormat(spec.getDestHost().getHypervisorType()));
                } else {
                    msg.setFormat(ImageConstant.QCOW2_FORMAT_STRING);
                }
            } else if (VolumeType.Data.toString().equals(vspec.getType())) {
                msg.setName(String.format("DATA-for-%s", spec.getVmInventory().getName()));
                msg.setDescription(String.format("DataVolume-%s", spec.getVmInventory().getUuid()));
                msg.setFormat(VolumeFormat.getVolumeFormatByMasterHypervisorType(spec.getDestHost().getHypervisorType()).toString());
                if (spec.getDataVolumeSystemTags() != null) {
                    tags.addAll(spec.getDataVolumeSystemTags());
                }
            } else {
                continue;
            }

            msg.setSystemTags(new ArrayList<>(tags));
            msg.setDiskOfferingUuid(vspec.getDiskOfferingUuid());
            msg.setSize(vspec.getSize());
            msg.setPrimaryStorageUuid(vspec.getPrimaryStorageInventory().getUuid());
            msg.setVmInstanceUuid(spec.getVmInventory().getUuid());
            msg.setVolumeType(vspec.getType());
            msg.setAccountUuid(accountUuid);

            bus.makeLocalServiceId(msg, VolumeConstant.SERVICE_ID);
            msgs.add(msg);
        }

        return msgs;
    }

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<CreateVolumeMsg> msgs = prepareMsg(data);
        bus.send(msgs, 1, new CloudBusListCallBack(trigger) {
            @Override
            public void run(List<MessageReply> replies) {
                ErrorCode err = null;
                for (MessageReply r : replies) {
                    VolumeSpec vspec = spec.getVolumeSpecs().get(replies.indexOf(r));

                    if (r.isSuccess()) {
                        CreateVolumeReply cr = r.castReply();
                        VolumeInventory inv = cr.getInventory();
                        if (inv.getType().equals(VolumeType.Root.toString())) {
                            spec.setDestRootVolume(inv);
                            UpdateQuery.New(VmInstanceVO.class)
                                    .isNull(VmInstanceVO_.rootVolumeUuid)
                                    .set(VmInstanceVO_.rootVolumeUuid, inv.getUuid())
                                    .condAnd(VmInstanceVO_.uuid, Op.EQ, spec.getVmInventory().getUuid())
                                    .update();
                        } else {
                            spec.getDestDataVolumes().add(inv);
                        }

                        vspec.setAssociatedVolumeUuid(inv.getUuid());
                        vspec.setIsVolumeCreated(true);
                    } else {
                        err = r.getError();
                        vspec.setIsVolumeCreated(false);
                    }
                }

                if (err != null) {
                    trigger.fail(err);
                } else {
                    trigger.next();
                }
            }
        });
    }

    @Override
    public void rollback(final FlowRollback chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<VolumeInventory> destVolumes = new ArrayList<>(spec.getDestDataVolumes().size() + 1);
        if (spec.getDestRootVolume() != null) {
            destVolumes.add(spec.getDestRootVolume());
        }
        destVolumes.addAll(spec.getDestDataVolumes());

        final List<DeleteVolumeMsg> msgs = CollectionUtils.transformToList(destVolumes, new Function<DeleteVolumeMsg, VolumeInventory>() {
            @Override
            public DeleteVolumeMsg call(VolumeInventory arg) {
                DeleteVolumeMsg msg = new DeleteVolumeMsg();
                msg.setDeletionPolicy(VolumeDeletionPolicy.Direct.toString());
                msg.setUuid(arg.getUuid());
                // don't do detach; because the VM is in state of Starting, it cannot do a detach operation.
                msg.setDetachBeforeDeleting(false);
                bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, arg.getUuid());
                return msg;
            }
        });

        if (msgs.isEmpty()) {
            chain.rollback();
            return;
        }

        new While<>(msgs).each((msg, compl) -> {
            bus.send(msg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        compl.done();
                        return;
                    }

                    DeleteVolumeGC gc = new DeleteVolumeGC();
                    gc.NAME = String.format("gc-volume-%s", msg.getVolumeUuid());
                    gc.deletionPolicy = VolumeDeletionPolicy.Direct.toString();
                    gc.volumeUuid = msg.getVolumeUuid();
                    gc.submit(TimeUnit.HOURS.toSeconds(8), TimeUnit.SECONDS);

                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(chain) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                // if ChangeImage, resume rootVolumeUuid
                if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.ChangeImage) {
                    VmInstanceVO vm = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class);
                    vm.setRootVolumeUuid(spec.getVmInventory().getRootVolumeUuid());
                    dbf.update(vm);
                }

                chain.rollback();
            }
        });
    }
}
