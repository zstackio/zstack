package org.zstack.sdnController.hardwareVxlan;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.network.l2.vxlan.vtep.APICreateVxlanVtepMsg;
import org.zstack.network.l2.vxlan.vtep.CreateVtepMsg;
import org.zstack.network.l2.vxlan.vtep.DeleteVtepMsg;
import org.zstack.network.l2.vxlan.vtep.PopulateVtepPeersMsg;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO_;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.*;
import org.zstack.sdnController.header.HardwareL2VxlanNetworkPoolInventory;
import org.zstack.sdnController.header.HardwareL2VxlanNetworkPoolVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;


/**
 * Created by shixin.ruan on 09/17/2019.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HardwareVxlanNetworkPool extends VxlanNetworkPool {
    private static final CLogger logger = Utils.getLogger(HardwareVxlanNetworkPool.class);

    public HardwareVxlanNetworkPool(L2NetworkVO vo) {
        super(vo);
    }

    private HardwareL2VxlanNetworkPoolVO getSelf() {
        return (HardwareL2VxlanNetworkPoolVO) self;
    }

    protected HardwareL2VxlanNetworkPoolInventory getSelfInventory() {
        return HardwareL2VxlanNetworkPoolInventory.valueOf(getSelf());
    }

    @Override
    protected void handle(final PopulateVtepPeersMsg msg) {
        throw new CloudRuntimeException("HardwareVxlanNetworkPool don't need vtep");
    }

    @Override
    protected void handle(CreateVtepMsg msg) {
        throw new CloudRuntimeException("HardwareVxlanNetworkPool don't need vtep");
    }

    @Override
    protected void handle(DeleteVtepMsg msg) {
        throw new CloudRuntimeException("HardwareVxlanNetworkPool don't need vtep");
    }

    @Override
    protected void handle(final APICreateVxlanVtepMsg msg) {
        throw new CloudRuntimeException("HardwareVxlanNetworkPool don't need vtep");
    }

    @Override
    protected void afterDetachVxlanPoolFromCluster(APIDetachL2NetworkFromClusterMsg msg) {
    }

    @Override
    protected void afterAttachVxlanPoolFromClusterFailed(APIAttachL2NetworkToClusterMsg msg) {
    }

    @Override
    public void deleteHook(Completion completion) {
        /* delete all l2 network of this pool */
        List<String> vxlanUuids = Q.New(VxlanNetworkVO.class).eq(VxlanNetworkVO_.poolUuid, self.getUuid())
                .select(VxlanNetworkVO_.uuid).listValues();

        new While<>(vxlanUuids).all((uuid, wcomp) -> {
            DeleteL2NetworkMsg msg = new DeleteL2NetworkMsg();
            msg.setUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, uuid);
            bus.send(msg, new CloudBusCallBack(wcomp) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.info(String.format("delete hardware vxlan network[uuid:%s] failed, reason:%s", uuid, reply.getError().getDetails()));
                    }
                    wcomp.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });

    }
}
