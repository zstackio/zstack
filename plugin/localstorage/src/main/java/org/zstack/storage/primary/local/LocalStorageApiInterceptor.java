package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.*;

import java.util.ArrayList;

/**
 * Created by frank on 7/1/2015.
 */
public class LocalStorageApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddLocalPrimaryStorageMsg) {
            validate((APIAddLocalPrimaryStorageMsg) msg);
        } else if (msg instanceof APILocalStorageMigrateVolumeMsg) {
            validate((APILocalStorageMigrateVolumeMsg) msg);
        } else if (msg instanceof APILocalStorageGetVolumeMigratableHostsMsg) {
            validate((APILocalStorageGetVolumeMigratableHostsMsg) msg);
        }

        return msg;
    }

    private void validate(APILocalStorageGetVolumeMigratableHostsMsg msg) {
        APILocalStorageGetVolumeMigratableReply reply = new APILocalStorageGetVolumeMigratableReply();

        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.add(LocalStorageResourceRefVO_.resourceType, Op.EQ, VolumeVO.class.getSimpleName());
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, msg.getVolumeUuid());
        LocalStorageResourceRefVO ref =  q.find();
        if (ref == null) {
            reply.setInventories(new ArrayList<HostInventory>());
            bus.reply(msg, reply);
            throw new StopRoutingException();
        }

        msg.setPrimaryStorageUuid(ref.getPrimaryStorageUuid());
    }

    private void validate(APILocalStorageMigrateVolumeMsg msg) {
        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.add(LocalStorageResourceRefVO_.resourceType, Op.EQ, VolumeVO.class.getSimpleName());
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, msg.getVolumeUuid());
        LocalStorageResourceRefVO ref =  q.find();
        if (ref == null) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("the volume[uuid:%s] is not on any local primary storage", msg.getVolumeUuid())
            ));
        }

        if (ref.getHostUuid().equals(msg.getDestHostUuid())) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("the volume[uuid:%s] is already on the host[uuid:%s]", msg.getVolumeUuid(), msg.getDestHostUuid())
            ));
        }

        SimpleQuery<LocalStorageHostRefVO> hq = dbf.createQuery(LocalStorageHostRefVO.class);
        hq.add(LocalStorageHostRefVO_.hostUuid, Op.EQ, msg.getDestHostUuid());
        hq.add(LocalStorageHostRefVO_.primaryStorageUuid, Op.EQ, ref.getPrimaryStorageUuid());
        if (!hq.isExists()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("the dest host[uuid:%s] doesn't belong to the local primary storage[uuid:%s] where the" +
                            " volume[uuid:%s] locates", msg.getDestHostUuid(), ref.getPrimaryStorageUuid(), msg.getVolumeUuid())
            ));
        }

        VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (VolumeStatus.Ready != vol.getStatus()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("the volume[uuid:%s] is not in status of Ready, cannot migrate it", msg.getVolumeUuid())
            ));
        }

        if (vol.getType() == VolumeType.Data && vol.getVmInstanceUuid() != null) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("the data volume[uuid:%s, name: %s] is still attached on the VM[uuid:%s]. Please detach" +
                            " it before migration", vol.getUuid(), vol.getName(), vol.getVmInstanceUuid())
            ));
        } else if (vol.getType() == VolumeType.Root) {
            SimpleQuery<VmInstanceVO> vmq = dbf.createQuery(VmInstanceVO.class);
            vmq.select(VmInstanceVO_.state);
            vmq.add(VmInstanceVO_.uuid, Op.EQ, vol.getVmInstanceUuid());
            VmInstanceState vmstate = vmq.findValue();
            if (VmInstanceState.Stopped != vmstate) {
                throw new ApiMessageInterceptionException(errf.stringToOperationError(
                        String.format("the volume[uuid:%s] is the root volume of the vm[uuid:%s]. Currently the vm is in" +
                                " state of %s, please stop it before migration", vol.getUuid(), vol.getVmInstanceUuid(), vmstate)
                ));
            }

            SimpleQuery<VolumeVO> vq = dbf.createQuery(VolumeVO.class);
            vq.add(VolumeVO_.type, Op.EQ, VolumeType.Data);
            vq.add(VolumeVO_.vmInstanceUuid, Op.EQ, vol.getVmInstanceUuid());
            long count = vq.count();
            if (count != 0) {
                throw new ApiMessageInterceptionException(errf.stringToOperationError(
                        String.format("the volume[uuid:%s] is the root volume of the vm[uuid:%s]. Currently the vm still" +
                                " has %s data volumes attached, please detach them before migration", vol.getUuid(), vol.getVmInstanceUuid(), count)
                ));
            }
        }

        msg.setPrimaryStorageUuid(ref.getPrimaryStorageUuid());
    }

    private void validate(APIAddLocalPrimaryStorageMsg msg) {
        if (!msg.getUrl().startsWith("/")) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("the url[%s] is not an absolute path starting with '/'", msg.getUrl())
            ));
        }
    }
}
