package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.header.message.Message
import org.zstack.header.rest.RESTConstant

/**
 * Created by xing5 on 2017/2/15.
 */
abstract class Spec implements Node, CreateAction, Tag, ApiHelper, DeleteAction {
    EnvSpec envSpec

    Spec(EnvSpec envSpec) {
        this.envSpec = envSpec
    }

    def findSpec(String name, Class type) {
        return envSpec.find(name, type)
    }

    def addDependency(String name, Class type) {
        def dep = findSpec(name, type)
        assert dep != null: "cannot find the dependency[name:$name, type:$type] for ${hasProperty("name") ? name : this}," +
                "check your environment()"
        dependencies.add(dep as Node)
    }

    void simulator(String path, Closure cl) {
        envSpec.simulator(path, cl)
    }

    void message(Class<? extends Message> msgClz, Closure cl) {
        envSpec.message(msgClz, cl)
    }

    final static void checkHttpCallType(HttpEntity<String> e, boolean isSync) {
        if (isSync) {
            assert e.getHeaders().getFirst(RESTConstant.TASK_UUID) == null: "you cannot send a KVMHostAsyncHttpCallMsg to a sync uri in agent"
        } else {
            assert e.getHeaders().getFirst(RESTConstant.TASK_UUID) != null: "you cannot send a KVMHostSyncHttpCallMsg to a async uri in agent"
        }
    }
    
    void message(Class<? extends Message> msgClz, Closure condition, Closure handler) {
        envSpec.message(msgClz, condition, handler)
    }
}