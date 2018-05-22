package org.zstack.query;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.SyncThread;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.thread.ThreadGlobalProperty;
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
import org.zstack.zql.ZQL;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ZQLQueryResult;
import org.zstack.zql.ast.ZQLMetadata;

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
    @Autowired
    private ThreadFacade thdf;

    public static final String USER_TAG = "__userTag__";
    public static final String SYSTEM_TAG = "__systemTag__";

    private void validateConditions(List<QueryCondition> conditions) {
        for (QueryCondition cond : conditions) {
            // will throw out IllegalArgumentException if op is invalid
            QueryOp.valueOf(cond.getOp());
        }
    }

    @Override
    public <T> List<T> query(APIQueryMessage msg, Class<T> inventoryClass) {
        validateConditions(msg.getConditions());
        ZQLQueryResult result = queryUseZQL(msg, inventoryClass);
        return result.inventories;
    }

    @Override
    public long count(APIQueryMessage msg, Class inventoryClass) {
        validateConditions(msg.getConditions());
        ZQLQueryResult result = queryUseZQL(msg, inventoryClass);
        return result.total;
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

        Set<Class<?>> inventoryClasses = BeanUtils.reflections.getTypesAnnotatedWith(Inventory.class);
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
        } else if (msg instanceof APIBatchQueryMsg) {
            handle((APIBatchQueryMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIBatchQueryMsg msg) {
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public Void call() {
                APIBatchQueryReply reply = new APIBatchQueryReply();
                reply.setResult(new BatchQuery().query(msg));
                bus.reply(msg, reply);
                return null;
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public String getSyncSignature() {
                return "batch-query";
            }

            @Override
            public int getSyncLevel() {
                return ThreadGlobalProperty.MAX_THREAD_NUM / 2;
            }
        });
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

    private Method getReplySetter(AutoQuery at) {
        Class replyClass = at.replyClass();
        Class inventoryClass = at.inventoryClass();
        try {
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

            return setter;
        } catch (OperationFailureException of) {
            throw of;
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            throw new OperationFailureException(errf.throwableToInternalError(e));
        }
    }

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
            APIQueryReply reply = (APIQueryReply) replyClass.getConstructor().newInstance();
            Method replySetter = getReplySetter(at);
            ZQLQueryResult result = queryUseZQL(msg, inventoryClass);
            if (result.total != null) {
                reply.setTotal(result.total);
            }
            if (result.inventories != null) {
                replySetter.invoke(reply, result.inventories);
            }
            bus.reply(msg, reply);
        } catch (OperationFailureException of) {
            throw of;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

            /*
        try {
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
            */
    }

    private String toZQLConditionString(QueryCondition c) {
        // make every condition value as string, the ZQL
        // will put them in right type because it knows every field's type

        QueryOp op = QueryOp.valueOf(c.getOp());
        if (c.getValue() == null) {
            return String.format("%s %s", c.getName(), c.getOp());
        } else if (op == QueryOp.IN || op == QueryOp.NOT_IN) {
            List<String> values = new ArrayList<>();
            for (String v : c.getValue().split(",")) {
                values.add(String.format("'%s'", v));
            }

            if (values.isEmpty()) {
                // use '' to represent an empty collection
                values.add("''");
            }

            return String.format("%s %s (%s)", c.getName(), c.getOp(), StringUtils.join(values, ","));
        } else {
            return String.format("%s %s '%s'", c.getName(), c.getOp(), c.getValue());
        }
    }

    private Class getQueryTargetInventoryClass(APIQueryMessage msg, Class inventoryClass) {
        Field tf = ZQLMetadata.getTypeFieldOfInventoryClass(inventoryClass);
        if (tf == null) {
            return inventoryClass;
        }

        Optional<QueryCondition> opt = msg.getConditions().stream().filter(c->c.getName().equals(tf.getName())).findFirst();
        if (!opt.isPresent()) {
            return inventoryClass;
        }

        QueryCondition condition = opt.get();
        return ZQLMetadata.getChildInventoryClassByType(inventoryClass, condition.getValue());
    }

    public ZQLQueryResult queryUseZQL(APIQueryMessage msg, Class inventoryClass) {
        List<String> sb = new ArrayList<>();
        sb.add(msg.isCount() ? "count" : "query");
        Class targetInventoryClass = getQueryTargetInventoryClass(msg, inventoryClass);
        sb.add(msg.getFields() == null || msg.getFields().isEmpty() ? ZQL.queryTargetNameFromInventoryClass(targetInventoryClass) : ZQL.queryTargetNameFromInventoryClass(targetInventoryClass) + "." + StringUtils.join(msg.getFields(), ","));

        List<QueryCondition> tagConditions = new ArrayList<>();

        if (msg.getConditions() != null && !msg.getConditions().isEmpty()) {
            List<String> conds = new ArrayList<>();
            msg.getConditions().forEach(c -> {
                if (c.getName().equals(SYSTEM_TAG) || c.getName().equals(USER_TAG)) {
                    tagConditions.add(c);
                    return;
                }

                conds.add(toZQLConditionString(c));
            });

            if (!conds.isEmpty()) {
                sb.add("where");
                sb.add(StringUtils.join(conds, " and "));
            }
        }

        if (!tagConditions.isEmpty()) {
            List<String> byConds = new ArrayList<>();
            tagConditions.forEach(c -> byConds.add(toZQLConditionString(c)));
            sb.add(String.format("restrict by (%s)", StringUtils.join(byConds, ",")));
        }

        if (!msg.isCount() && msg.isReplyWithCount()) {
            sb.add("return with (total)");
        }

        if (msg.getSortBy() != null) {
            sb.add(String.format("order by %s %s", msg.getSortBy(), msg.getSortDirection()));
        }

        if (msg.getLimit() != null) {
            sb.add(String.format("limit %s", msg.getLimit()));
        }

        if (msg.getStart() != null) {
            sb.add(String.format("offset %s", msg.getStart()));
        }

        ZQLContext.putAPISession(msg.getSession());
        String text = StringUtils.join(sb, " ");
        if (logger.isTraceEnabled()) {
            logger.trace(text);
        }
        ZQL zql = ZQL.fromString(text);
        ZQLQueryResult result = zql.execute();
        ZQLContext.cleanAPISession();
        return result;
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
