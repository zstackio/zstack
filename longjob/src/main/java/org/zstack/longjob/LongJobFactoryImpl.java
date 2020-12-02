package org.zstack.longjob;

import org.zstack.header.Component;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobErrors;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.message.APIEvent;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import static org.zstack.core.Platform.operr;

/**
 * Created by GuoYi on 11/14/17.
 */
public class LongJobFactoryImpl implements LongJobFactory, Component {
    private static final CLogger logger = Utils.getLogger(LongJobFactoryImpl.class);
    /**
     * Key:LongJobName
     */
    private TreeMap<String, LongJob> allLongJob = new TreeMap<>();
    private TreeMap<String, String> fullJobName = new TreeMap<>();

    @Override
    public LongJob getLongJob(String jobName) {
        LongJob job = allLongJob.get(jobName);
        if (null == job) {
            throw new OperationFailureException(operr("%s has no corresponding longjob", jobName));
        }
        return job;
    }

    @Override
    public boolean start() {
        LongJob job = null;
        Set<Class<?>> longJobClasses = BeanUtils.reflections.getTypesAnnotatedWith(LongJobFor.class);
        for (Class it : longJobClasses) {
            LongJobFor at = (LongJobFor) it.getAnnotation(LongJobFor.class);
            try {
                job = (LongJob) it.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            if (null == job) {
                logger.warn(String.format("[LongJob] class name [%s] but get LongJob instance is null ", at.getClass().getSimpleName()));
                continue;
            }
            logger.debug(String.format("[LongJob] collect class [%s]", job.getClass().getSimpleName()));

            String jobName = at.value().getSimpleName();
            allLongJob.put(jobName, job);
            fullJobName.put(jobName, at.value().getName());
        }
        return true;
    }

    @Override
    public TreeMap<String, String> getFullJobName() {
        return fullJobName;
    }

    @Override
    public boolean stop() {
        allLongJob.clear();
        return true;
    }
}
