package org.zstack.longjob;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.utils.BeanUtils;

import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created by GuoYi on 11/14/17.
 */
public class LongJobFactoryImpl implements LongJobFactory {
    @Override
    public LongJob getLongJob(String jobName) {
        LongJob job = null;
        List<Class> longJobClasses = BeanUtils.scanClass("org.zstack", LongJobFor.class);
        for (Class it : longJobClasses) {
            LongJobFor at = (LongJobFor) it.getAnnotation(LongJobFor.class);
            if (at.value().getSimpleName().equals(jobName)) {
                try {
                    job = (LongJob) it.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        if (job == null) {
            throw new OperationFailureException(operr(jobName + " has no corresponding longjob"));
        }
        return job;
    }
}
