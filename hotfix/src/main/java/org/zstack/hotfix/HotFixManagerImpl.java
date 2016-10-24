package org.zstack.hotfix;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by xing5 on 2016/10/25.
 */
public class HotFixManagerImpl extends AbstractService implements HotFixManager {
    private static final CLogger logger = Utils.getLogger(HotFixManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    public boolean start() {
        return true;
    }

    public boolean stop() {
        return true;
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIHotFix1169KvmSnapshotChainMsg) {
            handle((APIHotFix1169KvmSnapshotChainMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIHotFix1169KvmSnapshotChainMsg msg) {
        new HotFix1169(msg).fix();
    }

    public String getId() {
        return bus.makeLocalServiceId(HostFixConstant.SERVICE_ID);
    }
}
