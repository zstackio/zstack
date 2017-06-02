package org.zstack.core.workflow;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 2:38 PM
 * To change this template use File | Settings | File Templates.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SimpleFlowChain implements FlowTrigger, FlowRollback, FlowChain, FlowChainMutable {
    private static final CLogger logger = Utils.getLogger(SimpleFlowChain.class);

    private String id;
    private List<Flow> flows = new ArrayList<>();
    private Stack<Flow> rollBackFlows = new Stack<>();
    private Map data = new HashMap();
    private Iterator<Flow> it;
    private boolean isStart = false;
    private boolean isRollbackStart = false;
    private Flow currentFlow;
    private Flow currentRollbackFlow;
    private ErrorCode errorCode;
    private FlowErrorHandler errorHandler;
    private FlowDoneHandler doneHandler;
    private FlowFinallyHandler finallyHandler;
    private String name;
    private boolean skipRestRollbacks;
    private boolean allowEmptyFlow;
    private FlowMarshaller flowMarshaller;
    private List<FlowChainProcessor> processors;
    private List<List<Runnable>> afterDone = new ArrayList<>();
    private List<List<Runnable>> afterError = new ArrayList<>();
    private List<List<Runnable>> afterFinal = new ArrayList<>();

    private boolean isFailCalled;

    private static final Map<String, WorkFlowStatistic> statistics = new ConcurrentHashMap<>();

    private class FlowStopWatch {
        Map<String, Long> beginTime = new HashMap<>();
        void start(Flow flow) {
            long btime = System.currentTimeMillis();
            if (currentFlow != null) {
                String cname = getFlowName(currentFlow);
                long stime = beginTime.get(cname);
                WorkFlowStatistic stat = statistics.get(cname);
                stat.addStatistic(btime - stime);

                logger.debug(String.format("[FlowChain(%s):%s, flow:%s] takes %sms to complete",
                        id, name, cname, stat.getTotalTime()));
            }

            String fname = getFlowName(flow);
            beginTime.put(fname, btime);
            WorkFlowStatistic stat = statistics.get(fname);
            if (stat == null) {
                stat = new WorkFlowStatistic();
                stat.setName(fname);
                statistics.put(fname, stat);
            }
        }

        void stop() {
            long etime = System.currentTimeMillis();
            if (currentFlow != null) {
                String fname = getFlowName(currentFlow);
                long stime = beginTime.get(fname);
                WorkFlowStatistic stat = statistics.get(fname);
                stat.addStatistic(etime - stime);
            }
        }
    }

    private FlowStopWatch stopWatch;

    {
        if (CoreGlobalProperty.PROFILER_WORKFLOW) {
            stopWatch = new FlowStopWatch();
        }
    }

    @Autowired
    private ErrorFacade errf;

    public SimpleFlowChain() {
        id = "FCID_" + Platform.getUuid().substring(0, 8);
    }

    public SimpleFlowChain(Map<String, Object> data) {
        id = "FCID_" + Platform.getUuid().substring(0, 8);
        this.data.putAll(data);
    }

    @Override
    public List<Flow> getFlows() {
        return flows;
    }

    @Override
    public void setFlows(List<Flow> flows) {
        this.flows = flows;
    }

    @Override
    public FlowDoneHandler getFlowDoneHandler() {
        return doneHandler;
    }

    @Override
    public void setFlowDoneHandler(FlowDoneHandler handler) {
        done(handler);
    }

    @Override
    public FlowErrorHandler getFlowErrorHandler() {
        return errorHandler;
    }

    @Override
    public void setFlowErrorHandler(FlowErrorHandler handler) {
        error(handler);
    }

    @Override
    public FlowFinallyHandler getFlowFinallyHandler() {
        return finallyHandler;
    }

    @Override
    public void setFlowFinallyHandler(FlowFinallyHandler handler) {
        Finally(handler);
    }

    @Override
    public String getChainName() {
        return name;
    }

    @Override
    public void setChainName(String name) {
        setName(name);
    }

    @Override
    public Map getChainData() {
        return data;
    }

    @Override
    public void setChainData(Map data) {
        setData(data);
    }

    @Override
    public FlowChain insert(Flow flow) {
        flows.add(0, flow);
        return this;
    }

    @Override
    public FlowChain insert(int pos, Flow flow) {
        flows.add(pos, flow);
        return this;
    }

    @Override
    public FlowChain setFlowMarshaller(FlowMarshaller marshaller) {
        flowMarshaller = marshaller;
        return this;
    }

    public SimpleFlowChain then(Flow flow) {
        flows.add(flow);
        return this;
    }

    public SimpleFlowChain error(FlowErrorHandler handler) {
        DebugUtils.Assert(errorHandler==null, "there has been an FlowErrorHandler installed");
        errorHandler = handler;
        return this;
    }

    @Override
    public FlowChain Finally(FlowFinallyHandler handler) {
        finallyHandler = handler;
        return this;
    }

    @Override
    public FlowChain setData(Map data) {
        this.data.putAll(data);
        return this;
    }

    @Override
    public FlowChain putData(Entry...es) {
        for (Map.Entry e : es) {
            data.put(e.getKey(), e.getValue());
        }
        return this;
    }

    @Override
    public FlowChain setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public void setProcessors(List<FlowChainProcessor> processors) {
        this.processors = processors;
    }

    @Override
    public Map getData() {
        return this.data;
    }

    @Override
    public SimpleFlowChain done(FlowDoneHandler handler) {
        DebugUtils.Assert(doneHandler==null, "there has been a FlowDoneHandler installed");
        doneHandler = handler;
        return this;
    }

    private void collectAfterRunnable(Flow flow) {
        List<Field> ad = FieldUtils.getAnnotatedFieldsOnThisClass(AfterDone.class, flow.getClass());
        for (Field f : ad) {
            List lst = FieldUtils.getFieldValue(f.getName(), flow);
            if (lst != null) {
                afterDone.add(lst);
            }
        }

        ad = FieldUtils.getAnnotatedFieldsOnThisClass(AfterError.class, flow.getClass());
        for (Field f : ad) {
            List lst = FieldUtils.getFieldValue(f.getName(), flow);
            if (lst != null) {
                afterError.add(lst);
            }
        }

        ad = FieldUtils.getAnnotatedFieldsOnThisClass(AfterFinal.class, flow.getClass());
        for (Field f : ad) {
            List lst = FieldUtils.getFieldValue(f.getName(), flow);
            if (lst != null) {
                afterFinal.add(lst);
            }
        }
    }

    private void runFlow(Flow flow) {
        try {
            Flow toRun = null;
            if (flowMarshaller != null) {
                toRun = flowMarshaller.marshalTheNextFlow(currentFlow == null ? null : currentFlow.getClass().getName(),
                        flow.getClass().getName(), this, data);
                if (toRun != null) {
                    logger.debug(String.format("[FlowChain(%s): %s] FlowMarshaller[%s] replaces the next flow[%s] to the flow[%s]",
                            id, name, flowMarshaller.getClass(), flow.getClass(), toRun.getClass()));
                }
            }

            if (toRun == null) {
                toRun = flow;
            }

            if (CoreGlobalProperty.PROFILER_WORKFLOW) {
                stopWatch.start(toRun);
            }

            currentFlow = toRun;

            String flowName = getFlowName(currentFlow);
            String info = String.format("[FlowChain(%s): %s] start executing flow[%s]", id, name, flowName);
            logger.debug(info);
            collectAfterRunnable(toRun);
            toRun.run(this, data);
        } catch (OperationFailureException oe) {
            String errInfo = oe.getErrorCode() != null ? oe.getErrorCode().toString() : "";
            logger.warn(errInfo, oe);
            fail(oe.getErrorCode());
        } catch (FlowException fe) {
            String errInfo = fe.getErrorCode() != null ? fe.getErrorCode().toString() : "";
            logger.warn(errInfo, fe);
            fail(fe.getErrorCode());
        } catch (Throwable t) {
            logger.warn(String.format("[FlowChain(%s): %s] unhandled exception when executing flow[%s], start to rollback",
                    id, name, flow.getClass().getName()), t);
            fail(errf.throwableToInternalError(t));
        }
    }

    private void rollbackFlow(Flow flow) {
        try {
            logger.debug(String.format("[FlowChain(%s): %s] start to rollback flow[%s]", id, name, getFlowName(flow)));
            flow.rollback(this, data);
        } catch (Throwable t) {
            logger.warn(String.format("[FlowChain(%s): %s] unhandled exception when rollback flow[%s]," +
                    " continue to next rollback", id, name, flow.getClass().getSimpleName()), t);
            rollback();
        }
    }

    private void callErrorHandler(boolean info) {
        if (info) {
            logger.debug(String.format("[FlowChain(%s): %s] rolled back all flows because error%s", id, name, errorCode));
        }

        if (errorHandler != null) {
            try {
                errorHandler.handle(errorCode, this.data);
            } catch (Throwable t) {
                logger.warn(String.format("unhandled exception when calling %s", errorHandler.getClass()), t);
            }
        }

        if (!afterError.isEmpty()) {
            Collections.reverse(afterError);

            for (List errors : afterError) {
                CollectionUtils.safeForEach(errors, new ForEachFunction<Runnable>() {
                    @Override
                    public void run(Runnable arg) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("call after error handler %s", arg.getClass()));
                        }
                        arg.run();
                    }
                });
            }
        }

        callFinallyHandler();
    }

    private String getFlowName(Flow flow) {
        String name = FieldUtils.getFieldValue("__name__", flow);
        if (name == null) {
            name = flow.getClass().getSimpleName();
            if (name.equals("")) {
                name = flow.getClass().getName();
            }
        }

        if (logger.isTraceEnabled()) {
            String className = flow.getClass().getName();
            String[] ff = className.split("\\.");
            String filename = ff[ff.length-1];
            if (filename.contains("$")) {
                int index = filename.indexOf("$");
                filename = filename.substring(0, index);
            }

            name = String.format("%s.java:%s", filename, name);
        }

        return name;
    }

    @Override
    public void rollback() {
        if (!isFailCalled) {
            throw new CloudRuntimeException("rollback() cannot be called before fail() is called");
        }

        isRollbackStart = true;
        if (rollBackFlows.empty()) {
            callErrorHandler(true);
            return;
        }

        if (skipRestRollbacks) {
            List<String> restRollbackNames = CollectionUtils.transformToList(rollBackFlows, new Function<String, Flow>() {
                @Override
                public String call(Flow arg) {
                    return arg.getClass().getSimpleName();
                }
            });

            logger.debug(String.format("[FlowChain(%s): %s] we are instructed to skip rollbacks for remaining flows%s",
                    id, name, restRollbackNames));
            callErrorHandler(true);
            return;
        }

        if (currentRollbackFlow != null) {
            logger.debug(String.format("[FlowChain(%s): %s] successfully rolled back flow[%s]",
                    id, name, getFlowName(currentRollbackFlow)));
        } else {
            logger.debug(String.format("[FlowChain(%s): %s] start to rollback", id, name));
        }

        Flow flow = rollBackFlows.pop();
        currentRollbackFlow = flow;
        rollbackFlow(flow);
    }

    @Override
    public void skipRestRollbacks() {
        skipRestRollbacks = true;
    }

    @Override
    public void setError(ErrorCode error) {
        setErrorCode(error);
    }

    private void callFinallyHandler() {
        if (finallyHandler != null) {
            try {
                finallyHandler.Finally();
            } catch (Throwable t) {
                logger.warn(String.format("unhandled exception when calling %s", finallyHandler.getClass()), t);
            }
        }

        if (!afterFinal.isEmpty()) {
            Collections.reverse(afterFinal);

            for (List finals : afterFinal) {
                CollectionUtils.safeForEach(finals, new ForEachFunction<Runnable>() {
                    @Override
                    public void run(Runnable arg) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("call after final handler %s", arg.getClass()));
                        }

                        arg.run();
                    }
                });
            }
        }
    }

    private void callDoneHandler() {
        if (CoreGlobalProperty.PROFILER_WORKFLOW) {
            stopWatch.stop();
        }

        if (doneHandler != null) {
            try {
                doneHandler.handle(this.data);
            } catch (Throwable t) {
                logger.warn(String.format("unhandled exception when calling %s", doneHandler.getClass()), t);
            }
        }

        logger.debug(String.format("[FlowChain(%s): %s] successfully completed", id, name));

        if (!afterDone.isEmpty()) {
            Collections.reverse(afterDone);

            for (List dones : afterDone) {
                CollectionUtils.safeForEach(dones, new ForEachFunction<Runnable>() {
                    @Override
                    public void run(Runnable arg) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("call after done handler %s", arg.getClass()));
                        }
                        arg.run();
                    }
                });
            }
        }

        callFinallyHandler();
    }

    @Override
    public void fail(ErrorCode errorCode) {
        isFailCalled = true;
        setErrorCode(errorCode);
        rollBackFlows.push(currentFlow);
        rollback();
    }

    private boolean isSkipFlow(Flow flow) {
        Boolean skip = FieldUtils.getFieldValue("__skip__", flow);
        boolean ret = skip != null && skip;
        if (ret) {
            logger.debug(String.format("[FlowChain: %s] skip flow[%s] because it's __skip__ set to true", name, getFlowName(flow)));
        }
        return ret;
    }

    private Flow getFirstNotSkippedFlow() {
        Flow flow = null;
        while (it.hasNext()) {
            flow = it.next();
            if (!isSkipFlow(flow)) {
                break;
            }
        }

        return flow;
    }

    @Override
    public void next() {
        if (!isStart) {
            throw new CloudRuntimeException(
                    String.format("[FlowChain(%s): %s] you must call start() first, and only call next() in Flow.run()",
                            id, name));
        }

        if (isRollbackStart) {
            throw new CloudRuntimeException(
                    String.format("[FlowChain(%s): %s] rollback has started, you can't call next()", id, name));
        }

        rollBackFlows.push(currentFlow);

        logger.debug(String.format("[FlowChain(%s): %s] successfully executed flow[%s]", id, name, getFlowName(currentFlow)));

        Flow flow = getFirstNotSkippedFlow();
        if (flow == null) {
            // no flows, or all flows are skipped
            if (errorCode == null) {
                callDoneHandler();
            } else {
                callErrorHandler(false);
            }
        } else {
            runFlow(flow);
        }
    }

    @Override
    public void start() {
        if (processors != null) {
            for (FlowChainProcessor p : processors) {
                p.processFlowChain(this);
            }
        }

        if (flows.isEmpty() && allowEmptyFlow) {
            callDoneHandler();
            return;
        }

        if (flows.isEmpty()) {
            throw new CloudRuntimeException("you must call then() to add flow before calling start() or allowEmptyFlow() to run empty flow chain on purpose");
        }

        if (data == null) {
            data = new HashMap<String, Object>();
        }

        isStart = true;
        if (name == null) {
            name = "anonymous-chain";
        }

        logger.debug(String.format("[FlowChain(%s): %s] starts", id, name));

        if (logger.isTraceEnabled()) {
            List<String> names = CollectionUtils.transformToList(flows, new Function<String, Flow>() {
                @Override
                public String call(Flow arg) {
                    return String.format("%s[%s]", arg.getClass(), getFlowName(arg));
                }
            });
            logger.trace(String.format("execution path:\n%s", StringUtils.join(names, " -->\n")));
        }

        it = flows.iterator();
        Flow flow = getFirstNotSkippedFlow();
        if (flow == null) {
            // all flows are skipped
            callDoneHandler();
        } else {
            runFlow(flow);
        }
    }

    @Override
    public FlowChain noRollback(boolean no) {
        skipRestRollbacks = no;
        return this;
    }

    @Override
    public FlowChain allowEmptyFlow() {
        allowEmptyFlow = true;
        return this;
    }

    private void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public static Map<String, WorkFlowStatistic> getStatistics() {
        return statistics;
    }
}
