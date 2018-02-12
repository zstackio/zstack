package org.zstack.testlib

class SimpleEnvSpec extends EnvSpec {
    @Override
    void delete() {
        resourcesNeedDeletion.each {
            logger.info("run delete() method on ${it.class}")
            it.delete()
        }
    }

    @Override
    EnvSpec create(Closure cl = null) {
        adminLogin()

        deploy()
        if (cl != null) {
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
        }

        return this;
    }
}
