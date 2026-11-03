package org.zstack.sugonSdnController.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerFactory;
import org.zstack.sdnController.SdnControllerType;
import org.zstack.sdnController.header.SdnControllerVO;
import org.zstack.sdnController.header.SdnControllerVO_;
import org.zstack.sugonSdnController.network.TfZstackPortSync;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SugonSdnControllerFactory implements SdnControllerFactory, ManagementNodeReadyExtensionPoint {
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private TfZstackPortSync tfZstackPortSync;
    private Future<Void> trackerThread;

    @Override
    public void managementNodeReady() {
        if (trackerThread != null) {
            trackerThread.cancel(true);
        }
        // 4.8 has no controller ping lifecycle; retain daily discovery here.
        trackerThread = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.DAYS;
            }

            @Override
            public long getInterval() {
                return 1;
            }

            @Override
            public String getName() {
                return "discover-tf-controllers-for-port-sync";
            }

            @Override
            public void run() {
                List<String> controllerUuids = Q.New(SdnControllerVO.class)
                        .select(SdnControllerVO_.uuid)
                        .eq(SdnControllerVO_.vendorType, SugonSdnControllerConstant.TF_CONTROLLER)
                        .listValues();
                for (String controllerUuid : controllerUuids) {
                    tfZstackPortSync.triggerSyncIfDue(controllerUuid);
                }
            }
        });
    }


    SdnControllerType sdnControllerType = new SdnControllerType(SugonSdnControllerConstant.TF_CONTROLLER);
    @Override
    public SdnControllerType getVendorType() {
        return sdnControllerType;
    }

    @Override
    public SdnController getSdnController(SdnControllerVO vo) {
        return new SugonSdnController(vo);
    }
}
