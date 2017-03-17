package org.zstack.core.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.AbstractService;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.*;
import org.zstack.header.notification.ApiNotification;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xing5 on 2017/3/15.
 */
public class NotificationManager extends AbstractService {
    private CLogger logger = Utils.getLogger(getClass());

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private class ApiNotificationSender implements BeforeDeliveryMessageInterceptor, BeforePublishEventInterceptor {
        ConcurrentHashMap<String, APIMessage> apiMessages = new ConcurrentHashMap<>();
        Map<Class, Method> notificationMethods = new HashMap<>();

        public ApiNotificationSender() {
            Set<Method> methods = Platform.getReflections().getMethodsReturn(ApiNotification.class);
            for (Method m : methods) {
                notificationMethods.put(m.getDeclaringClass(), m);
            }
        }

        @Override
        public int orderOfBeforePublishEventInterceptor() {
            return 0;
        }

        private Method getApiNotificationMethod(APIMessage msg) {
            Method m = notificationMethods.get(msg.getClass());
            if (m != null) {
                return m;
            }

            Class clz = msg.getClass().getSuperclass();
            while (clz != Object.class) {
                m = notificationMethods.get(clz);
                if (m != null) {
                    return m;
                }

                clz = clz.getSuperclass();
            }

            return null;
        }

        @Override
        @AsyncThread
        public void beforePublishEvent(Event evt) {
            if (!(evt instanceof APIEvent)) {
                return;
            }

            APIEvent aevt = (APIEvent) evt;
            APIMessage msg = apiMessages.get(aevt.getApiId());
            apiMessages.remove(msg.getId());

            Method m = getApiNotificationMethod(msg);
            if (m == null) {
                logger.warn(String.format("API message[%s] does not define notification method", msg.getClass()));
                return;
            }

            try {
                ApiNotification notification = (ApiNotification) m.invoke(msg, aevt);
                if (notification == null) {
                    logger.warn(String.format("API message[%s]'s notification method[%s] returns null",
                            msg.getClass(), m.getName()));
                    return;
                }

                notification.makeNotifications();
                List<NotificationBuilder> lst = new ArrayList<>();
                for (ApiNotification.Inner inner : notification.getInners()) {
                    Map opaque = new HashMap();
                    opaque.put("success", aevt.isSuccess());
                    if (!aevt.isSuccess()) {
                        opaque.put("error", aevt.getError());
                    }

                    lst.add(new NotificationBuilder()
                            .content(inner.getContent())
                            .arguments(inner.getArguments())
                            .name(NotificationConstant.API_SENDER)
                            .sender(NotificationConstant.API_SENDER)
                            .resource(inner.getResourceUuid(), inner.getResourceUuid())
                            .opaque(opaque));
                }

                send(lst);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new CloudRuntimeException(e);
            }
        }

        @Override
        public int orderOfBeforeDeliveryMessageInterceptor() {
            return 0;
        }

        @Override
        public void intercept(Message msg) {
            if (!(msg instanceof APIMessage)) {
                return;
            }

            if (msg instanceof APISyncCallMessage) {
                return;
            }

            apiMessages.put(msg.getId(), (APIMessage) msg);
        }
    }

    private ApiNotificationSender apiNotificationSender = new ApiNotificationSender();

    void send(List<NotificationBuilder> builders) {
        for (NotificationBuilder builder : builders) {
            NotificationVO vo = new NotificationVO();
            vo.setName(builder.notificationName);
            vo.setArguments(JSONObjectUtil.toJsonString(builder.arguments));
            vo.setContent(builder.content);
            vo.setResourceType(builder.resourceType);
            vo.setResourceUuid(builder.resourceUuid);
            vo.setSender(builder.sender);
            vo.setStatus(NotificationStatus.Unread);
            vo.setType(builder.type);
            vo.setUuid(Platform.getUuid());
            vo.setTime(System.currentTimeMillis());
            if (builder.opaque != null) {
                vo.setOpaque(JSONObjectUtil.toJsonString(builder.opaque));
            }

            vo = dbf.persistAndRefresh(vo);
        }
    }

    void send(NotificationBuilder builder) {
        NotificationVO vo = new NotificationVO();
        vo.setName(builder.notificationName);
        vo.setArguments(JSONObjectUtil.toJsonString(builder.arguments));
        vo.setContent(builder.content);
        vo.setResourceType(builder.resourceType);
        vo.setResourceUuid(builder.resourceUuid);
        vo.setSender(builder.sender);
        vo.setStatus(NotificationStatus.Unread);
        vo.setType(builder.type);
        vo.setUuid(Platform.getUuid());
        vo.setTime(System.currentTimeMillis());
        vo = dbf.persistAndRefresh(vo);

        //TODO: send to bus
    }


    @Override
    public boolean start() {
        bus.installBeforeDeliveryMessageInterceptor(apiNotificationSender);
        bus.installBeforePublishEventInterceptor(apiNotificationSender);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(NotificationConstant.SERVICE_ID);
    }

}
