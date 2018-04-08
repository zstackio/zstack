package org.zstack.query

import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.control.CompilerConfiguration
import org.kohsuke.groovy.sandbox.GroovyInterceptor
import org.kohsuke.groovy.sandbox.SandboxTransformer
import org.kohsuke.groovy.sandbox.impl.Super
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.Action
import org.zstack.header.identity.SessionInventory
import org.zstack.header.identity.SuppressCredentialCheck
import org.zstack.header.message.APIResponse
import org.zstack.header.message.APISyncCallMessage
import org.zstack.header.message.MessageReply
import org.zstack.header.query.APIQueryMessage
import org.zstack.header.query.AutoQuery
import org.zstack.header.query.QueryCondition
import org.zstack.header.query.QueryOp
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger
import org.zstack.zql.ZQLQueryResult

import java.lang.reflect.Modifier
import java.util.regex.Pattern

class BatchQuery {
    private static CLogger logger = Utils.getLogger(BatchQuery.class)

    private QueryFacade queryf
    private SessionInventory session
    private CloudBus bus

    static class SandBox extends GroovyInterceptor {
        static List<Class> RECEIVER_WHITE_LIST = [
                Number[].class,
                Number.class,
                long[].class,
                long.class,
                int[].class,
                int.class,
                short[].class,
                short.class,
                double[].class,
                double.class,
                float[].class,
                float.class,
                String[].class,
                String.class,
                Date[].class,
                Date.class,
                Map.class,
                Collection.class,
                Script.class,
                Enum[].class,
                Enum.class
        ]

        static void checkReceiver(Object obj) {
            checkReceiver(obj.getClass())
        }

        static void checkReceiver(Class clz) {
            for (Class wclz : RECEIVER_WHITE_LIST) {
                if (wclz.isAssignableFrom(clz)) {
                    return
                }
            }

            throw new Exception("invalid operation on class[${clz.name}]")
        }

        static void checkMethod(String method) {
            if (method == "sleep") {
                throw new Exception("invalid operation[${method}]")
            }
        }

        Object onMethodCall(GroovyInterceptor.Invoker invoker, Object receiver, String method, Object... args) throws Throwable {
            checkReceiver(receiver)
            checkMethod(method)
            return super.onMethodCall(invoker, receiver, method, args)
        }

        Object onStaticCall(GroovyInterceptor.Invoker invoker, Class receiver, String method, Object... args) throws Throwable {
            checkReceiver(receiver)
            checkMethod(method)
            return super.onStaticCall(invoker, receiver, method, args)
        }

        Object onNewInstance(GroovyInterceptor.Invoker invoker, Class receiver, Object... args) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(receiver, (String)null, (Object[])args);
        }

