package org.zstack.query;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.query.*;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.SearchConstant;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.TypeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.argerr;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class QueryFacadeImpl extends AbstractService implements QueryFacade, GlobalApiMessageInterceptor {
    private static CLogger logger = Utils.getLogger(QueryFacadeImpl.class);
    private Map<String, QueryBuilderFactory> builerFactories = new HashMap<>();
    private String queryBuilderType = MysqlQueryBuilderFactory.type.toString();

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private void validateConditions(List<QueryCondition> conditions) {
        for (QueryCondition cond : conditions) {
            // will throw out IllegalArgumentException if op is invalid
            QueryOp.valueOf(cond.getOp());
        }
    }

    @Override
    public <T> List<T> query(APIQueryMessage msg, Class<T> inventoryClass) {
        validateConditions(msg.getConditions());

        QueryBuilderFactory factory = getFactory(queryBuilderType);
        QueryBuilder builder = factory.createQueryBuilder();
        return builder.query(msg, inventoryClass);
    }

    @Override
    public long count(APIQueryMessage msg, Class inventoryClass) {
        validateConditions(msg.getConditions());

        QueryBuilderFactory factory = getFactory(queryBuilderType);
        QueryBuilder builder = factory.createQueryBuilder();
        return builder.count(msg, inventoryClass);
    }

    private void populateExtensions() {
        for (QueryBuilderFactory extp : pluginRgty.getExtensionList(QueryBuilderFactory.class)) {
            QueryBuilderFactory old = builerFactories.get(extp.getQueryBuilderType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate QueryBuilderFactory[%s, %s] for type[%s]",
                        extp.getClass().getName(), old.getClass().getName(), extp.getQueryBuilderType()));
            }
            builerFactories.put(extp.getQueryBuilderType().toString(), extp);
        }
    }

    private QueryBuilderFactory getFactory(String type) {
        QueryBuilderFactory factory = builerFactories.get(type);
        if (factory == null) {
            throw new CloudRuntimeException(String.format("unable to find QueryBuilderFactory with type[%s]", type));
        }
        return factory;
    }

    private void checkBoxTypeInInventory() {
        if (!CoreGlobalProperty.CHECK_BOX_TYPE_IN_INVENTORY) {
            return;
        }

        List<Class> inventoryClasses = BeanUtils.scanClass("org.zstack", Inventory.class);
        List<String> errors = new ArrayList<>();
        for (Class clz : inventoryClasses) {
            boolean error = false;
            StringBuilder sb = new StringBuilder(String.format("inventory class[%s] contains below primitive fields:",
                    clz.getName()));
            for (Field f : FieldUtils.getAllFields(clz)) {
                if (f.isAnnotationPresent(APINoSee.class)) {
                    continue;
                }

                if (TypeUtils.isPrimitiveType(f.getType())) {
                    error = true;
                    sb.append(String.format("\n%s[%s]", f.getName(), f.getType().getName()));
                }
            }

            if (error) {
                errors.add(sb.toString());
            }
        }

        if (!errors.isEmpty()) {
            throw new CloudRuntimeException(String.format("detected some inventory class using primitive type." +
                    " Please change those primitive type field to corresponding box type:\n %s",
                    StringUtils.join(errors, "\n\n")));
        }
    }

    @Override
    public boolean start() {
        checkBoxTypeInInventory();
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public void setQueryBuilderType(String queryBuilderType) {
        this.queryBuilderType = queryBuilderType;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIGenerateInventoryQueryDetailsMsg) {
            handle((APIGenerateInventoryQueryDetailsMsg) msg);
        } else if (msg instanceof APIQueryMessage) {
            handle((APIQueryMessage) msg);
        } else if (msg instanceof APIGenerateQueryableFieldsMsg) {
            handle((APIGenerateQueryableFieldsMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGenerateQueryableFieldsMsg msg) {
        QueryBuilderFactory factory = getFactory(queryBuilderType);
        QueryBuilder builder = factory.createQueryBuilder();
        Map<String, List<String>> ret = builder.populateQueryableFields();

        if (APIGenerateQueryableFieldsMsg.PYTHON_FORMAT.equals(msg.getFormat())) {
            QueryableFieldsPythonWriter writer = new QueryableFieldsPythonWriter(msg.getOutputFolder(), ret);
            writer.write();
        } else {
            throw new CloudRuntimeException(String.format("unknown mediaType[%s]", msg.getFormat()));
        }

        APIGenerateQueryableFieldsEvent evt = new APIGenerateQueryableFieldsEvent(msg.getId());
        bus.publish(evt);
    }

    private Map<Class, Method> replySetter = new HashMap<>();
    private Map<Class, AutoQuery> autoQueryMap = new HashMap<>();

    private void handle(APIQueryMessage msg) {
        AutoQuery at = autoQueryMap.get(msg.getClass());
        if (at == null) {
            at = msg.getClass().getAnnotation(AutoQuery.class);
            if (at == null) {
                throw new OperationFailureException(errf.stringToInternalError(
                        String.format("message[%s] is not annotated by @AutoQuery", msg.getClass())
                ));
            }
            autoQueryMap.put(msg.getClass(), at);
        }

        Class replyClass = at.replyClass();
        Class inventoryClass = at.inventoryClass();
        try {
            APIQueryReply reply = (APIQueryReply) replyClass.newInstance();
            Method setter = replySetter.get(inventoryClass);
            if (setter == null) {
                setter = replyClass.getDeclaredMethod("setInventories", List.class);
                if (setter == null) {
                    throw new OperationFailureException(errf.stringToInternalError(
                            String.format("query reply[%s] has no method setInventories()", replyClass.getName())
                    ));
                }
                setter.setAccessible(true);
                replySetter.put(inventoryClass, setter);
            }

            if (msg.isCount()) {
                long count = count(msg, inventoryClass);
                reply.setTotal(count);
                bus.reply(msg, reply);
            } else {
                List invs = query(msg, inventoryClass);
                setter.invoke(reply, invs);
                //TODO: merge this into mysql query builder
                if (msg.isReplyWithCount()) {
                    long count = count(msg, inventoryClass);
                    reply.setTotal(count);
                }
                bus.reply(msg, reply);
            }
        } catch (OperationFailureException of) {
            throw of;
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            throw new OperationFailureException(errf.throwableToInternalError(e));
        }
    }

    private void handle(APIGenerateInventoryQueryDetailsMsg msg) {
        InventoryQueryDetailsGenerator.generate(msg.getOutputDir(), msg.getBasePackageNames());
        APIGenerateInventoryQueryDetailsEvent evt = new APIGenerateInventoryQueryDetailsEvent(msg.getId());
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SearchConstant.QUERY_FACADE_SERVICE_ID);
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APIQueryMessage.class);
        return ret;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        APIQueryMessage qmsg = (APIQueryMessage) msg;
        for (QueryCondition cond : qmsg.getConditions()) {
            try {
                QueryOp.valueOf(cond.getOp());
            } catch (IllegalArgumentException e) {
                throw new ApiMessageInterceptionException(errf.throwableToInvalidArgumentError(e));
            }

            if (!QueryOp.NOT_NULL.equals(cond.getOp()) && !QueryOp.IS_NULL.equals(cond.getOp()) && cond.getValue() == null) {
                throw new ApiMessageInterceptionException(argerr("'value' of query condition %s cannot be null",
                                JSONObjectUtil.toJsonString(cond)));
            }
        }

        return msg;
    }
}
