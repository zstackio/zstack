package org.zstack.core.debug;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.header.AbstractService;
import org.zstack.header.message.Message;

import java.util.List;

/**
 * Created by xing5 on 2016/7/25.
 */
public class DebugManagerImpl extends AbstractService implements DebugManager {
    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIDebugSignalMsg) {
            handle((APIDebugSignalMsg)msg);
        }
    }

    private void handle(APIDebugSignalMsg msg) {
        APIDebugSignalEvent evt = new APIDebugSignalEvent(msg.getId());
        for (String sig : msg.getSignals()) {
            DebugSignal dsig = DebugSignal.valueOf(sig);
            List<DebugSignalHandler> hs = sigHandlers.get(dsig);
            if (hs == null) {
                continue;
            }

            for (DebugSignalHandler h : hs) {
                h.handleDebugSignal(dsig);
            }
        }

        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(DebugConstant.SERVICE_ID);
    }
}
