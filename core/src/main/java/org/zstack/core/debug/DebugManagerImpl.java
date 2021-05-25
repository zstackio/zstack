package org.zstack.core.debug;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.progress.ChainInfo;
import org.zstack.header.core.progress.TaskInfo;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/7/25.
 */
public class DebugManagerImpl extends AbstractService implements DebugManager {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    String HEADER_CORRELATION_ID = "correlationId";
    String HEADERS = "headers";
    private static final CLogger logger = Utils.getLogger(DebugManagerImpl.class);

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
        } else if (msg instanceof APIGetDebugSignalMsg) {
            handle((APIGetDebugSignalMsg) msg);
        } else if (msg instanceof APICleanQueueMsg) {
            handle((APICleanQueueMsg) msg);
        }
    }

    private void handle(APICleanQueueMsg msg) {
        APICleanQueueEvent evt = new APICleanQueueEvent(msg.getId());
        ChainInfo taskInfo = thdf.cleanChainTaskInfo(msg.getSignatureName(), msg.getTaskIndex(), msg.getCleanUp(), msg.getRunningTask());
        if (taskInfo == null) {
            evt.setSuccess(false);
            evt.setError(operr("taskInfo was not found"));
            bus.publish(evt);
            return;
        }
        replyAllMsgFromTasks(taskInfo.getPendingTask());
        replyAllMsgFromTasks(taskInfo.getRunningTask());
        bus.publish(evt);
    }

    private void replyAllMsgFromTasks(List Tasks) {
        if (!Tasks.isEmpty()) {
            Tasks.forEach(task -> {
                bus.reply(getCorrelationIdFromTask(((TaskInfo) task).getContext()));
            });
        }
    }

    private String getCorrelationIdFromTask(String TaskContext) {
        String msgContext = TaskContext.substring(TaskContext.indexOf(":") + 1);
        logger.debug(String.format("msgContext: %s", msgContext));
        Map msgMap = JSONObjectUtil.toObject(msgContext, LinkedHashMap.class);
        Map headers = (Map) msgMap.get(HEADERS);
        return (String) headers.get(HEADER_CORRELATION_ID);
    }

    private void handle(APIGetDebugSignalMsg msg) {
        APIGetDebugSignalReply reply = new APIGetDebugSignalReply();
        reply.setSignals(getDebugSignals());
        bus.reply(msg, reply);
    }

    private void handle(APIDebugSignalMsg msg) {
        APIDebugSignalEvent evt = new APIDebugSignalEvent(msg.getId());
        for (String sig : msg.getSignals()) {
            handleSig(sig);
        }

        bus.publish(evt);
    }

    @Override
    public void handleSig(String sig) {
        List<DebugSignalHandler> hs = sigHandlers.get(sig);
        if (hs == null) {
            return;
        }

        for (DebugSignalHandler h : hs) {
            h.handleDebugSignal();
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(DebugConstant.SERVICE_ID);
    }
}
