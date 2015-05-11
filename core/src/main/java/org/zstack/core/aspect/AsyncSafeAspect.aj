package org.zstack.core.aspect;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

public aspect AsyncSafeAspect {
    private static final CLogger logger = Utils.getLogger(AsyncSafeAspect.class);

    pointcut asyncSafe1() : execution(* *.*(.., Completion, ..));
    pointcut asyncSafe2() : execution(* *.*(.., NoErrorCompletion, ..));
    pointcut asyncSafe3() : execution(* *.*(.., ReturnValueCompletion, ..));

    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;

    interface Wrapper {
        void call(ErrorCode err);
    }

    private List<Wrapper> getAsyncInterface(Object[] args) {
        List<Wrapper> wrappers = new ArrayList<Wrapper>();
        for (final Object arg : args) {
            Wrapper w = null;
            if (arg instanceof Completion) {
                w = new Wrapper() {
                    @Override
                    public void call(ErrorCode err) {
                        Completion completion = (Completion)arg;
                        completion.fail(err);
                    }
                };
            } else if (arg instanceof ReturnValueCompletion) {
                w = new Wrapper() {
                    @Override
                    public void call(ErrorCode err) {
                        ReturnValueCompletion completion = (ReturnValueCompletion) arg;
                        completion.fail(err);
                    }
                };
            } else if (arg instanceof NoErrorCompletion) {
                w = new Wrapper() {
                    @Override
                    public void call(ErrorCode err) {
                        NoErrorCompletion completion = (NoErrorCompletion)arg;
                        completion.done();
                    }
                };
            } else if (arg instanceof Message) {
                w = new Wrapper() {
                    @Override
                    public void call(ErrorCode err) {
                        Message msg = (Message) arg;
                        bus.replyErrorByMessageType(msg, err);
                    }
                };
            }

            if (w != null) {
                wrappers.add(w);
            }
        }


        return wrappers;
    }

    void around() : asyncSafe1() || asyncSafe2() || asyncSafe3() {
        try {
            proceed();
        } catch (Throwable t) {
            List<Wrapper> wrappers = getAsyncInterface(thisJoinPoint.getArgs());
            if (wrappers.isEmpty()) {
                String err = String.format(
                        "%s has triggered async safe aspectj, however, it has neither Completion nor ReturnValueCompletion in its method arguments", thisJoinPoint
                                .getSignature().toLongString());
                throw new CloudRuntimeException(err, t);
            }

            ErrorCode errCode = null;
            if (t instanceof OperationFailureException) {
                errCode = ((OperationFailureException) t).getErrorCode();
            } else {
                String err = String.format("unhandled exception happened when calling %s, %s", thisJoinPoint.getSignature().toLongString(), t.getMessage());
                errCode = errf.stringToInternalError(err);
                logger.warn(err, t);
            }

            for (Wrapper w : wrappers) {
                w.call(errCode);
            }
        }
    }
}
