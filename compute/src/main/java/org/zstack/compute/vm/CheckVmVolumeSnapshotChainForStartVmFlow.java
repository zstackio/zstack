package org.zstack.compute.vm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.GetVolumeBackingChainFromPrimaryStorageMsg;
import org.zstack.header.storage.primary.GetVolumeBackingChainFromPrimaryStorageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.header.volume.VolumeConstant.VOLUME_FORMAT_QCOW2;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CheckVmVolumeSnapshotChainForStartVmFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(CheckVmVolumeSnapshotChainForStartVmFlow.class);
    private static final Logger log = LoggerFactory.getLogger(CheckVmVolumeSnapshotChainForStartVmFlow.class);
    @Autowired
    protected CloudBus bus;

    @Override
    public void run(final FlowTrigger chain, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        Map<String, List<String>> volumesAliveSnapshotChain =
                VmVolumeSnapshotChainUtil.getVmVolumesAliveSnapshotChain(spec.getVmInventory().getAllDiskVolumes());
        if (volumesAliveSnapshotChain.isEmpty()) {
            logger.debug(String.format("no alive snapshot chain found for vm[uuid:%s], skip checking", spec.getVmInventory().getUuid()));
            chain.next();
            return;
        }

        List<VolumeInventory> vols = spec.getVmInventory().getAllDiskVolumes().stream()
                .filter(volumeVO -> Objects.equals(volumeVO.getFormat(), VOLUME_FORMAT_QCOW2))
                .filter(volumeVO -> !Objects.equals(volumeVO.getType(), VolumeType.Memory.toString()))
                .collect(Collectors.toList());
        List<ErrorCode> errors = new ArrayList<>();
        new While<>(vols).all((volume, completion) -> {
            GetVolumeBackingChainFromPrimaryStorageMsg gmsg = new GetVolumeBackingChainFromPrimaryStorageMsg();
            gmsg.setVolumeUuid(volume.getUuid());
            gmsg.setRootInstallPaths(Collections.singletonList(volume.getInstallPath()));
            gmsg.setPrimaryStorageUuid(volume.getPrimaryStorageUuid());
            gmsg.setVolumeFormat(volume.getFormat());
            bus.makeTargetServiceIdByResourceUuid(gmsg, PrimaryStorageConstant.SERVICE_ID, volume.getPrimaryStorageUuid());
            bus.send(gmsg, new CloudBusCallBack(chain) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errors.add(reply.getError());
                        completion.done();
                        return;
                    }

                    GetVolumeBackingChainFromPrimaryStorageReply gr = reply.castReply();
                    List<String> backingChainInstallPath = gr.getBackingChainInstallPath().get(volume.getInstallPath());

                    List<String> aliveChainInDB = volumesAliveSnapshotChain.get(volume.getUuid());
                    if (aliveChainInDB == null) {
                        completion.done();
                        return;
                    }
                    aliveChainInDB.remove(0);
                    if (aliveChainInDB.isEmpty()) {
                        completion.done();
                        return;
                    }

                    for (int i = 0; i < aliveChainInDB.size(); i++) {
                        if (!backingChainInstallPath.contains(aliveChainInDB.get(i))) {
                            errors.add(operr("the volume[%s]'s alive snapshot chain in database is %s, " +
                                    "but the actual chain is %s", volume.getUuid(), aliveChainInDB, backingChainInstallPath));
                        }
                    }
                    completion.done();
                }
            });
        }).run(new WhileDoneCompletion(chain) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errors.isEmpty()) {
                    chain.fail(ErrorCode.fromString(errors.toString()));
                } else {
                    chain.next();
                }
            }
        });
    }
}
