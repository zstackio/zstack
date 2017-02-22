package org.zstack.testlib

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
}