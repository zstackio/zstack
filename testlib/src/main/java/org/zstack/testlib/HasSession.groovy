package org.zstack.testlib

/**
 * Created by xing5 on 2017/2/15.
 */
trait HasSession {
    Closure session

    String accountName

    void useAccount(String name) {
        accountName = name

        if (this instanceof Spec) {
            preCreate {
                addDependency(name, AccountSpec.class)
            }
        }
    }
}