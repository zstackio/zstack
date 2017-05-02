package org.zstack.core.notification;

import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.*;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.notification.ApiNotificationFactory;
import org.zstack.header.notification.ApiNotificationFactoryExtensionPoint;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.argerr;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private RESTFacade restf;

    private Map<Class, ApiNotificationFactory> apiNotificationFactories = new HashMap<>();

    private BlockingQueue<NotificationBuilder> notificationsQueue = new LinkedBlockingQueue<>();
    private NotificationBuilder quitToken = new NotificationBuilder();
    private boolean exitQueue = false;

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

                if (m != null) {
                    notificationMethods.put(msg.getClass(), m);
                }
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
                        .resource(inner.getResourceUuid(), inner.getResourceType())
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
            send(builder);
        }
    }

    void send(NotificationBuilder builder) {
        try {
            notificationsQueue.offer(builder, 60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn(String.format("unable to write log %s", JSONObjectUtil.toJsonString(builder)), e);
        }
    }

    @Override
    public boolean start() {
        bus.installBeforeDeliveryMessageInterceptor(apiNotificationSender);
        bus.installBeforePublishEventInterceptor(apiNotificationSender);

        for (ApiNotificationFactoryExtensionPoint ext : plugRgty.getExtensionList(ApiNotificationFactoryExtensionPoint.class)) {
            apiNotificationFactories.putAll(ext.apiNotificationFactory());
        }


        NotificationGlobalConfig.WEBHOOK_URL.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                if (newValue == null || "null".equals(newValue)) {
                    return;
                }


                if (!new UrlValidator().isValid(newValue)) {
                    throw new OperationFailureException(argerr("%s is not a valid URL", newValue));
                }
            }
        });

        thdf.submit(new Task<Void>() {
            @Override
            public Void call() throws Exception {
                writeNotificationsToDb();
                return null;
            }

            @Override
            public String getName() {
                return "notification-thread";
            }
        });

        return true;
    }

    @ExceptionSafe
    private void writeNotificationsToDb() throws InterruptedException {
        while (!exitQueue) {
            List<NotificationBuilder> lst = new ArrayList<>();
            lst.add(notificationsQueue.take());
            notificationsQueue.drainTo(lst);

            try {
                List<NotificationInventory> invs = new SQLBatchWithReturn<List<NotificationInventory>>() {
                    @Override
                    protected List<NotificationInventory> scripts() {
                        List<NotificationInventory> invs = new ArrayList<>();

                        for (NotificationBuilder builder : lst) {
                            if (builder == quitToken) {
                                exitQueue = true;
                                continue;
                            }

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

                            dbf.getEntityManager().persist(vo);

                            invs.add(NotificationInventory.valueOf(vo));
                        }

                        return invs;
                    }
                }.execute();


                if (NotificationGlobalConfig.WEBHOOK_URL.value() != null && !NotificationGlobalConfig.WEBHOOK_URL.value().equals("null")) {
                    callWebhook(invs);
                }
            } catch (Throwable t) {
                logger.warn(String.format("failed to persists notifications:\n %s", JSONObjectUtil.toJsonString(lst)), t);
            }
        }
    }

    @AsyncThread
    private void callWebhook(List<NotificationInventory> lst) {
        restf.getRESTTemplate().postForEntity(NotificationGlobalConfig.WEBHOOK_URL.value(), JSONObjectUtil.toJsonString(lst), String.class);
    }

    @Override
    public boolean stop() {
        notificationsQueue.offer(quitToken);

        return true;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateNotificationsStatusMsg) {
            handle((APIUpdateNotificationsStatusMsg) msg);
        } else if (msg instanceof APIDeleteNotificationsMsg) {
            handle((APIDeleteNotificationsMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIDeleteNotificationsMsg msg) {
        APIDeleteNotificationsEvent evt = new APIDeleteNotificationsEvent(msg.getId());
        SQL.New(NotificationVO.class).in(NotificationVO_.uuid, msg.getUuids()).delete();
        bus.publish(evt);
    }

    private void handle(APIUpdateNotificationsStatusMsg msg) {
        APIUpdateNotificationsStatusEvent evt = new APIUpdateNotificationsStatusEvent(msg.getId());

        SQL.New(NotificationVO.class).set(NotificationVO_.status, NotificationStatus.valueOf(msg.getStatus()))
                .in(NotificationVO_.uuid, msg.getUuids()).update();

        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(NotificationConstant.SERVICE_ID);
    }

}