        Object onSuperCall(GroovyInterceptor.Invoker invoker, Class senderType, Object receiver, String method, Object... args) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(new Super(senderType, receiver), method, (Object[])args);
        }

        void onSuperConstructor(GroovyInterceptor.Invoker invoker, Class receiver, Object... args) throws Throwable {
            checkReceiver(receiver)
            this.onNewInstance(invoker, receiver, args);
        }

        Object onGetProperty(GroovyInterceptor.Invoker invoker, Object receiver, String property) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(receiver, property);
        }

        Object onSetProperty(GroovyInterceptor.Invoker invoker, Object receiver, String property, Object value) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(receiver, property, value);
        }

        Object onGetAttribute(GroovyInterceptor.Invoker invoker, Object receiver, String attribute) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(receiver, attribute);
        }

        Object onSetAttribute(GroovyInterceptor.Invoker invoker, Object receiver, String attribute, Object value) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(receiver, attribute, value);
        }

        Object onGetArray(GroovyInterceptor.Invoker invoker, Object receiver, Object index) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(receiver, (String)null, (Object)index);
        }

        Object onSetArray(GroovyInterceptor.Invoker invoker, Object receiver, Object index, Object value) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(receiver, (String)null, index, value);
        }
    }

    BatchQuery() {
        this.queryf = Platform.getComponentLoader().getComponent(QueryFacade.class)
        this.bus = Platform.getComponentLoader().getComponent(CloudBus.class)
    }
    private static SandBox sandbox = new SandBox()

    static Map<String, Class> queryMessageClass = [:]
    static LinkedHashMap<String, String> QUERY_OP_MAPPING = [:]

    static String lstrip(String s, String remove) {
        return StringUtils.removeStart(s, remove)
    }

    static String rstrip(String s, String remove) {
        return StringUtils.removeEnd(s, remove)
    }

    static {
        Platform.reflections.getSubTypesOf(APISyncCallMessage.class).each { clz ->
            if (Modifier.isAbstract(clz.modifiers)) {
                return
            }

            SuppressCredentialCheck at = clz.getAnnotation(SuppressCredentialCheck.class)
            if (at != null && !at.supportBacthQuery()) {
                return
            }

            String name = lstrip(rstrip(clz.simpleName, "Msg"), "API")
            queryMessageClass[name.toLowerCase()] = clz
            queryMessageClass[clz.name] = clz
        }

        // order is important, don't change it
        QUERY_OP_MAPPING.put("=null", QueryOp.IS_NULL.toString())
        QUERY_OP_MAPPING.put("!=null", QueryOp.NOT_NULL.toString())
        QUERY_OP_MAPPING.put("!=", QueryOp.NOT_EQ.toString())
        QUERY_OP_MAPPING.put(">=", QueryOp.GT_AND_EQ.toString())
        QUERY_OP_MAPPING.put("<=", QueryOp.LT_AND_EQ.toString())
        QUERY_OP_MAPPING.put("!?=", QueryOp.NOT_IN.toString())
        QUERY_OP_MAPPING.put("!~=", QueryOp.NOT_LIKE.toString())
        QUERY_OP_MAPPING.put("~=", QueryOp.LIKE.toString())
        QUERY_OP_MAPPING.put("?=", QueryOp.IN.toString())
        QUERY_OP_MAPPING.put("=", QueryOp.EQ.toString())
        QUERY_OP_MAPPING.put(">", QueryOp.GT.toString())
        QUERY_OP_MAPPING.put("<", QueryOp.LT.toString())
    }

    private Map syncApiCall(String apiname, String jstr) {
        Class msgClz = queryMessageClass[apiname]
        if (msgClz == null) {
            throw new OperationFailureException(Platform.argerr("no query API found for %s", apiname))
        }

        APISyncCallMessage msg = JSONObjectUtil.toObject(jstr, msgClz)
        msg.setSession(session)
        msg.setServiceId("api.portal")
        MessageReply reply = bus.call(msg)
        if (!reply.isSuccess()) {
            throw new OperationFailureException(reply.error)
        }

        APIResponse rsp = reply as APIResponse
        return ["result":rsp.toResponseMap(rsp)]
    }

    private Map doQuery(String qstr) {
        List<String> words = qstr.split(" ")
        words = words.findAll { !it.isEmpty() }
        if (words.isEmpty()) {
            throw new OperationFailureException(Platform.argerr("invalid query string: %s", qstr))
        }

        String api = words[0].toLowerCase()
        Class msgClz = queryMessageClass[api]
        if (msgClz == null) {
            throw new OperationFailureException(Platform.argerr("no query API found for %s", words[0]))
        }

        APIQueryMessage msg = msgClz.newInstance() as APIQueryMessage
        msg.setSession(session)
        if (AccountConstant.INITIAL_SYSTEM_ADMIN_UUID != msg.session.accountUuid && !msgClz.isAnnotationPresent(Action.class)) {
            // the resource is owned by admin and the account is a normal account
            //TODO: fix hard code check admin query
            return ["total": 0, "result": []]
        }

        msg.setConditions([])
        boolean count = false
        boolean replyWithCount = false

        AutoQuery at = msg.getClass().getAnnotation(AutoQuery.class)
        if (at == null) {
            throw new CloudRuntimeException("class[${msg.getClass().name}] is not annotated by AutoQuery")
        }

        Class inventoryClass = at.inventoryClass()

        if (words.size() > 1) {
            words[1..words.size() - 1].each { String word ->
                if (word.startsWith("fields=")) {
                    def values = lstrip(word, "fields=")
                    msg["fields"] = values.split(",")
                } else if (word.startsWith("limit=")) {
                    def value = lstrip(word, "limit=")
                    msg["limit"] = Integer.valueOf(value)
                } else if (word.startsWith("start=")) {
                    def value = lstrip(word, "start=")
                    msg["start"] = Integer.valueOf(value)
                } else if (word.startsWith("sortBy=")) {
                    def value = lstrip(word, "sortBy=")
                    msg["sortBy"] = value
                } else if (word.startsWith("sortDirection=")) {
                    def value = lstrip(word, "sortDirection=")
                    msg["sortDirection"] = value
                } else if (word.startsWith("count=")) {
                    def value = lstrip(word, "count=")
                    count = Boolean.valueOf(value)
                } else if (word.startsWith("replyWithCount=")) {
                    def value = lstrip(word, "replyWithCount=")
                    replyWithCount = Boolean.valueOf(value)
                } else {
                    String OP = null
                    String delimiter = null
                    for (String op : QUERY_OP_MAPPING.keySet()) {
                        if (word.contains(op)) {
                            OP = QUERY_OP_MAPPING.get(op)
                            delimiter = op
                            break
                        }
                    }

                    if (OP == null) {
                        throw new OperationFailureException(Platform.argerr("invalid query string[%s], word[%s] doesn't have a valid operator", qstr, word))
                    }

                    List<String> ks = word.split(Pattern.quote(delimiter), 2)
                    QueryCondition cond = new QueryCondition()
                    if (OP == QueryOp.IS_NULL.toString() || OP == QueryOp.NOT_NULL.toString()) {
                        cond.name = ks[0]
                        cond.op = OP
                    } else {
                        if (ks.size() != 2) {
                            throw new OperationFailureException(Platform.argerr("invalid query string[%s], word[%s] doesn't has key-value pair", qstr, word))
                        }
                        cond.name = ks[0]
                        cond.op = OP
                        cond.value = ks[1]
                    }

                    msg.getConditions().add(cond)
                }
            }
        }

        if (count) {
            msg.setCount(true)
        }
        if (replyWithCount) {
            msg.setReplyWithCount(true)
        }

        ZQLQueryResult ret = queryf.queryUseZQL(msg, inventoryClass)

        return ["total": ret.total, "result": ret.inventories == null ? null : JSONObjectUtil.rehashObject(ret.inventories, ArrayList.class)]
    }

    private String errorLine(String code, Throwable  e) {
        Throwable t = e
        while (t.cause != null) {
            t = t.cause
        }

        def trace = t.stackTrace.find {
            it.fileName ==~ /^Script\d+\.groovy$/
        }

        if (!trace.hasProperty("lineNumber")) {
            throw e
        }

        def lineNum = trace.lineNumber - 1
        println(code.readLines())
        def line = code.readLines()[lineNum]
        return "${e.message}, error at line ${lineNum}: ${line}"
    }

    Map<String, Object> query(APIBatchQueryMsg msg) {
        try {
            session = msg.getSession()
            Binding binding = new Binding()
            Map<String, Object> output = [:]

            def query = { doQuery(it) }
            def put = { k, v -> output[k] = v }
            def call = { apiName, value -> syncApiCall(apiName, value) }

            binding.setVariable("query", query)
            binding.setVariable("put", put)
            binding.setVariable("call", call)

            def cc = new CompilerConfiguration()
            cc.addCompilationCustomizers(new SandboxTransformer())

            def shell = new GroovyShell(binding, cc)
            sandbox.register()
            try {
                shell.evaluate(msg.script)
            } catch (Throwable t) {
                logger.warn(t.message, t)
                sandbox.unregister()
                throw new OperationFailureException(Platform.operr("${errorLine(msg.script, t)}"))
            } finally {
                sandbox.unregister()
            }

            return output
        } catch (Throwable t) {
            if (!(t instanceof OperationFailureException)) {
                logger.warn(t.message, t)
            }

            throw t
        }
    }
}
