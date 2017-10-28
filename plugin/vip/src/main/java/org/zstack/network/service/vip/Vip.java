package org.zstack.network.service.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class Vip {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    private String uuid;

    public Vip(String uuid) {
        this.uuid = uuid;
    }
    protected static final CLogger logger = Utils.getLogger(VipBase.class);

    ModifyVipAttributesStruct struct;

    public ModifyVipAttributesStruct getStruct() {
        return struct;
    }

    public void setStruct(ModifyVipAttributesStruct struct) {
        this.struct = struct;
    }

    public void acquire(Completion completion) {
        AcquireVipMsg msg = new AcquireVipMsg();
        msg.setVipUuid(uuid);
        msg.setStruct(struct);

        bus.makeTargetServiceIdByResourceUuid(msg, VipConstant.SERVICE_ID, uuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(reply.getError());
                }

                completion.success();
            }
        });
    }

    public void release(Completion completion) {
        ReleaseVipMsg msg = new ReleaseVipMsg();
        msg.setVipUuid(uuid);
        msg.setStruct(struct);

        bus.makeTargetServiceIdByResourceUuid(msg, VipConstant.SERVICE_ID, uuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(reply.getError());
                }

                completion.success();
            }
        });
    }
}
