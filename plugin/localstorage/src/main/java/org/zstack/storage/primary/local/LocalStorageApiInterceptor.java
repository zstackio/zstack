package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.*;

import javax.persistence.Tuple;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

import java.util.ArrayList;
import java.util.List;

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
        LocalStorageResourceRefVO ref = q.find();
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
        LocalStorageResourceRefVO ref = q.find();
        if (ref == null) {
            throw new ApiMessageInterceptionException(argerr("the volume[uuid:%s] is not on any local primary storage", msg.getVolumeUuid()));
        }

        if (ref.getHostUuid().equals(msg.getDestHostUuid())) {
            throw new ApiMessageInterceptionException(argerr("the volume[uuid:%s] is already on the host[uuid:%s]", msg.getVolumeUuid(), msg.getDestHostUuid()));
        }

        PrimaryStorageVO vo = dbf.findByUuid(ref.getPrimaryStorageUuid(), PrimaryStorageVO.class);
        if (vo == null) {
            throw new ApiMessageInterceptionException(argerr("the primary storage[uuid:%s] is not found", msg.getPrimaryStorageUuid()));
        }

        if (vo.getState() == PrimaryStorageState.Disabled) {
            throw new ApiMessageInterceptionException(argerr("the primary storage[uuid:%s] is disabled cold migrate is not allowed", ref.getPrimaryStorageUuid()));
        }

        SimpleQuery<LocalStorageHostRefVO> hq = dbf.createQuery(LocalStorageHostRefVO.class);
        hq.add(LocalStorageHostRefVO_.hostUuid, Op.EQ, msg.getDestHostUuid());
        hq.add(LocalStorageHostRefVO_.primaryStorageUuid, Op.EQ, ref.getPrimaryStorageUuid());
        if (!hq.isExists()) {
            throw new ApiMessageInterceptionException(argerr("the dest host[uuid:%s] doesn't belong to the local primary storage[uuid:%s] where the" +
                    " volume[uuid:%s] locates", msg.getDestHostUuid(), ref.getPrimaryStorageUuid(), msg.getVolumeUuid()));
        }

        VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (VolumeStatus.Ready != vol.getStatus()) {
            throw new ApiMessageInterceptionException(argerr("the volume[uuid:%s] is not in status of Ready, cannot migrate it", msg.getVolumeUuid()));
        }

        if (vol.getType() == VolumeType.Data && vol.getVmInstanceUuid() != null) {
            throw new ApiMessageInterceptionException(argerr("the data volume[uuid:%s, name: %s] is still attached on the VM[uuid:%s]. Please detach" +
                    " it before migration", vol.getUuid(), vol.getName(), vol.getVmInstanceUuid()));
        } else if (vol.getType() == VolumeType.Root) {
            new SQLBatch() {
                @Override
                protected void scripts() {
                    VmInstanceState vmstate = Q.New(VmInstanceVO.class)
                            .select(VmInstanceVO_.state)
                            .eq(VmInstanceVO_.uuid,vol.getVmInstanceUuid()).findValue();
                    if (VmInstanceState.Stopped != vmstate) {
                        throw new ApiMessageInterceptionException(operr("the volume[uuid:%s] is the root volume of the vm[uuid:%s]. Currently the vm is in" +
                                " state of %s, please stop it before migration", vol.getUuid(), vol.getVmInstanceUuid(), vmstate));
                    }


                    long count = Q.New(VolumeVO.class)
                            .eq(VolumeVO_.type,VolumeType.Data)
                            .eq(VolumeVO_.vmInstanceUuid,vol.getVmInstanceUuid()).count();
                    if (count != 0) {
                        throw new ApiMessageInterceptionException(operr("the volume[uuid:%s] is the root volume of the vm[uuid:%s]. Currently the vm still" +
                                " has %s data volumes attached, please detach them before migration", vol.getUuid(), vol.getVmInstanceUuid(), count));
                    }

                    String originClusterUuid = Q.New(VmInstanceVO.class)
                            .select(VmInstanceVO_.clusterUuid)
                            .eq(VmInstanceVO_.uuid, vol.getVmInstanceUuid()).findValue();
                    if(originClusterUuid == null){
                        throw new ApiMessageInterceptionException(
                                err(SysErrors.INTERNAL,"The clusterUuid of vm[uuid:%s] cannot be null when migrate the root volume[uuid:%s, name: %s]",vol.getVmInstanceUuid(),vol.getUuid(),vol.getName()));
                    }
                    String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid)
                            .eq(HostVO_.uuid,msg.getDestHostUuid()).findValue();

                    if(!originClusterUuid.equals(clusterUuid)){
                        List<String> originL2NetworkList  = sql("select l2NetworkUuid from L3NetworkVO" +
                                " where uuid in(select l3NetworkUuid from VmNicVO where vmInstanceUuid = :vmUuid)")
                                .param("vmUuid",vol.getVmInstanceUuid()).list();
                        List<String> l2NetworkList = sql("select l2NetworkUuid from L2NetworkClusterRefVO" +
                                " where clusterUuid = :clusterUuid")
                                .param("clusterUuid",clusterUuid).list();
                        for(String l2:originL2NetworkList){
                            if(!l2NetworkList.contains(l2)){
                                throw new ApiMessageInterceptionException(
                                        operr("The two clusters[uuid:%s,uuid:%s] must access each other in l2 network  when migrate the vm[uuid;%s] to another cluster", originClusterUuid, clusterUuid, vol.getVmInstanceUuid()));
                            }
                        }
                    }
                }
            }.execute();

        }



        msg.setPrimaryStorageUuid(ref.getPrimaryStorageUuid());
    }

    private void validate(APIAddLocalPrimaryStorageMsg msg) {
        String url = msg.getUrl();
        if (!url.startsWith("/")) {
            throw new ApiMessageInterceptionException(argerr("the url[%s] is not an absolute path starting with '/'", msg.getUrl()));
        }
        if (url.startsWith("/dev") || url.startsWith("/proc") || url.startsWith("/sys")) {
            throw new ApiMessageInterceptionException(argerr(" the url contains an invalid folder[/dev or /proc or /sys]"));
        }
    }
}
