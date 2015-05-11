package org.zstack.core.workflow;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 2:38 PM
 * To change this template use File | Settings | File Templates.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SimpleFlowChain implements FlowTrigger, FlowChain {
    private static final CLogger logger = Utils.getLogger(SimpleFlowChain.class);

    private List<Flow> flows = new ArrayList<Flow>();
    private Stack<Flow> rollBackFlows = new Stack<Flow>();
    private Map data = new HashMap();
    private Iterator<Flow> it;
    private boolean isStart = false;
    private boolean isRollbackStart = false;
    private Flow currentFlow;
    private Flow currentRollbackFlow;
    private ErrorCode errorCode;
    private FlowErrorHandler errorHandler;
    private FlowDoneHandler doneHandler;
    private String name;
    private boolean skipRestRollbacks;
    private boolean allowEmptyFlow;

    private static final Map<String, WorkFlowStatistic> statistics = new ConcurrentHashMap<String, WorkFlowStatistic>();

    private class FlowStopWatch {
        Map<String, Long> beginTime = new HashMap<String, Long>();
        void start(Flow flow) {
            long btime = System.currentTimeMillis();
            if (currentFlow != null) {
                String cname = getFlowName(currentFlow);
                long stime = beginTime.get(cname);
                WorkFlowStatistic stat = statistics.get(cname);
                stat.addStatistic(btime - stime);
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

    private FlowStopWatch stopWath;

    {
        if (CoreGlobalProperty.PROFILER_WORKFLOW) {
            stopWath = new FlowStopWatch();
        }
    }

    @Autowired
    private ErrorFacade errf;

    public SimpleFlowChain() {
    }

    public SimpleFlowChain(Map<String, Object> data) {
        this.data.putAll(data);
    }

    @Override
    public List<Flow> getFlows() {
        return flows;
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
    public FlowChain setData(Map data) {
        this.data = data;
        return  this;
    }

    @Override
    public FlowChain setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public Map getData() {
        return this.data;
    }

    @Override
    public SimpleFlowChain done(FlowDoneHandler handler) {
        DebugUtils.Assert(doneHandler==null, String.format("there has been a FlowDoneHandler installed"));
        doneHandler = handler;
        return this;
    }

    private void runFlow(Flow flow) {
        if (CoreGlobalProperty.PROFILER_WORKFLOW) {
            stopWath.start(flow);
        }

        try {
            currentFlow = flow;
            String info = String.format("[FlowChain: %s] start executing flow[%s]", name, getFlowName(currentFlow));
            logger.debug(info);
            flow.run(this, data);
        } catch (OperationFailureException oe) {
            String errInfo = oe.getErrorCode() != null ? oe.getErrorCode().toString() : "";
            logger.warn(errInfo, oe);
            setErrorCode(oe.getErrorCode());
            rollBackFlows.push(currentFlow);
            rollback();
        } catch (FlowException fe) {
            String errInfo = fe.getErrorCode() != null ? fe.getErrorCode().toString() : "";
            logger.warn(errInfo, fe);
            setErrorCode(fe.getErrorCode());
            rollBackFlows.push(currentFlow);
            rollback();
        } catch (Throwable t) {
            logger.warn(String.format("[FlowChain: %s] unhandled exception when executing flow[%s], start to rollback", name, flow.getClass().getName()), t);
            setErrorCode(errf.throwableToInternalError(t));
            rollBackFlows.push(currentFlow);
            rollback();
        }
    }

    private void rollbackFlow(Flow flow) {
        try {
            logger.debug(String.format("[FlowChain: %s] start to rollback flow[%s]", name, getFlowName(flow)));
            flow.rollback(this, data);
        } catch (Throwable t) {
            logger.warn(String.format("[FlowChain: %s] unhandled exception when rollback flow[%s], continue to next rollback", name, flow.getClass().getSimpleName()), t);
            rollback();
        }
    }

    private void callErrorHandler(boolean info) {
        try {
            if (info) {
                logger.debug(String.format("[FlowChain: %s] rolled back all flows because error%s", name, errorCode));
            }
            if (errorHandler != null) {
                errorHandler.handle(errorCode, this.data);
            }
        } catch (Throwable t) {
            logger.warn(String.format("[FlowChain: %s] unhandled exception when calling error handler", name), t);
        }
    }

    private String getFlowName(Flow flow) {
        String name = FieldUtils.getFieldValue("__name__", flow);
        if (name != null) {
            return name;
        }

        name = flow.getClass().getSimpleName();
        if (name == null || name.equals("")) {
            name = flow.getClass().getName();
        }
        return name;
    }

    @Override
    public void rollback() {
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

            logger.debug(String.format("FlowChain: %s] we are instructed to skip rollbacks for remaining flows%s", name, restRollbackNames));
            callErrorHandler(true);
            return;
        }

        if (currentRollbackFlow != null) {
            logger.debug(String.format("[FlowChain: %s] successfully rolled back flow[%s]", name, getFlowName(currentRollbackFlow)));
        } else {
            logger.debug(String.format("[FlowChain: %s] start to rollback", name));
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

    private void callDoneHandler() {
        if (CoreGlobalProperty.PROFILER_WORKFLOW) {
            stopWath.stop();
        }

        try {
            logger.debug(String.format("[FlowChain: %s] successfully completed", name));
            if (doneHandler != null) {
                doneHandler.handle(this.data);
            }
        } catch (Throwable t) {
            logger.warn(String.format("[FlowChain: %s] unhandled exception when calling done handler", name), t);
        }
    }

    @Override
    public void fail(ErrorCode errorCode) {
        setErrorCode(errorCode);
        rollBackFlows.push(currentFlow);
        rollback();
    }

    @Override
    public void next() {
        if (!isStart) {
            throw new CloudRuntimeException(String.format("[FlowChain: %s] you must call start() first, and only call next() in Flow.run()", name));
        }

        if (isRollbackStart) {
            throw new CloudRuntimeException(String.format("[FlowChain: %s] rollback has started, you can't call next()", name));
        }

        rollBackFlows.push(currentFlow);

        logger.debug(String.format("[FlowChain: %s] successfully executed flow[%s]", name, getFlowName(currentFlow)));

        if (!it.hasNext()) {
            if (errorCode == null) {
                callDoneHandler();
            } else {
                callErrorHandler(false);
            }
            return;
        }

        Flow flow = it.next();
        runFlow(flow);
    }

    @Override
    public void start() {
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
            name = String.format("anonymous-chain");
        }

        logger.debug(String.format("[FlowChain: %s] starts", name));
        it = flows.iterator();
        Flow flow = it.next();
        runFlow(flow);
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
