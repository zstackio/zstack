package org.zstack.sdnController;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.sdnController.header.APIAddSdnControllerMsg;
import org.zstack.sdnController.header.SdnControllerConstant.Processes;
import org.zstack.sdnController.header.SdnControllerConstant.Operations;
import org.zstack.sdnController.header.SdnControllerConstant.ResourceTypes;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Method;
import java.util.Locale;

@Aspect
@Component
public class SdnControllerLogAspect {
    final CLogger logger = Utils.getLogger(this.getClass());

    @Pointcut("@annotation(org.zstack.sdnController.SdnControllerLog)")
    public void logPointCut() {
    }

    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long beginTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long time = System.currentTimeMillis() - beginTime;
        saveLog(joinPoint, time);
        return result;
    }

    void saveLog(ProceedingJoinPoint joinPoint, long time) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = signature.getName();
        String className = joinPoint.getTarget().getClass().getName();
        String parseName = methodName.toUpperCase(Locale.ROOT);
        String process = "";
        String operation = "";
        String resourceType = "";
        String name = "";
        for (Processes op : Processes.values()) {
            if (parseName.contains(op.toString().toUpperCase(Locale.ROOT))){
                process = op.toString();
            }
        }
        for (Operations op : Operations.values()) {
            if (parseName.contains(op.toString().toUpperCase(Locale.ROOT))){
               operation = op.toString();
            }
        }
        for (ResourceTypes type : ResourceTypes.values()) {
            if (parseName.contains(type.toString().toUpperCase(Locale.ROOT))){
                resourceType = type.toString();
            }
        }
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof APIAddSdnControllerMsg) {
                name = ((APIAddSdnControllerMsg) arg).getName();
                break;
            }

            if (arg instanceof L2VxlanNetworkInventory) {
                name = ((L2VxlanNetworkInventory) arg).getName();
                break;
            }
        }

        SdnControllerLog sdnControllerLog = method.getAnnotation(SdnControllerLog.class);
        logger.debug(String.format("sdn controller [%s]'s %s operation [%s-%s] for %s, the execution time is %s ms", className, process, operation, resourceType, name, time));
    }
}
