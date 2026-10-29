package org.zstack.kvm.vmfiles;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotGroupMsg;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class VmHostFileAutoCompleter implements GlobalApiMessageInterceptor {
    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(
                APICreateVmInstanceFromVolumeSnapshotGroupMsg.class
        );
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVmInstanceFromVolumeSnapshotGroupMsg) {
            validate((APICreateVmInstanceFromVolumeSnapshotGroupMsg) msg);
        }
        return msg;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    private void validate(APICreateVmInstanceFromVolumeSnapshotGroupMsg msg) {
        if (msg.getResetTpm() == null) {
            msg.setResetTpm(false);
        }
    }
}
