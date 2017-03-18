package org.zstack.core.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.AbstractService;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.*;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.notification.ApiNotificationFactory;
import org.zstack.header.notification.ApiNotificationFactoryExtensionPoint;
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
    @Autowired
    private PluginRegistry plugRgty;

    private Map<Class, ApiNotificationFactory> apiNotificationFactories = new HashMap<>();

    private class ApiNotificationSender implements BeforeDeliveryMessageInterceptor, BeforePublishEventInterceptor {
        class Bundle {
            APIMessage message;
            ApiNotification notification;
        }

        ConcurrentHashMap<String, Bundle> apiMessages = new ConcurrentHashMap<>();
        Map<Class, Method> notificationMethods = new ConcurrentHashMap<>();

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

        private ApiNotification getApiNotification(APIMessage msg) throws InvocationTargetException, IllegalAccessException {
            Method m = notificationMethods.get(msg.getClass());
            if (m == null) {
                Class clz = msg.getClass().getSuperclass();
                while (clz != Object.class) {
                    m = notificationMethods.get(clz);
                    if (m != null) {
                        break;
                    }

                    clz = clz.getSuperclass();
                }

                notificationMethods.put(msg.getClass(), m);
            }

            ApiNotification notification = null;

            if (m != null) {
                notification = (ApiNotification) m.invoke(msg);
            } else {
                ApiNotificationFactory factory = apiNotificationFactories.get(msg.getClass());
                if (factory != null) {
                    notification = factory.createApiNotification(msg);
                }
            }

            return notification;
        }

        @Override
        @AsyncThread
        public void beforePublishEvent(Event evt) {
            if (!(evt instanceof APIEvent)) {
                return;
            }

            APIEvent aevt = (APIEvent) evt;
            Bundle b = apiMessages.get(aevt.getApiId());
            if (b == null) {
                return;
            }

            apiMessages.remove(aevt.getApiId());

            b.notification.after((APIEvent) evt);

            List<NotificationBuilder> lst = new ArrayList<>();
            for (ApiNotification.Inner inner : b.notification.getInners()) {
                Map opaque = new HashMap();
                opaque.put("session", b.message.getSession());
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

            if (!msg.getServiceId().endsWith(Platform.getManagementServerId())) {
                // a message to api portal
                return;
            }

            try {
                ApiNotification notification = getApiNotification((APIMessage) msg);

                if (notification == null) {
                    logger.warn(String.format("API message[%s] does not have an API notification method or the method returns null",
                            msg.getClass()));
                    return;
                }

                notification.before();

                Bundle b = new Bundle();
                b.message = (APIMessage) msg;
                b.notification = notification;
                apiMessages.put(msg.getId(), b);
            } catch (Throwable t) {
                logger.warn(String.format("unhandled exception %s", t.getMessage()), t);
            }
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

        for (ApiNotificationFactoryExtensionPoint ext : plugRgty.getExtensionList(ApiNotificationFactoryExtensionPoint.class)) {
            apiNotificationFactories.putAll(ext.apiNotificationFactory());
        }

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
