package org.zstack.test.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudBusAopProxy {
    private static final CLogger logger = Utils.getLogger(CloudBusAopProxy.class);

    public static enum Behavior {
        FAIL,
        TIMEOUT,
    }

    public static final String MESSAGE_ORIGINAL_SERVICE_ID = "OriginalServiceId";
    public static final String MESSAGE_BEHAVIOR = "behavior";

    private Map<Class<? extends Message>, Behavior> messages = new HashMap<Class<? extends Message>, Behavior>();

    public void addMessage(Class<? extends Message> clazz, Behavior bh) {
        messages.put(clazz, bh);
    }

    public void removeMessage(Class<? extends Message> clazz) {
        messages.remove(clazz);
    }

    @SuppressWarnings("unused")
    private Object singleMessageAdvice(ProceedingJoinPoint pjp, Message msg) throws Throwable {
        Behavior bh = messages.get(msg.getClass());
        if (bh == null) {
            return pjp.proceed(new Object[]{msg});
        }

        msg.putHeaderEntry(MESSAGE_ORIGINAL_SERVICE_ID, msg.getServiceId());
        msg.putHeaderEntry(MESSAGE_BEHAVIOR, bh.toString());
        msg.setServiceId(ManInTheMiddleService.SERVICE_ID);
        return pjp.proceed(new Object[]{msg});
    }

    @SuppressWarnings("unused")
    private Object singleCallbackMessageAdvice(ProceedingJoinPoint pjp, Message msg, CloudBusCallBack callback) throws Throwable {
        Behavior bh = messages.get(msg.getClass());
        if (bh == null) {
            return pjp.proceed(new Object[]{msg, callback});
        }

        msg.putHeaderEntry(MESSAGE_ORIGINAL_SERVICE_ID, msg.getServiceId());
        msg.putHeaderEntry(MESSAGE_BEHAVIOR, bh.toString());
        msg.setServiceId(ManInTheMiddleService.SERVICE_ID);
        return pjp.proceed(new Object[]{msg, callback});
    }

    @SuppressWarnings("unused")
    private <T extends Message> Object listMessageAdvice(ProceedingJoinPoint pjp, List<T> msgs) throws Throwable {
        for (Message msg : msgs) {
            Behavior bh = messages.get(msg.getClass());
            if (bh == null) {
                logger.warn(String.format("Cannot find behavior for message[%s], however, it's in a message list sent out all in one call, that means you may forget specifying behavior of this message", msg.getMessageName()));
                continue;
            }

            msg.putHeaderEntry(MESSAGE_ORIGINAL_SERVICE_ID, msg.getServiceId());
            msg.putHeaderEntry(MESSAGE_BEHAVIOR, bh.toString());
            msg.setServiceId(ManInTheMiddleService.SERVICE_ID);
        }
        return pjp.proceed(new Object[]{msgs});
    }
}
