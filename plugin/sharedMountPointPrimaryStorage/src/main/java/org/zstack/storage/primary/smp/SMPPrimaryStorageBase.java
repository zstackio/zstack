package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.primary.*;
import org.zstack.storage.primary.PrimaryStorageBase;

/**
 * Created by xing5 on 2016/3/26.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SMPPrimaryStorageBase extends PrimaryStorageBase {
    public SMPPrimaryStorageBase(PrimaryStorageVO self) {
        super(self);
    }

    @Override
    protected void handle(InstantiateVolumeMsg msg) {

    }

    @Override
    protected void handle(DeleteVolumeOnPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(DownloadDataVolumeToPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(DeleteBitsOnPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(DownloadIsoToPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(DeleteIsoFromPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {

    }

    @Override
    protected void connectHook(ConnectPrimaryStorageMsg msg, Completion completion) {

    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {

    }
}
