package org.zstack.header;

import org.apache.logging.log4j.ThreadContext;
import java.util.*;
import org.zstack.utils.TaskContext;

public aspect HasThreadContextAspect {
    public Map<String, String> HasThreadContext.threadContext;
    public List<String> HasThreadContext.threadContextStack;
    public Map<Object, Object> HasThreadContext.taskContext;

    after(HasThreadContext obj) : target(obj) && execution(HasThreadContext+.new(..)) {
        setThreadContext(obj);
    }

    public static void setThreadContext(HasThreadContext obj) {
        obj.threadContext = ThreadContext.getContext();
        obj.threadContextStack = ThreadContext.getImmutableStack().asList();
        obj.taskContext = TaskContext.getTaskContext();
    }
}