package org.zstack.core.safeguard;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public aspect SafeGuardAspect {
    private static final CLogger logger = Utils.getLogger(SafeGuardAspect.class);

    pointcut safeGuardCalled() : execution(@Guard * *.*(..));
    
    declare error: call(void SafeGuard.guard(..)) && !withincode(@Guard * *.*(..)) : "SafeGuard.guard() must be called in method with @Guard annotation";

    before() : safeGuardCalled() {
        SafeGuard.pushTop();
        String methodName = "[" + thisJoinPoint.getSignature().toLongString() + "]";
        String msg = "Saving safe guard stack pointer for " + methodName + ". Current statc top at: " + SafeGuard.getTop() + ". Current stack size: "
                + SafeGuard.getSize();
        logger.trace(msg);
    }

    private boolean isInstance(Class<? extends Throwable>[] arr, Throwable t) {
        for (Class<? extends Throwable> i : arr) {
           if (i.isInstance(t)) {
               return true;
           }
        }
        return false;
    }
    
    private boolean needRollback(JoinPoint point, Throwable t) {
        MethodSignature mtd = (MethodSignature) point.getStaticPart().getSignature();
        final Guard at = mtd.getMethod().getAnnotation(Guard.class);

        if (at.rollbackForClass().length != 0) {
            if (isInstance(at.rollbackForClass(), t)) {
                return true;
            } else {
                return false;
            }
        }

        if (at.noRollbackForClass().length != 0 && isInstance(at.noRollbackForClass(), t)) {
            return false;
        }

        return true;
    }

    after() throwing(Throwable t) : safeGuardCalled() {
        if (needRollback(thisJoinPoint, t)) {
            int rollbackNum = SafeGuard.getSize() - SafeGuard.getTop();
            String methodName = "[" + thisJoinPoint.getSignature().toLongString() + "]";
            SafeGuard.rollback(methodName, SafeGuard.popTop());
            String msg = methodName + " throws out a throwable(" + t.getClass().getCanonicalName() + ")." + " Having rolled back " + rollbackNum
                    + " safeguards. " + "Next rollback/revert will cause stack top back to: " + SafeGuard.getTop() + ". Current stack size: "
                    + SafeGuard.getSize();
            logger.trace(msg);
        } else {
            revert(thisJoinPoint);
        }
    }
    
    private void revert(JoinPoint point) {
        int revertNum = SafeGuard.getSize() - SafeGuard.getTop();
        SafeGuard.revert(SafeGuard.popTop());
        String methodName = "[" + point.getSignature().toLongString() + "]";
        String msg = "Having reverted " + revertNum + " safeguards stack pointer for " + methodName + ". Next rollback/revert will cause stack top back to: "
                + SafeGuard.getTop() + ". Current stack size: " + SafeGuard.getSize();
        logger.trace(msg);
    }

    after() returning : safeGuardCalled() {
        revert(thisJoinPoint);
    }
}
